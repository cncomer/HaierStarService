package com.bestjoy.app.haierstartservice.im;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.Menu;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.service.IMService;
import com.bestjoy.app.haierstartservice.ui.LoadMoreWithPageActivity;
import com.bestjoy.app.utils.JsonParser;
import com.bestjoy.app.utils.SpeechRecognizerEngine;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.shwy.bestjoy.utils.AdapterWrapper;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NotifyRegistrant;
import com.shwy.bestjoy.utils.PageInfo;
import com.shwy.bestjoy.utils.Query;

public class ConversationListActivity extends LoadMoreWithPageActivity implements View.OnClickListener{

	private static final String TAG = "ConversationListActivity";
	private static final int WHAT_REQUEST_REFRESH_LIST = 11000;
	private EditText mInputEdit;
	private ImageView mTextIcon, mVoiceIcon;
	private TextView mConnectedStatusView;
	private Button mVoiceBtn;
	private Handler mUiHandler;
	
	private IMService mImService;
	private ServiceConnection mServiceConnection;
	
	private ConversationAdapter mConversationAdapter;
	private RelationshipObject mRelationshipObject;
	private long mCurrentMessageId = -1;
	private int mCurrentPosition = -1;
	private boolean mIsRefreshing = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (this.isFinishing()) {
			return;
		}
		setShowHomeUp(true);
		setLoadMorePosition(LOAD_MORE_TOP);
		mServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mImService = ((IMService.MyBinder)service).getService();
				mImService.setIsInConversationSession(true, mRelationshipObject.mTarget);
				if (mImService != null && mImService.isConnected()) {
					mConnectedStatusView.setVisibility(View.GONE);
				} else {
					mConnectedStatusView.setVisibility(View.VISIBLE);
					mConnectedStatusView.setText(R.string.msg_offline_confirm_click);
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mImService.setIsInConversationSession(false, mRelationshipObject.mTarget);
			}
		};
		Intent intent = new Intent(mContext, IMService.class);
		mContext.bindService(intent,mServiceConnection, Context.BIND_AUTO_CREATE);
		mListView = getListView();
		addOnScrollListenerList(mOnScrollListener);
		
		mUiHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch(msg.what){
				case IMService.WHAT_SEND_MESSAGE_LOGIN:
					if (mImService != null && mImService.isConnected()) {
						mConnectedStatusView.setVisibility(View.GONE);
						mImService.setIsInConversationSession(true, mRelationshipObject.mTarget);
					} else {
						mConnectedStatusView.setVisibility(View.VISIBLE);
						mConnectedStatusView.setText(R.string.msg_im_status_logining);
						if (mImService != null) mImService.setIsInConversationSession(false, mRelationshipObject.mTarget);
					}
					break;
				case IMService.WHAT_SEND_MESSAGE_NO_NETWORK:
					mConnectedStatusView.setVisibility(View.VISIBLE);
					mConnectedStatusView.setText(R.string.msg_offline_no_network);
					if (mImService != null) mImService.setIsInConversationSession(false, mRelationshipObject.mTarget);
					break;
				case IMService.WHAT_SEND_MESSAGE_OFFLINE:
					mConnectedStatusView.setVisibility(View.VISIBLE);
					mConnectedStatusView.setText(R.string.msg_im_status_offline);
					if (mImService != null) mImService.setIsInConversationSession(false, mRelationshipObject.mTarget);
					break;
				case IMService.WHAT_SEND_MESSAGE_EXIT:
				case IMService.WHAT_SEND_MESSAGE_INVALID_USER:
					mConnectedStatusView.setVisibility(View.VISIBLE);
					mConnectedStatusView.setText(R.string.msg_im_status_offline);
					if (mImService != null) mImService.setIsInConversationSession(false, mRelationshipObject.mTarget);
					break;
				case WHAT_REQUEST_REFRESH_LIST:
					mConversationAdapter.callSuperOnContentChanged();
					if (mIsAtListBottom) {
						mListView.setSelection(mConversationAdapter.getCount());
					} else {
						MyApplication.getInstance().showMessage(R.string.new_msg_comming);
					}
					break;
				}
			}
		};
		NotifyRegistrant.getInstance().register(mUiHandler);
		IMService.connectIMService(this);
		
		initEditLayout();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	private OnScrollListener mOnScrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (!mIsRefreshing) {
				mCurrentPosition = firstVisibleItem;
				DebugUtils.logD(TAG, "onScroll() current item position=" + mCurrentPosition);
			}
		}
	};
	
	 @Override
     public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
	 }
	
	private void initEditLayout() {
		mInputEdit = (EditText) findViewById(R.id.input);
		mInputEdit.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					sendMessageLocked();
				}
				return false;
			}
			
		});
		
		mVoiceBtn = (Button) findViewById(R.id.button_voice);
		
		mTextIcon = (ImageView) findViewById(R.id.button_text_icon);
		mTextIcon.setOnClickListener(this);
		
		mVoiceIcon = (ImageView) findViewById(R.id.button_voice_icon);
		mVoiceIcon.setOnClickListener(this);
		
		findViewById(R.id.button_add_icon).setOnClickListener(this);
		
		//当前连接状态，点击可以出发重新连接
		mConnectedStatusView = (TextView) findViewById(R.id.status_view);
		mConnectedStatusView.setOnClickListener(this);
		
		updateEditLayout(true);
		initVoiceLayout();
		
	}
	/**
	 * 是否显示语音输入
	 * @param showVoiceInput
	 */
	private void updateEditLayout(boolean showVoiceIcon) {
		if (showVoiceIcon) {
			mVoiceIcon.setVisibility(View.VISIBLE);
			mTextIcon.setVisibility(View.GONE);
			mVoiceBtn.setVisibility(View.GONE);
			mInputEdit.setVisibility(View.VISIBLE);
		} else {
			mVoiceIcon.setVisibility(View.GONE);
			mTextIcon.setVisibility(View.VISIBLE);
			mVoiceBtn.setVisibility(View.VISIBLE);
			mInputEdit.setVisibility(View.GONE);
		}
	}
	
	private View mVoiceInputPopLayout;
	private TextView mVoiceInputStatus;
	private EditText mAskInput;
	private ImageView mVoiceImage;
	private SpeechRecognizerEngine mSpeechRecognizerEngine;
	private VoiceButtonTouchListener mVoiceButtonTouchListener;
	private void initVoiceLayout() {
		mVoiceInputPopLayout = findViewById(R.id.voice_input_layout);
		mVoiceInputPopLayout.setVisibility(View.GONE);
		mVoiceInputStatus = (TextView) findViewById(R.id.voice_input_status);
		mVoiceButtonTouchListener = new VoiceButtonTouchListener();
		mVoiceBtn.setOnTouchListener(mVoiceButtonTouchListener);
		
		mAskInput = (EditText) findViewById(R.id.voice_input_confirm);
		mVoiceImage = (ImageView) findViewById(R.id.voice_input_imageview);
		mSpeechRecognizerEngine = SpeechRecognizerEngine.getInstance(mContext);
		
	}
	
	private RecognizerListener mRecognizerListener = new RecognizerListener() {
		public boolean _isCanceled = false;
		
		@Override
		public void onBeginOfSpeech() {
			mAskInput.setHint(R.string.hint_voice_input);
			DebugUtils.logD(TAG, "chenkai onBeginOfSpeech");
			
		}
	
		@Override
		public void onEndOfSpeech() {
			mAskInput.setHint(R.string.hint_voice_input_wait);
			DebugUtils.logD(TAG, "chenkai onEndOfSpeech");
			mSpeechRecognizerEngine.stopListen();
		}
	
		@Override
		public void onError(SpeechError err) {
			//MyApplication.getInstance().showMessage(err.getPlainDescription(true));
			DebugUtils.logD(TAG, "chenkai onError " + err.getPlainDescription(true));
			if (mVoiceInputPopLayout.getVisibility() == View.VISIBLE) {
				mVoiceInputPopLayout.setVisibility(View.GONE);
			}
		}
	
		@Override
		public void onResult(RecognizerResult arg0, boolean arg1) {
			
			String text = JsonParser.parseIatResult(arg0.getResultString());
			DebugUtils.logD(TAG, "chenkai onResult " + text);
			if (!TextUtils.isEmpty(text)) {
				//BeepAndVibrate.getInstance().playBeepSoundAndVibrate();
				mAskInput.append(text);
				mAskInput.setSelection(mAskInput.length());
				
				if (mVoiceInputPopLayout.getVisibility() == View.VISIBLE && !mVoiceButtonTouchListener._isCanceled && mVoiceButtonTouchListener._isUp) {
					mVoiceInputPopLayout.setVisibility(View.GONE);
					doVoiceQuery(text);
				}
			}
		}
	
		@Override
		public void onVolumeChanged(int volume) {
			mVoiceImage.setImageLevel(volume);
		}

		@Override
		public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
			
		}
		
	};
	
	private class VoiceButtonTouchListener implements View.OnTouchListener{
		private float _downX = 0.0f;
		private float _downY = 0.0f;
		private boolean _isCanceled = false;
		private boolean _isUp = false;
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				_isCanceled = false;
				_isUp = false;
				mVoiceInputPopLayout.setVisibility(View.VISIBLE);
				_downX = event.getX();
				_downY = event.getY();
				mVoiceInputStatus.setText(R.string.msg_cancel_voice_input_down);
				mAskInput.setText("");
				mAskInput.setHint(R.string.hint_voice_input);
				mVoiceImage.setImageResource(R.drawable.voice_input_listen);
				mSpeechRecognizerEngine.stopListen();
				mSpeechRecognizerEngine.startListen(mRecognizerListener);
				break;
			case MotionEvent.ACTION_MOVE:
				float moveX = event.getX();
				float moveY = event.getY();
				if (_downY - moveY > 100) {
					_isCanceled = true;
					mVoiceInputStatus.setText(R.string.msg_cancel_voice_input_by_cancel);
					mSpeechRecognizerEngine.stopListen();
					mSpeechRecognizerEngine.cancel();
					mVoiceImage.setImageResource(R.drawable.voice_cancel);
					mAskInput.setText("");
					mAskInput.setHint(R.string.hint_voice_input_canceled);
				} else if (_isCanceled){
					_isCanceled = false;
					mVoiceInputStatus.setText(R.string.msg_cancel_voice_input_down);
					mAskInput.setHint(R.string.hint_voice_input);
					mSpeechRecognizerEngine.stopListen();
					mSpeechRecognizerEngine.startListen(mRecognizerListener);
					mVoiceImage.setImageResource(R.drawable.voice_input_listen);
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				_isCanceled = true;
				mVoiceInputPopLayout.setVisibility(View.GONE);
				mVoiceInputStatus.setText(R.string.msg_cancel_voice_input_by_cancel);
				mSpeechRecognizerEngine.stopListen();
				break;
			case MotionEvent.ACTION_UP:
				_isUp = true;
				String query = mAskInput.getText().toString().trim();
				mSpeechRecognizerEngine.stopListen();
				if (_isCanceled) {
					mVoiceInputPopLayout.setVisibility(View.GONE);
					mSpeechRecognizerEngine.cancel();
				} else if (query.length() > 0) {
					mVoiceInputPopLayout.setVisibility(View.GONE);
					doVoiceQuery(query);
				}
				break;
			}
			return false;
		}
	}
	
	private void doVoiceQuery(String query) {
		DebugUtils.logD(TAG, "doVoiceQuery() query=" + query);
		mInputEdit.getText().clear();
		mInputEdit.append(query);
		//这里是语音识别后进入文本编写界面以便用户修改输入内容
		updateEditLayout(true);
//		sendMessageLocked();
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		removeOnScrollListenerList(mOnScrollListener);
		if (mImService != null) mImService.setIsInConversationSession(false, mRelationshipObject.mTarget);
		mContext.unbindService(mServiceConnection);
		NotifyRegistrant.getInstance().unRegister(mUiHandler);
		mConversationAdapter.changeCursor(null);
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.button_voice:
			break;
		case R.id.button_text_icon:
			updateEditLayout(true);
			break;
		case R.id.button_voice_icon:
			updateEditLayout(false);
			break;
		case R.id.button_add_icon:
			MyApplication.getInstance().showUnsupportMessage();
			break;
		case R.id.status_view:
			IMService.connectIMService(this);
			break;
		}
	}
	
	private void sendMessageLocked() {
		if (!ComConnectivityManager.getInstance().isConnected()) {
			MyApplication.getInstance().showMessage(MyApplication.getInstance().getGernalNetworkError());
			return;
		}
		if (!mImService.isConnected()) {
			MyApplication.getInstance().showMessage(R.string.msg_im_status_logining);
			return;
		}
		String text = mInputEdit.getText().toString().trim();
		if (!TextUtils.isEmpty(text)) {
			mInputEdit.getText().clear();
			//不允许只输入空白字符，这样的内容是无意义的
			ConversationItemObject message = new ConversationItemObject();
			message.mUid = MyAccountManager.getInstance().getCurrentAccountUid();
			message.mPwd = MyAccountManager.getInstance().getAccountObject().mAccountPwd;
			message.mUName = MyAccountManager.getInstance().getAccountObject().mAccountName;
			message.mTargetType = mRelationshipObject.mTargetType;
			message.mTarget = mRelationshipObject.mTarget;
			message.mMessage = text;
			message.mMessageStatus = 0;
			message.mSeen = ConversationItemObject.SEEN;
			mImService.sendMessageAsync(message);
		}
	}
	
	private void sendMessageLocked(ConversationItemObject message) {
		if (!ComConnectivityManager.getInstance().isConnected()) {
			MyApplication.getInstance().showMessage(MyApplication.getInstance().getGernalNetworkError());
			return;
		}
		if (!mImService.isConnected()) {
			MyApplication.getInstance().showMessage(R.string.msg_im_status_logining);
			return;
		}
		mImService.sendMessageAsync(message);
	}
	
	@Override
	protected boolean checkIntent(Intent intent) {
		mRelationshipObject = intent.getParcelableExtra("relationship");
		if (mRelationshipObject == null) {
			DebugUtils.logE(TAG, "checkIntent failed, you must supply RelationshipObject");
			return false;
		}
		return true;
	}
	
	public static void startActivity(Context context, RelationshipObject relationshipObject) {
		Intent intent = new Intent(context, ConversationListActivity.class);
		intent.putExtra("relationship", relationshipObject);
		context.startActivity(intent);
	}
	
	
	
	private class ConversationAdapter extends CursorAdapter{
		private static final int TYPE_TOP = 0;
		private static final int TYPE_LEFT = 1;
		private static final int TYPE_RIGHT = 2;
		
		private static final int TYPE_COUNT = 3;

		public ConversationAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
		}
		
		@Override
		protected void onContentChanged() {
			//如果用户正在下拉刷新，我们不要刷新列表数据，因为框架会帮我们查询，changeCursor()
			if (mIsRefreshing) return;
			//一秒内延迟刷新，提高性能
			mUiHandler.removeMessages(WHAT_REQUEST_REFRESH_LIST);
			mUiHandler.sendEmptyMessageDelayed(WHAT_REQUEST_REFRESH_LIST, 250);
		}
		
		private void callSuperOnContentChanged() {
			super.onContentChanged();
		}


		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = null;
			ViewHolder viewHolder = new ViewHolder();
			int viewType = getItemViewType(cursor.getPosition());
			if (TYPE_LEFT == viewType) {
				view = LayoutInflater.from(context).inflate(R.layout.conversation_item_left, parent, false);
			} else if (TYPE_RIGHT == viewType) {
				view = LayoutInflater.from(context).inflate(R.layout.conversation_item_right, parent, false);
			}
			viewHolder._avator = (ImageView) view.findViewById(R.id.avator);
			viewHolder._error = (ImageView) view.findViewById(R.id.error);
			viewHolder._name = (TextView) view.findViewById(R.id.name);
			viewHolder._content = (TextView) view.findViewById(R.id.content);
			viewHolder._time = (TextView) view.findViewById(R.id.date);
			view.setTag(viewHolder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			viewHolder._name.setText(cursor.getString(IMHelper.INDEX_UNAME));
			viewHolder._content.setText(cursor.getString(IMHelper.INDEX_TEXT));
			viewHolder._error.setVisibility(View.GONE);
			viewHolder._conversationItemObject = ConversationItemObject.getConversationItemObjectFromCursor(cursor);
			if (cursor.getInt(IMHelper.INDEX_STATUS) == 0) {
				viewHolder._time.setText(R.string.msg_sending);
			} else if (cursor.getInt(IMHelper.INDEX_STATUS) == 2) {
				//发送失败
				viewHolder._time.setText(R.string.msg_sending_failed);
				//显示发送失败icon
				viewHolder._error.setVisibility(View.VISIBLE);
				viewHolder._error.setTag(viewHolder);
				viewHolder._error.setOnClickListener(mErrorOnclickListener);
			} else {
				viewHolder._time.setText(IMHelper.LOCAL_DATE_TIME_FORMAT.format(new Date(Long.valueOf(cursor.getString(IMHelper.INDEX_SERVICE_TIME)))));
			}
			
			
		}
		

		@Override
		public int getItemViewType(int position) {
			Cursor c = (Cursor) getItem(position);
			String sender = c.getString(IMHelper.INDEX_UID);
			if (sender.equals(MyAccountManager.getInstance().getCurrentAccountUid())) {
				//用户自己的消息显示在右边
				return TYPE_RIGHT;
			} else {
				return TYPE_LEFT;
			}
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_COUNT;
		}
	}
	
	private static class ViewHolder {
		private ImageView _avator, _error;
		private TextView _name, _content, _time;
		private ConversationItemObject _conversationItemObject;
	}
	
//	private LocalMessageAsyncTask mLocalMessageAsyncTask;
//	private void loadLocalMessageAsync() {
//		AsyncTaskUtils.cancelTask(mLocalMessageAsyncTask);
//		mLocalMessageAsyncTask = new LocalMessageAsyncTask();
//		mLocalMessageAsyncTask.execute();
//	}
//	
//	private class LocalMessageAsyncTask extends AsyncTask<Void, Void, Cursor> {
//
//		@Override
//		protected Cursor doInBackground(Void... params) {
//			return IMHelper.getAllLocalMessage(mContext.getContentResolver(), MyAccountManager.getInstance().getCurrentAccountUid(), mRelationshipObject.mTargetType, mRelationshipObject.mTarget);
//		}
//
//		@Override
//		protected void onPostExecute(Cursor result) {
//			super.onPostExecute(result);
//			mConversationAdapter.changeCursor(result);
//			if (mConversationAdapter.getCount() > 0) {
//				mListView.setSelection(mConversationAdapter.getCount());
//			}
//			
//		}
//
//		@Override
//		protected void onCancelled() {
//			super.onCancelled();
//		}
//		
//	}

	@Override
	protected AdapterWrapper<? extends BaseAdapter> getAdapterWrapper() {
		mConversationAdapter = new ConversationAdapter(this, null, true);
		return new AdapterWrapper<CursorAdapter>(mConversationAdapter);
	}
    
	@Override
	protected Cursor loadLocal(ContentResolver contentResolver) {
		Cursor cursor = IMHelper.getAllLocalMessage(mContext.getContentResolver(), MyAccountManager.getInstance().getCurrentAccountUid(), mRelationshipObject.mTargetType, mRelationshipObject.mTarget);
		if (cursor != null) {
			if (mCurrentMessageId == -1) {
				//第一次查询
				if (cursor.moveToLast()) {
					mCurrentMessageId =  cursor.getLong(IMHelper.INDEX_ID);
					mCurrentPosition = cursor.getCount() - 1;
				}
			} else {
				long id = -1;
				while(cursor.moveToNext()) {
					id = cursor.getLong(IMHelper.INDEX_ID);
					if (mCurrentMessageId == id) {
						mCurrentPosition = cursor.getPosition();
						break;
					}
				}
			}
		}
		DebugUtils.logD(TAG, "loadLocal() current item position=" + mCurrentPosition + ", id=" + mCurrentMessageId);
		return cursor;
	}

	@Override
	protected int savedIntoDatabase(ContentResolver cr, List<? extends InfoInterface> infoObjects) {
		return IMHelper.saveList(cr, infoObjects);
	}

	@Override
	protected List<? extends InfoInterface> getServiceInfoList(InputStream is, PageInfo pageInfo) {
		return IMHelper.parseList(is, pageInfo, mRelationshipObject.mTargetType);
	}

	@Override
	protected Query getQuery() {
		Query query = new Query();
		query.qServiceUrl = HaierServiceObject.getMessagesUrlByUidByTid(MyAccountManager.getInstance().getCurrentAccountUid(), mRelationshipObject.mTarget, "p2p");
		return query;
	}

	@Override
	protected void onLoadMoreStart() {
		mIsRefreshing = true;
//		mCurrentMessageId = -1;
		if (mConversationAdapter != null && mCurrentPosition != -1 && mCurrentPosition < mConversationAdapter.getCount()) {
			Cursor c = mConversationAdapter.getCursor();
			if (c != null && c.moveToPosition(mCurrentPosition)) {
				mCurrentMessageId = c.getLong(IMHelper.INDEX_ID);
			}
			
		}
		DebugUtils.logD(TAG, "onRefreshStart() current item position=" + mCurrentPosition + ", id=" + mCurrentMessageId);
	}

	@Override
	protected void onLoadMoreEnd() {
		mIsRefreshing = false;
		mListView.setSelection(mCurrentPosition);
		DebugUtils.logD(TAG, "onLoadMoreEnd() current item position=" + mCurrentPosition + ", id=" + mCurrentMessageId);
	}

	@Override
	protected int getContentLayout() {
		return R.layout.activity_im_conversation;
	}
	
	private View.OnClickListener mErrorOnclickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			final ViewHolder viewHolder = (ViewHolder) v.getTag();
			
			new AlertDialog.Builder(mContext)
			.setPositiveButton(android.R.string.cancel, null)
			.setItems(R.array.im_resend_items, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which) {
					case 0:  //重新发送
						viewHolder._conversationItemObject.deleteMessage(getContentResolver());
						sendMessageLocked(viewHolder._conversationItemObject);
						break;
					case 1: //删除
						viewHolder._conversationItemObject.deleteMessage(getContentResolver());
						break;
					}
					
				}
			})
			.show();
		}
		
	};

}
