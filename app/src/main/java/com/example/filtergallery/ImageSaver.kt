package com.example.filtergallery

import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Class for saving a render result images by default file path or by a specific file
 * **/
class ImageSaver(
    private val bitmap: Bitmap?,
    private val file: File? = null
) : Runnable {
    override fun run() {
        if (file == null) {
            val mImageFile = File(PATH)
            if (!mImageFile.exists()) {
                mImageFile.mkdir()
            }
            val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.ROOT).format(Date())
            val fileName = PATH + "IMG_" + timeStamp + ".jpg"
            val fileOutputStream = FileOutputStream(fileName)
            try {
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                fileOutputStream.close()
            }
        } else {
            val fileOutputStream = FileOutputStream(file)
            try {
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                fileOutputStream.close()
            }
        }
    }

    companion object {
        private val PATH = Environment.getExternalStorageDirectory().toString() + "/DCIM/"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}