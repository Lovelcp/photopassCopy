package com.pictureair.photopass.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by pengwu on 16/7/12.
 */
public class DownloadFileStatus implements Parcelable{

    private String url;  //文件存储路径，实际上使用的是1024的路径来命名，是由于购买照片后，重新登录后真实的原图路径会改变，会导致重新下载
    private String photoThumbnail_512;//缩略图512尺寸的路径
    private String photoThumbnail_1024;//缩略图1024尺寸的路径
    private String originalUrl;//原图路径
    private String newUrl;//新请求的路径
    private long currentSize;
    private long totalSize;
    private String loadSpeed;
    private String photoId;
    private int isVideo;
    private int position;
    private String photoThumbnail;
    public String shootOn;
    private String failedTime;//用作文件存储路径，文件名过长时保存会抛出异常，此时把文件名进行MD5处理，在预览图片的时候直接根据文件路径查看，而不是url
    private int videoWidth;//视频宽高
    private int videoHeight;
    public static final int DOWNLOAD_STATE_DOWNLOADING = 0x01;
    public static final int DOWNLOAD_STATE_WAITING = 0x02;
    public static final int DOWNLOAD_STATE_FAILURE = 0x03;
    public static final int DOWNLOAD_STATE_FINISH = 0x04;
    public static final int DOWNLOAD_STATE_RECONNECT = 0x05;
    public static final int DOWNLOAD_STATE_SELECT = 0x06;
    public static final int DOWNLOAD_STATE_UPLOADING = 0x07;
    public int status = DOWNLOAD_STATE_WAITING;
    public int lastStatus = 0;
    /**
     * 0表示未选中 1表示选中
     * */
    public int select = 0;


    public DownloadFileStatus(){

    }

    public DownloadFileStatus(Parcel source) {
        this.url = source.readString();
        this.photoThumbnail_512 = source.readString();
        this.photoThumbnail_1024 = source.readString();
        this.originalUrl = source.readString();
        this.newUrl = source.readString();
        this.currentSize = source.readLong();
        this.totalSize = source.readLong();
        this.loadSpeed =source.readString();
        this.photoId = source.readString();
        this.isVideo = source.readInt();
        this.status = source.readInt();
        this.position = source.readInt();
        this.photoThumbnail = source.readString();
        this.shootOn = source.readString();
        this.failedTime = source.readString();
        this.select = source.readInt();
        this.videoWidth = source.readInt();
        this.videoHeight = source.readInt();
        this.lastStatus = source.readInt();
    }

    public DownloadFileStatus(String url,String photoThumbnail_512,String photoThumbnail_1024,String originalUrl, long currentSize,
                              long totalSize, String loadSpeed , String photoId, int isVideo,String photoThumbnail,String shootOn,
                              String failedTime,int videoWidth,int videoHeight) {
        if (url == null) url = "";
        if (photoThumbnail_512 == null) photoThumbnail_512 = "";
        if (photoThumbnail_1024 == null) photoThumbnail_1024 = "";
        if (originalUrl == null) originalUrl = "";
        if (photoThumbnail == null) photoThumbnail = "";
        this.url = url;
        this.photoThumbnail_512 = photoThumbnail_512;
        this.photoThumbnail_1024 = photoThumbnail_1024;
        this.originalUrl = originalUrl;
        this.currentSize = currentSize;
        this.totalSize = totalSize;
        this.loadSpeed = loadSpeed;
        this.photoId = photoId;
        this.isVideo = isVideo;
        this.photoThumbnail = photoThumbnail;
        this.shootOn = shootOn;
        this.failedTime = failedTime;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null) url = "";
        this.url = url;
    }

    public String getPhotoThumbnail_512() {
        return photoThumbnail_512;
    }

    public void setPhotoThumbnail_512(String photoThumbnail_512) {
        if (photoThumbnail_512 == null) photoThumbnail_512 = "";
        this.photoThumbnail_512 = photoThumbnail_512;
    }

    public String getPhotoThumbnail_1024() {
        return photoThumbnail_1024;
    }

    public void setPhotoThumbnail_1024(String photoThumbnail_1024) {
        if (photoThumbnail_1024 == null) photoThumbnail_1024 = "";
        this.photoThumbnail_1024 = photoThumbnail_1024;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        if (originalUrl == null) originalUrl = "";
        this.originalUrl = originalUrl;
    }

    public String getNewUrl() {
        return newUrl;
    }

    public void setNewUrl(String newUrl) {
        if (newUrl == null) newUrl = "";
        this.newUrl = newUrl;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(long currentSize) {
//        if (!TextUtils.isEmpty(currentSize)){
//            currentSize = format(currentSize);
//        }
        this.currentSize = currentSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
//        if (!TextUtils.isEmpty(totalSize)){
//            totalSize = format(totalSize);
//        }
        this.totalSize = totalSize;
    }

    public String getLoadSpeed() {
        return loadSpeed;
    }

    public void setLoadSpeed(String loadSpeed) {
        if (!TextUtils.isEmpty(loadSpeed)){
            loadSpeed = format(loadSpeed);
        }
        this.loadSpeed = loadSpeed;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public int isVideo() {
        return isVideo;
    }

    public void setVideo(int video) {
        isVideo = video;
    }

    public int getIsVideo() {
        return isVideo;
    }

    public void setIsVideo(int isVideo) {
        this.isVideo = isVideo;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getPhotoThumbnail() {
        return photoThumbnail;
    }

    public void setPhotoThumbnail(String photoThumbnail) {
        if (photoThumbnail == null) photoThumbnail = "";
        this.photoThumbnail = photoThumbnail;
    }

    public String getShootOn() {
        return shootOn;
    }

    public void setShootOn(String shootOn) {
        this.shootOn = shootOn;
    }

    public String getFailedTime() {
        return failedTime;
    }

    public void setFailedTime(String failedTime) {
        this.failedTime = failedTime;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(photoThumbnail_512);
        dest.writeString(photoThumbnail_1024);
        dest.writeString(originalUrl);
        dest.writeString(newUrl);
        dest.writeLong(currentSize);
        dest.writeLong(totalSize);
        dest.writeString(loadSpeed);
        dest.writeString(photoId);
        dest.writeInt(isVideo);
        dest.writeInt(status);
        dest.writeInt(position);
        dest.writeString(photoThumbnail);
        dest.writeString(shootOn);
        dest.writeString(failedTime);
        dest.writeInt(select);
        dest.writeInt(lastStatus);
    }

    public static final Parcelable.Creator<DownloadFileStatus> CREATOR = new Creator<DownloadFileStatus>() {

        @Override
        public DownloadFileStatus[] newArray(int size) {
            return new DownloadFileStatus[size];
        }

        @Override
        public DownloadFileStatus createFromParcel(Parcel source) {
            return new DownloadFileStatus(source);
        }
    };

    private String format(String str){
        if (str.indexOf(",")>0){
            str = str.replace(",",".");
        }
        if (str.indexOf("，")>0){
            str = str.replace("，",".");
        }
        return str;
    }

}
