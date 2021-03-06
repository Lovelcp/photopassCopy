package com.pictureair.photopass.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pictureair.photopass.GalleryWidget.GalleryViewPager;
import com.pictureair.photopass.GalleryWidget.PhotoEventListener;
import com.pictureair.photopass.GalleryWidget.UrlPagerAdapter;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.greendao.PictureAirDbManager;
import com.pictureair.photopass.entity.PhotoDownLoadInfo;
import com.pictureair.photopass.entity.PhotoInfo;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.SPUtils;
import com.pictureair.photopass.widget.PWToast;
import com.pictureair.photopass.widget.SharePop;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 下载的图片预览页面
 * Created by pengwu on 16/7/29.
 */
public class DownloadPhotoPreviewActivity extends BaseActivity implements View.OnClickListener,PhotoEventListener {

    private TextView locationTextView;
    private GalleryViewPager mViewPager;
    private ImageView returnImageView;

    private ImageButton shareImgBtn;

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

    private SharePop sharePop;

    private boolean isShareDialogShowing = false;

    private int shareType = 0;

    private Handler previewPhotoHandler;

    private PWToast pwToast;

    public static final int NO_PHOTOS = 2323;
    private static final int CHECK_EXIST = 2324;

    @Override
    public void videoClick(int position) {

        PhotoInfo info = photolist.get(position);
        String fileName = AppUtil.getReallyFileName(info.getPhotoOriginalURL(), info.getIsVideo());
        PictureAirLog.e(TAG, "filename=" + fileName);
        File file = new File(Common.PHOTO_DOWNLOAD_PATH + "/" + fileName);
        if (!file.exists()){
            pwToast.setTextAndShow(R.string.photo_download_not_exists,PWToast.LENGTH_SHORT);
        }else {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("from_story", info);
            startActivity(intent);
            overridePendingTransition(R.anim.activity_fadein, R.anim.activity_fadeout);
        }
    }

    @Override
    public void buyClick(int position) {

    }

    @Override
    public void longClick(int position) {

    }

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
            case CHECK_EXIST://开始检查是否存在
                if (msg.arg1 >= photolist.size() || msg.arg1 != currentPosition) {
                    break;
                }

                PhotoInfo info = photolist.get(msg.arg1);
                File file;
                if (info.getIsVideo() == 1) {//视频
                    String fileName = AppUtil.getReallyFileName(info.getPhotoOriginalURL(), info.getIsVideo());
                    file = new File(Common.PHOTO_DOWNLOAD_PATH + "/" + fileName);
                } else {//照片
                    file = new File(info.getPhotoOriginalURL());
                }

                //更新收藏图标
                if (msg.arg1 == currentPosition && file.exists()) {//数据库查询的数据是true，并且对应的index还是之前的位置
                    shareImgBtn.setVisibility(View.VISIBLE);
                } else {
                    shareImgBtn.setVisibility(View.INVISIBLE);
                }
                break;

            case SharePop.TWITTER:
                shareType = msg.what;
                break;

            case SharePop.DISMISS_DIALOG:
                isShareDialogShowing = false;
                dismissPWProgressDialog();
                break;

            case SharePop.SHOW_DIALOG:
                isShareDialogShowing = true;
                showPWProgressDialog(null);
                break;

            case 7:
                mViewPager = (GalleryViewPager) findViewById(R.id.download_preview_viewer);
                UrlPagerAdapter pagerAdapter = new UrlPagerAdapter(DownloadPhotoPreviewActivity.this, photolist, 1, false);
                pagerAdapter.setOnPhotoEventListener(this);
                mViewPager.setOffscreenPageLimit(2);
                mViewPager.setAdapter(pagerAdapter);
                mViewPager.setCurrentItem(currentPosition, true);
                //初始化底部索引按钮
                updateIndexTools();

                PictureAirLog.v(TAG, "----------------------->initing...3");

                mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

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
                            updateIndexTools();
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
        sharePop = new SharePop(this);
        PictureAirLog.out("oncreate----->2");
        returnImageView = (ImageView) findViewById(R.id.download_preview_back);
        locationTextView = (TextView) findViewById(R.id.download_preview_title);
        photoFraRelativeLayout = (RelativeLayout) findViewById(R.id.download_preview_fra_layout);
        titleBar = (RelativeLayout) findViewById(R.id.download_preview_titlebar);
        shareImgBtn = (ImageButton) findViewById(R.id.download_preview_share);
        pwToast = new PWToast(MyApplication.getInstance());
        shareImgBtn.setOnClickListener(this);
        returnImageView.setOnClickListener(this);
        PictureAirLog.v(TAG, "----------------------->initing...1");

        Configuration cf = getResources().getConfiguration();
        int ori = cf.orientation;
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
            landscapeOrientation();
        }
        showPWProgressDialog(R.string.is_loading);
        getPreviewPhotos(getIntent());
    }

    private void getPreviewPhotos(Intent intent) {
        final Intent intent1 = intent;
        new Thread(){
            @Override
            public void run() {
                Bundle bundle = intent1.getExtras();
                currentPosition = bundle.getInt("position", 0);
                photolist = new ArrayList<>();

                String userId = SPUtils.getString(DownloadPhotoPreviewActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.USERINFO_ID, "");
                List<PhotoDownLoadInfo> photos = PictureAirDbManager.getPhotosOrderByTime(userId,"true");
                if (photos != null && photos.size() >0) {
                    for (int i=0;i<photos.size();i++) {
                        PhotoDownLoadInfo downLoadInfo = photos.get(i);

                        PhotoInfo photoInfo = new PhotoInfo();
                        photoInfo.setPhotoId(downLoadInfo.getPhotoId());

                        String loadUrl;
                        //如果文件名过长导致无法保存，会将文件名进行MD5，数据库中保存在FailedTime字段，此处是对这种情况的特殊处理
                        if (TextUtils.isEmpty(downLoadInfo.getFailedTime())){
                            String fileName = AppUtil.getReallyFileName(downLoadInfo.getUrl(), downLoadInfo.getIsVideo());
                            loadUrl = Common.PHOTO_DOWNLOAD_PATH + fileName;
                        }else{
                            loadUrl = downLoadInfo.getFailedTime();
                        }
                        if (downLoadInfo.getIsVideo() == 0) {
                            photoInfo.setPhotoOriginalURL(loadUrl);
                        }else{
                            photoInfo.setPhotoOriginalURL(downLoadInfo.getUrl());
                        }
                        photoInfo.setId(1L);
                        photoInfo.setPhotoThumbnail_128(downLoadInfo.getPreviewUrl());
                        photoInfo.setPhotoThumbnail_512(downLoadInfo.getPhotoThumbnail_512());
                        photoInfo.setPhotoThumbnail_1024(downLoadInfo.getPhotoThumbnail_1024());
                        photoInfo.setStrShootOn(downLoadInfo.getShootTime());
                        photoInfo.setIsPaid(1);
                        photoInfo.setIsVideo(downLoadInfo.getIsVideo());
                        photoInfo.setVideoHeight(downLoadInfo.getVideoHeight());
                        photoInfo.setVideoWidth(downLoadInfo.getVideoWidth());
                        photoInfo.setIsEnImage(1);
                        photoInfo.setIsOnLine(0);
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
                            if (path.equals(photolist.get(i).getPhotoOriginalURL())){
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
        showPWProgressDialog(R.string.is_loading);
        getPreviewPhotos(intent);
    }

    /**
     * 垂直模式
     */
    private void portraitOrientation() {
        isLandscape = false;
        titleBar.setVisibility(View.VISIBLE);
        if (mViewPager != null) {
            mViewPager.setBackgroundColor(ContextCompat.getColor(this, R.color.pp_light_gray_background));
        }
        photoFraRelativeLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.pp_light_gray_background));
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
        if (mViewPager != null) {
            mViewPager.resetImageView();
        }
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
                if (Build.VERSION.SDK_INT < 24) {
                    config.locale = Locale.US;
                } else {
                    config.setLocale(Locale.US);
                }
            } else if (language.equals(Common.SIMPLE_CHINESE)) {
                if (Build.VERSION.SDK_INT < 24) {
                    config.locale = Locale.SIMPLIFIED_CHINESE;
                } else {
                    config.setLocale(Locale.SIMPLIFIED_CHINESE);
                }
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

            case R.id.download_preview_share:
                sharePop.setShareInfo(photolist.get(mViewPager.getCurrentItem()), true, previewPhotoHandler);
                sharePop.showAtLocation(v, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                break;

            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sharePop != null) {
            PictureAirLog.out("sharePop not null");
            if (shareType != SharePop.TWITTER && isShareDialogShowing) {
                PictureAirLog.out("dismiss dialog");
                dismissPWProgressDialog();
            }
        }
    }

    /**
     * 更新底部索引工具
     */
    private void updateIndexTools() {
        PictureAirLog.v(TAG, "updateIndexTools-------->" + currentPosition);
        previewPhotoHandler.obtainMessage(CHECK_EXIST, currentPosition, 0).sendToTarget();
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
