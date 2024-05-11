package com.example.a3dshop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image);

        ImageView segmentedImageView = findViewById(R.id.segmentedImageView);

        // Get the path of the segmented image from the Intent
        String segmentedImagePath = getIntent().getStringExtra("segmentedImagePath");

        // Load the segmented image as a Bitmap
        Bitmap segmentedBitmap = BitmapFactory.decodeFile(segmentedImagePath);

        // Set the ImageView's image to the segmented bitmap
        segmentedImageView.setImageBitmap(segmentedBitmap);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;




        });
    }
}