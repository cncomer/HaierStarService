package com.bestjoy.app.haierstartservice.im;

import java.io.InputStream;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.ui.PullToRefreshListPageActivity;
import com.shwy.bestjoy.utils.AdapterWrapper;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.PageInfo;
import com.shwy.bestjoy.utils.Query;

public class RelationshipActivity extends PullToRefreshListPageActivity{
	
	private Handler mHandler;
	private static final int WHAT_REFRESH_LIST = 1000;
	private RelationshipAdapter mRelationshipAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing()) {
			return;
		}
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
	}
	
	 @Override
     public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
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
	protected com.shwy.bestjoy.utils.Query getQuery() {
		Query query =  new Query();
		query.qServiceUrl = HaierServiceObject.getRelationshipUrl(MyAccountManager.getInstance().getCurrentAccountUid(), MyAccountManager.getInstance().getAccountObject().mAccountPwd);
		
		return query;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
		super.onItemClick(parent, itemView, position, id);
		ViewHolder viewHolder = (ViewHolder) itemView.getTag();
		ConversationListActivity.startActivity(mContext, viewHolder._relationshipObject);
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
		return R.layout.pull_to_refresh_page_activity;
	}
	
	private class RelationshipAdapter extends CursorAdapter {

		public RelationshipAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
		}

		@Override
		protected void onContentChanged() {
			mHandler.removeMessages(WHAT_REFRESH_LIST);
			mHandler.sendEmptyMessageDelayed(WHAT_REFRESH_LIST, 500);
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
		}
		
	}
	
	private class ViewHolder {
		private TextView _name, _tel, _leixing, _xinghao, _buydate;
		private RelationshipObject _relationshipObject;
	}
	@Override
	protected void onRefreshStart() {
		BjnoteContent.RELATIONSHIP.delete(getContentResolver(), BjnoteContent.RELATIONSHIP.CONTENT_URI, BjnoteContent.RELATIONSHIP.UID_SELECTION, new String[]{MyAccountManager.getInstance().getCurrentAccountUid()});
	}
	@Override
	protected void onRefreshEnd() {
		
	}


}
