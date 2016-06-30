package com.pictureair.photopass.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.loopj.android.http.AsyncHttpClient;
import com.pictureair.photopass.R;
import com.pictureair.photopass.adapter.SendAddressAdapter;
import com.pictureair.photopass.entity.InvoiceInfo;
import com.pictureair.photopass.entity.SendAddress;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.JsonUtil;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.widget.EditTextWithClear;
import com.pictureair.photopass.widget.PWToast;
import com.pictureair.photopass.widget.NoScrollListView;
import com.pictureair.photopass.widget.PWToast;
import com.pictureair.photopass.widget.PictureWorksDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class InvoiceActivity extends BaseActivity implements View.OnClickListener {
    private final static int ADD_ADDRESS=101;
    private final static int MODI_ADDRESS=102;
    //是否开发票
    private boolean checkInvoice=true;
    private RelativeLayout personRl;
    private RelativeLayout companyRl;
    private RelativeLayout nocheckRl;
    private RelativeLayout photoRl;
    private RelativeLayout serviceRl;
    private ImageButton personIb;
    private ImageButton companyIb;
    private ImageButton photoIb;
    private ImageButton serviceIb;
    private ImageButton noInvoice;
    private ImageView backIV;
    private EditTextWithClear editText;
    private TextView info;
    private NoScrollListView listAddress;
    private RelativeLayout sendAddressRl,newAddressRl;
    private ScrollView scrollView;
    private Button newAddressBtn;
    private Button okBtn;
    private View lineTopList;
    private List<SendAddress> listData;
    private SendAddressAdapter addressAdapter;
    private SendAddress newAddAddress;
    private ImageView arrowIv;
    private int curEditItemPosition=-1;
    //当前发票所有信息
    private InvoiceInfo invoiceInfo;
    private final Handler invoiceHandler = new InvoiceHandler(this);


    private static class InvoiceHandler extends Handler {
        private final WeakReference<InvoiceActivity> mActivity;

        public InvoiceHandler(InvoiceActivity activity) {
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
            case API1.ADDRESS_LIST_SUCCESS://获取所有地址列表
                initAddressData(msg);
                break;
            case API1.ADD_ADDRESS_LIST_SUCCESS://添加新地址
                addAddressItem(newAddAddress,msg);
                break;
            case API1.MODIFY_ADDRESS_LIST_SUCCESS://修改地址
                updateAddressItem(newAddAddress,curEditItemPosition);
            case API1.MODIFY_ADDRESS_LIST_FAILED://修改地址失败
                addressAdapter.setModifying(false);
                break;
            case API1.DELETE_ADDRESS_LIST_SUCCESS://删除地址
                deleteAddressItem(curEditItemPosition);
                break;

        }
    }

    private void getAddressData(Message msg) {
        JSONObject resultJsonObject = (JSONObject) msg.obj;

        listData=JsonUtil.getAddressList(resultJsonObject);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_invoice);
        initView();
        invoiceInfo = new InvoiceInfo();
        fillData();
        loadData();
    }


    private void fillData(){
        if(null != getIntent().getParcelableExtra("invoiceInfo")) {
            invoiceInfo = getIntent().getParcelableExtra("invoiceInfo");

            if (invoiceInfo.getTitle() == InvoiceInfo.PERSONAL) {
                personIb.setImageResource(R.drawable.sele);
                companyIb.setImageResource(R.drawable.nosele);
                if (editText.getVisibility() == View.VISIBLE)
                    editText.setVisibility(View.GONE);

            } else {
                personIb.setImageResource(R.drawable.nosele);
                companyIb.setImageResource(R.drawable.sele);
                if (editText.getVisibility() != View.VISIBLE)
                    editText.setVisibility(View.VISIBLE);
                editText.setText(invoiceInfo.getCompanyName());
            }
            checkInvoice = invoiceInfo.isNeedInvoice();
            if (checkInvoice) {
                noInvoice.setImageResource(R.drawable.nosele);
            } else {
                noInvoice.setImageResource(R.drawable.sele);
            }

        }
    }

    private void initView() {
        listData=new ArrayList<SendAddress>();
        scrollView= (ScrollView) findViewById(R.id.invoice_scrollview);
        personRl=(RelativeLayout) findViewById(R.id.invoice_personal_rl);
        personIb=(ImageButton) findViewById(R.id.invoice_personal_ib);
        companyRl=(RelativeLayout) findViewById(R.id.invoice_company_rl);
        companyIb=(ImageButton) findViewById(R.id.invoice_company_ib);
//        photoRl=(RelativeLayout) findViewById(R.id.invoice_photo_rl);
//        photoIb=(ImageButton) findViewById(R.id.invoice_photo_ib);
//        serviceRl=(RelativeLayout) findViewById(R.id.invoice_service_rl);
//        serviceIb=(ImageButton) findViewById(R.id.invoice_service_ib);
        nocheckRl= (RelativeLayout) findViewById(R.id.invoice_nocheck);
        noInvoice=(ImageButton) findViewById(R.id.invoice_nocheck_ib);
        editText= (EditTextWithClear) findViewById(R.id.invoice_et);
        listAddress= (NoScrollListView) findViewById(R.id.invoice_address_list);
        sendAddressRl=(RelativeLayout) findViewById(R.id.invoice_new_addr_rl);
        newAddressRl=(RelativeLayout) findViewById(R.id.invoice_new_address_rl);
        newAddressBtn= (Button) findViewById(R.id.invoice_new_address_btn);
        okBtn= (Button) findViewById(R.id.invoice_btn);
        arrowIv= (ImageView) findViewById(R.id.arrow_invoice_iv);
        backIV= (ImageView) findViewById(R.id.invoice_back);
        lineTopList=findViewById(R.id.invoice_line);

        personRl.setOnClickListener(this);
        personIb.setOnClickListener(this);
        companyRl.setOnClickListener(this);
        companyIb.setOnClickListener(this);
//        photoRl.setOnClickListener(this);
//        photoIb.setOnClickListener(this);
//        serviceRl.setOnClickListener(this);
//        serviceIb.setOnClickListener(this);
        nocheckRl.setOnClickListener(this);
        noInvoice.setOnClickListener(this);
        backIV.setOnClickListener(this);
        sendAddressRl.setOnClickListener(this);
        okBtn.setOnClickListener(this);

        newAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(InvoiceActivity.this,NewAddressActivity.class);
                startActivityForResult(intent,ADD_ADDRESS);
            }
        });

//        personIb.setImageResource(R.drawable.sele);
//        photoIb.setImageResource(R.drawable.sele);
       setFilterListener();
    }

    //加载地址
    public void loadData(){
        API1.getInvoiceAddressList(invoiceHandler);
    }


    public void setFilterListener(){
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideInputMethodManager(v);
                return false;
            }
        });
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    hideInputMethodManager(v);
                }
            }
        });
        editText.addTextChangedListener(new TextWatcher() {
            int cou = 0;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cou = before + count;
                String editable = editText.getText().toString();
                String str = AppUtil.inputTextFilter(editable); //过滤特殊字符
                if (!editable.equals(str)) {
                    editText.setText(str);
                }
                editText.setSelection(editText.length());
                cou = editText.length();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    private void hideInputMethodManager(View v) {
        /* 隐藏软键盘 */
        InputMethodManager imm = (InputMethodManager) v.getContext()
                .getSystemService(INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.invoice_personal_ib:
            case R.id.invoice_personal_rl:
                personIb.setImageResource(R.drawable.sele);
                companyIb.setImageResource(R.drawable.nosele);
                if(editText.getVisibility()==View.VISIBLE)
                    editText.setVisibility(View.GONE);

                invoiceInfo.setTitle(InvoiceInfo.PERSONAL);
                invoiceInfo.setCompanyName("");
                break;
            case R.id.invoice_company_ib:
            case R.id.invoice_company_rl:
                personIb.setImageResource(R.drawable.nosele);
                companyIb.setImageResource(R.drawable.sele);
                if(editText.getVisibility()!=View.VISIBLE)
                    editText.setVisibility(View.VISIBLE);

                invoiceInfo.setTitle(InvoiceInfo.COMPANY);
                break;
//            case R.id.invoice_photo_ib:
//            case R.id.invoice_photo_rl:
//                photoIb.setImageResource(R.drawable.sele);
//                serviceIb.setImageResource(R.drawable.nosele);
//
//                invoiceInfo.setContent(InvoiceInfo.PHOTO);
//                break;
//            case R.id.invoice_service_ib:
//            case R.id.invoice_service_rl:
//                photoIb.setImageResource(R.drawable.nosele);
//                serviceIb.setImageResource(R.drawable.sele);
//
//                invoiceInfo.setContent(InvoiceInfo.PHOTO_SERVICE);
//                break;
            case R.id.invoice_new_addr_rl:
                if(listAddress.getVisibility()==View.VISIBLE){
                    lineTopList.setVisibility(View.GONE);
                    listAddress.setVisibility(View.GONE);
                    newAddressRl.setVisibility(View.GONE);
                    arrowIv.setRotation(0);
                }else{
                    arrowIv.setRotation(90);
                    listAddress.setVisibility(View.VISIBLE);
                    lineTopList.setVisibility(View.VISIBLE);
                    newAddressRl.setVisibility(View.VISIBLE);
                    if(null!=listData && listData.size()>0)
                        addressAdapter.notifyDataSetChanged();

                }
                break;
            case R.id.invoice_nocheck_ib:
            case R.id.invoice_nocheck:
                if(checkInvoice){
                    noInvoice.setImageResource(R.drawable.sele);
                }else{
                    noInvoice.setImageResource(R.drawable.nosele);
                }
                checkInvoice = !checkInvoice;
                invoiceInfo.setNeedInvoice(checkInvoice);
                break;
            case R.id.invoice_back:
                finish();
                break;
            case R.id.invoice_btn:
                setInvoice();
                break;
        }
    }

    private Handler mHandler=new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    });

    //初始化地址列表
    public void initAddressData(Message msg){
        getAddressData(msg);
        addressAdapter=new SendAddressAdapter(this,listData);
        addressAdapter.setAddressItemListener(new SendAddressAdapter.AddressItemListener() {
            @Override
            public void deleteItem(int position) {
                curEditItemPosition=position;
                String[] ids=new String[1];
                ids[0]=listData.get(position).getAddressId();
                API1.deleteInvoiceAddress(invoiceHandler,ids);
            }

            @Override
            public void editItem(int position) {
                curEditItemPosition=position;
                Intent intent=new Intent(InvoiceActivity.this,NewAddressActivity.class);
                Bundle b=new Bundle();
                b.putParcelable("address",listData.get(position));
                intent.putExtras(b);
                startActivityForResult(intent,MODI_ADDRESS);
            }

            @Override
            public void clickItem(int position,SendAddress address) {
                curEditItemPosition=position;
                newAddAddress=address;
                API1.modifyInvoiceAddress(invoiceHandler,address);
            }
        });

        listAddress.setAdapter(addressAdapter);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SendAddress address = null;
        if(resultCode==RESULT_OK){
            switch (requestCode){
                case ADD_ADDRESS://新增地址
                    address=parseAddressData(data);
                    if(null!=address){
                        newAddAddress=address;
                        API1.addInvoiceAddress(invoiceHandler,address);
                    }
                    break;
                case MODI_ADDRESS://修改地址
                    address=parseModifyAddressData(data,curEditItemPosition);
                    if(null!=address){
                        newAddAddress=address;
                        API1.modifyInvoiceAddress(invoiceHandler,address);
                    }
                    break;
            }
        }
    }

    //设置发票信息
    public void setInvoice(){
        if(checkData()) {
            Intent intent = new Intent();
            if (invoiceInfo.getTitle() == InvoiceInfo.COMPANY) {
                invoiceInfo.setCompanyName(editText.getText().toString());
            }
            if(null != addressAdapter && addressAdapter.getCurIndex() >= 0)
                invoiceInfo.setAddress(listData.get(addressAdapter.getCurIndex()));
            intent.putExtra("invoiceInfo", invoiceInfo);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    public boolean checkData(){
        if(invoiceInfo.isNeedInvoice()) {
            if (invoiceInfo.getTitle() != InvoiceInfo.PERSONAL && invoiceInfo.getTitle() != InvoiceInfo.COMPANY) {
                new PWToast(this).setTextAndShow(R.string.invoice_tips_title);
                return false;
            }
            if (null == addressAdapter || addressAdapter.getCurIndex() < 0) {
                new PWToast(this).setTextAndShow(R.string.invoice_tips_address);
                return false;
            }
        }
        return true;
    }

    public SendAddress parseAddressData(Intent data){
        SendAddress address=new SendAddress();
        if(null == data || TextUtils.isEmpty(data.getStringExtra("name")))
            return null;
        address.setName(data.getStringExtra("name"));
        address.setMobilePhone(data.getStringExtra("phone"));
        address.setProvince(data.getStringExtra("province"));
        address.setCity(data.getStringExtra("city"));
        address.setCountry(data.getStringExtra("country"));
        address.setDetailAddress(data.getStringExtra("address"));
        address.setSelected(true);
        return address;
    }

    public SendAddress parseModifyAddressData(Intent data,int position){
        SendAddress address = parseAddressData(data);
        if(address == null)
            return null;
        address.setAddressId(listData.get(curEditItemPosition).getAddressId());
        address.setSelected(listData.get(curEditItemPosition).isSelected());
        address.setZip(listData.get(curEditItemPosition).getZip());
        address.setArea(listData.get(curEditItemPosition).getArea());
        address.setTelePhone(listData.get(curEditItemPosition).getTelePhone());
        address.setSelected(true);
        return address;

    }

    //添加新地址
    public void addAddressItem(SendAddress address,Message msg){
        JSONObject resultJsonObject = (JSONObject) msg.obj;
        if(null != resultJsonObject && resultJsonObject.containsKey("addressId")){
            address.setAddressId(resultJsonObject.getString("addressId"));
        }
        listData.add(address);
        addressAdapter.setCurrentIndex(listData.size()-1);
        addressAdapter.setModifying(false);
        addressAdapter.notifyDataSetChanged();
    }

    //更新修改后的地址
    public void updateAddressItem(SendAddress address,int position){
        SendAddress sendAddress=listData.get(position);
        sendAddress.setName(address.getName());
        sendAddress.setMobilePhone(address.getMobilePhone());
        sendAddress.setProvince(address.getProvince());
        sendAddress.setCity(address.getCity());
        sendAddress.setCountry(address.getCountry());
        sendAddress.setDetailAddress(address.getDetailAddress());
        sendAddress.setSelected(address.isSelected());
        addressAdapter.setCurrentIndex(position);
        addressAdapter.setModifying(false);
        addressAdapter.notifyDataSetChanged();
    }

    public void deleteAddressItem(int position){
        listData.remove(position);
        addressAdapter.selectDefaultIndex(position);
        addressAdapter.notifyDataSetChanged();
    }
}
