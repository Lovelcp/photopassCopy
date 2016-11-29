package com.pictureair.photopass.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.customDialog.PWDialog;
import com.pictureair.photopass.entity.DealingInfo;
import com.pictureair.photopass.eventbus.BaseBusEvent;
import com.pictureair.photopass.eventbus.MainTabOnClickEvent;
import com.pictureair.photopass.eventbus.MainTabSwitchEvent;
import com.pictureair.photopass.eventbus.RedPointControlEvent;
import com.pictureair.photopass.eventbus.StoryLoadCompletedEvent;
import com.pictureair.photopass.fragment.FragmentPageDiscover;
import com.pictureair.photopass.fragment.FragmentPageMe;
import com.pictureair.photopass.fragment.FragmentPageShop;
import com.pictureair.photopass.fragment.FragmentPageStory;
import com.pictureair.photopass.service.DownloadService;
import com.pictureair.photopass.service.NotificationService;
import com.pictureair.photopass.util.ACache;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.ReflectionUtil;
import com.pictureair.photopass.util.SPUtils;
import com.pictureair.photopass.widget.CheckUpdateListener;
import com.pictureair.photopass.widget.CheckUpdateManager;
import com.pictureair.photopass.widget.PPDescriptor;
import com.pictureair.photopass.widget.PWToast;
import com.pictureair.photopass.widget.dropview.CoverManager;
import com.pictureair.photopass.widget.dropview.DropCover.OnDragCompeteListener;
import com.pictureair.photopass.widget.dropview.WaterDrop;

import net.xpece.material.navigationdrawer.descriptors.NavigationItemDescriptor;
import net.xpece.material.navigationdrawer.descriptors.NavigationSectionDescriptor;
import net.xpece.material.navigationdrawer.list.NavigationListFragmentCallbacks;
import net.xpece.material.navigationdrawer.list.SupportNavigationListFragment;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;


/**
 * 包含三个页面，photo显示、相机拍照、商城，默认进入第一个photo显示页面
 * 通过扫描或者登录之后会来到此页面
 */
public class MainTabActivity extends BaseFragmentActivity implements OnDragCompeteListener, Handler.Callback,
        PWDialog.OnCustomerViewCallBack, OnClickListener, CheckUpdateListener, PPDescriptor.DescriptorClickListener,NavigationListFragmentCallbacks {
    private FragmentPageStory fragmentPageStory;
    private FragmentPageDiscover fragmentPageDiscover;
    private FragmentPageShop fragmentPageShop;
    private FragmentPageMe fragmentPageMe;

    private FragmentManager fragmentManager;

    private LinearLayout storyTab, discoverTab, shopTab, meTab;
    private ImageView storyIV, discoverIV, shopIV, meIV;
    private TextView storyTV, discoverTV, shopTV, meTV;

    private RelativeLayout parentLayout;
    private WaterDrop waterDropView;
    private ImageView explored;
    private Handler handler;

    //story的引导层
    private RelativeLayout leadViewRL;
    private ImageView leadViewIV;
    private boolean showLeadView = true;

    //对话框
    private PWDialog pwDialog;
    private TextView specialDealBuyTV;
    private ImageView specialDealCloseIV;
    private DealingInfo dealingInfo;
    private boolean isDealing = false;

    //记录退出的时候的两次点击的间隔时间
    private long exitTime = 0;

    private PWToast newToast;

    //上次的tab页面，用来判断点击视频之后回到那个tab
    private int last_tab = 0;

    private boolean hasCreated = false;

    private MyApplication application;
    private CheckUpdateManager checkUpdateManager;
    private boolean needUpdate = true;
    private String currentLanguage;

    private static final String TAG = MainTabActivity.class.getSimpleName();

    private static final int SPECIAL_DIALOG = 99;
    private static final int START_CHECK_UPDATE = 100;

    /**
     * 消失动画的更新
     */
    private static final int UPDATE_EXPLORED = 101;

    /**
     * 更新间隔
     */
    private static final int UPDATE_EXPLORED_DURING_TIME = 50;

    /**
     * 动画播放索引值
     */
    private int expolredAnimFrameIndex = 0;

    private static List<NavigationSectionDescriptor> SECTIONS;

    private static final String REFLECTION_RESOURCE = "explored";

    private DrawerLayout mDrawerLayout;
    private SupportNavigationListFragment mNavFragment;
    private ActionBarDrawerToggle mDrawerToggle;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PictureAirLog.out(TAG + "==== onCreate");
        setContentView(R.layout.activity_main);
        application = (MyApplication) getApplication();
        handler = new Handler(this);
        hasCreated = true;
        initView();
        setTabSelection(0, false);
    }

    //清除acahe框架的缓存数据
    private void clearCache() {
        ACache.get(this).remove(Common.ALL_GOODS);
        ACache.get(this).remove(Common.ACACHE_ADDRESS);
    }

    /**
     * 初始化组件
     */
    private void initView() {
        fragmentManager = getSupportFragmentManager();

        storyTab = (LinearLayout) findViewById(R.id.main_photo_tab);
        discoverTab = (LinearLayout) findViewById(R.id.main_discover_tab);
        shopTab = (LinearLayout) findViewById(R.id.main_shop_tab);
        meTab = (LinearLayout) findViewById(R.id.main_me_tab);

        storyIV = (ImageView) findViewById(R.id.main_photo_iv);
        discoverIV = (ImageView) findViewById(R.id.main_discover_iv);
        shopIV = (ImageView) findViewById(R.id.main_shop_iv);
        meIV = (ImageView) findViewById(R.id.main_me_iv);

        storyTV = (TextView) findViewById(R.id.main_photo_tv);
        discoverTV = (TextView) findViewById(R.id.main_discover_tv);
        shopTV = (TextView) findViewById(R.id.main_shop_tv);
        meTV = (TextView) findViewById(R.id.main_me_tv);

        storyTab.setOnClickListener(new TabOnClick(0));
        discoverTab.setOnClickListener(new TabOnClick(1));
        shopTab.setOnClickListener(new TabOnClick(3));
        meTab.setOnClickListener(new TabOnClick(4));

        waterDropView = (WaterDrop) findViewById(R.id.waterdrop);
        waterDropView.setOnDragCompeteListener(this);
        AppUtil.expandViewTouchDelegate(waterDropView, 40, 40, 40, 40);

        parentLayout = (RelativeLayout) findViewById(R.id.parent);
        newToast = new PWToast(this);

        findViewById(R.id.fab).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainTabActivity.this, OpinionsActivity.class);
                startActivity(intent);
            }
        });

        // 自动检查更新
        currentLanguage = SPUtils.getString(this, Common.SHARED_PREFERENCE_APP, Common.LANGUAGE_TYPE, Common.ENGLISH);
        checkUpdateManager = new CheckUpdateManager(this, currentLanguage);
        checkUpdateManager.setOnCheckUpdateListener(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                checkUpdateManager.init();
                handler.sendEmptyMessage(START_CHECK_UPDATE);
            }
        }).start();

        parentLayout = (RelativeLayout) findViewById(R.id.parent);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        explored = new ImageView(this);
        explored.setLayoutParams(params);
        explored.setImageResource(R.drawable.explored1);
        explored.setVisibility(View.INVISIBLE);
        explored.setAdjustViewBounds(true);
        parentLayout.addView(explored);

        intiData();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.mnd_shadow_left, Gravity.RIGHT);
        mDrawerLayout.setDrawerShadow(R.drawable.mnd_shadow_right, Gravity.LEFT);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, android.R.string.untitled, android.R.string.untitled);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavFragment = (SupportNavigationListFragment) fragmentManager.findFragmentById(R.id.navigation_drawer);
        mNavFragment.setHeaderView(mNavFragment.getLayoutInflater2().inflate(R.layout.slide_custom_header, null), true);
        mNavFragment.setSections(SECTIONS);

        application.setIsStoryTab(true);

        CoverManager.getInstance().init(this);
    }

    private void intiData() {
        NavigationSectionDescriptor section = new NavigationSectionDescriptor()
                .addItem(new PPDescriptor(0, this).checked(false).date("2016-01-02").count(String.format(getString(R.string.story_photo_count),180)).num("SHDRH3H3H3H3H3H3H")
                        .checkedDrawable(R.drawable.sele).unCheckedDrawable(R.drawable.nosele))

                .addItem(new PPDescriptor(1, this).checked(false).date("2016-01-02").count(String.format(getString(R.string.story_photo_count),180)).num("SHDRH3H3H3H3H3H3H")
                        .checkedDrawable(R.drawable.sele).unCheckedDrawable(R.drawable.nosele))
                .addItem(new PPDescriptor(2, this).checked(false).date("2016-01-02").count(String.format(getString(R.string.story_photo_count),180)).num("SHDRH3H3H3H3H3H3H")
                        .checkedDrawable(R.drawable.sele).unCheckedDrawable(R.drawable.nosele))
                .addItem(new PPDescriptor(3, this).checked(false).date("2016-01-02").count(String.format(getString(R.string.story_photo_count),180)).num("SHDRH3H3H3H3H3H3H")
                        .checkedDrawable(R.drawable.sele).unCheckedDrawable(R.drawable.nosele))
                .addItem(new PPDescriptor(4, this).checked(false).date("2016-01-02").count(String.format(getString(R.string.story_photo_count),180)).num("SHDRH3H3H3H3H3H3H")
                        .checkedDrawable(R.drawable.sele).unCheckedDrawable(R.drawable.nosele))
                .addItem(new PPDescriptor(5, this).checked(false).date("2016-01-02").count(String.format(getString(R.string.story_photo_count),180)).num("SHDRH3H3H3H3H3H3H")
                        .checkedDrawable(R.drawable.sele).unCheckedDrawable(R.drawable.nosele))
                .addItem(new PPDescriptor(6, this).checked(false).date("2016-01-02").count(String.format(getString(R.string.story_photo_count),180)).num("SHDRH3H3H3H3H3H3H")
                        .checkedDrawable(R.drawable.sele).unCheckedDrawable(R.drawable.nosele));

        SECTIONS = new ArrayList<>();
        SECTIONS.add(section);

    }

    private void initLeadView() {
        leadViewRL = (RelativeLayout) findViewById(R.id.story_lead_view_rl);
        leadViewIV = (ImageView) findViewById(R.id.story_lead_iv);
        leadViewRL.setVisibility(View.VISIBLE);
        leadViewRL.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SPUtils.put(MainTabActivity.this, Common.SHARED_PREFERENCE_APP, Common.STORY_LEAD_VIEW, Common.STORY_LEAD_VIEW);
                leadViewRL.setVisibility(View.GONE);
                showLeadView = false;
                getSpecialDealGoods();
            }
        });

        if (application.getLanguageType().equals(Common.ENGLISH)) {
            leadViewIV.setImageResource(R.drawable.story_lead_en);
        } else if (application.getLanguageType().equals(Common.SIMPLE_CHINESE)) {
            leadViewIV.setImageResource(R.drawable.story_lead_zh);
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        PictureAirLog.out("maintab ==== resume");
        Intent intent1 = new Intent(this, com.pictureair.photopass.service.NotificationService.class);
        this.startService(intent1);
        if (hasCreated) {
            hasCreated = false;
            return;
        }
        if (application.getMainTabIndex() == 3) {
            PictureAirLog.out("skip to shop tab");
            setTabSelection(3, true);
            application.setMainTabIndex(-1);
            application.setIsStoryTab(false);
            last_tab = 3;
        } else if (application.getMainTabIndex() == 0) {
            PictureAirLog.out("skip to story tab");
            setTabSelection(0, true);
            application.setMainTabIndex(-1);
            application.setIsStoryTab(true);
            last_tab = 0;
        } else {
            PictureAirLog.out("skip to last tab");
            //设置成为上次的tab页面
            setTabSelection(last_tab, true);
            if (last_tab == 0) {
                application.setIsStoryTab(true);
            }
        }
        PictureAirLog.out("currenagLanguage--->" + currentLanguage + "___" + MyApplication.getInstance().getLanguageType());
        if (currentLanguage != null && !currentLanguage.equals(MyApplication.getInstance().getLanguageType())) {
            PictureAirLog.out("maintab ==== currentLanguage");
            removeFragment();
            //修改底部tab语言
            storyTV.setText(R.string.tab_story);
            discoverTV.setText(R.string.tab_discover);
            shopTV.setText(R.string.tab_shops);
            meTV.setText(R.string.tab_me);
            setTabSelection(4, false);
            currentLanguage = MyApplication.getInstance().getLanguageType();
        }
        PictureAirLog.out("pushcount-->" + application.getPushPhotoCount());
        if (application.getPushPhotoCount() + application.getPushViedoCount() > 0) {//显示红点
            waterDropView.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        checkUpdateManager.onDestroy();
        CoverManager.getInstance().destroy();
        clearCache();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.special_dialog_buy_tv:
                EventBus.getDefault().post(new MainTabOnClickEvent(dealingInfo, true, true));
                pwDialog.pwDialogDismiss();
                break;

            case R.id.special_dialog_deal_close_iv:
                EventBus.getDefault().post(new MainTabOnClickEvent(dealingInfo, true, false));
                pwDialog.pwDialogDismiss();
                break;

            default:
                break;
        }
    }

    @Override
    public void onItemClick(int position) {
        newToast.setTextAndShow("点击了第"+position+"项");
    }

    /**
     * 不需要写代码，单项点击使用上面的函数，不实现该类，解析会崩溃
     * */
    @Override
    public void onNavigationItemSelected(View view, int position, int id, NavigationItemDescriptor item) {

    }

    //tab按钮的点击监听
    private class TabOnClick implements OnClickListener {
        private int currentTab;

        public TabOnClick(int currentTab) {
            this.currentTab = currentTab;
        }

        @Override
        public void onClick(View v) {
            switch (currentTab) {
                case 0:
                    PictureAirLog.out("photo tab on click");
                    if (last_tab == 0) {//获取最新数据
                        PictureAirLog.d(TAG, "need refresh");
                        EventBus.getDefault().post(new MainTabOnClickEvent(true));
                    } else {
                        PictureAirLog.d(TAG, "need not refresh, need get new special deal goods");
                        getSpecialDealGoods();
                    }
                    setTabSelection(0, true);
                    last_tab = 0;
                    application.setIsStoryTab(true);
                    break;

                case 2:
                    PictureAirLog.out("camera tab on click");
                    setTabSelection(2, true);
                    application.setIsStoryTab(false);
                    break;

                case 1:
                case 3:
                case 4:
                    PictureAirLog.out(currentTab + " tab on click");
                    last_tab = currentTab;
                    setTabSelection(currentTab, true);
                    application.setIsStoryTab(false);
                    break;

                default:
                    break;
            }
        }

    }

    /**
     * 清除fragment
     */
    private void removeFragment(){
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        fragmentPageStory = (FragmentPageStory) fragmentManager.findFragmentByTag("tag1");
        fragmentPageDiscover = (FragmentPageDiscover) fragmentManager.findFragmentByTag("tag2");
        fragmentPageShop = (FragmentPageShop) fragmentManager.findFragmentByTag("tag4");
        fragmentPageMe = (FragmentPageMe) fragmentManager.findFragmentByTag("tag5");
        if (fragmentPageStory != null) {
            transaction.remove(fragmentPageStory);
            fragmentPageStory = null;
        }
        if (fragmentPageDiscover != null) {
            transaction.remove(fragmentPageDiscover);
            fragmentPageDiscover = null;
        }
        if (fragmentPageShop != null) {
            transaction.remove(fragmentPageShop);
            fragmentPageShop = null;
        }
        if (fragmentPageMe != null) {
            transaction.remove(fragmentPageMe);
            fragmentPageMe = null;
        }
        transaction.commitAllowingStateLoss();
    }

    private void setTabSelection(int index, boolean needHide) {

        if (checkCurrentSelection(index)) {//如果正在显示，不需要做任何处理
            PictureAirLog.out("current showing tab is ---> " + index);
            return;
        } else {
            PictureAirLog.out("current tab not showing--->" + index);
        }

        // 每次选中之前先清楚掉上次的选中状态
        clearSelection();
        // 开启一个Fragment事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (needHide) {
            // 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况
            hideFragments(transaction);
        }
        switch (index) {
            case 0:
                // 当点击了照片tab时，改变控件的图片和文字颜色
                storyIV.setImageResource(R.drawable.icon_photos_sel);
                storyTV.setTextColor(ContextCompat.getColor(this, R.color.pp_purple));
                if (fragmentPageStory == null) {
                    // 如果MessageFragment为空，则创建一个并添加到界面上
                    fragmentPageStory = new FragmentPageStory();
                    transaction.add(R.id.main_content, fragmentPageStory, "tag1");
                } else {
                    // 如果MessageFragment不为空，则直接将它显示出来
                    transaction.show(fragmentPageStory);
                }
                break;

            case 1:
                // 当点击了发现tab时，改变控件的图片和文字颜色
                discoverIV.setImageResource(R.drawable.icon_discover_sel);
                discoverTV.setTextColor(ContextCompat.getColor(this, R.color.pp_purple));
                if (fragmentPageDiscover == null) {
                    // 如果ContactsFragment为空，则创建一个并添加到界面上
                    fragmentPageDiscover = new FragmentPageDiscover();
                    transaction.add(R.id.main_content, fragmentPageDiscover, "tag2");
                } else {
                    // 如果ContactsFragment不为空，则直接将它显示出来
                    transaction.show(fragmentPageDiscover);
                }
                break;

            case 3:
                // 当点击了商店tab时，改变控件的图片和文字颜色
                shopIV.setImageResource(R.drawable.icon_shop_sel);
                shopTV.setTextColor(ContextCompat.getColor(this, R.color.pp_purple));
                if (fragmentPageShop == null) {
                    // 如果NewsFragment为空，则创建一个并添加到界面上
                    fragmentPageShop = new FragmentPageShop();
                    transaction.add(R.id.main_content, fragmentPageShop, "tag4");
                } else {
                    // 如果NewsFragment不为空，则直接将它显示出来
                    transaction.show(fragmentPageShop);
                }
                break;

            case 4:
                // 当点击了我的tab时，改变控件的图片和文字颜色
                meIV.setImageResource(R.drawable.icon_me_sel);
                meTV.setTextColor(ContextCompat.getColor(this, R.color.pp_purple));
                if (fragmentPageMe == null) {
                    PictureAirLog.out("fragment me is null");
                    // 如果SettingFragment为空，则创建一个并添加到界面上
                    fragmentPageMe = new FragmentPageMe();
                    transaction.add(R.id.main_content, fragmentPageMe, "tag5");
                } else {
                    // 如果SettingFragment不为空，则直接将它显示出来
                    PictureAirLog.out("fragment me is not null");
                    transaction.show(fragmentPageMe);
                }
                break;

            default:
                break;
        }
        transaction.commitAllowingStateLoss();
        PictureAirLog.out("maintab---->commit");
    }

    /**
     * 检查当前是显示的是哪个fragment
     * @param index
     * @return
     */
    private boolean checkCurrentSelection(int index) {
        boolean isShowing = false;
        switch (index) {
            case 0:
                if (fragmentPageStory != null)
                    isShowing = fragmentPageStory.isVisible();
                break;

            case 1:
                if (fragmentPageDiscover != null)
                    isShowing = fragmentPageDiscover.isVisible();
                break;

            case 2://camera

                break;

            case 3:
                if (fragmentPageShop != null)
                    isShowing = fragmentPageShop.isVisible();
                break;

            case 4:
                if (fragmentPageMe != null)
                    isShowing = fragmentPageMe.isVisible();
                break;

            default:
                break;
        }
        return isShowing;
    }

    /**
     * 清除掉所有的选中状态。
     */
    private void clearSelection() {
        storyIV.setImageResource(R.drawable.icon_photos_nor);
        storyTV.setTextColor(ContextCompat.getColor(this, R.color.pp_gray));
        discoverIV.setImageResource(R.drawable.icon_discover_nor);
        discoverTV.setTextColor(ContextCompat.getColor(this, R.color.pp_gray));
        shopIV.setImageResource(R.drawable.icon_shop_nor);
        shopTV.setTextColor(ContextCompat.getColor(this, R.color.pp_gray));
        meIV.setImageResource(R.drawable.icon_me_nor);
        meTV.setTextColor(ContextCompat.getColor(this, R.color.pp_gray));
    }

    /**
     * 将所有的Fragment都置为隐藏状态。
     *
     * @param transaction 用于对Fragment执行操作的事务
     */
    private void hideFragments(FragmentTransaction transaction) {
        fragmentPageStory = (FragmentPageStory) fragmentManager.findFragmentByTag("tag1");
        fragmentPageDiscover = (FragmentPageDiscover) fragmentManager.findFragmentByTag("tag2");
        fragmentPageShop = (FragmentPageShop) fragmentManager.findFragmentByTag("tag4");
        fragmentPageMe = (FragmentPageMe) fragmentManager.findFragmentByTag("tag5");

        if (fragmentPageStory != null) {
            transaction.hide(fragmentPageStory);
        }
        if (fragmentPageDiscover != null) {
            transaction.hide(fragmentPageDiscover);
        }
        if (fragmentPageShop != null) {
            transaction.hide(fragmentPageShop);
        }
        if (fragmentPageMe != null) {
            PictureAirLog.out("me ----> not null");
            transaction.hide(fragmentPageMe);
        } else {

            PictureAirLog.out("me ----> null");
        }
    }

    //双击退出app
    private void exitApp() {
        if ((System.currentTimeMillis() - exitTime) > 1000) {
            newToast.setTextAndShow(R.string.exit, Common.TOAST_SHORT_TIME);
            exitTime = System.currentTimeMillis();
        } else {
            newToast.cancelShow();
            finish();
            clearCache();
            //取消通知
            Intent intent = new Intent(MainTabActivity.this, NotificationService.class);
            intent.putExtra("status", "disconnect");
            startService(intent);
            //关闭下载
            Intent intent1 = new Intent(MyApplication.getInstance(), DownloadService.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean("logout",true);
            intent1.putExtras(bundle);
            startService(intent1);
            AppManager.getInstance().killAllActivity();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitApp();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showSpecialDealDialog() {
        if (pwDialog == null) {
            pwDialog = new PWDialog(this, SPECIAL_DIALOG)
                    .setPWDialogNegativeButton(null)
                    .setPWDialogPositiveButton(null)
                    .setPWDialogBackgroundColor(R.color.transparent)
                    .setPWDialogContentView(R.layout.dialog_special_deal, this)
                    .pwDialogCreate();
        }
        if (!isDealing) {
            pwDialog.pwDilogShow();
        }
        isDealing = true;
    }

    private void getSpecialDealGoods() {
        if (!needUpdate && !showLeadView) {//不需要更新apk，并且不需要显示引导层，才需要显示抢购
            //如果活动进行中，则不需要重新获取数据
            API1.getDealingGoods(MyApplication.getTokenId(), MyApplication.getInstance().getLanguageType(), handler);
        }
    }

    @Override
    public void initCustomerView(View view, int dialogId) {
        if (dialogId == SPECIAL_DIALOG) {
            specialDealBuyTV = (TextView) view.findViewById(R.id.special_dialog_buy_tv);
            specialDealCloseIV = (ImageView) view.findViewById(R.id.special_dialog_deal_close_iv);
            specialDealBuyTV.setOnClickListener(this);
            specialDealCloseIV.setOnClickListener(this);
        }
    }

    @Override
    public void checkUpdateCompleted(int result) {//抢购的弹框需要在更新框之后
        needUpdate = result == CheckUpdateManager.APK_NEED_UPDATE;
        getSpecialDealGoods();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    public void onDrag(float endX, float endY) {
        //小红点的拖拽消失，消失之后的消息回调，暂时此处不需要做任何的操作
        explored.setVisibility(View.VISIBLE);
        explored.setX(endX - explored.getWidth() / 2);
        explored.setY(endY - explored.getHeight() / 2);
        handler.sendEmptyMessage(UPDATE_EXPLORED);
    }

    @Override
    public void onVisible(boolean visible) {
        waterDropView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public boolean handleMessage(Message msg) {
        // TODO Auto-generated method stub
        switch (msg.what) {
            /**
             * 使用handler只是为了监听动画结束，从而将imageview消失，一般的帧动画是没有监听动画结束的监听
             */
            case UPDATE_EXPLORED:
                expolredAnimFrameIndex++;
                PictureAirLog.out("index--->" + expolredAnimFrameIndex);
                if (expolredAnimFrameIndex < 6) {//更新动画
                    PictureAirLog.out("update");
                    explored.setImageResource(ReflectionUtil.getDrawableId(this, REFLECTION_RESOURCE + expolredAnimFrameIndex));
                    handler.sendEmptyMessageDelayed(UPDATE_EXPLORED, UPDATE_EXPLORED_DURING_TIME);
                } else {//动画结束，隐藏控件
                    PictureAirLog.out("dismiss");
                    expolredAnimFrameIndex = 0;
                    explored.setVisibility(View.GONE);
                }
                break;

            case START_CHECK_UPDATE:
                checkUpdateManager.startCheck();
                break;

            case API1.GET_DEALING_GOODS_SUCCESS:
                PictureAirLog.d(msg.obj.toString());
                DealingInfo curDealingInfo = (DealingInfo) msg.obj;
                if (dealingInfo != null) {//比较误差，取误差最小值
                    if (Math.abs(dealingInfo.getTimeOffset()) < Math.abs(curDealingInfo.getTimeOffset())) {
                        curDealingInfo.setTimeOffset(dealingInfo.getTimeOffset());

                    }
                }
                dealingInfo = curDealingInfo;

                if (dealingInfo.getState() == 1 || dealingInfo.getState() == -2 || dealingInfo.getState() == -3) {
                    showSpecialDealDialog();//抢购活动，需要在更新提示框之后出现
                } else if (dealingInfo.getState() == 0){//活动结束
                    isDealing = false;
                    EventBus.getDefault().post(new MainTabOnClickEvent(dealingInfo, false, false));
                }
                break;

            case API1.GET_DEALING_GOODS_FAILED:
                break;

            default:
                break;
        }
        return false;
    }


    /**
     * 检测dropView显示消失事件
     *
     * @param baseBusEvent
     */
    @Subscribe
    public void onUserEvent(BaseBusEvent baseBusEvent) {
        if (baseBusEvent instanceof RedPointControlEvent) {//红点的刷新
            PictureAirLog.out("control the badgeView----->");
            RedPointControlEvent redPointControlEvent = (RedPointControlEvent) baseBusEvent;
            if (redPointControlEvent.isShowRedPoint()) {//显示红点
                waterDropView.setVisibility(View.VISIBLE);
            } else {//消失红点
                if (waterDropView.isShown()) {
                    waterDropView.setVisibility(View.GONE);
                }
            }
            //刷新列表
            EventBus.getDefault().removeStickyEvent(redPointControlEvent);
        } else if (baseBusEvent instanceof MainTabSwitchEvent) {//切换到对应的tab
            MainTabSwitchEvent mainTabSwitchEvent = (MainTabSwitchEvent) baseBusEvent;
            setTabSelection(mainTabSwitchEvent.getMainTabSwitchIndex(), true);
            application.setIsStoryTab(mainTabSwitchEvent.getMainTabSwitchIndex() == 0 ? true : false);
            last_tab = mainTabSwitchEvent.getMainTabSwitchIndex();
            EventBus.getDefault().removeStickyEvent(mainTabSwitchEvent);

        } else if (baseBusEvent instanceof StoryLoadCompletedEvent) {//显示引导层
            StoryLoadCompletedEvent storyLoadCompletedEvent = (StoryLoadCompletedEvent) baseBusEvent;
            showLeadView = storyLoadCompletedEvent.isShowLeadView();
            if (showLeadView) {
                initLeadView();
            } else {
                getSpecialDealGoods();
            }
            EventBus.getDefault().removeStickyEvent(storyLoadCompletedEvent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> listFragments = getSupportFragmentManager().getFragments();
        if (listFragments != null && listFragments.size() > 0) {
            for (Fragment fragment : listFragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
}
