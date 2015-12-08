package com.pictureair.photopass.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.WriterException;
import com.pictureair.photopass.R;
import com.pictureair.photopass.activity.AboutActivity;
import com.pictureair.photopass.activity.BaseFragment;
import com.pictureair.photopass.activity.HelpActivity;
import com.pictureair.photopass.activity.MyPPActivity;
import com.pictureair.photopass.activity.MyPPPActivity;
import com.pictureair.photopass.activity.OrderActivity;
import com.pictureair.photopass.activity.ProfileActivity;
import com.pictureair.photopass.activity.SettingActivity;
import com.pictureair.photopass.selectHeadorBg.SetHeadPhotoAct;
import com.pictureair.photopass.util.AppManager;
import com.pictureair.photopass.util.AppUtil;
import com.pictureair.photopass.util.Common;
import com.pictureair.photopass.util.PictureAirLog;
import com.pictureair.photopass.util.ScreenUtil;
import com.pictureair.photopass.widget.MyToast;

import java.io.File;
/**
 * Camera界面，需要跳转至camera界面
 * @author bauer_bao
 *
 */
public class FragmentPageMe extends BaseFragment implements OnClickListener{
	private static final String TAG = "FragmentPageMe";
	private RelativeLayout rl_myprofile, rl_myorder,rl_pp, rl_ppp, rl_help,rl_setting, rl_about, rl7;
	private ImageView headPhoto, icon2, code_pic;
	private TextView hint2, hint4, name;// hint是条目右边的小标签，根据需要添加信息
	private TextView title1;
	private SharedPreferences sp;
	private MyToast newToast;
	private Context context;
	private Long originalHeadLastModifyTime;
	private File file;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {	
		View view = inflater.inflate(R.layout.fragment_me, null);
		context = getActivity();
		//初始化控件
		sp = context.getSharedPreferences(Common.USERINFO_NAME, Context.MODE_PRIVATE);
		
		newToast = new MyToast(getActivity());
		code_pic = (ImageView) view.findViewById(R.id.code_pic);
		LayoutParams params = code_pic.getLayoutParams();
		params.height =  ScreenUtil.getScreenWidth(getActivity()) / 3;
		code_pic.setLayoutParams(params);
		if (sp.getString(Common.USERINFO_USER_PP, "").isEmpty()) {
			
		}else{
		try {
			code_pic.setImageBitmap(AppUtil.createQRCode(sp.getString(Common.USERINFO_USER_PP, ""), ScreenUtil.getScreenWidth(getActivity()) / 5));
		} catch (WriterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		headPhoto = (ImageView) view.findViewById(R.id.user_photo);
		name = (TextView) view.findViewById(R.id.user_name);
		rl_myprofile = (RelativeLayout) view.findViewById(R.id.rl_myprofile);
		rl_myorder = (RelativeLayout) view.findViewById(R.id.rl_myorder);
		rl_pp = (RelativeLayout) view.findViewById(R.id.rl_pp);
		rl_ppp = (RelativeLayout) view.findViewById(R.id.rl_ppp);
		rl_help = (RelativeLayout) view.findViewById(R.id.rl_help);
		rl_setting = (RelativeLayout) view.findViewById(R.id.rl_setting);
		rl_about = (RelativeLayout) view.findViewById(R.id.rl_about);
		title1 = (TextView) view.findViewById(R.id.title_1);
		headPhoto.setOnClickListener(this);
		rl_myprofile.setOnClickListener(this);
		rl_myorder.setOnClickListener(this);
		rl_pp.setOnClickListener(this);
		rl_ppp.setOnClickListener(this);
		rl_help.setOnClickListener(this);
		rl_setting.setOnClickListener(this);
		rl_about.setOnClickListener(this);
		// 初始化数据
		initData();
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Intent i = new Intent();
		switch (v.getId()) {
//		case R.id.backgroud_img:
//			Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);
//			getAlbum.setType("image/*");
//			startActivityForResult(getAlbum, 1);
//			break;
		case R.id.user_photo:
			// 跳转到图片选择返回一张图片更新头像
			i.setClass(context, SetHeadPhotoAct.class);
			startActivity(i);
//			getActivity().finish();
			break;
		case R.id.rl_myprofile:
			i.setClass(context, ProfileActivity.class);
			startActivity(i);
			break;
		case R.id.rl_myorder:
			// 跳转到我的订单页面
			i.setClass(context, OrderActivity.class);
			startActivity(i);
			break;
		case R.id.rl_pp:
			i.setClass(context, MyPPActivity.class);
			startActivity(i);
			break;
		case R.id.rl_ppp:
			// 跳转到PPP页面
			i.setClass(context, MyPPPActivity.class);
			startActivity(i);
			break;
		case R.id.rl_help:
			i.setClass(context, HelpActivity.class);
			startActivity(i);
			break;
		case R.id.rl_setting:
			i.setClass(context, SettingActivity.class);
			startActivity(i);
			break;
		case R.id.rl_about:
			i.setClass(context, AboutActivity.class);
			startActivity(i);
			break;
		default:
			break;
		}
	}
	
	
//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		// TODO Auto-generated method stub
//		Bitmap bm = null;
//		BufferedOutputStream stream = null;
//		ContentResolver resolver = context.getContentResolver();
//		super.onActivityResult(requestCode, resultCode, data);
////		if (resultCode != RESULT_OK) { // 此处的 RESULT_OK 是系统自定义得一个常量
////			return;
////		}
//		// 此处的用于判断接收的Activity是不是你想要的那个
//		if (requestCode == 1) {
//			try {
//				Uri originalUri = data.getData(); // 获得图片的uri
//				bm = MediaStore.Images.Media.getBitmap(resolver, originalUri);
//				bm = UtilOfDraw.comp(bm);
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//				byte[] datas = baos.toByteArray();
//				File bgPhoto = new File(Common.BGPHOTO_PAHT);
//				bgPhoto.createNewFile();
//				FileOutputStream fstream = new FileOutputStream(bgPhoto);
//				stream = new BufferedOutputStream(fstream);
//				stream.write(datas);
//				// 需要更新服务器中用户背景图片信息
//				StringBuffer sb = new StringBuffer();
//				sb.append(Common.BASE_URL).append(Common.SET_USER_PHOTO);
//				RequestParams params = new RequestParams();
//				String tokenId = sp.getString(Common.USERINFO_TOKENID, null);
//				params.put("file", bgPhoto,"application/octet-stream");
//				params.put("updateType", "coverHeaderImage");
//				params.put(Common.USERINFO_TOKENID, tokenId);
//				API.SetPhoto(sb.toString(), params, handler,0);
//				dialog = ProgressDialog.show(context,  getString(R.string.loading___), getString(R.string.photo_is_uploading), true, true);
//				dialog.show();
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				if (stream != null) {
//					try {
//						stream.close();
//					} catch (IOException e1) {
//						e1.printStackTrace();
//					}
//				}
//			}
//		}
//		initData();
//	}
	
	
	private void initData(){
		String un = sp.getString(Common.USERINFO_NICKNAME, "pictureAir");
		file= new File(Common.USER_PATH + Common.HEADPHOTO_PATH);
		name.setText(un);
		setHeadImage();
	}
	
	private void setHeadImage() {
		// TODO Auto-generated method stub
		if ("".equals(sp.getString(Common.USERINFO_HEADPHOTO, ""))) {//如果user中的头像为空，显示默认头像
			PictureAirLog.v(TAG, "weikong" + sp.getString(Common.USERINFO_HEADPHOTO, ""));
			headPhoto.setImageDrawable(getResources().getDrawable(R.drawable.default_photo));
			originalHeadLastModifyTime = 0L;
		}else {//如果有数据，加载图片
			Bitmap bm1 = null;
			Options options = new Options();
			options.inSampleSize = 2;
			bm1 = BitmapFactory.decodeFile(Common.USER_PATH + Common.HEADPHOTO_PATH, options);
			
			originalHeadLastModifyTime = file.lastModified();
			if (bm1 != null) {
				PictureAirLog.v(TAG, "tupian meiyou");
				headPhoto.setImageBitmap(bm1);
			} else {
				headPhoto.setImageDrawable(getResources().getDrawable(R.drawable.default_photo));
			}
			
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		AppManager.getInstance().killActivity(getActivity());
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (!"".equals(sp.getString(Common.USERINFO_HEADPHOTO, ""))) {
//			if (!file.exists()) {
//				
//			}
			long nowLastModify = file.lastModified();
			if (nowLastModify > originalHeadLastModifyTime) {
				originalHeadLastModifyTime = nowLastModify;
				setHeadImage();
			}
		}
		String un = sp.getString(Common.USERINFO_NICKNAME, "pictureAir");
		name.setText(un);
	}

	@Override
	public void onPause() {
	    super.onPause();
	}
}