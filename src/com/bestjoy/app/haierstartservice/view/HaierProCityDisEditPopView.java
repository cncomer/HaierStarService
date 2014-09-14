package com.bestjoy.app.haierstartservice.view;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.InputType;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.HomeObject;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.DeviceDBHelper;

public class HaierProCityDisEditPopView implements OnTouchListener {
	private static final String TAG = "HaierProCityDisEditPopView";
	private Context mContext;
	private EditText mProEditView;
	private EditText mCityEditView;
	private EditText mDisEditView;
	private EditText mPlaceDetail;
	private TextView mPlaceDetailTextView;
	private View popupView;
	private PopupWindow mPopupWindow;
	private GridView gridView;
	private int screenWidth;
	private int screenHeight;
	private int mEditMode;
	private String mAdminCode;
	private HomeObject mHomeObject;
	private MyCursorAdapter mAddressAdapter;
	private static final int MODE_PROVINCE = 1;
	private static final int MODE_CITY = MODE_PROVINCE + 1;
	private static final int MODE_DISTRICT = MODE_CITY + 1;
	
//	private HashSet<String> resultSet = new HashSet<String>();
//	ArrayList<String> resultList = new ArrayList<String>();

	private static final String[] PRO_REGION_PROJECTION = new String[]{
		"DISTINCT " + DeviceDBHelper.DEVICE_HAIER_REGION_CODE + " as _id",
		DeviceDBHelper.DEVICE_HAIER_REGION_CODE,
		DeviceDBHelper.DEVICE_HAIER_COUNTRY,        //2
		DeviceDBHelper.DEVICE_HAIER_PROVICE,        //3
		DeviceDBHelper.DEVICE_HAIER_CITY,           //4
		DeviceDBHelper.DEVICE_HAIER_REGION_NAME,    //5
		DeviceDBHelper.DEVICE_HAIER_ADMIN_CODE,	
		
		DeviceDBHelper.DEVICE_HAIER_PRO_CODE,  //pro_code    7 
		DeviceDBHelper.DEVICE_HAIER_CITY_CODE, //city_code   8
		DeviceDBHelper.DEVICE_HAIER_AREA_CODE, //area_code   9
		
	};
	
	private static final String[] CITY_REGION_PROJECTION = new String[]{
		"DISTINCT " + DeviceDBHelper.DEVICE_HAIER_REGION_CODE + " as _id",
		DeviceDBHelper.DEVICE_HAIER_REGION_CODE,
		DeviceDBHelper.DEVICE_HAIER_COUNTRY,        //2
		DeviceDBHelper.DEVICE_HAIER_PROVICE,        //3
		DeviceDBHelper.DEVICE_HAIER_CITY,  //4
		DeviceDBHelper.DEVICE_HAIER_REGION_NAME,    //5
		DeviceDBHelper.DEVICE_HAIER_ADMIN_CODE,
		
		DeviceDBHelper.DEVICE_HAIER_PRO_CODE,  //pro_code    7 
		DeviceDBHelper.DEVICE_HAIER_CITY_CODE, //city_code   8
		DeviceDBHelper.DEVICE_HAIER_AREA_CODE, //area_code   9
		
	};
	
	private static final String[] AREA_REGION_PROJECTION = new String[]{
		DeviceDBHelper.DEVICE_HAIER_REGION_CODE + " as _id",
		DeviceDBHelper.DEVICE_HAIER_REGION_CODE,
		DeviceDBHelper.DEVICE_HAIER_COUNTRY,        //2
		DeviceDBHelper.DEVICE_HAIER_PROVICE,        //3
		DeviceDBHelper.DEVICE_HAIER_CITY,  //4
		DeviceDBHelper.DEVICE_HAIER_REGION_NAME,    //5
		DeviceDBHelper.DEVICE_HAIER_ADMIN_CODE,
		
		DeviceDBHelper.DEVICE_HAIER_PRO_CODE,  //pro_code    7 
		DeviceDBHelper.DEVICE_HAIER_CITY_CODE, //city_code   8
		DeviceDBHelper.DEVICE_HAIER_AREA_CODE, //area_code   9
		
	};
	
	//add by chenkai, 根据省市区一起查找区域码 begin
	private static final int PRO_CODE_INDEX = 7;
	private static final int CITY_CODE_INDEX = 8;
	private static final int AREA_CODE_INDEX = 9;
	private static final int PROVICE_NAME_INDEX = 3;
	private static final int CITY_NAME_INDEX = 4;
	private static final int AREA_NAME_INDEX = 5;
	//add by chenkai, 根据省市区一起查找区域码 end
	public HaierProCityDisEditPopView(Context context, View view) {
		mContext = context;
		mHomeObject = new HomeObject();
		initViews(view);
		initData();
	}
	public HaierProCityDisEditPopView(Context context) {
		mContext = context;
		mHomeObject = new HomeObject();
		initViews(context);
		initData();
	}
	
	public void setOnClickListener(View.OnClickListener listenr) {
		mProEditView.setClickable(true);
		mCityEditView.setClickable(true);
		mDisEditView.setClickable(true);
		mProEditView.setOnClickListener(listenr);
		mCityEditView.setOnClickListener(listenr);
		mDisEditView.setOnClickListener(listenr);
		
	}
	
	private void initData() {
		popupView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.layout_popupwindow, null);
		gridView = (GridView) popupView.findViewById(R.id.gridview);

		final Display display = ((Activity) mContext).getWindow().getWindowManager().getDefaultDisplay();
		if (display != null) {
			screenWidth = display.getWidth();
			screenHeight = display.getHeight();
		}
		
		int size = screenWidth > screenHeight ? screenWidth : screenHeight;
		
		gridView.setHorizontalSpacing(((int) (size * 0.01)));
		gridView.setVerticalSpacing(((int) (size * 0.01)));
		gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		gridView.setNumColumns(GridView.AUTO_FIT);
		gridView.setColumnWidth(((int) (size * 0.15)));
		gridView.setOnItemClickListener(gridItemClickListener);
		mAddressAdapter = new MyCursorAdapter(mContext, null, false);
		gridView.setAdapter(mAddressAdapter);
	}

	public String getProName() {
		return mHomeObject.mHomeProvince;
	}

	public String getCityName() {
		return mHomeObject.mHomeCity;
	}

	public String getDisName() {
		return mHomeObject.mHomeDis;
	}
	
	public String getDetailPlaceName() {
		return mHomeObject.mHomePlaceDetail;
	}
	
	public String getDisID() {
		if(mAdminCode != null) return mAdminCode;
		String pro = mProEditView.getText().toString().trim();
		String city = mCityEditView.getText().toString().trim();
		String dis = mDisEditView.getText().toString().trim();
		String selection = DeviceDBHelper.DEVICE_HAIER_PROVICE + "='" + pro + "' and " + DeviceDBHelper.DEVICE_HAIER_CITY + "='" + city + "' and " + DeviceDBHelper.DEVICE_HAIER_REGION_NAME + "='" + dis + "'";
		Cursor cursor = mContext.getContentResolver().query(
				BjnoteContent.HaierRegion.CONTENT_URI, AREA_REGION_PROJECTION, selection, null, null);
		if(cursor.moveToNext()) {
			mAdminCode = cursor.getString(cursor.getColumnIndex(DeviceDBHelper.DEVICE_HAIER_ADMIN_CODE));
		}
		if (cursor != null) {
			cursor.close();
		}
		
		return mAdminCode;
	}

	private void initViews(Context context) {
		mProEditView = (EditText) ((Activity) context).findViewById(R.id.edit_province);
		mCityEditView = (EditText) ((Activity) context).findViewById(R.id.edit_city);
		mDisEditView = (EditText) ((Activity) context).findViewById(R.id.edit_district);
		mPlaceDetail = (EditText) ((Activity) context).findViewById(R.id.edit_place_detail);
		mPlaceDetailTextView = (TextView) ((Activity) context).findViewById(R.id.edit_place_detail);

		mProEditView.setOnTouchListener(this);
		mCityEditView.setOnTouchListener(this);
		mDisEditView.setOnTouchListener(this);
		mProEditView.setInputType(InputType.TYPE_NULL);
		mCityEditView.setInputType(InputType.TYPE_NULL);
		mDisEditView.setInputType(InputType.TYPE_NULL);
	}
	private void initViews(View view) {
		mProEditView = (EditText) view.findViewById(R.id.edit_province);
		mCityEditView = (EditText) view.findViewById(R.id.edit_city);
		mDisEditView = (EditText) view.findViewById(R.id.edit_district);
		mPlaceDetail = (EditText) view.findViewById(R.id.edit_place_detail);
		mPlaceDetailTextView = (TextView) view.findViewById(R.id.text_place_detail);
		
		mProEditView.setOnTouchListener(this);
		mCityEditView.setOnTouchListener(this);
		mDisEditView.setOnTouchListener(this);
		mProEditView.setInputType(InputType.TYPE_NULL);
		mCityEditView.setInputType(InputType.TYPE_NULL);
		mDisEditView.setInputType(InputType.TYPE_NULL);
		//默认是可编辑的
		setCanEditable(true);
	}

	public void setHomePlaceDetailVisibility(int visibility) {
		mPlaceDetailTextView.setVisibility(visibility);
		mPlaceDetail.setVisibility(visibility);
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (view.getId() == mProEditView.getId()) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mEditMode = MODE_PROVINCE;
					Cursor cursor = mContext.getContentResolver().query(BjnoteContent.HaierRegion.CONTENT_URI, PRO_REGION_PROJECTION, "(" + DeviceDBHelper.DEVICE_HAIER_PROVICE + " IS NOT NULL) GROUP BY (" + DeviceDBHelper.DEVICE_HAIER_PROVICE + ")", null, null);
					mAddressAdapter.changeCursor(cursor);
					break;
				case MotionEvent.ACTION_UP:
					initPopWindow(view);
					break;
			}
		} else if (view.getId() == mCityEditView.getId()) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mEditMode = MODE_CITY;
					if (mHomeObject.mHomeProvince != null) {
						String where = DeviceDBHelper.DEVICE_HAIER_PROVICE + "='" + mHomeObject.mHomeProvince + "'" + " and (" + DeviceDBHelper.DEVICE_HAIER_CITY + " IS NOT NULL) GROUP BY (" + DeviceDBHelper.DEVICE_HAIER_CITY + ")";
						Cursor cursor = mContext.getContentResolver().query(BjnoteContent.HaierRegion.CONTENT_URI, CITY_REGION_PROJECTION, where, null, null);
						mAddressAdapter.changeCursor(cursor);
					}
					break;
				case MotionEvent.ACTION_UP:
					if (mHomeObject.mHomeProvince != null) {
						initPopWindow(view);
					} else {
						Toast.makeText(mContext, R.string.input_province_tips,
								Toast.LENGTH_SHORT).show();
					}
					break;
			}
		} else if (view.getId() == mDisEditView.getId()) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mEditMode = MODE_DISTRICT;
					if (mHomeObject.mHomeCity != null) {
						String where = DeviceDBHelper.DEVICE_HAIER_PROVICE + "='" + mHomeObject.mHomeProvince + "' and " + DeviceDBHelper.DEVICE_HAIER_CITY + "='" + mHomeObject.mHomeCity + "'";
						Cursor cursor = mContext.getContentResolver().query(BjnoteContent.HaierRegion.CONTENT_URI, AREA_REGION_PROJECTION, where, null, DeviceDBHelper.DEVICE_HAIER_REGION_CODE);
						mAddressAdapter.changeCursor(cursor);
					}
					break;
				case MotionEvent.ACTION_UP:
					if (mHomeObject.mHomeCity != null) {
						initPopWindow(view);
					} else {
						Toast.makeText(mContext, R.string.input_city_tips, Toast.LENGTH_SHORT).show();
					}
					break;
			}
		}
		return false;
	}
	
	private void initPopWindow(View view) {
		if (mPopupWindow == null) {
			mPopupWindow = new PopupWindow(popupView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true);
			mPopupWindow.setAnimationStyle(R.style.AnimationPreview);  
			mPopupWindow.setTouchable(true);
			mPopupWindow.setOutsideTouchable(true);
			mPopupWindow.setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), (Bitmap) null));
		}
		mPopupWindow.showAsDropDown(view, 0, 0);
		
	}

	private OnItemClickListener gridItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
			if (position < mAddressAdapter.getCount()) {
				switch (mEditMode) {
				case MODE_PROVINCE:
					mHomeObject.mHomeProvince = mAddressAdapter.getItemTitle(position);
					mProEditView.setText(mHomeObject.mHomeProvince);
					mCityEditView.getText().clear();
					mDisEditView.getText().clear();
					mAdminCode = null;
					mHomeObject.mHomeCity = null;
					break;
				case MODE_CITY:
					mHomeObject.mHomeCity = mAddressAdapter.getItemTitle(position);
					mCityEditView.setText(mHomeObject.mHomeCity);
					mDisEditView.getText().clear();
					mAdminCode = null;
					mHomeObject.mHomeDis = null;
					break;
				case MODE_DISTRICT:
					mHomeObject.mHomeDis = mAddressAdapter.getItemTitle(position);
					mDisEditView.setText(mHomeObject.mHomeDis);
					mAdminCode = mAddressAdapter.getRegionCode(position);
					break;
				
				}
			}
			mPopupWindow.dismiss();
		}
	};
	
	class MyCursorAdapter extends CursorAdapter {
		public MyCursorAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
		}
		
		public String getRegionCode(int position) {
			long id = getItemId(position);
			return String.valueOf(id);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			TextView convertView = (TextView) LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
			convertView.setGravity(Gravity.CENTER_HORIZONTAL);
			return convertView;
		}
		
		public String getItemTitle(int position) {
			Cursor c = (Cursor) getItem(position);
			switch(mEditMode) {
			case MODE_PROVINCE:
				return c.getString(PROVICE_NAME_INDEX);
			case MODE_CITY:
				return c.getString(CITY_NAME_INDEX);
			case MODE_DISTRICT:
				return c.getString(AREA_NAME_INDEX);
			}
			return "Not defined";
			
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			
			((TextView) view).setText(getItemTitle(cursor.getPosition()));
		}
	}
	
	public void setHomeObject(HomeObject homeObject) {
		if(homeObject == null) {
			homeObject = new HomeObject();
		}
		mHomeObject = homeObject;
		updateHomeView();
	}
	
	public void updateHomeView() {
		mProEditView.setText(mHomeObject.mHomeProvince);
		mCityEditView.setText(mHomeObject.mHomeCity);
		mDisEditView.setText(mHomeObject.mHomeDis);
		mPlaceDetail.setText(mHomeObject.mHomePlaceDetail);
	}
	
	public void setCanEditable(boolean canEditable) {
		mProEditView.setEnabled(canEditable);
		mCityEditView.setEnabled(canEditable);
		mDisEditView.setEnabled(canEditable);
	}

	public HomeObject getHomeObject() {
		if(mHomeObject == null) {
			mHomeObject = new HomeObject();
		}
		mHomeObject.mHomeProvince = mProEditView.getText().toString().trim();
		mHomeObject.mHomeCity = mCityEditView.getText().toString().trim();
		mHomeObject.mHomeDis = mDisEditView.getText().toString().trim();
		mHomeObject.mHomePlaceDetail = mPlaceDetail.getText().toString().trim();
		
		return mHomeObject;
	}
	
	public void clear() {
		if (mAddressAdapter != null) {
			mAddressAdapter.changeCursor(null);
			mAddressAdapter = null;
		}
	}
}
