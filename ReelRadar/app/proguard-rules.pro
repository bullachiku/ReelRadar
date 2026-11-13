# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase & Coroutines
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class kotlinx.coroutines.** { *; }

# Glance
-keep class androidx.glance.** { *; }
