package com.olekdia.soundpool;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.olekdia.androidcommon.extensions.FileExtensionsKt;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SoundSampleDescriptor implements Closeable {

    public final long mFileOffset;
    public final long mFileSize;

    private AssetFileDescriptor mAssetDescr;
    private ParcelFileDescriptor mParcelDescr;
    private FileDescriptor mFd;

    public SoundSampleDescriptor(
        final Context context,
        final SoundSampleMetadata metadata
    ) throws FileNotFoundException {
        if (metadata.mPath == null) {
            mParcelDescr = null;

            mAssetDescr = context.getResources().openRawResourceFd(metadata.mRawResId);
            mFileOffset = mAssetDescr.getStartOffset();
            mFileSize = mAssetDescr.getLength();
            mFd = mAssetDescr.getFileDescriptor();
        } else if (FileExtensionsKt.isFileDocUri(metadata.mPath)) {
            final ContentResolver cr = context.getContentResolver();
            final Uri uri = Uri.parse(metadata.mPath);
            mFileOffset = 0;
            mFileSize = FileExtensionsKt.getFileSize(uri, cr);
            mParcelDescr = cr.openFileDescriptor(uri, "r");
            mFd = mParcelDescr.getFileDescriptor();
        } else {
            mAssetDescr = null;

            final File file = new File(metadata.mPath);
            mParcelDescr = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            mFileOffset = 0;
            mFileSize = file.length();
            mFd = mParcelDescr.getFileDescriptor();
        }
    }

    public final FileDescriptor getFileDescriptor() {
        return mFd;
    }

    @Override
    public void close() throws IOException {
        if (mParcelDescr != null) {
            mParcelDescr.close();
        } else if (mAssetDescr != null) {
            if (mAssetDescr instanceof Closeable) {
                mAssetDescr.close();
            } else if (mAssetDescr.getParcelFileDescriptor() instanceof Closeable) {
                mAssetDescr.getParcelFileDescriptor().close();
            }
        }
        mAssetDescr = null;
        mParcelDescr = null;
        mFd = null;
    }
}