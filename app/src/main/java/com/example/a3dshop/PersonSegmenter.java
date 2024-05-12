package com.example.a3dshop;

import org.tensorflow.lite.Interpreter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PersonSegmenter {
    private Interpreter tflite;

    private ContentResolver contentResolver;

    private static final String MODEL_PATH = "40.tflite";

    public PersonSegmenter(AssetManager assetManager, ContentResolver cr) throws IOException {
        contentResolver = cr;
        tflite = new Interpreter(loadModelFile(assetManager));
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd(MODEL_PATH);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    private float[][][][] preprocessImage(Bitmap bitmap) {
        // Resize the bitmap to the model input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, false);

        // Convert the bitmap to a float array
        int width = resizedBitmap.getWidth();
        int height = resizedBitmap.getHeight();
        int[] intValues = new int[width * height];
        float[] floatValues = new float[width * height * 3];
        resizedBitmap.getPixels(intValues, 0, width, 0, 0, width, height);
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = ((val >> 16) & 0xFF) / 255.0f; // Blue
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f; // Green
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f; // Red
        }

        // Reshape the float array to match the input shape of the model
        float[][][][] input = new float[1][512][512][3];
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                for (int k = 0; k < 3; k++) {
                    input[0][i][j][k] = floatValues[(i * 512 + j) * 3 + k];
                }
            }
        }

        return input;
    }

    private Bitmap createMask(float[][][][] output) {
        int height = output[0].length;
        int width = output[0][0].length;
        int[] mask = new int[width * height];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int color = output[0][i][j][0] > 0.5 ? 255 : 0;
                mask[i * width + j] = Color.argb(255, color, color, color); // Set alpha to 255, and red, green, and blue to the output value or 0
                //System.out.println("Mask pixel: " + mask[i * width + j]);
            }
        }
        // Create a Bitmap from the mask
        Bitmap maskBitmap = Bitmap.createBitmap(mask, width, height, Bitmap.Config.ARGB_8888);

        return maskBitmap;
    }

    private Bitmap applyMaskToOriginal(Bitmap original, Bitmap mask) {
        // Resize the mask to match the original image size
        Bitmap resizedMask = Bitmap.createScaledBitmap(mask, original.getWidth(), original.getHeight(), true);

        // Create a mutable copy of the original image to modify it
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);

        // Iterate over each pixel in the original image and the resized mask
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                // If the mask pixel is not white, set the pixel to transparent
                if (resizedMask.getPixel(x, y) != Color.WHITE) {
                    result.setPixel(x, y, Color.TRANSPARENT);
                }
            }
        }

        return result;
    }

    private void saveMask(Bitmap mask, String filename) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/images");

        Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (imageUri != null) {
            try {
                OutputStream outputStream = contentResolver.openOutputStream(imageUri);
                if (outputStream != null) {
                    mask.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    System.out.println("Mask image is saved");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Mask image saved to: " + imageUri);
    }

    public Bitmap segmentPerson(Bitmap bitmap) {
        // Preprocess the image: resize, normalize, etc.
        float[][][][] preprocessed = preprocessImage(bitmap);
        System.out.println("Preprocessed image");
        // Prepare the output array
        float[][][][] output = new float[1][512][512][1];
        System.out.println("Prepared output");
        // Run the model
        tflite.run(preprocessed, output);
        System.out.println("Model Run");
        // Print the output of each segment

        // Create a mask from the output
        Bitmap mask = createMask(output);
        System.out.println("Mask created");

        String filename = "mask-" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";

        saveMask(mask, filename);  // Save the mask as an image
        // Apply the mask to the original image to get the segmented image
        Bitmap segmented = applyMaskToOriginal(bitmap, mask);
        System.out.println("Mask applied to original image");
        return segmented;
    }
}



