package com.pictureair.photopass.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.pictureair.photopass.R;
import com.pictureair.photopass.customDialog.CustomDialog;
import com.pictureair.photopass.db.PictureAirDbManager;
import com.pictureair.photopass.entity.PhotoInfo;
import com.pictureair.photopass.service.DownloadService;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.DisneyVideoTool;
import com.pictureair.photopass.util.ScreenUtil;
import com.pictureair.photopass.widget.MyToast;
import com.pictureair.photopass.widget.SharePop;
import com.pictureair.photopass.widget.VideoPlayerView;
import com.pictureair.photopass.widget.VideoPlayerView.MySizeChangeLinstener;

import java.util.ArrayList;


/**
 * 播放视频（需要优化）
 *
 * @author bass
 */
public class VideoPlayerActivity extends Activity implements OnClickListener {
    private final static String TAG = "VideoPlayerActivity";
    private static final int UPDATE_UI = 2866;
    private SeekBar seekBar = null;
    private TextView durationTextView = null;
    private TextView playedTextView = null;
    private ImageButton btnPlayOrStop = null;
    private RelativeLayout rlHead,rlBackground;
    private LinearLayout llEnd;
    private ImageView ivBack, ivIsLove;
    private TextView tvLoding;
    private MyToast myToast;
    private SharePop sharePop;
    private SharedPreferences sharedPreferences;


    private Context context;
    private LinearLayout llControler, llShow, llShare, llDownload;
    private VideoPlayerView vv = null;

    private int playedTime;// 最小化 保存播放时间
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private static int controlHeight = 0;
    private final static int TIME = 3000;
    private boolean isControllerShow = true;
    private boolean isPaused = false;
    private final static int PROGRESS_CHANGED = 0;
    private final static int HIDE_CONTROLER = 1;
    private PhotoInfo videoInfo;
    private int mNetWorkType;  //当前网络的状态
    private CustomDialog customdialog; //  对话框
    private PictureAirDbManager pictureAirDbManager;


    public boolean isOnline = Common.isOnline;//测试

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROGRESS_CHANGED:// 进度改变
                    int i = vv.getCurrentPosition();
                    seekBar.setProgress(i);
                    if (isOnline) {
                        int j = vv.getBufferPercentage();
                        seekBar.setSecondaryProgress(j * seekBar.getMax() / 100);
                    } else {
                        seekBar.setSecondaryProgress(0);
                    }

                    i /= 1000;
                    int minute = i / 60;
                    int hour = minute / 60;
                    int second = i % 60;
                    minute %= 60;
                    // playedTextView.setText(String.format("%02d:%02d:%02d", hour,
                    // minute, second));
                    playedTextView.setText(String.format("%02d:%02d", minute,
                            second));
                    sendEmptyMessageDelayed(PROGRESS_CHANGED, 100);
                    break;
                case HIDE_CONTROLER:
                    hideController();
                    break;

                case UPDATE_UI:
                    Configuration cf = context.getResources().getConfiguration();
                    int ori = cf.orientation;
                    if (ori == cf.ORIENTATION_LANDSCAPE){
                        crossScreen();
                    }
                    else if (ori == cf.ORIENTATION_PORTRAIT){
                        verticalScreen();
                    }

                    //更新收藏图标
                    if (videoInfo.isLove == 1 || pictureAirDbManager.checkLovePhoto(videoInfo.photoId, sharedPreferences.getString(Common.USERINFO_ID, ""), videoInfo.photoPathOrURL)) {
                        videoInfo.isLove = 1;
                        ivIsLove.setImageResource(R.drawable.preview_photo_love_sele);
                    } else {
                        videoInfo.isLove = 0;
                        ivIsLove.setImageResource(R.drawable.preview_photo_love_nor);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager.getInstance().addActivity(this);
        setContentView(R.layout.activity_video_player);
        context = this;
        initView();
        initBtnEvent();// 所有按钮的事件˙
        myOnError();// 播放错误
        myVVSizeChangeLinstener();// 屏幕大小发生改变的情况
        getScreenSize();// 给底下菜单的布局
        initSeekBarEvent();// 进度条调整
//		 myGestureDetectr();//手势检测器
        initVVEvent();// VideoPlayerView类的回调
        startVideo();// 开始播放视频
    }


    private void initView() {
        videoInfo = (PhotoInfo)getIntent().getExtras().get(DisneyVideoTool.FROM_STORY);
        sharePop = new SharePop(context);
        pictureAirDbManager = new PictureAirDbManager(context);
        sharedPreferences = getSharedPreferences(Common.USERINFO_NAME, MODE_PRIVATE);
        myToast = new MyToast(context);

        rlBackground = (RelativeLayout)findViewById(R.id.rl_background);
        tvLoding = (TextView)findViewById(R.id.tv_loding);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        ivIsLove = (ImageView) findViewById(R.id.iv_isLove);
        llShare = (LinearLayout) findViewById(R.id.ll_share);
        llDownload = (LinearLayout) findViewById(R.id.ll_download);

        rlHead = (RelativeLayout) findViewById(R.id.ll_head);
        llEnd = (LinearLayout) findViewById(R.id.ll_end);
        llShow = (LinearLayout) findViewById(R.id.ll_show);
//        verticalScreen();

        // 控制栏的
        llControler = (LinearLayout) findViewById(R.id.ll_controler);
        durationTextView = (TextView) findViewById(R.id.duration);
        playedTextView = (TextView) findViewById(R.id.has_played);

        btnPlayOrStop = (ImageButton) findViewById(R.id.btn_play_or_stop);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        vv = (VideoPlayerView) findViewById(R.id.vv);

        btnPlayOrStop.setImageResource(R.drawable.play);
        btnPlayOrStop.setAlpha(0xBB);
        btnPlayOrStop.setVisibility(View.GONE);
        myHandler.sendEmptyMessage(UPDATE_UI);
    }

    /**
     * 所有按钮的事件
     */
    private void initBtnEvent() {
        ivBack.setOnClickListener(this);
        llShow.setOnClickListener(this);
        llShare.setOnClickListener(this);
        llDownload.setOnClickListener(this);
        ivIsLove.setOnClickListener(this);
    }

    private void startVideo() {
        vv.setVideoPath(Common.DATA_VIDEO);
        cancelDelayHide();
        hideControllerDelay();
    }

    private void initVVEvent() {
        vv.setMyMediapalerPrepared(new VideoPlayerView.myMediapalerPrepared() {
            @Override
            public void myOnrepared(MediaPlayer mp) {
                tvLoding.setVisibility(View.GONE);
            }
        });
        vv.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer arg0) {
                setVideoScale(SCREEN_DEFAULT);// 按比例（全屏）
                // setVideoScale(SCREEN_FULL);//全屏（会改变视频尺寸）
                if (isControllerShow) {
                    showController();
                }
                int i = vv.getDuration();
                Log.d("onCompletion", "" + i);
                seekBar.setMax(i);
                i /= 1000;
                int minute = i / 60;
                int hour = minute / 60;
                int second = i % 60;
                minute %= 60;
                durationTextView.setText(String.format("%02d:%02d", minute,
                        second));
                // durationTextView.setText(String.format("%02d:%02d:%02d",
                // hour, minute, second));

                vv.start();
                btnPlayOrStop.setImageResource(R.drawable.pause);
                hideControllerDelay();
                myHandler.sendEmptyMessage(PROGRESS_CHANGED);
            }
        });

        vv.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer arg0) {
//				vv.stopPlayback();
//				startVideo();
                vv.pause();
                btnPlayOrStop.setImageResource(R.drawable.play);
                cancelDelayHide();
                showController();
                isPaused = true;
            }
        });
    }

    private void initSeekBarEvent() {
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekbar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    if (!isOnline) {
                        vv.seekTo(progress);
                    }
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                myHandler.removeMessages(HIDE_CONTROLER);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
            }
        });
    }

    private void myVVSizeChangeLinstener() {
        vv.setMySizeChangeLinstener(new MySizeChangeLinstener() {
            @Override
            public void doMyThings() {
                setVideoScale(SCREEN_DEFAULT);
            }
        });
    }

    private void myOnError() {
        vv.setOnErrorListener(new OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                vv.stopPlayback();
                return false;
            }

        });
    }

    @Override
    protected void onPause() {
        playedTime = vv.getCurrentPosition();
        vv.pause();
        btnPlayOrStop.setImageResource(R.drawable.play);
        super.onPause();
    }

    @Override
    protected void onResume() {
        vv.seekTo(playedTime);
        vv.start();
        if (vv.isPlaying()) {
            btnPlayOrStop.setImageResource(R.drawable.pause);
            hideControllerDelay();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        myHandler.removeMessages(PROGRESS_CHANGED);
        myHandler.removeMessages(HIDE_CONTROLER);

        if (vv.isPlaying()) {
            vv.stopPlayback();
        }
        AppManager.getInstance().killActivity(this);
        super.onDestroy();
    }

    /**
     * 给底下菜单的布局
     */
    private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        screenHeight = display.getHeight();
        screenWidth = display.getWidth();
        controlHeight = screenHeight / 10;// 控制栏高的布局
    }

    private void hideControllerDelay() {
        myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
    }

    private void hideController() {
        if (llControler.getVisibility() == View.VISIBLE) {
            llControler.setVisibility(View.GONE);
            btnPlayOrStop.setVisibility(View.GONE);
        }
    }

    private void showController() {
        llControler.setVisibility(View.VISIBLE);
        btnPlayOrStop.setVisibility(View.VISIBLE);
        isControllerShow = true;
    }

    private void cancelDelayHide() {
        myHandler.removeMessages(HIDE_CONTROLER);
    }

    private final static int SCREEN_FULL = 0;
    private final static int SCREEN_DEFAULT = 1;

    // 设置可以全屏
    private void setVideoScale(int flag) {
        switch (flag) {
            case SCREEN_FULL:
                Log.d(TAG, "screenWidth: " + screenWidth + " screenHeight: "
                        + screenHeight);
                vv.setVideoScale(screenWidth, screenHeight);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;

            case SCREEN_DEFAULT:
                int videoWidth = vv.getVideoWidth();
                int videoHeight = vv.getVideoHeight();
                int mWidth = screenWidth;
                int mHeight = screenHeight - 25;
                if (videoWidth > 0 && videoHeight > 0) {
                    if (videoWidth * mHeight > mWidth * videoHeight) {
                        mHeight = mWidth * videoHeight / videoWidth;
                    } else if (videoWidth * mHeight < mWidth * videoHeight) {
                        mWidth = mHeight * videoWidth / videoHeight;
                    } else {

                    }
                }
                vv.setVideoScale(screenWidth, screenHeight);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;
        }
    }

    /**
     * 得到屏幕宽高
     */
    int height;
    int width;

    public void getDisplayMetrics() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;    //得到宽度
        height = dm.heightPixels;  //得到高度
    }

    /**
     * 当横竖屏切换的时候会直接调用onCreate方法中的 onConfigurationChanged方法
     */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        getScreenSize();
//		if (isControllerShow) {
//			cancelDelayHide();
//			hideController();
//			showController();
//			hideControllerDelay();
//		}
        getDisplayMetrics();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            crossScreen();//横屏计算大小
//			Toast.makeText(context, " 横屏", 100).show();
        } else {
            verticalScreen();//竖屏计算大小
//			Toast.makeText(context, " 竖屏", 100).show();
        }
        isPaused = false;

        super.onConfigurationChanged(newConfig);
    }

    private void crossScreen() {
        rlHead.setVisibility(View.GONE);
        llEnd.setVisibility(View.GONE);
        rlBackground.setBackgroundColor(getResources().getColor(R.color.black));
        ViewGroup.LayoutParams layoutParams = llShow.getLayoutParams();
        layoutParams.height = ScreenUtil.getScreenHeight(this);
        layoutParams.width = layoutParams.height * 4 / 3;
        llShow.setLayoutParams(layoutParams);

        setVideoScale(SCREEN_FULL);
        vv.start();
        btnPlayOrStop.setImageResource(R.drawable.pause);
//			cancelDelayHide();
        hideControllerDelay();
    }

    private void verticalScreen() {
        rlHead.setVisibility(View.VISIBLE);
        llEnd.setVisibility(View.VISIBLE);
        rlBackground.setBackgroundColor(getResources().getColor(R.color.gray_light));
        ViewGroup.LayoutParams layoutParams = llShow.getLayoutParams();
        layoutParams.width = ScreenUtil.getScreenWidth(this);
        layoutParams.height = layoutParams.width * 3 / 4;
        llShow.setLayoutParams(layoutParams);

        setVideoScale(SCREEN_DEFAULT);
        vv.start();
        btnPlayOrStop.setImageResource(R.drawable.pause);
//			cancelDelayHide();
        hideControllerDelay();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.ll_share:
                Toast.makeText(context,"is share",Toast.LENGTH_SHORT).show();

                break;
            case R.id.ll_download:
                downloadVideo();
                break;
            case R.id.iv_isLove:
                isLoveEvent();
                break;
            case R.id.iv_back:
                finish();
                break;
            case R.id.ll_show:
                // 单次处理
                if (isPaused) {
                    vv.start();
                    btnPlayOrStop.setImageResource(R.drawable.pause);
                    cancelDelayHide();
                    hideControllerDelay();
                } else {
                    vv.pause();
                    btnPlayOrStop.setImageResource(R.drawable.play);
                    cancelDelayHide();
                    showController();
                }
                isPaused = !isPaused;
                break;

            default:
                break;
        }
    }

    /**
     * 下载视频
     * 1.检查是否有网络
     * 2.查看本地是否存在
     * 3.查看网络类型 遵守 下载规则
     * 4.开始下载
     */
    private void downloadVideo() {
        if (AppUtil.getNetWorkType(context) == AppUtil.NETWORKTYPE_INVALID) {
            myToast.setTextAndShow(R.string.http_failed, Common.TOAST_SHORT_TIME);
            return;
        }
        if (videoInfo.onLine == 1) {//是pp的照片
            downLoadPhotos();
        } else {
            myToast.setTextAndShow(R.string.neednotdownload, Common.TOAST_SHORT_TIME);
        }
    }
    //判断网络类型  并做操作。
    public void downLoadPhotos() {
        mNetWorkType = AppUtil.getNetWorkType(getApplicationContext());
        if (mNetWorkType == AppUtil.NETWORKTYPE_MOBILE) {
            //如果是手机流量 ，弹出对话狂
            customdialog = new CustomDialog.Builder(context)
                    .setMessage(getResources().getString(R.string.dialog_download_message))
                    .setNegativeButton(getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            customdialog.dismiss();
                        }
                    })
                    .setPositiveButton(getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            downloadPic();
                            customdialog.dismiss();
                        }
                    })
                    .setCancelable(false)
                    .create();
            customdialog.show();
        } else if (mNetWorkType == AppUtil.NETWORKTYPE_WIFI) {
            downloadPic();
        } else {
            // 网络不可用
        }
    }
    //直接下载
    private void downloadPic() {
        ArrayList<PhotoInfo> list = new ArrayList<PhotoInfo>();
        list.add(videoInfo);
        Intent intent = new Intent(context, DownloadService.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("photos", list);
        intent.putExtras(bundle);
        startService(intent);
    }

    /**
     * 收藏事件
     * @return
     */
    public boolean isLoveEvent() {
        if (videoInfo.isLove == 1) {
            Log.d(TAG, "cancel love");
            pictureAirDbManager.setPictureLove(videoInfo.photoId, sharedPreferences.getString(Common.USERINFO_ID, ""), videoInfo.photoPathOrURL, false);
            videoInfo.isLove = 0;
            ivIsLove.setImageResource(R.drawable.preview_photo_love_nor);
        } else {
            Log.d(TAG, "add love");
            pictureAirDbManager.setPictureLove(videoInfo.photoId, sharedPreferences.getString(Common.USERINFO_ID, ""), videoInfo.photoPathOrURL, true);
            videoInfo.isLove = 1;
            ivIsLove.setImageResource(R.drawable.preview_photo_love_sele);
        }
        return true;
    }

    /*****************************************************************************************************************
     * 全屏点触事件****
     */
//	private void myGestureDetectr() {
//		mGestureDetector = new GestureDetector(new SimpleOnGestureListener() {
//
//			@Override
//			public boolean onDoubleTap(MotionEvent e) {
//				// 按两次
//				if (isFullScreen) {
//					setVideoScale(SCREEN_DEFAULT);
//				} else {
//					setVideoScale(SCREEN_FULL);
//				}
//				isFullScreen = !isFullScreen;
//				Log.d(TAG, "onDoubleTap");
//
//				if (isControllerShow) {
//					showController();
//				}
//				return true;
//			}
//
//			@Override
//			public boolean onSingleTapConfirmed(MotionEvent e) {
//				// 单次处理
//				if (!isControllerShow) {
//					showController();
//					hideControllerDelay();
//				} else {
//					cancelDelayHide();
//					hideController();
//				}
//				return true;
//			}
//
//			@Override
//			public void onLongPress(MotionEvent e) {
//				// 长按处理
//				if (isPaused) {
//					vv.start();
//					btnPlayOrStop.setImageResource(R.drawable.pause);
//					cancelDelayHide();
//					hideControllerDelay();
//				} else {
//					vv.pause();
//					btnPlayOrStop.setImageResource(R.drawable.play);
//					cancelDelayHide();
//					showController();
//				}
//				isPaused = !isPaused;
//			}
//		});
//	}
//
//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//
//		boolean result = mGestureDetector.onTouchEvent(event);
//
//		if (!result) {
//			if (event.getAction() == MotionEvent.ACTION_UP) {
//
//			}
//			result = super.onTouchEvent(event);
//		}
//		return result;
//	}
    /*****************************************************************************************************/

}
