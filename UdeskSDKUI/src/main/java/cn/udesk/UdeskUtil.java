package cn.udesk;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import udesk.core.UdeskCoreConst;
import udesk.core.model.MessageInfo;

public class UdeskUtil {
	public static final String  ImgFolderName = "UDeskIMg";
	public static final String  AudioFolderName = "UDeskAudio";
	public static final String  SaveImg = "saveImg";

	public static Uri getOutputMediaFileUri(Context context) {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		if (Build.VERSION.SDK_INT>=24){
			return FileProvider.getUriForFile(context,getFileProviderName(context),getOutputMediaFile(context,"IMG_" + timeStamp + ".jpg"));
		}else{
			return Uri.fromFile(getOutputMediaFile(context,"IMG_" + timeStamp + ".jpg"));
		}

	}

	public static String getFileProviderName(Context context){
		return context.getPackageName()+".udeskfileprovider";
	}

	/**
	 * 提供的Uri 解析出文件绝对路径
	 * @param uri
	 * @return
	 */
	public static String parseOwnUri(Uri uri,Context context){
		if(uri==null)return "";
		String path;
		if(TextUtils.equals(uri.getAuthority(),getFileProviderName(context))){
			path=new File(Environment.getExternalStorageDirectory(),uri.getPath().replace("my_external/","")).getAbsolutePath();
		}else {
			path=uri.getPath();
		}
		return path;
	}

	public static File getOutputMediaFile(Context context,String mediaName) {
		File mediaStorageDir = null;
		try {
//            mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ImgFolderName);
			mediaStorageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), ImgFolderName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}

		return new File(mediaStorageDir.getPath() + File.separator + mediaName);
	}

	public static boolean isExitFile(String path) {
		File file = new File(path);
		return file.exists();
	}


	public static boolean audiofileIsDown(Context context ,String url) {
		if (TextUtils.isEmpty(url)) {
			return false;
		}
		String fileName = url.substring(url.lastIndexOf("/") + 1);
		File mediaStorageDir = new File(
				context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES),
				AudioFolderName);
		if (!mediaStorageDir.exists()) {
			return false;
		}
		String filepath = mediaStorageDir.getPath() + File.separator + fileName;
		File file = new File(filepath);
		return file.exists();
	}


	public static String getDownAudioPath(Context context ,String url) {
		String fileName = url.substring(url.lastIndexOf("/") + 1);
		File mediaStorageDir = new File(
				context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES),
				AudioFolderName);

		return mediaStorageDir.getPath() + File.separator + fileName;
	}


	public static String getOutputAudioPath(Context context) {
		return getOutputAudioPath(context,"audio_"
				+ new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
	}

	public static File getOutputAudioFile(Context context) {
		return getOutputAudioFile(context,"audio_"
				+ new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
	}

	public static File getOutputAudioFile(Context context ,String mediaName) {
		String path = getOutputAudioPath(context,mediaName);
		if (TextUtils.isEmpty(path)) {
			return null;
		} else {
			return new File(path);
		}
	}

	public static String getOutputAudioPath(Context context , String mediaName) {
		File mediaStorageDir = new File(
				context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES),
				AudioFolderName);

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}

		File noMediaFile = new File(mediaStorageDir, ".nomedia");
		if (!noMediaFile.exists()) {
			try {
				noMediaFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return mediaStorageDir.getPath() + File.separator + mediaName;
	}


	public static File getAudioFile(Context context ,String url) {
		String fileName = url.substring(url.lastIndexOf("/") + 1);
		String path = getOutputAudioPath(context,fileName);
		if (TextUtils.isEmpty(path)) {
			return null;
		} else {
			return new File(path);
		}
	}


	public static String getSaveImgPath(Context context ,String url) {
//		String fileName = url.substring(url.lastIndexOf("/") + 1);
		File mediaStorageDir = new File(
				context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES),
				SaveImg);

		return mediaStorageDir.getPath() ;
//				+ File.separator + fileName;
	}


	public static String buildImageLoaderImgUrl(MessageInfo message){

		if(!TextUtils.isEmpty(message.getLocalPath()) && isExitFile(message.getLocalPath())){
			return message.getLocalPath();
		}else{
			return message.getMsgContent();
		}
	}



	public static String getFormUrlPara(Context context){
		StringBuilder builder = new StringBuilder();
		builder.append("?sdk_token=").append(UdeskSDKManager.getInstance().getSdkToken(context))
				.append("&sdk_version=").append(UdeskCoreConst.sdkversion);
		if (!isZh(context)){
			builder.append("&language=en-us");
		}
		Map<String, String> userinfo = UdeskSDKManager.getInstance().getUserinfo();
		Map<String,String> textField = UdeskSDKManager.getInstance().getTextField();
		if(userinfo != null && !userinfo.isEmpty()){
			Set<String> keySet = userinfo.keySet();
			for (String key : keySet) {
				if(!TextUtils.isEmpty(userinfo.get(key))){
					if(key.equals("sdk_token")){
						continue;
					}
					if(key.equals(UdeskConst.UdeskUserInfo.NICK_NAME)){
						builder.append("&c_name=").append(userinfo.get(key));
					}else if(key.equals(UdeskConst.UdeskUserInfo.CELLPHONE)){
						builder.append("&c_phone=").append(userinfo.get(key));
					}else if(key.equals(UdeskConst.UdeskUserInfo.EMAIL)){
						builder.append("&c_email=").append(userinfo.get(key));
					}else if(key.equals(UdeskConst.UdeskUserInfo.DESCRIPTION)){
						builder.append("&c_desc=").append(userinfo.get(key));
					}
				}
			}
		}
		if(textField != null && !textField.isEmpty()){
			Set<String> textFieldSet = textField.keySet();
			for (String key : textFieldSet) {
				if(!TextUtils.isEmpty(textField.get(key))){
					builder.append("&c_cf_").append(key).append("=").append(textField.get(key));
				}
			}
		}
		return builder.toString();
	}

	public static  void initCrashReport(Context context){
		//因为CrashReport中有个内存没法释放，并且没法去改对应代码，因此将奔溃报告注释
//		CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
//		strategy.setAppVersion(UdeskCoreConst.sdkversion);
//		CrashReport.initCrashReport(context, UdeskCoreConst.buglyAppid, false, strategy);
	}

	public static void closeCrashReport(){
//		try{
//			CrashReport.closeCrashReport();
//		}catch (Exception e){
//
//		}

	}

	public static int getDisplayWidthPixels(Activity activity) {
		DisplayMetrics dMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
		return dMetrics.widthPixels;
	}

	public static String formatLongTypeTimeToString(Context context ,long time){
		long OFFSET_DAY = 3600 * 24;
		String timeYes = context.getString(R.string.udesk_im_time_format_yday);
		String timeQt =  context.getString(R.string.udesk_im_time_format_dby);
		String timeDate = "yyyy/MM/dd";
		Calendar calendar = Calendar.getInstance();
		StringBuilder build = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

		// 解析需要转化时间
		calendar.setTimeInMillis(time);
		int year = calendar.get(Calendar.YEAR);
		int day = calendar.get(Calendar.DAY_OF_YEAR);

		// 拼接 转化结果
		build.append(" ").append(sdf.format(calendar.getTime()));// 先添加

		// 先解析当前时间。取出当前年，日 等信息
		calendar.setTimeInMillis(System.currentTimeMillis());
		int nowYear = calendar.get(Calendar.YEAR);
		int nowDay = calendar.get(Calendar.DAY_OF_YEAR);

		if (year != nowYear) {// 不是一年内
			calendar.set(Calendar.HOUR_OF_DAY, 0); // 凌晨1点
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);

			if ((calendar.getTimeInMillis() - time) <= OFFSET_DAY) {// 昨天
				return timeYes;
			} else if ((calendar.getTimeInMillis() - time) <= (OFFSET_DAY << 2)) {// 前天
				// 。这里不用判断是否大于OFFSET_DAY
				return timeQt;
			} else {
				sdf.applyLocalizedPattern(timeDate);
				return sdf.format(time);
			}

		} else if (day == nowDay) {// 这里是一年内的当天
			// 当天的话 就不用管了
		} else {// 一年内
			int dayOffset = (nowDay - day);// nowDay要大一些
			if (dayOffset == 0) {
				// 同一天不用 添加日期判断
			} else if (dayOffset == 1) {// 1表示差一天，即昨天
				return timeYes;
			} else if (dayOffset == 2) {// 1表示差两天，即前天
				return timeQt;
			} else {
				timeDate = "MM月dd日";
				sdf.applyLocalizedPattern(timeDate);
				return sdf.format(time);
			}
		}

		return build.toString();
	}


	public static boolean isZh(Context context) {
		Locale locale = context.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		return language.endsWith("zh");
	}


	public static String getDeviceId(Context context) {
		String deviceId = "";
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		try {
			deviceId = telephonyManager.getDeviceId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(!TextUtils.isEmpty(deviceId)){
			return  deviceId;
		}
		deviceId = telephonyManager.getSimSerialNumber();
		if(!TextUtils.isEmpty(deviceId)){
			return  deviceId;
		}
		deviceId = android.os.Build.SERIAL;
		if(!TextUtils.isEmpty(deviceId)){
			return  deviceId;
		}
		return deviceId;
	}
}
