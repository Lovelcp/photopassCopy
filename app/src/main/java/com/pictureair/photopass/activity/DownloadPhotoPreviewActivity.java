package com.pictureair.photopass.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pictureair.photopass.GalleryWidget.GalleryViewPager;
import com.pictureair.photopass.GalleryWidget.UrlPagerAdapter;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.db.PictureAirDbManager;
import com.pictureair.photopass.entity.PhotoDownLoadInfo;
import com.pictureair.photopass.entity.PhotoInfo;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.PictureAirLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by pengwu on 16/7/29.
 */
public class DownloadPhotoPreviewActivity extends BaseActivity implements View.OnClickListener{

    private TextView locationTextView;
    private GalleryViewPager mViewPager;
    private ImageView returnImageView;

    private PictureAirDbManager pictureAirDbManager;

    private RelativeLayout titleBar;
    private static final String TAG = "PreviewPhotoActivity";

    //图片显示框架
    private ArrayList<PhotoInfo> photolist;
    private int currentPosition;//记录当前预览照片的索引值

    /**
     * 是否是横屏模式
     */
    private boolean isLandscape = false;

    private RelativeLayout photoFraRelativeLayout;

    private Handler previewPhotoHandler;

    public static final int NO_PHOTOS = 2323;

    private static class PreViewHandler extends Handler{
        private final WeakReference<DownloadPhotoPreviewActivity> mActivity;

        public PreViewHandler(DownloadPhotoPreviewActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mActivity.get() == null) {
                return;
            }
            mActivity.get().dealHandler(msg);
        }
    }

    private void dealHandler(Message msg) {

        switch (msg.what) {
            case 7:
                mViewPager = (GalleryViewPager) findViewById(R.id.download_preview_viewer);
                UrlPagerAdapter pagerAdapter = new UrlPagerAdapter(DownloadPhotoPreviewActivity.this, photolist,1);
                mViewPager.setOffscreenPageLimit(2);
                mViewPager.setAdapter(pagerAdapter);
                mViewPager.setCurrentItem(currentPosition, true);
                //初始化底部索引按钮
                updateIndexTools(true);

                PictureAirLog.v(TAG, "----------------------->initing...3");

                mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                    @Override
                    public void onPageSelected(int arg0) {
                        //初始化每张图片的love图标
                        PictureAirLog.v(TAG, "----------------------->initing...4");
                        currentPosition = arg0;
                    }

                    @Override
                    public void onPageScrolled(int arg0, float arg1, int arg2) {

                    }

                    @Override
                    public void onPageScrollStateChanged(int arg0) {
                        PictureAirLog.v(TAG, "----------------------->initing...5");
                        if (arg0 == 0) {//结束滑动
                            updateIndexTools(false);
                        }
                    }
                });

                PictureAirLog.v(TAG, "----------------------->initing...6");
                break;
            case NO_PHOTOS://没有图片
                dismissPWProgressDialog();
                finish();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_photo_preview);
        PictureAirLog.out("oncreate start----");
        init();
        PictureAirLog.out("oncreate finish----");
    }

    private void init() {
        // TODO Auto-generated method stub
        previewPhotoHandler = new PreViewHandler(this);
        pictureAirDbManager = new PictureAirDbManager(this);
        PictureAirLog.out("oncreate----->2");
        returnImageView = (ImageView) findViewById(R.id.download_preview_back);
        locationTextView = (TextView) findViewById(R.id.download_preview_title);
        photoFraRelativeLayout = (RelativeLayout) findViewById(R.id.download_preview_fra_layout);
        titleBar = (RelativeLayout) findViewById(R.id.download_preview_titlebar);
        returnImageView.setOnClickListener(this);
        PictureAirLog.v(TAG, "----------------------->initing...1");

        Configuration cf = getResources().getConfiguration();
        int ori = cf.orientation;
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
            landscapeOrientation();
        }
        showPWProgressDialog();
        getPreviewPhotos(getIntent());
    }

    private void getPreviewPhotos(Intent intent) {
        final Intent intent1 = intent;
        new Thread(){
            @Override
            public void run() {
                Bundle bundle = intent1.getExtras();
                currentPosition = bundle.getInt("position", 0);
                photolist = new ArrayList<PhotoInfo>();
                SharedPreferences sPreferences = MyApplication.getInstance().getSharedPreferences(Common.SHARED_PREFERENCE_USERINFO_NAME, Context.MODE_PRIVATE);
                String userId = sPreferences.getString(Common.USERINFO_ID, "");
                List<PhotoDownLoadInfo> photos = pictureAirDbManager.getPhotos(userId,true);
                if (photos != null && photos.size() >0) {
                    for (int i=0;i<photos.size();i++) {
                        PhotoDownLoadInfo downLoadInfo = photos.get(i);

                        PhotoInfo photoInfo = new PhotoInfo();
                        photoInfo.photoId = downLoadInfo.getPhotoId();
                        String fileName = AppUtil.getReallyFileName(downLoadInfo.getUrl(),0);
                        String loadUrl = Common.PHOTO_DOWNLOAD_PATH+fileName;
                        photoInfo.photoPathOrURL = loadUrl;
                        photoInfo.photoThumbnail = downLoadInfo.getPreviewUrl();
                        photoInfo.photoThumbnail_512 = "";
                        photoInfo.photoThumbnail_1024 = "";
                        photoInfo.photoPassCode = "";
                        photoInfo.locationId = "";
                        photoInfo.locationName = "";
                        photoInfo.locationCountry = "";
                        photoInfo.shootOn = downLoadInfo.getShootTime();
                        photoInfo.shootTime = "";
                        photoInfo.shareURL = "";
                        photoInfo.isPayed = 1;
                        photoInfo.isVideo = downLoadInfo.getIsVideo();
                        photoInfo.isLove = 0;
                        photoInfo.fileSize = 0;
                        photoInfo.videoHeight = 0;
                        photoInfo.videoWidth = 0;
                        photoInfo.isHasPreset = 0;
                        photoInfo.isEncrypted = 0;
                        photoInfo.onLine = 0;
                        photoInfo.isChecked = 0;
                        photoInfo.isSelected = 0;
                        photoInfo.isUploaded = 0;
                        photoInfo.showMask = 0;
                        photoInfo.lastModify = 0l;
                        photoInfo.index = "";

                        photolist.add(photoInfo);
                    }
                }

                if (photolist.size() == 0) {
                    /**
                     * 图片为空的情况。
                     * 如果收到删除数据推送，本地数据处理完毕，
                     * 但是用户正好点进图片预览，这个时候会出现为空的情况，需要finish
                     */
                    PictureAirLog.out("no photos need return");
                    previewPhotoHandler.sendEmptyMessage(NO_PHOTOS);
                    return;
                }

                if (currentPosition == -1) {
                    String url = bundle.getString("path","");
                    currentPosition = 0;
                    if (!TextUtils.isEmpty(url)){
                        String path = Common.PHOTO_DOWNLOAD_PATH+AppUtil.getReallyFileName(url,0);
                        for (int i = 0; i < photolist.size(); i++) {
                            if (path.equals(photolist.get(i).photoPathOrURL)){
                                currentPosition = i;
                            }
                        }
                    }
                }

                previewPhotoHandler.sendEmptyMessage(7);
            }
        }.start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        PictureAirLog.e("DownloadPhotoPreviewActivity","onNewIntent");
        showPWProgressDialog();
        getPreviewPhotos(intent);
    }

    /**
     * 垂直模式
     */
    private void portraitOrientation() {
        isLandscape = false;
        titleBar.setVisibility(View.VISIBLE);
        if (mViewPager != null) {
            mViewPager.setBackgroundColor(getResources().getColor(R.color.pp_light_gray_background));
        }
        photoFraRelativeLayout.setBackgroundColor(getResources().getColor(R.color.pp_light_gray_background));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * 横屏模式
     */
    private void landscapeOrientation() {
        isLandscape = true;
        if (mViewPager != null) {
            mViewPager.setBackgroundColor(Color.BLACK);
        }
        photoFraRelativeLayout.setBackgroundColor(Color.BLACK);
        titleBar.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            PictureAirLog.out("landscape----->");
            landscapeOrientation();
        } else {
            PictureAirLog.out("portrait----->");
            portraitOrientation();
        }

        super.onConfigurationChanged(newConfig);

        String language = MyApplication.getInstance().getLanguageType();
        PictureAirLog.out("language------>" + language);
        Configuration config = getResources().getConfiguration();
        if (!language.equals("")) {//语言不为空
            if (language.equals(Common.ENGLISH)) {
                config.locale = Locale.US;
            } else if (language.equals(Common.SIMPLE_CHINESE)) {
                config.locale = Locale.SIMPLIFIED_CHINESE;
            }
        }
        PictureAirLog.out("new config---->" + config.locale);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        getResources().updateConfiguration(config, displayMetrics);
        PictureAirLog.out("update configuration done");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            doBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //退出app进行的判断，判断是否是栈中的唯一一个app，如果是，启动主页
    private void doBack() {
        // TODO Auto-generated method stub
        if (AppManager.getInstance().getActivityCount() == 1) {//一个activity的时候
            Intent intent = new Intent(this, MainTabActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.download_preview_back:
                doBack();
                break;
        }
    }

    /**
     * 更新底部索引工具
     */
    private void updateIndexTools(boolean isOnCreate) {
        PictureAirLog.v(TAG, "updateIndexTools-------->" + currentPosition);

        //更新序列号
        locationTextView.setText(String.format(getString(R.string.photo_index), currentPosition + 1,photolist.size()));

        PictureAirLog.out("set enable in other conditions");
        dismissPWProgressDialog();

        if (isLandscape) {//横屏模式
            if (mViewPager != null) {
                mViewPager.setBackgroundColor(Color.BLACK);
            }
        }
    }
}