# Recommended flags for Firebase Auth
-keepattributes Signature
-keepattributes *Annotation*

# Don't warn about retrofit or okio classes
-dontwarn okio.**
-dontnote retrofit2.Platform
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
-dontwarn retrofit2.Platform$Java8
