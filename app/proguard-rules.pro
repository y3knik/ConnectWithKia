-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepclasseswithmembers,allowshrinking,allowobfuscation class **$$serializer { *; }
-keep,includedescriptorclasses class com.github.y3knik.connectwithkia.**$$serializer { *; }
-keepclasseswithmembers class com.github.y3knik.connectwithkia.** {
    kotlinx.serialization.KSerializer serializer(...);
}
