package com.saigyouji.android.criminalintent

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import kotlin.math.roundToInt

fun getScaledBitmap(path: String, activity: Activity): Bitmap{
    val size = Point()
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    {
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        size.x = bounds.width()
        size.y = bounds.height()
    }
    else
    {
        activity.windowManager.defaultDisplay.getSize(size)
    }
    return getScaledBitmap(path, size.x, size.y)
}
fun getScaledBitmap(path:String, destWidth:Int, destHeight:Int): Bitmap{
    //Read in the dimensions of the image on disk
    var options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(path, options)

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()
    var inSampleSize = 1
    //figure out how much to scale down by
    if(srcHeight > destHeight || srcWidth > destWidth){
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth

        val sampleScale = if(heightScale > widthScale)
            heightScale
        else
            widthScale
        inSampleSize = sampleScale.roundToInt()
    }
    options = BitmapFactory.Options()
    options.inSampleSize = inSampleSize

    //Read in and create final BitmapFactory
    return BitmapFactory.decodeFile(path, options)
}