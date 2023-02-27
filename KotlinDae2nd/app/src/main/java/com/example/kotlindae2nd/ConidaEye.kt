package com.example.kotlindae2nd

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage class ConidaEye(nwContext : Activity) {
    private var nowContext = nwContext
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService =Executors.newSingleThreadExecutor()
    private val objectDetector: ObjectDetector by lazy {
        val localModel = LocalModel.Builder()
            .setAssetFilePath("coneAndOther.tflite")
            .build()
        val customOptions = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.6f)
            .setMaxPerObjectLabelCount(1)
            .build()
        ObjectDetection.getClient(customOptions)
    }

    init{
        checkPermission()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(nowContext)

        cameraProviderFuture.addListener({
            // ライフサイクルにバインドするために利用する
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            val analysisUseCase = ImageAnalysis.Builder()
                .build().apply {
                    setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                        // CameraImageはCloseしないと次のフレームが取れない
                        objectDetector.process(InputImage.fromMediaImage(imageProxy.image!!,imageProxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener {
                                for(detectedObject in it){
                                    val box = detectedObject.boundingBox
                                    for(label in detectedObject.labels){
                                        println(label.text)
                                        if(label.text=="Cone"){
                                            println("=============================")
                                            val centY = imageProxy.height/2
                                            val centX = imageProxy.width/2
                                            val diffY = (box.top+box.bottom)/2 - centY
                                            val diffX = (box.right+box.left)/2 -centX
                                            println("**********")
                                            println(diffY)
                                            println(diffX)
                                            println("**********")
                                            if(diffY>0){
                                                println("right")
                                            }else{
                                                println("left")
                                            }
                                            println("=============================")
                                        }
                                    }
                                }
                            }.addOnCompleteListener{
                                imageProxy.close()
                            }
                    }
                }
            // 「インカメ」を設定
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // バインドされているカメラの解除
                cameraProvider.unbindAll()

                // カメラをライフサイクルにリバインド
                cameraProvider.bindToLifecycle(nowContext as LifecycleOwner,cameraSelector,imageCapture,analysisUseCase)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(nowContext))
    }


    private fun checkPermission(){
        if (ActivityCompat.checkSelfPermission(
                nowContext,
                Manifest.permission.CAMERA,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                nowContext,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
    companion object{
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).toTypedArray()
    }

    fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return


        // 写真の名前の設定（タイムスタンプ）とMediaStoreの設定
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        //保存するオプションを作成
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(nowContext.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // 画像のキャプチャ
        // 結果はImageCapture.OnImageSavedCallbackのコールバックで返ってくる
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(nowContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)
                }
            }
        )
    }
    fun finish(){
        cameraExecutor.shutdown()
    }

}
