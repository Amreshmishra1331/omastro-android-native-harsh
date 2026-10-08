# Proguard rules for OmAstro AI Native
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.omastro.app.data.model.** { *; }

# Razorpay
-keep class com.razorpay.** {*;}
-dontwarn com.razorpay.**

# Coil
-dontwarn coil.**
