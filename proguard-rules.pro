-keepattributes LineNumberTable,SourceFile
-keepnames class me.aap.utils.** { *; }

-keep class com.jcraft.jsch.** { *; }

-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class * { @com.google.api.client.util.Key <fields>; }

-assumenosideeffects class me.aap.utils.log.Log {
    public static void d(...);
}
