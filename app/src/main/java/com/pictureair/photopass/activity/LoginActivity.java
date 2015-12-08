package com.pictureair.photopass.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.util.API;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.HttpUtil;
import com.pictureair.photopass.util.Installation;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.SignAndLoginUtil;
import com.pictureair.photopass.widget.CheckUpdateManager;
import com.pictureair.photopass.widget.CustomProgressDialog;
import com.pictureair.photopass.widget.EditTextWithClear;
import com.pictureair.photopass.widget.MyToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.gui.RegisterPage;
import cz.msebera.android.httpclient.Header;

/**
 * 登录页面 点击登录按钮之后，需要触发几个接口 1.登录接口 2.登录成功之后，需要获取一些信息，会调用获取购物车数量，获取storeId，获取PP列表
 * 3.全部获取之后，需要确认之前有扫描过pp或者ppp，如果有，则自动绑定
 * */

public class LoginActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "LoginActivity";
	// 申明控件
	private RelativeLayout parentRelativeLayout;
	private TextView tv_country, tv_country_num;// 国家，区号
	private TextView otherLogin;// 其他方式登录
	private Button login, sign;
	private TextView forgot;
	private EditTextWithClear userName, password;
	private CustomProgressDialog dialog;
	private LinearLayout rl_country;// 国家
	// 返回按键 反馈
	long i = 0;
	// 申明变量
	private StringBuffer loginUrl = new StringBuffer();// 登录的url
	private String storeIdString = "";// 商店ID字符串
	private String currency = "";// 货币
	private static final int START_OTHER_REGISTER_ACTIVITY = 1;// 启动 其他注册的侧面
	private static final int START_NATIONAL_LIST_SELECTION_ACTIVITY = 2; // 启动国家列表窗口
	private static final int GET_IP_SUCCESS = 3;
	private static final int GET_IP_FAILED = 4;
	private static final int GET_TOKENID_FAILED = 5;
	// 申明其他类
	private SharedPreferences sp, appPreferences;
	private Editor editor;
	private MyToast myToast;
	@SuppressWarnings("unused")
	private MyApplication myApplication;
	// 区号,国家
	private String countryCode = "+86";
	private String country = "";
	private RegisterPage registerPage;
	private CheckUpdateManager checkUpdateManager;// 自动检查更新

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case GET_TOKENID_FAILED:
				if (dialog.isShowing()) {
					dialog.dismiss();
				}
				myToast.setTextAndShow(R.string.http_failed,
						Common.TOAST_SHORT_TIME);
				break;

			case API.SUCCESS:// 登录成功，获取购物车的数量
				API.getcartcount(LoginActivity.this,
						sp.getString(Common.USERINFO_ID, ""), handler);
				break;

			case API.FAILURE:
				String result = msg.obj.toString();
				dialog.dismiss();
				myToast.setTextAndShow(result, Common.TOAST_SHORT_TIME);
				if ("tokenExpired".equals(result)) {
					PictureAirLog.v(TAG,"tokenExpired");
					editor = sp.edit();
					editor.putString(Common.USERINFO_TOKENID, null);
					editor.commit();
				}
				break;

			// case API.GET_PPP_FAILED:

			case API.GET_CART_COUNT_SUCCESS:// 成功获取购物车数量
				Log.d(TAG, "get cart count success---------------");
				API.getPPSByUserId(sp.getString(Common.USERINFO_TOKENID, null),
						handler);// 获取pp列表
				break;

			case API.GET_CART_COUNT_FAILED:// 获取购物车数量失败
				dialog.dismiss();
				myToast.setTextAndShow(R.string.failed, Common.TOAST_SHORT_TIME);
				break;
			case API.GET_STOREID_FAILED:
				dialog.dismiss();
				myToast.setTextAndShow(R.string.failed, Common.TOAST_SHORT_TIME);
				break;

			case GET_IP_SUCCESS:
				API.getStoreIdbyIP(msg.obj.toString(), handler);
				break;

			// case API.GET_PPP_SUCCESS:
			// PictureAirLog.v(TAG,"get ppp success ----------------");
			// JSONObject ppplistJsonObject = (JSONObject) msg.obj;
			// try {
			// JSONArray ppplistArray = ppplistJsonObject
			// .getJSONArray("PPPList");
			// if (0 != ppplistArray.length()) {// 说明有ppp
			// PictureAirLog.v(TAG,"length=" + ppplistArray.length());
			// Editor editor = sp.edit();
			// editor.putInt(Common.PPP_COUNT, ppplistArray.length());
			// editor.commit();
			// }
			// } catch (JSONException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// PictureAirLog.v(TAG,"netIP-----------> "+ AppUtil.GetNetIp());
			// API.getStoreIdbyIP("140.206.125.195", handler);
			// break;

			case API.GET_PPS_SUCCESS:// 获取pp列表成功
				/**
				 * 获取pp成功之后，需要放入sharedPrefence中
				 */
				JSONObject ppsJsonObject = (JSONObject) msg.obj;
				Log.d(TAG, "pps===" + ppsJsonObject);
				if (ppsJsonObject.has("PPList")) {
					try {
						JSONArray pplists = ppsJsonObject
								.getJSONArray("PPList");
						editor = sp.edit();
						editor.putInt(Common.PP_COUNT, pplists.length());
						editor.commit();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					Log.d(TAG, "pp size == 0");
				}

				new Thread() {
					public void run() {
						String netIP = "211.95.27.34";
						// String netIP = AppUtil.GetNetIp();
						PictureAirLog.v(TAG,"netIP-----------> " + netIP);
						if (netIP.equals("")) {// 获取失败
							handler.sendEmptyMessage(GET_IP_FAILED);
						} else {// 获取成功
							Message message = handler.obtainMessage();
							message.what = GET_IP_SUCCESS;
							message.obj = netIP;
							handler.sendMessage(message);
						}

					};
				}.start();
				break;

			case GET_IP_FAILED:
			case API.GET_PPS_FAILED:// 获取pp列表失败
				dialog.dismiss();
				myToast.setTextAndShow(R.string.failed, Common.TOAST_SHORT_TIME);
				break;

			case API.GET_STOREID_SUCCESS:// 获取storeId成功
				Log.d(TAG, "get storeid success--------------------");
				JSONObject obj = (JSONObject) msg.obj;
				try {
					storeIdString = obj.getString("storeId").toString();
					currency = obj.getString("currency").toString();// 获取货币
					Log.d(TAG, "storeid:" + storeIdString + ";currency:"
							+ currency);
					Editor editor = sp.edit();
					editor.putString(Common.CURRENCY, currency);
					editor.putString(Common.STORE_ID, storeIdString);
					editor.commit();
				} catch (JSONException e) {
					e.printStackTrace();
				}

				loginsuccess();
				break;

			case API.MODIFY_PWD_FAILED:
				PictureAirLog.v(TAG,"signorfotget------modify pwd failed");
				// 提示错误
				break;

			case API.MODIFY_PWD_SUCCESS:
				PictureAirLog.v(TAG,"signorforget------modify pwd success");
				// 跳转至登录界面
				break;

			case START_OTHER_REGISTER_ACTIVITY:

				// 其他注册的按钮//
				PictureAirLog.v(TAG,"other way on click----------");
				startActivity(new Intent(LoginActivity.this,
						OtherRegisterActivity.class));

				break;

			case START_NATIONAL_LIST_SELECTION_ACTIVITY:
				// 传递2，说明是从注册页面跳转到国家列表界面，传递3为 此activity跳转到国家列表界面
				Intent intent2 = new Intent();
				intent2.setClass(LoginActivity.this,
						NationalListSelectionActivity.class);
				intent2.putExtra("key", 2);

				startActivity(intent2);

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
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			View v = getCurrentFocus();
			if (AppUtil.isShouldHideInput(v, ev)) {
				// if (!password.hasFocus() && !userName.hasFocus()) {
				hideInputMethodManager(v);
				// }
			}
			return super.dispatchTouchEvent(ev);
		}
		// 必不可少，否则所有的组件都不会有TouchEvent了
		if (getWindow().superDispatchTouchEvent(ev)) {
			return true;
		}
		return onTouchEvent(ev);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		AppManager.getInstance().addActivity(this);// 添加到activity管理器
		initview();// 初始化

	}

	/**
	 * 登录成功之后的跳转
	 */
	private void loginsuccess() {
		PictureAirLog.v(TAG,"loginsuccess----------------");
		dialog.dismiss();
		// 发送广播
		Log.d(TAG, "start push service");
		Intent intent = new Intent();
		// intent.setAction("com.receiver.AlertManagerRecriver");
		// sendBroadcast(intent);
		// intent = new Intent();
		// 判断是否需要到主界面还是到商品界面
		if (null == getIntent().getStringExtra("activity")) {// 没有标记表示进入主界面
			intent.setClass(LoginActivity.this, MainTabActivity.class);
			startActivity(intent);
		}
		finish();
	}

	private void initview() {
		loginUrl.append(Common.BASE_URL).append(Common.LOGIN);// 链接地址
		sp = getSharedPreferences(Common.USERINFO_NAME, MODE_PRIVATE);// userInfo
		appPreferences = getSharedPreferences(Common.APP, MODE_PRIVATE);// userInfo

		myToast = new MyToast(LoginActivity.this);// 获取toast
		parentRelativeLayout = (RelativeLayout) findViewById(R.id.login_parent);
		login = (Button) findViewById(R.id.login);// 登录按钮
		sign = (Button) findViewById(R.id.sign);// 注册按钮
		userName = (EditTextWithClear) findViewById(R.id.login_username);// 文本框
		password = (EditTextWithClear) findViewById(R.id.login_password);// 密码框
		forgot = (TextView) findViewById(R.id.forgot);// 忘记密码？
		otherLogin = (TextView) findViewById(R.id.otherLogin);// 其他方式登录
		rl_country = (LinearLayout) findViewById(R.id.rl_country);// 国家
		tv_country = (TextView) findViewById(R.id.tv_country);
		tv_country_num = (TextView) findViewById(R.id.tv_country_num);

		rl_country.setOnClickListener(this);
		login.setOnClickListener(this);
		sign.setOnClickListener(this);
		forgot.setOnClickListener(this);
		otherLogin.setOnClickListener(this);

		// 自动检查更新
		checkUpdateManager = new CheckUpdateManager(this,
				appPreferences.getString(Common.LANGUAGE_TYPE, ""),
				parentRelativeLayout);
		checkUpdateManager.startCheck();

		myApplication = (MyApplication) getApplication();
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
		initSSMSSDK();

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
		switch (v.getId()) {
		case R.id.rl_country:
			// NationalListSelectionActivity
			PictureAirLog.v(TAG,"国家按钮");
			startActivityForResult(new Intent(LoginActivity.this,
					NationalListSelectionActivity.class), 0);
			break;

		case R.id.login:
			hideInputMethodManager(v);
			if (userName.getText().toString().trim().isEmpty()) {
				myToast.setTextAndShow(R.string.username_null,
						Common.TOAST_SHORT_TIME);
				break;
			}
			String pwd = password.getText().toString();
			// 比较密码合法性
			switch (AppUtil.checkPwd(pwd, pwd)) {
			case AppUtil.PWD_ALL_SAPCE:// 全部为空格
				myToast.setTextAndShow(R.string.pwd_no_all_space,
						Common.TOAST_SHORT_TIME);
				break;

			case AppUtil.PWD_AVAILABLE:// 密码可用
				login();// 登录
				break;

			case AppUtil.PWD_EMPTY:// 空
				myToast.setTextAndShow(R.string.pwd_is_empty,
						Common.TOAST_SHORT_TIME);
				break;

			case AppUtil.PWD_INCONSISTENCY:// 不一致
				// myToast.setTextAndShow(R.string.pw_is_inconsistency,
				// Common.TOAST_SHORT_TIME);
				break;

			case AppUtil.PWD_SHORT:// 小于6位
				myToast.setTextAndShow(R.string.notify_password_hint,
						Common.TOAST_SHORT_TIME);

				break;

			case AppUtil.PWD_HEAD_OR_FOOT_IS_SPACE:// 密码首尾不能为空格
				myToast.setTextAndShow(R.string.pwd_head_or_foot_space,
						Common.TOAST_SHORT_TIME);
				break;
			}
			break;

		case R.id.sign:
			PictureAirLog.v(TAG,"tap sign");
			sendSMS(0);
			break;

		case R.id.forgot:
			PictureAirLog.v(TAG,"tap forget password");
			sendSMS(1);
			break;

		case R.id.otherLogin:
			PictureAirLog.v(TAG,"tap other login 其他方式登录");
			Intent intent = new Intent(LoginActivity.this,
					OtherLoginActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}

	/**
	 * 登录
	 */
	public void login() {
		dialog = CustomProgressDialog.show(this,
				getString(R.string.is_loading), false, null);
		// 登录成功时可把一些后续需要使用到的信息保存起来，比如地点收藏状态，pp和pp+信息等，具体看后台返回的数据决定
		if (null == sp.getString(Common.USERINFO_TOKENID, null)) {
			PictureAirLog.v(TAG,"no tokenid");
			final StringBuffer sb = new StringBuffer();
			sb.append(Common.BASE_URL).append(Common.GET_TOKENID);
			RequestParams params = new RequestParams();
			params.put(Common.TERMINAL, "android");
			params.put(Common.UUID, Installation.id(this));
			HttpUtil.get(sb.toString(), params, new JsonHttpResponseHandler() {
				@Override
				public void onStart() {
					// TODO Auto-generated method stub
					super.onStart();
					PictureAirLog.v(TAG,"get tokenid start");
				}

				public void onSuccess(int statusCode, Header[] headers,
						JSONObject response) {
					super.onSuccess(statusCode, headers, response);
					try {
						PictureAirLog.v(TAG,"tokenid==" + response);
						Editor e = sp.edit();
						if (response.has(Common.USERINFO_TOKENID)) {
							PictureAirLog.v(TAG,"add tokenid=============");
							e.putString(Common.USERINFO_TOKENID,
									response.getString(Common.USERINFO_TOKENID));
						}
						e.commit();
						API.Login(LoginActivity.this, userName.getText()
								.toString().trim(), password.getText()
								.toString().trim(), handler);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

				@Override
				public void onFailure(int statusCode, Header[] headers,
						String responseString, Throwable throwable) {
					// TODO Auto-generated method stub
					super.onFailure(statusCode, headers, responseString,
							throwable);
					throwable.printStackTrace();
				}
			});
		} else {
			PictureAirLog.v(TAG,"has tokenid");
			API.Login(this, userName.getText().toString().trim(), password
					.getText().toString().trim(), handler);
		}

	}

	/** 初始化发短信 **/
	private void initSSMSSDK() {
		SMSSDK.initSDK(this, Common.APPKEY, Common.APPSECRET);
	}

	/**
	 * 第三方短信验证方法
	 * */
	private void sendSMS(final int type) {
		registerPage = new RegisterPage(type, handler);

		registerPage.setRegisterCallback(new EventHandler() {
			public void afterEvent(int event, int result, Object data) {
				PictureAirLog.v(TAG,"type ---- >" + type + ",result--->"
						+ result + "data-->" + data);
				// 解析注册结果
				if (result == SMSSDK.RESULT_COMPLETE) {
					@SuppressWarnings("unchecked")
					HashMap<String, Object> phoneMap = (HashMap<String, Object>) data;

					String pwd = phoneMap.get("pwd").toString();
					String phone = phoneMap.get("phone").toString();
					// 把手机号发送到服务器判断账号是否存在，存在则跳转到重置密码页面
					PictureAirLog.v(TAG,"type -------->" + type);
					if (type == 0) {
						/*
						 * 服务器返回手机号不存在，注册 将验证都再smssdk中区完成 然后回调上来参数 参数有：phone,pwd
						 * 拿到值。就直接注册。 注册成功：跳转到主页ok
						 */
						PictureAirLog.v(TAG,"phone:" + phone);
						PictureAirLog.v(TAG,"pwd:" + pwd);
						/*
						 * ＊＊＊＊交给SignAndLoginPhoneNumberService类来逻辑处理 注册 并 登录
						 */
						// sendSign(phone, pwd);
						new SignAndLoginUtil(LoginActivity.this, phone, pwd);

					} else if (type == 1) {
						// 服务器返回手机号存在，修改密码
						Intent intent = new Intent(LoginActivity.this,
								SignOrForgetActivity.class);
						intent.putExtra("phone", phone);
						intent.putExtra("type", type);
						startActivity(intent);
					}
				}
			}
		});
		registerPage.show(this);
	}

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - i > 2000) {
			myToast.setTextAndShow(R.string.exit, Common.TOAST_SHORT_TIME);
			i = System.currentTimeMillis();
		} else {
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == 111) {
			countryCode = data.getStringExtra("countryCode");
			country = data.getStringExtra("country");
			tv_country.setText(country);
			tv_country_num.setText("+" + countryCode);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		AppManager.getInstance().killActivity(this);
		if (registerPage != null) {
			PictureAirLog.v(TAG,"logout onDestroy, need finish registerPage");
			registerPage.finish();
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
}
