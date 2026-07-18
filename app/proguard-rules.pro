# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.otpbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 友盟 Android 统计 SDK 混淆保留
-keep class com.umeng.** { *; }
-keep class org.repackage.** { *; }
-keep class com.uyumao.** { *; }
-keepclassmembers class * {
    public <init>(org.json.JSONObject);
}
