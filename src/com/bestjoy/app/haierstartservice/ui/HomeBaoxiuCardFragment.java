package com.bestjoy.app.haierstartservice.ui;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.haierstartservice.account.HomeObject;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2.TaskType;
import com.bestjoy.app.utils.DebugUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;

public class HomeBaoxiuCardFragment extends SherlockFragment implements OnItemClickListener{
	private static final String TOKEN = HomeBaoxiuCardFragment.class.getName();
	private HomeObject mHomeObject;
	private ListView mListView;
	private CardsAdapter mCardsAdapter;
	private OnBaoxiuCardItemClickListener mOnItemClickListener;
	
	private ContentObserver mContentObserver;
	
	private long mAid = -1, mUid = -1;
	
	public static interface OnBaoxiuCardItemClickListener {
		void onItemClicked(BaoxiuCardObject card) ;
	}
	public void setHomeBaoxiuCard(HomeObject homeObject) {
		mHomeObject = homeObject;
		mAid = homeObject.mHomeAid;
		mUid = homeObject.mHomeUid;
	}
	

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong("aid", mAid);
		outState.putLong("uid", mUid);
		super.onSaveInstanceState(outState);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mAid = savedInstanceState.getLong("aid");
			mUid = savedInstanceState.getLong("uid");
			DebugUtils.logD(TOKEN, "onCreate() savedInstanceState!=null, mAid="+ mAid + ", mUid="+mUid + ",loadCardsAsync()");
			loadCardsAsync();
		}
		PhotoManagerUtilsV2.getInstance().requestToken(TOKEN);
		mContentObserver = new ContentObserver(new Handler()) {
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				loadCardsAsync();
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.home_baoxiucard_fragment, container, false);
		mListView = (ListView) view.findViewById(R.id.listview);
		mCardsAdapter = new CardsAdapter(getActivity(), null, true);
		mListView.setAdapter(mCardsAdapter);
		mListView.setOnItemClickListener(this);
		getActivity().getContentResolver().registerContentObserver(BjnoteContent.BaoxiuCard.CONTENT_URI, true, mContentObserver);
		return view;
	}
	
	public void setOnItemClickListener(OnBaoxiuCardItemClickListener listener) {
		mOnItemClickListener = listener;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (mOnItemClickListener == null) {
			mOnItemClickListener = (MyChooseDevicesActivity)getActivity();
		}
		loadCardsAsync();
	}
	

	@Override
	public void onDestroy() {
		super.onDestroy();
		PhotoManagerUtilsV2.getInstance().releaseToken(TOKEN);
	}


	@Override
	public void onDestroyView() {
		super.onDestroyView();
		AsyncTaskUtils.cancelTask(mLoadCardsTask);
		mCardsAdapter.changeCursor(null);
		getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
	}

	private LoadCardsTask mLoadCardsTask;
	private void loadCardsAsync() {
		AsyncTaskUtils.cancelTask(mLoadCardsTask);
		if (mAid > 0 && mUid > 0) {
			mLoadCardsTask = new LoadCardsTask();
			mLoadCardsTask.execute();
		}
		
	}

	private class LoadCardsTask extends AsyncTask<Void, Void, Cursor> {

		@Override
		protected Cursor doInBackground(Void... params) {
			return BaoxiuCardObject.getAllBaoxiuCardsCursor(getActivity().getContentResolver(), mUid, mAid);
		}

		@Override
		protected void onPostExecute(Cursor result) {
			super.onPostExecute(result);
			mCardsAdapter.changeCursor(result);
		}
		
		
	}
	
	private class CardsAdapter extends CursorAdapter {

		public CardsAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.home_baoxiucard_list_item, parent, false);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			BaoxiuCardObject card = BaoxiuCardObject.getFromBaoxiuCardsCursor(cursor);
			ViewHolder holder = (ViewHolder) view.getTag();
			if (holder == null) {
				holder = new ViewHolder();
				holder._tag = (TextView) view.findViewById(R.id.tag);
				holder._pinpai = (TextView) view.findViewById(R.id.pinpai);
				holder._xinghao = (TextView) view.findViewById(R.id.xinghao);
				holder._title2 = (TextView) view.findViewById(R.id.title2);
				holder._zhengji = (TextView) view.findViewById(R.id.zhengji);
				holder._avator = (ImageView) view.findViewById(R.id.avator);
				holder._component = view.findViewById(R.id.component);
				view.setTag(holder);
			}
			//设置view
			holder._tag.setText(BaoxiuCardObject.getTagName(card.mCardName, card.mLeiXin));
			holder._pinpai.setText(card.mPinPai);
			holder._xinghao.setText(card.mXingHao);
			holder._card = card;
			if (!TextUtils.isEmpty(card.mPKY) && !card.mPKY.equals(BaoxiuCardObject.DEFAULT_BAOXIUCARD_IMAGE_KEY)) {
				PhotoManagerUtilsV2.getInstance().loadPhotoAsync(TOKEN, holder._avator , card.mPKY, null, TaskType.HOME_DEVICE_AVATOR);
			} else {
				//设置默认的ky图片
				holder._avator.setImageResource(R.drawable.ky_default);
			}
			//整机保修
			int validity = card.getBaoxiuValidity();
			if (validity > 0) {
				if (validity > 9999) {
					holder._zhengji.setText(getString(R.string.baoxiucard_validity_toomuch));
				} else {
					holder._zhengji.setText(getString(R.string.baoxiucard_validity, validity));
				}
			} else {
				holder._zhengji.setText(getString(R.string.baoxiucard_outdate));
			}
			
			//主要部件保修
			validity = card.getComponentBaoxiuValidity();
			//目前不显示部件保修
			holder._component.setVisibility(View.INVISIBLE);
			if (validity > 0) {
				holder._title2.setText(getString(R.string.baoxiucard_validity, validity));
			} else {
				holder._title2.setText(getString(R.string.baoxiucard_outdate));
			}
			
		}
		
	}
	
	private static final class ViewHolder {
		//分别是保修卡名字，品牌， 型号， 部件保修剩余时间， 整机保修剩余时间
		private TextView _tag, _pinpai, _xinghao, _title2, _title3;
		//分别是部件保修和整机保修布局(整个)
		private View _component;
		private TextView _zhengji;
		private ImageView _avator;
		private BaoxiuCardObject _card;
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (mOnItemClickListener != null) {
			//回调主Activity，如果有的话
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			mOnItemClickListener.onItemClicked(viewHolder._card);
		}
		
	}
	
	
	
}
