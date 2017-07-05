package com.firebase.ui.storage;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.ui.storage.images.FirebaseImage;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for manipulating files in Firebase Storage while keeping associated metadata
 * tied to records in Firebase Database.
 */
public class FirebaseFile {

    /**
     * Default type, can represent any file.
     */
    public static final int TYPE_FILE = 0;

    /**
     * Image type, file will be processed as an image and
     * {@link com.firebase.ui.storage.images.ImageInfo} will be attached to the {@link FileInfo}.
     */
    public static final int TYPE_IMAGE = 1;

    // Root path where files are stored in Storage and where Metadata is stored in Database
    private static final String FILES_ROOT = "_files_";

    // Queue for long-running operations
    private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
    private static final ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, QUEUE);

    /**
     * Get an Uploader instance to begin uploading a file.
     * @param context context in which to perform the upload.
     * @param reference the DatabaseReference that the file should be attached to.
     * @return an Uploader instance.
     */
    public static Uploader upload(Context context, DatabaseReference reference) {
        return new Uploader(context, reference, null);
    }

    /**
     * Get an Uploader instance to begin uploading aa file.
     * @param context context in which to perform the upload.
     * @param reference the DatabaseReference that the file should be attached to.
     * @param app FirebaseApp instance.
     * @return an Uploader instance.
     */
    public static Uploader upload(Context context, DatabaseReference reference, FirebaseApp app) {
        return new Uploader(context, reference, app);
    }

    /**
     * Get a Downloader instance to download a file as a stream.
     * @param context context in which to perform the download.
     * @param reference the DatabaseReference to which the file was attached.
     * @return a Downloader instance.
     */
    public static BaseDownloader downloadStream(Context context, DatabaseReference reference) {
        return new BaseDownloader(context, reference) {
            @Override
            protected void processFileInfo(final FileInfo fileInfo) {
                fileInfo.getReference().getStream()
                        .addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(StreamDownloadTask.TaskSnapshot taskSnapshot) {
                                reportSuccess(new DownloadResult(fileInfo, taskSnapshot.getStream()));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                reportError("Error getting stream: " + e.getMessage());
                            }
                        });
            }
        };
    }

    /**
     * Get a Downloader instance to download a file as a byte array.
     * @param context context in which to perform the download.
     * @param reference the DatabaseReference to which the file was attached.
     * @return a Downloader instance.
     */
    public static BaseDownloader downloadBytes(Context context, DatabaseReference reference) {
        return new BaseDownloader(context, reference) {
            @Override
            protected void processFileInfo(final FileInfo fileInfo) {
                fileInfo.getReference().getBytes(fileInfo.bytes)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                reportSuccess(new DownloadResult(fileInfo, bytes));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                reportError("Error getting stream: " + e.getMessage());
                            }
                        });
            }
        };
    }

    /**
     * Helper object to upload a file to Firebase Storage and place the metadata
     * (an {@link FileInfo}) in Firebase Database.
     */
    public static class Uploader {

        private static final String TAG = "FBFile.Uploader";

        private int mFileType = TYPE_FILE;

        private Context mContext;
        private FirebaseStorage mStorage;
        private StorageReference mStorageRef;

        private Uri mUri;
        private byte[] mBytes;
        private DatabaseReference mDatabaseRef;

        private TaskCompletionSource<UploadResult> mTaskSource = new TaskCompletionSource<>();

        private Uploader(Context context, DatabaseReference reference, FirebaseApp app) {
            this.mContext = context;
            this.mDatabaseRef = reference;

            if (app == null) {
                this.mStorage = FirebaseStorage.getInstance();
            } else {
                this.mStorage = FirebaseStorage.getInstance(app);
            }
        }

        /**
         * Provide a local Uri pointing to the file to upload, usually obtained from the
         * Camera or a file picker intent.
         * @param uri the picture to upload.
         * @return this uploader, for chaining.
         */
        public Uploader from(Uri uri) {
            if (mBytes != null && mBytes.length > 0) {
                throw new IllegalStateException("Cannot call from() twice with different sources.");
            }
            this.mUri = uri;
            return this;
        }

        /**
         * Provide a byte array of the file to upload.
         * @param bytes bytes to upload.
         * @return this uploader, for chaining.
         */
        public Uploader from(byte[] bytes) {
            if (mUri != null) {
                throw new IllegalStateException("Cannot call from() twice with different sources.");
            }
            this.mBytes = bytes;
            return  this;
        }

        /**
         * Specify the specific file type, such as {@link FirebaseFile#TYPE_IMAGE} for an image
         * or {@link FirebaseFile#TYPE_FILE} for a general file.
         * @param type the file type.
         * @return this uploader, for chaining.
         */
        public Uploader fileType(int type) {
            this.mFileType = type;
            return this;
        }

        /**
         * Begin executing the upload with all specified options.
         * Note: you may want to perform this action in a background service rather than an
         * Activity context.
         */
        public Task<UploadResult> execute() {
            // Check that we have a source Uri and a Firebase ref.
            boolean hasUri = (mUri != null);
            boolean hasBytes = (mBytes != null && mBytes.length > 0);
            if (!(hasBytes || hasUri) || mDatabaseRef == null) {
                throw new IllegalArgumentException(
                        "Must call from() and attachTo() before calling execute()");
            }

            // Get a references to where the file will live
            mStorageRef = mStorage.getReference().child(getStoragePath(mDatabaseRef));

            // Upload either the specified File or Bytes
            Task<UploadTask.TaskSnapshot> uploadTask = hasUri ?
                    mStorageRef.putFile(mUri) : mStorageRef.putBytes(mBytes);

            // Listen for results of the upload, then chain the FileInfo attachment operation
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<FileInfo>>() {
                @Override
                public Task<FileInfo> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    // Get the result of the task, re-throwing the exception if applicable
                    UploadTask.TaskSnapshot snapshot = task.getResult(Exception.class);
                    FileInfo fileInfo = new FileInfo(mStorageRef, snapshot.getMetadata());

                    // Start FileInfo attachment task
                    return attachFileInfo(fileInfo);
                }
            }).addOnSuccessListener(new OnSuccessListener<FileInfo>() {
                @Override
                public void onSuccess(FileInfo fileInfo) {
                    reportSuccess(fileInfo);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onFailure:" + mStorageRef.getName(), e);
                    reportError("Failed to upload file.");
                }
            });

            return mTaskSource.getTask();
        }

        /**
         * Process the FileInfo based on the file type, then attach it to the DatabaseReference.
         */
        private Task<FileInfo> attachFileInfo(final FileInfo fileInfo) {
            final TaskCompletionSource<FileInfo> source = new TaskCompletionSource<>();

            EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    switch (mFileType) {
                        case TYPE_IMAGE:
                            // TODO(samstern): if getImageInfo returns null should we
                            //                 error out the whole upload?
                            // Attach ImageInfo to the FileInfo
                            fileInfo.imageInfo = FirebaseImage.getImageInfo(
                                    FirebaseFile.Uploader.this);
                            break;
                        case TYPE_FILE:
                            // Nothing to do here
                            break;
                    }

                    // Synchronously write the FileInfo to the Database
                    DatabaseReference infoRef = getFileInfoRef(mDatabaseRef);
                    try {
                        Tasks.await(infoRef.setValue(fileInfo));
                    } catch (Exception e) {
                        Log.w(TAG, "Error writing FileInfo to Database", e);
                        source.setException(new Exception("Failed to get file info"));
                    }

                    source.setResult(fileInfo);
                }
            });

            return source.getTask();
        }

        private void reportSuccess(FileInfo info) {
            mTaskSource.setResult(new UploadResult(info));
        }

        private void reportError(String message) {
            mTaskSource.setException(new Exception(message));
        }

        public Context getContext() {
            return mContext;
        }

        public DatabaseReference getDatabaseRef() {
            return mDatabaseRef;
        }

        public StorageReference getStorageRef() {
            return mStorageRef;
        }

        public Uri getUri() {
            return mUri;
        }

        public byte[] getBytes() {
            return mBytes;
        }

    }

    public abstract static class BaseDownloader {

        private static final String TAG = "FBFile.Downloader";

        private Context mContext;
        private DatabaseReference mDatabaseRef;

        private TaskCompletionSource<DownloadResult> mTaskSource = new TaskCompletionSource<>();

        protected BaseDownloader(Context context, DatabaseReference reference) {
            if (context == null || reference == null) {
                throw new IllegalArgumentException("Must supply a Context and DatabaseReference.");
            }

            this.mContext = context;
            this.mDatabaseRef = reference;
        }

        /**
         * Download the FileInfo and pass it to the executor.
         */
        public Task<DownloadResult> execute() {
            // Get metadata from the Firebase Ref
            final DatabaseReference fileInfoRef = FirebaseFile.getFileInfoRef(mDatabaseRef);
            fileInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    FileInfo fileInfo = dataSnapshot.getValue(FileInfo.class);
                    if (fileInfo == null) {
                        Log.e(TAG, "FileInfo == null at " + getPath(mDatabaseRef));
                        reportError("No FileInfo at " + getPath(mDatabaseRef));
                        return;
                    }

                    // Call for additional processing
                    processFileInfo(fileInfo);
                }

                @Override
                public void onCancelled(DatabaseError firebaseError) {
                    Log.e(TAG, "onCancelled:" + firebaseError.getMessage());
                    reportError("Firebase value listen cancelled at " + getPath(fileInfoRef));
                }
            });

            return mTaskSource.getTask();
        }

        /**
         * Take final FileInfo and do additional processing. This method is expected to call
         * {@link #reportSuccess(DownloadResult)} or {@link #reportError(String)} at the termination
         * of processing.
         */
        protected abstract void processFileInfo(FileInfo fileInfo);

        protected void reportSuccess(FileInfo fileInfo) {
            reportSuccess(new DownloadResult(fileInfo));
        }

        protected void reportSuccess(DownloadResult result) {
            mTaskSource.setResult(result);
        }

        protected void reportError(String message) {
            mTaskSource.setException(new Exception(message));
        }

        protected Context getContext() {
            return mContext;
        }

        protected DatabaseReference getDatabaseRef() {
            return mDatabaseRef;
        }

        protected Task<DownloadResult> getTask() {
            return mTaskSource.getTask();
        }
    }

    /**
     * Get a reference to FileInfo based on a DatabaseReference.
     */
    private static DatabaseReference getFileInfoRef(DatabaseReference ref) {
        return ref.getRoot().child(FILES_ROOT).child(getPath(ref));
    }

    /**
     * Get Storage path for file based on a DatabaseReference.
     */
    private static String getStoragePath(DatabaseReference ref) {
        String databasePath = getPath(ref);
        if (databasePath.startsWith("/")) {
            databasePath = databasePath.substring(1);
        }

        // Get a references to where the file will live
        return FILES_ROOT + "/" + databasePath;
    }

    /**
     * Get the path to a DatabaseReference relative to the root. No leading slash.
     */
    private static String getPath(DatabaseReference reference) {
        DatabaseReference parent = reference.getParent();
        if (parent == null) {
            // This is the root
            return "";
        } else {
            // Recurse on parent reference
            return getPath(reference.getParent()) + "/" + reference.getKey();
        }
    }
}
