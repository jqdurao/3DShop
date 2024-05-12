package com.example.a3dshop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

import android.provider.MediaStore;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 2;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PersonSegmenter personSegmenter;
    private ImageCapture imageCapture = null;
    private PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //testModel();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            System.out.println("Requesting permissions");
        }

        Button captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(v -> {

            System.out.println("Capture button clicked");

            ContentValues contentValues = new ContentValues();


            String filename = "input-" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";

            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
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
                    Uri savedUri = outputFileResults.getSavedUri();
                    String msg = "Photo capture succeeded: " + savedUri;
                    Toast.makeText(getBaseContext(), msg,Toast.LENGTH_SHORT).show();
                    System.out.println(msg);
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(savedUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        // Use the PersonSegmenter to segment the person in the image
                        personSegmenter = new PersonSegmenter(getAssets(), getContentResolver());
                        Bitmap segmentedBitmap = personSegmenter.segmentPerson(bitmap);
                        System.out.println("Bitmap");

                        // Save the segmented image to the Pictures directory
                        String filename = "output-" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/images");

                        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                        if (imageUri != null) {
                            OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                            if (outputStream != null) {
                                segmentedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                                outputStream.flush();
                                outputStream.close();
                            }
                        }

                        Toast.makeText(getBaseContext(), "Image no bg saved",Toast.LENGTH_SHORT).show();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    System.out.println("Error capturing: " + error.getMessage());
                    String msg = "Photo capture failed: " + error.getMessage();
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        });
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

    public void testModel() {
        try {
            // Load the model
            PersonSegmenter personSegmenter = new PersonSegmenter(getAssets(), getContentResolver());

            // Load a known input image
            InputStream inputStream = getAssets().open("foto_test.jpeg");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Run the model
            Bitmap output = personSegmenter.segmentPerson(bitmap);

            // Save the output image to the Pictures directory
            String filename = "output-" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/images");

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri != null) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
                    if (outputStream != null) {
                        output.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.flush();
                        outputStream.close();
                        Toast.makeText(MainActivity.this, "Output image is saved", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Output image saved to: " + imageUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}


