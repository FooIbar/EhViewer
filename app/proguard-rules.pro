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

# Workaround for https://issuetracker.google.com/331556916
-keepclassmembers class androidx.biometric.BiometricManager {
    public int canAuthenticate(int);
}

# Ktor logger
-dontwarn org.slf4j.impl.StaticLoggerBinder

-dontwarn org.conscrypt.Conscrypt

-keepattributes LineNumberTable

-allowaccessmodification
-repackageclasses
