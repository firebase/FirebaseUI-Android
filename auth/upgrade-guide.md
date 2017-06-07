# Removals

- `AuthUI#signOut(Activity)` and `AuthUI#delete(Activity)` -> `AuthUI#signOut(AppCompatActivity)` and `AuthUI#delete(AppCompatActivity)`
- `AuthUI.SignInIntentBuilder#setProviders(String...)` -> `AuthUI.SignInIntentBuilder#setProviders(List<IdpConfig>)`
- `com.firebase.ui.auth.ui.ResultCodes` -> Check for `Activity#RESULT_CANCELLED` and use `IdpResponse#getErrorCode()`
