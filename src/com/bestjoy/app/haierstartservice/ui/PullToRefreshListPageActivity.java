package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.shwy.bestjoy.utils.AdapterWrapper;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.PageInfo;
import com.shwy.bestjoy.utils.Query;

public abstract class PullToRefreshListPageActivity extends BaseActionbarActivity implements AdapterView.OnItemClickListener{

	private static final String TAG ="ExchangeBusinessCardPullToRefreshActivity";
	protected ListView mListView;
	protected TextView mEmptyView;
	private Query mQuery;
	
	private AdapterWrapper<? extends BaseAdapter> mAdapterWrapper;
	private ContentResolver mContentResolver;
	
	private PullToRefreshListView mPullRefreshListView;
	
	private boolean mFirstinit= false;
	private boolean mDestroyed = false;
	private static final int DEFAULT_PAGEINDEX = 0;
	private static final int PER_PAGE_SIZE = 10;
	private int mCurrentPageIndex = DEFAULT_PAGEINDEX;
	
	private View mLoadMoreFootView;
	
	private long mLastRefreshTime, mLastClickTitleTime;
	/**如果导航回该界面，从上次刷新以来已经10分钟了，那么自动开始刷新*/
	private static final int MAX_REFRESH_TIME = 1000 * 60 * 10;
	
	private ProgressBar mFooterViewProgressBar;
	private TextView mFooterViewStatusText;
	private boolean mIsUpdate = false;
	
	private PageInfo mPageInfo;
	
	private boolean isNeedRequestAgain = true;
	/**如果当前在列表底部了，当有新消息到来的时候我们需要自动滚定到最新的消息处，否则提示下面有新的消息*/
	private boolean mIsAtListBottom = false;
	
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
	protected abstract void onRefreshStart();
	protected abstract void onRefreshEnd();
	protected abstract int getContentLayout();
	
	
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
		if (mQuery == null) {
			DebugUtils.logD(TAG, "getQuery() return null");
		}
		mPageInfo = mQuery.mPageInfo;
		if (mPageInfo == null) {
			mPageInfo = new PageInfo();
		}
		
		// Set a listener to be invoked when the list should be refreshed.
		mPullRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
						DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

				// Update the LastUpdatedLabel
				refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
				//重设为0，这样我们可以从头开始更新数据
//				mCurrentPageIndex = DEFAULT_PAGEINDEX;
				mPageInfo.reset();
				isNeedRequestAgain = true;
//				addFooterView();
//				updateFooterView(false, null);
				int count = mAdapterWrapper.getCount();
				mPageInfo.computePageSize(count);
				// Do work to refresh the list here.
				new QueryServiceTask().execute();
			}
		});
		
		mPullRefreshListView.setScrollingWhileRefreshingEnabled(true);
		
		// Add an end-of-list listener
//		mPullRefreshListView.setOnLastItemVisibleListener(new OnLastItemVisibleListener() {
//
//			@Override
//			public void onLastItemVisible() {
//				DebugUtils.logE(TAG, "End of List!");
////				int pos = mListView.getLastVisiblePosition();
////				View view = mListView.getChildAt(pos);
////				int size = view.getMeasuredHeight() * (mListView.getCount() + 2);
////				if (size >= mListView.getHeight()) {
////					// Do work to refresh the list here.
////					mLoadMoreFootView.setVisibility(View.VISIBLE);
////					updateFooterView(true, null);
////					new QueryServiceTask().execute();
////				}
//				if (!mIsUpdate && mIsAtListBottom) {
////					mLoadMoreFootView.setVisibility(View.VISIBLE);
////					addFooterView();
//					DebugUtils.logExchangeBC(TAG, "we go to load more.");
//					if (isNeedRequestAgain) {
//						updateFooterView(true, null);
//						new QueryServiceTask().execute();
//					} else {
//						DebugUtils.logExchangeBC(TAG, "isNeedRequestAgain is false, we not need to load more");
//					}
//				}
//				
//			}
//		});
		
		mListView = mPullRefreshListView.getRefreshableView();
		mAdapterWrapper = getAdapterWrapper();
		mListView.setOnItemClickListener(this);
		addFooterView();
		updateFooterView(false, null);
		mListView.setAdapter(mAdapterWrapper.getAdapter());
		mListView.setEmptyView(mEmptyView);
		removeFooterView();
		mContext = this;
		mFirstinit = true;
		
		mListView.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && mIsAtListBottom && !mIsUpdate) {
					DebugUtils.logExchangeBC(TAG, "we go to load more.");
					if (isNeedRequestAgain) {
						updateFooterView(true, null);
						new QueryServiceTask().execute();
					} else {
						DebugUtils.logExchangeBC(TAG, "isNeedRequestAgain is false, we not need to load more");
					}
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (totalItemCount > 0) {
					if (firstVisibleItem == 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
						mIsAtListBottom = true;
					} else if (firstVisibleItem > 0 && firstVisibleItem + visibleItemCount < totalItemCount) {
						mIsAtListBottom = false;
					} else {
						mIsAtListBottom = true;
					}
					
				}
				
			}
			
		});
		
	}
	@Override
	public void onResume() {
		super.onResume();
		long resumTime = System.currentTimeMillis();
		if (mFirstinit || resumTime - mLastRefreshTime > MAX_REFRESH_TIME) {
			//第一次进入的时候手动刷新一次
			mPullRefreshListView.setRefreshing();
			mPageInfo.reset();
			addFooterView();
			int count = mAdapterWrapper.getCount();
			mPageInfo.computePageSize(count);
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
		if (mAdapterWrapper != null) mAdapterWrapper.releaseAdapter();
	}
	
	private void addFooterView() {
		if (mLoadMoreFootView != null) return; 
		if (mLoadMoreFootView == null) {
			mLoadMoreFootView = LayoutInflater.from(mContext).inflate(R.layout.load_more_footer, mListView, false);
			mFooterViewProgressBar = (ProgressBar) mLoadMoreFootView.findViewById(R.id.load_more_progressBar);
			mFooterViewStatusText = (TextView) mLoadMoreFootView.findViewById(R.id.load_more_text);
		}
		mListView.addFooterView(mLoadMoreFootView, true, false);
	}
	
	private void removeFooterView() {
		if (mLoadMoreFootView != null) {
			mListView.removeFooterView(mLoadMoreFootView);
			mLoadMoreFootView = null;
		}
	}
	
	private void updateFooterView(boolean loading, String status) {
		if (mLoadMoreFootView == null) {
			addFooterView();
		}
		if (loading) {
			mFooterViewProgressBar.setVisibility(View.VISIBLE);
		} else {
			mFooterViewProgressBar.setVisibility(View.GONE);
		}
		if (!TextUtils.isEmpty(status)) mFooterViewStatusText.setText(status);
	}
	
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
					DebugUtils.logD(TAG, "first load local data....");
					final Cursor cursor = loadLocal(mContentResolver);
					if (cursor != null && cursor.getCount() != 0) {
						int requestCount = cursor.getCount();
						MyApplication.getInstance().postAsync(new Runnable() {

							@Override
							public void run() {
								mAdapterWrapper.changeCursor(cursor);
							}
							
						});
						
						DebugUtils.logD(TAG, "load local data finish....localCount is " + requestCount);
						mPageInfo.computePageSize(requestCount);
					}
				}
				
				if (mPageInfo.mPageIndex == PageInfo.DEFAULT_PAGEINDEX) {
					//开始刷新
					onRefreshStart();
				}
//				while (isNeedRequestAgain) {
					DebugUtils.logD(TAG, "openConnection....");
					DebugUtils.logD(TAG, "start pageIndex " + mPageInfo.mPageIndex + " pageSize = " + mPageInfo.mPageSize);
					InputStream is = NetworkUtils.openContectionLocked(HaierServiceObject.buildPageQuery(mQuery.qServiceUrl, mPageInfo.mPageIndex, mPageInfo.mPageSize), MyApplication.getInstance().getSecurityKeyValuesObject());
					if (is == null) {
						DebugUtils.logD(TAG, "finish task due to openContectionLocked return null InputStream");
						isNeedRequestAgain = false;
						return 0;
					}
					DebugUtils.logD(TAG, "begin parseList....");
					List<? extends InfoInterface> serviceInfoList = getServiceInfoList(is, mPageInfo);
					int newCount = serviceInfoList.size();
					DebugUtils.logD(TAG, "find new date #count = " + newCount + " totalSize = " + mPageInfo.mTotalCount);
					
					if (newCount == 0) {
						DebugUtils.logD(TAG, "no more date");
						isNeedRequestAgain = false;
						return 0;
					}
					if (mPageInfo.mTotalCount <= mPageInfo.mPageIndex * mPageInfo.mPageSize) {
						DebugUtils.logD(TAG, "returned data count is less than that we requested, so not need to pull data again");
						isNeedRequestAgain = false;
					}
					
					DebugUtils.logD(TAG, "begin insert or update local database");
					insertOrUpdateCount = savedIntoDatabase(mContentResolver, serviceInfoList);
					//若果不需要再请求数据了，我们返回更新的结果
//					if (!isNeedRequestAgain || insertOrUpdateCount >= PER_PAGE_SIZE / 2) {
//						return insertOrUpdateCount;
//					}
					if (isNeedRequestAgain) {
						mPageInfo.mPageIndex+=1;
					}
					final Cursor cursor = loadLocal(mContentResolver);
					if (cursor != null && cursor.getCount() != 0) {
						MyApplication.getInstance().postAsync(new Runnable() {

							@Override
							public void run() {
								mAdapterWrapper.changeCursor(cursor);
							}
							
						});
					}
//					mCurrentPageIndex++;
					return insertOrUpdateCount;
//				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				isNeedRequestAgain=false;
				return -1;
			} catch (IOException e) {
				e.printStackTrace();
				isNeedRequestAgain=false;
				return -1;
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
			} else if (result == 0) {
				MyApplication.getInstance().showMessage(R.string.msg_nomore_for_receive);
			}
			if (!isNeedRequestAgain) {
				removeFooterView();
				
			}
			mLastRefreshTime = System.currentTimeMillis();
			// Call onRefreshComplete when the list has been refreshed.
		    mPullRefreshListView.onRefreshComplete();
//		    mLoadMoreFootView.setVisibility(View.GONE);
		    mIsUpdate = false;
		    onRefreshEnd();
		}
	}
	
}
