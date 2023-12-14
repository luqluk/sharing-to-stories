package com.android.shareinstagram

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.palette.graphics.Palette
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest


class LayoutToImageConverter(private val context: Context) {

    fun convertLayoutToImage(
        binding: ViewBinding,
        resource: Bitmap,
        title: String,
        subtitle: String,
        segment: String
    ): Bitmap {
        val view = binding.root
        val imageView = binding.root.findViewById<ImageView>(R.id.imageView2)
        val tv_title = binding.root.findViewById<TextView>(R.id.title)
        val tv_subtitle = binding.root.findViewById<TextView>(R.id.subtitle)
        val tv_segment = binding.root.findViewById<TextView>(R.id.segment)

        tv_title.text = title
        tv_subtitle.text = subtitle
        tv_segment.text = segment

        imageView.setImageBitmap(resource)

        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap =
            Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(ContextCompat.getColor(context, android.R.color.transparent))
        }
        view.draw(canvas)

        return bitmap
    }

    fun saveBitmapImageTemporarily(bitmap: Bitmap, context: Context): Uri? {
        val timestamp = System.currentTimeMillis()

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.DATE_ADDED, timestamp)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?.let { uri ->
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            return uri
                        }
                    } catch (e: Exception) {
                        Log.e("TAG", "saveBitmapImageTemporarily: ", e)
                    }
                }
        } else {
            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "$timestamp.png")

            try {
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    return context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                }
            } catch (e: Exception) {
                Log.e("TAG", "saveBitmapImageTemporarily: ", e)
            }
        }
        return null
    }

    fun getDarkerColorHexFromBitmap(bitmap: Bitmap, x: Int, y: Int, darkenFactor: Float): String {
        val pixelColor = bitmap.getPixel(x, y)

        val red = (Color.red(pixelColor) * darkenFactor).toInt()
        val green = (Color.green(pixelColor) * darkenFactor).toInt()
        val blue = (Color.blue(pixelColor) * darkenFactor).toInt()

        // Combine RGB components into a single integer
        val rgb = (red shl 16) or (green shl 8) or blue

        // Convert the integer color to a hex string
        return String.format("#%06X", 0xFFFFFF and rgb)
    }

    fun getTopBottomColorBitmap(bitmap: Bitmap): ColorResult {

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Calculate top center coordinates
        val topCenterX = bitmapWidth / 2
        val topCenterY = 0.coerceAtMost(bitmapHeight - 1) // Ensure it's within valid range

        // Calculate bottom center coordinates
        val bottomCenterX = bitmapWidth / 2
        val bottomCenterY = (bitmapHeight - 1).coerceAtLeast(0) // Ensure it's within valid range

        val topColor = bitmap.getPixel(topCenterX, topCenterY)
        val bottomColor = bitmap.getPixel(bottomCenterX, bottomCenterY)

        // Convert the integer color to a hex string
        return ColorResult(
            hexColor(topColor),
            hexColor(bottomColor)
        )
    }

    fun hexColor(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    data class ColorResult(
        val top: String,
        val bottom: String,
    )

    fun getDominantColor(context: Context, bitmap: Bitmap, darkenFactor: Float = 0.8f): String {
        val palette = Palette.from(bitmap).generate()

        // Extract the dominant color or use the default color if extraction fails
        val defaultColor = ResourcesCompat.getColor(context.resources, R.color.black, null)
        val dominantColor = palette.getDominantColor(defaultColor)

        // Darken the color based on the darken factor
        val darkenedColor = darkenColor(dominantColor, darkenFactor)

        // Convert the darkened color to hex
        return String.format("#%06X", 0xFFFFFF and darkenedColor)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = color shr 24 and 0xFF
        val r = ((color shr 16 and 0xFF) * factor).toInt()
        val g = ((color shr 8 and 0xFF) * factor).toInt()
        val b = ((color and 0xFF) * factor).toInt()

        return a shl 24 or (r shl 16) or (g shl 8) or b
    }

    private fun roundedBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val pain = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val recF = RectF(rect)
        pain.style = Paint.Style.FILL
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(recF, radius, radius, pain)
        pain.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, recF, pain)

        return output
    }

}


class SquareTransformation : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val size = Math.min(toTransform.width, toTransform.height)
        val x = (toTransform.width - size) / 2
        val y = (toTransform.height - size) / 2

        val squaredBitmap = pool.get(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(squaredBitmap)
        val paint = Paint()

        val srcRect = Rect(x, y, x + size, y + size)
        val destRect = Rect(0, 0, size, size)

        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.isDither = true

        canvas.drawBitmap(toTransform, srcRect, destRect, paint)

        return squaredBitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        // Intentionally empty. See the documentation for BitmapTransformation.
    }


}