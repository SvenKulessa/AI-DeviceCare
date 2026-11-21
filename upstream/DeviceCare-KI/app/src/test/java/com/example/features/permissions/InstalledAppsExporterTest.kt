package com.example.features.permissions

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InstalledAppsExporterTest {

  private val userApp =
    InstalledAppPermissionInfo(
      packageName = "com.example.usercam",
      appName = "Foto, Notiz \"Pro\"",
      isSystemApp = false,
      totalRequestedPermissions = 5,
      grantedDangerousPermissions =
        listOf(
          GrantedDangerousPermission(
            fullName = "android.permission.CAMERA",
            shortName = "CAMERA",
            category = SensitivityCategory.CAMERA,
            description = "Kamera verwenden",
          )
        ),
      versionName = "2.4.1",
    )

  private val systemApp =
    InstalledAppPermissionInfo(
      packageName = "com.android.systemui",
      appName = "System-UI",
      isSystemApp = true,
      totalRequestedPermissions = 18,
      grantedDangerousPermissions =
        listOf(
          GrantedDangerousPermission(
            fullName = "android.permission.ACCESS_FINE_LOCATION",
            shortName = "ACCESS_FINE_LOCATION",
            category = SensitivityCategory.LOCATION,
            description = "Standort",
          )
        ),
      versionName = "15",
    )

  private val standardSystemApp =
    InstalledAppPermissionInfo(
      packageName = "com.google.android.webview",
      appName = "Android System WebView",
      isSystemApp = true,
      totalRequestedPermissions = 2,
      grantedDangerousPermissions = emptyList(),
      versionName = null,
    )

  private val scanResult =
    InstalledAppsScanResult(
      scannedAtMillis = 1_700_000_000_000L,
      visiblePackageCount = 3,
      apps = listOf(systemApp, userApp, standardSystemApp),
    )

  @Test
  fun `summarize counts user and system apps separately and together`() {
    val summaryAll = InstalledAppsExporter.summarize(scanResult, includeSystemApps = true)
    assertEquals(3, summaryAll.totalVisibleApps)
    assertEquals(1, summaryAll.userAppsCount)
    assertEquals(2, summaryAll.systemAppsCount)
    assertEquals(3, summaryAll.exportAppsCount)

    val summaryUserOnly = InstalledAppsExporter.summarize(scanResult, includeSystemApps = false)
    assertEquals(3, summaryUserOnly.totalVisibleApps)
    assertEquals(1, summaryUserOnly.userAppsCount)
    assertEquals(2, summaryUserOnly.systemAppsCount)
    assertEquals(1, summaryUserOnly.exportAppsCount)
  }

  @Test
  fun `exportAsCsv includes both user and system apps and escapes commas and quotes`() {
    val csv = InstalledAppsExporter.exportAsCsv(scanResult, includeSystemApps = true)
    val lines = csv.trimEnd().lines()

    assertEquals(4, lines.size)
    assertEquals(
      "app_name,package_name,app_type,version_name,total_requested_permissions,granted_dangerous_count,critical_sensors,granted_dangerous_permissions",
      lines[0],
    )
    // User app comes before system apps, sorted alphabetically inside each group
    assertTrue(lines[1].startsWith("\"Foto, Notiz \"\"Pro\"\"\",com.example.usercam,Nutzer-App,2.4.1,5,1,CAMERA,android.permission.CAMERA"))
    assertTrue(lines[2].contains("com.google.android.webview,System-App,,2,0"))
    assertTrue(lines[3].contains("com.android.systemui,System-App,15,18,1,LOCATION"))
  }

  @Test
  fun `exportAsCsv excludes system apps when includeSystemApps is false`() {
    val csv = InstalledAppsExporter.exportAsCsv(scanResult, includeSystemApps = false)
    val lines = csv.trimEnd().lines()

    assertEquals(2, lines.size)
    assertFalse(csv.contains("com.android.systemui"))
    assertFalse(csv.contains("com.google.android.webview"))
  }

  @Test
  fun `exportAsJson produces valid json containing system and user apps metadata`() {
    val jsonString =
      InstalledAppsExporter.exportAsJson(
        scanResult = scanResult,
        includeSystemApps = true,
        exportedAtMillis = 1_700_000_123_000L,
      )
    val root = JSONObject(jsonString)

    assertEquals(1, root.getInt("schemaVersion"))
    assertTrue(root.getBoolean("includeSystemApps"))
    assertEquals(3, root.getInt("exportedCount"))
    assertEquals(1, root.getInt("userAppsCount"))
    assertEquals(2, root.getInt("systemAppsCount"))

    val appsArray = root.getJSONArray("apps")
    assertEquals(3, appsArray.length())

    val first = appsArray.getJSONObject(0)
    assertEquals("com.example.usercam", first.getString("packageName"))
    assertEquals("user", first.getString("appType"))
    assertFalse(first.getBoolean("isSystemApp"))
    assertEquals("2.4.1", first.getString("versionName"))

    val last = appsArray.getJSONObject(2)
    assertEquals("com.android.systemui", last.getString("packageName"))
    assertEquals("system", last.getString("appType"))
    assertTrue(last.getBoolean("isSystemApp"))
  }

  @Test
  fun `suggestedFileName formats deterministic utc stamp and extension`() {
    val csvName = InstalledAppsExporter.suggestedFileName(AppExportFormat.CSV, 0L)
    val jsonName = InstalledAppsExporter.suggestedFileName(AppExportFormat.JSON, 0L)
    assertEquals("devicecare-apps-19700101-0000.csv", csvName)
    assertEquals("devicecare-apps-19700101-0000.json", jsonName)
  }

  @Test
  fun `writeTextToUri writes UTF-8 content to file destination`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<android.content.Context>()
      val tempFile = File(context.cacheDir, "installed-apps-export-test.csv")
      val uri = Uri.fromFile(tempFile)
      val csv = InstalledAppsExporter.exportAsCsv(scanResult, includeSystemApps = true)

      val result = InstalledAppsExporter.writeTextToUri(context, uri, csv)

      assertTrue(result.isSuccess)
      assertEquals(csv, tempFile.readText(Charsets.UTF_8))
      tempFile.delete()
    }
  }

  @Test
  fun `writeTempExportFile cleans previous exports and buildShareFileIntent attaches FileProvider uri`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<android.content.Context>()
      val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val staleFile = File(exportDir, "stale-old-export.csv").apply { writeText("old") }
      assertTrue(staleFile.exists())

      val fileResult =
        InstalledAppsExporter.writeTempExportFile(
          context = context,
          scanResult = scanResult,
          format = AppExportFormat.JSON,
          includeSystemApps = true,
          timestampMillis = 0L,
        )

      assertTrue(fileResult.isSuccess)
      val file = fileResult.getOrThrow()
      assertTrue(file.exists())
      assertEquals("devicecare-apps-19700101-0000.json", file.name)
      assertFalse(staleFile.exists())

      val intent =
        InstalledAppsExporter.buildShareFileIntent(
          context = context,
          file = file,
          format = AppExportFormat.JSON,
          exportedCount = 3,
          includeSystemApps = true,
        )

      assertEquals(Intent.ACTION_SEND, intent.action)
      assertEquals("application/json", intent.type)
      assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
      @Suppress("DEPRECATION")
      val streamUri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
          intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
      assertEquals("content", streamUri?.scheme)
      assertTrue(streamUri?.authority?.endsWith(".fileprovider") == true)

      file.delete()
    }
  }
}
