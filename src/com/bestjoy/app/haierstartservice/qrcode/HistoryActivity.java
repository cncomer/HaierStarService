package com.bestjoy.app.haierstartservice.qrcode;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.database.HistoryItem;
import com.bestjoy.app.haierstartservice.ui.BaseActionbarActivity;
import com.bestjoy.app.haierstartservice.ui.CaptureActivity;
import com.shwy.bestjoy.utils.Intents;

public final class HistoryActivity extends BaseActionbarActivity implements OnItemClickListener{

	private static final int SEND_ID = Menu.FIRST;
	private static final int CLEAR_ID = Menu.FIRST + 1;

	private HistoryManager historyManager;
	private HistoryItemAdapter adapter;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (this.isFinishing()) {
			return;
		}
		setContentView(R.layout.activity_history);
		this.historyManager = new HistoryManager(this);
		adapter = new HistoryItemAdapter(this);
		ListView listview = (ListView) findViewById(R.id.listview);
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		List<HistoryItem> items = historyManager.buildHistoryItems();
		adapter.clear();
		for (HistoryItem item : items) {
			adapter.add(item);
		}
		if (adapter.isEmpty()) {
			adapter.add(new HistoryItem(null, null, null));
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		menu.add(Menu.NONE, position, position,
				R.string.history_clear_one_history_text);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		int position = item.getItemId();
		historyManager.deleteHistoryItem(position);
		finish();
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (historyManager.hasHistoryItems()) {
			menu.add(0, SEND_ID, 0, R.string.history_send).setIcon(
					android.R.drawable.ic_menu_share);
			menu.add(0, CLEAR_ID, 0, R.string.history_clear_text).setIcon(
					android.R.drawable.ic_menu_delete);
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SEND_ID:
			CharSequence history = historyManager.buildHistory();
			Uri historyFile = HistoryManager.saveHistory(history.toString());
			if (historyFile == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.msg_sd_unavailable);
				builder.setPositiveButton(R.string.button_ok, null);
				builder.show();
			} else {
				Intent intent = new Intent(Intent.ACTION_SEND,
						Uri.parse("mailto:"));
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				String subject = getResources().getString(
						R.string.history_email_title);
				intent.putExtra(Intent.EXTRA_SUBJECT, subject);
				intent.putExtra(Intent.EXTRA_TEXT, subject);
				intent.putExtra(Intent.EXTRA_STREAM, historyFile);
				intent.setType("text/csv");
				startActivity(intent);
			}
			break;
		case CLEAR_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.msg_sure);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int i2) {
							historyManager.clearHistory();
							dialog.dismiss();
							finish();
						}
					});
			builder.setNegativeButton(R.string.button_cancel, null);
			builder.show();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (adapter.getItem(position).getResult() != null) {
			Intent intent = new Intent(this, CaptureActivity.class);
			intent.putExtra(Intents.History.ITEM_NUMBER, position);
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
		
	}

}
