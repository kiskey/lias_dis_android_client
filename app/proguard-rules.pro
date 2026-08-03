# ====================================================================
# File: app/proguard-rules.pro
# Version: 1.0.0
# Purpose: R8 optimization rules. Ensures kotlinx.serialization,
#          OkHttp, and Compose internals survive minification and
#          resource shrinking for a zero-waste release APK.
# ====================================================================

# --- General Optimization ---
-dontwarn org.slf4j.impl.**
-dontwarn org.slf4j.LoggerFactory

# --- Kotlin Coroutines ---
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>(android.os.Handler);
}

# --- OkHttp & Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.PublicClassAPI
-keepnames class okio.internal.PublicClassAPI

# --- Kotlinx Serialization ---
# Keep the `Companion` field of serializable objects (used by serializer() extension).
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
# Keep `serializer()` functions on companion objects of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep generated `*$$serializer` classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$$serializer {
    *** INSTANCE;
}
-keep,includedescriptorclasses class **$$serializer { *; }

# --- Keep Application Data Models ---
-keep class com.lias.remote.core.models.** { *; }
