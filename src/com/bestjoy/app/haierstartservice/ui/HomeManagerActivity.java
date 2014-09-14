package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.HomeObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.utils.DebugUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.SecurityUtils;

public class HomeManagerActivity extends BaseActionbarActivity{

	private static final String TAG = "HomeManagerActivity";
	private ListView mHomeListView;
	private HomeManagerAdapter mHomeManagerAdapter;
	private static boolean mIsEditMode;
	private static HashMap<Long, Boolean> deleteHomeIDList = new HashMap<Long, Boolean>();
	private boolean mIsFirstOnresume = true;
	
	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_manager_main);

		mHomeListView = (ListView) findViewById(R.id.home_listview);
		mHomeManagerAdapter = new HomeManagerAdapter(this);
		mHomeListView.setAdapter(mHomeManagerAdapter);
		mHomeListView.setOnItemClickListener(mHomeManagerAdapter);
		mHomeListView.setAdapter(mHomeManagerAdapter);
	}
	
	public void onResume() {
		super.onResume();
		if (!mIsFirstOnresume) {
			mHomeManagerAdapter.notifyDataSetChanged();
		}
		mIsFirstOnresume = false;
	}

	public static void startActivity(Context context) {
		Intent intent = new Intent(context, HomeManagerActivity.class);
		context.startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem newHomeItem = menu.add(R.string.menu_create, R.string.menu_create, 0, R.string.menu_create);
		MenuItem deleteItem = menu.add(R.string.menu_delete, R.string.menu_delete, 0, R.string.menu_delete);
		MenuItem editItem = menu.add(R.string.menu_edit_for_delete, R.string.menu_edit_for_delete, 0, R.string.menu_edit_for_delete);
		MenuItem doneItem = menu.add(R.string.menu_back, R.string.menu_back, 0, R.string.menu_back);
		deleteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		newHomeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		editItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		doneItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mIsEditMode) {
			menu.findItem(R.string.menu_create).setVisible(false);
			menu.findItem(R.string.menu_edit_for_delete).setVisible(false);
			menu.findItem(R.string.menu_back).setVisible(true);
			menu.findItem(R.string.menu_delete).setVisible(deleteHomeIDList.size() > 0);
		} else {
			menu.findItem(R.string.menu_create).setVisible(true);
			menu.findItem(R.string.menu_edit_for_delete).setVisible(true);
			menu.findItem(R.string.menu_back).setVisible(false);
			menu.findItem(R.string.menu_delete).setVisible(false);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.string.menu_create:
			HomeObject.setHomeObject(new HomeObject());
			NewHomeActivity.startActivity(this);
			break;
		case R.string.menu_delete:
			if(deleteHomeIDList.size() <= 0) {
				MyApplication.getInstance().showMessage(R.string.none_select_tips);
			} else {				
				new AlertDialog.Builder(mContext)
				.setMessage(R.string.msg_delete_home_confirm)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doDeleteHomeAsync();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
			}
			break;
		case R.string.menu_edit_for_delete:
			mIsEditMode = true;
			mHomeManagerAdapter.notifyDataSetChanged();
			invalidateOptionsMenu();
			break;
		case R.string.menu_back:
			onMenuDoneClick();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void onMenuDoneClick() {
		mIsEditMode = false;
		deleteHomeIDList.clear();
		mHomeManagerAdapter.notifyDataSetChanged();
		invalidateOptionsMenu();
	}

	DeleteHomeAsyncTask mDeleteHomeAsyncTask;
	private void doDeleteHomeAsync() {
		AsyncTaskUtils.cancelTask(mDeleteHomeAsyncTask);
		showDialog(DIALOG_PROGRESS);
		mDeleteHomeAsyncTask = new DeleteHomeAsyncTask();
		mDeleteHomeAsyncTask.execute();
		
	}
	
	private class DeleteHomeAsyncTask extends AsyncTask<Integer, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Integer... param) {
			boolean deleted = false;
			ContentResolver cr = mContext.getContentResolver();
			long uid = MyAccountManager.getInstance().getAccountObject().mAccountUid;
			for(long aid : deleteHomeIDList.keySet()) {
				deleted = deleteFromService(aid);
				if (deleted) {
					//还要删除本地的家数据
					HomeObject.deleteHomeInDatabaseForAccount(cr, uid, aid);
				}
			}
			deleteHomeIDList.clear();
			//刷新本地家
			MyAccountManager.getInstance().initAccountHomes();
			MyAccountManager.getInstance().updateHomeObject(-1);
			return false;
		}

		/**
		 * 从服务器上删除家数据
		 * @param AID
		 * @return
		 */
		private synchronized boolean deleteFromService(long aid) {
			InputStream is = null;
			final int LENGTH = 2;
			String[] urls = new String[LENGTH];
			String[] paths = new String[LENGTH];
			urls[0] = HaierServiceObject.HOME_DELETE_URL + "AID=";
			paths[0] = String.valueOf(aid);
			urls[1] = "&key=";
			paths[1] = SecurityUtils.MD5.md5(MyAccountManager.getInstance().getAccountObject().mAccountTel + MyAccountManager.getInstance().getAccountObject().mAccountPwd);
			DebugUtils.logD(TAG, "urls = " + Arrays.toString(urls));
			DebugUtils.logD(TAG, "paths = " + Arrays.toString(paths));
			try {
				is = NetworkUtils.openContectionLocked(urls, paths, MyApplication.getInstance().getSecurityKeyValuesObject());
				if (is != null) {
					String content = NetworkUtils.getContentFromInput(is);
					HaierResultObject resultObject = HaierResultObject.parse(content);
					MyApplication.getInstance().showMessageAsync(resultObject.mStatusMessage);
					return resultObject.isOpSuccessfully();
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				NetworkUtils.closeInputStream(is);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			if (mHomeManagerAdapter.getCount() == 0) {
				MainActivity.startActivityForTop(mContext);
			} else {
				mHomeManagerAdapter.notifyDataSetChanged();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if(mIsEditMode) {
				onMenuDoneClick();
				return true;
			}
			break;
		}
		return super.onKeyUp(keyCode, event);
	}

	public class HomeManagerAdapter extends BaseAdapter implements ListView.OnItemClickListener{

		private Context _context;
		private HomeManagerAdapter (Context context) {
			_context = context;
		}
		@Override
		public int getCount() {
			return MyAccountManager.getInstance().getAccountObject().mAccountHomeCount;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(_context).inflate(R.layout.home_list_item, parent, false);
				holder = new ViewHolder();
				holder._name = (TextView) convertView.findViewById(R.id.home_name);
				holder._check = (CheckBox) convertView.findViewById(R.id.home_checkbox);
				holder._home_detail = (TextView) convertView.findViewById(R.id.home_detail);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			if(mIsEditMode) {
				holder._check.setVisibility(View.VISIBLE);
			} else {
				holder._check.setVisibility(View.GONE);
			}
			HomeObject homeObject = MyAccountManager.getInstance().getAccountObject().mAccountHomes.get(position);
			holder._aid = homeObject.mHomeAid;
			Boolean checked = deleteHomeIDList.get(holder._aid);
			if(checked != null && checked) {
				holder._check.setChecked(true);
			} else {
				holder._check.setChecked(false);
			}
			String name = homeObject.mHomeName;
			String nameDtail = homeObject.mHomeProvince + homeObject.mHomeCity + homeObject.mHomeDis;
			if(TextUtils.isEmpty(name)) name = _context.getString(R.string.my_home);
			holder._name.setText(name);
			holder._home_detail.setText(nameDtail);
			return convertView;
		}
		
		private class ViewHolder {
			private CheckBox _check;
			private long _aid;
			private TextView _name, _home_detail;
			private ImageView _flag;
		}

		@Override
		public void onItemClick(AdapterView<?> listView, View view, int pos, long arg3) {
			if(mIsEditMode) {
				ViewHolder viewHolder = (ViewHolder) view.getTag();
				viewHolder._check.setChecked(!viewHolder._check.isChecked());
				deleteHomeIDList.put(viewHolder._aid, viewHolder._check.isChecked());
				invalidateOptionsMenu();
			} else {
				HomeObject.setHomeObject(MyAccountManager.getInstance().getAccountObject().mAccountHomes.get(pos));
				NewHomeActivity.startActivity(mContext);
			}
		}
		
	}
}
