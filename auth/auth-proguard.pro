# Twitter and Facebook are optional
-dontwarn com.twitter.**
-dontwarn com.facebook.**

# Recommended flags for Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*

# Don't warn about retrofit or okio classes
-dontwarn okio.**
-dontwarn retrofit2.Call
-dontnote retrofit2.Platform
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
-dontwarn retrofit2.Platform$Java8
