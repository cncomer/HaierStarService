package com.bestjoy.app.haierstartservice.view;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;

public class HaierPopView implements OnTouchListener, OnClickListener {
	private static final String TAG = "HaierPopView";
	private static final int COLUMN_NUMBER = 4;
	private Context mContext;
	private View popupView;
	private PopupWindow mPopupWindow;
	private GridView gridView;
	private int screenWidth;
	private int screenHeight;
	private AddressAdapter mAddressAdapter;
	private EditText mEditText;
	private Button mChooseButton;
	
	ArrayList<String> resultList = new ArrayList<String>();

	public HaierPopView(Context context, View view, int editTextId, int buttonId) {
		mContext = context;
		//setDataSource(mContext.getResources().getStringArray(R.array.buy_places));
		initViews(view, editTextId, buttonId);
		initData();
	}

	
	private void initViews(View view, int editTextId, int buttonId) {
		mEditText = (EditText) view.findViewById(editTextId);
		//mEditText.setOnTouchListener(this);
		//mEditText.setInputType(InputType.TYPE_NULL);
		mChooseButton = (Button) view.findViewById(buttonId);
		mChooseButton.setOnClickListener(this);
	}

	public Editable getText() {
		return mEditText.getText();
	}

	public void setText(String text) {
		mEditText.setText(text);
	}
	
	private void initData() {
		popupView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.layout_popupwindow, null);
		gridView = (GridView) popupView.findViewById(R.id.gridview);

		final Display display = ((Activity) mContext).getWindow().getWindowManager().getDefaultDisplay();
		if (display != null) {
			screenWidth = display.getWidth();
			screenHeight = display.getHeight();
		}
		DisplayMetrics matrics = new DisplayMetrics();
		display.getMetrics(matrics);
		int size = screenWidth > screenHeight ? screenWidth : screenHeight;
		int padding = (int) (matrics.density * 10);
		gridView.setHorizontalSpacing((int) (matrics.density * 5));
		gridView.setPadding(padding, padding, padding, padding);
		gridView.setVerticalSpacing((int) (matrics.density * 5));
		gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		gridView.setNumColumns(GridView.AUTO_FIT);
		gridView.setColumnWidth(((int) (size * 0.15)));
		gridView.setOnItemClickListener(gridItemClickListener);
		mAddressAdapter = new AddressAdapter();
		gridView.setAdapter(mAddressAdapter);
	}

	public void setDataSource(String[] strings) {
		for(String str : strings) {
			resultList.add(str);
		}
	}
	private void initPopWindow(View view) {
		MyApplication.getInstance().hideInputMethod(mEditText.getWindowToken());
		if (mPopupWindow == null) {
			mPopupWindow = new PopupWindow(popupView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true);
			mPopupWindow.setAnimationStyle(R.style.AnimationPreview);  
			mPopupWindow.setTouchable(true);
			mPopupWindow.setOutsideTouchable(true);
			mPopupWindow.setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), (Bitmap) null));
		}
		mAddressAdapter.changeAddressData();
		mPopupWindow.showAsDropDown(view, 0, 0);
	}

	private OnItemClickListener gridItemClickListener = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
			if (position < resultList.size()) {
				mEditText.setText(resultList.get(position));
			}
			mPopupWindow.dismiss();
		}
	};
	class AddressAdapter extends BaseAdapter {
		LayoutInflater mInflater = null;

		public AddressAdapter() {
			mInflater = LayoutInflater.from(mContext);
		}
		public void changeAddressData() {
			notifyDataSetChanged();
		}
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder = null;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.grid_item, null);
				viewHolder = new ViewHolder();
				viewHolder._title = (TextView) convertView;
				viewHolder._title.setGravity(Gravity.CENTER_HORIZONTAL);
				convertView .setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			if(position < resultList.size()) {
				viewHolder._title.setText(resultList.get(position));
			}
			return convertView;
		}

		public int getCount() {
			return resultList != null ? resultList.size() : 0;
		}

		public Object getItem(int position) {
			return resultList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
	}
	
	private static class ViewHolder {
		private TextView _title;
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if(view.getId() == mEditText.getId()) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_UP:
				initPopWindow(view);
				break;
			}
		}
		return false;
	}


	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.menu_choose_tujing:
			initPopWindow(view);
			break;
		case R.id.menu_choose_yanbao:
			initPopWindow(view);
			break;
		}
	}

}
