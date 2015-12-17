package com.pictureair.photopass.activity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.UmengUtil;

/**
 * Created by milo on 15/12/16.
 */
public class BaseFragmentActivity extends FragmentActivity{
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getInstance().addActivity(this);
        this.context = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 统计页面、时长
        if (context.getClass() == MainTabActivity.class) {
            UmengUtil.onResume(context, true);
        } else {
            UmengUtil.onResume(context, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (context.getClass() == MainTabActivity.class) {
            UmengUtil.onPause(context, true);
        } else {
            UmengUtil.onPause(context, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().killActivity(this);
    }

    /**
     * 判断网络是否连接
     *
     * @param act
     * @return
     */
    public static boolean isNetWorkConnect(Context act) {
        ConnectivityManager manager = (ConnectivityManager) act.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo networkinfo = manager.getActiveNetworkInfo();
        if (networkinfo == null || !networkinfo.isAvailable()) {
            return false;
        }
        return true;
    }
}