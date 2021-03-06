package com.pictureair.hkdlphotopass.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.pictureair.hkdlphotopass.MyApplication;
import com.pictureair.hkdlphotopass.R;
import com.pictureworks.android.util.AppManager;
import com.pictureworks.android.util.AppUtil;
import com.pictureworks.android.util.Common;
import com.pictureworks.android.util.PictureAirLog;
import com.pictureworks.android.util.SignAndLoginUtil;
import com.pictureworks.android.widget.EditTextWithClear;
import com.pictureworks.android.widget.MyToast;

public class OtherLoginActivity extends BaseActivity implements OnClickListener, SignAndLoginUtil.OnLoginSuccessListener {
    private static final String TAG = "OtherLoginActivity";
    // 申明控件
    private Button sign, login;
    private EditTextWithClear userName, password;

    private TextView forgot;
//	private ImageView back;

    // 申明变量
    private StringBuffer loginUrl = new StringBuffer();// 登录的url

    // 申明其他类
    private MyToast myToast;

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
        loginUrl.append(Common.BASE_URL_TEST).append(Common.LOGIN);// 链接地址
        myToast = new MyToast(OtherLoginActivity.this);// 获取toast
        setTopLeftValueAndShow(R.drawable.back_white, true);
        setTopTitleShow(R.string.user_login);
        sign = (Button) findViewById(R.id.sign);
        login = (Button) findViewById(R.id.btnOtherLogin);
        userName = (EditTextWithClear) findViewById(R.id.otherLogin_email);
        password = (EditTextWithClear) findViewById(R.id.otherLogin_password);
        forgot = (TextView) findViewById(R.id.forgot);
        sign.setOnClickListener(this);
        login.setOnClickListener(this);
        forgot.setOnClickListener(this);

        login.setTypeface(MyApplication.getInstance().getFontBold());
        sign.setTypeface(MyApplication.getInstance().getFontBold());

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
                        new SignAndLoginUtil(OtherLoginActivity.this, userName.getText().toString().trim(), password
                                .getText().toString(), false, false, null, null, null, null, Common.APP_TYPE_HKDLPP, OtherLoginActivity.this);
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

            case R.id.sign:
                Intent intent = new Intent(this, OtherRegisterActivity.class);
                startActivity(intent);
                break;

            default:
                break;
        }
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

}
