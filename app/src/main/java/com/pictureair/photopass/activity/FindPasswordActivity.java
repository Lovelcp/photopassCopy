package com.pictureair.photopass.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.SignAndLoginUtil;
import com.pictureair.photopass.widget.CheckUpdateManager;
import com.pictureair.photopass.widget.CustomProgressDialog;
import com.pictureair.photopass.widget.MyToast;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.gui.CustomButtonFont;
import cn.smssdk.gui.EditTextWithClear;
import cn.smssdk.gui.RegisterPage;

/**
 * 忘记密码：2个页面
 * 输入邮箱页面
 * 提示页面
 *
 * @author Bass
 */
public class FindPasswordActivity extends BaseActivity implements OnClickListener {
    private static final String TAG = "FindPasswordActivity";
    private Context context;
    private MyToast myToast;
    private CustomProgressDialog customProgressDialog;
    //输入邮箱页面
    private LinearLayout llFindPwdContent;
    private EditTextWithClear etEmail;
    private CustomButtonFont btnSendEmail;
    //提示页面
    private LinearLayout llContenHint;
    private CustomButtonFont btnBackLoginPage;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null != customProgressDialog && customProgressDialog.isShowing())
                customProgressDialog.dismiss();
            switch (msg.what) {
                case API1.FIND_PWD_SUCCESS:
                    llFindPwdContent.setVisibility(View.GONE);
                    llContenHint.setVisibility(View.VISIBLE);
                    break;

                case API1.FIND_PWD_FAILED:


                    myToast.setTextAndShow(R.string.failed,Common.TOAST_SHORT_TIME);
                    break;

                default:
                    break;

            }
        }
    };

    /**
     * 点击键盘之外，隐藏键盘
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pwd);
        context = this;
        initview();// 初始化

    }

    private void initview() {
        myToast = new MyToast(context);
        setTopLeftValueAndShow(R.drawable.back_white, true);
        setTopTitleShow(R.string.reset_pwd);

        llFindPwdContent = (LinearLayout) findViewById(R.id.ll_find_pwd_content);
        etEmail = (EditTextWithClear) findViewById(R.id.et_email);
        btnSendEmail = (CustomButtonFont) findViewById(R.id.btn_send_email);
        //hint page
        llContenHint = (LinearLayout) findViewById(R.id.ll_conten_hint);
        btnBackLoginPage = (CustomButtonFont) findViewById(R.id.btn_back_login);

        btnSendEmail.setOnClickListener(this);
        btnBackLoginPage.setOnClickListener(this);

        etEmail.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
				/* 判断是否是“GO”键 */
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    hideInputMethodManager(v);
                    btnSendEmail.performClick();
                    return true;
                }
                return false;
            }
        });

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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back_login:
                backLogin();
                break;

            case R.id.btn_send_email:
                hideInputMethodManager(v);

                String etEmailStr = etEmail.getText().toString().trim();
                if (etEmailStr.isEmpty()) {
                    myToast.setTextAndShow(R.string.email_is_empty,
                            Common.TOAST_SHORT_TIME);
                    return;
                }
                if (!AppUtil.isEmail(etEmailStr)) {
                    myToast.setTextAndShow(R.string.email_error,
                            Common.TOAST_SHORT_TIME);
                    return;
                }

                customProgressDialog = CustomProgressDialog.show(context, context.getString(R.string.is_loading), false, null);
                API1.findPwd(mHandler, etEmailStr, null, null);
                break;

            default:
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (AppUtil.isShouldHideInput(v, ev)) {
                hideInputMethodManager(v);
            }
            return super.dispatchTouchEvent(ev);
        }
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
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
    protected void onDestroy() {
        super.onDestroy();
        if (null != mHandler)
            mHandler.removeCallbacksAndMessages(null);
    }

    private void backLogin() {
//        startActivity(new Intent(context, OtherLoginActivity.class));
        finish();
    }

}
