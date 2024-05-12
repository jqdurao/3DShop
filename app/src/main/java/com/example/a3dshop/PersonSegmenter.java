package com.example.a3dshop;

import org.tensorflow.lite.Interpreter;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class PersonSegmenter {
    private Interpreter tflite;

    private static final String MODEL_PATH = "1.tflite";

    public PersonSegmenter(AssetManager assetManager) throws IOException {
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
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 257, 257, true);

        // Convert the bitmap to a float array
        int width = resizedBitmap.getWidth();
        int height = resizedBitmap.getHeight();
        int[] intValues = new int[width * height];
        float[] floatValues = new float[width * height * 3];
        resizedBitmap.getPixels(intValues, 0, width, 0, 0, width, height);
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = ((val >> 16) & 0xFF) / 127.5f - 1;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 127.5f - 1;
            floatValues[i * 3 + 2] = (val & 0xFF) / 127.5f - 1;
        }

        // Reshape the float array to match the input shape of the model
        float[][][][] input = new float[1][257][257][3];
        for (int i = 0; i < 257; i++) {
            for (int j = 0; j < 257; j++) {
                for (int k = 0; k < 3; k++) {
                    input[0][i][j][k] = floatValues[(i * 257 + j) * 3 + k];
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
                int maxIndex = 0;
                float maxValue = Float.MIN_VALUE;
                for (int k = 0; k < 21; k++) {
                    if (output[0][i][j][k] > maxValue) {
                        maxValue = output[0][i][j][k];
                        maxIndex = k;
                    }
                }
                // Assign the pixel in the mask the class of the channel with the highest value
                mask[i * width + j] = maxIndex;
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
                // If the mask pixel does not represent the person, set the pixel to transparent
                if (resizedMask.getPixel(x, y) == 0) {
                    result.setPixel(x, y, 0);
                }
            }
        }

        return result;
    }

    public Bitmap segmentPerson(Bitmap bitmap) {
        // Preprocess the image: resize, normalize, etc.
        float[][][][] preprocessed = preprocessImage(bitmap);
        //System.out.println(preprocessed.toString());
        // Prepare the output array
        float[][][][] output = new float[1][257][257][21];
        // Run the model
        tflite.run(preprocessed, output);
        // Create a mask from the output
        Bitmap mask = createMask(output);
        // Apply the mask to the original image to get the segmented image
        Bitmap segmented = applyMaskToOriginal(bitmap, mask);
        return segmented;
    }
}
