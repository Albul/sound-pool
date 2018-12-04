package com.albul.supportsoundpool;

import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;

public class SoundChannelMetadata {

    public final int mChannelId;
    public final ParcelFileDescriptor mParcelDescr;
    public final AssetFileDescriptor mAssetDescr;
    public final long mFileOffset;
    public final long mFileSize;

    public SoundChannelMetadata(final int channelId,
                                final ParcelFileDescriptor parcelDescr, final AssetFileDescriptor assetDescr,
                                final long fileOffset, final long fileSize) {
        mChannelId = channelId;
        mParcelDescr = parcelDescr;
        mAssetDescr = assetDescr;
        mFileOffset = fileOffset;
        mFileSize = fileSize;
    }

    public final FileDescriptor getFileDescriptor() {
        if (mParcelDescr != null) return mParcelDescr.getFileDescriptor();
        if (mAssetDescr != null) return mAssetDescr.getFileDescriptor();
        return null;
    }

    public final void close() {
        try {
            (mParcelDescr == null ? mAssetDescr : mParcelDescr).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}