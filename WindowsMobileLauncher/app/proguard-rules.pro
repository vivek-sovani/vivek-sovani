# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep launcher classes
-keep class com.windowsmobile.launcher.** { *; }

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep serialized data classes
-keepclassmembers class com.windowsmobile.launcher.AppTile {
    *;
}
