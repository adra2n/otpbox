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

# 友盟 U-App 统计 SDK：防止被混淆导致 SDK 不可用
-keep class com.umeng.** { *; }
-keep class org.repackage.** { *; }
-keep class com.uyumao.** { *; }
-keepclassmembers class * {
    public <init>(org.json.JSONObject);
}
-keep class com.otpbox.R$* { *; }
-keep public class com.otpbox.R { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}
