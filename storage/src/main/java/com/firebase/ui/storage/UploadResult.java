package com.firebase.ui.storage;

/**
 * Result of an upload operation.
 */
public class UploadResult {

    private FileInfo mInfo;

    UploadResult(FileInfo info) {
        mInfo = info;
    }

    /**
     * FileInfo of the uploaded file.
     */
    public FileInfo getInfo() {
        return mInfo;
    }

}
