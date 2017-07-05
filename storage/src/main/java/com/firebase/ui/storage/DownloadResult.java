package com.firebase.ui.storage;

import java.io.InputStream;

/**
 * Result of a download operation.
 */
public class DownloadResult {

    private FileInfo mFileInfo;
    private byte[] mBytes;
    private InputStream mStream;

    DownloadResult(FileInfo fileInfo) {
        mFileInfo = fileInfo;
    }

    DownloadResult(FileInfo fileInfo, byte[] bytes) {
        this.mFileInfo = fileInfo;
        this.mBytes = bytes;
    }

    DownloadResult(FileInfo fileInfo, InputStream stream) {
        this.mFileInfo = fileInfo;
        this.mStream = stream;
    }

    /**
     * FileInfo of the downloaded file.
     */
    public FileInfo getInfo() {
        return mFileInfo;
    }

    /**
     * Content of the downloaded file, as a byte[].
     * Null unless specifically requested.
     */
    public byte[] getBytes() {
        return mBytes;
    }

    /**
     * InputStream for the downloaded file.
     * Null unless specifically requested.
     */
    public InputStream getStream() {
        return mStream;
    }
}
