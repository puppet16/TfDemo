package com.luck.tfdemo.numDistinguish;

import android.os.Handler;
import android.os.HandlerThread;

import com.luck.library.base.BaseActivity;
import com.luck.library.utils.LogUtils;

/**
 * ============================================================
 * 作 者 : 李桐桐
 * 创建日期 ： 2020/5/3
 * 描 述 :
 * ============================================================
 **/
public abstract class DistinguishActivity extends BaseActivity {

    private Handler handler;
    private HandlerThread handlerThread;

    @Override
    public synchronized void onResume() {
        LogUtils.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LogUtils.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LogUtils.e(e, "Exception!");
        }

        super.onPause();
    }


    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
}
