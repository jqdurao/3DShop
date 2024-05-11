package com.example.a3dshop;

import org.tensorflow.lite.Interpreter;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;

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

    public Bitmap segmentPerson(Bitmap bitmap) {
        // Preprocess the image: resize, normalize, etc.
        float[][][] preprocessed = preprocessImage(bitmap);
        // Run the model
        // Postprocess the output to create a mask
        // Apply the mask to the original image to get the segmented image
        // This is a simplified placeholder. Actual implementation will depend on the model being used.
        return bitmap;
    }

    private float[][][][] preprocessImage(Bitmap bitmap) {
        // Resize the bitmap to the model input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        // Convert the bitmap to a Mat
        Mat mat = new Mat();
        Utils.bitmapToMat(resizedBitmap, mat);

        // Convert the Mat to a float array
        float[] floatArray = new float[mat.rows() * mat.cols() * mat.channels()];
        mat.get(0, 0, floatArray);

        // Normalize pixel values to be between -1 and 1
        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = (floatArray[i] / 127.5f) - 1;
        }

        // Reshape the float array to match the input shape of the model
        float[][][][] input = new float[1][224][224][3];
        for (int i = 0; i < 224; i++) {
            for (int j = 0; j < 224; j++) {
                for (int k = 0; k < 3; k++) {
                    input[0][i][j][k] = floatArray[(i * 224 + j) * 3 + k];
                }
            }
        }

        return input;
    }
}


