package com.luck.tfdemo.numDistinguish;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.luck.library.utils.LogUtils;
import com.luck.tfdemo.R;
import com.luck.tfdemo.classification.env.ImageUtils;
import com.luck.tfdemo.classification.tflite.Classifier;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * ============================================================
 * 作 者 : 李桐桐
 * 创建日期 ： 2020/5/4
 * 描 述 :
 * ============================================================
 **/
public class NumDistinguishActivity2 extends DistinguishActivity {

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "num_cnn3_0.4.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/num_labels.txt";
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;


    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    @BindView(R.id.tvDistinguishResult)
    TextView mTvResult;
    @BindView(R.id.tvDistinguishTime)
    TextView mTvTime;
    @BindView(R.id.ivDistinguishPic)
    ImageView mIvPic;
    Distinguish mDistinguish;
    private int previewWidth, previewHeight;
    @Override
    protected void initPage() {
        int cropSize = TF_OD_API_INPUT_SIZE;
        try {
            mDistinguish =
                    DistinguishManager2.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LogUtils.e(e, "Exception initializing Distinguish!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Distinguish could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        previewHeight = DESIRED_PREVIEW_SIZE.getHeight();
        previewWidth = DESIRED_PREVIEW_SIZE.getWidth();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        0, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_image_num_distinguish;
    }

    @OnClick(R.id.btnDistinguish)
    public void onClick(View v) {
        processImage();
    }
    public Bitmap getNewBitmap(Bitmap bitmap, int newWidth ,int newHeight){
        // 获得图片的宽高.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 计算缩放比例.
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数.
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片.
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return newBitmap;
    }

    protected void processImage() {

        mIvPic.setDrawingCacheEnabled(true);
        rgbFrameBitmap = getNewBitmap(Bitmap.createBitmap(mIvPic.getDrawingCache()), previewWidth, previewHeight);
        ImageUtils.saveBitmap(mIvPic.getDrawingCache(),"origin.png");
        mIvPic.setDrawingCacheEnabled(false);
        ImageUtils.saveBitmap(rgbFrameBitmap,"change.png");
        ++timestamp;
        final long currTimestamp = timestamp;
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        ImageUtils.saveBitmap(croppedBitmap);
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Distinguish.Recognition> results =
                                mDistinguish.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mTvResult.setText(String.format(Locale.CHINA, "识别为：%s", results.get(0).getTitle()));
                                        mTvTime.setText(String.format(Locale.CHINA, "花费时间：%d ms", lastProcessingTimeMs));
                                    }
                                });
                    }
                });
    }
}
