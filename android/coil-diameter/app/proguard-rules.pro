# ProGuard rules for Precnik

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Compose rules
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
