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

# https://issuetracker.google.com/413078297
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Ktor logger
-dontwarn org.slf4j.impl.StaticLoggerBinder

# https://issuetracker.google.com/222232895
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.Sidecar*

-keepattributes LineNumberTable

-allowaccessmodification
-repackageclasses
