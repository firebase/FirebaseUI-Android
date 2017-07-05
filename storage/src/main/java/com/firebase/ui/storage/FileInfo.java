package com.firebase.ui.storage;

import com.firebase.ui.storage.images.ImageInfo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;

@IgnoreExtraProperties
public class FileInfo implements Serializable {

    /** Required Fields **/
    public String storagePath;
    public long bytes;
    public Object lastUpdated;

    /** Optional Fields **/
    public ImageInfo imageInfo;

    public FileInfo() {}

    public FileInfo(StorageReference reference, StorageMetadata metadata) {
        this.storagePath = reference.getPath();
        this.bytes = metadata.getSizeBytes();
        this.lastUpdated = ServerValue.TIMESTAMP;
    }

    @Exclude
    public StorageReference getReference() {
        return getReference(null);
    }

    @Exclude
    public StorageReference getReference(FirebaseApp app) {
        if (app == null) {
            return FirebaseStorage.getInstance().getReference(storagePath);
        } else {
            return FirebaseStorage.getInstance(app).getReference(storagePath);
        }
    }

    @Override
    public String toString() {
        return "{ " +
                "storagePath:" + storagePath + ", " +
                "bytes:" + bytes + ", " +
                "imageInfo:" + imageInfo +
                " }";
    }

}
