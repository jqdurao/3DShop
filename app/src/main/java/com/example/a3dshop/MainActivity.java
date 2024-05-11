package com.example.a3dshop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.net.Uri;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.core.ImageCapture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

import android.provider.MediaStore;
import android.content.ContentValues;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 2;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PersonSegmenter personSegmenter;
    private ImageCapture imageCapture = null;
    private PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        Button captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(v -> {

            System.out.println("Capture button clicked");

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/images"); // Save to Pictures/images folder

            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(
                            getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    ).build();

            System.out.println("File created");
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {

                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    System.out.println("Image saved");
                    Uri savedUri = outputFileResults.getSavedUri();
                    String msg = "Photo capture succeeded: " + savedUri;
                    Toast.makeText(getBaseContext(), msg,Toast.LENGTH_SHORT).show();
                    System.out.println(msg);
                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    System.out.println("Error capturing: " + error.getMessage());
                    String msg = "Photo capture failed: " + error.getMessage();
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        });
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            System.out.println("Requesting permissions");
        }


    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                System.out.println("Error: " + e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        imageCapture = new ImageCapture.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}