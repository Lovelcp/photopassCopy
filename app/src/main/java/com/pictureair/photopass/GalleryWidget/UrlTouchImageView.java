/*
 Copyright (c) 2012 Roman Truba

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial
 portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.pictureair.photopass.GalleryWidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.pictureair.photopass.R;
import com.pictureair.photopass.util.GlideUtil;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.ScreenUtil;

public class UrlTouchImageView extends RelativeLayout {
    protected TouchImageView mImageView;
    protected ImageView progressImageView;

    protected Context mContext;

    private Bitmap bitmap;

    private static final int LOAD_FILE_DONE = 1;
    private int defaultType;

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case LOAD_FILE_DONE:
                    if (bitmap != null) {
                        PictureAirLog.out("LOAD_FILE_DONE: " + "bitmap != null");
                        mImageView.setScaleType(ScaleType.MATRIX);
                        mImageView.setImageBitmap(bitmap);
                    } else {
                        PictureAirLog.out("LOAD_FILE_DONE: " + "bitmap == null");
                        mImageView.setScaleType(ScaleType.CENTER);
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_failed);
                        mImageView.setImageBitmap(bitmap);
                    }
                    mImageView.setVisibility(VISIBLE);
                    progressImageView.setVisibility(GONE);
                    break;

                default:
                    break;
            }
        }

        ;
    };

    public UrlTouchImageView(Context ctx) {
        super(ctx);
        mContext = ctx;
        init();

    }

    public UrlTouchImageView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        mContext = ctx;
        init();
    }

    public TouchImageView getImageView() {
        return mImageView;
    }

    protected void init() {
        mImageView = new TouchImageView(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mImageView.setLayoutParams(params);
        this.addView(mImageView);
        mImageView.setVisibility(GONE);

        progressImageView = new ImageView(mContext);
        params = new LayoutParams(ScreenUtil.getScreenWidth(mContext) / 3, ScreenUtil.getScreenWidth(mContext) / 3);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressImageView.setLayoutParams(params);
        progressImageView.setImageResource(R.drawable.loading_0);
        this.addView(progressImageView);
    }

    /**
     * 设置图片的url
     *
     * @param imageUrl 网络图片路径
     */
    public void setUrl(String imageUrl, boolean isEncrypted) {
        //使用imageloader加载图片
        GlideUtil.load(mContext, imageUrl, isEncrypted, new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> glideAnimation) {
                progressImageView.setImageResource(getImageResource(100));
                bitmap = Bitmap.createBitmap(loadedImage).copy(Bitmap.Config.ARGB_8888, false);
                handler.sendEmptyMessage(LOAD_FILE_DONE);
            }
        });
    }

    /**
     * 设置进度条可见
     * @param visible
     */
    public void setProgressImageViewVisible(boolean visible) {
        progressImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setScaleType(ScaleType scaleType) {
        mImageView.setScaleType(scaleType);
    }


    /**
     * 设置本地图片的路径
     *
     * @param imagePath 本地路径
     */
    public void setImagePath(String imagePath) {
        //为什么设置800，800，本地图片尽量不需要去加载原图，容易OOM，因此只要加载特定尺寸即可。
        //测试发现，800，800，不是最终的bitmap大小，而是和原图相比缩小了1倍，需要继续验证。
        GlideUtil.loadOverride(mContext, GlideUtil.getFileUrl(imagePath), 800, 800, new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String s, Target<Bitmap> target, boolean b) {
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap bitmap, String s, Target<Bitmap> target, boolean b, boolean b1) {
                PictureAirLog.out("width--->" + bitmap.getWidth() + "===" + bitmap.getHeight());
                progressImageView.setImageResource(getImageResource(100));
                progressImageView.setVisibility(GONE);
                mImageView.setScaleType(ScaleType.MATRIX);
                mImageView.setVisibility(VISIBLE);
                return false;
            }
        }, mImageView);
    }

    /**
     * 根据当前的下载percent来显示对应的图片，实现下载进度条的功能
     *
     * @param currentProgress
     * @return
     */
    private int getImageResource(long currentProgress) {
        int result = R.drawable.loading_0;
        if (currentProgress >= 0 && currentProgress <= 8) {
            result = R.drawable.loading_0;
        } else if (currentProgress > 8 && currentProgress <= 16) {
            result = R.drawable.loading_1;
        } else if (currentProgress > 16 && currentProgress <= 25) {
            result = R.drawable.loading_2;
        } else if (currentProgress > 25 && currentProgress <= 33) {
            result = R.drawable.loading_3;
        } else if (currentProgress > 33 && currentProgress <= 41) {
            result = R.drawable.loading_4;
        } else if (currentProgress > 41 && currentProgress <= 50) {
            result = R.drawable.loading_5;
        } else if (currentProgress > 50 && currentProgress <= 58) {
            result = R.drawable.loading_6;
        } else if (currentProgress > 58 && currentProgress <= 66) {
            result = R.drawable.loading_7;
        } else if (currentProgress > 66 && currentProgress <= 75) {
            result = R.drawable.loading_8;
        } else if (currentProgress > 75 && currentProgress <= 83) {
            result = R.drawable.loading_9;
        } else if (currentProgress > 83 && currentProgress <= 91) {
            result = R.drawable.loading_10;
        } else if (currentProgress > 91 && currentProgress < 100) {
            result = R.drawable.loading_11;
        } else {
            result = R.drawable.loading_12;
        }

        return result;
    }

    public void setDefaultType(int defaultType) {
        this.defaultType = defaultType;
    }
}
