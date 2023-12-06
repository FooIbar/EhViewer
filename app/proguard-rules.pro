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

# https://issuetracker.google.com/issues/307323842
-keepclassmembers class androidx.compose.ui.platform.WindowInfoImpl {
    setWindowFocused(boolean);
}

# Ktor logger
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Apache5 Http Client
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid

-allowaccessmodification
-repackageclasses
