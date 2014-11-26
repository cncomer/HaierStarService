package com.bestjoy.app.haierstartservice.ui;

import java.util.Date;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.DateUtils;
import com.umeng.message.entity.UMessage;

public class YMessageListActivity extends BaseActionbarActivity implements OnItemClickListener{

	private YmessageCursorAdapter mYmessageCursorAdapter;
	private HashMap<String, Boolean> mSelectIds = new HashMap<String, Boolean>();
	private boolean mDeletedMode = false;
	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);
		ListView listView = (ListView) findViewById(R.id.listview);
		View progressBar = findViewById(R.id.progressBar);
		progressBar.setVisibility(View.VISIBLE);
		mYmessageCursorAdapter = new YmessageCursorAdapter(this, null, true);
		listView.setAdapter(mYmessageCursorAdapter);
		listView.setOnItemClickListener(this);
		loadUmessagesAsync();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		AsyncTaskUtils.cancelTask(mLoadUmessageAsyncTask);
		mYmessageCursorAdapter.changeCursor(null);
		
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add(0, R.string.menu_edit_for_delete, 1, R.string.menu_edit_for_delete);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item = menu.add(0, R.string.menu_delete, 2, R.string.menu_delete);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item = menu.add(0, R.string.menu_done, 3, R.string.menu_done);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		 return true;
	 }
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.string.menu_edit_for_delete:
			mDeletedMode = true;
			mYmessageCursorAdapter.notifyDataSetChanged();
			invalidateOptionsMenu();
			return true;
		case R.string.menu_delete:
			if (mSelectIds.size() == 0) {
				MyApplication.getInstance().showMessage(R.string.msg_no_selection_for_delete);
			} else {
				deleteAsync();
			}
			return true;
		case R.string.menu_done:
			mDeletedMode = false;
			mSelectIds.clear();
			mYmessageCursorAdapter.notifyDataSetChanged();
			invalidateOptionsMenu();
			return true;
		}
		return super.onOptionsItemSelected(item);
		
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.string.menu_edit_for_delete).setVisible(!mDeletedMode);
		menu.findItem(R.string.menu_delete).setVisible(mDeletedMode);
		menu.findItem(R.string.menu_done).setVisible(mDeletedMode);
		return super.onPrepareOptionsMenu(menu);
	}



	private LoadUmessageAsyncTask mLoadUmessageAsyncTask;
	private void loadUmessagesAsync() {
		AsyncTaskUtils.cancelTask(mLoadUmessageAsyncTask);
		mLoadUmessageAsyncTask = new LoadUmessageAsyncTask();
		mLoadUmessageAsyncTask.execute();
	}
	
	class LoadUmessageAsyncTask extends AsyncTask<Void, Void, Cursor> {

		@Override
		protected Cursor doInBackground(Void... params) {
			return mContext.getContentResolver().query(BjnoteContent.YMESSAGE.CONTENT_URI, BjnoteContent.YMESSAGE.PROJECTION, null, null, "" + HaierDBHelper.ID + " desc");
		}

		@Override
		protected void onPostExecute(Cursor result) {
			super.onPostExecute(result);
			mYmessageCursorAdapter.changeCursor(result);
			findViewById(R.id.progressBar).setVisibility(View.GONE);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			findViewById(R.id.progressBar).setVisibility(View.GONE);
		}
		
	}
	
	private class YmessageCursorAdapter extends CursorAdapter {

		public YmessageCursorAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
		}
		
		protected void onContentChanged() {
			super.onContentChanged();
	    }

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(R.layout.history_list_item, parent, false);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder._title = (TextView) view.findViewById(R.id.history_title);
			viewHolder._text = (TextView) view.findViewById(R.id.history_detail);
			viewHolder._text.setAutoLinkMask(Linkify.ALL);
			viewHolder._date = (TextView) view.findViewById(R.id.history_date);
			viewHolder._date.setVisibility(View.VISIBLE);
			
			viewHolder._checkbox = (CheckBox) view.findViewById(R.id.checkbox);
			view.setTag(viewHolder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			viewHolder._UMessage = YouMengMessageHelper.getInstance().getUMessageFromCursor(cursor);
			if (viewHolder._UMessage != null) {
				viewHolder._text.setText(viewHolder._UMessage.text);
				viewHolder._title.setText(viewHolder._UMessage.title);
			} else {
				viewHolder._title.setText(cursor.getString(BjnoteContent.YMESSAGE.INDEX_TITLE));
				viewHolder._text.setText(cursor.getString(BjnoteContent.YMESSAGE.INDEX_TEXT));
			}
			if (mDeletedMode) {
				viewHolder._checkbox.setVisibility(View.VISIBLE);
				Boolean checked = mSelectIds.get(viewHolder._UMessage.msg_id);
				if (checked == null) {
					checked = false;
				}
				viewHolder._checkbox.setChecked(checked);
			} else {
				viewHolder._checkbox.setVisibility(View.GONE);
			}
			
			viewHolder._date.setText(DateUtils.TOPIC_SUBJECT_DATE_TIME_FORMAT.format(new Date(cursor.getLong(BjnoteContent.YMESSAGE.INDEX_DATE))));
		}
		
	}
	
	private class ViewHolder {
		private TextView _date, _title, _text;
		private CheckBox _checkbox;
		private UMessage _UMessage;
	}
	
	public static void startActivity(Context context) {
		Intent intent = new Intent(context, YMessageListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (mDeletedMode) {
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			boolean checked = !viewHolder._checkbox.isChecked();
			if (!checked) {
				mSelectIds.remove(viewHolder._UMessage.msg_id);
			} else {
				mSelectIds.put(viewHolder._UMessage.msg_id, checked);
			}
			viewHolder._checkbox.setChecked(checked);
		}
		
	}
	
	private DeleteTask mDeleteTask;
	private static final String SELECTION_MSGID = HaierDBHelper.YOUMENG_MESSAGE_ID + "=?";
	private void deleteAsync() {
		AsyncTaskUtils.cancelTask(mDeleteTask);
		mDeleteTask = new DeleteTask();
		mDeleteTask.execute();
		showDialog(DIALOG_PROGRESS);
	}
	private class DeleteTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			ContentResolver cr = mContext.getContentResolver();
			for(String msgid : mSelectIds.keySet()) {
				BjnoteContent.delete(cr, BjnoteContent.YMESSAGE.CONTENT_URI, SELECTION_MSGID, new String[]{msgid});
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			mDeletedMode = false;
			mSelectIds.clear();
			mYmessageCursorAdapter.notifyDataSetChanged();
			invalidateOptionsMenu();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
		}
		
	}

}
