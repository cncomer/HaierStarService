package com.bestjoy.app.haierstartservice.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.haierstartservice.account.HomeObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.utils.DebugUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.Intents;
/**
 * 我的家UI，需要选择设备的调用，都可以直接进入该Activity进行选择
 * @author chenkai
 *
 */
public class MyChooseDevicesActivity extends BaseActionbarActivity implements HomeBaoxiuCardFragment.OnBaoxiuCardItemClickListener {
	/**用来发起选择设备的Action*/
	public static final String ACTION_CHOOSE_DEVICE = "com.bestjoy.app.haierwarrantycard.Intent.ACTION_CHOOSE_DEVICE";
	private static final String TAG = "MyChooseDevicesActivity";
	private ViewPager mViewPager;
	private boolean mIsChooseDevice = false;
	
	private Bundle mBundle = null;
	
	private int mHomeSelected = 0;
	private MyPagerAdapter mMyPagerAdapter;
	
	private ContentObserver mContentObserver;

	@Override
	protected boolean checkIntent(Intent intent) {
		mBundle = intent.getExtras();
		if (mBundle == null) {
			DebugUtils.logD(TAG, "you must pass Bundle object in createChooseDevice()");
			return false;
		}
		return true;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//每次进来我们先重置这个静态成员
		BaoxiuCardObject.setBaoxiuCardObject(null);
		//重置家
		HomeObject.setHomeObject(null);
		DebugUtils.logD(TAG, "onCreate()");
		if (isFinishing()) {
			return ;
		}
		if (ACTION_CHOOSE_DEVICE.equals(getIntent().getAction())) {
			DebugUtils.logD(TAG, "want to choose device");
			mIsChooseDevice = true;
		}
		setContentView(R.layout.activity_choose_devices_main);
		String title = mBundle.getString(Intents.EXTRA_NAME);
		if (!TextUtils.isEmpty(title)) {
			setTitle(title);
		}
		mViewPager = (ViewPager) findViewById(R.id.pagerview);
		mMyPagerAdapter = new MyPagerAdapter(this.getSupportFragmentManager());
		mViewPager.setAdapter(mMyPagerAdapter);
		mViewPager.setOnPageChangeListener(mMyPagerAdapter);
		
		mContentObserver = new ContentObserver(new Handler()) {
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				DebugUtils.logD(TAG, "mContentObserver.onChange()");
				loadHomesAsync();
			}
		};
		//监听Home数据表的变化，一旦变化了，我们重新查询一次家
		getContentResolver().registerContentObserver(BjnoteContent.Homes.CONTENT_URI, true, mContentObserver);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		getContentResolver().unregisterContentObserver(mContentObserver);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//		ModleSettings.createActionBarMenu(menu, mBundle);
		MenuItem homeItem = menu.add(R.string.menu_manage_home, R.string.menu_manage_home, 0, R.string.menu_manage_home);
		homeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.string.menu_manage_home:
			//管理家
	    	//HomeObject.setHomeObject(mMyPagerAdapter.getHome(mHomeSelected).clone());
	    	HomeManagerActivity.startActivity(mContext);
			return true;
		case android.R.id.home:
			finish();
			return true;
		default:
			//当选择了一个Home时候，我们要设置HomeObject对象
			if (mMyPagerAdapter.getCount() > 0) {
				Bundle newBundle = new Bundle();
			    newBundle.putAll(mBundle);
			    newBundle.putLong("aid", mMyPagerAdapter.getHome(mHomeSelected).mHomeAid);
			    newBundle.putLong("uid", MyAccountManager.getInstance().getCurrentAccountId());
//				boolean handle = ModleSettings.onActionBarMenuSelected(item, mContext, newBundle);
//				if (!handle) {
//					return super.onOptionsItemSelected(item);
//				}
			}
		}
		return super.onOptionsItemSelected(item);
		
	}


	private LoadHomesAsyncTask mLoadHomesAsyncTask;
	private void loadHomesAsync() {
		DebugUtils.logD(TAG, "loadHomesAsync()");
		AsyncTaskUtils.cancelTask(mLoadHomesAsyncTask);
		mLoadHomesAsyncTask = new LoadHomesAsyncTask();
		mLoadHomesAsyncTask.execute();
	}
	private class LoadHomesAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			MyAccountManager.getInstance().initAccountHomes();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mMyPagerAdapter.notifyDataSetChanged();
			if (mHomeSelected < mMyPagerAdapter.getCount() && mHomeSelected != mViewPager.getCurrentItem()) {
				mViewPager.setCurrentItem(mHomeSelected);
			}
			
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}
		
	}


	class MyPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {
		
		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
		}
		
		

		@Override
		public CharSequence getPageTitle(int position) {
			return getHome(position).getHomeTag(mContext);
		}
		
		private HomeObject getHome(int position) {
			return MyAccountManager.getInstance().getAccountObject().mAccountHomes.get(position);
		}



		@Override
		public void onPageScrollStateChanged(int arg0) {
			
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			
		}

		@Override
		public void onPageSelected(int arg0) {
			mHomeSelected = arg0;
		}

		@Override
		public Fragment getItem(int position) {
			HomeBaoxiuCardFragment f = new HomeBaoxiuCardFragment();
			f.setOnItemClickListener(MyChooseDevicesActivity.this);
			f.setHomeBaoxiuCard(getHome(position));
			return f;
		}

		@Override
		public int getCount() {
			if (MyAccountManager.getInstance().getAccountObject() == null) {
				return 0;
			}
			return MyAccountManager.getInstance().getAccountObject().mAccountHomes.size();
		}

	}
	
	public static void startIntent(Context context, Bundle bundle) {
		Intent intent = new Intent(context, MyChooseDevicesActivity.class);
		if (bundle != null) {
			intent.putExtras(bundle);
		}
		context.startActivity(intent);
	}
	/**
	 * 发起一个选择设备的Intent,需要发起Activity处理选择后的操作
	 * @param context
	 * @param title
	 * @param type  算泽设备的类型，比如安装、新建保修卡
	 * @return
	 */
	public static Intent createChooseDevice(Context context, Bundle bundel) {
		Intent intent = new Intent(context, MyChooseDevicesActivity.class);
		intent.setAction(ACTION_CHOOSE_DEVICE);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.putExtras(bundel);
		return intent;
	}

	@Override
	public void onItemClicked(final BaoxiuCardObject card) {
	    if (mIsChooseDevice) {
	    	//一些特殊的操作，可以放在这里，目前暂不需要实现
	    }
//	    int id = ModleSettings.getModelIdFromBundle(mBundle);
//	    Bundle newBundle = new Bundle();
//	    newBundle.putAll(mBundle);
//	    newBundle.putLong("aid", card.mAID);
//	    newBundle.putLong("bid", card.mBID);
//	    newBundle.putLong("uid", card.mUID);
//	    if (id == R.id.model_my_card) {
//	    	//进入详细页面
////	    	CardViewActivity.startActivit(mContext, newBundle);
//	    } else {
//	    	//目前只有海尔支持预约安装和预约维修，如果不是，我们需要提示用户
//	    	if (HaierServiceObject.isHaierPinpaiGenaral(card.mPinPai)) {
//			    ModleSettings.doChoose(mContext, newBundle);
//	    	} else {
//	    		new AlertDialog.Builder(mContext)
//		    	.setMessage(R.string.must_haier_confirm_yuyue)
//		    	.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//					
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						if (!TextUtils.isEmpty(card.mBXPhone)) {
//							Intents.callPhone(mContext, card.mBXPhone);
//						} else {
//							MyApplication.getInstance().showMessage(R.string.msg_no_bxphone);
//						}
//						
//					}
//				})
//				.setNegativeButton(android.R.string.cancel, null)
//				.show();
//	    	}
//	    	
//	    	
//	    }
		
	}
	
}
