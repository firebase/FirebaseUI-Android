# Upgrading to FirebaseUI 5.0

FirebaseUI version `5.0.0` has no breaking API changes from version `4.3.2` but updates some
critical dependencies to new major versions.

In order to use FirebaseUI, make sure your application does not declare any of the following
dependencies at a lower version:

```
com.google.firebase:firebase-core:16.0.9
com.google.firebase:firebase-auth:17.0.0
com.google.firebase:firebase-firestore:19.0.0
com.google.firebase:firebase-database:17.0.0
com.google.firebase:firebase-storage:17.0.0
```

All of the underlying breaking changes were part of the May 7th Firebase SDK release. You can read
more about this release here:
https://firebase.google.com/support/release-notes/android#update_-_may_07_2019
