package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.app.PendingIntent.CanceledException;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.pdf.PdfDocument.PageInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.shwy.bestjoy.utils.AdapterWrapper;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.Query;

public abstract class PullToRefreshListWithoutPageActivity extends BaseActionbarActivity implements AdapterView.OnItemClickListener{

	private static final String TAG ="PullToRefreshListWithoutPageActivity";
	protected ListView mListView;
	protected TextView mEmptyView;
	private Query mQuery;
	
	protected AdapterWrapper<? extends BaseAdapter> mAdapterWrapper;
	private ContentResolver mContentResolver;
	
	private PullToRefreshListView mPullRefreshListView;
	
	private boolean mFirstinit= false;
	private boolean mDestroyed = false;
	
	private View mLoadMoreFootView;
	
	private long mLastRefreshTime;
	/**如果导航回该界面，从上次刷新以来已经10分钟了，那么自动开始刷新*/
	private static final int MAX_REFRESH_TIME = 1000 * 60 * 10;
	
	private ProgressBar mFooterViewProgressBar;
	private TextView mFooterViewStatusText;
	private boolean mIsUpdate = false;
	
	private boolean isNeedRequestAgain = true;
	
	//子类必须实现的方法
	/**提供一个CursorAdapter类的包装对象*/
	protected abstract AdapterWrapper<? extends BaseAdapter> getAdapterWrapper();
	/**检查intent是否包含必须数据，如果没有将finish自身*/
//	protected abstract boolean checkIntent(Intent intent);
	/**返回本地的Cursor*/
	protected abstract Cursor loadLocal(ContentResolver contentResolver);
	protected abstract int savedIntoDatabase(ContentResolver contentResolver, List<? extends InfoInterface> infoObjects);
	protected abstract List<? extends InfoInterface> getServiceInfoList(InputStream is, PageInfo pageInfo);
	protected abstract com.shwy.bestjoy.utils.Query getQuery();
	protected abstract int getContentLayout();
	/***
	 * 是否一进入该Activity就向服务器查询数据
	 * @return
	 */
	protected boolean isEnterToQueryService() {
		return true;
	}
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DebugUtils.logD(TAG, "onCreate()");
		if (this.isFinishing()) {
			return;
		}
		DebugUtils.logD(TAG, "setContentView()");
		setContentView(getContentLayout());
		
		mContentResolver = getContentResolver();
		
		mPullRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
		mPullRefreshListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
		
		mEmptyView = (TextView) findViewById(android.R.id.empty);
		
		
		mQuery = getQuery();
		
		// Set a listener to be invoked when the list should be refreshed.
		mPullRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
						DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

				// Update the LastUpdatedLabel
				refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
				//重设为0，这样我们可以从头开始更新数据
				isNeedRequestAgain = true;
				// Do work to refresh the list here.
				new QueryServiceTask().execute();
			}
		});
		
		mPullRefreshListView.setScrollingWhileRefreshingEnabled(false);
		
		mListView = mPullRefreshListView.getRefreshableView();
		mAdapterWrapper = getAdapterWrapper();
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapterWrapper.getAdapter());
		mListView.setEmptyView(mEmptyView);
		
		mContext = this;
		mFirstinit = true;
		mLastRefreshTime = System.currentTimeMillis();
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		long resumTime = System.currentTimeMillis();
		if (mFirstinit || resumTime - mLastRefreshTime > MAX_REFRESH_TIME) {
			//第一次进入的时候手动刷新一次
			mPullRefreshListView.setRefreshing();
			 //Do work to refresh the list here.
			new QueryServiceTask().execute();
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mDestroyed = true;
		mAdapterWrapper.releaseAdapter();
	}
	
//	private void addFooterView() {
//		if (mLoadMoreFootView != null) return; 
//		if (mLoadMoreFootView == null) {
//			mLoadMoreFootView = LayoutInflater.from(mContext).inflate(R.layout.load_more_footer, mListView, false);
//			mFooterViewProgressBar = (ProgressBar) mLoadMoreFootView.findViewById(R.id.load_more_progressBar);
//			mFooterViewStatusText = (TextView) mLoadMoreFootView.findViewById(R.id.load_more_text);
//		}
//		mListView.addFooterView(mLoadMoreFootView, true, false);
//	}
//	
//	private void removeFooterView() {
//		if (mLoadMoreFootView != null) {
//			mListView.removeFooterView(mLoadMoreFootView);
//			mLoadMoreFootView = null;
//		}
//	}
//	
//	private void updateFooterView(boolean loading, String status) {
//		if (mLoadMoreFootView == null) {
//			addFooterView();
//		}
//		if (loading) {
//			mFooterViewProgressBar.setVisibility(View.VISIBLE);
//		} else {
//			mFooterViewProgressBar.setVisibility(View.GONE);
//		}
//		if (!TextUtils.isEmpty(status)) mFooterViewStatusText.setText(status);
//	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
		//子类实现具体的动作
	}
	

	/**更新或是新增的总数 >0表示有更新数据，需要刷新，=-1网络问题， =-2 已是最新数据 =0 没有更多数据*/
	private class QueryServiceTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... arg0) {
			
			mIsUpdate = true;
			int insertOrUpdateCount = 0;
			try {
				if (mFirstinit) {
					mFirstinit = false;
					if (!isEnterToQueryService()) {
						throw new CanceledException();
					}
					DebugUtils.logD(TAG, "first load local data....");
					final Cursor cursor = loadLocal(mContentResolver);
					if (cursor != null || cursor.getCount() != 0) {
						int requestCount = cursor.getCount();
						MyApplication.getInstance().postAsync(new Runnable() {

							@Override
							public void run() {
								mAdapterWrapper.changeCursor(cursor);
							}
							
						});
						
						DebugUtils.logD(TAG, "load local data finish....localCount is " + requestCount);
					} else if (cursor != null && cursor.getCount() == 0) {
						cursor.close();
					}
				}
				
//				while (isNeedRequestAgain) {
					DebugUtils.logD(TAG, "openConnection....");
					InputStream is = NetworkUtils.openContectionLocked(mQuery.qServiceUrl, MyApplication.getInstance().getSecurityKeyValuesObject());
					if (is == null) {
						DebugUtils.logD(TAG, "finish task due to openContectionLocked return null InputStream");
						isNeedRequestAgain = false;
						return 0;
					}
					DebugUtils.logD(TAG, "begin parseList....");
					List<? extends InfoInterface> serviceInfoList = getServiceInfoList(is, null);
					int newCount = serviceInfoList.size();
					DebugUtils.logD(TAG, "find date #count = " + newCount);
					
					if (newCount == 0) {
						DebugUtils.logD(TAG, "no more date");
						isNeedRequestAgain = false;
						return 0;
					}
//					if (mPageInfo.mTotalCount <= mPageInfo.mPageIndex * mPageInfo.mPageSize) {
//						DebugUtils.logD(TAG, "returned data count is less than that we requested, so not need to pull data again");
//						isNeedRequestAgain = false;
//					}
					
					DebugUtils.logD(TAG, "begin insert or update local database");
					insertOrUpdateCount = savedIntoDatabase(mContentResolver, serviceInfoList);
					//若果不需要再请求数据了，我们返回更新的结果
//					if (!isNeedRequestAgain || insertOrUpdateCount >= PER_PAGE_SIZE / 2) {
//						return insertOrUpdateCount;
//					}
//					if (isNeedRequestAgain) {
//						mPageInfo.mPageIndex+=1;
//					}
//					mCurrentPageIndex++;
					return insertOrUpdateCount;
//				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return -1;
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			} catch (CanceledException e) {
				e.printStackTrace();
				isNeedRequestAgain = true;
				return 1;
			} finally {
				final Cursor cursor = loadLocal(mContentResolver);
				if (cursor != null || cursor.getCount() != 0) {
					MyApplication.getInstance().postAsync(new Runnable() {

						@Override
						public void run() {
							mAdapterWrapper.changeCursor(cursor);
						}
						
					});
				}
				mLastRefreshTime = System.currentTimeMillis();
			}
		}


		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (mDestroyed) return;
//			showRefreshing(false);
			if (result == -1){
				MyApplication.getInstance().showMessage(R.string.msg_network_error_for_receive);
			} else if (result == -2) {
				MyApplication.getInstance().showMessage(R.string.msg_nonew_for_receive);
			}
			if (!isNeedRequestAgain) {
				MyApplication.getInstance().showMessage(R.string.msg_nomore_for_receive);
			}
			// Call onRefreshComplete when the list has been refreshed.
		    mPullRefreshListView.onRefreshComplete();
//		    mLoadMoreFootView.setVisibility(View.GONE);
		    mIsUpdate = false;
		}
	}
}
