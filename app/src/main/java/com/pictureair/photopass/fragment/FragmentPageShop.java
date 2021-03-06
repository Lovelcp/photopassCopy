package com.pictureair.photopass.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.activity.BaseFragment;
import com.pictureair.photopass.activity.CartActivity;
import com.pictureair.photopass.activity.DetailProductActivity;
import com.pictureair.photopass.activity.PPPDetailProductActivity;
import com.pictureair.photopass.adapter.ShopGoodListViewAdapter;
import com.pictureair.photopass.entity.AddressJson;
import com.pictureair.photopass.entity.GoodsInfo;
import com.pictureair.photopass.entity.GoodsInfoJson;
import com.pictureair.photopass.http.rxhttp.RxSubscribe;
import com.pictureair.photopass.util.ACache;
import com.pictureair.photopass.util.API2;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.JsonTools;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.SPUtils;
import com.pictureair.photopass.widget.NoNetWorkOrNoCountView;
import com.pictureair.photopass.widget.PWToast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * shop类
 * 显示全部商品，调用全部商品API
 *
 * @author bauer_bao
 */
public class FragmentPageShop extends BaseFragment implements OnClickListener {
    private static final String TAG = "FragmentPageShop";
    //申明控件
    private ImageView shoppingBag;
    private TextView cartCountTextView;
    private SwipeRefreshLayout refreshLayout;
    private ListView xListView;
    private NoNetWorkOrNoCountView noNetWorkOrNoCountView;

    //申明变量
    private int cartCount = 0; // 记录数据库中有几条记录
    private String currency = "";//货币种类

    //申明实例类
    private List<GoodsInfo> allGoodsList;//全部商品
    private ShopGoodListViewAdapter shopGoodListViewAdapter;

    //申明其他
    private PWToast newToast;

    private boolean hasHidden = false;

    private Activity activity;

    private final Handler fragmentPageShopHandler = new FragmentPageShopHandler(this);

    private static class FragmentPageShopHandler extends Handler {
        private final WeakReference<FragmentPageShop> mActivity;

        public FragmentPageShopHandler(FragmentPageShop activity) {
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

    /**
     * 处理Message
     *
     * @param msg
     */
    private void dealHandler(Message msg) {
        switch (msg.what) {
            case NoNetWorkOrNoCountView.BUTTON_CLICK_WITH_RELOAD://noView的按钮响应重新加载点击事件
                //重新加载购物车数据
                if (AppUtil.getNetWorkType(MyApplication.getInstance()) == 0) {
                    newToast.setTextAndShow(R.string.no_network, Common.TOAST_SHORT_TIME);
                    return;
                }
                PictureAirLog.v(TAG, "onclick with reload");
                showPWProgressDialog();
                //重新加载数据
                initData(true);
                break;

            default:
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        View view = inflater.inflate(R.layout.fragment_shop, container, false);
        newToast = new PWToast(MyApplication.getInstance());
        setImmersiveMode(view);
        //找控件
        shoppingBag = (ImageView) view.findViewById(R.id.frag3_cart);
        cartCountTextView = (TextView) view.findViewById(R.id.textview_cart_count);
        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        xListView = (ListView) view.findViewById(R.id.shopListView);
        noNetWorkOrNoCountView = (NoNetWorkOrNoCountView) view.findViewById(R.id.shopNoNetWorkView);

        //初始化数据
        cartCount = SPUtils.getInt(activity, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, 0);//获取购物车数量
        currency =  Common.DEFAULT_CURRENCY;//SPUtils.getString(activity, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CURRENCY, Common.DEFAULT_CURRENCY);//获取币种
        //设置购物车数量
        if (cartCount <= 0) {
            cartCountTextView.setVisibility(View.INVISIBLE);
        } else {
            cartCountTextView.setVisibility(View.VISIBLE);
            cartCountTextView.setText(cartCount + "");
        }
        allGoodsList = new ArrayList<>();//初始化商品列表
        showPWProgressDialog();
        PictureAirLog.out("currency---->" + currency);
        shopGoodListViewAdapter = new ShopGoodListViewAdapter(allGoodsList, activity, currency);
        xListView.setAdapter(shopGoodListViewAdapter);
        //绑定监听
        refreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        refreshLayout.setEnabled(true);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                PictureAirLog.out("start refresh");
                initData(true);
            }
        });

        shoppingBag.setOnClickListener(this);
        cartCountTextView.setOnClickListener(this);
        xListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent;
                if (allGoodsList == null || allGoodsList.get(position) == null) {
                    return;
                }
                //根据类型判断 为0,虚拟商品（PP+、Digital Photo）
                if (allGoodsList.get(position).getEntityType() == 0 && allGoodsList.get(position).getName().equals(Common.GOOD_NAME_PPP)) {
                    intent = new Intent(activity, PPPDetailProductActivity.class);
                    intent.putExtra("goods", allGoodsList.get(position));
                } else {
                    intent = new Intent(activity, DetailProductActivity.class);
                    //从第二张开始显示
                    intent.putExtra("goods", allGoodsList.get(position));
                }
                FragmentPageShop.this.startActivity(intent);
            }
        });

        initData(false);//初始化数据
        return view;
    }

    /**
     * 初始化数据
     */
    public void initData(boolean isClearCache) {
        String goodsByACache = "";
        if (isClearCache) {
            //清空緩存，重新拉取数据
            ACache.get(MyApplication.getInstance()).remove(Common.ALL_GOODS);
            ACache.get(MyApplication.getInstance()).remove(Common.ACACHE_ADDRESS);
        } else {
            //从缓层中获取数据
            goodsByACache = ACache.get(activity).getAsString(Common.ALL_GOODS);
        }
        PictureAirLog.v(TAG, "initData: goodsByACache: " + goodsByACache);
        getGoods(goodsByACache);
    }

    /**
     * 获取商品信息
     */
    private void getGoods(String goodsCache) {
        //从网络获取商品,先检查网络
        if (AppUtil.getNetWorkType(MyApplication.getInstance()) != 0) {
            Observable.just(goodsCache)
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Func1<String, Observable<JSONObject>>() {
                        @Override
                        public Observable<JSONObject> call(String s) {
                            if (!TextUtils.isEmpty(s)) {
                                return Observable.just(JSONObject.parseObject(s));

                            } else {
                                return API2.getGoods()
                                        .map(new Func1<JSONObject, JSONObject>() {
                                            @Override
                                            public JSONObject call(JSONObject jsonObject) {
                                                ACache.get(MyApplication.getInstance()).put(Common.ALL_GOODS, jsonObject.toString(), ACache.TIME_DAY);
                                                return jsonObject;
                                            }
                                        });
                            }
                        }
                    })
                    .map(new Func1<JSONObject, GoodsInfoJson>() {
                        @Override
                        public GoodsInfoJson call(JSONObject jsonObject) {
                            return JsonTools.parseObject(jsonObject, GoodsInfoJson.class);//GoodsInfoJson.getString()
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(this.<GoodsInfoJson>bindToLifecycle())
                    .subscribe(new RxSubscribe<GoodsInfoJson>() {
                        @Override
                        public void _onNext(GoodsInfoJson goodsInfoJson) {
                            PictureAirLog.v(TAG, "GET_GOODS_SUCCESS");
                            allGoodsList.clear();
                            if (goodsInfoJson != null && goodsInfoJson.getGoods().size() > 0) {
                                allGoodsList.addAll(goodsInfoJson.getGoods());
                                PictureAirLog.v(TAG, "goods size: " + allGoodsList.size());
                                refreshLayout.setVisibility(View.VISIBLE);
                                noNetWorkOrNoCountView.setVisibility(View.GONE);
                                //更新界面
                                shopGoodListViewAdapter.refresh(allGoodsList);
                                if (refreshLayout.isRefreshing()) {
                                    refreshLayout.setRefreshing(false);
                                }
                                dismissPWProgressDialog();

                                //获取收货地址列表
                                String addressByACache = ACache.get(MyApplication.getInstance()).getAsString(Common.ACACHE_ADDRESS);
                                PictureAirLog.i(TAG, "initData: addressByACache: " + addressByACache);
                                if (addressByACache == null || addressByACache.equals("")) {
                                    getOutLets();
                                }
                            } else {
                                _onError(401);
                            }

                        }

                        @Override
                        public void _onError(int status) {
                            //显示重新加载界面
                            if (refreshLayout.isRefreshing()) {
                                refreshLayout.setRefreshing(false);
                            }
                            dismissPWProgressDialog();
                            showNetWorkView();
                        }

                        @Override
                        public void onCompleted() {

                        }
                    });
        } else {
            if (refreshLayout.isRefreshing()) {
                refreshLayout.setRefreshing(false);
            }
            showNetWorkView();
        }
    }

    /**
     * 获取门店地址
     */
    private void getOutLets() {
        API2.getOutlets()
                .map(new Func1<JSONObject, JSONObject>() {
                    @Override
                    public JSONObject call(JSONObject jsonObject) {
                        PictureAirLog.d("get outlet success");
                        AddressJson addressJson = JsonTools.parseObject(jsonObject, AddressJson.class);
                        if (addressJson != null && addressJson.getOutlets().size() > 0) {
                            //存入缓存
                            if (TextUtils.isEmpty(ACache.get(MyApplication.getInstance()).getAsString(Common.ACACHE_ADDRESS))) {
                                ACache.get(MyApplication.getInstance()).put(Common.ACACHE_ADDRESS, jsonObject.toString(), ACache.TIME_DAY);
                            }
                        }
                        return jsonObject;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<JSONObject>bindToLifecycle())
                .subscribe(new RxSubscribe<JSONObject>() {
                    @Override
                    public void _onNext(JSONObject jsonObject) {

                    }

                    @Override
                    public void _onError(int status) {
                        dismissPWProgressDialog();
                    }

                    @Override
                    public void onCompleted() {
                        dismissPWProgressDialog();
                    }
                });
    }

    /**
     * 重新加载View
     */
    public void showNetWorkView() {
        refreshLayout.setVisibility(View.GONE);
        dismissPWProgressDialog();
        noNetWorkOrNoCountView.setVisibility(View.VISIBLE);
        noNetWorkOrNoCountView.setResult(R.string.no_network, R.string.click_button_reload, R.string.reload, R.drawable.no_network, fragmentPageShopHandler, true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.textview_cart_count:
            case R.id.frag3_cart:
                if (AppUtil.getNetWorkType(MyApplication.getInstance()) == 0) {
                    newToast.setTextAndShow(R.string.no_network, Common.TOAST_SHORT_TIME);
                    return;
                }
                Intent intent = new Intent(activity, CartActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasHidden) {
            PictureAirLog.out("truely resume----->shop");
            cartCount = SPUtils.getInt(activity, Common.SHARED_PREFERENCE_USERINFO_NAME, Common.CART_COUNT, 0);
            if (cartCount > 0) {
                cartCountTextView.setVisibility(View.VISIBLE);
                cartCountTextView.setText(cartCount + "");
            } else {
                cartCountTextView.setVisibility(View.INVISIBLE);
            }

        } else {
            PictureAirLog.out("fake resume----->shop");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentPageShopHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        hasHidden = hidden;
    }
}