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

-allowaccessmodification
-repackageclasses
