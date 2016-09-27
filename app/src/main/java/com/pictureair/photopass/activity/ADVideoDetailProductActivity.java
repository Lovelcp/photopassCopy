package com.pictureair.photopass.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.entity.CartItemInfo;
import com.pictureair.photopass.entity.CartPhotosInfo;
import com.pictureair.photopass.entity.GoodsInfo;
import com.pictureair.photopass.entity.GoodsInfoJson;
import com.pictureair.photopass.entity.PhotoInfo;
import com.pictureair.photopass.util.ACache;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.JsonTools;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.SPUtils;
import com.pictureair.photopass.util.ScreenUtil;
import com.pictureair.photopass.widget.PWToast;
import com.pictureair.photopass.widget.videoPlayer.OnVideoPlayerViewEventListener;
import com.pictureair.photopass.widget.videoPlayer.PWVideoPlayerManagerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bauer_bao on 16/9/2.
 */
public class ADVideoDetailProductActivity extends BaseActivity implements View.OnClickListener, OnVideoPlayerViewEventListener {

    private LinearLayout animatedPhotoBackgroundLL, animatedPhotoBottomBtnsLL;
    private RelativeLayout animatedPhotoTopBarRl;
    private ImageView backImageView;
    private ImageView cartImageView;
    private TextView cartCountTextView, animatedPhotoIntroduce;
    private Button buyPPPBtn, upgradePPP, addPPPToCart;

    private PWToast pwToast;

    private PhotoInfo videoInfo;
    private String tabName;
    private int currentPosition;//记录当前预览照片的索引值

    //视频组件
    private PWVideoPlayerManagerView pwVideoPlayerManagerView;

    //加入购物车组件
    private ViewGroup animMaskLayout;//动画层
    private ImageView buyImg;// 这是在界面上跑的小图片

    private final static String TAG = ADVideoDetailProductActivity.class.getSimpleName();

    private int screenWidth = 0;
    private int screenHeight = 0;
    private boolean isOnline ;//网络true || 本地false
    private String videoPath;//视频本地路径 || 视频网络地址

    //商品数据
    private List<GoodsInfo> allGoodsList;//全部商品
    private GoodsInfo pppGoodsInfo;
    private String[] photoUrls;
    private boolean isBuyNow = false;

    private Handler adVideoHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case API1.GET_GOODS_SUCCESS:
                    GoodsInfoJson goodsInfoJson = JsonTools.parseObject(msg.obj.toString(), GoodsInfoJson.class);//GoodsInfoJson.getString()
                    if (goodsInfoJson != null && goodsInfoJson.getGoods().size() > 0) {
                        allGoodsList = goodsInfoJson.getGoods();
                        PictureAirLog.v(TAG, "goods size: " + allGoodsList.size());
                    }
                    //获取PP+
                    for (GoodsInfo goodsInfo : allGoodsList) {
                        if (goodsInfo.getName().equals(Common.GOOD_NAME_PPP)) {
                            pppGoodsInfo = goodsInfo;
                            //封装购物车宣传图
                            photoUrls = new String[goodsInfo.getPictures().size()];
                            for (int i = 0; i < goodsInfo.getPictures().size(); i++) {
                                photoUrls[i] = goodsInfo.getPictures().get(i).getUrl();
                            }
                            break;
                        }
                    }
                    dismissPWProgressDialog();
                    break;

                case API1.GET_PPPS_BY_SHOOTDATE_FAILED:
                case API1.GET_GOODS_FAILED:
                case API1.ADD_TO_CART_FAILED:
                    dismissPWProgressDialog();
                    pwToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    break;

                case API1.ADD_TO_CART_SUCCESS:
                    JSONObject jsonObject = (JSONObject) msg.obj;
                    int currentCartCount = SPUtils.getInt(ADVideoDetailProductActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, 0);
                    SPUtils.put(ADVideoDetailProductActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, currentCartCount + 1);

                    String cartId = jsonObject.getString("cartId");
                    dismissPWProgressDialog();
                    if (isBuyNow) {
                        MyApplication.getInstance().setIsBuyingPhotoInfo(null, null, videoInfo.photoPassCode, videoInfo.shootTime);
                        MyApplication.getInstance().setBuyPPPStatus(Common.FROM_AD_ACTIVITY);

                        //生成订单
                        Intent intent = new Intent(ADVideoDetailProductActivity.this, SubmitOrderActivity.class);
                        ArrayList<CartItemInfo> orderinfoArrayList = new ArrayList<>();
                        CartItemInfo cartItemInfo = new CartItemInfo();
                        cartItemInfo.setCartId(cartId);
                        cartItemInfo.setProductName(pppGoodsInfo.getName());
                        cartItemInfo.setProductNameAlias(pppGoodsInfo.getNameAlias());
                        cartItemInfo.setUnitPrice(pppGoodsInfo.getPrice());
                        cartItemInfo.setEmbedPhotos(new ArrayList<CartPhotosInfo>());
                        cartItemInfo.setDescription(pppGoodsInfo.getDescription());
                        cartItemInfo.setQty(1);
                        cartItemInfo.setStoreId(pppGoodsInfo.getStoreId());
                        cartItemInfo.setPictures(photoUrls);
                        cartItemInfo.setPrice(pppGoodsInfo.getPrice());
                        cartItemInfo.setCartProductType(3);

                        orderinfoArrayList.add(cartItemInfo);
                        intent.putExtra("orderinfo", orderinfoArrayList);
                        startActivity(intent);
                    } else {
                        buyImg = new ImageView(ADVideoDetailProductActivity.this);// buyImg是动画的图片
                        buyImg.setImageResource(R.drawable.addtocart);// 设置buyImg的图片
                        setAnim(buyImg);
                    }
                    break;

                case API1.GET_PPPS_BY_SHOOTDATE_SUCCESS:  //根据已有PP＋升级
                    dismissPWProgressDialog();
                    if (API1.PPPlist.size() > 0) {
                        //将 tabname 存入sp
                        SPUtils.put(ADVideoDetailProductActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, "tabName", tabName);
                        SPUtils.put(ADVideoDetailProductActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, "currentPosition", currentPosition);

                        Intent intent = new Intent(ADVideoDetailProductActivity.this, SelectPPActivity.class);
                        intent.putExtra("photoPassCode", videoInfo.photoPassCode);
                        intent.putExtra("shootTime", videoInfo.shootTime);
                        startActivity(intent);
                    } else {
                        pwToast.setTextAndShow(R.string.no_ppp_tips, Common.TOAST_SHORT_TIME);
                    }
                    break;

                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advideo_detail_product);

        initView();
        initData();

        Configuration cf = getResources().getConfiguration();
        int ori = cf.orientation;
        adjustScreenUI(ori == cf.ORIENTATION_LANDSCAPE);

        if (isOnline && AppUtil.getNetWorkType(this) == AppUtil.NETWORKTYPE_INVALID) {
            pwVideoPlayerManagerView.setLoadingText(R.string.no_network);
        } else {
            pwVideoPlayerManagerView.startPlayVideo(videoPath, isOnline);// 开始播放视频
        }
    }

    private void initView() {
        backImageView = (ImageView) findViewById(R.id.rt);
        cartImageView = (ImageView) findViewById(R.id.button_bag);
        cartCountTextView = (TextView) findViewById(R.id.textview_cart_count);
        buyPPPBtn = (Button) findViewById(R.id.animated_photo_buy_ppp_btn);
        upgradePPP = (Button) findViewById(R.id.animated_photo_upgrade_ppp_btn);
        addPPPToCart = (Button) findViewById(R.id.animated_photo_add_cart_btn);
        animatedPhotoTopBarRl = (RelativeLayout) findViewById(R.id.animated_photo_top_rl);
        animatedPhotoIntroduce = (TextView) findViewById(R.id.animated_photo_product_detail);
        animatedPhotoBottomBtnsLL = (LinearLayout) findViewById(R.id.animated_photo_bottom_btns_ll);
        animatedPhotoBackgroundLL = (LinearLayout) findViewById(R.id.animated_photo_backtground_ll);
        pwVideoPlayerManagerView = (PWVideoPlayerManagerView) findViewById(R.id.animated_photo_pwvideo_pmv);

        backImageView.setOnClickListener(this);
        cartImageView.setOnClickListener(this);
        cartCountTextView.setOnClickListener(this);
        buyPPPBtn.setOnClickListener(this);
        upgradePPP.setOnClickListener(this);
        addPPPToCart.setOnClickListener(this);
        pwVideoPlayerManagerView.setOnClickListener(this);
        pwVideoPlayerManagerView.setOnVideoPlayerViewEventListener(this);
    }

    private void initData() {
        pwToast = new PWToast(this);
        videoInfo = (PhotoInfo) getIntent().getExtras().get("videoInfo");
        Bundle bundle = getIntent().getBundleExtra("bundle");
        currentPosition = bundle.getInt("position", 0);
        tabName = bundle.getString("tab");

        getScreenSize();
        getReallyVideoUrl();
        setVideoResolution(true);
        showPWProgressDialog(true);
        getGoods();
    }

    private void getGoods() {
        String goodsByACache = ACache.get(this).getAsString(Common.ALL_GOODS);
        PictureAirLog.v(TAG, "initData: goodsByACache: " + goodsByACache);
        if (goodsByACache != null && !goodsByACache.equals("")) {
            adVideoHandler.obtainMessage(API1.GET_GOODS_SUCCESS, goodsByACache).sendToTarget();
        } else {
            //从网络获取商品,先检查网络
            if (AppUtil.getNetWorkType(MyApplication.getInstance()) != 0) {
                API1.getGoods(adVideoHandler);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rt://返回按钮
                finish();
                break;

            case R.id.button_bag://购物车按钮
            case R.id.textview_cart_count:
                if (AppUtil.getNetWorkType(MyApplication.getInstance()) == AppUtil.NETWORKTYPE_INVALID) {
                    pwToast.setTextAndShow(R.string.no_network, Common.TOAST_SHORT_TIME);
                    return;
                }
                Intent intent = new Intent(ADVideoDetailProductActivity.this, CartActivity.class);
                startActivity(intent);
                break;

            case R.id.animated_photo_buy_ppp_btn://购买ppp
                if (AppUtil.getNetWorkType(MyApplication.getInstance()) == AppUtil.NETWORKTYPE_INVALID) {
                    pwToast.setTextAndShow(R.string.no_network, Common.TOAST_SHORT_TIME);
                    return;
                }
                //购买按钮，需要将当前商品的类型和单价存储起来
                isBuyNow = true;//立即购买
                addtocart();
                break;

            case R.id.animated_photo_upgrade_ppp_btn://使用已存在的ppp升级
                if (AppUtil.getNetWorkType(MyApplication.getInstance()) == AppUtil.NETWORKTYPE_INVALID) { //判断网络情况。
                    pwToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    return;
                }else{
                    showPWProgressDialog();
                    API1.getPPPsByShootDate(adVideoHandler, videoInfo.shootTime);
                }
                break;

            case R.id.animated_photo_add_cart_btn://把ppp加入购物车
                if (AppUtil.getNetWorkType(MyApplication.getInstance()) == AppUtil.NETWORKTYPE_INVALID) {
                    pwToast.setTextAndShow(R.string.no_network, Common.TOAST_SHORT_TIME);
                    return;
                }
                //加入购物车，会有动画效果,如果没有登录，先提示登录
                isBuyNow = false;
                addtocart();
                break;

            case R.id.animated_photo_pwvideo_pmv:
                PictureAirLog.out("click video");
                // 单次处理
                if (pwVideoPlayerManagerView.isPaused()) {
                    pwVideoPlayerManagerView.resumeVideo();
                } else {
                    pwVideoPlayerManagerView.pausedVideo();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        getScreenSize();
        adjustScreenUI(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);//调整UI
        setPausedOrPlay();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pwVideoPlayerManagerView.pausedVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartCount();
    }

    @Override
    protected void onDestroy() {
        pwVideoPlayerManagerView.stopVideo();
        adVideoHandler.removeCallbacksAndMessages(null);

        if (!MyApplication.getInstance().getBuyPPPStatus().equals(Common.FROM_AD_ACTIVITY_PAYED)) {//如果已经购买完成，则不需要清除数据，否则才会清除
            MyApplication.getInstance().setBuyPPPStatus("");
            //按返回，把状态全部清除
            MyApplication.getInstance().clearIsBuyingPhotoList();
        }
        super.onDestroy();
    }

    /**
     * 更新购物车数量
     */
    private void updateCartCount() {
        // TODO Auto-generated method stub
        int recordcount = SPUtils.getInt(this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, 0);
        if (recordcount <= 0) {
            cartCountTextView.setVisibility(View.INVISIBLE);
        } else {
            cartCountTextView.setVisibility(View.VISIBLE);
            cartCountTextView.setText(recordcount + "");
        }
    }

    private void getReallyVideoUrl(){
        String fileName = AppUtil.getReallyFileName(videoInfo.photoPathOrURL, 1);
        PictureAirLog.e(TAG, "filename=" + fileName);
        File filedir = new File(Common.PHOTO_DOWNLOAD_PATH);
        filedir.mkdirs();
        final File file = new File(filedir + "/" + fileName);
        if (!file.exists()) {
            videoPath = videoInfo.adURL;
            PictureAirLog.v(TAG, " 网络播放:"+videoPath);
            isOnline = true;
        } else {
            PictureAirLog.v(TAG, " 本地播放");
            videoPath = file.getPath();
            isOnline = false;
        }
        if (videoInfo.videoHeight == 0) {
            videoInfo.videoHeight = 480;
        }
        if (videoInfo.videoWidth == 0) {
            videoInfo.videoWidth = 480;
        }
    }

    /**
     * 获取屏幕宽高
     */
    private void getScreenSize() {
        screenHeight = ScreenUtil.getScreenHeight(this);
        screenWidth = ScreenUtil.getScreenWidth(this);
    }


    @Override
    public void setVideoScale(int flag) {
        switch (flag) {
            case PWVideoPlayerManagerView.SCREEN_FULL:
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;

            case PWVideoPlayerManagerView.SCREEN_DEFAULT:
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                break;
        }
    }

    @Override
    public void setControllerVisible(boolean visible) {

    }

    @Override
    public void onError() {
        pwVideoPlayerManagerView.setLoadingText(R.string.http_error_code_401);
    }

    /**
     * 调整ui
     * @param isLandscape 是不是横屏模式
     */
    private void adjustScreenUI(boolean isLandscape) {
        animatedPhotoTopBarRl.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        animatedPhotoIntroduce.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        animatedPhotoBottomBtnsLL.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        animatedPhotoBackgroundLL.setBackgroundColor(ContextCompat.getColor(this, isLandscape ? R.color.black : R.color.gray_light));
        setVideoResolution(!isLandscape);
        setVideoScale(isLandscape ? PWVideoPlayerManagerView.SCREEN_FULL : PWVideoPlayerManagerView.SCREEN_DEFAULT);
    }

    /**
     * 设置视频显示尺寸
     *
     * @param isVertical  竖屏？横屏
     */
    private void setVideoResolution(boolean isVertical) {
        ViewGroup.LayoutParams layoutParams = pwVideoPlayerManagerView.getLayoutParams();
        int videoHeight;
        if (isVertical) {//竖屏
            layoutParams.width = ScreenUtil.getScreenWidth(this);
            layoutParams.height = layoutParams.width * videoInfo.videoHeight / videoInfo.videoWidth;
            videoHeight = layoutParams.height;
        } else {//横屏
            if (AppUtil.getOrientationMarginByAspectRatio(videoInfo.videoWidth,
                    videoInfo.videoHeight, screenWidth, screenHeight) == AppUtil.HORIZONTAL_MARGIN) {//水平留白
                layoutParams.height = screenHeight;
                layoutParams.width = layoutParams.height * videoInfo.videoWidth / videoInfo.videoHeight;
                videoHeight = layoutParams.height;
            } else {//上下留白
                layoutParams.width = screenWidth;
                layoutParams.height = screenHeight;
                videoHeight = layoutParams.width * videoInfo.videoHeight / videoInfo.videoWidth;
            }
        }
        pwVideoPlayerManagerView.setVideoScale(layoutParams.width, videoHeight);
        pwVideoPlayerManagerView.setLayoutParams(layoutParams);
    }

    private void setPausedOrPlay() {
        if (pwVideoPlayerManagerView.isPaused()) {
            pwVideoPlayerManagerView.pausedVideo();
        } else {
            pwVideoPlayerManagerView.resumeVideo();
        }
    }

    /**
     * 添加购物车
     */
    private void addtocart() {
        showPWProgressDialog();
        //调用addToCart API1
        API1.addToCart(pppGoodsInfo.getGoodsKey(), 1, isBuyNow, null, adVideoHandler);
    }

    /**
     * 设置添加购物车动画
     *
     * @param v
     */
    private void setAnim(final View v) {
        animMaskLayout = null;
        animMaskLayout = createAnimLayout();
        int[] start_location = new int[2];// 一个整型数组，用来存储按钮的在屏幕的X、Y坐标
        start_location[0] = ScreenUtil.getScreenWidth(this) / 2 - Common.CART_WIDTH;//减去的值和图片大小有关系
        start_location[1] = ScreenUtil.getScreenHeight(this) / 2 - Common.CART_HEIGHT;
        // 将组件添加到我们的动画层上
        final View view = addViewToAnimLayout(animMaskLayout, v, start_location);
        int[] end_location = new int[2];
        cartCountTextView.getLocationInWindow(end_location);
        // 计算位移
        final int endX = end_location[0] - start_location[0];
        final int endY = end_location[1] - start_location[1];

        ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setInterpolator(new LinearInterpolator());//匀速
        scaleAnimation.setRepeatCount(0);//不重复
        scaleAnimation.setFillAfter(true);//停在最后动画
        AnimationSet set = new AnimationSet(false);
        set.setFillAfter(false);
        set.addAnimation(scaleAnimation);
        set.setDuration(500);//动画整个时间
        view.startAnimation(set);//开始动画
        set.setAnimationListener(new Animation.AnimationListener() {
            // 动画的开始
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            // 动画的结束
            @Override
            public void onAnimationEnd(Animation animation) {
                //x轴的路径动画，匀速
                TranslateAnimation translateAnimationX = new TranslateAnimation(0, endX, 0, 0);
                translateAnimationX.setInterpolator(new LinearInterpolator());
                translateAnimationX.setRepeatCount(0);// 动画重复执行的次数
                //y轴的路径动画，加速
                TranslateAnimation translateAnimationY = new TranslateAnimation(0, 0, 0, endY);
                translateAnimationY.setInterpolator(new AccelerateInterpolator());
                translateAnimationY.setRepeatCount(0);// 动画重复执行的次数
                ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 0.1f, 1.0f, 0.1f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
                AnimationSet set2 = new AnimationSet(false);
                //要先添加形状的，后添加位移的，不然动画效果不能达到要求
                set2.addAnimation(scaleAnimation);
                set2.addAnimation(translateAnimationY);
                set2.addAnimation(translateAnimationX);

                set2.setFillAfter(false);
                set2.setStartOffset(200);//等待时间
                set2.setDuration(800);// 动画的执行时间
                view.startAnimation(set2);
                set2.setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        v.setVisibility(View.GONE);//控件消失
                        int i = SPUtils.getInt(ADVideoDetailProductActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, 0);
                        if (i <= 0) {
                            cartCountTextView.setVisibility(View.INVISIBLE);
                        } else {
                            cartCountTextView.setVisibility(View.VISIBLE);
                            cartCountTextView.setText(i + "");
                        }
                    }
                });
            }
        });
    }

    private ViewGroup createAnimLayout() {
        ViewGroup rootView = (ViewGroup) this.getWindow().getDecorView();
        LinearLayout animLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;

        animLayout.setLayoutParams(lp);
        animLayout.setBackgroundResource(android.R.color.transparent);
        rootView.addView(animLayout);
        return animLayout;
    }

    private View addViewToAnimLayout(ViewGroup vg, View view, int[] location) {
        int x = location[0];
        int y = location[1];
        vg.addView(view);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = x;
        lp.topMargin = y;
        view.setLayoutParams(lp);
        return view;
    }
}