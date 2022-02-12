package com.example.camerax

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.camerax.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    /**
     * ListenableFuture is a lightweight interface that actively listens for operations occurring
     * outside the main thread (asynchronous operations).
     * In this case, the operation being observed is the ProcessCameraProvider.
     */
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    /**
     * Add a cameraSelector attribute that will help to decide whether to use the front or back camera:
     */
    private lateinit var cameraSelector: CameraSelector

    //Grand Camera Request
    /**
     * registerForActivityResult is Android’s new API that obtains data from outside the application.
     * It prevents the scenario whereby the app’s process is killed before it can obtain the result from the other application.
     */
    private val cameraProviderResult = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissionGranted->
        if(permissionGranted){
            startCamera()
        }else {
            Snackbar.make(binding.root,"The camera permission is required", Snackbar.LENGTH_INDEFINITE).show()
        }
    }

    /**
     * imageCapture is a use case just like the Preview use case. It is used for capturing images.
    *imgCaptureExecutor is an interface that extends Executor,
     * Its work is to provide a thread that will be used for capturing an image.
     */
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProviderResult.launch(android.Manifest.permission.CAMERA)

        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        binding.imgCaptureBtn.setOnClickListener {
            takePhoto()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                animateFlash()
            }
        }

        binding.switchBtn.setOnClickListener {
            //change the cameraSelector
            cameraSelector = if(cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA){
                CameraSelector.DEFAULT_FRONT_CAMERA
            }else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            // restart the camera
            startCamera()
        }

        binding.galleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

    }


    companion object
    {
        val TAG = "MainActivity"
    }

    //A use case is a way developers can access camera features.
    private fun startCamera()
    {
        // listening for data from the camera
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            // connecting a preview use case to the preview in the xml file.
            val preview = Preview.Builder().build().also{
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }
            try{
                // clear all the previous use cases first.
                cameraProvider.unbindAll()
                // binding the lifecycle of the camera to the lifecycle of the application.
                cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageCapture)
            } catch (e: Exception) {
                Log.d(TAG, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
        /**
         * ContextCompat.getMainExecutor(this) is used to run the asynchronous operation that is being listened
         * by the cameraProviderFuture.
         Its context is within the application.
         */
    }

    private fun takePhoto()
    {
        imageCapture?.let{
            //Create a storage location whose fileName is timestamped in milliseconds.
            val fileName = "JPEG_${System.currentTimeMillis()}"
            val file = File(externalMediaDirs[0],fileName)

            // Save the image in the above file
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            /* pass in the details of where and how the image is taken.(arguments 1 and 2 of takePicture)
            pass in the details of what to do after an image is taken.(argument 3 of takePicture) */

            it.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults){
                        Log.i(TAG,"The image has been saved in ${file.toUri()}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            binding.root.context,
                            "Error taking photo",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "Error taking photo:$exception")
                    }

                })
        }
    }

    /**
     * Next, create an animateFlash() function that animates a screen flash when an image is taken:
     * The foreground changes to white after a delay of 100 milliseconds.It goes back to normal after delay of 50 milliseconds.
     * This feature of changing the foreground properties is only available for Android M devices and above.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash()
    {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }


}