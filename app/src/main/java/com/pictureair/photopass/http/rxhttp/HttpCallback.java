package com.pictureair.photopass.http.rxhttp;


/**
 * Created by milo on 15/12/7.
 * 创建Http请求辅助类
 * 注意：这里必须实现接口里面的所有方法，不然后面调用就会自动实现接口里面没有实现的方法。
 */
public abstract class HttpCallback implements HttpInterface {
    //实现所有方法，方便后面任意调用其中某一个

    @Override
    public void onProgress(long bytesWritten, long totalSize) {

    }

    @Override
    public void doOnSubscribe() {

    }
}
