package com.olekdia.soundpool

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.olekdia.androidcommon.extensions.getFileSize
import com.olekdia.androidcommon.extensions.isFileDocUri
import com.olekdia.androidcommon.extensions.isHttpPath
import com.olekdia.common.INVALID_L
import java.io.Closeable
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.net.URL

class SoundSampleDescriptor @Throws(IOException::class)
constructor(
    context: Context,
    metadata: SoundSampleMetadata
) : Closeable {

    /**
     * File size in bytes
     */
    val fileSize: Long
    private val fileOffset: Long

    private var assetDescr: AssetFileDescriptor? = null
    private var parcelDescr: ParcelFileDescriptor? = null

    private var httpPath: String? = null
    private var fileDescriptor: FileDescriptor? = null

    init {
        when {
            metadata.path == null -> {
                parcelDescr = null
                assetDescr = context.resources.openRawResourceFd(metadata.rawResId)
                    .also {
                        fileOffset = it.startOffset
                        fileSize = it.length
                        fileDescriptor = it.fileDescriptor
                    }
            }
            metadata.path.isFileDocUri -> {
                val cr = context.contentResolver
                val uri = Uri.parse(metadata.path)

                parcelDescr = cr.openFileDescriptor(uri, "r")
                    .also {
                        fileOffset = 0
                        fileSize = uri.getFileSize(cr)
                        fileDescriptor = it!!.fileDescriptor
                    }
            }

            metadata.path.isHttpPath -> {
                httpPath = metadata.path
                fileOffset = 0
                fileSize = URL(metadata.path).getFileSize().let {
                    if (it == INVALID_L) {
                        DEFAULT_BUFFER_SIZE.toLong()
                    } else {
                        it
                    }
                }
            }

            else -> {
                assetDescr = null

                val file = File(metadata.path)
                parcelDescr = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    .also {
                        fileOffset = 0
                        fileSize = file.length()
                        fileDescriptor = it!!.fileDescriptor
                    }
            }
        }
    }

    fun setExtractorDataSource(extractor: MediaExtractor) {
        fileDescriptor?.let {
            extractor.setDataSource(it, fileOffset, fileSize)
        } ?: httpPath?.let {
            extractor.setDataSource(it)
        } ?: run {
            throw IOException()
        }
    }

    @Throws(IOException::class)
    override fun close() {
        parcelDescr?.close()
        assetDescr?.let {
            if (it is Closeable) {
                it.close()
            } else if (it.parcelFileDescriptor is Closeable) {
                it.parcelFileDescriptor.close()
            }
        }
        assetDescr = null
        parcelDescr = null
    }
}