# ProGuard rules for Livnica

# Keep the application class
-keep class com.livnica.** { *; }

# Keep data classes used for JSON serialization
-keepclassmembers class com.livnica.DayRecord { *; }
-keepclassmembers class com.livnica.MonthSummary { *; }
-keepclassmembers class com.livnica.BatchRecord { *; }
-keepclassmembers class com.livnica.Shift { *; }
-keepclassmembers class com.livnica.BrigadeType { *; }
-keepclassmembers class com.livnica.OvertimeType { *; }
-keepclassmembers class com.livnica.PayConfig { *; }
-keepclassmembers class com.livnica.AppConfig { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Compose rules
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# JSON parsing with org.json
-keep class org.json.** { *; }
