package com.pictureair.photopass.activity;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.pictureair.photopass.R;
import com.pictureair.photopass.adapter.EditActivityAdapter;
import com.pictureair.photopass.customDialog.PWDialog;
import com.pictureair.photopass.db.PictureAirDbManager;
import com.pictureair.photopass.editPhoto.BitmapUtils;
import com.pictureair.photopass.editPhoto.EditPhotoUtil;
import com.pictureair.photopass.editPhoto.Matrix3;
import com.pictureair.photopass.editPhoto.StickerItem;
import com.pictureair.photopass.editPhoto.StickerView;
import com.pictureair.photopass.entity.DiscoverLocationItemInfo;
import com.pictureair.photopass.entity.EditPhotoInfo;
import com.pictureair.photopass.entity.FrameOrStikerInfo;
import com.pictureair.photopass.entity.PhotoInfo;
import com.pictureair.photopass.entity.StikerInfo;
import com.pictureair.photopass.filter.Amaro;
import com.pictureair.photopass.filter.BeautifyFilter;
import com.pictureair.photopass.filter.BlurFilter;
import com.pictureair.photopass.filter.EarlyBird;
import com.pictureair.photopass.filter.Filter;
import com.pictureair.photopass.filter.HDRFilter;
import com.pictureair.photopass.filter.LomoFi;
import com.pictureair.photopass.filter.LomoFilter;
import com.pictureair.photopass.filter.NormalFilter;
import com.pictureair.photopass.filter.OldFilter;
import com.pictureair.photopass.util.ACache;
import com.pictureair.photopass.util.API1;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.GlideUtil;
import com.pictureair.photopass.util.LocationUtil;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.SPUtils;
import com.pictureair.photopass.util.ScreenUtil;
import com.pictureair.photopass.widget.HorizontalListView;
import com.pictureair.photopass.widget.PWToast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

//显示的时候用压缩过的bitmap，合成的时候，用原始的bitmap
public class EditPhotoActivity extends BaseActivity implements OnClickListener, LocationUtil.OnLocationNotificationListener, PWDialog.OnPWDialogClickListener {
	//视图
	private StickerView mStickerView;// 贴图层View
	private Bitmap mainBitmap; //低层显示的bitmap，就是编辑的图片。
	private ImageView mainImage; // 原始图

	private ImageView back,btn_left_back; //结束当前页面的back, 取消操作。
	private TextView edit_accessory,titleTextView,preview_save,edit_filter,edit_text,edit_frame;//饰品按钮，标题，保存,滤镜按钮, 文字按钮,边框按钮
	private ImageButton btn_onedit_save,btn_cancel,btn_forward;//保存，返回，前进
	private LinearLayout edittoolsbar,font_bar; // 工具条 和 底部的 旋转 bar

	private HorizontalListView top_HorizontalListView;  //显示饰品的滑动条

	private PWDialog pictureWorksDialog;

	//适配器
	private EditActivityAdapter eidtAdapter; //通用的适配器
	//对象
	private PhotoInfo photoInfo;
	private String photoURL;


	private LoadImageTask mLoadImageTask;

	private int imageWidth, imageHeight;// 展示图片控件 宽 高
	//
	private List<String> filterPathList = new ArrayList<String>(); //滤镜图片的路径列表

	private ArrayList<FrameOrStikerInfo> frameInfos = new ArrayList<FrameOrStikerInfo>(); //保存 高清边框的集合。
	private ArrayList<FrameOrStikerInfo> frameFromDBInfos;//来自数据库的数据
	private ArrayList<FrameOrStikerInfo> stikerInfos = new ArrayList<FrameOrStikerInfo>();// 饰品图片路径列表
	private ArrayList<FrameOrStikerInfo> stickerFromDBInfos;//来自数据库的数据
	private ArrayList<DiscoverLocationItemInfo> locationItemInfos;

	public static final String STICKERPATH = "rekcits";
	public static final String FILTERPATH = "retlif";
	public static final String FRAMEPATH = "emarf";
	public static final int FRAMECOUNT = 7+1;//正常frame的数量+1个frame_none

	private File nameFile; //保存文件的目录
	private File tempFile; //保存文件的临时目录
	private SimpleDateFormat dateFormat;
	// 保存图片路径的集合。
	private ArrayList<EditPhotoInfo> editPhotoInfoArrayList;
	private ArrayList<EditPhotoInfo> tempEditPhotoInfoArrayList = new ArrayList<EditPhotoInfo>(); // 用于后退前进
	private int index = -1; // 索引。   控制图片步骤 前进后退。

	private int lastEditionPosition = 0;

	private boolean isOnlinePic = false;
	private boolean isEncrypted = false;
	private Filter filter;
	private Bitmap newImage; // 滤镜处理过的bitmap

	private int editType = 0; //编辑 类型的类别。 0 默认值，1，代表边框，2，代表 滤镜， 3 ，代表饰品，4 代表字体。

	//有关 文字
	private TextView tvLeft90,tvRight90;  //设置字体，设置颜色

	//	有关相框
	private ImageView frameImageView;

	private int curFramePosition = 0;

	private PictureAirDbManager pictureAirDbManager;
	private LocationUtil locationUtil;

	private static final String TAG = "EditPhotoActivity: ";

	private static final int INIT_DATA_FINISHED = 104;
	private static final int LOAD_IMAGE_FINISH = 103;
	private static final int START_ASYNC = 105;

	private static final int SAVE_PHOTO_DIALOG = 101;

	private boolean loadingFrame = false;

	//绘制 真是图片显示区域。 控制 饰品 与文字拖动范围
	private float leftTopX;
	private float leftTopY;
	private float rightBottomX;
	private float rightBottomY;
	int displayBitmapWidth = 0;
	int displayBitmapHeight = 0;
	//end

	// 记录旋转角度。
	private int rotateAngle;

	Matrix touchMatrix; //纪录图片的 Matrix
	LinkedHashMap<Integer, StickerItem> addItems;

	private PWToast myToast;

	private final Handler editPhotoHandler = new EditPhotoHandler(this);

	private static class EditPhotoHandler extends Handler{
		private final WeakReference<EditPhotoActivity> mActivity;

		public EditPhotoHandler(EditPhotoActivity activity){
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
			case 9999: //加载网络图片。
				GlideUtil.load(this, photoURL, isEncrypted, new SimpleTarget<Bitmap>() {
					@Override
					public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
						mainBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
						mainImage.setImageBitmap(mainBitmap);
						dismissPWProgressDialog();
					}
				});
				break;
			case LOAD_IMAGE_FINISH:
			case INIT_DATA_FINISHED:
				dismissPWProgressDialog();
				break;

			case START_ASYNC:
				ExcuteFilterTask excuteFilterTask = new ExcuteFilterTask();
				excuteFilterTask.execute(mainBitmap);
				break;

			case 1111:
				editType = 1;
				if (msg.arg1 == lastEditionPosition) {//和上次操作一直，不需要进行任何操作
					PictureAirLog.out(TAG + "same with last edition");
					break;

				} else {
					PictureAirLog.out(TAG + "not same with last edition");
					lastEditionPosition = msg.arg1;
				}
				curFramePosition = msg.arg1;
				if (curFramePosition == 0) {//如果裁剪的话，需要还原
					cancelFrameEdition();

				} else {
					// 判断 如果图片是 4:3 就不要去裁减。
					if ((float) mainBitmap.getWidth() / mainBitmap.getHeight() == (float) 4 / 3
							|| (float) mainBitmap.getWidth() / mainBitmap.getHeight() == (float) 3 / 4) {

					} else {
						mainBitmap = EditPhotoUtil.cropBitmap(mainBitmap, 4, 3);
						mainImage.setImageBitmap(mainBitmap);
//					changeMainBitmap(EditPhotoUtil.cropBitmap(mainBitmap, 4, 3));
					}
				}

				btn_onedit_save.setVisibility(curFramePosition != 0 ? View.VISIBLE : View.GONE);
				showPWProgressDialog(R.string.dealing);
				loadframe(curFramePosition);
				break;

			case API1.GET_LAST_CONTENT_SUCCESS://获取更新包成功
				PictureAirLog.d(TAG, "get lastest info success" + msg.obj);
				try {
					com.alibaba.fastjson.JSONObject resultJsonObject = com.alibaba.fastjson.JSONObject.parseObject(msg.obj.toString());
					if (resultJsonObject.containsKey("assets")) {
						pictureAirDbManager.insertFrameAndStickerIntoDB(resultJsonObject.getJSONObject("assets"));
					}

					if (resultJsonObject.containsKey("time")) {
						PictureAirLog.d(TAG, "lastest time is " + resultJsonObject.getString("time"));
						SPUtils.put(this, Common.SHARED_PREFERENCE_APP, Common.GET_LAST_CONTENT_TIME, resultJsonObject.getString("time"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				//写入数据库之后，再从数据库拿数据
				frameFromDBInfos = pictureAirDbManager.getLastContentDataFromDB(1);
				for (int i = 0; i < frameFromDBInfos.size(); i++) {
					if (frameFromDBInfos.get(i).locationId.equals("common")) {//通用边框
						frameInfos.add(frameFromDBInfos.get(i));
					}
				}
				//从数据库获取饰品信息
				stickerFromDBInfos = pictureAirDbManager.getLastContentDataFromDB(0);
				for (int j = 0; j < stickerFromDBInfos.size(); j++) {
					if (stickerFromDBInfos.get(j).locationId.equals("common")) {//通用饰品
						stikerInfos.add(stickerFromDBInfos.get(j));
					}
				}
				break;

			case API1.GET_LAST_CONTENT_FAILED://获取更新包失败

				break;

			default:
				break;
		}
	}

	@Override
	public void onPWDialogButtonClicked(int which, int dialogId) {
		switch (which) {
			case DialogInterface.BUTTON_NEGATIVE:
				if (dialogId == SAVE_PHOTO_DIALOG) {
					finish();
				}
				break;

			case DialogInterface.BUTTON_POSITIVE:
				if (dialogId == SAVE_PHOTO_DIALOG) {
					String url = nameFile + "/" + dateFormat.format(new Date()) + ".jpg";
					EditPhotoUtil.copyFile(editPhotoInfoArrayList.get(index).getPhotoPath(), url);
					scan(url);
				}
				break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_photo);
		initView();
		showPWProgressDialog();
		initData();
		//开始从网络获取最新数据
		API1.getLastContent(SPUtils.getString(this, Common.SHARED_PREFERENCE_APP, Common.GET_LAST_CONTENT_TIME, null), editPhotoHandler);
		myToast = new PWToast(getApplicationContext());
	}

	private void initView(){
		locationUtil = new LocationUtil(this);
		locationItemInfos = new ArrayList<DiscoverLocationItemInfo>();
		mainImage = (ImageView) findViewById(R.id.main_image);
		// 贴图  view
		mStickerView = (StickerView) findViewById(R.id.sticker_panel);

		frameImageView = (ImageView) findViewById(R.id.framephoto_imageView1); // 相框

		back = (ImageView) findViewById(R.id.edit_return);
		edit_accessory = (TextView) findViewById(R.id.edit_accessory);
		titleTextView = (TextView) findViewById(R.id.title_edit);
		edittoolsbar = (LinearLayout) findViewById(R.id.edittoolsbar);
		top_HorizontalListView = (HorizontalListView) findViewById(R.id.horizontalListView);
		preview_save = (TextView) findViewById(R.id.preview_save);
		btn_onedit_save = (ImageButton) findViewById(R.id.btn_onedit_save);
		btn_cancel = (ImageButton) findViewById(R.id.btn_cancel);
		btn_forward = (ImageButton) findViewById(R.id.btn_forward);
		btn_left_back = (ImageView) findViewById(R.id.btn_left_back);
		edit_filter = (TextView) findViewById(R.id.edit_filter);
		edit_text = (TextView) findViewById(R.id.edit_text);
		edit_frame = (TextView) findViewById(R.id.edit_frame);
		font_bar = (LinearLayout) findViewById(R.id.font_bar);
		tvLeft90 = (TextView) findViewById(R.id.tv_left90);
		tvRight90 = (TextView) findViewById(R.id.tv_right90);


		edit_frame.setOnClickListener(this);
		tvLeft90.setOnClickListener(this);
		tvRight90.setOnClickListener(this);
		edit_text.setOnClickListener(this);
		edit_filter.setOnClickListener(this);
		btn_forward.setOnClickListener(this);
		btn_cancel.setOnClickListener(this);
		preview_save.setOnClickListener(this);
		btn_onedit_save.setOnClickListener(this);
		btn_left_back.setOnClickListener(this);
		edit_accessory.setOnClickListener(this);
		back.setOnClickListener(this);
	}

	private void initData(){
		rotateAngle = 0;
		nameFile = new File(Common.PHOTO_SAVE_PATH);

		if (!nameFile.isDirectory()) {
			nameFile.mkdirs();// 创建根目录文件夹
		}
		EditPhotoUtil.deleteTempPic(Common.TEMPPIC_PATH); //每次进入清空temp文件夹。

		tempFile = new File(Common.TEMPPIC_PATH);
		if (!tempFile.isDirectory()) {
			tempFile.mkdirs();// 创建根目录文件夹
		}
		pictureAirDbManager = new PictureAirDbManager(this);

		addFilterImages(FILTERPATH);
		addFrameImages(FRAMEPATH);
		addStickerImages(STICKERPATH); //获取资源文件的  饰品   加载饰品资源

		dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");

		editPhotoInfoArrayList = new ArrayList<EditPhotoInfo>();

		imageWidth = 900;
		imageHeight = 1200;

		photoInfo = getIntent().getParcelableExtra("photo");
		if (photoInfo.onLine == 1) {
			//网络图片。
			photoURL = photoInfo.photoThumbnail_1024;
			isOnlinePic = true;
			isEncrypted = AppUtil.isEncrypted(photoInfo.isEncrypted);
			loadOnlineImg();
		}else{
			//本地图片
			photoURL = photoInfo.photoPathOrURL;
			isOnlinePic = false;
			loadImage(photoURL, true);
		}
		addEditPhotoInfo(photoURL, 0, 0, null, "", 0);

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (locationItemInfos.size() == 0) {//说明不存在，需要获取所有的location地点信息
			locationItemInfos.addAll(AppUtil.getLocation(getApplicationContext(), ACache.get(getApplicationContext()).getAsString(Common.DISCOVER_LOCATION), false));

			locationUtil.setLocationItemInfos(locationItemInfos, this);
		}
		locationUtil.startLocation();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		locationUtil.stopLocation();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
			case R.id.edit_return:
				finishCurrentActivity();
				break;

			case R.id.btn_left_back://编辑的时候，不适用当前编辑
				leftback();
				break;

			//编辑边框。
			case R.id.edit_frame:
				lastEditionPosition = 0;
				titleTextView.setText(R.string.frames);
				onEditStates();
				eidtAdapter = new EditActivityAdapter(EditPhotoActivity.this,mainBitmap, new ArrayList<String>(),1, frameInfos, editPhotoHandler);
				top_HorizontalListView.setAdapter(eidtAdapter);
				top_HorizontalListView.setOnItemClickListener(null);
				break;

			case R.id.edit_filter:
				lastEditionPosition = 0;
				onEditStates();
				titleTextView.setText(R.string.magicbrush);
				eidtAdapter = new EditActivityAdapter(EditPhotoActivity.this, mainBitmap, filterPathList, 2, new ArrayList<FrameOrStikerInfo>(), editPhotoHandler);
				top_HorizontalListView.setAdapter(eidtAdapter);
				top_HorizontalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
											int position, long arg3) {
						// TODO Auto-generated method stub
						btn_onedit_save.setVisibility(position != 0 ? View.VISIBLE : View.GONE);
						editType = 2;
						if (position == lastEditionPosition) {//和上次操作一直，不需要进行任何操作
							PictureAirLog.out(TAG + "same with last edition");
							return;

						} else {
							PictureAirLog.out(TAG + "not same with last edition");
							lastEditionPosition = position;
						}
						switch (position) {
							case 0:
								filter = new NormalFilter();
								break;
							case 1:
								filter = new LomoFilter();
								break;
							case 2:
								// 流年效果
								filter = new Amaro();
								break;
							case 3:
								// 自然美肤效果
								filter = new BeautifyFilter();
								break;
							case 4:
								// HDR 效果
								filter = new HDRFilter();
								break;
							case 5:
								// 自然美肤效果
								filter = new BlurFilter();
								break;
							case 6:
								// 怀旧效果
								filter = new OldFilter();
								break;
							default:
								break;
						}
						if (photoInfo.onLine == 1) {

							// 1.获取需要显示文件的文件名
							String fileString = AppUtil.getReallyFileName(editPhotoInfoArrayList.get(0).getPhotoPath(), 0);
							// 2、判断文件是否存在sd卡中
							File file = new File(Common.PHOTO_DOWNLOAD_PATH + fileString);
							if (file.exists()) {// 3、如果存在SD卡，则从SD卡获取图片信息
								loadImage(file.toString(), false);
							}else{
								GlideUtil.load(EditPhotoActivity.this, editPhotoInfoArrayList.get(0).getPhotoPath(), new SimpleTarget<Bitmap>() {
									@Override
									public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
										mainBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
										editPhotoHandler.sendEmptyMessage(START_ASYNC);
									}
								});
							}

						}else{
							new Thread() {
								@Override
								public void run() {
									super.run();
									mainBitmap = BitmapUtils.loadImageByPath(editPhotoInfoArrayList.get(0).getPhotoPath(), imageWidth,
											imageHeight);
									editPhotoHandler.sendEmptyMessage(START_ASYNC);
								}
							}.start();
						}
					}
				});
				break;
			case R.id.edit_text:
				editType = 4;
				titleTextView.setText(R.string.rotate);
				onEditStates();
				break;


			case R.id.edit_accessory:
				if(mainBitmap == null || mainBitmap.isRecycled()) {
					return;
				}
				calRec();
				mStickerView.setVisibility(View.VISIBLE); // 事先让视图可见。
				//饰品编辑
				onEditStates();
				titleTextView.setText(R.string.decoration);
				eidtAdapter = new EditActivityAdapter(EditPhotoActivity.this,mainBitmap, new ArrayList<String>(),3, stikerInfos, editPhotoHandler);
				top_HorizontalListView.setAdapter(eidtAdapter);
				top_HorizontalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
											int position, long arg3) {
						// TODO Auto-generated method stub
						btn_onedit_save.setVisibility(View.VISIBLE);
						editType = 3;
						String stickerUrl = "";
						if (stikerInfos.get(position).onLine == 1) {//网络图片
							stickerUrl = Common.PHOTO_URL + stikerInfos.get(position).frameOriginalPathPortrait;
						}else {
							stickerUrl = stikerInfos.get(position).frameOriginalPathPortrait;
						}
						//ImageLoader 加载
						GlideUtil.load(EditPhotoActivity.this, stickerUrl, new SimpleTarget<Bitmap>() {
							@Override
							public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
								mStickerView.addBitImage(bitmap);
							}
						});
					}
				});
				break;
			case R.id.btn_onedit_save: //保存到临时目录
				if (index == 0){ //只要是从原图开始操作，就清空 editPhotoInfoArrayList
					editPhotoInfoArrayList.clear();
					addEditPhotoInfo(photoURL, 0, 0, null, "", 0);
				}
				if (!AppUtil.checkPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					myToast.setTextAndShow(R.string.permission_storage_message, Common.TOAST_SHORT_TIME);
					break;
				}
				SaveStickersTask task = new SaveStickersTask();
				if (editType == 2) { //滤镜处理过的
					task.execute(newImage);
				}else if(editType == 4 || editType == 1){
					task.execute(mainBitmap);
				}else if (editType == 3){
					touchMatrix = mainImage.getImageMatrix();
					addItems = mStickerView.getBank();
					task.execute(mainBitmap);
				}
				break;

			case R.id.preview_save: //真正的保存按钮。
				final String url = nameFile + "/" + dateFormat.format(new Date()) + ".jpg";
				if (!AppUtil.checkPermission(getApplicationContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					myToast.setTextAndShow(R.string.permission_storage_message, Common.TOAST_SHORT_TIME);
					break;
				}

				if (index == 0 && isOnlinePic == true){  //如果是网络图片，并且 index ＝ 0 的时候，就没有保存到临时文件目录的文件，故保存Bitmap
					showPWProgressDialog();
					new Thread(new Runnable() {
						@Override
						public void run() {
							EditPhotoUtil.saveBitmap(mainBitmap , url);
							scan(url);
							Looper.prepare();
							dismissPWProgressDialog();
							Looper.loop();
						}
					}).start();
				}else{
					EditPhotoUtil.copyFile(editPhotoInfoArrayList.get(index).getPhotoPath(), url);
					scan(url);
				}

				break;
			case R.id.btn_forward: // 前进按钮。
				if (index == -1) {
					index = editPhotoInfoArrayList.size() - 1;
				}

				if (editPhotoInfoArrayList.size() > index + 1) {
					index++;
					loadImage(editPhotoInfoArrayList.get(index).getPhotoPath(), true);
					tempEditPhotoInfoArrayList.add(editPhotoInfoArrayList.get(editPhotoInfoArrayList.size()-1)); //前进，就加一个编辑对象。
				}
				check();
				break;
			case R.id.btn_cancel: //后退按钮。
				if (index == -1) {
					index = editPhotoInfoArrayList.size() - 1;
				}
				if (index >= 1) {
					index--;
				}

				if (editPhotoInfoArrayList.size() - 2 >= 0) {
					if (index == 0) {
						if (isOnlinePic) {
							loadOnlineImg();
							tempEditPhotoInfoArrayList.remove(editPhotoInfoArrayList.get(editPhotoInfoArrayList.size()-1));
						}else{
							loadImage(editPhotoInfoArrayList.get(index).getPhotoPath(), true);
							tempEditPhotoInfoArrayList.remove(editPhotoInfoArrayList.get(editPhotoInfoArrayList.size()-1));
						}
					}else{
						loadImage(editPhotoInfoArrayList.get(index).getPhotoPath(), true);
						tempEditPhotoInfoArrayList.remove(editPhotoInfoArrayList.get(editPhotoInfoArrayList.size()-1));
					}
				}
				check();
				break;

			case R.id.tv_left90:
				btn_onedit_save.setVisibility(View.VISIBLE);
				mainBitmap = EditPhotoUtil.rotateImage(mainBitmap,-90);
				mainImage.setImageBitmap(mainBitmap);

				rotateAngle = rotateAngle - 90;
				break;

			case R.id.tv_right90:
				btn_onedit_save.setVisibility(View.VISIBLE);
				mainBitmap = EditPhotoUtil.rotateImage(mainBitmap,90);
				mainImage.setImageBitmap(mainBitmap);

				rotateAngle = rotateAngle + 90;
				break;

			default:
				break;
		}
	}


	// 判断 后退 前进按钮的状态
	private void check() {
		if (index == -1) {
			index = editPhotoInfoArrayList.size() - 1;
		}
		if (index == editPhotoInfoArrayList.size() - 1) {
			btn_forward.setImageResource(R.drawable.forward1);
			btn_forward.setClickable(false);
		} else {
			btn_forward.setImageResource(R.drawable.forward);
			btn_forward.setClickable(true);
		}
		if (index == 0) {
			btn_cancel.setImageResource(R.drawable.cancel1);
			btn_cancel.setClickable(false);
			preview_save.setVisibility(View.INVISIBLE);
		} else {
			btn_cancel.setImageResource(R.drawable.cancel);
			btn_cancel.setClickable(true);
			preview_save.setVisibility(View.VISIBLE);
		}
	}

	// 进入编辑某个效果的状态
	private void onEditStates() {
		if(editType == 4){
			font_bar.setVisibility(View.VISIBLE);
			top_HorizontalListView.setVisibility(View.GONE);
		}else{
			top_HorizontalListView.setVisibility(View.VISIBLE);
		}
		titleTextView.setVisibility(View.VISIBLE);
		preview_save.setVisibility(View.GONE);
		btn_forward.setVisibility(View.GONE);
		btn_cancel.setVisibility(View.GONE);
		back.setVisibility(View.GONE);
		btn_left_back.setVisibility(View.VISIBLE);
		edittoolsbar.setVisibility(View.INVISIBLE);
	}

	//退出 编辑状态
	private void exitEditStates(){
		editType = 0;//退出编辑状态，类型初始化。
		titleTextView.setVisibility(View.GONE);
		btn_onedit_save.setVisibility(View.GONE);
		btn_forward.setVisibility(View.VISIBLE);
		btn_cancel.setVisibility(View.VISIBLE);
		back.setVisibility(View.VISIBLE);
		btn_left_back.setVisibility(View.GONE);
		top_HorizontalListView.setVisibility(View.GONE);
		edittoolsbar.setVisibility(View.VISIBLE);
		//字体的编辑条消失。
		font_bar.setVisibility(View.GONE);

	}

	//读取 assets 目录下的图片
	public void addStickerImages(String folderPath) {
		stikerInfos.clear();
		FrameOrStikerInfo frameOrStikerInfo;
		try {
			String[] files =getResources().getAssets()
					.list(folderPath);
			for (String name : files) {
				frameOrStikerInfo = new FrameOrStikerInfo();
				frameOrStikerInfo.frameOriginalPathPortrait = GlideUtil.getAssetUrl(folderPath + File.separator + name);
				frameOrStikerInfo.locationId = "common";
				frameOrStikerInfo.isActive = 1;
				frameOrStikerInfo.onLine = 0;
				stikerInfos.add(frameOrStikerInfo);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//读取 assets 目录下 filter目录的图片
	public void addFilterImages(String folderPath) {
		filterPathList.clear();

		try {
			String[] files =getResources().getAssets().list(folderPath);
			for (String name : files){
				filterPathList.add(GlideUtil.getAssetUrl(folderPath + File.separator + name));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	//读取 assets 目录下frame目录的图片
	public void addFrameImages(String folderPath) {
		frameInfos.clear();

		try {
			String[] files =getResources().getAssets().list(folderPath);
			for (int i=0; i< FRAMECOUNT; i++){
				FrameOrStikerInfo frameInfo = new FrameOrStikerInfo();
				if (i == 0){
					frameInfo.frameThumbnailPathH160 = GlideUtil.getAssetUrl(folderPath + File.separator + files[i]);
					frameInfo.frameThumbnailPathV160 = GlideUtil.getAssetUrl(folderPath + File.separator + files[i]);
					frameInfo.frameOriginalPathLandscape = GlideUtil.getAssetUrl(folderPath + File.separator + files[i]);
					frameInfo.frameOriginalPathPortrait = GlideUtil.getAssetUrl(folderPath + File.separator + files[i]);
				}else{
					int index = (i - 1) * 4;
					frameInfo.frameOriginalPathLandscape = GlideUtil.getAssetUrl(folderPath + File.separator + files[index+1]);
					frameInfo.frameThumbnailPathH160 = GlideUtil.getAssetUrl(folderPath + File.separator + files[index+2]);
					frameInfo.frameOriginalPathPortrait = GlideUtil.getAssetUrl(folderPath + File.separator + files[index+3]);
					frameInfo.frameThumbnailPathV160 = GlideUtil.getAssetUrl(folderPath + File.separator + files[index+4]);
				}
				frameInfos.add(frameInfo);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 异步载入本地编辑图片
	 *
	 * @param filepath
	 */
	public void loadImage(String filepath, boolean isInitLoad) {
//		photoURL = filepath;
		if (mLoadImageTask != null) {
			mLoadImageTask.cancel(true);
		}
		mLoadImageTask = new LoadImageTask(isInitLoad);
		mLoadImageTask.execute(filepath);
	}

	private final class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
		private boolean isInitLoad;
		public LoadImageTask(boolean isInitLoad) {
			this.isInitLoad = isInitLoad;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bitmap = BitmapUtils.loadImageByPath(params[0], imageWidth, imageHeight);
			if(AppUtil.getExifOrientation(params[0])!=0){ // 修改图片显示方向问题。
				bitmap = AppUtil.rotaingImageView(AppUtil.getExifOrientation(params[0]),bitmap);
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (mainBitmap != null) {
				mainBitmap.recycle();
				mainBitmap = null;
				System.gc();
			}
			mainBitmap = result;

			if (isInitLoad) {
				if (mainBitmap != null) {
					mainImage.setImageBitmap(mainBitmap);
				}
				if (null != editPhotoHandler){
					editPhotoHandler.sendEmptyMessage(INIT_DATA_FINISHED);
				}
			} else {
				editPhotoHandler.sendEmptyMessage(START_ASYNC);
			}
		}
	}

	/**
	 * 加载网络图片
	 */
	private void loadOnlineImg(){
		PictureAirLog.out(TAG + "photoURL" + photoURL);
		/*
		 * 需要使用三级缓存，1.判断SD卡是否存在，2.判断缓存中是否存在，3.从网上下载
		 */
		// 1.获取需要显示文件的文件名
		String fileString = AppUtil.getReallyFileName(photoURL,0);
		// 2、判断文件是否存在sd卡中
		File file = new File(Common.PHOTO_DOWNLOAD_PATH + fileString);
		if (file.exists()) {// 3、如果存在SD卡，则从SD卡获取图片信息
			PictureAirLog.out(TAG + "file exists" + fileString);
			loadImage(file.toString(), true);
		}else{
			PictureAirLog.out(TAG + "file not exist");
			editPhotoHandler.sendEmptyMessage(9999); //加载网络图片。
		}
	}

	/**
	 * 切换底图Bitmap
	 *
	 * @param newBit
	 */
	public void changeMainBitmap(Bitmap newBit) {
		if (mainBitmap != null) {
			if (!mainBitmap.isRecycled()) {// 回收
				mainBitmap.recycle();
			}
			mainBitmap = newBit;
		} else {
			mainBitmap = newBit;
		}// end if
		mainImage.setImageBitmap(mainBitmap);
	}

	// 扫描SD卡
	private void scan(final String file) {
		// TODO Auto-generated method stub
		MediaScannerConnection.scanFile(this, new String[] { file }, null,
				new MediaScannerConnection.OnScanCompletedListener() {
					@Override
					public void onScanCompleted(String arg0, Uri arg1) {
						// TODO Auto-generated method stub
						SPUtils.put(EditPhotoActivity.this, Common.SHARED_PREFERENCE_APP, Common.LAST_PHOTO_URL, file);
						// 可以添加一些返回的数据过去，还有扫描最好放在返回去之后。
						Intent intent = new Intent();
						intent.putExtra("photoUrl", file);
						setResult(11, intent);
						PictureAirLog.out("set result--------->");
						finish();
					}
				});
	}


	//保存贴图 滤镜 的异步方法。
	/**
	 * 保存贴图任务
	 *
	 * @author panyi
	 *
	 */

	private final class SaveStickersTask extends
			AsyncTask<Bitmap, Void, Bitmap> {
		@Override
		protected Bitmap doInBackground(Bitmap... params) {
			// System.out.println("保存贴图!");
			if (params[0] == null || params[0].isRecycled()) {
				return null;
			}
			String url = tempFile + "/" + dateFormat.format(new Date()) + ".jpg";
			if (editType == 2) {//滤镜
				EditPhotoUtil.saveBitmap(params[0], url);
//				pathList.add(url);
				addEditPhotoInfo(url, editType, 0, null, "",0);
				index = editPhotoInfoArrayList.size() - 1;
				return params[0];
			}else if(editType == 3){//饰品
				List<StikerInfo> stikerInfoList = new ArrayList<StikerInfo>();

				Bitmap resultBit = Bitmap.createBitmap(params[0]).copy(Bitmap.Config.ARGB_8888, true);
				Canvas canvas = new Canvas(resultBit);
				canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));  //抗锯齿
				float[] data = new float[9];
				touchMatrix.getValues(data);// 底部图片变化记录矩阵原始数据
				Matrix3 cal = new Matrix3(data);// 辅助矩阵计算类
				Matrix3 inverseMatrix = cal.inverseMatrix();// 计算逆矩阵
				Matrix m = new Matrix();
				m.setValues(inverseMatrix.getValues());

				for (Integer id : addItems.keySet()) {
					StickerItem item = addItems.get(id);
					item.matrix.postConcat(m);// 乘以底部图片变化矩阵
					canvas.drawBitmap(item.bitmap, item.matrix, null);
					stikerInfoList.add(new StikerInfo(item.bitmap, item.matrix)); //添加进去
				}// end for
				EditPhotoUtil.saveBitmap(resultBit, url);
//				pathList.add(url);
				addEditPhotoInfo(url, editType, 0, stikerInfoList, "",0);
				index = editPhotoInfoArrayList.size() - 1;
				return resultBit;
			}else if(editType == 4){
				Bitmap resultBit = Bitmap.createBitmap(params[0]).copy(
						Bitmap.Config.ARGB_8888, true);
				EditPhotoUtil.saveBitmap(resultBit, url);
//				pathList.add(url);
				addEditPhotoInfo(url, editType, 0, null, "",rotateAngle);
				rotateAngle = 0; //设置完之后恢复状态。
				index = editPhotoInfoArrayList.size() - 1;
				return resultBit;
			}else if(editType == 1){
				Bitmap mainBitmap = params[0];
				Bitmap heBitmap = Bitmap.createBitmap(mainBitmap.getWidth(), mainBitmap.getHeight(),
						Config.ARGB_8888);
				//不论边框显示与否，都让他合成。   即使是原图。
				Bitmap frameBitmap;
				String loadPhotoUrl;
				if (mainBitmap.getWidth()<mainBitmap.getHeight()) {
					if(frameInfos.get(curFramePosition).onLine == 1){
						loadPhotoUrl = "file://" + getFilesDir().toString() + "/frames/frame_portrait_" + AppUtil.getReallyFileName(frameInfos.get(curFramePosition).frameOriginalPathPortrait,0);
					}else{
						loadPhotoUrl = frameInfos.get(curFramePosition).frameOriginalPathPortrait;
					}
				}else{
					if(frameInfos.get(curFramePosition).onLine == 1){
						loadPhotoUrl = "file://" + getFilesDir().toString() + "/frames/frame_landscape_" + AppUtil.getReallyFileName(frameInfos.get(curFramePosition).frameOriginalPathLandscape,0);
					}else{
						loadPhotoUrl = frameInfos.get(curFramePosition).frameOriginalPathLandscape;
					}
				}

				Canvas canvas = new Canvas(heBitmap);
				Paint point = new Paint();
				point.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_OVER));
				canvas.drawBitmap(mainBitmap, 0, 0, point);

				frameBitmap = GlideUtil.load(EditPhotoActivity.this, loadPhotoUrl, mainBitmap.getWidth(), mainBitmap.getHeight());
				if (frameBitmap != null) {
					Matrix matrix2 = new Matrix();
					matrix2.postScale((float) mainBitmap.getWidth() / (frameBitmap.getWidth()),
							(float) mainBitmap.getHeight() / (frameBitmap.getHeight()));
					frameBitmap = Bitmap.createBitmap(frameBitmap, 0, 0,
							frameBitmap.getWidth(), frameBitmap.getHeight(), matrix2, true);

					canvas.drawBitmap(frameBitmap, 0, 0, point);
					frameBitmap.recycle();
					matrix2.reset();
				}
				EditPhotoUtil.saveBitmap(heBitmap, url);
				addEditPhotoInfo(url, editType, curFramePosition, null, "",0);
				index = editPhotoInfoArrayList.size() - 1;
				return heBitmap;
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissPWProgressDialog();
		}

		@Override
		protected void onCancelled(Bitmap result) {
			super.onCancelled(result);
			dismissPWProgressDialog();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			tempEditPhotoInfoArrayList.clear();
			tempEditPhotoInfoArrayList.addAll(editPhotoInfoArrayList);
			mStickerView.clear();
			frameImageView.setVisibility(View.INVISIBLE);
			changeMainBitmap(result);
			exitEditStates();
			preview_save.setVisibility(View.VISIBLE);
			check();
			dismissPWProgressDialog();

		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showPWProgressDialog(R.string.saving);
		}
	}


	/**
	 * 执行滤镜任务
	 *
	 * @author talon
	 *
	 */
	private final class ExcuteFilterTask extends
			AsyncTask<Bitmap, Void, Bitmap> {
		@Override
		protected Bitmap doInBackground(Bitmap... params) {
			if (params[0] == null || params[0].isRecycled()) {
				return null;
			}
			if (filter instanceof LomoFi) {
				newImage = ((LomoFi) filter).transform(params[0]);
			} else if (filter instanceof EarlyBird) {
				newImage = ((EarlyBird) filter).transform(params[0],
						getResources());
			} else if (filter instanceof Amaro) {
				newImage = ((Amaro) filter).transform(params[0]);
			} else if (filter instanceof NormalFilter) {
				newImage = ((NormalFilter) filter).transform(params[0]);
			} else if (filter instanceof LomoFilter) {
				newImage = ((LomoFilter) filter).transform(params[0]);
			} else if (filter instanceof BeautifyFilter) {
				newImage = ((BeautifyFilter) filter).transform(params[0]);
			} else if (filter instanceof HDRFilter) {
				newImage = ((HDRFilter) filter).transform(params[0]);
			} else if (filter instanceof OldFilter) {
				newImage = ((OldFilter) filter).transform(params[0]);
			} else if (filter instanceof BlurFilter) {
				newImage = ((BlurFilter) filter).transform(params[0]);
			}

			newImage = savaFitlerAfter(newImage); // 滤镜合成之后，再去合成曾经操作过的步骤。
			return newImage;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissPWProgressDialog();
		}

		@Override
		protected void onCancelled(Bitmap result) {
			super.onCancelled(result);
			dismissPWProgressDialog();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (result != null) {
				mainImage.setImageBitmap(result);
			}
			dismissPWProgressDialog();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showPWProgressDialog(R.string.dealing);
		}
	}

	/**
	 * 合成边框方法
	 *  position
	 * @author talon
	 *
	 */
	private void loadframe(int position) {
		if (position != 0) {// 如果不为0，表示有边框
			if (frameInfos.get(position).onLine == 1) {//网络图片，这个时候已经下载，所以直接取本地图片路径
				// 判断宽高，加载不同的边框。加载预览边框
				if (mainBitmap.getWidth() < mainBitmap.getHeight()) {
					GlideUtil.loadWithNoPlaceHolder(this, "file://" + getFilesDir().toString() + "/frames/frame_portrait_" + AppUtil.getReallyFileName(frameInfos.get(position).frameOriginalPathPortrait,0),
							new ImageloaderListener());
				}else{
					GlideUtil.loadWithNoPlaceHolder(this, "file://" + getFilesDir().toString() + "/frames/frame_landscape_" + AppUtil.getReallyFileName(frameInfos.get(position).frameOriginalPathLandscape,0),
							new ImageloaderListener());
				}
			}else {//本地图片
				// 判断宽高，加载不同的边框。加载预览边框
				if (mainBitmap.getWidth() < mainBitmap.getHeight()) {
					GlideUtil.loadWithNoPlaceHolder(this, frameInfos.get(position).frameOriginalPathPortrait, new ImageloaderListener());
				}else{
					GlideUtil.loadWithNoPlaceHolder(this, frameInfos.get(position).frameOriginalPathLandscape, new ImageloaderListener());
				}
			}
		} else {// 没有边框
			dismissPWProgressDialog();
			frameImageView.setVisibility(View.INVISIBLE);
		}
	}

	/*
	 * Imageloader 加载监听类，目的是监听加载完毕的事件
	 */
	private class ImageloaderListener extends SimpleTarget<Bitmap> {

		@Override
		public void onLoadStarted(Drawable drawable) {
		}

		@Override
		public void onLoadFailed(Exception e, Drawable drawable) {
			editPhotoHandler.sendEmptyMessage(LOAD_IMAGE_FINISH);
		}

		@Override
		public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
			editPhotoHandler.sendEmptyMessage(LOAD_IMAGE_FINISH);
			frameImageView.setImageBitmap(bitmap);
			frameImageView.setVisibility(View.VISIBLE);
		}

		@Override
		public void onLoadCleared(Drawable drawable) {
			editPhotoHandler.sendEmptyMessage(LOAD_IMAGE_FINISH);
		}

		@Override
		public void setRequest(Request request) {

		}

		@Override
		public Request getRequest() {
			return null;
		}

		@Override
		public void onStart() {

		}

		@Override
		public void onStop() {
			editPhotoHandler.sendEmptyMessage(LOAD_IMAGE_FINISH);
		}

		@Override
		public void onDestroy() {
			editPhotoHandler.sendEmptyMessage(LOAD_IMAGE_FINISH);
		}
	}

	// 没有保存的时候的对话框
	private void createIsSaveDialog() {
		if (pictureWorksDialog == null) {
			pictureWorksDialog = new PWDialog(this, SAVE_PHOTO_DIALOG)
					.setPWDialogMessage(R.string.exit_hint)
					.setPWDialogNegativeButton(R.string.button_cancel)
					.setPWDialogPositiveButton(R.string.button_ok)
					.setOnPWDialogClickListener(this)
					.pwDialogCreate();
		}
		pictureWorksDialog.pwDilogShow();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		EditPhotoUtil.deleteTempPic(Common.TEMPPIC_PATH);
		if (mainBitmap != null) {
			mainBitmap.recycle();
			mainBitmap = null;
		}
		editPhotoHandler.removeCallbacksAndMessages(null);
	}

	@Override
	public void inOrOutPlace(final String locationIds, final boolean in) {//位置改变之后就要改变边框的内容
		// TODO Auto-generated method stub
		PictureAirLog.d(TAG, "in or out special location..." + locationIds);
		new Thread(){
			public void run() {
				while (!loadingFrame) {//等待边框处理完毕
					if (frameFromDBInfos != null && stickerFromDBInfos != null) {
						loadingFrame = true;
					}
				}
				//1.根据locationIds来判断需要显示或者隐藏的边框
//				frameInfos.addAll(frameFromDBInfos);
				for (int i = 0; i < frameFromDBInfos.size(); i++) {
					PictureAirLog.out("locationIds:"+locationIds+":locationId:"+frameFromDBInfos.get(i).locationId);
					if (locationIds.contains(frameFromDBInfos.get(i).locationId)) {//有属于特定地点的边框
						if (in) {//进入
							frameInfos.add(frameFromDBInfos.get(i));
						}else {//离开
							frameInfos.remove(frameFromDBInfos.get(i));
						}
					}
				}
//				stikerInfos.addAll(stickerFromDBInfos);
				for (int j = 0; j < stickerFromDBInfos.size(); j++) {
					PictureAirLog.out("locationIds:"+locationIds+":locationId:"+stickerFromDBInfos.get(j).locationId);
					if (locationIds.contains(stickerFromDBInfos.get(j).locationId)) {//有属于特定地点的边框
						if (in) {//进入
							stikerInfos.add(stickerFromDBInfos.get(j));
						}else {//离开
							stikerInfos.remove(stickerFromDBInfos.get(j));
						}
					}
				}
			};
		}.start();
	}

	/**
	 * 计算出 图片真正显示的坐标。
	 */
	private void calRec(){
		if (mainBitmap.getHeight() / (float)mainBitmap.getWidth() > mainImage.getHeight() / (float)mainImage.getWidth()) {//左右会留白
			displayBitmapHeight = mainImage.getHeight();//displayBitmapHeight : ? = bitmapReallyHeight : bitmapReallyWidth
			displayBitmapWidth = (int) (displayBitmapHeight * mainBitmap.getWidth() / (float) mainBitmap.getHeight());
		}else {//上下留白
			displayBitmapWidth = mainImage.getWidth();
			displayBitmapHeight = (int) (displayBitmapWidth * mainBitmap.getHeight() / (float)mainBitmap.getWidth());
		}
		leftTopX = (ScreenUtil.getScreenWidth(this) - displayBitmapWidth) / 2;
		rightBottomX = leftTopX + displayBitmapWidth;
		//leftTopY = 图片上边距＋imageview.getY
		//图片上边距 ＝ （imageview的高 － 图片显示在imageview上的高）／ 2
		leftTopY = (mainImage.getHeight() - displayBitmapHeight) / 2;
		rightBottomY = leftTopY + displayBitmapHeight;
		mStickerView.updateCoordinate(leftTopX, leftTopY, rightBottomX, rightBottomY);
	}

	/**
	 * 纪录 编辑的过程
	 * @param photoPath
	 * @param editType
	 * @param framePosition
	 * @param stikerInfoList
	 * @param filterName
	 */
	private void addEditPhotoInfo(String photoPath, int editType, int framePosition, List<StikerInfo> stikerInfoList, String filterName,int rotateAngle){
		EditPhotoInfo editPhotoInfo = new EditPhotoInfo();
		editPhotoInfo.setPhotoPath(photoPath);
		editPhotoInfo.setEditType(editType);
		editPhotoInfo.setFramePosition(framePosition);

		if (stikerInfoList != null){
			editPhotoInfo.setStikerInfoList(stikerInfoList);
		}
		editPhotoInfo.setFilterName(filterName);
		editPhotoInfo.setRotateAngle(rotateAngle);

		editPhotoInfoArrayList.add(editPhotoInfo);
	}


	/**
	 * 保存滤镜之后，添加以前的操作。
	 * 针对 添加滤镜，不对照片与边框有效 的方法。 线程中。
	 */
	private Bitmap savaFitlerAfter(Bitmap bitmap){
		if (tempEditPhotoInfoArrayList.size() == 1){

		}else{
			for (int i = 0; i < tempEditPhotoInfoArrayList.size(); i++){
				if (tempEditPhotoInfoArrayList.get(i).getEditType() == 1){  //为边框时
					//合成边框
					bitmap = saveFrame(bitmap, tempEditPhotoInfoArrayList.get(i).getFramePosition());
				}
				if(tempEditPhotoInfoArrayList.get(i).getEditType() == 3){  // 为饰品 时
					// 合成饰品。
					bitmap = saveStiker(bitmap, tempEditPhotoInfoArrayList.get(i).getStikerInfoList());
				}
				if(tempEditPhotoInfoArrayList.get(i).getEditType() == 4){  // 为饰品 时
					// 旋转图片。
					bitmap = saveRotate(bitmap,tempEditPhotoInfoArrayList.get(i).getRotateAngle());
				}
			}
		}
		return bitmap;
	}

	/**
	 * 保存 边框
	 * @param bitmap
	 * @return
	 */
	private Bitmap saveFrame(Bitmap bitmap, int currentFramePosition){
		Bitmap mainBitmap = bitmap;

		if ((float) mainBitmap.getWidth() / mainBitmap.getHeight() == (float) 4 / 3 || (float) mainBitmap.getWidth() / mainBitmap.getHeight() == (float) 3 / 4) {

		} else {
			mainBitmap = EditPhotoUtil.cropBitmap(mainBitmap, 4, 3);
		}

		Bitmap heBitmap = Bitmap.createBitmap(mainBitmap.getWidth(), mainBitmap.getHeight(), Config.ARGB_8888);
		//不论边框显示与否，都让他合成。   即使是原图。
		Bitmap frameBmp;
		String loadPhotoUrl;
		if (mainBitmap.getWidth() < mainBitmap.getHeight()) {
			if(frameInfos.get(currentFramePosition).onLine == 1){
				loadPhotoUrl = "file://" + getFilesDir().toString() + "/frames/frame_portrait_" + AppUtil.getReallyFileName(frameInfos.get(currentFramePosition).frameOriginalPathPortrait,0);
			}else{
				loadPhotoUrl = frameInfos.get(currentFramePosition).frameOriginalPathPortrait;
			}
		}else{
			if(frameInfos.get(currentFramePosition).onLine == 1){
				loadPhotoUrl = "file://" + getFilesDir().toString() + "/frames/frame_landscape_" + AppUtil.getReallyFileName(frameInfos.get(currentFramePosition).frameOriginalPathLandscape,0);
			}else{
				loadPhotoUrl = frameInfos.get(currentFramePosition).frameOriginalPathLandscape;
			}
		}
		Canvas canvas = new Canvas(heBitmap);
		Paint point = new Paint();
		point.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_OVER));
		canvas.drawBitmap(mainBitmap, 0, 0, point);

		frameBmp = GlideUtil.load(this, loadPhotoUrl, mainBitmap.getWidth(), mainBitmap.getHeight());
		if (frameBmp != null) {
			Matrix matrix2 = new Matrix();
			matrix2.postScale((float) mainBitmap.getWidth() / (frameBmp.getWidth()),
					(float) mainBitmap.getHeight() / (frameBmp.getHeight()));
			frameBmp = Bitmap.createBitmap(frameBmp, 0, 0, frameBmp.getWidth(), frameBmp.getHeight(), matrix2, true);
			canvas.drawBitmap(frameBmp, 0, 0, point);
			frameBmp.recycle();
			matrix2.reset();
		}
		return heBitmap;
	}

	/**
	 * 保存 饰品
	 * @param bitmap
	 * @return
	 */
	private Bitmap saveStiker(Bitmap bitmap, List<StikerInfo> stikerInfoList) {
		Bitmap resultBit = Bitmap.createBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, true));
		for (int i = 0; i < stikerInfoList.size(); i++) {
			Canvas canvas = new Canvas(resultBit);
			canvas.drawBitmap(stikerInfoList.get(i).getStickerBitmap(), stikerInfoList.get(i).getStickerMatrix(), null);
		}
		return resultBit;
	}

	/**
	 * 保存旋转图片
	 * @param bitmap
	 * @param rotateAngle
	 * @return
	 */
	private Bitmap saveRotate(Bitmap bitmap, int rotateAngle) {
		bitmap = EditPhotoUtil.rotateImage(bitmap,rotateAngle);
		return bitmap;
	}

	/**
	 * 监听返回键
	 * @param keyCode
	 * @param event
     * @return
     */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (top_HorizontalListView.isShown() || editType == 4){  //如果进入编辑状态。
				leftback();
			}else {
				finishCurrentActivity();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void finishCurrentActivity(){
		// 判断 是否 需要保存图片
		if (tempFile != null && tempFile.exists() && tempFile.isDirectory() && tempFile.list().length > 0 && index != 0) {
			// 提示是否需要保存图片。
			createIsSaveDialog();
		} else {
			finish();
		}
	}

	private void cancelFrameEdition() {
		//恢复到没有裁减的状态。
		if (editPhotoInfoArrayList.size() == 1 || index == 0){ //代表最初的图片。
			if (photoInfo.onLine == 1) {
				loadOnlineImg();
			}else{
				loadImage(photoURL, true);
			}
		}else{ // 如果 pathList不仅仅存在 一个。说明本地都存在。 恢复到前一个
			loadImage(editPhotoInfoArrayList.get(index).getPhotoPath(), true);
		}
	}

	/**
	 * 退出编辑状态。
	 */
	private void leftback(){
		//如果添加了边框。让边框消失。
		if (editType == 1) {
			if (frameImageView.isShown()) {
				frameImageView.setVisibility(View.INVISIBLE);
			}
			cancelFrameEdition();
		}

		//如果有饰品，饰品消失。
		if (editType == 3) {
			if (mStickerView.isShown()) {
				mStickerView.setVisibility(View.GONE);
				mStickerView.clear();
			}
		}

		//如果添加了滤镜。
		if(editType == 2){
			if (newImage!=null) {
				newImage = null;
//					newImage.recycle();
			}
//					mainImage.setImageBitmap(mainBitmap);
			if (editPhotoInfoArrayList.size() == 1){ //代表最初的图片。
				if (photoInfo.onLine == 1) {
					loadOnlineImg();
				}else{
					loadImage(photoURL, true);
				}
			}else{ // 如果 pathList不仅仅存在 一个。说明本地都存在。 恢复到前一个
				loadImage(editPhotoInfoArrayList.get(editPhotoInfoArrayList.size() - 1).getPhotoPath(), true);
			}
		}

		// 如果旋转了
		if (editType == 4) { // 恢复到原始状态。
			if (editPhotoInfoArrayList.size() == 1){ //代表最初的图片。
				if (photoInfo.onLine == 1) {
					loadOnlineImg();
				}else{
					loadImage(photoURL, true);
				}
			}else{ // 如果 pathList不仅仅存在 一个。说明本地都存在。 恢复到前一个
				loadImage(editPhotoInfoArrayList.get(editPhotoInfoArrayList.size() - 1).getPhotoPath(), true);
			}
		}
		exitEditStates(); // 推出编辑状态
		if(editPhotoInfoArrayList.size() > 1){
			preview_save.setVisibility(View.VISIBLE);
		}
	}

}
