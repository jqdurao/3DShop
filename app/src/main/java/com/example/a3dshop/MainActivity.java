package com.example.a3dshop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.net.Uri;
import android.widget.Button;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

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
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 2;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private ImageCapture imageCapture = null;
    private PreviewView previewView;
    private PersonSegmenter personSegmenter;

    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        File appDirectory = getFilesDir();
        outputDirectory = new File(appDirectory, "images");

        // Initialize the PersonSegmenter with the path of your model
        personSegmenter = new PersonSegmenter("my_model.tflite");

        Button captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(v -> {

            System.out.println("Capture button clicked");

            File photoFile = new File(outputDirectory, new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg");
            ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            System.out.println("File created");
            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {

                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    System.out.println("Image saved");
                    Uri savedUri = Uri.fromFile(photoFile);
                    String msg = "Photo capture succeeded: " + savedUri;
                    Toast.makeText(getBaseContext(), msg,Toast.LENGTH_SHORT).show();

                    // Load the image
                    Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                    // Segment the person
                    Bitmap segmentedBitmap = personSegmenter.segmentPerson(bitmap);

                    // Display the segmented image
                    ImageView imageView = findViewById(R.id.captured_image);
                    imageView.setImageBitmap(segmentedBitmap);
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

    // ... rest of your code ...
}