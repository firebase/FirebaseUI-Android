# Twitter and Facebook are optional
-dontwarn com.facebook.**
-dontwarn com.twitter.**

# Don't note a bunch of dynamically referenced classes
-dontnote com.google.**
-dontnote com.facebook.**
-dontnote com.twitter.**
-dontnote com.squareup.okhttp.**
-dontnote okhttp3.internal.**

# Recommended flags for Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit config
-dontnote retrofit2.Platform
-dontwarn retrofit2.** # Also keeps Twitter at bay as long as they keep using Retrofit
-dontwarn okio.**
-keepattributes Exceptions
