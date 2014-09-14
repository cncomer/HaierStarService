package com.bestjoy.app.haierstartservice.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.haierstartservice.account.XinghaoObject;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.DeviceDBHelper;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NetworkUtils;

public class NewCardChooseFragment extends SherlockFragment implements View.OnClickListener{
	private static final String TAG = "NewCardChooseFragment";
	private ListView mDaleiListViews, mXiaoleiListViews, mPinpaiListViews, mXinghaoListViews;
	
	private TextView mDalei, mXiaolei, mPinpai, mXinghao;
	
	private long mDaleiId = -1, mPinpaiId = -1, mXinghaoId = -1;
	/**当前选中的品牌的code码*/
	private String mPinPaiCode = null;
	/**选择的小类id,是字符型的，注意*/
	private String mXiaoleiId = null;
	
	private BaoxiuCardObject mBaoxiuCardObject;
	
	private static final String[] DALEI_PROJECTION = new String[]{
		HaierDBHelper.ID,
		DeviceDBHelper.DEVICE_DALEI_NAME,                //1
		DeviceDBHelper.DEVICE_DALEI_ID,
	};
	
	private static final String[] XIAOLEI_PROJECTION = new String[]{
		HaierDBHelper.ID,
		DeviceDBHelper.DEVICE_XIALEI_NAME,              //1
		DeviceDBHelper.DEVICE_XIALEI_DID,
		DeviceDBHelper.DEVICE_XIALEI_XID,
		
		
	};
	
	private static final String[] PINPAI_PROJECTION = new String[]{
		HaierDBHelper.ID,
		DeviceDBHelper.DEVICE_PINPAI_NAME,            //1
		DeviceDBHelper.DEVICE_PINPAI_XID,
		DeviceDBHelper.DEVICE_PINPAI_PID,
		DeviceDBHelper.DEVICE_PINPAI_PINYIN,
		DeviceDBHelper.DEVICE_PINPAI_CODE,
		DeviceDBHelper.DEVICE_PINPAI_BXPHONE,       //6
	};
	
	private static final String XIAOLEI_SELECTION = DeviceDBHelper.DEVICE_XIALEI_DID + "=?";
	private static final String PINPAI_SELECTION = DeviceDBHelper.DEVICE_PINPAI_XID + "=?";
	
	private static final String PINPAI_ORDER_BY_SORTID = "sortid asc";

	private View mProgressBarLayout;
	
	
	private List<InfoInterface> mXinghaoDataList;
	
	private MyAdapter mMyAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBaoxiuCardObject = new BaoxiuCardObject();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.new_card_choose_fragment, container, false);
		mProgressBarLayout = view.findViewById(R.id.progressbarLayout);
		
		mDalei = (TextView) view.findViewById(R.id.title_dalei);
		mDalei.setOnClickListener(this);
		TextPaint tp = mDalei.getPaint();
		tp.setFakeBoldText(true);
		
		
		mXiaolei = (TextView) view.findViewById(R.id.title_xiaolei);
		mXiaolei.setOnClickListener(this);
		tp = mXiaolei.getPaint();
		tp.setFakeBoldText(true);
		
		mPinpai = (TextView) view.findViewById(R.id.title_pinpai);
		mPinpai.setOnClickListener(this);
		tp = mPinpai.getPaint();
		tp.setFakeBoldText(true);
		
		mXinghao = (TextView) view.findViewById(R.id.title_xinghao);
		mXinghao.setOnClickListener(this);
		tp = mXinghao.getPaint();
		tp.setFakeBoldText(true);
		
		
		mDaleiListViews = (ListView) view.findViewById(R.id.dalei);
		mXiaoleiListViews = (ListView) view.findViewById(R.id.xiaolei);
		mPinpaiListViews = (ListView) view.findViewById(R.id.pinpai);
		mXinghaoListViews = (ListView) view.findViewById(R.id.xinghao);
		
		initListView(mDaleiListViews);
		initListView(mXiaoleiListViews);
		initListView(mPinpaiListViews);
		
		//型号单独出来
		//modify by chenkai, 增加型号模糊查询, 2014.06.15 begin
		//mXinghaoListViews.setAdapter(new MyAdapter(mXinghaoListViews.getId()));
		mMyAdapter = new MyAdapter(mXinghaoListViews.getId(), null);
		mXinghaoListViews.setAdapter(mMyAdapter);
		//modify by chenkai, 增加型号模糊查询, 2014.06.15 end
		mXinghaoListViews.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mXinghaoListViews.setOnItemClickListener(new ListViewItemSelectedListener(mXinghaoListViews.getId()));
		
		setListViewVisibility(View.GONE, View.GONE, View.GONE, View.GONE);
		
		return view;
	}
	
	private void initListView(ListView listView) {
		listView.setAdapter(new Adapter(getActivity(), listView.getId(), false));
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setOnItemClickListener(new ListViewItemSelectedListener(listView.getId()));
	}
	
	private void releaseAdapter(CursorAdapter adapter) {
		adapter.changeCursor(null);
		adapter = null;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		AsyncTaskUtils.cancelTask(mLoadDataAsyncTask);
		releaseAdapter((CursorAdapter)mDaleiListViews.getAdapter());
		releaseAdapter((CursorAdapter)mXiaoleiListViews.getAdapter());
		releaseAdapter((CursorAdapter)mPinpaiListViews.getAdapter());
		if (mXinghaoDataList != null) mXinghaoDataList.clear();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	public BaoxiuCardObject getBaoxiuCardObject() {
		return mBaoxiuCardObject;
	}
	
	private LoadDataAsyncTask mLoadDataAsyncTask;
	public void loadDataAsync(ListView listview) {
		AsyncTaskUtils.cancelTask(mLoadDataAsyncTask);
		mProgressBarLayout.setVisibility(View.VISIBLE);
		AsyncTaskUtils.cancelTask(mLoadDataAsyncTask);
		mLoadDataAsyncTask = new LoadDataAsyncTask(listview);
		mLoadDataAsyncTask.execute();
	}
	
	private class LoadDataAsyncTask extends AsyncTask<Void, Void, Cursor> {
		private ListView _listView;
		public LoadDataAsyncTask(ListView listView) {
			_listView = listView;
		}

		@Override
		protected Cursor doInBackground(Void... arg0) {
			switch(_listView.getId()) {
			case R.id.dalei:
				return getActivity().getContentResolver().query(BjnoteContent.DaLei.CONTENT_URI, DALEI_PROJECTION, null, null, null);
			case R.id.xiaolei:
				return getActivity().getContentResolver().query(BjnoteContent.XiaoLei.CONTENT_URI, XIAOLEI_PROJECTION, XIAOLEI_SELECTION, new String[]{String.valueOf(mDaleiId)}, null);
			case R.id.pinpai:
				return getActivity().getContentResolver().query(BjnoteContent.PinPai.CONTENT_URI, PINPAI_PROJECTION, PINPAI_SELECTION, new String[]{mXiaoleiId}, PINPAI_ORDER_BY_SORTID);
			case R.id.xinghao:
				//delete by chenkai, 目前不缓存型号到本地 begin
				//return getLocalOrDownload(R.id.xinghao);
				mXinghaoDataList = getServiceDataList(R.id.xinghao);
				return null;
				//delete by chenkai, 目前不缓存型号到本地 end
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			//add by chenkai, 增加型号模糊查询, 2014.06.15 begin
			switch(_listView.getId()) {
			case R.id.xinghao:
				Activity activity = getActivity();
				if (activity != null && activity instanceof NewCardActivity) {
					((NewCardActivity)getActivity()).invalidateOptionsMenu();
				}
				break;
			}
			//add by chenkai, 增加型号模糊查询, 2014.06.15 end
			if (mProgressBarLayout != null) mProgressBarLayout.setVisibility(View.GONE);
		}

		@Override
		protected void onPostExecute(Cursor result) {
			super.onPostExecute(result);
			switch(_listView.getId()) {
			case R.id.dalei:
			case R.id.xiaolei:
			case R.id.pinpai:
				if (result == null || result != null && result.getCount() == 0) {
					MyApplication.getInstance().showMessageAsync(R.string.msg_no_local_data);
				}
				((CursorAdapter) _listView.getAdapter()).changeCursor(result);
				break;
			case R.id.xinghao:
//				if (result == null || result != null && result.getCount() == 0) {
//					MyApplication.getInstance().showMessageAsync(R.string.msg_download_no_xinghao_wait);
//				}
				//modify by chenkai, 增加型号模糊查询, 2014.06.15 begin
				//((BaseAdapter)_listView.getAdapter()).notifyDataSetChanged();
				mMyAdapter.changeData(mXinghaoDataList);
				((NewCardActivity)getActivity()).invalidateOptionsMenu();
				//modify by chenkai, 增加型号模糊查询, 2014.06.15 end
				break;
			}
			
			_listView.setTag(new Object());
			mProgressBarLayout.setVisibility(View.GONE);
		}
		
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.title_dalei:
			setListViewVisibility(mDaleiListViews.getVisibility() == View.VISIBLE ? View.GONE:View.VISIBLE, View.GONE, View.GONE, View.GONE);
			if (mDaleiListViews.getTag() == null) {
				//TAG对象表示的是ListView是否已经加载过数据了，否则我们还需要异步加载
				loadDataAsync(mDaleiListViews);
			}
			break;
		case R.id.title_xiaolei:
			if (mDaleiId != -1) {
				setListViewVisibility(View.GONE, mXiaoleiListViews.getVisibility() == View.VISIBLE ? View.GONE:View.VISIBLE, View.GONE, View.GONE);
				if (mXiaoleiListViews.getTag() == null) {
					loadDataAsync(mXiaoleiListViews);
				}
				
			}
			break;
		case R.id.title_pinpai:
			if (mXiaoleiId != null) {
				setListViewVisibility(View.GONE, View.GONE, mPinpaiListViews.getVisibility() == View.VISIBLE ? View.GONE:View.VISIBLE, View.GONE);
				if (mPinpaiListViews.getTag() == null) {
					loadDataAsync(mPinpaiListViews);
				}
			}
			break;
		case R.id.title_xinghao:
			if (mPinPaiCode != null) {
				setListViewVisibility(View.GONE, View.GONE, View.GONE, mXinghaoListViews.getVisibility() == View.VISIBLE ? View.GONE:View.VISIBLE);
				if (mXinghaoListViews.getTag() == null) {
					loadDataAsync(mXinghaoListViews);
				}
			}
			break;
		}
		
	}
	
	private void setListViewVisibility(int showDalei, int showXiaolei, int showPinpai, int showXinghao) {
		mDaleiListViews.setVisibility(showDalei);
		mXiaoleiListViews.setVisibility(showXiaolei);
		mPinpaiListViews.setVisibility(showPinpai);
		mXinghaoListViews.setVisibility(showXinghao);
	}

	
	private class Adapter extends CursorAdapter {
		private int _listViewId;

		public Adapter(Context context, int listViewId, boolean autoRequery) {
			super(context, null, autoRequery);
			_listViewId = listViewId;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(getActivity()).inflate(R.layout.child_textview, parent, false);
		}
		

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder viewHoldr = (ViewHolder) view.getTag();
			if (view.getTag() == null) {
				viewHoldr = new ViewHolder();
			}
			viewHoldr._title = (TextView) view;
			switch(_listViewId){
			case R.id.dalei:
				viewHoldr._dId = cursor.getLong(2);
				viewHoldr._id = cursor.getLong(0);
				break;
			case R.id.xiaolei:
				viewHoldr._id = cursor.getLong(0);
				viewHoldr._dId = cursor.getLong(2);
				viewHoldr._xId = cursor.getString(3);
				break;
			case R.id.pinpai:
				viewHoldr._id = cursor.getLong(0);
				viewHoldr._xId = cursor.getString(2);
				viewHoldr._pId = cursor.getLong(3);
				viewHoldr._pinpaiCode = cursor.getString(5);
				viewHoldr.mBXphone = cursor.getString(6);
				break;
			case R.id.xinghao:
				viewHoldr._id = cursor.getLong(0);
				viewHoldr._pinpaiCode = cursor.getString(3);
				viewHoldr._mn = cursor.getString(1);
				viewHoldr._ky = cursor.getString(2);
				viewHoldr._wy = cursor.getString(4);
				break;
			}
			viewHoldr._title.setText(cursor.getString(1));
			view.setTag(viewHoldr);
			
		}
		
	}
	
	private class ViewHolder {
		private TextView _title;
		private long _id, _dId, _pId, _xinghaoId;
		private int _position;
		private String _xId, _pinpaiCode, _mn, _ky, mBXphone, _wy;
		
	}
	
	
	private String getGroupTitle(int titleId, String subTitle) {
		StringBuilder sb = new StringBuilder();
		sb.append(getString(titleId)).append(getString(R.string.title_choose_type_divider)).append(subTitle);
		return sb.toString();
	}
	
	private class ListViewItemSelectedListener implements AdapterView.OnItemClickListener{

		private int _listViewId;
		public ListViewItemSelectedListener(int listViewId) {
			_listViewId = listViewId;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			switch(_listViewId) {
			case R.id.dalei:
				if (viewHolder._dId != mDaleiId) {
					mDaleiId = viewHolder._dId;
					mDalei.setText(getGroupTitle(R.string.title_dalei, viewHolder._title.getText().toString()));
					
					mXiaoleiId = null;
					mBaoxiuCardObject.mLeiXin = "";
					mXiaolei.setText(R.string.title_xiaolei);
					
					mPinpaiId = -1;
					mPinPaiCode = null;
					mBaoxiuCardObject.mPinPai = "";
					mBaoxiuCardObject.mBXPhone = "";
					mPinpai.setText(R.string.title_pinpai);
					
					mXinghaoId = -1;
					if (mXinghaoDataList != null) mXinghaoDataList.clear();
					mBaoxiuCardObject.mXingHao = "";
					mBaoxiuCardObject.mWY = "1";
					mBaoxiuCardObject.mKY = "";
					mXinghao.setText(R.string.title_xinghao);
					
					mXiaoleiListViews.setTag(null);
					mPinpaiListViews.setTag(null);
					mXinghaoListViews.setTag(null);
					parent.setVisibility(View.GONE);
					mXiaolei.performClick();
				}
				break;
			case R.id.xiaolei:
				if (viewHolder._xId != mXiaoleiId) {
					mXiaoleiId = viewHolder._xId;
					mBaoxiuCardObject.mLeiXin = viewHolder._title.getText().toString();
					mXiaolei.setText(getGroupTitle(R.string.title_xiaolei, mBaoxiuCardObject.mLeiXin));
					
					mPinpaiId = -1;
					mPinPaiCode = null;
					mBaoxiuCardObject.mPinPai = "";
					mBaoxiuCardObject.mBXPhone = "";
					mPinpai.setText(R.string.title_pinpai);
					
					mXinghaoId = -1;
					if (mXinghaoDataList != null) mXinghaoDataList.clear();
					mBaoxiuCardObject.mXingHao = "";
					mBaoxiuCardObject.mWY = "1";
					mBaoxiuCardObject.mKY = "";
					mXinghao.setText(R.string.title_xinghao);
					
					mPinpaiListViews.setTag(null);
					mXinghaoListViews.setTag(null);
					parent.setVisibility(View.GONE);
					mPinpai.performClick();
				}
				break;
			case R.id.pinpai:
				if (viewHolder._pId != mPinpaiId) {
					mPinpaiId = viewHolder._pId;
					mPinPaiCode = viewHolder._pinpaiCode;
					mBaoxiuCardObject.mPinPai = viewHolder._title.getText().toString();
					mBaoxiuCardObject.mBXPhone = viewHolder.mBXphone;
					
					mPinpai.setText(getGroupTitle(R.string.title_pinpai, mBaoxiuCardObject.mPinPai));
					
					mXinghaoId = -1;
					if (mXinghaoDataList != null) mXinghaoDataList.clear();
					mBaoxiuCardObject.mXingHao = "";
					mBaoxiuCardObject.mWY = "1";
					mBaoxiuCardObject.mKY = "";
					mXinghao.setText(R.string.title_xinghao);
					
					mXinghaoListViews.setTag(null);
					parent.setVisibility(View.GONE);
					//add by chenkai, 增加型号模糊查询, 2014.06.15 begin
					//选择的品牌发生变化时，我们需要清空上一次品牌的型号数据 begin
					if (mXinghaoDataList != null) {
						mXinghaoDataList.clear();
						mMyAdapter.changeData(null);
					}
					//选择的品牌发生变化时，我们需要清空上一次品牌的型号数据 end
					//add by chenkai, 增加型号模糊查询, 2014.06.15 end
					mXinghao.performClick();
				}
				break;
			case R.id.xinghao:
				if (viewHolder._id != mXinghaoId) {
					mXinghaoId = viewHolder._id;
					mBaoxiuCardObject.mXingHao = viewHolder._mn;
					mBaoxiuCardObject.mKY = viewHolder._ky;
					mBaoxiuCardObject.mWY = viewHolder._wy;
					mXinghao.setText(getGroupTitle(R.string.title_xinghao, mBaoxiuCardObject.mXingHao));
				}
				break;
			}
			
		}
		
	}
	
	
	private Cursor getLocalOrDownload(int id) {
		switch(id) {
		case R.id.xinghao:
			//对于型号来说，由于要从服务器上获取，所以，这里的而处理与前三者不同，我们先要判断是否本地已经缓存了，有则直接使用，没有则先获取数据保存导本地再查询出来。
			Cursor c = getActivity().getContentResolver().query(BjnoteContent.XingHao.CONTENT_URI, XinghaoObject.XINGHAO_PROJECTION, XinghaoObject.XINGHAO_CODE_SELECTION, new String[]{mPinPaiCode}, null);
			//TODO 这里可能需要判断即使已经有数据了，也要重新更新型号列表，如新增，目前咱不住处理
			if (c != null) {
				if (c.getCount() > 0) {
					return c;
				} else {
					c.close();
					//下载型号列表
					InputStream is = null;
					try {
						if (!ComConnectivityManager.getInstance().isConnected()) {
							//没有网络连接，提示用户
							MyApplication.getInstance().showMessageAsync(R.string.msg_can_not_access_network);
							return null;
						}
						is = NetworkUtils.openContectionLocked(XinghaoObject.getUpdateUrl(mPinPaiCode), MyApplication.getInstance().getSecurityKeyValuesObject());
						if (is == null) {
							DebugUtils.logD(TAG, "can't open connection " + XinghaoObject.getUpdateUrl(mPinPaiCode));
						} else {
							MyApplication.getInstance().showMessageAsync(R.string.msg_download_xinghao_wait);
							List<InfoInterface> list = XinghaoObject.parse(is, mPinPaiCode);
							if (list.size() > 0) {
								DebugUtils.logD(TAG, "find " + list.size() + " records for pinpaiCode " + mPinPaiCode);
								ContentResolver cr = getActivity().getContentResolver();
								for(InfoInterface object:list) {
									object.saveInDatebase(cr, null);
								}
							}
							return getActivity().getContentResolver().query(BjnoteContent.XingHao.CONTENT_URI, XinghaoObject.XINGHAO_PROJECTION, XinghaoObject.XINGHAO_CODE_SELECTION, new String[]{mPinPaiCode}, null);
						}
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			break;
		}
		return null;
	}
	
	public List<InfoInterface> getServiceDataList(int id) {
		switch(id) {
		case R.id.xinghao:
			DebugUtils.logD(TAG, "getServiceDataList " + XinghaoObject.getUpdateUrl(mPinPaiCode));
			InputStream is = null;
			OutputStream out = null;
			try {
				//查看本地是否有缓存
				File file = MyApplication.getInstance().getCachedXinghaoFile(mPinPaiCode);
				if (file.exists() && file.length() > 0) {
					DebugUtils.logD(TAG, "getCachedXinghaoFile exised " + file.getAbsolutePath());
					is = new FileInputStream(file);
					DebugUtils.logD(TAG, "parse cachedXinghaoFile " + file.getAbsolutePath());
					System.gc();
					return XinghaoObject.parse(is, mPinPaiCode);
				} else {
					//不存在，我们下载型号列表
					if (!ComConnectivityManager.getInstance().isConnected()) {
						//没有网络连接，提示用户
						MyApplication.getInstance().showMessageAsync(R.string.msg_can_not_access_network);
					} else {
						DebugUtils.logD(TAG, "getCachedXinghaoFile not exised " + file.getAbsolutePath());
						is = NetworkUtils.openContectionLocked(XinghaoObject.getUpdateUrl(mPinPaiCode), MyApplication.getInstance().getSecurityKeyValuesObject());
						if (is == null) {
							DebugUtils.logD(TAG, "can't open connection " + XinghaoObject.getUpdateUrl(mPinPaiCode));
							MyApplication.getInstance().showMessageAsync(R.string.msg_download_xinghao_error);
						} else {
							MyApplication.getInstance().showMessageAsync(R.string.msg_download_xinghao_wait);
							out = new FileOutputStream(file);
							byte[] buffer = new byte[4096];
							int read = 0;
							DebugUtils.logD(TAG, "write cachedXinghaoFile " + file.getAbsolutePath());
							while((read = is.read(buffer)) > 0) {
								out.write(buffer, 0, read);
							}
							out.flush();
							NetworkUtils.closeOutStream(out);
							NetworkUtils.closeInputStream(is);
							is = new FileInputStream(file);
							DebugUtils.logD(TAG, "parse cachedXinghaoFile " + file.getAbsolutePath());
							return XinghaoObject.parse(is, mPinPaiCode);
						}
					}
					
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				MyApplication.getInstance().showMessageAsync(R.string.msg_download_xinghao_error);
			} catch (OutOfMemoryError oom) {
				oom.printStackTrace();
				MyApplication.getInstance().showMessageAsync(R.string.msg_download_xinghao_oom_error);
			} catch (IOException e) {
				e.printStackTrace();
				MyApplication.getInstance().showMessageAsync(R.string.msg_download_xinghao_error);
			} finally {
				NetworkUtils.closeInputStream(is);
				NetworkUtils.closeOutStream(out);
			}
			break;
		}
		return new ArrayList<InfoInterface>();
	}
	//modify by chenkai, 增加型号模糊查询, 2014.06.15 begin
	private class MyAdapter extends BaseAdapter {
		private int _listViewId;
		private List _data;

		public MyAdapter(int listViewId, List data) {
			_listViewId = listViewId;
			_data = data;
		}

		@Override
		public int getCount() {
			if (_data == null) {
				return 0;
			} else {
				return _data.size();
			}
		}
		
		public void changeData(List data) {
			_data = data;
			notifyDataSetChanged();
		}

		@Override
		public Object getItem(int position) {
			return _data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position + 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHoldr = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.child_textview, parent, false);
				viewHoldr = new ViewHolder();
				convertView.setTag(viewHoldr);
			} else {
				viewHoldr = (ViewHolder) convertView.getTag();
			}
			viewHoldr._title = (TextView) convertView;
			switch(_listViewId) {
			case R.id.xinghao:
				XinghaoObject object = (XinghaoObject) _data.get(position);
				viewHoldr._id = getItemId(position);
				viewHoldr._pinpaiCode = object.mPinpaiCode;
				viewHoldr._mn = object.mMN;
				viewHoldr._ky = object.mKY;
				viewHoldr._wy = object.mWY;
				viewHoldr._title.setText(object.mMN);
				break;
			}
			
			return convertView;
		}
		
	}
	
	//modify by chenkai, 增加型号模糊查询, 2014.06.15 begin
	
		//add by chenkai, 增加型号模糊查询, 2014.06.15 begin
		public boolean enableFilterXinghao() {
			return !TextUtils.isEmpty(mPinPaiCode) && mXinghaoDataList != null && mXinghaoDataList.size() > 0;
		}
		FilterAsyncTask mFilterAsyncTask = null;
		public void filterXinghao(String filterText) {
			DebugUtils.logD(TAG, "start filterXinghao " + filterText);
			MyAdapter myAdapter = ((MyAdapter)mXinghaoListViews.getAdapter());
			if (TextUtils.isEmpty(filterText)) {
				myAdapter.changeData(mXinghaoDataList);
			} else if (mXinghaoDataList != null && mXinghaoDataList.size() > 0){
				AsyncTaskUtils.cancelTask(mFilterAsyncTask);
				mFilterAsyncTask = new FilterAsyncTask();
				mFilterAsyncTask.execute(filterText);
			}
		}
		
		private class FilterAsyncTask extends AsyncTask<String, Void, List<InfoInterface>> {

			@Override
	        protected List<InfoInterface> doInBackground(String... params) {
				String filter = params[0];
				List<InfoInterface> result = new ArrayList<InfoInterface>(mXinghaoDataList.size());
				for(InfoInterface infoInterface:mXinghaoDataList) {
					if (this.isCancelled()) {
						DebugUtils.logD(TAG, "FilterAsyncTask is canceled by user");
						return result;
					}
					if (infoInterface instanceof XinghaoObject) {
						XinghaoObject object = (XinghaoObject)infoInterface;
						//忽略大小写
						if (object.mMN.toLowerCase().contains(filter.toLowerCase())) {
							result.add(infoInterface);
						}
					}
					
				}
				DebugUtils.logD(TAG, "end filterXinghao " + filter);
		        return result;
	        }

			@Override
	        protected void onPostExecute(List<InfoInterface> result) {
		        super.onPostExecute(result);
		        if (isCancelled()) {
		        	onCancelled();
		        	return;
		        }
		        mMyAdapter.changeData(result);
	        }

			@Override
	        protected void onCancelled() {
		        super.onCancelled();
		        mMyAdapter.changeData(null);
	        }
			
		}
		//add by chenkai, 增加型号模糊查询, 2014.06.15 end
}
