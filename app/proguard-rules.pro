-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# https://issuetracker.google.com/347963245
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# Ktor logger
-dontwarn org.slf4j.impl.StaticLoggerBinder

-dontwarn org.conscrypt.Conscrypt

# https://issuetracker.google.com/222232895
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.Sidecar*

-keepattributes LineNumberTable

-allowaccessmodification
-repackageclasses
