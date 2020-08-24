package com.olekdia.soundpool

import com.olekdia.common.misc.Path

class SoundSampleMetadata(
    val sampleId: Int,
    val rawResId: Int,
    path: String?
) {
    val path: Path = Path(path ?: "")
}