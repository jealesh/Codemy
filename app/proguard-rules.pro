# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep Kotlin companion objects
-keepclassmembers class ** {
    public static ** Companion;
}

# Keep data class fields for serialization
-keepclassmembers class com.inc.codemy.models.** {
    *;
}

# Keep Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface * {
    *;
}

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep your data model classes for kotlinx.serialization
-keep,allowobfuscation,allowshrinking class com.inc.codemy.models.** { *; }

# Keep WebView interface for Pyodide
-keepclassmembers class com.inc.codemy.ProgrammingFragment {
    *;
}
