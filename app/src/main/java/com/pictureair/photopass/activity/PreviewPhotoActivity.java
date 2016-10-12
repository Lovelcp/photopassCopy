package com.pictureair.photopass.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pictureair.photopass.GalleryWidget.GalleryViewPager;
import com.pictureair.photopass.GalleryWidget.PhotoEventListener;
import com.pictureair.photopass.GalleryWidget.UrlPagerAdapter;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.controller.GetLastestVideoInfoPresenter;
import com.pictureair.photopass.controller.IGetLastestVideoInfoView;
import com.pictureair.photopass.customDialog.PWDialog;
import com.pictureair.photopass.db.PictureAirDbManager;
import com.pictureair.photopass.entity.CartItemInfo;
import com.pictureair.photopass.entity.CartItemInfoJson;
import com.pictureair.photopass.entity.CartPhotosInfo;
import com.pictureair.photopass.entity.DiscoverLocationItemInfo;
import com.pictureair.photopass.entity.GoodsInfo;
import com.pictureair.photopass.entity.GoodsInfoJson;
import com.pictureair.photopass.entity.PhotoInfo;
import com.pictureair.photopass.service.DownloadService;
import com.pictureair.photopass.util.ACache;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.JsonTools;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.ReflectionUtil;
import com.pictureair.photopass.util.SPUtils;
import com.pictureair.photopass.util.ScreenUtil;
import com.pictureair.photopass.util.SettingUtil;
import com.pictureair.photopass.util.UmengUtil;
import com.pictureair.photopass.widget.PWToast;
import com.pictureair.photopass.widget.SharePop;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * 预览图片，可以进行编辑，分享，下载和制作礼物的操作
 *
 * @author bauer_bao
 */
public class PreviewPhotoActivity extends BaseActivity implements OnClickListener, Handler.Callback,
        PWDialog.OnPWDialogClickListener, PhotoEventListener, IGetLastestVideoInfoView {
    private SettingUtil settingUtil;
    //工具条
    private TextView editButton;
    private TextView shareButton;
    private TextView downloadButton;
    private TextView makegiftButton;

    private TextView locationTextView;

    private GalleryViewPager mViewPager;
    private ImageView returnImageView;

    private ImageButton loveImageButton;

    private PWToast newToast;
    private SharePop sharePop;
    private MyApplication myApplication;
    private PictureAirDbManager pictureAirDbManager;
    private PhotoInfo photoInfo;

    private RelativeLayout titleBar;
    private LinearLayout toolsBar, indexBar;
    private static final String TAG = "PreviewPhotoActivity";

    private int shareType = 0;

    //图片显示框架
    private ArrayList<PhotoInfo> photolist;
    private ArrayList<PhotoInfo> targetphotolist;
    private ArrayList<DiscoverLocationItemInfo> locationList = new ArrayList<DiscoverLocationItemInfo>();
    private int currentPosition;//记录当前预览照片的索引值

    private boolean isEdited = false;
    private String tabName;

    /**
     * 是否是横屏模式
     */
    private boolean isLandscape = false;

    //底部切换索引按钮
    private TextView lastPhotoImageView;
    private TextView nextPhotoImageView;
    private TextView currentPhotoIndexTextView;
    private TextView currentPhotoInfoTextView;
    private TextView currentPhotoADTextView;

    private RelativeLayout photoFraRelativeLayout;

    private Dialog dia;
    private TextView buy_ppp, cancel, buynow, use_ppp, touchtoclean;

    private Date date;
    private SimpleDateFormat simpleDateFormat;
    private CartItemInfoJson cartItemInfoJson;//存放意见购买后的购物信息

    private static final int CHECK_FAVORITE = 666;
    private static final int GET_FAVORITE_DATA_DONE = 1000;
    private static final int GET_LOCATION_AD = 777;
    private static final int GET_LOCATION_AD_DONE = 1001;
    private static final int CREATE_BLUR_DIALOG = 888;
    private static final int NO_PHOTOS_AND_RETURN = 1002;

    private static final int LOCAL_PHOTO_EDIT_DIALOG = 1003;
    private static final int FRAME_PHOTO_EDIT_DIALOG = 1004;
    private static final int GO_SETTING_DIALOG = 1005;
    private static final int DOWNLOAD_DIALOG = 1006;
    private static final int GO_DOWNLOAD_ACTIVITY_DIALOG = 1007;
    private static final int VIDEO_STILL_MAKING_DIALOG = 1008;

    private PWDialog pictureWorksDialog;

    private List<GoodsInfo> allGoodsList;//全部商品
    private GoodsInfo pppGoodsInfo;
    private String[] photoUrls;

    private Handler previewPhotoHandler;

    //点击视频播放的处理对象
    private GetLastestVideoInfoPresenter lastestVideoInfoPresenter;

    /**
     * 处理Message
     *
     * @param msg
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case SharePop.TWITTER:
                shareType = msg.what;
                break;

            case API1.BUY_PHOTO_SUCCESS:
                dismissPWProgressDialog();
                cartItemInfoJson = JsonTools.parseObject((JSONObject) msg.obj, CartItemInfoJson.class);//CartItemInfoJson.getString()
                if (cartItemInfoJson.getItems().size() == 0) {
                    break;
                }
                PictureAirLog.v(TAG, "BUY_PHOTO_SUCCESS" + cartItemInfoJson.toString());
                //将当前购买的照片信息存放到application中
                myApplication.setIsBuyingPhotoInfo(photolist.get(currentPosition).photoId, tabName, null, null);
                if (myApplication.getRefreshViewAfterBuyBlurPhoto().equals(Common.FROM_MYPHOTOPASS)) {
                } else if (myApplication.getRefreshViewAfterBuyBlurPhoto().equals(Common.FROM_VIEWORSELECTACTIVITY)) {
                } else {
                    myApplication.setRefreshViewAfterBuyBlurPhoto(Common.FROM_PREVIEW_PHOTO_ACTIVITY);
                }
                List<CartItemInfo> cartItemInfoList = cartItemInfoJson.getItems();
                Intent intent = new Intent(PreviewPhotoActivity.this, SubmitOrderActivity.class);
                ArrayList<CartItemInfo> orderinfo = new ArrayList<>();
                CartItemInfo cartItemInfo = cartItemInfoList.get(0);
                cartItemInfo.setCartProductType(2);
                orderinfo.add(cartItemInfo);
                int curCarts = SPUtils.getInt(this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, 0);
                SPUtils.put(this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, curCarts + 1);
                intent.putExtra("orderinfo", orderinfo);
                startActivity(intent);
                break;

            case API1.BUY_PHOTO_FAILED:
                //购买失败
                dismissPWProgressDialog();
                newToast.setTextAndShow(ReflectionUtil.getStringId(MyApplication.getInstance(), msg.arg1), Common.TOAST_SHORT_TIME);
                break;

            case API1.GET_GOODS_SUCCESS:
                GoodsInfoJson goodsInfoJson = JsonTools.parseObject(msg.obj.toString(), GoodsInfoJson.class);//GoodsInfoJson.getString()
                if (goodsInfoJson != null && goodsInfoJson.getGoods() != null && goodsInfoJson.getGoods().size() > 0) {
                    allGoodsList = goodsInfoJson.getGoods();
                    PictureAirLog.v(TAG, "goods size: " + allGoodsList.size());
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
                    API1.addToCart(pppGoodsInfo.getGoodsKey(), 1, true, null, previewPhotoHandler);
                }
                break;

            case API1.GET_GOODS_FAILED:
                dismissPWProgressDialog();
                newToast.setTextAndShow(ReflectionUtil.getStringId(MyApplication.getInstance(), msg.arg1), Common.TOAST_SHORT_TIME);
                break;

            case API1.ADD_TO_CART_FAILED:
                dismissPWProgressDialog();
                newToast.setTextAndShow(ReflectionUtil.getStringId(MyApplication.getInstance(), msg.arg1), Common.TOAST_SHORT_TIME);

                break;

            case API1.ADD_TO_CART_SUCCESS:
                dismissPWProgressDialog();
                JSONObject jsonObject = (JSONObject) msg.obj;
                int currentCartCount = SPUtils.getInt(this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, 0);
                SPUtils.put(this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, currentCartCount + 1);
                String cartId = jsonObject.getString("cartId");

                myApplication.setIsBuyingPhotoInfo(null, null, photolist.get(currentPosition).photoPassCode, photolist.get(currentPosition).shootTime);
                myApplication.setBuyPPPStatus(Common.FROM_PREVIEW_PPP_ACTIVITY);

                //生成订单
                Intent intent1 = new Intent(PreviewPhotoActivity.this, SubmitOrderActivity.class);
                ArrayList<CartItemInfo> orderinfoArrayList = new ArrayList<>();
                CartItemInfo cartItemInfo1 = new CartItemInfo();
                cartItemInfo1.setCartId(cartId);
                cartItemInfo1.setProductName(pppGoodsInfo.getName());
                cartItemInfo1.setProductNameAlias(pppGoodsInfo.getNameAlias());
                cartItemInfo1.setUnitPrice(pppGoodsInfo.getPrice());
                cartItemInfo1.setEmbedPhotos(new ArrayList<CartPhotosInfo>());
                cartItemInfo1.setDescription(pppGoodsInfo.getDescription());
                cartItemInfo1.setQty(1);
                cartItemInfo1.setStoreId(pppGoodsInfo.getStoreId());
                cartItemInfo1.setPictures(photoUrls);
                cartItemInfo1.setPrice(pppGoodsInfo.getPrice());
                cartItemInfo1.setCartProductType(3);

                orderinfoArrayList.add(cartItemInfo1);
                intent1.putExtra("orderinfo", orderinfoArrayList);
//                intent1.putExtra("isBack", "1");//取消付款后是否回到当前页面
                startActivity(intent1);
                break;

            case 7://操作比较耗时，会影响oncreate绘制
                mViewPager = (GalleryViewPager) findViewById(R.id.viewer);
                UrlPagerAdapter pagerAdapter = new UrlPagerAdapter(PreviewPhotoActivity.this, photolist);
                pagerAdapter.setOnPhotoEventListener(this);
                mViewPager.setOffscreenPageLimit(2);
                mViewPager.setAdapter(pagerAdapter);
                mViewPager.setCurrentItem(currentPosition, true);
                //初始化底部索引按钮
                updateIndexTools();

                mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

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
                        // TODO Auto-generated method stub

                        PictureAirLog.v(TAG, "----------------------->initing...5");
                        if (arg0 == 0) {//结束滑动
                            updateIndexTools();//只能写在这里，不能写在onPageSelected，不然出现切换回来之后，显示错乱
                            setUmengPhotoSlide();//统计滑动图片次数
                        }
                    }
                });
                break;

            case GET_LOCATION_AD:
                final int oldPositon = msg.arg1;
                if (myApplication.isGetADLocationSuccess()) {
                    //从数据库中查找
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String adStr = pictureAirDbManager.getADByLocationId(photoInfo.locationId, MyApplication.getInstance().getLanguageType());
                            previewPhotoHandler.obtainMessage(GET_LOCATION_AD_DONE, oldPositon, 0, adStr).sendToTarget();
                        }
                    }).start();

                } else {
                    //从网络获取
                    API1.getADLocations(oldPositon, previewPhotoHandler);
                }
                dismissPWProgressDialog();
                break;

            case GET_LOCATION_AD_DONE:
                if (msg.arg1 == currentPosition) {
                    if (!msg.obj.toString().equals("")) {//如果获取的对应索引值，依旧是当期的索引值，则显示广告
                        PictureAirLog.out("current position need show ad");
                        currentPhotoADTextView.setVisibility(View.VISIBLE);
                        currentPhotoADTextView.setText(msg.obj.toString());
                    } else {
                        currentPhotoADTextView.setVisibility(View.GONE);
                    }
                }
                break;

            case API1.GET_AD_LOCATIONS_SUCCESS:
                PictureAirLog.out("ad location---->" + msg.obj.toString());
                final int oldPosition1 = msg.arg1;
                final JSONObject adJsonObject = JSONObject.parseObject(msg.obj.toString());
                myApplication.setGetADLocationSuccess(true);
                /**
                 * 1.存入数据库
                 * 2.在application中记录结果
                 */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String adString = pictureAirDbManager.insertADLocations(adJsonObject.getJSONArray("locations"),
                            photoInfo.locationId, MyApplication.getInstance().getLanguageType());
                        previewPhotoHandler.obtainMessage(GET_LOCATION_AD_DONE, oldPosition1, 0, adString).sendToTarget();
                    }
                }).start();
                break;

            case API1.GET_AD_LOCATIONS_FAILED:
                break;

            case CHECK_FAVORITE://开始获取收藏信息
                final int oldPosition = msg.arg1;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        previewPhotoHandler.obtainMessage(GET_FAVORITE_DATA_DONE, oldPosition, 0,
                                pictureAirDbManager.checkLovePhoto(photoInfo,
                                        SPUtils.getString(PreviewPhotoActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.USERINFO_ID, ""))).sendToTarget();
                    }
                }).start();
                break;

            case GET_FAVORITE_DATA_DONE://获取数据成功
                //更新收藏图标
                if (Boolean.valueOf(msg.obj.toString()) && msg.arg1 == currentPosition) {//数据库查询的数据是true，并且对应的index还是之前的位置
                    PictureAirLog.out("current postion and is favorite");
                    photoInfo.isLove = 1;
                    loveImageButton.setImageResource(R.drawable.discover_like);
                } else {
                    PictureAirLog.out("not the favorite");
                    loveImageButton.setImageResource(R.drawable.discover_no_like);
                }
                break;

            case CREATE_BLUR_DIALOG:
                createBlurDialog();
                break;

            case API1.GET_PPPS_BY_SHOOTDATE_SUCCESS:  //根据已有PP＋升级
                if (API1.PPPlist.size() > 0) {
                    //将 tabname 存入sp
                    SPUtils.put(this, Common.SHARED_PREFERENCE_USERINFO_NAME, "tabName", tabName);
                    SPUtils.put(this, Common.SHARED_PREFERENCE_USERINFO_NAME, "currentPosition", currentPosition);

                    dia.dismiss();

                    intent = new Intent(PreviewPhotoActivity.this, SelectPPActivity.class);
                    intent.putExtra("photoPassCode",photoInfo.photoPassCode);
                    intent.putExtra("shootTime",photoInfo.shootTime);
                    startActivity(intent);
                } else {
                    newToast.setTextAndShow(R.string.no_ppp_tips, Common.TOAST_SHORT_TIME);
                }
                break;

            case API1.GET_PPPS_BY_SHOOTDATE_FAILED:
                newToast.setTextAndShow(ReflectionUtil.getStringId(MyApplication.getInstance(), msg.arg1), Common.TOAST_SHORT_TIME);
                break;

            case NO_PHOTOS_AND_RETURN://没有图片
                dismissPWProgressDialog();
                finish();
                break;

            default:
                break;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_photo);
        PictureAirLog.out("oncreate start----");
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE );
        init();//初始化UI
        PictureAirLog.out("oncreate finish----");
    }

    private void init() {
        // TODO Auto-generated method stub
        pictureWorksDialog = new PWDialog(this)
                .setOnPWDialogClickListener(this)
                .pwDialogCreate();
        previewPhotoHandler = new Handler(this);
        pictureAirDbManager = new PictureAirDbManager(this);
        settingUtil = new SettingUtil(pictureAirDbManager);
        newToast = new PWToast(this);
        sharePop = new SharePop(this);
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PictureAirLog.out("oncreate----->2");
        returnImageView = (ImageView) findViewById(R.id.button1_shop_rt);

        locationTextView = (TextView) findViewById(R.id.preview_location);
        editButton = (TextView) findViewById(R.id.preview_edit);
        shareButton = (TextView) findViewById(R.id.preview_share);
        downloadButton = (TextView) findViewById(R.id.preview_download);
        makegiftButton = (TextView) findViewById(R.id.preview_makegift);
        loveImageButton = (ImageButton) findViewById(R.id.preview_love);

        lastPhotoImageView = (TextView) findViewById(R.id.index_last);
        nextPhotoImageView = (TextView) findViewById(R.id.index_next);
        currentPhotoInfoTextView = (TextView) findViewById(R.id.index_time);
        currentPhotoIndexTextView = (TextView) findViewById(R.id.current_index);
        currentPhotoADTextView = (TextView) findViewById(R.id.preview_photo_ad_intro_tv);

        touchtoclean = (TextView) findViewById(R.id.textview_blur);
        photoFraRelativeLayout = (RelativeLayout) findViewById(R.id.fra_layout);

        titleBar = (RelativeLayout) findViewById(R.id.preview_titlebar);
        toolsBar = (LinearLayout) findViewById(R.id.toolsbar);
        indexBar = (LinearLayout) findViewById(R.id.index_bar);

        previewPhotoHandler.sendEmptyMessage(CREATE_BLUR_DIALOG);

        myApplication = (MyApplication) getApplication();

        returnImageView.setOnClickListener(this);
        editButton.setOnClickListener(this);
        shareButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        makegiftButton.setOnClickListener(this);
        loveImageButton.setOnClickListener(this);

        lastPhotoImageView.setOnClickListener(this);
        nextPhotoImageView.setOnClickListener(this);

        Configuration cf = getResources().getConfiguration();
        int ori = cf.orientation;
        if (ori == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
            landscapeOrientation();
        }

        showPWProgressDialog();
        getPreviewPhotos();
    }

    private void getPreviewPhotos() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                //获取本地图片
                targetphotolist = new ArrayList<>();
                targetphotolist.addAll(AppUtil.getLocalPhotos(PreviewPhotoActivity.this, Common.PHOTO_SAVE_PATH, Common.ALBUM_MAGIC));
                Collections.sort(targetphotolist);

                //获取intent传递过来的信息
                photolist = new ArrayList<>();
                Bundle bundle = getIntent().getBundleExtra("bundle");
                currentPosition = bundle.getInt("position", 0);
                PictureAirLog.out("currentposition---->" + currentPosition);
                tabName = bundle.getString("tab");
                PictureAirLog.out("tabName--->" + tabName);
                long cacheTime = System.currentTimeMillis() - PictureAirDbManager.CACHE_DAY * PictureAirDbManager.DAY_TIME;

                if (tabName.equals("all")) {//获取全部照片
                    locationList.addAll(AppUtil.getLocation(PreviewPhotoActivity.this, ACache.get(PreviewPhotoActivity.this).getAsString(Common.DISCOVER_LOCATION), true));
                    try {
                        photolist.addAll(AppUtil.getSortedAllPhotos(PreviewPhotoActivity.this, locationList, targetphotolist,
                                pictureAirDbManager, simpleDateFormat.format(new Date(cacheTime)),
                                simpleDateFormat, MyApplication.getInstance().getLanguageType()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                } else if (tabName.equals("photopass")) {//获取pp图片
                    locationList.addAll(AppUtil.getLocation(PreviewPhotoActivity.this, ACache.get(PreviewPhotoActivity.this).getAsString(Common.DISCOVER_LOCATION), true));
                    try {
                        photolist.addAll(AppUtil.getSortedPhotoPassPhotos(locationList, pictureAirDbManager,
                                simpleDateFormat.format(new Date(cacheTime)), simpleDateFormat, MyApplication.getInstance().getLanguageType(), false));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                } else if (tabName.equals("local")) {//获取本地图片
                    photolist.addAll(targetphotolist);

                } else if (tabName.equals("bought")) {//获取已经购买的图片
                    locationList.addAll(AppUtil.getLocation(PreviewPhotoActivity.this, ACache.get(PreviewPhotoActivity.this).getAsString(Common.DISCOVER_LOCATION), true));
                    try {
                        photolist.addAll(AppUtil.getSortedPhotoPassPhotos(locationList, pictureAirDbManager,
                                simpleDateFormat.format(new Date(cacheTime)), simpleDateFormat, MyApplication.getInstance().getLanguageType(), true));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                } else if (tabName.equals("favourite")) {//获取收藏图片
                    locationList.addAll(AppUtil.getLocation(PreviewPhotoActivity.this, ACache.get(PreviewPhotoActivity.this).getAsString(Common.DISCOVER_LOCATION), true));
                    photolist.addAll(AppUtil.insertSortFavouritePhotos(
                            pictureAirDbManager.getFavoritePhotoInfoListFromDB(PreviewPhotoActivity.this,
                                    SPUtils.getString(PreviewPhotoActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.USERINFO_ID, ""),
                                    simpleDateFormat.format(new Date(cacheTime)), locationList, MyApplication.getInstance().getLanguageType())));

                } else if (tabName.equals("editStory")){//编辑PP照片页面
                    String ppCode = bundle.getString("ppCode");
                    locationList.addAll(AppUtil.getLocation(PreviewPhotoActivity.this, ACache.get(PreviewPhotoActivity.this).getAsString(Common.DISCOVER_LOCATION), true));
                    photolist.addAll(AppUtil.insertSortFavouritePhotos(
                            pictureAirDbManager.getPhotoInfosByPPCode(ppCode, locationList, MyApplication.getInstance().getLanguageType())));

                } else {//获取列表图片
                    ArrayList<PhotoInfo> temp = bundle.getParcelableArrayList("photos");//获取图片路径list
                    if (temp != null) {
                        photolist.addAll(temp);
                    }
                }

                if (currentPosition == -1) {//购买图片后返回
                    String photoId = bundle.getString("photoId", "");
                    PictureAirLog.out("photoid--->" + photoId);
                    for (int i = 0; i < photolist.size(); i++) {
                        PictureAirLog.out("photoinfo.photoid----->" + photolist.get(i).photoId);
                        if (TextUtils.isEmpty(photolist.get(i).photoId)) {//本地图片，没有PhotoId，需要过滤

                        } else if (photolist.get(i).photoId.equals(photoId)){
                            photolist.get(i).isPayed = 1;
                            currentPosition = i;
                            break;
                        }
                    }
                }

                if (currentPosition == -2) {//绑定PP后返回
                    String ppsStr = bundle.getString("ppsStr");
                    refreshPP(photolist,ppsStr);
                    currentPosition = SPUtils.getInt(PreviewPhotoActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, "currentPosition",0);
                }

                if (currentPosition < 0) {
                    currentPosition = 0;
                }
                if (photolist.size() == 0) {
                    /**
                     * 图片为空的情况。
                     * 如果收到删除数据推送，本地数据处理完毕，
                     * 但是用户正好点进图片预览，这个时候会出现为空的情况，需要finish
                     */
                    PictureAirLog.out("no photos need return");
                    previewPhotoHandler.sendEmptyMessage(NO_PHOTOS_AND_RETURN);
                    return;
                }
                if (currentPosition > photolist.size() - 1) {//出现的情况就是，刚点进去，同步删除的处理将数据库删除了，需要取最后一个数据
                    currentPosition = photolist.size() - 1;
                }
                PhotoInfo currentPhotoInfo = photolist.get(currentPosition);

                PictureAirLog.out("photolist size ---->" + photolist.size());
                Iterator<PhotoInfo> photoInfoIterator = photolist.iterator();
                while (photoInfoIterator.hasNext()) {
                    PhotoInfo info = photoInfoIterator.next();
                    if (info.isVideo == 1 && info.isPayed == 0) {
                        photoInfoIterator.remove();
                    }
                }
                PictureAirLog.out("photolist size ---->" + photolist.size());
                PictureAirLog.out("currentPosition ---->" + currentPosition);
                currentPosition = photolist.indexOf(currentPhotoInfo);
                PictureAirLog.out("photoid--->" + photolist.get(currentPosition).photoId);
                PictureAirLog.out("currentPosition ---->" + currentPosition);
                PictureAirLog.v(TAG, "photo size is " + photolist.size());
                PictureAirLog.v(TAG, "thumbnail is " + photolist.get(currentPosition).photoThumbnail);
                PictureAirLog.v(TAG, "thumbnail 512 is " + photolist.get(currentPosition).photoThumbnail_512);
                PictureAirLog.v(TAG, "thumbnail 1024 is " + photolist.get(currentPosition).photoThumbnail_1024);
                PictureAirLog.v(TAG, "original is " + photolist.get(currentPosition).photoPathOrURL);
                PictureAirLog.v(TAG, "----------------------->initing...2");
                previewPhotoHandler.sendEmptyMessage(7);
            }
        }.start();
    }

    private void createBlurDialog() {
        dia = new Dialog(this, R.style.dialogTans);
        Window window = dia.getWindow();
        window.setGravity(Gravity.CENTER);
        //		window.setWindowAnimations(R.style.from_bottom_anim);
        dia.setCanceledOnTouchOutside(true);
        View view = View.inflate(this, R.layout.tans_dialog, null);
        dia.setContentView(view);
        WindowManager.LayoutParams layoutParams = dia.getWindow().getAttributes();
        layoutParams.width = ScreenUtil.getScreenWidth(this);
        dia.getWindow().setAttributes(layoutParams);

        buy_ppp = (TextView) dia.findViewById(R.id.buy_ppp);
        cancel = (TextView) dia.findViewById(R.id.cancel);
        buynow = (TextView) dia.findViewById(R.id.buynow);
        use_ppp = (TextView) dia.findViewById(R.id.use_ppp);
        buynow.setOnClickListener(this);
        buy_ppp.setOnClickListener(this);
        cancel.setOnClickListener(this);
        use_ppp.setOnClickListener(this);
    }

    /**
     * 更新底部索引工具
     */
    private void updateIndexTools() {
        PictureAirLog.v(TAG, "updateIndexTools-------->" + currentPosition);
        //初始化图片收藏按钮，需要判断isLove=1或者是否在数据库中
        if (isEdited) {
            photoInfo = targetphotolist.get(currentPosition);
        } else {//编辑前
            photoInfo = photolist.get(currentPosition);
        }
        previewPhotoHandler.obtainMessage(CHECK_FAVORITE, currentPosition, 0).sendToTarget();

        //更新title地点名称
        locationTextView.setText(photoInfo.locationName);

        //更新序列号
        currentPhotoIndexTextView.setText(String.format(getString(R.string.photo_index), currentPosition + 1, isEdited ? targetphotolist.size() : photolist.size()));
        currentPhotoInfoTextView.setText(photoInfo.shootOn.substring(0, 16));
        //更新上一张下一张按钮
        if (currentPosition == 0) {
            lastPhotoImageView.setVisibility(View.INVISIBLE);
        } else {
            lastPhotoImageView.setVisibility(View.VISIBLE);
        }
        if (currentPosition == (isEdited ? targetphotolist.size() - 1 : photolist.size() - 1)) {
            nextPhotoImageView.setVisibility(View.INVISIBLE);
        } else {
            nextPhotoImageView.setVisibility(View.VISIBLE);
        }

        //如果是未购买图片，判断是否是第一次进入，如果是，则显示引导图层
        if (photoInfo.isPayed == 0 && photoInfo.onLine == 1) {//未购买的图片
            PictureAirLog.v(TAG, "need show blur view");
            touchtoclean.setVisibility(View.VISIBLE);
            currentPhotoADTextView.setVisibility(View.GONE);
            dismissPWProgressDialog();
        } else if (photoInfo.isPayed == 1 && photoInfo.onLine == 1) {
            touchtoclean.setVisibility(View.GONE);
            previewPhotoHandler.obtainMessage(GET_LOCATION_AD, currentPosition, 0).sendToTarget();
            PictureAirLog.out("set enable in get ad");
        } else {
            touchtoclean.setVisibility(View.GONE);
            currentPhotoADTextView.setVisibility(View.GONE);
            PictureAirLog.out("set enable in other conditions");
            dismissPWProgressDialog();
        }

        if (isLandscape) {//横屏模式
            if (mViewPager != null) {
                mViewPager.setBackgroundColor(Color.BLACK);
            }
            touchtoclean.setTextColor(ContextCompat.getColor(this, R.color.white));
            touchtoclean.setShadowLayer(2, 2, 2, ContextCompat.getColor(this, R.color.pp_dark_blue));
        } else {
            touchtoclean.setTextColor(ContextCompat.getColor(this, R.color.pp_dark_blue));
            touchtoclean.setShadowLayer(2, 2, 2, ContextCompat.getColor(this, R.color.transparent));
        }

        if (photoInfo.isVideo == 1) {
            editButton.setVisibility(View.GONE);
            makegiftButton.setVisibility(View.GONE);
        } else {
            editButton.setVisibility(View.VISIBLE);
            makegiftButton.setVisibility(View.VISIBLE);
        }
    }

    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.button1_shop_rt:
                finish();
                break;

            case R.id.preview_love://收藏按钮的操作
                if (isEdited) {
                    photoInfo = targetphotolist.get(mViewPager.getCurrentItem());
                } else {//编辑前
                    photoInfo = photolist.get(mViewPager.getCurrentItem());
                }
                if (photoInfo == null) {
                    return;
                }
                if (photoInfo.isLove == 1) {
                    PictureAirLog.d(TAG, "cancel love");
                    pictureAirDbManager.setPictureLove(photoInfo, SPUtils.getString(PreviewPhotoActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.USERINFO_ID, ""), false);
                    photoInfo.isLove = 0;
                    loveImageButton.setImageResource(R.drawable.discover_no_like);
                } else {
                    PictureAirLog.d(TAG, "add love");
                    pictureAirDbManager.setPictureLove(photoInfo, SPUtils.getString(PreviewPhotoActivity.this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.USERINFO_ID, ""), true);
                    photoInfo.isLove = 1;
                    loveImageButton.setImageResource(R.drawable.discover_like);
                }
                myApplication.needScanFavoritePhotos = true;
                break;

            case R.id.preview_edit://编辑
                if (photoInfo == null) {
                    return;
                }
                if (photoInfo.onLine == 0) {
                    pictureWorksDialog.setPWDialogId(LOCAL_PHOTO_EDIT_DIALOG)
                            .setPWDialogMessage(R.string.local_photo_cannot_edit_content)
                            .setPWDialogNegativeButton(null)
                            .setPWDialogPositiveButton(R.string.photo_cannot_edit_yes)
                            .pwDilogShow();
                    return;
                }
                if (photoInfo.isPayed == 1) {
                    if (photoInfo.isHasPreset == 0) { // 如果没有模版，就去执行编辑操作。 如果有模版就弹出提示。
                        intent = new Intent(this, EditPhotoActivity.class);
                        if (isEdited) {//已经编辑过，取targetlist中的值
                            intent.putExtra("photo", targetphotolist.get(mViewPager.getCurrentItem()));
                        } else {//没有编辑，取正常的值
                            intent.putExtra("photo", photolist.get(mViewPager.getCurrentItem()));
                        }
                        startActivityForResult(intent, 1);
                    } else {
                        pictureWorksDialog.setPWDialogId(FRAME_PHOTO_EDIT_DIALOG)
                                .setPWDialogMessage(R.string.photo_cannot_edit_content)
                                .setPWDialogNegativeButton(null)
                                .setPWDialogPositiveButton(R.string.photo_cannot_edit_yes)
                                .pwDilogShow();
                    }
                } else {
                    dia.show();
                }
                break;

            case R.id.preview_share:
                if (photoInfo == null) {
                    return;
                }
                if (photoInfo.isPayed == 1) {
                    dia.dismiss();
                    if (mViewPager.getCurrentItem() >= photolist.size()) {
                        return;
                    }
                    PictureAirLog.v(TAG, "start share=" + photolist.get(mViewPager.getCurrentItem()).photoPathOrURL);
                    if (isEdited) {//编辑后
                        sharePop.setshareinfo(targetphotolist.get(mViewPager.getCurrentItem()), previewPhotoHandler);
                    } else {//编辑前
                        sharePop.setshareinfo(photolist.get(mViewPager.getCurrentItem()), previewPhotoHandler);
                    }
                    sharePop.showAtLocation(v, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                } else {
                    dia.show();
                }
                break;

            case R.id.preview_download://下载,如果不是pp的照片，提示不需要下载，如果是pp的照片，并且没有支付，提示购买，如果已经购买，如果没有下载，则下载，否则提示已经下载
                if (AppUtil.getNetWorkType(PreviewPhotoActivity.this) == AppUtil.NETWORKTYPE_INVALID) {
                    newToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    return;
                }
                if (photoInfo == null) {
                    return;
                }
                if (photoInfo.isPayed == 1) {
                    if (isEdited) {//编辑后
                        newToast.setTextAndShow(R.string.neednotdownload, Common.TOAST_SHORT_TIME);
                    } else {//编辑前
                        if (photoInfo.onLine == 1) {//是pp的照片
                            judgeOnePhotoDownloadFlow();
                        } else {
                            newToast.setTextAndShow(R.string.neednotdownload, Common.TOAST_SHORT_TIME);
                        }
                    }

                } else {
                    dia.show();
                }

                break;

            case R.id.preview_makegift:
                if (photoInfo == null) {
                    return;
                }

                if (photoInfo.onLine == 0) {
                    newToast.setTextAndShow(R.string.local_photo_not_support_makegift, Common.TOAST_SHORT_TIME);
                    return;
                }

                if (photoInfo.locationId.equals("photoSouvenirs")) {//排除纪念照的照片
                    newToast.setTextAndShow(R.string.not_support_makegift, Common.TOAST_SHORT_TIME);
                    return;
                }

                if (AppUtil.getNetWorkType(PreviewPhotoActivity.this) == AppUtil.NETWORKTYPE_INVALID) {
                    newToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    return;
                }

                PictureAirLog.v(TAG, "makegift");
                intent = new Intent(this, MakegiftActivity.class);
                //判断是否已经被编辑过
                if (isEdited) {//已经被编辑过，那么取得是targetList中的值
                    intent.putExtra("selectPhoto", targetphotolist.get(mViewPager.getCurrentItem()));
                } else {//没有编辑过，直接获取之前的值
                    intent.putExtra("selectPhoto", photolist.get(mViewPager.getCurrentItem()));
                }
                startActivity(intent);
                if (dia != null && dia.isShowing()) {
                    dia.dismiss();
                }
                break;

            case R.id.cancel:
                dia.dismiss();
                break;

            case R.id.buynow:
                if (AppUtil.getNetWorkType(PreviewPhotoActivity.this) == AppUtil.NETWORKTYPE_INVALID) {
                    newToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    dia.dismiss();
                    return;
                }
                if (photoInfo == null) {
                    return;
                }
                showPWProgressDialog();
                API1.buyPhoto(photoInfo.photoId, previewPhotoHandler);
                dia.dismiss();
                break;

            case R.id.buy_ppp:
                //直接购买PP+
                if (AppUtil.getNetWorkType(PreviewPhotoActivity.this) == AppUtil.NETWORKTYPE_INVALID) {
                    newToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    dia.dismiss();
                    return;
                }
                showPWProgressDialog();
                //获取商品
                getALlGoods();
                dia.dismiss();
                break;
            case R.id.use_ppp:
                if (photoInfo == null) {
                    return;
                }
                if (AppUtil.getNetWorkType(PreviewPhotoActivity.this) == AppUtil.NETWORKTYPE_INVALID) { //判断网络情况。
                    newToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    dia.dismiss();
                    return;
                }else{
                    API1.getPPPsByShootDate(previewPhotoHandler, photoInfo.shootTime);
                }
                break;

            case R.id.index_last://上一张
                changeTab(false);
                setUmengPhotoSlide();//统计滑动图片次数
                break;

            case R.id.index_next://下一张
                changeTab(true);
                setUmengPhotoSlide();//统计滑动图片次数
                break;

            default:
                break;
        }
    }

    /**
     * 初始化数据
     */
    public void getALlGoods() {
        //从缓层中获取数据
        String goodsByACache = ACache.get(MyApplication.getInstance()).getAsString(Common.ALL_GOODS);
        if (goodsByACache != null && !goodsByACache.equals("")) {
            previewPhotoHandler.obtainMessage(API1.GET_GOODS_SUCCESS, goodsByACache).sendToTarget();
        } else {
            //从网络获取商品,先检查网络
            if (AppUtil.getNetWorkType(MyApplication.getInstance()) != 0) {
                API1.getGoods(previewPhotoHandler);
            } else {
                //提醒检查网络
                newToast.setTextAndShow(R.string.no_network, Common.TOAST_SHORT_TIME);
            }
        }
    }

    /**
     * 左右滑动切换图片
     */
    private void changeTab(boolean next) {
        if (next) {
            PictureAirLog.v(TAG, "--------->next");
            if (currentPosition < (isEdited ? targetphotolist.size() - 1 : photolist.size() - 1)) {
                currentPosition++;
            } else {
                return;
            }
        } else {
            PictureAirLog.v(TAG, "--------->last");
            if (currentPosition > 0) {
                currentPosition--;
            } else {
                return;
            }
        }
        touchtoclean.setVisibility(View.GONE);
        mViewPager.setCurrentItem(currentPosition);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == 11) {
                //保存完图片的处理
                PictureAirLog.v(TAG, "save success");
                //1.获取新图片的数据，生成一个新的对象
                PhotoInfo selectPhotoItemInfo = new PhotoInfo();
                selectPhotoItemInfo.photoPathOrURL = data.getStringExtra("photoUrl");
                File file = new File(selectPhotoItemInfo.photoPathOrURL);
                selectPhotoItemInfo.lastModify = file.lastModified();
                date = new Date(selectPhotoItemInfo.lastModify);
                selectPhotoItemInfo.shootOn = simpleDateFormat.format(date);
                selectPhotoItemInfo.shootTime = selectPhotoItemInfo.shootOn.substring(0, 10);
                selectPhotoItemInfo.isChecked = 0;
                selectPhotoItemInfo.isSelected = 0;
                selectPhotoItemInfo.showMask = 0;
                selectPhotoItemInfo.locationName = getString(R.string.story_tab_magic);
                //					selectPhotoItemInfo.albumName = albumName;
                selectPhotoItemInfo.onLine = 0;
                selectPhotoItemInfo.isUploaded = 0;
                selectPhotoItemInfo.isPayed = 1;
                selectPhotoItemInfo.isVideo = 0;
                selectPhotoItemInfo.isHasPreset = 0;
                selectPhotoItemInfo.isEncrypted = 0;
                selectPhotoItemInfo.isRefreshInfo = 0;

                //2.将新图片插入到targetList中
                targetphotolist.add(0, selectPhotoItemInfo);
                //3.修改viewPager中的值为targetList
                mViewPager.setAdapter(new UrlPagerAdapter(this, targetphotolist));
                mViewPager.setCurrentItem(0, true);
                currentPosition = 0;
                //4.更新底部工具栏
                isEdited = true;

                updateIndexTools();

                myApplication.setneedScanPhoto(true);
                myApplication.scanMagicFinish = false;
            }
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        //如果手指在上面的时候，如果同时休眠，在唤醒之后，页面上有个清晰圈
        //需要通知handler释放清晰圈
        if (photoInfo != null && photoInfo.isPayed == 0 && photoInfo.onLine == 1) {
            previewPhotoHandler.sendEmptyMessage(2);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PictureAirLog.v(TAG, "----------->" + myApplication.getRefreshViewAfterBuyBlurPhoto());
        if (photoInfo != null && photoInfo.isPayed == 0 && photoInfo.onLine == 1) {
            if (myApplication.getRefreshViewAfterBuyBlurPhoto().equals(Common.FROM_MYPHOTOPASSPAYED)) {

            } else if (myApplication.getRefreshViewAfterBuyBlurPhoto().equals(Common.FROM_VIEWORSELECTACTIVITYANDPAYED)) {

            } else if (myApplication.getRefreshViewAfterBuyBlurPhoto().equals(Common.FROM_PREVIEW_PHOTO_ACTIVITY_PAY)) {

            } else {
                myApplication.setRefreshViewAfterBuyBlurPhoto("");
            }

            if (!myApplication.getBuyPPPStatus().equals(Common.FROM_PREVIEW_PPP_ACTIVITY_PAYED)) {//如果已经购买完成，则不需要清除数据，否则才会清除
                myApplication.setBuyPPPStatus("");
                //按返回，把状态全部清除
                myApplication.clearIsBuyingPhotoList();
            }

        }
        previewPhotoHandler.removeCallbacksAndMessages(null);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mViewPager.resetImageView();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            PictureAirLog.out("landscape----->");
            landscapeOrientation();
        } else {
            PictureAirLog.out("portrait----->");
            portraitOrientation();
        }

        if (dia != null) {
            WindowManager.LayoutParams layoutParams = dia.getWindow().getAttributes();
            layoutParams.width = ScreenUtil.getScreenWidth(this);
            dia.getWindow().setAttributes(layoutParams);
        }

        if (pictureWorksDialog != null) {
            pictureWorksDialog.autoFitScreen();
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

    /**
     * 垂直模式
     */
    private void portraitOrientation() {
        isLandscape = false;
        titleBar.setVisibility(View.VISIBLE);
        toolsBar.setVisibility(View.VISIBLE);
        indexBar.setVisibility(View.VISIBLE);
        if (mViewPager != null) {
            mViewPager.setBackgroundColor(getResources().getColor(R.color.pp_light_gray_background));
        }
        photoFraRelativeLayout.setBackgroundColor(getResources().getColor(R.color.pp_light_gray_background));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        touchtoclean.setTextColor(getResources().getColor(R.color.pp_dark_blue));
        touchtoclean.setShadowLayer(2, 2, 2, getResources().getColor(R.color.transparent));
    }

    /**
     * 横屏模式
     */
    private void landscapeOrientation() {
        isLandscape = true;
        if (sharePop.isShowing()) {
            sharePop.dismiss();
        }
        if (mViewPager != null) {
            mViewPager.setBackgroundColor(Color.BLACK);
        }
        photoFraRelativeLayout.setBackgroundColor(Color.BLACK);
        titleBar.setVisibility(View.GONE);
        toolsBar.setVisibility(View.GONE);
        indexBar.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        touchtoclean.setTextColor(getResources().getColor(R.color.white));
        touchtoclean.setShadowLayer(2, 2, 2, getResources().getColor(R.color.pp_dark_blue));
    }

    //直接下载
    private void downloadPic() {
        ArrayList<PhotoInfo> list = new ArrayList<PhotoInfo>();
        list.add(photolist.get(mViewPager.getCurrentItem()));
        Intent intent = new Intent(PreviewPhotoActivity.this, DownloadService.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("photos", list);
        bundle.putInt("prepareDownloadCount",list.size());//该参数用于传递下载的图片数量
        intent.putExtras(bundle);
        startService(intent);

        //弹框提示，可以进去下载管理页面
        pictureWorksDialog.setPWDialogId(GO_DOWNLOAD_ACTIVITY_DIALOG)
                .setPWDialogMessage(R.string.edit_story_addto_downloadlist)
                .setPWDialogNegativeButton(null)
                .setPWDialogPositiveButton(R.string.reset_pwd_ok)
                .pwDilogShow();
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (sharePop != null) {
            PictureAirLog.out("sharePop not null");
            if (shareType != SharePop.TWITTER) {
                PictureAirLog.out("dismiss dialog");
                sharePop.dismissDialog();
            }
        }
    }

    /**
     * tips 1，网络下载流程。
     */
    private void judgeOnePhotoDownloadFlow() { // 如果当前是wifi，无弹窗提示。如果不是wifi，则提示。
        if (!AppUtil.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            newToast.setTextAndShow(R.string.permission_storage_message, Common.TOAST_SHORT_TIME);
            return;
        }
        if (AppUtil.getNetWorkType(PreviewPhotoActivity.this) == AppUtil.NETWORKTYPE_WIFI) {
            downloadPic();
        } else {
            // 判断用户是否设置过 “仅wifi” 的选项。
            if (settingUtil.isOnlyWifiDownload(SPUtils.getString(this, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.USERINFO_ID, ""))) {
                pictureWorksDialog.setPWDialogId(GO_SETTING_DIALOG)
                        .setPWDialogMessage(R.string.one_photo_download_msg1)
                        .setPWDialogNegativeButton(R.string.one_photo_download_no_msg1)
                        .setPWDialogPositiveButton(R.string.one_photo_download_yes_msg1)
                        .pwDilogShow();
            } else {
                pictureWorksDialog.setPWDialogId(DOWNLOAD_DIALOG)
                        .setPWDialogMessage(R.string.one_photo_download_msg2)
                        .setPWDialogNegativeButton(R.string.one_photo_download_no_msg2)
                        .setPWDialogPositiveButton(R.string.one_photo_download_yes_msg2)
                        .pwDilogShow();
            }
        }
    }

    private void setUmengPhotoSlide() {
        UmengUtil.onEvent(PreviewPhotoActivity.this, Common.EVENT_PHOTO_SLIDE);
    }

    /**
     * 更新同一组PP, PP 卡号相同，日期相同的更新。
     * @param photolist
     * @param ppsStr    //ppsStr:[{"bindDate":"2016-05-04","code":"SHDRF22A2PWFH4N6"}]
     */
    private void refreshPP(List<PhotoInfo> photolist, String ppsStr) {
        JSONArray ppsArray = JSONArray.parseArray(ppsStr);
        JSONObject jsonObject = (JSONObject) ppsArray.get(0);
        if (photolist != null) {
            for (int i = 0; i < photolist.size(); i++) {
                if (photolist.get(i).photoPassCode != null) {
                    if (photolist.get(i).photoPassCode.replace(",","").equals(jsonObject.getString("code"))) {
                        if (photolist.get(i).shootOn.contains(jsonObject.getString("bindDate"))) {
                            photolist.get(i).isPayed = 1;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPWDialogButtonClicked(int which, int dialogId) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            switch (dialogId) {
                case GO_SETTING_DIALOG:
                    //去更改：跳转到设置界面。
                    Intent intent = new Intent(PreviewPhotoActivity.this, SettingActivity.class);
                    startActivity(intent);
                    break;

                case DOWNLOAD_DIALOG:
                    downloadPic();
                    break;

                case GO_DOWNLOAD_ACTIVITY_DIALOG:
                    AppManager.getInstance().killActivity(LoadManageActivity.class);
                    Intent i = new Intent(PreviewPhotoActivity.this, LoadManageActivity.class);
                    startActivity(i);
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public void videoClick(int position) {
        PictureAirLog.out("start check the video info -->" + position);
        /**
         * 1.检查数据库，是否需要重新获取数据
         * 2.获取最新的视频信息
         * 3.储存最新信息
         * 4.跳转或者弹框提示
         */
        showPWProgressDialog(R.string.is_loading);

        if (lastestVideoInfoPresenter == null) {
            lastestVideoInfoPresenter = new GetLastestVideoInfoPresenter(this, this, MyApplication.getTokenId());
        }

        lastestVideoInfoPresenter.videoInfoClick(photolist.get(position).photoId, position);
    }

    @Override
    public void touchClear(boolean visible) {
        touchtoclean.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void getNewInfoDone(int dealStatus, int position, PhotoInfo photoInfo, boolean checkByNetwork) {
        dismissPWProgressDialog();

        switch (dealStatus) {
            case GetLastestVideoInfoPresenter.NETWORK_ERROR://网络问题
                newToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                break;

            case GetLastestVideoInfoPresenter.VIDEO_MAKING://依旧在制作中
                pictureWorksDialog.setPWDialogId(VIDEO_STILL_MAKING_DIALOG)
                        .setPWDialogMessage(R.string.magic_in_the_making)
                        .setPWDialogNegativeButton(null)
                        .setPWDialogPositiveButton(R.string.button_ok)
                        .pwDilogShow();
                break;

            case GetLastestVideoInfoPresenter.VIDEO_FINISHED://已经制作完成
                //list拿错数据
                PhotoInfo info;
                if (checkByNetwork) {
                    info = photoInfo;
                } else {
                    info = photolist.get(position);
                }

                Intent intent = new Intent(this, VideoPlayerActivity.class);
                intent.putExtra("from_story", info);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_fadein, R.anim.activity_fadeout);
                break;

            default:
                break;
        }
    }
}