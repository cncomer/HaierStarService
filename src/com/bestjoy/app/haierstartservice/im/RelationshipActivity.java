package com.bestjoy.app.haierstartservice.im;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.service.IMService;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2;
import com.bestjoy.app.haierstartservice.ui.PullToRefreshListPageActivity;
import com.shwy.bestjoy.utils.AdapterWrapper;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.ComPreferencesManager;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NotifyRegistrant;
import com.shwy.bestjoy.utils.PageInfo;
import com.shwy.bestjoy.utils.Query;

public class RelationshipActivity extends PullToRefreshListPageActivity{
	private static final String TAG = "RelationshipActivity";
	public static final String FIRST = "RelationshipActivity.FIRST";
	private Handler mHandler;
	private static final int WHAT_REFRESH_LIST = 1000;
	private RelationshipAdapter mRelationshipAdapter;
	private boolean mIsRefresh = false;
	
	private HashMap<String, Boolean> mChecked = new HashMap<String, Boolean>();
	private HashSet<RelationshipObject> mSelectedRelationshipObjectSet = new HashSet<RelationshipObject>();
	/**是否处于编辑模式，这里是指群聊选择*/
	private boolean mIsInEditable = false;
	private boolean mIsIMServiceRunning = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing()) {
			return;
		}
		setShowHomeUp(true);
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch(msg.what){
				case WHAT_REFRESH_LIST:
					mRelationshipAdapter.refreshList();
					break;
				}
			}
			
		};
		PhotoManagerUtilsV2.getInstance().requestToken(TAG);
		
		initSortLayout();
		checkIMService();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mIsIMServiceRunning = ComConnectivityManager.getInstance().isConnected();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mContext.unbindService(mServiceConnection);
		NotifyRegistrant.getInstance().unRegister(mUiHandler);
	}
	
	private int mSortBtnSelectedTextColor, mSortBtnUnselectedTextColor;
	private Drawable mSortBtnSelectedBackground, mSortBtnUnselectedBackground;
	private Button mSortBtnByTime, mSortBtnByType;
	private int mSortType = -1;
	private Query mQuery;
	private void initSortLayout() {
		mSortBtnSelectedTextColor = this.getResources().getColor(R.color.sort_text_color_selected);
		mSortBtnUnselectedTextColor = this.getResources().getColor(R.color.sort_text_color_unselected);
		mSortBtnSelectedBackground = this.getResources().getDrawable(R.drawable.sort_btn_selected);
		mSortBtnUnselectedBackground = this.getResources().getDrawable(R.drawable.sort_btn_unselected);
		
		mSortBtnByTime = (Button) findViewById(R.id.time);
		mSortBtnByType = (Button) findViewById(R.id.type);
		
		View.OnClickListener clickListener = new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				switch(v.getId()) {
				case R.id.time:
					setOrderTypeTab(RelationshipAdapter.DATA_SORT_BY_TIME);
					forceRefresh();
					break;
				case R.id.type:
					setOrderTypeTab(RelationshipAdapter.DATA_SORT_BY_TYPE);
					forceRefresh();
					break;
				}
			}
			
		};
		mSortBtnByTime.setOnClickListener(clickListener);
		mSortBtnByType.setOnClickListener(clickListener);
		setOrderTypeTab(RelationshipAdapter.DATA_SORT_BY_TIME);
	}
	
	private void setOrderTypeTab(int type) {
		mSortType = type;
		if (mSortType == RelationshipAdapter.DATA_SORT_BY_TIME) {
			mSortBtnByTime.setBackgroundDrawable(mSortBtnSelectedBackground);
			mSortBtnByTime.setTextColor(mSortBtnSelectedTextColor);
			mSortBtnByType.setBackgroundDrawable(mSortBtnUnselectedBackground);
			mSortBtnByType.setTextColor(mSortBtnUnselectedTextColor);
		} else if (mSortType == RelationshipAdapter.DATA_SORT_BY_TYPE) {
			mSortBtnByType.setBackgroundDrawable(mSortBtnSelectedBackground);
			mSortBtnByType.setTextColor(mSortBtnSelectedTextColor);
			mSortBtnByTime.setBackgroundDrawable(mSortBtnUnselectedBackground);
			mSortBtnByTime.setTextColor(mSortBtnUnselectedTextColor);
		}
		mQuery.qServiceUrl = HaierServiceObject.getRelationshipUrl(MyAccountManager.getInstance().getCurrentAccountUid(), MyAccountManager.getInstance().getAccountObject().mAccountPwd, mSortType);
	}
	@Override
	protected boolean isNeedForceRefreshOnResume() {
		boolean first = ComPreferencesManager.getInstance().isFirstLaunch(FIRST, true);
		if (first) {
			ComPreferencesManager.getInstance().setFirstLaunch(FIRST, false);
		}
		return first;
	}
	
	private Handler mUiHandler;
	private IMService mImService;
	private ServiceConnection mServiceConnection;
	private void checkIMService() {
		mIsIMServiceRunning = false;
		mUiHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch(msg.what){
				case IMService.WHAT_SEND_MESSAGE_LOGIN:
				case IMService.WHAT_SEND_MESSAGE_OFFLINE:
				case IMService.WHAT_SEND_MESSAGE_EXIT:
				case IMService.WHAT_SEND_MESSAGE_INVALID_USER:
				case IMService.WHAT_SEND_MESSAGE_NO_NETWORK:
					mIsIMServiceRunning = mImService.isConnected();
					invalidateOptionsMenu();
					break;
				}
			}
		};
		NotifyRegistrant.getInstance().register(mUiHandler);
		mServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mImService = ((IMService.MyBinder)service).getService();
				if (mImService != null && mImService.isConnected()) {
					mIsIMServiceRunning = mImService.isConnected();
				} else {
					mIsIMServiceRunning = false;
				}
				invalidateOptionsMenu();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mIsIMServiceRunning = false;
			}
		};
		Intent intent = new Intent(mContext, IMService.class);
		mContext.bindService(intent,mServiceConnection, Context.BIND_AUTO_CREATE);
		
		IMService.connectIMService(this);
	}
	private void showQunMessageInputDialog() {
		// mIsInEditable = false;
//		 invalidateOptionsMenu();
		int len = mSelectedRelationshipObjectSet.size();
		if (len == 0) {
			MyApplication.getInstance().showMessage(R.string.msg_no_receiver);
			return;
		}
		View view = mContext.getLayoutInflater().inflate(R.layout.qunfa, null, false);
		final EditText input = (EditText) view.findViewById(R.id.feedback);
		final TextView sendto = (TextView) view.findViewById(R.id.sendto);
		StringBuilder sb = new StringBuilder();
		String format = mContext.getResources().getString(R.string.format_send_to_some);
		if (len > 5) {
			format = mContext.getResources().getString(R.string.format_send_to_many);
			len = 5;
		}
		Iterator<RelationshipObject> iterator = mSelectedRelationshipObjectSet.iterator();
		int index = 0;
		while(iterator.hasNext() && index < len) {
			sb.append(iterator.next().mTargetName);
			if (index < len -1) {
				sb.append("、");
			}
			index++;
		}
		sendto.setText(String.format(format, sb.toString(), mSelectedRelationshipObjectSet.size()));
		final Button commit = (Button) view.findViewById(R.id.button_commit);
		final Button cancel = (Button) view.findViewById(R.id.button_cancel);
		final Dialog dialog = new AlertDialog.Builder(mContext)
		.setView(view)
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				resetStatus();
			}
		})
		.setTitle(R.string.title_tip_input_content_for_qunfa)
		.show();
		View.OnClickListener clickListener = new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				switch(v.getId()) {
				case R.id.button_commit:
					String content = input.getText().toString().trim();
					if (!TextUtils.isEmpty(content)) {
						if (mIsIMServiceRunning) {
							sendQunMessageAsync(content);
							dialog.dismiss();
						} else {
							MyApplication.getInstance().showMessage(R.string.msg_offline_no_network);
						}
					}
					break;
				case R.id.button_cancel:
					 dialog.dismiss();
					 resetStatus();
					break;
				}
			}
			
		};
		commit.setOnClickListener(clickListener);
		cancel.setOnClickListener(clickListener);
	}
	
	 @Override
     public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
	 }
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		 switch(item.getItemId()){
		 case R.string.button_qun_send:
			 mIsInEditable = true;
			 invalidateOptionsMenu();
			 mRelationshipAdapter.notifyDataSetChanged();
			 break;
		 case R.string.button_ok:
			 showQunMessageInputDialog();
			 return true;
		 case R.string.button_cancel:
			 resetStatus();
			 break;
		 }
		
		return super.onOptionsItemSelected(item);
	 }
	 public boolean onCreateTitleBarOptionsMenu(Menu menu) {
		 menu.add(0, R.string.button_qun_send, 0, R.string.button_qun_send);
		 menu.add(1, R.string.button_cancel, 0, R.string.button_cancel);
		 menu.add(1, R.string.button_ok, 0, R.string.button_ok);
		return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mIsIMServiceRunning && mRelationshipAdapter != null && mRelationshipAdapter.getCount() > 0) {
			menu.setGroupVisible(1, mIsInEditable);
			menu.setGroupVisible(0, !mIsInEditable);
		} else {
			menu.setGroupVisible(1, false);
			menu.setGroupVisible(0, false);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	private SendQunMessageTask mSendQunMessageTask;
	private void sendQunMessageAsync(String content) {
		showDialog(DIALOG_PROGRESS);
		AsyncTaskUtils.cancelTask(mSendQunMessageTask);
		mSendQunMessageTask = new SendQunMessageTask();
		mSendQunMessageTask.execute(content);
	}
	private class SendQunMessageTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			for(RelationshipObject relationshipObject : mSelectedRelationshipObjectSet) {
				//不允许只输入空白字符，这样的内容是无意义的
				ConversationItemObject message = new ConversationItemObject();
				message.mUid = MyAccountManager.getInstance().getCurrentAccountUid();
				message.mPwd = MyAccountManager.getInstance().getAccountObject().mAccountPwd;
				message.mUName = MyAccountManager.getInstance().getAccountObject().mAccountName;
				message.mTargetType = relationshipObject.mTargetType;
				message.mTarget = relationshipObject.mTarget;
				message.mMessage = params[0];
				message.mMessageStatus = 0;
				message.mSeen = ConversationItemObject.SEEN;
				DebugUtils.logD(TAG, "SendQunMessageTask send to " + relationshipObject.mTargetName + ", message " + message.mMessage);
				mImService.sendMessageAsync(message);
				if (isCancelled()) {
					return null;
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			resetStatus();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
			resetStatus();
		}
		
	}
	
	private void resetStatus() {
		mIsInEditable = false;
		 mChecked.clear();
		 mSelectedRelationshipObjectSet.clear();
		invalidateOptionsMenu();
		 mRelationshipAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected AdapterWrapper<? extends BaseAdapter> getAdapterWrapper() {
		mRelationshipAdapter = new RelationshipAdapter(mContext, null, true);
		return new AdapterWrapper<CursorAdapter>(mRelationshipAdapter);
	}

	@Override
	protected Cursor loadLocal(ContentResolver cr) {
		return BjnoteContent.RELATIONSHIP.getAllRelationships(cr, MyAccountManager.getInstance().getCurrentAccountUid());
	}

	@Override
	protected int savedIntoDatabase(ContentResolver cr, List<? extends InfoInterface> infoObjects) {
		int insertOrUpdateCount = 0;
		if (infoObjects != null) {
			for(InfoInterface object:infoObjects) {
				if (object.saveInDatebase(cr, null)) {
					insertOrUpdateCount++;
				}
			}
		}
		return insertOrUpdateCount;
	}

	@Override
	protected List<? extends InfoInterface> getServiceInfoList(InputStream is, PageInfo pageInfo) {
		return RelationshipObject.parseList(is, pageInfo);
	}

	@Override
	protected Query getQuery() {
		mQuery =  new Query();
		mQuery.qServiceUrl = HaierServiceObject.getRelationshipUrl(MyAccountManager.getInstance().getCurrentAccountUid(), MyAccountManager.getInstance().getAccountObject().mAccountPwd, mSortType);
		return mQuery;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
		super.onItemClick(parent, itemView, position, id);
		ViewHolder viewHolder = (ViewHolder) itemView.getTag();
		if (mIsInEditable) {
			boolean last = mChecked.get(viewHolder._relationshipObject.mRelationshipId);
			mChecked.put(viewHolder._relationshipObject.mRelationshipId, !last);
			viewHolder._checkbox.setChecked(!last);
			if (!last) {
				//如果上次是unchecked,我们要添加
				mSelectedRelationshipObjectSet.add(viewHolder._relationshipObject);
			} else {
				mSelectedRelationshipObjectSet.remove(viewHolder._relationshipObject);
			}
		} else {
			ConversationListActivity.startActivity(mContext, viewHolder._relationshipObject);
		}
	}
	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}
	
	public static void startActivity(Context context) {
		Intent intent = new Intent(context, RelationshipActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
		
	}
	@Override
	protected int getContentLayout() {
		return R.layout.activity_relationship;
	}
	
	private class RelationshipAdapter extends CursorAdapter {

		private int mSortType = -1;
		public static final int DATA_SORT_BY_TIME = 1;
		public static final int DATA_SORT_BY_TYPE = 2;
		public RelationshipAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
		}
		
		public void setSortType(int orderType) {
			if (mSortType != orderType) {
				mSortType = orderType;
			}
			
		}

		@Override
		protected void onContentChanged() {
			if (mIsRefresh) {
				return;
			}
			mHandler.removeMessages(WHAT_REFRESH_LIST);
			mHandler.sendEmptyMessageDelayed(WHAT_REFRESH_LIST, 250);
		}


		private void refreshList() {
			super.onContentChanged();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(R.layout.relationship_item, parent, false);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder._name = (TextView) view.findViewById(R.id.name);
			
			viewHolder._leixing = (TextView) view.findViewById(R.id.data1);
			viewHolder._xinghao = (TextView) view.findViewById(R.id.data2);
			viewHolder._tel = (TextView) view.findViewById(R.id.data3);
			viewHolder._buydate = (TextView) view.findViewById(R.id.data4);
			viewHolder._checkbox = (CheckBox) view.findViewById(R.id.checkbox);
			view.setTag(viewHolder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			viewHolder._relationshipObject = RelationshipObject.getFromCursor(cursor);
			viewHolder._name.setText(cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_UNAME));
			viewHolder._leixing.setText(cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_LEIXING));
			viewHolder._xinghao.setText(cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_XINGHAO));
			viewHolder._tel.setText(cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_CELL));
			viewHolder._buydate.setText(cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_BUYDATE));
			viewHolder._checkbox.setVisibility(View.GONE);
			if (mIsInEditable) {
				if (mChecked.get(viewHolder._relationshipObject.mRelationshipId) == null) {
					mChecked.put(viewHolder._relationshipObject.mRelationshipId, false);
				}
				viewHolder._checkbox.setVisibility(View.VISIBLE);
				viewHolder._checkbox.setChecked(mChecked.get(viewHolder._relationshipObject.mRelationshipId));
			}
		}
		
	}
	
	private class ViewHolder {
		private TextView _name, _tel, _leixing, _xinghao, _buydate;
		private CheckBox _checkbox;
		private RelationshipObject _relationshipObject;
	}
	@Override
	protected void onRefreshStart() {
		mIsRefresh = true;
		BjnoteContent.RELATIONSHIP.delete(getContentResolver(), BjnoteContent.RELATIONSHIP.CONTENT_URI, BjnoteContent.RELATIONSHIP.UID_SELECTION, new String[]{MyAccountManager.getInstance().getCurrentAccountUid()});
	}
	@Override
	protected void onRefreshEnd() {
		mIsRefresh = false;
	}


}
