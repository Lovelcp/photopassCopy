package com.pictureAir.widget;

import static com.mob.tools.utils.R.getStringRes;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import cn.sharesdk.facebook.Facebook;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.tencent.qzone.QZone;
import cn.sharesdk.twitter.Twitter;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;
import cn.sharesdk.wechat.moments.WechatMoments.ShareParams;

import com.mob.tools.utils.UIHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.pictureAir.R;
import com.pictureAir.util.Common;
import com.pictureAir.util.PictureAirLog;
import com.pictureAir.util.ScreenUtil;
import com.pictureAir.util.UmengUtil;
import com.umeng.analytics.MobclickAgent;

/**
 * 此控件负责photo页面中menu下拉菜单的内容
 */
public class SharePop extends PopupWindow implements OnClickListener,
		PlatformActionListener, Callback {
	// private static final int MSG_TOAST = 1;
	private static final int MSG_ACTION_CCALLBACK = 2;
	private static final int MSG_CANCEL_NOTIFY = 3;
	public static final int TWITTER = 40;
	private Context context;
	private LayoutInflater inflater;
	private View defaultView;
	private TextView wechat, wechatMoments, qq, qqzone, sina, facebook,
			twitter;
	private TextView sharecancel;
	// private PlatformActionListener callback;
	private String imagePath, imageUrl, shareUrl, type;
	// private MyToast myToast;
	private String sharePlatform;
	private Handler handler;
	/**
	 * 打开程序进行分享比较耗时间，所以再点击分享的时候，就显示进度条
	 */
	private CustomProgressDialog dialog;
	/**
	 * 控制dialog的显示或者消失
	 */
	private boolean isOpenning = false;
	private String shareType; // 分享类型，判断是 什么分享平台。 微信：1，qqzone：2，sina：3，twitter
								// ：4

	public SharePop(Context context) {
		super(context);
		this.context = context;
		initPopupWindow();
	}

	public void initPopupWindow() {
		ShareSDK.initSDK(context);
		// myToast = new MyToast(context);
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		defaultView = inflater.inflate(R.layout.share_dialog, null);
		defaultView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));
		setContentView(defaultView);
		setWidth(LayoutParams.MATCH_PARENT);
		setHeight(LayoutParams.WRAP_CONTENT);
		setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
		setAnimationStyle(R.style.from_bottom_anim);
		setFocusable(true);
		setOutsideTouchable(true);

		wechat = (TextView) defaultView.findViewById(R.id.wechat);
		wechatMoments = (TextView) defaultView
				.findViewById(R.id.wechat_moments);
		qq = (TextView) defaultView.findViewById(R.id.qq);
		qqzone = (TextView) defaultView.findViewById(R.id.qqzone);
		sina = (TextView) defaultView.findViewById(R.id.sina);
		facebook = (TextView) defaultView.findViewById(R.id.facebook);
		twitter = (TextView) defaultView.findViewById(R.id.twitter);
		sharecancel = (TextView) defaultView.findViewById(R.id.share_cancel);

		wechat.setOnClickListener(this);
		wechatMoments.setOnClickListener(this);
		qq.setOnClickListener(this);
		qqzone.setOnClickListener(this);
		sina.setOnClickListener(this);
		twitter.setOnClickListener(this);
		facebook.setOnClickListener(this);
		sharecancel.setOnClickListener(this);

	}

	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		super.dismiss();
	}

	/**
	 * 设置需要分享的信息
	 * 
	 * @param imagePath
	 *            本地图片路径
	 * @param imageUrl
	 *            网络图片url
	 * @param type
	 *            判断是否是本地还是网络，类型有“local”“online”
	 */
	public void setshareinfo(String imagePath, String imageUrl,
			String shareUrl, String type, Handler handler) {
		this.imagePath = imagePath;
		this.imageUrl = imageUrl;
		this.shareUrl = shareUrl;
		this.type = type;
		this.handler = handler;
		// System.out.println(this.imagePath+"+"+this.imageUrl+"_"+this.type);
	}

	/**
	 * 微信分享，不支持图文分享，只能分享图片，或者图片以链接的形式分享出去，但是都不能添加文字
	 * 
	 * @param context
	 * @param imagePath
	 *            本地图片路径
	 * @param imageUrl
	 *            网络图片url
	 * @param type
	 *            判断是否是本地还是网络，类型有“local”“online”
	 */
	public void wechatmonentsShare(Context context, String imagePath,
			String imageUrl, String shareUrl, String type) {
		// System.out.println("sharing info "+ imagePath+"_"+imageUrl+"_"+type);

		Platform platform = ShareSDK.getPlatform(context, WechatMoments.NAME);
		platform.setPlatformActionListener(this);// 如果没有通过审核，这个监听没有什么作用
		// platform.isValid()
		ShareParams shareParams = new ShareParams();
		shareParams.title = context.getString(R.string.share_text);
		// shareParams.text = "PhotoPass";
		// 本地图片可以
		if ("local".equals(type)) {// 本地图片
			shareParams.shareType = Platform.SHARE_IMAGE;// 只分享图片，这个时候不需要url属性。
			shareParams.imagePath = imagePath;
		} else if ("online".equals(type)) {// 网络图片
			shareParams.shareType = Platform.SHARE_WEBPAGE;// 以网页的形式分享图片
			shareParams.imageUrl = imageUrl;
			shareParams.url = shareUrl;// share_webpage的时候需要这个参数
		}
		platform.share(shareParams);
	}

	/**
	 * 微信好友分享，不支持图文分享，只能分享图片，或者图片以链接的形式分享出去，但是都不能添加文字
	 * 
	 * @param context
	 * @param imagePath
	 *            本地图片路径
	 * @param imageUrl
	 *            网络图片url
	 * @param type
	 *            判断是否是本地还是网络，类型有“local”“online”
	 */
	private void wechatFriendsShare(Context context, String imagePath,
			String imageUrl, String shareUrl, String type) {
		// TODO Auto-generated method stub
		Platform platform = ShareSDK.getPlatform(context, Wechat.NAME);
		platform.setPlatformActionListener(this);// 如果没有通过审核，这个监听没有什么作用
		// platform.isValid()
		ShareParams shareParams = new ShareParams();
		shareParams.title = context.getString(R.string.share_text);
		// shareParams.text = "PhotoPass";
		// 本地图片可以
		if ("local".equals(type)) {// 本地图片
			shareParams.shareType = Platform.SHARE_IMAGE;// 只分享图片，这个时候不需要url属性。
			shareParams.imagePath = imagePath;
		} else if ("online".equals(type)) {// 网络图片
			shareParams.shareType = Platform.SHARE_WEBPAGE;// 以网页的形式分享图片
			// shareParams.imageUrl = imageUrl;
			shareParams.url = shareUrl;// share_webpage的时候需要这个参数
		}
		platform.share(shareParams);
	}

	/**
	 * QZone分享，是根据qq平台去分享到空间
	 * 
	 * @param context
	 * @param imagePath
	 *            本地图片路径
	 * @param imageUrl
	 *            网络图片url
	 * @param type
	 *            判断是否是本地还是网络，类型有“local”“online”
	 */
	public void qzoneShare(Context context, String imagePath, String imageUrl,
			String shareUrl, String type) {
		// System.out.println("sharing info "+ imagePath+"_"+imageUrl+"_"+type);
		Platform platform = ShareSDK.getPlatform(context, QZone.NAME);
		if (platform.isClientValid()) {
			platform.setPlatformActionListener(this);// 如果没有通过审核，这个监听没有什么作用
			cn.sharesdk.tencent.qzone.QZone.ShareParams shareParams = new cn.sharesdk.tencent.qzone.QZone.ShareParams();
			shareParams.title = "pictureAir";
			shareParams.text = context.getResources().getString(
					R.string.share_text);
			if ("local".equals(type)) {// 本地图片
				shareParams.imagePath = imagePath;
				shareParams.titleUrl = "http://www.pictureair.com";
				// shareParams.siteUrl = "http://www.pictureair.com";
			} else if ("online".equals(type)) {// 网络图片
				shareParams.imageUrl = imageUrl;
				shareParams.titleUrl = shareUrl;
				shareParams.siteUrl = shareUrl;
			}
			shareParams.site = context.getString(R.string.app_name);
			platform.share(shareParams);
		} else {
			if (dialog.isShowing()) {
				dialog.dismiss();
				isOpenning = false;
			}
			showNotification(2000,
					context.getString(R.string.share_failure_qzone));
		}
	}

	/**
	 * qq好友分享
	 * 
	 * @param context
	 * @param imagePath
	 * @param imageUrl
	 * @param shareUrl
	 * @param type
	 */
	private void qqShare(Context context, String imagePath, String imageUrl,
			String shareUrl, String type) {
		// TODO Auto-generated method stub
		Platform platform = ShareSDK.getPlatform(context, QQ.NAME);
		if (platform.isClientValid()) {
			platform.setPlatformActionListener(this);// 如果没有通过审核，这个监听没有什么作用
			cn.sharesdk.tencent.qzone.QZone.ShareParams shareParams = new cn.sharesdk.tencent.qzone.QZone.ShareParams();
			shareParams.title = "pictureAir";
			shareParams.text = context.getResources().getString(
					R.string.share_text);
			if ("local".equals(type)) {// 本地图片
				shareParams.imagePath = imagePath;
				shareParams.titleUrl = "http://www.pictureair.com";
				// shareParams.siteUrl = "http://www.pictureair.com";
			} else if ("online".equals(type)) {// 网络图片
				shareParams.imageUrl = imageUrl;
				shareParams.titleUrl = shareUrl;
				// shareParams.siteUrl = shareUrl;
			}
			// shareParams.site = context.getString(R.string.app_name);
			platform.share(shareParams);
		} else {
			if (dialog.isShowing()) {
				dialog.dismiss();
				isOpenning = false;
			}
			showNotification(2000,
					context.getString(R.string.share_failure_qzone));
		}
	}

	/**
	 * sina分享
	 * 
	 * @param context
	 * @param imagePath
	 *            本地图片路径
	 * @param imageUrl
	 *            网络图片URL
	 * @param type
	 *            本地还是网络的标记
	 * @param commend
	 *            文本评论
	 */
	public void sinaShare(Context context, String imagePath, String imageUrl,
			String shareUrl, String type) {
		// System.out.println("sharing info "+ imagePath+"_"+imageUrl+"_"+type);
		Platform platform = ShareSDK.getPlatform(context, SinaWeibo.NAME);
		// if (platform.isClientValid()) {

		platform.SSOSetting(false);// 未审核，必须要关闭SSO，true代表关闭。审核过了之后才要设置为false
		// platform.authorize();
		platform.setPlatformActionListener(this);// 如果没有通过审核，这个监听没有什么作用
		cn.sharesdk.sina.weibo.SinaWeibo.ShareParams shareParams = new cn.sharesdk.sina.weibo.SinaWeibo.ShareParams();
		shareParams.text = context.getString(R.string.share_text);
		shareParams.setTitleUrl(shareUrl);
		if ("local".equals(type)) {// 本地图片
			shareParams.imagePath = imagePath;
		} else if ("online".equals(type)) {// 网络图片，未审核的不支持网络图片，所以只能把链接分享出来
			shareParams.imageUrl = imageUrl;
			// shareParams.text = imageUrl;

		}
		platform.share(shareParams);
		// } else {
		// if (dialog.isShowing()) {
		// dialog.dismiss();
		// isOpenning = false;
		// }
		// showNotification(2000,
		// context.getString(R.string.share_failure_weibo));
		// }
	}

	/**
	 * facebook分享
	 * 
	 * @param context
	 * @param imagePath
	 *            本地图片路径
	 * @param imageUrl
	 *            网络图片URL
	 * @param type
	 *            本地还是网络的标记
	 * @param commend
	 *            文本评论
	 */
	public void facebookShare(Context context, String imagePath,
			String imageUrl, String shareUrl, String type) {
		// System.out.println("sharing info "+ imagePath+"_"+imageUrl+"_"+type);
		Platform platform = ShareSDK.getPlatform(context, Facebook.NAME);
		if (platform.isClientValid()) {

			platform.setPlatformActionListener(this);// 如果没有通过审核，这个监听没有什么作用
			cn.sharesdk.facebook.Facebook.ShareParams shareParams = new cn.sharesdk.facebook.Facebook.ShareParams();
			shareParams.text = "pictureAir";
			if ("local".equals(type)) {// 本地图片
				shareParams.setImagePath(imagePath);
			} else if ("online".equals(type)) {// 网络图片，未审核的不支持网络图片，所以只能把链接分享出来
				shareParams.setImageUrl(imageUrl);
			}
			platform.share(shareParams);
		} else {
			if (dialog.isShowing()) {
				dialog.dismiss();
				isOpenning = false;
			}
			showNotification(2000,
					context.getString(R.string.share_failure_facebook));
		}
	}

	/**
	 * twitter分享，只能以网页的形式进行分享，同时，本地图片不支持过大的文件
	 * 
	 * @param context
	 * @param imagePath
	 * @param imageUrl
	 * @param type
	 */
	private void twitterShare(Context context, String imagePath,
			String imageUrl, String shareUrl, String type) {
		Platform platform = ShareSDK.getPlatform(context, Twitter.NAME);
		// if (platform.isClientValid()) {
		// System.out.println("imagePath is " + imagePath + ",imageurl is " +
		// shareUrl);
		platform.setPlatformActionListener(this);
		cn.sharesdk.twitter.Twitter.ShareParams shareParams = new cn.sharesdk.twitter.Twitter.ShareParams();
		if ("local".equals(type)) {
			shareParams.text = context.getString(R.string.share_text);
			shareParams.setImagePath(imagePath);
		} else if ("online".equals(type)) {
			shareParams.text = shareUrl;
		}
		platform.share(shareParams);
		// } else {
		// if (dialog.isShowing()) {
		// dialog.dismiss();
		// isOpenning = false;
		// }
		// showNotification(2000,
		// context.getString(R.string.share_failure_twitter));
		// }
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		// Message msg = mHandler.obtainMessage();
		// 显示进度条，等待app打开
		dialog = CustomProgressDialog.show(context, null, false, null);
		// dialog = CustomProgressDialog.show(context,
		// context.getString(R.string.launching_app), false, null);
		isOpenning = true;
		switch (v.getId()) {
		case R.id.wechat_moments:
			System.out.println("wechat share");
			shareType = Common.EVENT_ONCLICK_SHARE_WECHAT_MOMENTS;
			UmengUtil.onEvent(context, shareType);
			wechatmonentsShare(context, imagePath, imageUrl, shareUrl, type);
			// mHandler.sendEmptyMessage(WECHAT);
			break;

		case R.id.wechat:
			// System.out.println("wechat share");
			shareType = Common.EVENT_ONCLICK_SHARE_WECHAT;
			UmengUtil.onEvent(context, shareType);
			wechatFriendsShare(context, imagePath, imageUrl, shareUrl, type);
			// mHandler.sendEmptyMessage(WECHAT);
			break;

		case R.id.share_cancel:
			isOpenning = false;
			dialog.dismiss();
			break;

		case R.id.qq:
			shareType = Common.EVENT_ONCLICK_SHARE_QQ;
			UmengUtil.onEvent(context, shareType);
			if (type.equals("local")) {// 本地
				createThumbNail(v.getId());
			} else {
				qqShare(context, imagePath, imageUrl, shareUrl, type);
			}
			// mHandler.sendEmptyMessage(QZONE);
			break;

		case R.id.qqzone:
			shareType = Common.EVENT_ONCLICK_SHARE_QQZONE;
			UmengUtil.onEvent(context, shareType);
			qzoneShare(context, imagePath, imageUrl, shareUrl, type);
			// mHandler.sendEmptyMessage(QZONE);
			break;

		case R.id.sina:
			shareType = Common.EVENT_ONCLICK_SHARE_SINA_WEIBO;
			UmengUtil.onEvent(context, shareType);
			if (type.equals("local")) {// 本地
				createThumbNail(v.getId());
			} else {
				sinaShare(context, imagePath, imageUrl, shareUrl, type);
			}
			// mHandler.sendEmptyMessage(SINA);
			// newToast.setTextAndShow("This function doesn't open",
			// Common.TOAST_SHORT_TIME);
			break;

		case R.id.facebook:
			PictureAirLog.out("fb on click");
			shareType = Common.EVENT_ONCLICK_SHARE_FACEBOOK;
			UmengUtil.onEvent(context, shareType);
			facebookShare(context, imagePath, imageUrl, shareUrl, type);
			// mHandler.sendEmptyMessage(FACEBOOK);
			break;

		case R.id.twitter:
			shareType = Common.EVENT_ONCLICK_SHARE_TWITTER;
			UmengUtil.onEvent(context, shareType);
			sharePlatform = "twitter";
			handler.sendEmptyMessage(TWITTER);
			if (type.equals("local")) {// 本地
				createThumbNail(v.getId());
			} else {
				twitterShare(context, imagePath, imageUrl, shareUrl, type);
			}

			break;

		default:
			break;
		}

		if (isShowing()) {
			dismiss();
			ShareSDK.stopSDK();

		}

	}

	private void createThumbNail(final int id) {
		// TODO Auto-generated method stub
		new Thread() {
			public void run() {
				ImageLoader.getInstance().loadImage("file:///" + imagePath,
						new ImageLoadingListener() {

							@Override
							public void onLoadingStarted(String imageUri,
									View view) {
								// TODO Auto-generated method stub

							}

							@Override
							public void onLoadingFailed(String imageUri,
									View view, FailReason failReason) {
								// TODO Auto-generated method stub

							}

							@Override
							public void onLoadingComplete(String imageUri,
									View view, Bitmap loadedImage) {
								// TODO Auto-generated method stub
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								loadedImage.compress(
										Bitmap.CompressFormat.JPEG, 30, baos);
								byte[] datas = baos.toByteArray();
								File shareFile = new File(Common.SHARE_PATH);
								if (!shareFile.exists()) {
									shareFile.mkdirs();
								}
								shareFile = new File(Common.SHARE_PATH
										+ ScreenUtil
												.getReallyFileName(imagePath)
										+ ".jpg");
								if (shareFile.exists()) {

								} else {

									BufferedOutputStream stream = null;
									try {
										shareFile.createNewFile();
										FileOutputStream fStream = new FileOutputStream(
												shareFile);
										stream = new BufferedOutputStream(
												fStream);
										stream.write(datas);
									} catch (Exception e) {
										// TODO: handle exception
									} finally {
										if (stream != null) {
											try {
												stream.close();
											} catch (Exception e2) {
												// TODO: handle exception
											}
										}
									}
								}
								switch (id) {
								case R.id.twitter:
									// 生成缩略图成功， 需要开始分享
									twitterShare(context, shareFile.toString(),
											imageUrl, shareUrl, type);
									break;

								case R.id.sina:
									sinaShare(context, imagePath, imageUrl,
											shareUrl, type);
									break;

								case R.id.qq:
									qqShare(context, imagePath, imageUrl,
											shareUrl, type);
									break;

								default:
									break;
								}
							}

							@Override
							public void onLoadingCancelled(String imageUri,
									View view) {
								// TODO Auto-generated method stub

							}
						});
			};
		}.start();
	}

	/**
	 * 将开始程序的对话框消失掉
	 */
	public void dismissDialog() {
		// TODO Auto-generated method stub
		if (dialog != null && dialog.isShowing() && isOpenning) {
			PictureAirLog.out("share pop dismiss");
			// System.out.println("sharePop----dismiss");
			dialog.dismiss();
			isOpenning = false;
		} else {
			// System.out.println("sharePop--not--dismiss");

		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {

		// case MSG_TOAST: {
		// String text = String.valueOf(msg.obj);
		// // Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
		// myToast.setTextAndShow(text, Common.TOAST_SHORT_TIME);
		// }
		// break;
		case MSG_ACTION_CCALLBACK: {
			switch (msg.arg1) {
			case 1: {
				// System.out.println("share 1");
				// 成功
				int resId = getStringRes(context, "share_completed");
				if (resId > 0) {
					showNotification(2000, context.getString(resId));
				}
			}
				break;
			case 2: {
				// 失败
				// System.out.println("share 2");
				String expName = msg.obj.getClass().getSimpleName();
				if ("WechatClientNotExistException".equals(expName)
						|| "WechatTimelineNotSupportedException"
								.equals(expName)
						|| "WechatFavoriteNotSupportedException"
								.equals(expName)) {
					int resId = getStringRes(context, "share_failure_wechat");
					if (resId > 0) {
						showNotification(2000, context.getString(resId));
					}
				} else if ("QQClientNotExistException".equals(expName)) {
					int resId = getStringRes(context, "share_failure_qzone");
					if (resId > 0) {
						showNotification(2000, context.getString(resId));
					}
				} else {
					int resId = getStringRes(context, "share_failed");
					if (resId > 0) {
						showNotification(2000, context.getString(resId));
					}
				}
			}
				break;

			case 3: {
				// 取消
				// System.out.println("share 3");
				// PictureAirLog.out("3333");
				int resId = getStringRes(context, "share_canceled");
				if (resId > 0) {
					// PictureAirLog.out("3333"+context.getString(resId));
					showNotification(2000, context.getString(resId));
				}
			}
				break;
			}
		}
			break;
		case MSG_CANCEL_NOTIFY: {
			// System.out.println("share 4");
			NotificationManager nm = (NotificationManager) msg.obj;
			if (nm != null) {
				nm.cancel(msg.arg1);
			}
		}
			break;
		}
		return false;
	}

	@Override
	public void onCancel(Platform arg0, int arg1) {
		// TODO Auto-generated method stub
		// System.out.println("cancel");
		if (dialog.isShowing()) {
			dialog.dismiss();
			isOpenning = false;
		}
		Message msg = new Message();
		msg.what = MSG_ACTION_CCALLBACK;
		msg.arg1 = 3;
		msg.arg2 = arg1;
		msg.obj = arg0;
		UIHandler.sendMessage(msg, this);
	}

	@Override
	public void onComplete(Platform arg0, int arg1, HashMap<String, Object> arg2) {
		// TODO Auto-generated method stub
		// System.out.println("complete");
		if (dialog.isShowing()) {
			dialog.dismiss();
			isOpenning = false;
		}
		Message msg = new Message();
		msg.what = MSG_ACTION_CCALLBACK;
		msg.arg1 = 1;
		msg.arg2 = arg1;
		msg.obj = arg0;

		UIHandler.sendMessage(msg, this);
		//统计分享成功
		onEventUmeng(arg0);
		if (sharePlatform.equals("twitter")) {
			arg0.removeAccount(true);
		}
	}

	@Override
	public void onError(Platform arg0, int arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		// System.out.println("error");
		arg2.printStackTrace();
		if (dialog.isShowing()) {
			dialog.dismiss();
			isOpenning = false;
		}
		Message msg = new Message();
		msg.what = MSG_ACTION_CCALLBACK;
		msg.arg1 = 2;
		msg.arg2 = arg1;
		msg.obj = arg2;
		UIHandler.sendMessage(msg, this);

		// 分享失败的统计
		ShareSDK.logDemoEvent(4, arg0);
	}

	/**
	 * 分享完成后统计分享数据
	 * 
	 * @param platform
	 */
	public void onEventUmeng(Platform platform) {
		if (platform == null) {
			return;
		}
		String typeName = platform.getName();
		String eventName = null;
		if (typeName.equals("SinaWeibo")) {
			eventName = Common.EVENT_SHARE_SINA_WEIBO_FINISH;
		}  else if (typeName.equals("QZone")) {
			eventName = Common.EVENT_SHARE_QQZONE_FINISH;

		}
		else if (typeName.equals("QZone")) {
			eventName = Common.EVENT_SHARE_QQZONE_FINISH;

		} else if (typeName.equals("Wechat")) {
			eventName = Common.EVENT_SHARE_WECHAT_FINISH;

		} else if (typeName.equals("WechatMoments")) {
			eventName = Common.EVENT_SHARE_WECHAT_MOMENTS_FINISH;

		} else if (typeName.equals("Facebook")) {
			eventName = Common.EVENT_SHARE_FACEBOOK_FINISH;

		} else if (typeName.equals("Twitter")) {
			eventName = Common.EVENT_SHARE_TWITTER_FINISH;
		}

		// 分享完成统计事件
		if (!TextUtils.isEmpty(eventName)) {
			UmengUtil.onEvent(context, eventName);
		}

	}

	// 在状态栏提示分享操作
	private void showNotification(long cancelTime, String text) {
		try {
			Context app = context.getApplicationContext();
			NotificationManager nm = (NotificationManager) app
					.getSystemService(Context.NOTIFICATION_SERVICE);
			final int id = Integer.MAX_VALUE / 13 + 1;
			nm.cancel(id);

			long when = System.currentTimeMillis();
			Notification notification = new Notification(R.drawable.pp_icon,
					text, when);
			PendingIntent pi = PendingIntent.getActivity(app, 0, new Intent(),
					0);
			notification.setLatestEventInfo(app,
					context.getString(R.string.app_name), text, pi);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			nm.notify(id, notification);

			if (cancelTime > 0) {
				Message msg = new Message();
				msg.what = MSG_CANCEL_NOTIFY;
				msg.obj = nm;
				msg.arg1 = id;
				UIHandler.sendMessageDelayed(msg, cancelTime, this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
