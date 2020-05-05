package com.luck.tfdemo.numdistinguish2;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.luck.library.base.BaseActivity;
import com.luck.tfdemo.R;
import com.nex3z.fingerpaintview.FingerPaintView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NumDistinguishActivity3 extends BaseActivity {
    private static final String LOG_TAG = NumDistinguishActivity3.class.getSimpleName();

    @BindView(R.id.fpv_paint) FingerPaintView mFpvPaint;
    @BindView(R.id.tv_prediction)
    TextView mTvPrediction;
    @BindView(R.id.tv_probability)
    TextView mTvProbability;
    @BindView(R.id.tv_timecost)
    TextView mTvTimeCost;

    private DistinguishManager3 mDistinguishManager3;

    @Override
    protected void initPage() {
        try {
            mDistinguishManager3 = new DistinguishManager3(this);
        } catch (IOException e) {
            Toast.makeText(this, R.string.failed_to_create_classifier, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "init(): Failed to create Classifier", e);
        }
    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_num_distinguish3;
    }

    @OnClick(R.id.btn_detect)
    void onDetectClick() {
        if (mDistinguishManager3 == null) {
            Log.e(LOG_TAG, "onDetectClick(): Classifier is not initialized");
            return;
        } else if (mFpvPaint.isEmpty()) {
            Toast.makeText(this, R.string.please_write_a_digit, Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap image = mFpvPaint.exportToBitmap(
                DistinguishManager3.IMG_WIDTH, DistinguishManager3.IMG_HEIGHT);
        DistinguishResult distinguishResult = mDistinguishManager3.classify(image);
        renderResult(distinguishResult);
    }

    @OnClick(R.id.btn_clear)
    void onClearClick() {
        mFpvPaint.clear();
        mTvPrediction.setText(R.string.empty);
        mTvProbability.setText(R.string.empty);
        mTvTimeCost.setText(R.string.empty);
    }

    private void renderResult(DistinguishResult distinguishResult) {
        mTvPrediction.setText(String.valueOf(distinguishResult.getNumber()));
        mTvProbability.setText(String.valueOf(distinguishResult.getProbability()));
        mTvTimeCost.setText(String.format(getString(R.string.timecost_value),
                distinguishResult.getTimeCost()));
    }

    @Override
    protected boolean hasCustomSlide() {
        return false;
    }
}
