/*
 * Copyright (C) 2012 Lightbox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pictureair.photopass.editPhoto;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Environment;
import android.view.Display;
import android.view.View;

import com.pictureair.photopass.util.PictureAirLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * BitmapUtils
 *
 * @author panyi
 */
public class BitmapUtils {
	/** Used to tag logs */
	@SuppressWarnings("unused")
	private static final String TAG = "BitmapUtils";

	public static final long MAX_SZIE = 1024 * 5120;// 500KB

	public static Bitmap loadImageByPath(final String imagePath, int reqWidth,
			int reqHeight) {
		File file = new File(imagePath);
		if (file.length() < MAX_SZIE) {
			return getSampledBitmap(imagePath, reqWidth, reqHeight);
		} else {// 压缩图片
			return getImageCompress(imagePath);
		}
	}

	public static int getOrientation(final String imagePath) {
		int rotate = 0;
		try {
			File imageFile = new File(imagePath);
			ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rotate;
	}

	public static Bitmap getSampledBitmap(String filePath, int reqWidth,
			int reqHeight) {
		Options options = new Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(filePath, options);

		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = (int) Math
						.floor(((float) height / reqHeight) + 0.5f); // Math.round((float)height
																		// /
																		// (float)reqHeight);
			} else {
				inSampleSize = (int) Math
						.floor(((float) width / reqWidth) + 0.5f); // Math.round((float)width
																	// /
																	// (float)reqWidth);
			}
		}
		// System.out.println("inSampleSize--->"+inSampleSize);

		options.inSampleSize = inSampleSize;
		options.inJustDecodeBounds = false;

		return BitmapFactory.decodeFile(filePath, options);
	}

	public static BitmapSize getBitmapSize(String filePath) {
		Options options = new Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(filePath, options);

		return new BitmapSize(options.outWidth, options.outHeight);
	}

	public static BitmapSize getScaledSize(int originalWidth,
			int originalHeight, int numPixels) {
		float ratio = (float) originalWidth / originalHeight;

		int scaledHeight = (int) Math.sqrt((float) numPixels / ratio);
		int scaledWidth = (int) (ratio * Math.sqrt((float) numPixels
				/ ratio));

		return new BitmapSize(scaledWidth, scaledHeight);
	}

	public static class BitmapSize {
		public int width;
		public int height;

		public BitmapSize(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}

	public static byte[] bitmapTobytes(Bitmap bitmap) {
		ByteArrayOutputStream a = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 30, a);
		return a.toByteArray();
	}

	public static byte[] bitmapTobytesNoCompress(Bitmap bitmap) {
		ByteArrayOutputStream a = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, a);
		return a.toByteArray();
	}

	public static Bitmap genRotateBitmap(byte[] data) {
		Bitmap bMap = BitmapFactory.decodeByteArray(data, 0, data.length);
		// 自定义相机拍照需要旋转90预览支持竖屏
		Matrix matrix = new Matrix();// 矩阵
		matrix.reset();// 设置为单位矩阵
		matrix.postRotate(90);// 旋转90度
		Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(),
				bMap.getHeight(), matrix, true);
		bMap.recycle();
		bMap = null;
		System.gc();
		return bMapRotate;
	}

	public static Bitmap byteToBitmap(byte[] data) {
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	/**
	 * 将view转为bitmap
	 *
	 * @param view
	 * @return
	 */
	public static Bitmap getBitmapFromView(View view) {
		Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(),
				view.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(returnedBitmap);
		Drawable bgDrawable = view.getBackground();
		if (bgDrawable != null)
			bgDrawable.draw(canvas);
		else
			canvas.drawColor(Color.WHITE);
		view.draw(canvas);
		return returnedBitmap;
	}

	// 按大小缩放
	public static Bitmap getImageCompress(final String srcPath) {
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		// 开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// 此时返回bm为空

		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		// 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
		float hh = 800f;// 这里设置高度为800f
		float ww = 480f;// 这里设置宽度为480f
		// 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
		int be = 1;// be=1表示不缩放
		if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
			be = (int) (newOpts.outWidth / ww);
		} else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
			be = (int) (newOpts.outHeight / hh);
		}
		if (be <= 0)
			be = 1;
		newOpts.inSampleSize = be;// 设置缩放比例
		// 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
		bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
		return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
	}

	// 图片按比例大小压缩
	public static Bitmap compress(Bitmap image) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		if (baos.toByteArray().length / 1024 > 1024) {// 判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
			baos.reset();// 重置baos即清空baos
			image.compress(Bitmap.CompressFormat.JPEG, 50, baos);// 这里压缩50%，把压缩后的数据存放到baos中
		}
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		// 开始读入图片，此时把options.inJustDecodeBounds 设回true了
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		// 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
		float hh = 800f;// 这里设置高度为800f
		float ww = 480f;// 这里设置宽度为480f
		// 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
		int be = 1;// be=1表示不缩放
		if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
			be = (int) (newOpts.outWidth / ww);
		} else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
			be = (int) (newOpts.outHeight / hh);
		}
		if (be <= 0)
			be = 1;
		newOpts.inSampleSize = be;// 设置缩放比例
		// 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
		isBm = new ByteArrayInputStream(baos.toByteArray());
		bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
		return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
	}

	// 图片质量压缩
	private static Bitmap compressImage(Bitmap image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
		int options = 100;

		while (baos.toByteArray().length / 1024 > 100) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
			baos.reset();// 重置baos即清空baos
			image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
			options -= 10;// 每次都减少10
//			System.out.println("options--->" + options + "    "
//					+ (baos.toByteArray().length / 1024));
		}
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
		return bitmap;
	}

	public void printscreen_share(View v, Activity context) {
		View view1 = context.getWindow().getDecorView();
		Display display = context.getWindowManager().getDefaultDisplay();
		view1.layout(0, 0, display.getWidth(), display.getHeight());
		view1.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(view1.getDrawingCache());
	}

	// 图片转为文件
	public static boolean saveBitmap2file(Bitmap bmp, String filepath) {
		CompressFormat format = Bitmap.CompressFormat.PNG;
		int quality = 100;
		OutputStream stream = null;
		try {
			// 判断SDcard状态
			if (!Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				// 错误提示
				return false;
			}

			// 检查SDcard空间
			File SDCardRoot = Environment.getExternalStorageDirectory();
			if (SDCardRoot.getFreeSpace() < 10000) {
				// 弹出对话框提示用户空间不够
				PictureAirLog.d("Utils", "存储空间不够");
				return false;
			}

			// 在SDcard创建文件夹及文件
			File bitmapFile = new File(SDCardRoot.getPath() + filepath);
			bitmapFile.getParentFile().mkdirs();// 创建文件夹
			stream = new FileOutputStream(SDCardRoot.getPath() + filepath);// "/sdcard/"
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return bmp.compress(format, quality, stream);
	}

	/**
	 * 截屏
	 *
	 * @param activity
	 * @return
	 */
	public static Bitmap getScreenViewBitmap(Activity activity) {
		View view = activity.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap bitmap = view.getDrawingCache();
		return bitmap;
	}

	/**
	 * 一个 View的图像
	 *
	 * @param view
	 * @return
	 */
	public static Bitmap getViewBitmap(View view) {
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap bitmap = view.getDrawingCache();
		return bitmap;
	}

}
