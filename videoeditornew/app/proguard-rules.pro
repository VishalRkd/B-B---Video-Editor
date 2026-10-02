# Keep QuVideo / VivaCut JNI engine classes
-keep class xiaoying.** { *; }
-keep interface xiaoying.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
