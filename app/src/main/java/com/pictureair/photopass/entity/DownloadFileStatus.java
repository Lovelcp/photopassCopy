package com.pictureair.photopass.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by pengwu on 16/7/12.
 */
public class DownloadFileStatus implements Parcelable{

    private String url;  //图片路径
    private String newUrl;//新请求的路径
    private String currentSize;
    private String totalSize;
    private String loadSpeed;
    private String photoId;
    private int isVideo;
    private int position;
    private String photoThumbnail;
    public String shootOn;
    private String failedTime;
    public static final int DOWNLOAD_STATE_DOWNLOADING = 0x01;
    public static final int DOWNLOAD_STATE_WAITING = 0x02;
    public static final int DOWNLOAD_STATE_FAILURE = 0x03;
    public static final int DOWNLOAD_STATE_FINISH = 0x04;
    public static final int DOWNLOAD_STATE_RECONNECT = 0x05;
    public static final int DOWNLOAD_STATE_SELECT = 0x06;
    public int status = DOWNLOAD_STATE_WAITING;
    /**
     * 0表示未选中 1表示选中
     * */
    public int select = 0;


    public DownloadFileStatus(){

    }

    public DownloadFileStatus(Parcel source) {
        this.url = source.readString();
        this.newUrl = source.readString();
        this.currentSize = source.readString();
        this.totalSize = source.readString();
        this.loadSpeed =source.readString();
        this.photoId = source.readString();
        this.isVideo = source.readInt();
        this.status = source.readInt();
        this.position = source.readInt();
        this.photoThumbnail = source.readString();
        this.shootOn = source.readString();
        this.failedTime = source.readString();
        this.select = source.readInt();
    }

    public DownloadFileStatus(String url, String currentSize, String totalSize, String loadSpeed , String photoId, int isVideo,String photoThumbnail,String shootOn,String failedTime) {
        this.url = url;
        this.currentSize = currentSize;
        this.totalSize = totalSize;
        this.loadSpeed = loadSpeed;
        this.photoId = photoId;
        this.isVideo = isVideo;
        this.photoThumbnail = photoThumbnail;
        this.shootOn = shootOn;
        this.failedTime = failedTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNewUrl() {
        return newUrl;
    }

    public void setNewUrl(String newUrl) {
        this.newUrl = newUrl;
    }

    public String getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(String currentSize) {
        if (!TextUtils.isEmpty(currentSize)){
            currentSize = format(currentSize);
        }
        this.currentSize = currentSize;
    }

    public String getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(String totalSize) {
        if (!TextUtils.isEmpty(totalSize)){
            totalSize = format(totalSize);
        }
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(newUrl);
        dest.writeString(currentSize);
        dest.writeString(totalSize);
        dest.writeString(loadSpeed);
        dest.writeString(photoId);
        dest.writeInt(isVideo);
        dest.writeInt(status);
        dest.writeInt(position);
        dest.writeString(photoThumbnail);
        dest.writeString(shootOn);
        dest.writeString(failedTime);
        dest.writeInt(select);
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
