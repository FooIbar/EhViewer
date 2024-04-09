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

# Ktor logger
-dontwarn org.slf4j.impl.StaticLoggerBinder

-dontwarn org.conscrypt.Conscrypt

-keepattributes LineNumberTable

-allowaccessmodification
-repackageclasses
