package com.pictureair.photopass.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.customDialog.PWDialog;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.ReflectionUtil;
import com.pictureair.photopass.util.RegisterTool;
import com.pictureair.photopass.util.SignAndLoginUtil;
import com.pictureair.photopass.widget.CustomButtonFont;
import com.pictureair.photopass.widget.EditTextWithClear;
import com.pictureair.photopass.widget.PWToast;
import com.pictureair.photopass.widget.RegisterOrForgetCallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OtherLoginActivity extends BaseActivity implements OnClickListener, SignAndLoginUtil.OnLoginSuccessListener ,TextWatcher, PWDialog.OnPWDialogClickListener, RegisterOrForgetCallback{
    private static final String TAG = "OtherLoginActivity";
    // 申明控件
    private Button login;//
    private EditTextWithClear userName, password, shorMessage;

    private TextView forgot,tv_country, tv_country_num, sign;
    private LinearLayout rl_country, ll_email, ll_message;// 国家
    private CustomButtonFont btn_next;
    private String countryCode = "86";
    private String country = "";
    private String phoneStr = "";
    private String smsStr = "";
    private PWDialog pictureWorksDialog;
    private RegisterTool registerTool;
    private boolean countDownFinish = true;

//	private ImageView back;

    // 申明变量
    private StringBuffer loginUrl = new StringBuffer();// 登录的url

    // 申明其他类
    private PWToast myToast;

    private SignAndLoginUtil signAndLoginUtil;
    private boolean mIsMsgLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_login);
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {

        Intent intent = getIntent();
        if (intent != null) {
            mIsMsgLogin = intent.getBooleanExtra("msgLogin", false);
        }
        loginUrl.append(Common.BASE_URL_TEST).append(Common.LOGIN);// 链接地址
        myToast = new PWToast(OtherLoginActivity.this);// 获取toast
        signAndLoginUtil = new SignAndLoginUtil(this, this);
        setTopLeftValueAndShow(R.drawable.back_white, true);
//        setTopTitleShow(R.string.user_login);
        ll_email = (LinearLayout) findViewById(R.id.other_login_email);
        ll_message = (LinearLayout) findViewById(R.id.other_login_message);

        if (!mIsMsgLogin) {

            login = (Button) findViewById(R.id.btnOtherLogin);
            userName = (EditTextWithClear) findViewById(R.id.otherLogin_email);
            password = (EditTextWithClear) findViewById(R.id.otherLogin_password);
            forgot = (TextView) findViewById(R.id.forgot);
            ll_email.setVisibility(View.VISIBLE);
            ll_message.setVisibility(View.GONE);
            login.setOnClickListener(this);
            forgot.setOnClickListener(this);

            login.setTypeface(MyApplication.getInstance().getFontBold());

            userName.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // TODO Auto-generated method stub
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    /* 隐藏软键盘 */
                        userName.clearFocus();
                        password.requestFocus();
                        return true;
                    }
                    return false;
                }
            });
            password.setOnEditorActionListener(new OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView v, int actionId,
                                              KeyEvent event) {
                    // TODO Auto-generated method stub
				/* 判断是否是“GO”键 */
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        hideInputMethodManager(v);//
                        login.performClick(); //
                        return true;
                    }
                    return false;
                }
            });
        } else {
            registerTool = new RegisterTool(MyApplication.getTokenId(), OtherLoginActivity.this, this, MyApplication.getInstance().getLanguageType());
            registerTool.setWhatActivity("sms");

            rl_country = (LinearLayout) findViewById(R.id.other_login_rl_country);
            tv_country = (TextView) findViewById(R.id.other_login_tv_country);
            tv_country_num = (TextView) findViewById(R.id.other_login_tv_country_num);
            userName = (EditTextWithClear) findViewById(R.id.other_login_username);
            login = (Button) findViewById(R.id.btnOtherLogin2);
            shorMessage = (EditTextWithClear) findViewById(R.id.other_login_verification);
            btn_next = (CustomButtonFont) findViewById(R.id.other_btn_next);
            ll_email.setVisibility(View.GONE);
            ll_message.setVisibility(View.VISIBLE);
            sign = (TextView) findViewById(R.id.other_login_sign);
            sign.setVisibility(View.VISIBLE);
            rl_country.setOnClickListener(this);
            btn_next.setOnClickListener(this);
            sign.setOnClickListener(this);
            userName.addTextChangedListener(this);
            shorMessage.addTextChangedListener(this);
            login.setOnClickListener(this);


            userName.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // TODO Auto-generated method stub
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    /* 隐藏软键盘 */
                        userName.clearFocus();
                        shorMessage.requestFocus();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * 点击键盘之外，隐藏键盘
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (AppUtil.isShouldHideInput(v, ev)) {
                hideInputMethodManager(v);
            }
            return super.dispatchTouchEvent(ev);
        }
        // 必不可少，否则所有的组件都不会有TouchEvent了
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.forgot:
				startActivity(new Intent(OtherLoginActivity.this,FindPasswordActivity.class));
                break;

            case R.id.btnOtherLogin:
                PictureAirLog.out("登录按钮");
                // 登录
                hideInputMethodManager(v);
                if (userName.getText().toString().trim().isEmpty()) {
                    myToast.setTextAndShow(R.string.email_is_empty,
                            Common.TOAST_SHORT_TIME);
                    break;
                }
                if (!AppUtil.isEmail(userName.getText().toString().trim())) {
                    myToast.setTextAndShow(R.string.email_error,
                            Common.TOAST_SHORT_TIME);
                    break;
                }

                String pwd = password.getText().toString();
                // 判断密码的合法性
                switch (AppUtil.checkPwd(pwd, pwd)) {
                    case AppUtil.PWD_ALL_SAPCE:// 全部为空格
                        myToast.setTextAndShow(R.string.pwd_no_all_space,
                                Common.TOAST_SHORT_TIME);
                        break;

                    case AppUtil.PWD_SHORT:// 小于6位
                    case AppUtil.PWD_AVAILABLE:// 密码可用
                        signAndLoginUtil.start(userName.getText().toString().trim(), password
                                .getText().toString(), false, false, null, null, null, null, null, null);
                        break;

                    case AppUtil.PWD_EMPTY:// 空
                        myToast.setTextAndShow(R.string.modify_password_empty_hint,
                                Common.TOAST_SHORT_TIME);
                        break;

//                        myToast.setTextAndShow(R.string.notify_password_hint,
//                                Common.TOAST_SHORT_TIME);
//
//                        break;

                    case AppUtil.PWD_HEAD_OR_FOOT_IS_SPACE:// 密码首尾不能为空格
                        myToast.setTextAndShow(R.string.pwd_head_or_foot_space,
                                Common.TOAST_SHORT_TIME);
                        break;
                }
                break;

            case R.id.btnOtherLogin2:
                if (!checkPhoneNumber()) {
                    myToast.setTextAndShow(R.string.smssdk_write_right_mobile_phone, Common.TOAST_SHORT_TIME);
                    return;
                }
                signAndLoginUtil.start(countryCode + userName.getText().toString().trim(), null, false, false, null, null, null, null,Common.LOGINTYPE, shorMessage.getText().toString().trim());

                break;

            case R.id.other_login_rl_country:
                Intent intent1 = new Intent();
                intent1.setClass(OtherLoginActivity.this, SelectCountryActivity.class);
                startActivityForResult(intent1, SelectCountryActivity.requestCountry);
                break;

            case R.id.other_login_sign:
                startActivity(new Intent(OtherLoginActivity.this,
                        RegisterOrForgetActivity.class).putExtra("activity","sign"));
//                Intent intent = new Intent(this, OtherRegisterActivity.class);
//                startActivity(intent);
                break;

            case R.id.other_btn_next:
                if (!checkPhoneNumber()) {
                    myToast.setTextAndShow(R.string.smssdk_write_right_mobile_phone, Common.TOAST_SHORT_TIME);
                    return;
                }
                showPwDialog();
                break;

            default:
                break;
        }
    }


    /**
     * 点击发送验证码过后 检查电话号码
     */
    private boolean checkPhoneNumber() {
        boolean tem = true;
        if (countryCode.equals("86")) {
//            Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$");
            Pattern p = Pattern.compile("^1(3|5|7|8|4)\\d{9}");
            Matcher matcher = p.matcher(phoneStr);
            tem = matcher.matches();
        }
        return tem;
    }

    private void showPwDialog() {
        String dialogMsg = "+" + countryCode + " " + phoneStr;
        dialogMsg = getString(R.string.smssdk_make_sure_mobile_detail,dialogMsg);
        PictureAirLog.out("diamsg--->" + dialogMsg);

        if (pictureWorksDialog == null) {
            pictureWorksDialog = new PWDialog(OtherLoginActivity.this)
                    .setPWDialogMessage(dialogMsg)
                    .setPWDialogNegativeButton(R.string.cancel1)
                    .setPWDialogPositiveButton(R.string.ok)
                    .setPWDialogContentCenter(false)
                    .setOnPWDialogClickListener(this)
                    .pwDialogCreate();
        }
        pictureWorksDialog.setPWDialogMessage(dialogMsg)
                .pwDilogShow();
    }

    /**
     * 隐藏软键盘
     */
    private void hideInputMethodManager(View v) {
		/* 隐藏软键盘 */
        InputMethodManager imm = (InputMethodManager) v.getContext()
                .getSystemService(INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
    }

    @Override
    public void loginSuccess() {
        Intent intent = new Intent();
        intent.setClass(OtherLoginActivity.this, MainTabActivity.class);
        startActivity(intent);
        AppManager.getInstance().killActivity(LoginActivity.class);
        finish();
    }

    @Override
    public void TopViewClick(View view) {
        super.TopViewClick(view);
        switch (view.getId()) {
            case R.id.topLeftView:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != registerTool) {
            registerTool.onDestroy();
        }

        if (signAndLoginUtil != null) {
            signAndLoginUtil.destroy();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode!=0 && requestCode == SelectCountryActivity.requestCountry) {
            String[] strs = data.getExtras().getStringArray("country");
//            Toast.makeText(getContext(),"国家名称：" + strs[0] + "\n" + "国家区号：" + strs[1] + "\n" + "国家简码：" + strs[4],Toast.LENGTH_SHORT).show();
            if (null != strs) {
                countryCode = strs[1];
                country = strs[0];
                tv_country.setText(country);
                tv_country_num.setText("+" + countryCode);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        phoneStr = userName.getText().toString().trim();
        smsStr = shorMessage.getText().toString().trim();

        if (countDownFinish) {
            if (phoneStr.length() > 0) {
                btn_next.setEnabled(true);
            } else {
                btn_next.setEnabled(false);
            }
        }

        if (phoneStr.length() > 0 && smsStr.length() > 0) {
            login.setEnabled(true);
        } else {
            login.setEnabled(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onPWDialogButtonClicked(int which, int dialogId) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            registerTool.sendSMSValidateCode(countryCode + phoneStr);
        }
    }

    @Override
    public void goneDialog() {
        dismissPWProgressDialog();
    }

    @Override
    public void showDialog() {
        showPWProgressDialog();
    }

    @Override
    public void onFai(int StringId) {
        myToast.setTextAndShow(ReflectionUtil.getStringId(OtherLoginActivity.this, StringId), Common.TOAST_SHORT_TIME);
    }

    @Override
    public void onFai(String StringId) {
        myToast.setTextAndShow(StringId, Common.TOAST_SHORT_TIME);
    }

    @Override
    public void onSuccess() {
        startActivity(new Intent(this, MainTabActivity.class));
        finish();
    }

    @Override
    public void countDown(int time) {
        if (time == 0) {
            countDownFinish = true;
            btn_next.setText(R.string.smssdk_send_verification_code);// 再次发送验证码
            if (phoneStr.length() > 0) {
                btn_next.setEnabled(true);
            } else {
                btn_next.setEnabled(false);
            }
        } else {
//            PictureAirLog.out("------>倒计时");
            countDownFinish = false;
            String unReceive = getString(R.string.smssdk_receive_msg, time);// 倒计时
            btn_next.setText(Html.fromHtml(unReceive));
            btn_next.setEnabled(false);
        }
    }

    @Override
    public void nextPageForget() {

    }
}
