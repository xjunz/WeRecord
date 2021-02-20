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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class net.sqlcipher.**{*;}
-keep interface net.sqlcipher.**{*;}
#Caused by: java.lang.RuntimeException: Field keySize_ for b.c.b.a.g0.u not found. Known fields are [public int b.c.b.a.g0.u.f, public static final b.c.b.a.g0.u b.c.b.a.g0.u.g, public static volatile b.c.c.y0 b.c.b.a.g0.u.h]
-keep class com.google.crypto.**{ *; }
#FilterFragment.onPrepareFilter()
-keepclassmembers class androidx.appcompat.widget.AppCompatSpinner{private <fields>;}
