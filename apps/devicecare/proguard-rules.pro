# AI-DeviceCare – optimierte R8-Regeln (2026)
# Philosophie: So wenig wie möglich behalten. R8 + default ProGuard-Datei
# erledigen den Großteil. Nur das Nötige explizit schützen.

# ---- Stacktraces lesbar halten ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Kotlin / Compose (minimal) ----
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn androidx.compose.**

# ---- AndroidX (nur Warnungen unterdrücken) ----
-dontwarn androidx.**

# ---- AI-DeviceCare: nur Contract-relevante Klassen ----
# Evidence-IDs und Status-Enums müssen stabil bleiben.
-keep class online.capitalai.devicecare.core.evidence.ObservationStatus { *; }
-keep class online.capitalai.devicecare.core.evidence.DeviceObservation { *; }
-keep class online.capitalai.devicecare.core.evidence.DeviceObservationBatch { *; }

# Collector-Einstiegspunkte (falls per Reflection oder Tests genutzt)
-keep class online.capitalai.devicecare.features.battery.AndroidBatteryCollector { *; }
-keep class online.capitalai.devicecare.features.permissions.AndroidPermissionCollector { *; }

# ---- Logging entfernen ----
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ---- Enums ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Companion-Objekte (Kotlin) ----
-keepclassmembers class * {
    public static ** Companion;
}

# ---- Gezielte dontwarn für genutzte, aber optionale APIs ----
-dontwarn android.os.BatteryManager
-dontwarn android.os.PowerManager
-dontwarn android.app.ActivityManager
-dontwarn org.jetbrains.**
-dontwarn java.lang.invoke.**

# ---- Optional: Crashlytics / Play Services (auskommentiert) ----
# -keepattributes *Annotation*
# -keep class com.google.firebase.crashlytics.** { *; }
# -keep class com.google.android.gms.** { *; }
