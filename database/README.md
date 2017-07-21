# FirebaseUI Database

## Using FirebaseUI to populate a [`RecyclerView`](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html)

To use the FirebaseUI to display Firebase data, we need a few things:

  1. A Java class that represents our database objects
  1. A custom recycler view adapter to map from a collection from Firebase to Android

### Creating a model class

In your app, create a class that represents the data from Firebase that you want to show in the RecyclerView.

So say we have these chat messages in our Firebase database:

![Chat messages in dashboard](../doc-images/chat-messages.png "Chat messages in console")

We can represent a chat message with this Java class:

```java
public class Chat {
    private String mName;
    private String mMessage;
    private String mUid;

    public Chat() {
        // Needed for Firebase
    }

    public Chat(String name, String uid, String message) {
        mName = name;
        mMessage = message;
        mUid = uid;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(String uid) {
        mUid = uid;
    }
}
```
A few things to note here:

 * The fields have the exact same name as the properties in Firebase. This allows Firebase to automatically map the properties to these fields.
 * There is a default (parameter-less constructor) that is necessary for Firebase to be able to create a new instance of this class.
 * There is a convenience constructor that takes the member fields, so that we easily create a fully initialized `Chat` in our app
 * the `getText`, `getUid`, and `getName` methods are so-called getters and follow a JavaBean pattern

A little-known feature of Firebase for Android is that you can pass an instance of this `Chat` class to `setValue()`:

```java
DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
Chat msg = new Chat("puf", "1234", "Hello FirebaseUI world!");
ref.push().setValue(msg);
```
The Firebase Android client will read the values from the `msg` and write them into the properties of the new child in the database.

Conversely, we can read a `Chat` straight from a `DataSnapshot` in our event handlers:

```java
ref.limitToLast(5).addValueEventListener(new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot snapshot) {
        for (DataSnapshot msgSnapshot: snapshot.getChildren()) {
            Chat msg = msgSnapshot.getValue(Chat.class);
            Log.i("Chat", chat.getName()+": "+chat.getText());
        }
    }
    @Override
    public void onCancelled(DatabaseError error) {
        Log.e("Chat", "The read failed: " + error.getText());
    }
});
```
In the above snippet we have a query for the last 5 chat messages. Whenever those change (i.e. when an new message is added)
we get the `Chat` objects from the `DataSnapshot` with `getValue(Chat.class)`. The Firebase Android client will
then read the properties that it got from the database and map them to the fields of our `Chat` class.

But when we build our app using FirebaseUI, we often won't need to register our own EventListener. The
`FirebaseRecyclerAdapter` takes care of that for us.

### Find the RecyclerView

We'll assume you've already added a `RecyclerView` to your layout and have looked it up in the `onCreate` method of your activity:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    RecyclerView messages = (RecyclerView) findViewById(R.id.messages);
    messages.setLayoutManager(new LinearLayoutManager(this));
}
```

There's nothing magical going on here; we're just mapping numeric IDs and casts into a nice, type-safe contract.

### Connect to Firebase

First we'll set up a reference to the database of chat messages:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    RecyclerView messages = (RecyclerView) findViewById(R.id.messages);
    messages.setLayoutManager(new LinearLayoutManager(this));

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
}
```

### Create a custom ViewHolder

A ViewHolder is similar to container of a ViewGroup that allows simple lookup of the sub-views of the group.
If we use the same layout as before (`android.R.layout.two_line_list_item`), there are two `TextView`s in there.
We can wrap that in a ViewHolder with:

```java
public class ChatHolder extends RecyclerView.ViewHolder {
    private final TextView mNameField;
    private final TextView mMessageField;

    public ChatHolder(View itemView) {
        super(itemView);
        mNameField = (TextView) itemView.findViewById(android.R.id.text1);
        mMessageField = (TextView) itemView.findViewById(android.R.id.text2);
    }

    public void setName(String name) {
        mNameField.setText(name);
    }

    public void setMessage(String message) {
        mMessageField.setText(message);
    }
}
```

### Create custom FirebaseRecyclerAdapter subclass

Next, we need to create a subclass of the `FirebaseRecyclerAdapter` with the correct parameters
and implement its `populateViewHolder` method:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    RecyclerView messages = (RecyclerView) findViewById(R.id.messages);
    messages.setLayoutManager(new LinearLayoutManager(this));

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

    mAdapter = new FirebaseRecyclerAdapter<Chat, ChatHolder>(
            Chat.class,
            R.layout.message,
            ChatHolder.class,
            ref) {
        @Override
        public void populateViewHolder(ChatHolder holder, Chat chat, int position) {
            holder.setName(chat.getName());
            holder.setMessage(chat.getMessage());
        }
    };

    messages.setAdapter(mAdapter);
}
```

In this last snippet we create a subclass of `FirebaseRecyclerAdapter`.
We tell is that it is of type `<Chat>`, so that it is a type-safe collection. We also tell it to use
`Chat.class` when reading messages from the database. Next we say that each message will be displayed in
a `android.R.layout.two_line_list_item`, which is a built-in layout in Android that has two `TextView` elements
under each other. Then we say that the adapter belongs to `this` activity and that it needs to monitor the
data location in `ref`.

We also have to override the `populateViewHolder` method, from the `FirebaseRecyclerAdapter`. The
`FirebaseRecyclerAdapter` will call our `populateViewHolder` method for each `Chat` it finds in the database.
It passes us the `Chat` and a `View`, which is an instance of the `android.R.layout.two_line_list_item`
we specified in the constructor. So what we do in our subclass is map the fields from `chatMessage` to the
correct `TextView` controls from the `view`. The code is a bit verbose, but hey... that's Java and Android for you.

### Clean up When the Activity is Destroyed

Finally, we need to clean up after ourselves. When the activity is destroyed, we need to call `cleanup()`
on the adapter so that it can stop listening for changes in the Firebase database.

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    mAdapter.cleanup();
}
```

### Send Chat Messages

Remember when we showed how to use the `Chat` class in `setValue()`.
We can now use that in our activity to allow sending a message:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    RecyclerView messages = (RecyclerView) findViewById(R.id.messages);
    messages.setLayoutManager(new LinearLayoutManager(this));

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

    mAdapter = new FirebaseRecyclerAdapter<Chat, ChatHolder>(
            Chat.class,
            R.layout.message,
            ChatHolder.class,
            ref) {
        @Override
        public void populateViewHolder(ChatHolder holder, Chat chat, int position) {
            holder.setName(chat.getName());
            holder.setMessage(chat.getMessage());
        }
    };

    messages.setAdapter(mAdapter);

    final EditText message = (EditText) findViewById(R.id.message_text);
    findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ref.push().setValue(new Chat("puf", "1234", message.getText().toString()));
            message.setText("");
        }
    });
}

@Override
protected void onDestroy() {
    super.onDestroy();
    mAdapter.cleanup();
}
```

You're done! You now have a minimal, yet fully functional, chat app in about 30 lines of code. Not bad, right?

## Using FirebaseUI to populate a `ListView`

ListView is the older, yet simpler way to handle lists of items. Using it is analogous to
using a `FirebaseRecyclerAdapter`, but with `FirebaseListAdapter` instead and no `ViewHolder`:

```java
ListView messagesView = (ListView) findViewById(R.id.messages_list);
DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
mAdapter = new FirebaseListAdapter<Chat>(this, Chat.class, android.R.layout.two_line_list_item, ref) {
    @Override
    protected void populateView(View view, Chat chatMessage, int position) {
        ((TextView) view.findViewById(android.R.id.text1)).setText(chatMessage.getName());
        ((TextView) view.findViewById(android.R.id.text2)).setText(chatMessage.getText());

    }
};
messagesView.setAdapter(mAdapter);
```

## Using FirebaseUI with indexed data

If your data is [properly indexed](https://firebase.google.com/docs/database/android/structure-data#best_practices_for_data_structure), change your adapter initialization like so:

For a `RecyclerView`, use `FirebaseIndexRecyclerAdapter` instead of `FirebaseRecyclerAdapter`:
```java
new FirebaseIndexRecyclerAdapter<Chat, ChatHolder>(
        Chat.class,
        android.R.layout.two_line_list_item,
        ChatHolder.class,
        keyRef, // The Firebase location containing the list of keys to be found in dataRef.
        dataRef) //The Firebase location to watch for data changes. Each key key found at keyRef's location represents a list item in the RecyclerView.
```

`keyRef` is the location of your keys, and `dataRef` is the location of your data.

### A note on ordering

The order in which your receive your data depends on the order from `keyRef`, not `dataRef`:
```javascript
{
  "data": {
    // This order doesn't matter, the order is taken from keys/(user1 or user2).
    "3": true,
    "1": "some data",
    "2": 5
  },
  "keys": {
    // These two users have different orders for their data thanks to key side ordering.
    "user1": {
      "1": true,
      "2": true,
      "3": true
    },
    "user2": {
      "3": true,
      "2": true,
      "1": true
    }
  }
}
```
