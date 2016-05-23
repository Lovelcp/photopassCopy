package com.pictureair.photopass.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;

import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.activity.MainTabActivity;
import com.pictureair.photopass.activity.OrderActivity;
import com.pictureair.photopass.activity.OrderDetailActivity;
import com.pictureair.photopass.adapter.OrderListViewAdapter;
import com.pictureair.photopass.entity.CartItemInfo;
import com.pictureair.photopass.entity.OrderInfo;
import com.pictureair.photopass.entity.OrderProductInfo;
import com.pictureair.photopass.eventbus.BaseBusEvent;
import com.pictureair.photopass.eventbus.OrderFragmentEvent;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.OrderInfoDateSortUtil;
import com.pictureair.photopass.util.OrderProductDateSortUtil;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.ReflectionUtil;
import com.pictureair.photopass.util.UniversalImageLoadTool;
import com.pictureair.photopass.widget.MyToast;
import com.pictureair.photopass.widget.NoNetWorkOrNoCountView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.pictureair.photopass.util.AppManager;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;

/**
 * Created by bass on 16/4/25.
 */
public class OrderFragment extends Fragment {
    private final String TAG = "OrderFragment";
    private static OrderFragment orderFragment;
    private View view;
    private ExpandableListView orderListView;

    private ArrayList<OrderInfo> orderList;

    private List<OrderProductInfo> childlist;

    private OrderListViewAdapter allOrderAdapter;

    private String currency;

    private Context context;
    private MyToast myToast;
    private MyApplication application;
    private int tab = 0;

    private static final int REFRESH = 0X001;
    private static Handler mHandler;
    private SwipeRefreshLayout refreshLayout;


    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case API1.DELETE_ORDER_SUCCESS:
                    PictureAirLog.v(TAG, "DELETE_ORDER_SUCCESS");
                    // 移除第几个position。
                    Bundle b = msg.getData();
                    OrderInfo groupInfo = b.getParcelable("group");
                    ArrayList<CartItemInfo> childInfo = (ArrayList) b.getParcelableArrayList("child");

                    // 删除Delivery中的对象
//                    deliveryOrderList.remove(groupInfo);
                    switch (tab) {
                        case 0://未付款
                            break;
                        case 1://已付款，未收货
                            childlist.remove(childInfo);
                            break;
                        case 2://订单完成
                            //删除全部订单 中的对象
                            orderList.remove(groupInfo);
                            childlist.remove(childInfo);
                            break;
                        default:
                            break;
                    }
                    if (allOrderAdapter != null) {
                        allOrderAdapter.notifyDataSetChanged();
                    }
                    break;
                case API1.DELETE_ORDER_FAILED:
                    myToast.setTextAndShow(ReflectionUtil.getStringId(MyApplication.getInstance(), msg.arg1), Common.TOAST_SHORT_TIME);
                    break;

                case NoNetWorkOrNoCountView.BUTTON_CLICK_WITH_NO_RELOAD://noView的按钮响应非重新加载的点击事件
                    //去跳转到商品页面
                    //需要删除页面，保证只剩下mainTab页面，
                    AppManager.getInstance().killOtherActivity(MainTabActivity.class);
                    //同时将mainTab切换到shop Tab
                    application.setMainTabIndex(3);
                    break;
                case REFRESH:
                    myToast.setTextAndShow("000", Common.TOAST_SHORT_TIME);
                    break;
            }
            return false;
        }
    });

    public OrderFragment() {
    }

    public static OrderFragment getInstance(Handler handler, ArrayList<OrderInfo> orderInfos3,
                                            List<OrderProductInfo> orderChildlist3, String currency, int tab
    ) {
        mHandler = handler;
        orderFragment = new OrderFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("orderList", orderInfos3);
        bundle.putSerializable("orderChildList", (Serializable) orderChildlist3);

        bundle.putString("currency", currency);
        bundle.putInt("tab", tab);

        orderFragment.setArguments(bundle);
        return orderFragment;
    }

    /**
     * 订单信息降序排列
     *
     * @param orderInfoList1
     * @return
     */
    private ArrayList<OrderInfo> OrderDateSort(ArrayList<OrderInfo> orderInfoList1) {
        Collections.sort(orderInfoList1, new OrderInfoDateSortUtil());
        return orderInfoList1;
    }

    /**
     * 商品信息信息降序排列
     *
     * @param orderInfoList1
     * @return
     */
    private List<OrderProductInfo> OrderProductDateSort(List<OrderProductInfo> orderInfoList1) {
        Collections.sort(orderInfoList1, new OrderProductDateSortUtil());
        return orderInfoList1;
    }

    @Override
    public void onAttach(Context context) {

        if (getArguments() != null) {

            orderList = getArguments().getParcelableArrayList("orderList");
            orderList = OrderDateSort(orderList);

            childlist = (List<OrderProductInfo>) getArguments().getSerializable("orderChildList");
            childlist = OrderProductDateSort(childlist);

            currency = getArguments().getString("currency");
            tab = getArguments().getInt("tab");
        }
        application = (MyApplication) getActivity().getApplication();
        this.myToast = new MyToast(context);
        this.context = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.order_list, container, false);
        }
        NoNetWorkOrNoCountView netWorkOrNoCountView = (NoNetWorkOrNoCountView) view.findViewById(R.id.nonetwork);

        orderListView = (ExpandableListView) view.findViewById(R.id.order_viewpager_listview1);

        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        refreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        refreshLayout.setEnabled(true);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                PictureAirLog.out("start refresh");
                refreshLayout.setEnabled(false);
                mHandler.sendEmptyMessage(OrderActivity.REFRESH);
            }
        });

        allOrderAdapter = new OrderListViewAdapter(context, orderList, childlist, currency, handler);
        orderListView.setGroupIndicator(null);
        orderListView.setAdapter(allOrderAdapter);
        switch (tab) {
            case 0://未付款
                orderListView.setOnGroupClickListener(new GroupOnClick(0));
                orderListView.setOnChildClickListener(new ChildOnClick(0));
                break;
            case 1://已付款，未收货
                orderListView.setOnGroupClickListener(new GroupOnClick(1));
                orderListView.setOnChildClickListener(new ChildOnClick(1));
                break;
            case 2://订单完成
                orderListView.setOnGroupClickListener(new GroupOnClick(2));
                orderListView.setOnChildClickListener(new ChildOnClick(2));
                break;
            default:
                break;
        }

        if (orderList == null || orderList.size() == 0) {
            orderListView.setVisibility(View.GONE);
            refreshLayout.setVisibility(View.GONE);
            netWorkOrNoCountView.setResult(R.string.order_empty_resultString, R.string.want_to_buy, R.string.order_empty_buttonString, R.drawable.no_order_data, handler, false);
            netWorkOrNoCountView.setVisibility(View.VISIBLE);
        } else {
            expandGropu(tab);
        }

        if (null != refreshLayout)
            orderListView.setOnScrollListener(new SwipeListViewOnScrollListener(refreshLayout, new PauseOnScrollListener(UniversalImageLoadTool.getImageLoader(), true, true)));

        return view;
    }

    //expandablelistview展开child项
    public void expandGropu(int current) {
        switch (current) {
            case 0:
            case 1:
            case 2:
                for (int i = 0; i < orderList.size(); i++) {
                    if (orderListView != null) {
                        orderListView.expandGroup(i);
                    }
                }
                break;
            default:
                break;
        }
    }

    //group点击事件监听
    private class GroupOnClick implements ExpandableListView.OnGroupClickListener {
        private int index;

        public GroupOnClick(int index) {
            this.index = index;
        }

        @Override
        public boolean onGroupClick(ExpandableListView parent, View v,
                                    int groupPosition, long id) {
            startNewActivity(index, groupPosition);
            return true;
        }
    }

    //child的点击事件
    private class ChildOnClick implements ExpandableListView.OnChildClickListener {

        private int index;

        public ChildOnClick(int index) {
            this.index = index;
        }

        @Override
        public boolean onChildClick(ExpandableListView parent, View v,
                                    int groupPosition, int childPosition, long id) {
            startNewActivity(index, groupPosition);
            return true;
        }

    }

    //跳转到订单详情界面
    private void startNewActivity(int index, int groupPosition) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(context, OrderDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("groupitem", orderList.get(groupPosition));
        bundle.putSerializable("childitemlist",  childlist.get(groupPosition).getCartItemInfos());

        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    public void onResume() {
        PictureAirLog.out(TAG + "orderFragment----------------->");
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        PictureAirLog.out(TAG + "orderFragment onDetach----------------->");

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe
    public void onUserEvent(BaseBusEvent baseBusEvent) {
        if (baseBusEvent instanceof OrderFragmentEvent) {
            PictureAirLog.out(TAG + "orderFragment onUserEvent----------------->");
            OrderFragmentEvent orderFragmentEvent = (OrderFragmentEvent) baseBusEvent;

            switch (orderFragmentEvent.getRequest()) {
                case 1:
                    if (refreshLayout.isRefreshing()) {
                        refreshLayout.setEnabled(true);
                        refreshLayout.setRefreshing(false);
                    }
                    EventBus.getDefault().removeStickyEvent(orderFragmentEvent);

                    break;

                case 0:
                    if (null != refreshLayout && refreshLayout.isRefreshing()) {
                        refreshLayout.setEnabled(true);
                        refreshLayout.setRefreshing(false);
                    }
                    currency = orderFragmentEvent.getCurrency();
                    switch (tab) {
                        case 0://未付款
                            orderList = orderFragmentEvent.getOrderInfos1();
                            orderList = OrderDateSort(orderList);

                            childlist = orderFragmentEvent.getOrderChildlist1();
                            childlist = OrderProductDateSort(childlist);
                            EventBus.getDefault().removeStickyEvent(orderFragmentEvent);
                            break;
                        case 1://已付款，未收货
                            orderList = orderFragmentEvent.getOrderInfos2();
                            orderList = OrderDateSort(orderList);

                            childlist = orderFragmentEvent.getOrderChildlist2();
                            childlist = OrderProductDateSort(childlist);
                            EventBus.getDefault().removeStickyEvent(orderFragmentEvent);
                            break;
                        case 2://订单完成
                            orderList = orderFragmentEvent.getOrderInfos3();
                            orderList = OrderDateSort(orderList);

                            childlist = orderFragmentEvent.getOrderChildlist3();
                            childlist = OrderProductDateSort(childlist);
                            EventBus.getDefault().removeStickyEvent(orderFragmentEvent);
                            break;
                        default:
                            break;
                    }
                    if (null != allOrderAdapter) {
                        allOrderAdapter.notifyDataSetChanged();
                    }
                    break;

                default:
                    break;

            }

        }
    }

    /**
     * 解决SwipeRefreshLayout下拉刷新冲突
     */
    public static class SwipeListViewOnScrollListener implements AbsListView.OnScrollListener {

        private SwipeRefreshLayout mSwipeView;
        private AbsListView.OnScrollListener mOnScrollListener;

        public SwipeListViewOnScrollListener(SwipeRefreshLayout swipeView) {
            mSwipeView = swipeView;
        }

        public SwipeListViewOnScrollListener(SwipeRefreshLayout swipeView,
                                             AbsListView.OnScrollListener onScrollListener) {
            mSwipeView = swipeView;
            mOnScrollListener = onScrollListener;
        }

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {
        }

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            View firstView = absListView.getChildAt(firstVisibleItem);

            // 当firstVisibleItem是第0位。如果firstView==null说明列表为空，需要刷新;或者top==0说明已经到达列表顶部, 也需要刷新
            if (firstVisibleItem == 0 && (firstView == null || firstView.getTop() == 0)) {
                mSwipeView.setEnabled(true);
            } else {
                mSwipeView.setEnabled(false);
            }
            if (null != mOnScrollListener) {
                mOnScrollListener.onScroll(absListView, firstVisibleItem, visibleItemCount, totalItemCount);
            }
        }
    }


}
