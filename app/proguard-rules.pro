# OpenRocky ProGuard Rules

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.xnu.rocky.**$$serializer { *; }
-keepclassmembers class com.xnu.rocky.** { *** Companion; }
-keepclasseswithmembers class com.xnu.rocky.** { kotlinx.serialization.KSerializer serializer(...); }
-keep @kotlinx.serialization.Serializable class com.xnu.rocky.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Chaquopy
-keep class com.chaquo.** { *; }

# Google Tink / Security Crypto - missing annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Markdown renderer
-keep class com.mikepenz.markdown.** { *; }
-dontwarn org.commonmark.**

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# Coil
-keep class coil.** { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
