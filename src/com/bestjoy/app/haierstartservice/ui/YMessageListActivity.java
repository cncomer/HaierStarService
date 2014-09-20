package com.bestjoy.app.haierstartservice.ui;

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.DateUtils;
import com.umeng.message.entity.UMessage;

public class YMessageListActivity extends BaseActionbarActivity{

	private YmessageCursorAdapter mYmessageCursorAdapter;
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
		
		loadUmessagesAsync();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		AsyncTaskUtils.cancelTask(mLoadUmessageAsyncTask);
		mYmessageCursorAdapter.changeCursor(null);
		
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
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
			view.setTag(viewHolder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			UMessage message = YouMengMessageHelper.getInstance().getUMessageFromCursor(cursor);
			if (message != null) {
				viewHolder._text.setText(message.text);
				viewHolder._title.setText(message.title);
			} else {
				viewHolder._title.setText(cursor.getString(BjnoteContent.YMESSAGE.INDEX_TITLE));
				viewHolder._text.setText(cursor.getString(BjnoteContent.YMESSAGE.INDEX_TEXT));
			}
			viewHolder._date.setText(DateUtils.TOPIC_SUBJECT_DATE_TIME_FORMAT.format(new Date(cursor.getLong(BjnoteContent.YMESSAGE.INDEX_DATE))));
		}
		
	}
	
	private class ViewHolder {
		private TextView _date, _title, _text;
	}
	
	public static void startActivity(Context context) {
		Intent intent = new Intent(context, YMessageListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}
