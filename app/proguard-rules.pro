# Protectstar iShredder Excellent - Stable Release R8/ProGuard Rules

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve BuildConfig metadata
-keep class com.example.BuildConfig { *; }

# Preserve JobScheduler background service
-keep class com.example.engine.AutoShredJobService { *; }

# Preserve Room database & entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
