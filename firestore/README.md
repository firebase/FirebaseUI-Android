# FirebaseUI for Cloud Firestore

FirebaseUI makes it simple to bind data from Cloud Firestore to your app's UI.

Before using this library, you should be familiar with the following topics:

* [Structuring and querying data in Cloud Firestore][firestore-docs].
* [Displaying lists of data using a RecyclerView][recyclerview].

## Table of contents

1. [Data model](#data-model)
1. [Querying](#querying)
1. [Populating a RecyclerView](#using-firebaseui-to-populate-a-recyclerview)
   1. [Choosing an adapter](#choosing-an-adapter)
   1. [Using the FirestoreRecyclerAdapter](#using-the-firestorerecycleradapter)
       1. [Adapter lifecyle](#firestorerecycleradapter-lifecycle)
       1. [Events](#data-and-error-events)
   1. [Using the FirestorePagingAdapter](#using-the-firestorepagingadapter)
       1. [Adapter lifecyle](#firestorepagingadapter-lifecycle)
       1. [Events](#paging-events)

## Data model

Imagine you have a chat app where each chat message is a document in the `chats` collection
of your database. In your app, you may represent a chat message like this:

```java
public class Chat {
    private String mName;
    private String mMessage;
    private String mUid;
    private Date mTimestamp;

    public Chat() { } // Needed for Firebase

    public Chat(String name, String message, String uid) {
        mName = name;
        mMessage = message;
        mUid = uid;
    }

    public String getName() { return mName; }

    public void setName(String name) { mName = name; }

    public String getMessage() { return mMessage; }

    public void setMessage(String message) { mMessage = message; }

    public String getUid() { return mUid; }

    public void setUid(String uid) { mUid = uid; }

    @ServerTimestamp
    public Date getTimestamp() { return mTimestamp; }

    public void setTimestamp(Date timestamp) { mTimestamp = timestamp; }
}
```

A few things to note about this model class:

* The getters and setters follow the JavaBean naming pattern which allows Firestore to map
  the data to field names (ex: `getName()` provides the `name` field).
* The class has an empty constructor, which is required for Firestore's automatic data mapping.

For a properly constructed model class like the `Chat` class above, Firestore can perform automatic
serialization in `DocumentReference#set()` and automatic deserialization in
`DocumentSnapshot#toObject()`. For more information on data mapping in Firestore, see the
documentation on [custom objects][firestore-custom-objects].

## Querying

On the main screen of your app, you may want to show the 50 most recent chat messages.
In Firestore, you would use the following query:

```java
Query query = FirebaseFirestore.getInstance()
        .collection("chats")
        .orderBy("timestamp")
        .limit(50);
```

To retrieve this data without FirebaseUI, you might use `addSnapshotListener` to listen for
live query updates:

```java
query.addSnapshotListener(new EventListener<QuerySnapshot>() {
    @Override
    public void onEvent(@Nullable QuerySnapshot snapshot,
                        @Nullable FirebaseFirestoreException e) {
        if (e != null) {
            // Handle error
            //...
            return;
        }

        // Convert query snapshot to a list of chats
        List<Chat> chats = snapshot.toObjects(Chat.class);

        // Update UI
        // ...
    }
});
```

## Using FirebaseUI to populate a `RecyclerView`

If you're displaying a list of data, you likely want to bind the `Chat` objects to a
`RecyclerView`. This means implementing a custom `RecyclerView.Adapter` and coordinating
updates with the `EventListener` on the `Query`.

Fear not, FirebaseUI does all of this for you automatically!


### Choosing an adapter

FirebaseUI offers two types of RecyclerView adapters for Cloud Firestore:

  * `FirestoreRecyclerAdapter` — binds a `Query` to a `RecyclerView` and responds to all real-time
    events included items being added, removed, moved, or changed. Best used with small result sets
    since all results are loaded at once.
  * `FirestorePagingAdapter` — binds a `Query` to a `RecyclerView` by loading data in pages. Best
    used with large, static data sets. Real-time events are not respected by this adapter, so it
    will not detect new/removed items or changes to items already loaded.

### Using the `FirestoreRecyclerAdapter`

The `FirestoreRecyclerAdapter` binds a `Query` to a `RecyclerView`. When documents are added,
removed, or change these updates are automatically applied to your UI in real time.

First, configure the adapter by building `FirestoreRecyclerOptions`. In this case we will continue
with our chat example:

```java
// Configure recycler adapter options:
//  * query is the Query object defined above.
//  * Chat.class instructs the adapter to convert each DocumentSnapshot to a Chat object
FirestoreRecyclerOptions<Chat> options = new FirestoreRecyclerOptions.Builder<Chat>()
        .setQuery(query, Chat.class)
        .build();
```

If you need to customize how your model class is parsed, you can use a custom `SnapshotParser`:

```java
...setQuery(..., new SnapshotParser<Chat>() {
    @NonNull
    @Override
    public Chat parseSnapshot(@NonNull DocumentSnapshot snapshot) {
        return ...;
    }
});
```

Next create the `FirestoreRecyclerAdapter` object. You should already have a `ViewHolder` subclass
for displaying each item. In this case we will use a custom `ChatHolder` class:

```java
FirestoreRecyclerAdapter adapter = new FirestoreRecyclerAdapter<Chat, ChatHolder>(options) {
    @Override
    public void onBindViewHolder(ChatHolder holder, int position, Chat model) {
        // Bind the Chat object to the ChatHolder
        // ...
    }

    @Override
    public ChatHolder onCreateViewHolder(ViewGroup group, int i) {
        // Create a new instance of the ViewHolder, in this case we are using a custom
        // layout called R.layout.message for each item
        View view = LayoutInflater.from(group.getContext())
                .inflate(R.layout.message, group, false);

        return new ChatHolder(view);
    }
};
```

Finally attach the adapter to your `RecyclerView` with the `RecyclerView#setAdapter()` method.
Don't forget to also set a `LayoutManager`!

#### `FirestoreRecyclerAdapter` lifecycle

##### Start/stop listening

The `FirestoreRecyclerAdapter` uses a snapshot listener to monitor changes to the Firestore query.
To begin listening for data, call the `startListening()` method. You may want to call this
in your `onStart()` method. Make sure you have finished any authentication necessary to read the
data before calling `startListening()` or your query will fail.

```java
@Override
protected void onStart() {
    super.onStart();
    adapter.startListening();
}
```

Similarly, the `stopListening()` call removes the snapshot listener and all data in the adapter.
Call this method when the containing Activity or Fragment stops:

```java
@Override
protected void onStop() {
    super.onStop();
    adapter.stopListening();
}
```

##### Automatic listening

If you don't want to manually start/stop listening you can use
[Android Architecture Components][arch-components] to automatically manage the lifecycle of the
`FirestoreRecyclerAdapter`. Pass a `LifecycleOwner` to
`FirestoreRecyclerOptions.Builder#setLifecycleOwner(...)` and FirebaseUI will automatically
start and stop listening in `onStart()` and `onStop()`.

#### Data and error events

When using the `FirestoreRecyclerAdapter` you may want to perform some action every time data
changes or when there is an error. To do this, override the `onDataChanged()` and `onError()`
methods of the adapter:

```java
FirestoreRecyclerAdapter adapter = new FirestoreRecyclerAdapter<Chat, ChatHolder>(options) {
    // ...

    @Override
    public void onDataChanged() {
        // Called each time there is a new query snapshot. You may want to use this method
        // to hide a loading spinner or check for the "no documents" state and update your UI.
        // ...
    }

    @Override
    public void onError(FirebaseFirestoreException e) {
        // Called when there is an error getting a query snapshot. You may want to update
        // your UI to display an error message to the user.
        // ...
    }
};
```


### Using the `FirestorePagingAdapter`

The `FirestorePagingAdapter` binds a `Query` to a `RecyclerView` by loading documents in pages.
This results in a time and memory efficient binding, however it gives up the real-time events
afforted by the `FirestoreRecyclerAdapter`.

The `FirestorePagingAdapter` is built on top of the [Android Paging Support Library][paging-support].
Before using the adapter in your application, you must add a dependency on the support library:

```groovy
implementation 'android.arch.paging:runtime:1.x.x'
```

First, configure the adapter by building `FirestorePagingOptions`. Since the paging adapter
is not appropriate for a chat application (it would not detect new messages), we will consider
an adapter that loads a generic `Item`:

```java
// The "base query" is a query with no startAt/endAt/limit clauses that the adapter can use
// to form smaller queries for each page.  It should only include where() and orderBy() clauses
Query baseQuery = mItemsCollection.orderBy("value", Query.Direction.ASCENDING);

// This configuration comes from the Paging Support Library
// https://developer.android.com/reference/android/arch/paging/PagedList.Config.html
PagedList.Config config = new PagedList.Config.Builder()
        .setEnablePlaceholders(false)
        .setPrefetchDistance(10)
        .setPageSize(20)
        .build();

// The options for the adapter combine the paging configuration with query information
// and application-specific options for lifecycle, etc.
FirestorePagingOptions<Item> options = new FirestorePagingOptions.Builder<Item>()
        .setLifecycleOwner(this)
        .setQuery(baseQuery, config, Item.class)
        .build();
```

If you need to customize how your model class is parsed, you can use a custom `SnapshotParser`:

```java
...setQuery(..., new SnapshotParser<Item>() {
    @NonNull
    @Override
    public Item parseSnapshot(@NonNull DocumentSnapshot snapshot) {
        return ...;
    }
});
```

Next, create the `FirestorePagingAdapter` object. You should already have a `ViewHolder` subclass
for displaying each item. In this case we will use a custom `ItemViewHolder` class:

```java
FirestorePagingAdapter<Item, ItemViewHolder> adapter =
        new FirestorePagingAdapter<Item, ItemViewHolder>(options) {
            @NonNull
            @Override
            public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Create the ItemViewHolder
                // ...
            }

            @Override
            protected void onBindViewHolder(@NonNull ItemViewHolder holder,
                                            int position,
                                            @NonNull Item model) {
                // Bind the item to the view holder
                // ...
            }
        };
```

Finally attach the adapter to your `RecyclerView` with the `RecyclerView#setAdapter()` method.
Don't forget to also set a `LayoutManager`!

#### `FirestorePagingAdapter` lifecycle

##### Start/stop listening

The `FirestorePagingAdapter` listens for scrolling events and loads additional pages from the
database only when needed.

To begin populating data, call the `startListening()` method. You may want to call this
in your `onStart()` method. Make sure you have finished any authentication necessary to read the
data before calling `startListening()` or your query will fail.

```java
@Override
protected void onStart() {
    super.onStart();
    adapter.startListening();
}
```

Similarly, the `stopListening()` call freezes the data in the `RecyclerView` and prevents any future
loading of data pages.

Call this method when the containing Activity or Fragment stops:

```java
@Override
protected void onStop() {
    super.onStop();
    adapter.stopListening();
}
```

##### Automatic listening

If you don't want to manually start/stop listening you can use
[Android Architecture Components][arch-components] to automatically manage the lifecycle of the
`FirestorePagingAdapter`. Pass a `LifecycleOwner` to
`FirestorePagingOptions.Builder#setLifecycleOwner(...)` and FirebaseUI will automatically
start and stop listening in `onStart()` and `onStop()`.

#### Paging events

When using the `FirestorePagingAdapter`, you may want to perform some action every time data
changes or when there is an error. To do this, override the `onLoadingStateChanged()`
method of the adapter:

```java
FirestorePagingAdapter<Item, ItemViewHolder> adapter =
        new FirestorePagingAdapter<Item, ItemViewHolder>(options) {

            // ...

            @Override
            protected void onLoadingStateChanged(@NonNull LoadingState state) {
                switch (state) {
                    case LOADING_INITIAL:
                        // The initial load has begun
                        // ...
                    case LOADING_MORE:
                        // The adapter has started to load an additional page
                        // ...
                    case LOADED:
                        // The previous load (either initial or additional) completed
                        // ...
                    case ERROR:
                        // The previous load (either initial or additional) failed. Call
                        // the retry() method in order to retry the load operation.
                        // ...
                }
            }
        };
```

[firestore-docs]: https://firebase.google.com/docs/firestore/
[firestore-custom-objects]: https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects
[recyclerview]: https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html
[arch-components]: https://developer.android.com/topic/libraries/architecture/index.html
[paging-support]: https://developer.android.com/topic/libraries/architecture/paging.html
