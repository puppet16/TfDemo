package com.luck.tfdemo.numDistinguish;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.luck.library.base.BaseActivity;
import com.luck.library.utils.LogUtils;
import com.luck.tfdemo.R;
import com.luck.tfdemo.classification.env.ImageUtils;
import com.luck.tfdemo.classification.tflite.Classifier;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;

public class NumDistinguishActivity extends DistinguishActivity {
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final int TF_OD_API_INPUT_SIZE = 300;

    @BindView(R.id.tvDistinguishResult)
    TextView mTvResult;
    @BindView(R.id.tvDistinguishTime)
    TextView mTvTime;
    @BindView(R.id.ivDistinguishPic)
    ImageView mIvPic;

    DistinguishManager mDistinguish;
    private long lastProcessingTimeMs;
    private int previewWidth, previewHeight;
    private Bitmap croppedBitmap = null;


    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    @Override
    protected void initPage() {
        try {
            LogUtils.d(
                    "Creating DistinguishManager");
            mDistinguish = new DistinguishManager(this);
        } catch (IOException e) {
            LogUtils.e(e, "Failed to create DistinguishManager.");
        }

        previewHeight = DESIRED_PREVIEW_SIZE.getHeight();
        previewWidth = DESIRED_PREVIEW_SIZE.getWidth();
        int cropSize = TF_OD_API_INPUT_SIZE;
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        0, false);

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
        Bitmap rgbFrameBitmap = getNewBitmap(Bitmap.createBitmap(mIvPic.getDrawingCache()), 28, 28);
        ImageUtils.saveBitmap(Bitmap.createBitmap(mIvPic.getDrawingCache()), "1-recongnize-1.png");

        mIvPic.setDrawingCacheEnabled(false);

        ImageUtils.saveBitmap(rgbFrameBitmap, "1-recongnize-2.png");
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(getNewBitmap(rgbFrameBitmap, 28, 28), frameToCropTransform, null);
        ImageUtils.saveBitmap(croppedBitmap, "1-recongnize-3.png");
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mDistinguish != null) {
                            final long startTime = SystemClock.uptimeMillis();
                            final List<Classifier.Recognition> results =
                                    mDistinguish.recognizeImage(rgbFrameBitmap, 0);
                            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                            LogUtils.v("Detect: %s", results);

                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            mTvResult.setText(String.format(Locale.CHINA, "识别为：%s", results.get(0).getTitle()));
                                            mTvTime.setText(String.format(Locale.CHINA, "花费时间：%d", lastProcessingTimeMs));
                                        }
                                    });
                        }
                    }
                });
    }
}
