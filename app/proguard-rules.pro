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

# Compose multiplatform resources
-dontwarn androidx.test.platform.app.InstrumentationRegistry

# Ktor logger
-dontwarn org.slf4j.impl.StaticLoggerBinder

# https://issuetracker.google.com/222232895
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.Sidecar*

-keepattributes LineNumberTable

-allowaccessmodification
-repackageclasses
