package com.pictureair.photopass.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FragmentAdapter extends FragmentStatePagerAdapter {
	private List<Fragment> mFragments;
	private List<String> tagList;
	private FragmentManager fragmentManager;

	public FragmentAdapter(FragmentManager fm, List<Fragment> fragments) {
		super(fm);
		fragmentManager = fm;
		mFragments = fragments;
		tagList = new ArrayList<>();
	}

	@Override
	public Fragment getItem(int position) {
//		System.out.println("get item----------in fragmentadatper");
		return mFragments.get(position);
	}

	@Override
	public int getCount() {
		return mFragments.size();
	}
	
	@Override
	public Object instantiateItem(ViewGroup arg0, int arg1) {
		tagList.add(arg0.getId() + "_" + arg1);
		return super.instantiateItem(arg0, arg1);
	}

	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		super.destroyItem(container, position, object);
	}
	
	
	@Override
	public int getItemPosition(Object object) {
		return PagerAdapter.POSITION_NONE;
	}
	
	
	@Override
	public void finishUpdate(ViewGroup container) {
		// TODO Auto-generated method stub
		super.finishUpdate(container);
	}

}