package com.task.sm.videoeditor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import java.io.File
import kotlin.math.atan2

@UnstableApi
class EditActivity : AppCompatActivity() {

    lateinit var playerView: PlayerView
    lateinit var overlayContainer: FrameLayout
    lateinit var btnAddText: Button
    lateinit var btnAddGif: Button
    lateinit var btnSave: Button
    private lateinit var videoUri: Uri
    private lateinit var player: ExoPlayer
    private var outputFile: File? = null
    private var transformer: Transformer?=null
    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        // Initialize views
        playerView = findViewById(R.id.player_view)
        overlayContainer = findViewById(R.id.overlay_container)
        btnAddText = findViewById(R.id.btn_add_text)
        btnAddGif = findViewById(R.id.btn_add_gif)
        btnSave = findViewById(R.id.btn_save)
        progressBar = findViewById(R.id.progress)

        val videoPath = intent.getStringExtra("video_path")
        videoUri = Uri.parse(videoPath)

        // Initialize the player and the recorder
        initPlayer(videoUri)

        btnAddText.setOnClickListener {
            addDraggableText("Sample Text")
        }

        btnAddGif.setOnClickListener {
            // Use static image for GIF here. You can expand later.
            addDraggableImage(R.drawable.whale)
        }

        btnSave.setOnClickListener {
            progressBar.isVisible = true
            progressBar.isActivated = true
            exportVideoWithOverlays()
        }
    }

    private fun initPlayer(uri: Uri) {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Use the PlayerView to automatically handle the Surface for playback
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun addDraggableText(text: String) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.parseColor("#66000000"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        overlayContainer.addView(textView)
        makeDraggableAndRotatable(textView)
    }

    private fun addDraggableImage(@DrawableRes resId: Int) {
        val imageView = ImageView(this).apply {
            setImageResource(resId)
            layoutParams = FrameLayout.LayoutParams(200, 200)
        }
        overlayContainer.addView(imageView)
        makeDraggableAndRotatable(imageView)
    }

    private fun makeDraggableAndRotatable(view: View) {
        var dX = 0f
        var dY = 0f
        var rotationAngle = 0f

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialRotation = 0f
            private var lastAngle = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        initialRotation = view.rotation
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (event.pointerCount == 1) {
                            view.x = event.rawX + dX
                            view.y = event.rawY + dY
                        } else if (event.pointerCount == 2) {
                            val dx = event.getX(1) - event.getX(0)
                            val dy = event.getY(1) - event.getY(0)
                            val angle =
                                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            view.rotation = initialRotation + angle
                        }
                    }
                }
                return true
            }
        })
    }

    private fun scanFileToGallery(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android Q (API 29) and above, use MediaStore to insert the video into the gallery
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
            val contentResolver = contentResolver
            val uri =
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            val outputStream = contentResolver.openOutputStream(uri!!)
            outputStream?.write(file.readBytes())
            outputStream?.close()

            // Scan the file to make sure it appears in the gallery
            MediaScannerConnection.scanFile(
                this,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4")
            ) { path, uri ->
                Log.d("EditActivity", "File scanned: $path -> URI: $uri")
            }
        } else {
            // For older versions, use MediaScannerConnection directly
            MediaScannerConnection.scanFile(
                this,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4")
            ) { path, uri ->
                Log.d("EditActivity", "File scanned: $path -> URI: $uri")
            }
        }
    }

    private fun getOverlayBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            overlayContainer.width,
            overlayContainer.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        overlayContainer.draw(canvas)
        return bitmap
    }

    override fun onStop() {
        super.onStop()
        player.release()
//        mediaRecorder.release()
    }

    // Code for exporting overlays
    private fun exportVideoWithOverlays() {
        val overlayBitmap = getOverlayBitmap()

        val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
        val overlayEffect = OverlayEffect(listOf(bitmapOverlay))

        // Create an Effects instance with the BitmapOverlay
        val effects = Effects(emptyList(), listOf(overlayEffect))

        // Output file
        outputFile = createOutputFile(this)

        // Create an EditedMediaItem
        val editedMediaItem = EditedMediaItem.Builder(androidx.media3.common.MediaItem.fromUri(videoUri))
            .setEffects(effects)
            .build()


        transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    super.onCompleted(composition, exportResult)
                    Toast.makeText(this@EditActivity, "Video saved", Toast.LENGTH_LONG).show()
                    if (outputFile!=null) {
                        progressBar.isVisible = false
                        progressBar.isActivated = false
                        scanFileToGallery(outputFile!!)
                    }
                    else Log.d(javaClass.name,"Output file is null")
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    super.onError(composition, exportResult, exportException)
                    progressBar.isVisible = false
                    progressBar.isActivated = false
                    Toast.makeText(this@EditActivity, "Error: onError", Toast.LENGTH_LONG).show()
                }

                override fun onFallbackApplied(
                    composition: Composition,
                    originalTransformationRequest: TransformationRequest,
                    fallbackTransformationRequest: TransformationRequest
                ) {
                    super.onFallbackApplied(
                        composition,
                        originalTransformationRequest,
                        fallbackTransformationRequest
                    )
                    progressBar.isVisible = false
                    progressBar.isActivated = false
                    Toast.makeText(this@EditActivity, "Error: onFallbackApplied", Toast.LENGTH_LONG).show()
                }


            })
            .build()

        outputFile?.path?.let { transformer?.start(editedMediaItem, it) }
    }



    // Create output file
    private fun createOutputFile(context: Context): File {
        val timeStamp = System.currentTimeMillis()
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile("VIDEO_$timeStamp", ".mp4", storageDir)
    }
}

