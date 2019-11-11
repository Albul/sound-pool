package com.olekdia.soundpool;

public class SoundSampleMetadata {

    public final int mSampleId;
    public final int mRawResId;
    public final String mPath;

    public SoundSampleMetadata(
        final int sampleId,
        final int rawResId,
        final String path
    ) {
        mSampleId = sampleId;
        mRawResId = rawResId;
        mPath = path;
    }
}