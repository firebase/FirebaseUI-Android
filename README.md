# FirebaseUI-Android

This library provides the simplest way to bind the Firebase Android SDK to your native Android app.
Get started now, be running in minutes.

## Using the library in your Android app

To use the FirebaseUI library in our project, we need to do a few things:

1. Add the library to the list of dependencies of our project
2. Create a class to represent the properties of our objects, as they are stored into the database
3. Create a custom list adapter to map from Firebase to Android

The FirebaseUI library is most prominent in step 3. But first we have to add it to our project.

### Adding the library to your project (gradle.build or module dependencies dialog)

If your Android app already uses Firebase, you have added a dependency to the Firebase SDK to your dependencies.
In this step we'll add the FirebaseUI library as another dependency.

![Open module settings](doc-images/1-module-settings-menu.png "Open the module settings")
![Current dependencies](doc-images/2-module-settings-add-library.png "Current dependencies")
![Find library](doc-images/3-module-settings-find-library.png "Find the firebase-ui library")
![Updated dependencies](doc-images/4-module-settings-library-added.png "Updated dependencies")

You can also add the library dependency directly to your app's gradle.build file:

![Added to gradle.build](doc-images/5-gradle-dependency-added.png "Added to gradle.build")

After the project is synchronized, we're ready to start using Firebase functionality in our app.

### Creating a model class

In your app, create a class that represents the data from Firebase that you want to show in the ListView.

So say we have these chat messages in our Firebase database:

![Chat messages in dashboard](doc-images/5-chat-messages-in-dashboard.png "Chat messages in dashboard")

We can represent a chat message with this Java class:

    public class ChatMessage {
        String message;
        String name;

        public ChatMessage() {
        }

        public ChatMessage(String name, String message) {
            this.message = message;
            this.name = name;
        }

        public String getMessage() {
            return message;
        }

        public String getName() {
            return name;
        }
    }

A few things to note here:

 * the field have the exact same name as the properties in Firebase. This allows Firebase to automatically map the properties to these fields.
 * there is a default (parameterless constructor) that is necessary for Firebase to be able to create a new instance of this class
 * there is a convenience constructor that takes the member fields, so that we easily create a fully initialized `ChatMessage` in our app
 * the `getMessage` and `getName` methods are so-called getters and follow a JavaBean pattern

A little-known feature of Firebase for Android is that you can pass an instance of this `ChatMessage` class to `setValue()`:

    Firebase ref = new Firebase("https://nanochat.firebaseio.com/");
    ChatMessage msg = new ChatMessage("puf", "Hello FirebaseUI world!");
    ref.push().setValue(msg);

The Firebase Android client will read the values from the `msg` and write them into the properties of the new child in the database.

Conversely, we can read a `ChatMessage` straight from a `DataSnapshot` in our event handlers:

    ref.limitToLast(5).addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot snapshot) {
            for (DataSnapshot msgSnapshot: snapshot.getChildren()) {
                ChatMessage msg = msgSnapshot.getValue(ChatMessage.class);
                Log.i("Chat", chat.getName()+": "+chat.getMessage());
            }
        }
        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.e("Chat", "The read failed: " + firebaseError.getMessage());
        }
    });

In the above snippet we have a query for the last 5 chat messages. Whenever those change (i.e. when an new message is added)
we get the `ChatMessage` objects from the `DataSnapshot` with `getValue(ChatMessage.class)`. The Firebase Android client will
then read the properties that it got from the database and map them to the fields of our `ChatMessage` class.

But when we build our app using the `FirebaseListAdapter`, we often won't need to register our own EventListener. The
`FirebaseListAdapter` takes care of that for us.

### Subclassing the FirebaseListAdapter



## Contributing to the library