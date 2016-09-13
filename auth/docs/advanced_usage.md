# Advanced Usage of FirebaseUI for Authentication

For simple usage of FirebaseUI Auth, see the [basic usage](./basic_usage.md) docs.

## OAuth Scope Customization

### Google
By default, FirebaseUI requests the `email` and `profile` scopes when using Google Sign In. If you
would like to request additional scopes from the user, add a string array resource named
`google_permissions` to your `strings.xml` file like this:

```xml
<!--
    For a list of all scopes, see:
    https://developers.google.com/identity/protocols/googlescopes
-->
<string-array name="google_permissions">
    <!-- Request permission to read the user's Google Drive files -->
    <item>https://www.googleapis.com/auth/drive.readonly</item>
</string-array>
```


### Facebook

By default, FirebaseUI requests the `email` and `public_profile` permissions when initiating
Facebook Login.  If you would like to override these scopes, a string array resource named
`facebook_permissions` to your `strings.xml` file like this:

```xml
<!--
    See:
    https://developers.facebook.com/docs/facebook-login/android
    https://developers.facebook.com/docs/facebook-login/permissions
-->
<string-array name="facebook_permissions">
    <!-- Request permission to know the user's birthday -->
    <item>user_birthday</item>
</string-array>
```

# UI customization

To provide customization of the visual style of the activities that implement
the flow, a new theme can be declared. Standard material design color
and typography properties will take effect as expected. For example, to define
a green theme:

```xml
<style name="GreenTheme" parent="FirebaseUI">
    <item name="colorPrimary">@color/material_green_500</item>
    <item name="colorPrimaryDark">@color/material_green_700</item>
    <item name="colorAccent">@color/material_purple_a700</item>
    <item name="colorControlNormal">@color/material_green_500</item>
    <item name="colorControlActivated">@color/material_lime_a700</item>
    <item name="colorControlHighlight">@color/material_green_a200</item>
    <item name="android:windowBackground">@color/material_green_50</item>
</style>
```

With associated colors:

```xml
<color name="material_green_50">#E8F5E9</color>
<color name="material_green_500">#4CAF50</color>
<color name="material_green_700">#388E3C</color>
<color name="material_green_a200">#69F0AE</color>
<color name="material_lime_a700">#AEEA00</color>
<color name="material_purple_a700">#AA00FF</color>
```

This would then be used in the construction of the sign-in intent:

```java
startActivityForResult(
    AuthUI.getInstance(this).createSignInIntentBuilder()
        // ...
        .setTheme(R.style.GreenTheme)
        .build());
```

Your application theme could also simply be used, rather than defining a new
one.

If you wish to change the string messages, the existing strings can be
easily overridden by name in your application. See
[the built-in strings.xml](src/main/res/values/strings.xml) and simply
redefine a string to change it, for example:

```java
<resources>
  <!-- was "Signing up..." -->
  <string name="progress_dialog_signing_up">Creating your shiny new account...</string>
</resources>
```
