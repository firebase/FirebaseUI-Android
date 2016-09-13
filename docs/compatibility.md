# Compatibility with Firebase / Google Play Services

FirebaseUI libraries have the following transitive dependencies on the Firebase SDK:
```
firebase-ui-auth
|--- com.google.firebase:firebase-auth
|--- com.google.android.gms:play-services-auth

firebase-ui-database
|--- com.google.firebase:firebase-database
```

Each version of FirebaseUI has dependency on a fixed version of these libraries, defined as the variable `firebase_version`
in `common/constants.gradle`.  If you are using any dependencies in your app of the form
`compile 'com.google.firebase:firebase-*:x.y.z'` or `compile 'com.google.android.gms:play-services-*:x.y.z'`
you need to make sure that you use the same version that your chosen version of FirebaseUI requires.

For convenience, here are some examples:

| FirebaseUI Version | Firebase/Play Services Version |
|--------------------|--------------------------------|
| 0.5.3              | 9.4.0                          |
| 0.4.4              | 9.4.0                          |
| 0.4.3              | 9.2.1                          |
| 0.4.2              | 9.2.0                          |
| 0.4.1              | 9.0.2                          |
| 0.4.0              | 9.0.0                          |
