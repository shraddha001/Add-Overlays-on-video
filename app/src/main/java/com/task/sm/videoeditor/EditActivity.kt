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
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.*
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
    private var transformer: Transformer? = null
    lateinit var progressBar: ProgressBar

    // Guidelines
    private lateinit var leftGuide: View
    private lateinit var rightGuide: View
    private lateinit var topGuide: View
    private lateinit var bottomGuide: View
    private lateinit var centerVerticalGuide: View
    private lateinit var centerHorizontalGuide: View

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

        // Guidelines
        leftGuide = findViewById(R.id.left_guideline)
        rightGuide = findViewById(R.id.right_guideline)
        topGuide = findViewById(R.id.top_guideline)
        bottomGuide = findViewById(R.id.bottom_guideline)
        centerVerticalGuide = findViewById(R.id.center_vertical_guideline)
        centerHorizontalGuide = findViewById(R.id.center_horizontal_guideline)

        val videoPath = intent.getStringExtra("video_path")
        videoUri = Uri.parse(videoPath)

        initPlayer(videoUri)

        btnAddText.setOnClickListener {
            addDraggableText("Sample Text")
        }

        btnAddGif.setOnClickListener {
            addDraggableImage(R.drawable.whale)
        }

        btnSave.setOnClickListener {
            progressBar.isVisible = true
            exportVideoWithOverlays()
        }
    }

    private fun showGuidelines() {

        leftGuide.visibility = View.VISIBLE
        rightGuide.visibility = View.VISIBLE
        topGuide.visibility = View.VISIBLE
        bottomGuide.visibility = View.VISIBLE
        centerVerticalGuide.visibility = View.VISIBLE
        centerHorizontalGuide.visibility = View.VISIBLE

        Log.d("Anchal", "showGuidelines: ")
        Log.d("Anchal", "showGuidelines:left "+leftGuide.isVisible)
        Log.d("Anchal", "showGuidelines:right "+rightGuide.isVisible)
        Log.d("Anchal", "showGuidelines:centervertical "+centerVerticalGuide.isVisible)
        Log.d("Anchal", "showGuidelines:centerhorizontal "+centerHorizontalGuide.isVisible)
    }


    private fun hideGuidelines() {
        Log.d("Anchal", "hideGuidelines: ")
        leftGuide.visibility = View.GONE
        rightGuide.visibility = View.GONE
        topGuide.visibility = View.GONE
        bottomGuide.visibility = View.GONE
        centerVerticalGuide.visibility = View.GONE
        centerHorizontalGuide.visibility = View.GONE
    }

    private fun initPlayer(uri: Uri) {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
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

//    private fun makeDraggableAndRotatable(view: View) {
//        var dX = 0f
//        var dY = 0f
//        view.setOnTouchListener(object : View.OnTouchListener {
//            private var initialRotation = 0f
//            override fun onTouch(v: View?, event: MotionEvent): Boolean {
//                when (event.actionMasked) {
//                    MotionEvent.ACTION_DOWN -> {
//                        dX = view.x - event.rawX
//                        dY = view.y - event.rawY
//                        initialRotation = view.rotation
//                        showGuidelines() // 👈 show when user starts dragging
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        if (event.pointerCount == 1) {
//                            view.x = event.rawX + dX
//                            view.y = event.rawY + dY
//                        } else if (event.pointerCount == 2) {
//                            val dx = event.getX(1) - event.getX(0)
//                            val dy = event.getY(1) - event.getY(0)
//                            val angle =
//                                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
//                            view.rotation = initialRotation + angle
//                        }
//                    }
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        hideGuidelines() // 👈 hide after drop
//                    }
//                }
//                return true
//            }
//        })
//    }

    private var lastVibrateTime = 0L // prevent continuous vibration

//    private fun makeDraggableAndRotatable(view: View) {
//        var dX = 0f
//        var dY = 0f
//        view.setOnTouchListener(object : View.OnTouchListener {
//            private var initialRotation = 0f
//            override fun onTouch(v: View?, event: MotionEvent): Boolean {
//                when (event.actionMasked) {
//                    MotionEvent.ACTION_DOWN -> {
//                        dX = view.x - event.rawX
//                        dY = view.y - event.rawY
//                        initialRotation = view.rotation
//                        showGuidelines() // show when user starts dragging
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        if (event.pointerCount == 1) {
//                            view.x = event.rawX + dX
//                            view.y = event.rawY + dY
//
//                            // 👉 Check vertical/horizontal center alignment
//                            checkIfInCenter(view)
//                        } else if (event.pointerCount == 2) {
//                            val dx = event.getX(1) - event.getX(0)
//                            val dy = event.getY(1) - event.getY(0)
//                            val angle =
//                                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
//                            view.rotation = initialRotation + angle
//                        }
//                    }
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        hideGuidelines() // hide after drop
//                    }
//                }
//                return true
//            }
//        })
//    }
//    private fun checkIfInCenter(view: View) {
//        val parentWidth = overlayContainer.width
//        val parentHeight = overlayContainer.height
//
//        val viewCenterX = view.x + view.width / 2
//        val viewCenterY = view.y + view.height / 2
//
//        val centerX = parentWidth / 2f
//        val centerY = parentHeight / 2f
//
//        val tolerance = 50 // pixels range for detection
//        val currentTime = System.currentTimeMillis()
//
//        var aligned = false
//
//        // 👉 Check vertical center alignment
//        if (Math.abs(viewCenterX - centerX) < tolerance) {
//            aligned = true
//            Log.d("Anchal", "Aligned with Vertical Center")
//        }
//
//        // 👉 Check horizontal center alignment
//        if (Math.abs(viewCenterY - centerY) < tolerance) {
//            aligned = true
//            Log.d("Anchal", "Aligned with Horizontal Center")
//        }
//
//        // 👉 Vibrate only once every 500ms when aligned
//        if (aligned && currentTime - lastVibrateTime > 500) {
//            vibrateOnPickup(this)
//            lastVibrateTime = currentTime
//        }
//    }


    private var hasVibrated = false // 👈 new flag

//    private fun makeDraggableAndRotatable(view: View) {
//        var dX = 0f
//        var dY = 0f
//        view.setOnTouchListener(object : View.OnTouchListener {
//            private var initialRotation = 0f
//            override fun onTouch(v: View?, event: MotionEvent): Boolean {
//                when (event.actionMasked) {
//                    MotionEvent.ACTION_DOWN -> {
//                        dX = view.x - event.rawX
//                        dY = view.y - event.rawY
//                        initialRotation = view.rotation
//                        hasVibrated = false // 👈 reset on pickup
//                        showGuidelines()
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        if (event.pointerCount == 1) {
//                            view.x = event.rawX + dX
//                            view.y = event.rawY + dY
//
//                            checkIfInCenter(view) // check alignment
//                        } else if (event.pointerCount == 2) {
//                            val dx = event.getX(1) - event.getX(0)
//                            val dy = event.getY(1) - event.getY(0)
//                            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
//                            view.rotation = initialRotation + angle
//                        }
//                    }
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        hideGuidelines()
//                    }
//                }
//                return true
//            }
//        })
//    }
//
//    private fun checkIfInCenter(view: View) {
//        if (hasVibrated) return // 👈 already vibrated this drag
//
//        val parentWidth = overlayContainer.width
//        val parentHeight = overlayContainer.height
//
//        val viewCenterX = view.x + view.width / 2
//        val viewCenterY = view.y + view.height / 2
//
//        val centerX = parentWidth / 2f
//        val centerY = parentHeight / 2f
//
//        val tolerance = 50
//
//        // 👉 Trigger vibration only once when aligned
//        if (Math.abs(viewCenterX - centerX) < tolerance ||
//            Math.abs(viewCenterY - centerY) < tolerance
//        ) {
//            vibrateOnPickup(this)
//            hasVibrated = true // 👈 mark as vibrated
//            Log.d("Anchal", "Vibration triggered once")
//        }
//    }


//    private var hasVibrated = false // for center guides
    private var hasVibratedEdge = false // for edge guides

//    private fun makeDraggableAndRotatable(view: View) {
//        var dX = 0f
//        var dY = 0f
//        view.setOnTouchListener(object : View.OnTouchListener {
//            private var initialRotation = 0f
//            override fun onTouch(v: View?, event: MotionEvent): Boolean {
//                when (event.actionMasked) {
//                    MotionEvent.ACTION_DOWN -> {
//                        dX = view.x - event.rawX
//                        dY = view.y - event.rawY
//                        initialRotation = view.rotation
//                        hasVibrated = false
//                        hasVibratedEdge = false
//                        showGuidelines()
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        if (event.pointerCount == 1) {
//                            view.x = event.rawX + dX
//                            view.y = event.rawY + dY
//
//                            checkCenterGuides(view)   // center vertical/horizontal
//                            checkEdgeGuides(view)     // left/right/top/bottom
//                        } else if (event.pointerCount == 2) {
//                            val dx = event.getX(1) - event.getX(0)
//                            val dy = event.getY(1) - event.getY(0)
//                            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
//                            view.rotation = initialRotation + angle
//                        }
//                    }
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        hideGuidelines()
//                    }
//                }
//                return true
//            }
//        })
//    }


    private fun makeDraggableAndRotatable(view: View) {
        var dX = 0f
        var dY = 0f
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialRotation = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        initialRotation = view.rotation
                        hasVibrated = false
                        hasVibratedEdge = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.pointerCount == 1) {
                            view.x = event.rawX + dX
                            view.y = event.rawY + dY

                            // 👉 update guideline visibility dynamically
                            updateGuidelines(view)
                        } else if (event.pointerCount == 2) {
                            val dx = event.getX(1) - event.getX(0)
                            val dy = event.getY(1) - event.getY(0)
                            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            view.rotation = initialRotation + angle
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        hideGuidelines()
                    }
                }
                return true
            }
        })
    }

    private fun updateGuidelines(view: View) {
        hideGuidelines() // hide all first

        val parentWidth = overlayContainer.width
        val parentHeight = overlayContainer.height

        val viewCenterX = view.x + view.width / 2
        val viewCenterY = view.y + view.height / 2

        val viewLeft = view.x
        val viewRight = view.x + view.width
        val viewTop = view.y
        val viewBottom = view.y + view.height

        val centerX = parentWidth / 2f
        val centerY = parentHeight / 2f
        val tolerance = 50

        var aligned = false

        // 👉 Check center vertical
        if (Math.abs(viewCenterX - centerX) < tolerance) {
            centerVerticalGuide.visibility = View.VISIBLE
            aligned = true
            if (!hasVibrated) {
                vibrateOnPickup(this)
                hasVibrated = true
            }
        }

        // 👉 Check center horizontal
        if (Math.abs(viewCenterY - centerY) < tolerance) {
            centerHorizontalGuide.visibility = View.VISIBLE
            aligned = true
            if (!hasVibrated) {
                vibrateOnPickup(this)
                hasVibrated = true
            }
        }

        // 👉 Check edges only if not aligned with center
        if (!aligned) {
            when {
                viewLeft <= 0 -> {
                    leftGuide.visibility = View.VISIBLE
                    if (!hasVibratedEdge) {
                        vibrateOnPickup(this)
                        hasVibratedEdge = true
                    }
                }
                viewRight >= parentWidth -> {
                    rightGuide.visibility = View.VISIBLE
                    if (!hasVibratedEdge) {
                        vibrateOnPickup(this)
                        hasVibratedEdge = true
                    }
                }
                viewTop <= 0 -> {
                    topGuide.visibility = View.VISIBLE
                    if (!hasVibratedEdge) {
                        vibrateOnPickup(this)
                        hasVibratedEdge = true
                    }
                }
                viewBottom >= parentHeight -> {
                    bottomGuide.visibility = View.VISIBLE
                    if (!hasVibratedEdge) {
                        vibrateOnPickup(this)
                        hasVibratedEdge = true
                    }
                }
            }
        }
    }


    /**
     * Vibrate when aligned to center guides (with tolerance)
     */
    private fun checkCenterGuides(view: View) {
        if (hasVibrated) return

        val parentWidth = overlayContainer.width
        val parentHeight = overlayContainer.height

        val viewCenterX = view.x + view.width / 2
        val viewCenterY = view.y + view.height / 2

        val centerX = parentWidth / 2f
        val centerY = parentHeight / 2f
        val tolerance = 50

        if (Math.abs(viewCenterX - centerX) < tolerance ||
            Math.abs(viewCenterY - centerY) < tolerance
        ) {
            vibrateOnPickup(this)
            hasVibrated = true
            Log.d("Anchal", "Vibrate: Center guide")
        }
    }

    /**
     * Vibrate once when view touches left/right/top/bottom guides
     */
    private fun checkEdgeGuides(view: View) {
        if (hasVibratedEdge) return

        val viewLeft = view.x
        val viewRight = view.x + view.width
        val viewTop = view.y
        val viewBottom = view.y + view.height

        val parentWidth = overlayContainer.width
        val parentHeight = overlayContainer.height

        // Check edges
        if (viewLeft <= 0 ||                // Left edge
            viewRight >= parentWidth ||     // Right edge
            viewTop <= 0 ||                 // Top edge
            viewBottom >= parentHeight      // Bottom edge
        ) {
            vibrateOnPickup(this)
            hasVibratedEdge = true
            Log.d("Anchal", "Vibrate: Edge guide")
        }
    }




    fun vibrateOnPickup(context: Context) {
        val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100, // duration in ms
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }


    private fun scanFileToGallery(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
            val uri =
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            val outputStream = contentResolver.openOutputStream(uri!!)
            outputStream?.write(file.readBytes())
            outputStream?.close()
            MediaScannerConnection.scanFile(
                this,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4")
            ) { path, uri ->
                Log.d("EditActivity", "File scanned: $path -> URI: $uri")
            }
        } else {
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
    }

    private fun exportVideoWithOverlays() {
        val overlayBitmap = getOverlayBitmap()
        val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
        val overlayEffect = OverlayEffect(listOf(bitmapOverlay))
        val effects = Effects(emptyList(), listOf(overlayEffect))
        outputFile = createOutputFile(this)
        val editedMediaItem =
            EditedMediaItem.Builder(androidx.media3.common.MediaItem.fromUri(videoUri))
                .setEffects(effects)
                .build()

        transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    super.onCompleted(composition, exportResult)
                    runOnUiThread {
                        Toast.makeText(this@EditActivity, "Video saved", Toast.LENGTH_LONG).show()
                        if (outputFile != null) {
                            progressBar.isVisible = false
                            scanFileToGallery(outputFile!!)
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    super.onError(composition, exportResult, exportException)
                    runOnUiThread {
                        progressBar.isVisible = false
                        Toast.makeText(this@EditActivity, "Error: onError", Toast.LENGTH_LONG).show()
                    }
                }
            })
            .build()

        outputFile?.path?.let { transformer?.start(editedMediaItem, it) }
    }

    private fun createOutputFile(context: Context): File {
        val timeStamp = System.currentTimeMillis()
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile("VIDEO_$timeStamp", ".mp4", storageDir)
    }
}
