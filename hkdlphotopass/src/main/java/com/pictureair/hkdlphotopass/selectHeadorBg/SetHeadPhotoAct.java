package com.pictureair.hkdlphotopass.selectHeadorBg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.alibaba.fastjson.JSONObject;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;
import com.pictureair.hkdlphotopass.MyApplication;
import com.pictureair.hkdlphotopass.R;
import com.pictureair.hkdlphotopass.activity.BaseActivity;
import com.pictureworks.android.util.BlurUtil;
import com.pictureworks.android.util.API1;
import com.pictureworks.android.util.Common;
import com.pictureworks.android.util.ReflectionUtil;
import com.pictureworks.android.widget.CustomProgressBarPop;
import com.pictureworks.android.widget.MyToast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * 头像选取
 */
public class SetHeadPhotoAct extends BaseActivity implements OnClickListener {
    private ClipImageLayout mClipImageLayout;
    private final String IMAGE_TYPE = "image/*";
    private final int IMAGE_CODE = 0; // 这里的IMAGE_CODE是自己任意定义的
    private ImageView clip;
    private ImageView back;
    private SharedPreferences sp;
    private CustomProgressBarPop dialog;
    private MyToast myToast;
    private File headPhoto;

    private final Handler setHeadPhotoHandler = new SetHeadPhotoHandler(this);


    private static class SetHeadPhotoHandler extends Handler{
        private final WeakReference<SetHeadPhotoAct> mActivity;

        public SetHeadPhotoHandler(SetHeadPhotoAct activity){
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
     * @param msg
     */
    private void dealHandler(Message msg) {
        switch (msg.what) {
            case API1.UPDATE_USER_IMAGE_SUCCESS:
                JSONObject jsonObject = (JSONObject) msg.obj;
                String imageUrl = jsonObject.getString("imageUrl");
                Editor e = sp.edit();
                e.putString(Common.USERINFO_HEADPHOTO, imageUrl);
                e.apply();

                //上传成功之后，需要将临时的头像文件的名字改为正常的名字
                File oldFile = new File(Common.USER_PATH + Common.HEADPHOTO_PATH);
                if (oldFile.exists()) {//如果之前的文件存在
                    //先删除之前的文件
                    oldFile.delete();
                    //修改现在的文件名字
                    if (headPhoto.exists()) {
                        headPhoto.renameTo(oldFile);
                    }
                } else {
                    //文件不存在，则重新创建文件夹
                    try {
                        oldFile.createNewFile();
                        headPhoto.renameTo(oldFile);

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

                clearImageCache(imageUrl);//清除之前的缓存
                dialog.dismiss();
//                myToast.setTextAndShow(R.string.save_success, Common.TOAST_SHORT_TIME);
                finish();
                break;

            case API1.UPDATE_USER_IMAGE_FAILED:
                //删除头像的临时文件
                dialog.dismiss();
                if (headPhoto.exists()) {
                    headPhoto.delete();
                }
                myToast.setTextAndShow(ReflectionUtil.getStringId(MyApplication.getInstance(), msg.arg1), Common.TOAST_SHORT_TIME);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(Common.SHARED_PREFERENCE_USERINFO_NAME, MODE_PRIVATE);

        setContentView(R.layout.activity_set_head_photo);
        myToast = new MyToast(this);
        Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);
        getAlbum.setType(IMAGE_TYPE);
        initView();
        //跳转相册选择页面
        startActivityForResult(getAlbum, IMAGE_CODE);
    }

    private void initView() {
        mClipImageLayout = (ClipImageLayout) findViewById(R.id.clipImageLayout);
        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(this);
        clip = (ImageView) findViewById(R.id.clip);
        clip.setOnClickListener(this);
    }

    /**
     * 清除缓存，更换头像时清除
     */
    public static void clearImageCache(String avatarUrl) {
        DiskCacheUtils.removeFromCache(Common.PHOTO_URL + avatarUrl, ImageLoader.getInstance().getDiskCache());
        MemoryCacheUtils.removeFromCache(Common.PHOTO_URL + avatarUrl, ImageLoader.getInstance().getMemoryCache());
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clip:
                if (!isNetWorkConnect(this)) {
                    myToast.setTextAndShow(R.string.http_error_code_401, Common.TOAST_SHORT_TIME);
                    return;
                }
                dialog = new CustomProgressBarPop(this, findViewById(R.id.setHeadRelativeLayout), CustomProgressBarPop.TYPE_UPLOAD);
                dialog.show(0);
                Bitmap bitmap = mClipImageLayout.clip();
                bitmap = BlurUtil.comp(bitmap);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] datas = baos.toByteArray();
                File userFile = new File(Common.USER_PATH);
                if (!userFile.exists()) {
                    userFile.mkdirs();
                }
                headPhoto = new File(Common.USER_PATH + Common.HEADPHOTO_PATH + "_temp");
                BufferedOutputStream stream = null;
                try {
                    headPhoto.createNewFile();
                    FileOutputStream fstream = new FileOutputStream(headPhoto);
                    stream = new BufferedOutputStream(fstream);
                    stream.write(datas);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {

                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    try {
                        // 需要更新服务器中用户头像图片信息
                        RequestParams params = new RequestParams();
                        params.put(Common.USERINFO_TOKENID, MyApplication.getTokenId());
                        params.put("updateType", "avatar");
                        params.put("file", headPhoto);
                        API1.updateUserImage(params, setHeadPhotoHandler, 0, dialog);
                    } catch (FileNotFoundException ee) {
                        // TODO Auto-generated catch block
                        ee.printStackTrace();
                    }
                }
                break;

            case R.id.back:
                finish();
                break;

            default:
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            finish();
            return;
        }
        // 此处的用于判断接收的Activity是不是你想要的那个
        if (requestCode == IMAGE_CODE) {
            Uri originalUri = data.getData(); // 获得图片的uri
            mClipImageLayout.setImage(originalUri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setHeadPhotoHandler.removeCallbacksAndMessages(null);
    }
}
