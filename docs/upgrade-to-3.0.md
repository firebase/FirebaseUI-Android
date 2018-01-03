# Upgrading to FirebaseUI 3.0

This document outlines the key _breaking_ changes made between versions `2.3.0` and `3.0.0` as well
as suggested upgrade paths. This is not a comprehensive list of all features or bug fixes, please
reference the [release notes][release-notes] for that information.

For a full, machine-generated compatibility report see the `docs/compat_reports` folder.

## Auth

No breaking changes

## Realtime Database

* **Adapter initialization** - in previous versions the adapter classes (`FirebaseRecyclerAdapter`,
 `FirebaseListAdapter`, etc) had multiple constructor overloads. In `3.x`, each adapter has
 a single constructor that takes an `Options` object like `FirebaseRecyclerOptions`. These
 options objects can be constructed via their respective builders. For more information,
 see `database/README.md`.
* **View logic** - adapter classes no longer automatically create `View` or `ViewHolder` objects
 as required by their parent adapter classes. These methods are left to the developer, see the
 sample app for how to implement `onCreateViewHolder()` and other required methods.
* **Adapter lifecycle** - in previous versions the adapters began listening immediately upon
 instantiation and had a `cleanup()` call to stop listening. In `3.x` you must explicitly call
 `startListening()` and `stopListening()` or pass a `LifecycleOwner` to the options builder.
* **Errors and event types** - errors in the adapters are now exposed via the `onError()` method.
 Data changes are exposed through `onChildChanged()` and `onDataChanged()`.
* **Indexed adapters** - the indexed adapters have been removed. To use indexed data, use the
 `setIndexedQuery()` method when building adapter options.
* **Observable snapshot arrays** - `ObservableSnapshotArray<T>` and related classes previously
 implemented `List<DataSnapshot>`. They now implement `List<T>` for simpler iteration over
 model objects.

## Cloud Firestore

New module `firebase-ui-firestore`. See `firestore/README.md` for more information.

## Cloud Storage

* **Glide 4.0** - the underlying Glide dependency was upgraded to version `4.1.x`. This
 new version of Glide changes how custom loaders (like `FirebaseImageLoader`) are added.
 Rather than passing a `FirebaseImageLoader` to each `Glide` invocation, you must create a
 Glide app module and register the loader there. For more information see `storage/README.md`.
 For comprehensive documentation on upgrading your app to Glide `4.x` see the
 [official migration guide][glide-migration].

[glide-migration]: http://bumptech.github.io/glide/doc/migrating.html
[release-notes]: https://github.com/firebase/FirebaseUI-Android/releases/tag/3.0.0
