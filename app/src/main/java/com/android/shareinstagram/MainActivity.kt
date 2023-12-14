package com.android.shareinstagram

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.shareinstagram.ImageLoader.dpToPx
import com.android.shareinstagram.databinding.ActivityMainBinding
import com.android.shareinstagram.databinding.StickerLayoutBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.facebook.FacebookSdk


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val button = binding.btnShare

        val imageUrl =
            "https://www.w3schools.com/css/img_5terre.jpg"
        button.setOnClickListener {
            loadData(this, imageUrl, "Hello World", "MyTelkomsel", "Mini Games")

        }

    }



    fun loadData(context: Context, image: String, title: String, subtitle: String, segment: String) {

        val multiTransformation =
            MultiTransformation(SquareTransformation(), RoundedCorners(dpToPx(context, 9)))

        Glide.with(context)
            .asBitmap()
            .load(image)
            .apply(RequestOptions()
                .transform(multiTransformation))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val layout = LayoutToImageConverter(context)
                    val binding = StickerLayoutBinding.inflate(layoutInflater)
                    val bitmap = layout.convertLayoutToImage(binding, resource, title, subtitle, segment)
                    val uri = layout.saveBitmapImageTemporarily(bitmap, context)
                    shareToInstagram(uri, bitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Do nothing
                }
            })
    }

    fun shareToInstagram(uri: Uri?, bitmap: Bitmap) {
        // Instantiate an intent
        val intent = Intent("com.instagram.share.ADD_TO_STORY")

        // Attach your App ID to the intent
        val sourceApplication = "1855218138249865" // This is your application's FB ID get this from https://developers.facebook.com/docs/android/getting-started#app-id
        intent.putExtra("source_application", sourceApplication)

        intent.type = MEDIA_TYPE_JPEG;
        intent.putExtra("interactive_asset_uri", uri)

        val topColor = LayoutToImageConverter(this).getTopBottomColorBitmap(bitmap).top
        val bottomColor = LayoutToImageConverter(this).getTopBottomColorBitmap(bitmap).bottom


        intent.putExtra("top_background_color", topColor)
        intent.putExtra("bottom_background_color", bottomColor)

        // Grant URI permissions for the image
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        grantUriPermission(
            "com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        // Create a list of intents
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            "Hello, check out this link: https://www.example.com"
        )

        // Create a list of intents
        val intentList = mutableListOf<Intent>()
        intentList.add(sendIntent)
        intentList.add(intent)

        // Verify that there are apps available to handle the intent and start it
        val chooserIntent = Intent.createChooser(intentList.removeAt(0), "Choose an action")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toTypedArray())

        startActivity(chooserIntent)

    }


    companion object {
        const val MEDIA_TYPE_JPEG = "image/jpeg"

    }
}