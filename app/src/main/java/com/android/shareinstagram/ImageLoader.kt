package com.android.shareinstagram

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target

object ImageLoader {

    fun loadImageWithRoundCorner(
        context: Context,
        imageUrl: String,
        callback: ImageLoadCallback?,
        param: SimpleTarget<Bitmap>
    ) {
        val requestOptions = RequestOptions()
            .transforms(MultiTransformation(CenterCrop(), RoundedCorners(dpToPx(context, 10))))
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(context)
            .asBitmap() // Load as Bitmap
            .load(imageUrl)
            .apply(requestOptions)
            .listener(object : RequestListener<Bitmap?> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap?>,
                    isFirstResource: Boolean
                ): Boolean {
                    callback?.onLoadFailed()
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: Target<Bitmap?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    callback?.onLoadSuccess()
                    return false
                }
            })
            .submit() // Use submit to get the Bitmap
    }

    fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return Math.round(dp * density)
    }

    interface ImageLoadCallback {
        fun onLoadSuccess()
        fun onLoadFailed()
    }
}

