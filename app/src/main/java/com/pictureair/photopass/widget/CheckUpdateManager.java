package com.pictureair.photopass.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pictureair.photopass.MyApplication;
import com.pictureair.photopass.R;
import com.pictureair.photopass.customDialog.PWDialog;
import com.pictureair.photopass.db.PictureAirDbManager;
import com.pictureair.photopass.entity.BaseCheckUpdate;
import com.pictureair.photopass.entity.FileInfo;
import com.pictureair.photopass.service.BreakpointDownloadService;
import com.pictureair.photopass.util.ACache;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.PictureAirLog;

import java.io.File;
import java.util.ArrayList;

/**
 * 检查更新apk封装类
 */
public class CheckUpdateManager implements PWDialog.OnPWDialogClickListener {
    private BaseCheckUpdate baseCheckUpdate;
    private Context context;
    private ArrayList<String> deviceInfos;
    private PWDialog pictureWorksDialog;
    private String downloadURL, forceUpdate, currentLanguage;
    private String channelStr;
    private PWProgressBarDialog pwProgressBarDialog;
    private PWToast myToast;
    private String version;
    private File downloadAPKFile;
    private static final int INSTALL_APK = 201;
    private static final int GENERATE_APK_FAILED = 202;
    private boolean isRegisterReceiver = false;
    private boolean hasDestroyed = false;
    private final int UPDATE_PB = 203;
    private static final int APK_NEED_NOT_UPDATE = 204;
    private static final int APK_NEED_UPDATE = 205;
    private static final int UPDATE_DIALOG = 206;
    private PictureAirDbManager dbDAO = null;

    /**
     * 接受广播
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BreakpointDownloadService.ACTION_UPDATE.equals(intent.getAction())) {
                long bytesWritten = intent.getLongExtra("bytesWritten", 0);
                long totalSize = 100;
                boolean onFailure = intent.getBooleanExtra("onFailure", false);

                if (bytesWritten == 100){
                    //下载完毕
                    handler.sendEmptyMessage(INSTALL_APK);
                } else if (onFailure){//下载失败
                    handler.sendEmptyMessage(API1.DOWNLOAD_APK_FAILED);
                } else{//还在下载
                    PictureAirLog.out("totalSize---->" + totalSize);
                    long[] numbers ={bytesWritten,totalSize};
                    handler.obtainMessage(UPDATE_PB,numbers).sendToTarget();
                }
            }
        }
    };

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_PB:
                    pwProgressBarDialog.setProgress(((long[])msg.obj)[0], ((long[])msg.obj)[1]);
                    break;

                case API1.GET_TOKEN_ID_FAILED:
                    break;

                case API1.GET_TOKEN_ID_SUCCESS:
                    checkApk();
                    break;

                case API1.GET_UPDATE_SUCCESS:
                    dealUpdateInfo(msg.obj.toString());
                    break;

                case API1.GET_UPDATE_FAILED:
                    if (!hasDestroyed) {
                        String updateInfo = ACache.get(context).getAsString(Common.UPDATE_INFO);
                        PictureAirLog.out("acahe--->" + updateInfo);
                        if (TextUtils.isEmpty(updateInfo)) {//缓存中没有
                            PictureAirLog.out("apk need not update");
                        } else {//缓存中有数据
                            handler.obtainMessage(API1.GET_UPDATE_SUCCESS, updateInfo).sendToTarget();
                        }
                    } else {
                        PictureAirLog.out("apk need not update");
                    }
                    break;

                case APK_NEED_NOT_UPDATE://不更新
                    PictureAirLog.out("apk need not update");
                    break;

                case APK_NEED_UPDATE:
                    PictureAirLog.out("apk need update");
                    //开始显示对话框
                    showUpdateApkDialog(msg);
                    break;

                case API1.DOWNLOAD_APK_FAILED:
                    PictureAirLog.out("download apk failed");
                    pwProgressBarDialog.pwProgressBarDialogDismiss();
                    myToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    pictureWorksDialog.pwDilogShow();
                    break;

                case INSTALL_APK:
                    insertAPK();
                    break;

                case GENERATE_APK_FAILED://生成apk失败
                    myToast.setTextAndShow(R.string.generate_apk_failed, Common.TOAST_SHORT_TIME);
                    break;

                default:
                    break;
            }
        }

        ;
    };

    /**
     * 处理更新数据，检查是否需要更新
     * @param updateInfo
     */
    private void dealUpdateInfo(String updateInfo) {
        JSONObject jsonObject = JSONObject.parseObject(updateInfo);
        String thisVerName = deviceInfos.get(1);
        String language = currentLanguage;
        PictureAirLog.out("channel------>" + channelStr);
        PictureAirLog.out("thisVer------>" + thisVerName);
        if (jsonObject.getJSONObject("version").getJSONArray("versionOS").toString().contains("android")) {
            //结果不为null，并且结果更新平台中有android，则需要更新
            JSONObject versionObject = jsonObject.getJSONObject("version");
            String versionName = versionObject.getString("version");
            String mandatory = versionObject.getString("mandatory");
            String content_EN = versionObject.getString("content_EN");
            String content = versionObject.getString("content");
            String channel = "";
            String downloadUrl = "";
            String websiteDownloadUrl = "";

            JSONArray array = versionObject.getJSONArray("downloadChannel");
            for (int i = 0; i < array.size(); i++) {
                channel = array.getJSONObject(i).getString("channel");
                if (channel.equals("tencent")) {//官网渠道
                    websiteDownloadUrl = array.getJSONObject(i).getString("downloadUrl");
                }
                if (channelStr.equals(channel)) {
                    downloadUrl = array.getJSONObject(i).getString("downloadUrl");
                    break;
                }
            }

            boolean flag = false;//为false则不更新
            int[] number = CheckUpdateManager.verNameChangeInt(thisVerName);
            int[] newNumber = CheckUpdateManager.verNameChangeInt(versionName);
            //根据版本号判断是否需要更新
            for (int i = 0; i < number.length; i++) {
                if (number[i] < newNumber[i]) {
                    //需要更新
                    flag = true;
                    break;

                } else if (number[i] == newNumber[i]){
                    continue;

                } else {
                    break;

                }
            }

            if (TextUtils.isEmpty(downloadUrl)) {//判断下载链接是否为空
                if (TextUtils.isEmpty(websiteDownloadUrl)) {//官网下载链接为空
                    flag = false;
                } else {
                    downloadUrl = websiteDownloadUrl;
                }
            } else {

            }

            if (flag) {
                //更新
                String[] objsStrings = new String[4];
                objsStrings[0] = versionName;
                objsStrings[1] = mandatory;

                objsStrings[3] = downloadUrl;
                PictureAirLog.d("api update", language);

                if (null != language && language.equals("en")) {
                    objsStrings[2] = content_EN;
                } else {
                    objsStrings[2] = content;
                }
                Message message = new Message();
                message.what = APK_NEED_UPDATE;
                message.obj = objsStrings;
                handler.sendMessage(message);
            } else {
                handler.sendEmptyMessage(APK_NEED_NOT_UPDATE);
            }
        } else {
            handler.sendEmptyMessage(APK_NEED_NOT_UPDATE);
        }
    }

    /**
     * 询问是否更新APK Dialog。
     * @param msg
     */
    private void showUpdateApkDialog(Message msg) {

        String objsString[] = (String[]) msg.obj;
        downloadURL = objsString[3];
        forceUpdate = objsString[1];
        version = objsString[0];
        if (hasDestroyed){
            return;
        }

        if (pictureWorksDialog == null) {
            pictureWorksDialog = new PWDialog(context, UPDATE_DIALOG)
                    .setPWDialogTitle(String.format(context.getString(R.string.update_version), version))
                    .setPWDialogMessage(objsString[2])
                    .setPWDialogNegativeButton(forceUpdate.equals("true") ? null : context.getString(R.string.cancel1))
                    .setPWDialogPositiveButton(R.string.down)
                    .setPWDialogContentCenter(false)
                    .setOnPWDialogClickListener(this)
                    .pwDialogCreate();
        }
        pictureWorksDialog.pwDilogShow();
    }

    /**
     * 自动检查更新封装类
     * <p/>
     * 怎么使用
     * 1.申明变量
     * 2.checkUpdateManager = new CheckUpdateManager(this, currentLanguage);
     * 3.checkUpdateManager.startCheck();
     *
     * @param context
     * @param currentLanguage 当前语言
     */
    public CheckUpdateManager(Context context, String currentLanguage) {
        this.context = context;
        this.currentLanguage = currentLanguage;
        myToast = new PWToast(context);
        channelStr = AppUtil.getMetaData(context, "UMENG_CHANNEL");
    }

    /**
     * 初始化
     */
    public void init(){
        baseCheckUpdate = CheckUpdate.getInstance();
        dbDAO = new PictureAirDbManager(context);
    }

    /**
     * 先检查tokenid，再
     * 开始检查更新
     */
    public void startCheck() {
        deviceInfos = AppUtil.getDeviceInfos(context);
        if (MyApplication.getTokenId() == null) {
            baseCheckUpdate.getTokenId(context, handler);
        } else {
            checkApk();
        }
    }

    /**
     * 开始检查更新
     */
    private void checkApk(){
        baseCheckUpdate.checkUpdate(context, handler);
    }

    /**
     * 根据版本号，返回数组进行判断
     */
    public static int[] verNameChangeInt(String versionName) {
        int[] number = new int[3];
        String[] numberStr = versionName.split("\\.");
        int number0 = 0;
        int number1 = 0;
        int number2 = 0;
        try {
            number0 = Integer.valueOf(numberStr[0]);
            number1 = Integer.valueOf(numberStr[1]);
            number2 = Integer.valueOf(numberStr[2]);
        } catch (Exception e) {

        }
        number[0] = number0;
        number[1] = number1;
        number[2] = number2;
        return number;
    }

    /**
     * 确定下载
     */
    public void dialogButtonPositive() {
        String fileName = "SHDRPhotoPass_" + version + ".apk";
        PictureAirLog.out("apk yes --->" + fileName);
        downloadAPKFile = new File(Common.DOWNLOAD_APK_PATH + fileName);

        if (downloadAPKFile.exists() && !dbDAO.isExistsThread()) {//文件已经存在
            PictureAirLog.out("apk exist");
            insertAPK();
        } else {//文件不存在，需要去下载
            //开始下载.启用service下载
            if (pwProgressBarDialog == null) {
                pwProgressBarDialog = new PWProgressBarDialog(context).pwProgressBarDialogCreate(PWProgressBarDialog.TYPE_DOWNLOAD);
            }
            pwProgressBarDialog.pwProgressBarDialogShow();

            //注册广播接收器
            registerReceiver();
            //创建文件信息
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(0);
            fileInfo.setUrl(downloadURL);
            fileInfo.setFileName(fileName);
            fileInfo.setFinished(0);
            fileInfo.setLength(0);

            if (!downloadAPKFile.exists()) {//如果文件不存在，需要把数据库中对应的记录删除
                dbDAO.deleteThread(downloadURL, 0);
            }

            Intent intent = new Intent(context, BreakpointDownloadService.class);
            intent.setAction(BreakpointDownloadService.ACTION_STRAT);
            intent.putExtra("fileInfo", fileInfo);
            context.startService(intent);
        }
    }

    /**
     * 安装apk
     */
    private void insertAPK() {
        /**
         * 需要在安装之前判断apk文件的md5值
         * 需要服务器穿一个md5值 与当前本地算出的md5做对比
         * 如果文件较大 需要放在线程中处理
         * start
         */
//        try {
//            AppUtil.getMd5ByFile(downloadAPKFile);
//        }catch (Exception e){
//
//        }
        /**
         * end
         * 需要在安装之前判断apk文件的md5值
         */
        if (pwProgressBarDialog != null) {
            pwProgressBarDialog.pwProgressBarDialogDismiss();
        }
        //开始安装新版本
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(downloadAPKFile), "application/vnd.android.package-archive");
        context.startActivity(intent);
        AppManager.getInstance().AppExit(context);
    }

    /**
     * 注册广播
     */
    public void registerReceiver() {
        isRegisterReceiver = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BreakpointDownloadService.ACTION_UPDATE);
        context.registerReceiver(receiver, filter);
    }

    /**
     * 销毁CheckUpdateManager
     */
    public void onDestroy(){
        PictureAirLog.out("check update manager on destroy");
        hasDestroyed = true;
        unregisterReceiver();
        dismissDialog();

        if (pwProgressBarDialog != null) {
            pwProgressBarDialog.pwProgressBarDialogDismiss();
        }
    }

    /**
     * 注销广播
     */
    public void unregisterReceiver() {
        if (null != receiver && isRegisterReceiver){
            try{
                context.unregisterReceiver(receiver);
                isRegisterReceiver = false;
            }catch (Exception e){
            }
        }
    }

    /**
     * 注销dialog
     */
    private void dismissDialog(){
        if (null != pictureWorksDialog){
            pictureWorksDialog.dismiss();
            pictureWorksDialog = null;
        }
    }

    @Override
    public void onPWDialogButtonClicked(int which, int dialogId) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (dialogId == UPDATE_DIALOG) {
                    if (AppUtil.checkPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        dialogButtonPositive();//确认下载
                    }else{
                        myToast.setTextAndShow(R.string.permission_storage_message, Common.TOAST_SHORT_TIME);
                    }
                }
                break;
        }
    }
}
