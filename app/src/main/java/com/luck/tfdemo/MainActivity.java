package com.luck.tfdemo;

import android.app.Activity;
import android.content.Intent;
import android.util.SparseArray;
import android.view.View;

import com.luck.library.base.BaseActivity;
import com.luck.tfdemo.classification.ImageClassificationActivity;
import com.luck.tfdemo.numDistinguish.NumDistinguishActivity;
import com.luck.tfdemo.numDistinguish.NumDistinguishActivity2;
import com.luck.tfdemo.numdistinguish2.NumDistinguishActivity3;

import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    private SparseArray<Class<? extends Activity>> mActivityArray = new SparseArray<Class<? extends Activity>>() {
        {
            put(R.id.btnImageClassification, ImageClassificationActivity.class);
            put(R.id.btnNumDistinguish, NumDistinguishActivity.class);
            put(R.id.btnNumDistinguish2, NumDistinguishActivity2.class);
            put(R.id.btnNumDistinguish3, NumDistinguishActivity3.class);
        }
    };

    @Override
    protected void initPage() {

    }

    @Override
    protected int getPageLayoutId() {
        return R.layout.activity_main;
    }

    @OnClick({R.id.btnImageClassification, R.id.btnNumDistinguish, R.id.btnNumDistinguish2
            , R.id.btnNumDistinguish3})
    public void onClick(View v) {
        startActivity(new Intent(mActivity, mActivityArray.get(v.getId())));
    }
}
