-keepattributes LineNumberTable,SourceFile
-keepnames class me.aap.utils.** { *; }

-keep class com.jcraft.jsch.** { *; }

-assumenosideeffects class me.aap.utils.log.Log {
    public static int d(...);
}
