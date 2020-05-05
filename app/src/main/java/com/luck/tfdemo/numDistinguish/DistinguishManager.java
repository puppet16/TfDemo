package com.luck.tfdemo.numDistinguish;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.os.Trace;

import com.luck.library.utils.LogUtils;
import com.luck.tfdemo.classification.tflite.Classifier;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * ============================================================
 * 作 者 : 李桐桐
 * 创建日期 ： 2020/5/3
 * 描 述 :
 * ============================================================
 **/
public class DistinguishManager {
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 1.0f;

    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel;

    /** Image size along the x axis. */
    private final int imageSizeX;

    /** Image size along the y axis. */
    private final int imageSizeY;

    /** Optional GPU delegate for accleration. */
    private GpuDelegate gpuDelegate = null;

    /** Optional NNAPI delegate for accleration. */
    private NnApiDelegate nnApiDelegate = null;

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite;

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    /** Labels corresponding to the output of the vision model. */
    private List<String> labels;

    /** Input image TensorBuffer. */
    private TensorImage inputImageBuffer;

    /** Output probability TensorBuffer. */
    private final TensorBuffer outputProbabilityBuffer;

    /** Processer to apply post processing of the output probability. */
    private final TensorProcessor probabilityProcessor;

    public DistinguishManager(Context context) throws IOException {
        tfliteModel = FileUtil.loadMappedFile(context, "num_cnn3_0.4.tflite");
        gpuDelegate = new GpuDelegate();
        tfliteOptions.addDelegate(gpuDelegate);
        tfliteOptions.setNumThreads(1);
        tflite = new Interpreter(tfliteModel, tfliteOptions);

        // Loads labels out from the label file.
        labels = FileUtil.loadLabels(context, "num_labels.txt");

        // Reads type and shape of input and output tensors, respectively.
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(imageDataType);

        // Creates the output tensor and its processor.
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        // Creates the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        LogUtils.d("Created a Tensorflow Lite Image Classifier.");
    }
//
//    private ByteBuffer handleInputImageBuffer(TensorImage image) {
//        int inputSize = 300;
//        int[] intValues = new int[inputSize * inputSize];
//        ByteBuffer temp = image.getBuffer();
//        for (int i = 0; i < inputSize; ++i) {
//            for (int j = 0; j < inputSize; ++j) {
//                int pixelValue = intValues[i * inputSize + j];
//                temp.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                temp.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                temp.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//            }
//        }
//        return temp;
//    }
    /** Runs inference and returns the classification results. */
    public List<Classifier.Recognition> recognizeImage(final Bitmap bitmap, int sensorOrientation) {
        // Logs this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("loadImage");
        long startTimeForLoadImage = SystemClock.uptimeMillis();
        inputImageBuffer = loadImage(bitmap, sensorOrientation);
        long endTimeForLoadImage = SystemClock.uptimeMillis();
        Trace.endSection();
        LogUtils.v("Timecost to load the image: " + (endTimeForLoadImage - startTimeForLoadImage));

        // Runs the inference call.
        Trace.beginSection("runInference");
        long startTimeForReference = SystemClock.uptimeMillis();
        tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
        long endTimeForReference = SystemClock.uptimeMillis();
        Trace.endSection();
        LogUtils.v("Timecost to run model inference: " + (endTimeForReference - startTimeForReference));

        // Gets the map of label and probability.
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        Trace.endSection();

        // Gets top-k results.
        return getTopKProbability(labeledProbability);
    }

    /** Loads input image, and applies preprocessing. */
    private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90;
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(numRotation))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    /** Gets the top-k results. */
    private static List<Classifier.Recognition> getTopKProbability(Map<String, Float> labelProb) {
        // Find the best classifications.
        PriorityQueue<Classifier.Recognition> pq =
                new PriorityQueue<>(
                        3,
                        new Comparator<Classifier.Recognition>() {
                            @Override
                            public int compare(Classifier.Recognition lhs, Classifier.Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (Map.Entry<String, Float> entry : labelProb.entrySet()) {
            pq.add(new Classifier.Recognition("" + entry.getKey(), entry.getKey(), entry.getValue(), null));
        }

        final ArrayList<Classifier.Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), 3);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    /** Closes the interpreter and model to release resources. */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (nnApiDelegate != null) {
            nnApiDelegate.close();
            nnApiDelegate = null;
        }
        tfliteModel = null;
    }

    protected TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
//        return new NormalizeOp(new float[]{IMAGE_MEAN,IMAGE_MEAN,IMAGE_MEAN}, new float[]{IMAGE_STD,IMAGE_STD,IMAGE_STD});
    }
    protected TensorOperator getPostprocessNormalizeOp() {
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
//        return new NormalizeOp(0f, 255f);
    }
}
