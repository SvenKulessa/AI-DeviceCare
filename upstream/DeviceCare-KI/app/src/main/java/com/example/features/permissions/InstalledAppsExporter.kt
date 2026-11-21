package com.example.features.permissions

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class AppExportFormat(
  val extension: String,
  val mimeType: String,
  val label: String,
) {
  CSV(extension = "csv", mimeType = "text/csv", label = "CSV-Tabelle"),
  JSON(extension = "json", mimeType = "application/json", label = "JSON-Datei"),
}

data class AppExportSummary(
  val totalVisibleApps: Int,
  val userAppsCount: Int,
  val systemAppsCount: Int,
  val exportAppsCount: Int,
)

/**
 * Formatiert und schreibt die lokal gescannte Liste OS-sichtbarer installierter Apps (standardmäßig
 * inklusive System-Apps) über den berechtigungsfreien Android Storage Access Framework (SAF)
 * Dateidialog.
 */
object InstalledAppsExporter {

  fun summarize(
    scanResult: InstalledAppsScanResult,
    includeSystemApps: Boolean = true,
  ): AppExportSummary {
    val userCount = scanResult.apps.count { !it.isSystemApp }
    val systemCount = scanResult.apps.count { it.isSystemApp }
    val exportCount = if (includeSystemApps) scanResult.apps.size else userCount
    return AppExportSummary(
      totalVisibleApps = scanResult.apps.size,
      userAppsCount = userCount,
      systemAppsCount = systemCount,
      exportAppsCount = exportCount,
    )
  }

  fun selectAppsForExport(
    scanResult: InstalledAppsScanResult,
    includeSystemApps: Boolean = true,
  ): List<InstalledAppPermissionInfo> =
    scanResult.apps
      .filter { includeSystemApps || !it.isSystemApp }
      .sortedWith(
        compareBy<InstalledAppPermissionInfo> { it.isSystemApp }
          .thenBy { it.appName.lowercase(Locale.ROOT) }
          .thenBy { it.packageName.lowercase(Locale.ROOT) }
      )

  fun export(
    scanResult: InstalledAppsScanResult,
    format: AppExportFormat,
    includeSystemApps: Boolean = true,
    exportedAtMillis: Long = System.currentTimeMillis(),
  ): String =
    when (format) {
      AppExportFormat.CSV -> exportAsCsv(scanResult, includeSystemApps)
      AppExportFormat.JSON -> exportAsJson(scanResult, includeSystemApps, exportedAtMillis)
    }

  fun exportAsCsv(
    scanResult: InstalledAppsScanResult,
    includeSystemApps: Boolean = true,
  ): String {
    val apps = selectAppsForExport(scanResult, includeSystemApps)
    return buildString {
      appendLine(
        "app_name,package_name,app_type,version_name,total_requested_permissions,granted_dangerous_count,critical_sensors,granted_dangerous_permissions"
      )
      apps.forEach { app ->
        val appType = if (app.isSystemApp) "System-App" else "Nutzer-App"
        val criticalSensors = app.criticalSensorTags.joinToString("; ")
        val grantedPerms = app.grantedDangerousPermissions.joinToString("; ") { it.fullName }
        append(escapeCsvCell(app.appName))
        append(',')
        append(escapeCsvCell(app.packageName))
        append(',')
        append(escapeCsvCell(appType))
        append(',')
        append(escapeCsvCell(app.versionName.orEmpty()))
        append(',')
        append(app.totalRequestedPermissions)
        append(',')
        append(app.grantedDangerousPermissions.size)
        append(',')
        append(escapeCsvCell(criticalSensors))
        append(',')
        appendLine(escapeCsvCell(grantedPerms))
      }
    }
  }

  fun exportAsJson(
    scanResult: InstalledAppsScanResult,
    includeSystemApps: Boolean = true,
    exportedAtMillis: Long = System.currentTimeMillis(),
  ): String {
    val apps = selectAppsForExport(scanResult, includeSystemApps)
    val userCount = apps.count { !it.isSystemApp }
    val systemCount = apps.count { it.isSystemApp }

    val appsArray = JSONArray()
    apps.forEach { app ->
      val criticalArray = JSONArray().apply { app.criticalSensorTags.forEach { put(it) } }
      val permsArray =
        JSONArray().apply {
          app.grantedDangerousPermissions.forEach { perm ->
            put(
              JSONObject()
                .put("fullName", perm.fullName)
                .put("shortName", perm.shortName)
                .put("category", perm.category.name)
            )
          }
        }
      appsArray.put(
        JSONObject()
          .put("appName", app.appName)
          .put("packageName", app.packageName)
          .put("appType", if (app.isSystemApp) "system" else "user")
          .put("isSystemApp", app.isSystemApp)
          .put("versionName", app.versionName ?: JSONObject.NULL)
          .put("totalRequestedPermissions", app.totalRequestedPermissions)
          .put("grantedDangerousCount", app.grantedDangerousPermissions.size)
          .put("criticalSensors", criticalArray)
          .put("grantedDangerousPermissions", permsArray)
      )
    }

    return JSONObject()
      .put("schemaVersion", 1)
      .put("scannedAtMillis", scanResult.scannedAtMillis)
      .put("exportedAtMillis", exportedAtMillis)
      .put(
        "scopeNote",
        "Alle vom Android-System sichtbaren installierten Apps ohne QUERY_ALL_PACKAGES (lokaler Export)",
      )
      .put("includeSystemApps", includeSystemApps)
      .put("totalVisiblePackages", scanResult.visiblePackageCount)
      .put("exportedCount", apps.size)
      .put("userAppsCount", userCount)
      .put("systemAppsCount", systemCount)
      .put("apps", appsArray)
      .toString(2)
  }

  fun suggestedFileName(
    format: AppExportFormat,
    timestampMillis: Long = System.currentTimeMillis(),
  ): String {
    val formatter =
      SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val stamp = formatter.format(Date(timestampMillis))
    return "devicecare-apps-$stamp.${format.extension}"
  }

  suspend fun writeTextToUri(
    context: Context,
    uri: Uri,
    content: String,
  ): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val stream =
          context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Ausgabestream für Zieldatei konnte nicht geöffnet werden.")
        stream.use { output ->
          output.write(bytes)
          output.flush()
        }
        bytes.size
      }
    }

  suspend fun writeTempExportFile(
    context: Context,
    scanResult: InstalledAppsScanResult,
    format: AppExportFormat,
    includeSystemApps: Boolean = true,
    timestampMillis: Long = System.currentTimeMillis(),
  ): Result<File> =
    withContext(Dispatchers.IO) {
      runCatching {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists() && !exportDir.mkdirs()) {
          throw IOException("Temporäres Export-Verzeichnis konnte nicht erstellt werden.")
        }
        // Halte den Cache klein: ältere temporäre Exportdateien vorab entfernen
        exportDir.listFiles()?.forEach { oldFile ->
          if (oldFile.isFile) {
            oldFile.delete()
          }
        }
        val targetFile = File(exportDir, suggestedFileName(format, timestampMillis))
        val payload = export(scanResult, format, includeSystemApps, timestampMillis)
        targetFile.writeText(payload, Charsets.UTF_8)
        targetFile
      }
    }

  fun buildShareFileIntent(
    context: Context,
    file: File,
    format: AppExportFormat,
    exportedCount: Int,
    includeSystemApps: Boolean,
  ): Intent {
    val contentUri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val scopeLabel = if (includeSystemApps) "inkl. System-Apps" else "nur Nutzer-Apps"
    return Intent(Intent.ACTION_SEND).apply {
      type = format.mimeType
      putExtra(Intent.EXTRA_STREAM, contentUri)
      putExtra(Intent.EXTRA_SUBJECT, "AI-DeviceCare App-Liste ($exportedCount Apps, $scopeLabel)")
      putExtra(
        Intent.EXTRA_TEXT,
        "Lokaler Export von $exportedCount OS-sichtbaren Apps ($scopeLabel, ohne QUERY_ALL_PACKAGES).",
      )
      clipData = ClipData.newRawUri(file.name, contentUri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  fun launchShareFileChooser(
    context: Context,
    shareIntent: Intent,
    chooserTitle: String,
    activityStarter: (Intent) -> Unit = { context.startActivity(it) },
  ): Boolean {
    val chooser =
      Intent.createChooser(shareIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    return try {
      activityStarter(chooser)
      true
    } catch (_: ActivityNotFoundException) {
      false
    } catch (_: RuntimeException) {
      false
    }
  }

  internal fun escapeCsvCell(value: String): String {
    val sanitized = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
    val needsQuotes =
      sanitized.contains(',') || sanitized.contains('"') || sanitized.startsWith(' ') || sanitized.endsWith(' ')
    return if (needsQuotes) {
      "\"${sanitized.replace("\"", "\"\"")}\""
    } else {
      sanitized
    }
  }
}
