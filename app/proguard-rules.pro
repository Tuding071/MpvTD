# MPV library
-keep class is.xyz.mpv.** { *; }
-keep interface is.xyz.mpv.** { *; }

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# App classes
-keep class com.mpvtd.** { *; }
