package com.bestjoy.app.haierstartservice.view;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.im.RelationshipActivity;
import com.bestjoy.app.haierstartservice.ui.CreateUserBaoxiucardActivity;
import com.bestjoy.app.haierstartservice.ui.IDCardViewActivity;
import com.bestjoy.app.haierstartservice.ui.YMessageListActivity;
import com.shwy.bestjoy.utils.DebugUtils;

public class ModuleViewUtils {

	private static final String TAG = "ModuleViewUtils";
	private Context mContext;
	private static ModuleViewUtils INSTANCE = new ModuleViewUtils();
	
	private static final int[] MODULE_IDS = new int[] {
		R.id.model_my_idcard,
		R.id.model_new_baoxiucard,
		R.id.model_my_business,
		R.id.model_my_messages,
		R.id.model_my_store,
	};
	
	public static ModuleViewUtils getInstance() {
		return INSTANCE;
	}
	
	public void setContext(Context context) {
		mContext = context;
	}
	
	public void initModules(Activity activity) {
		int index = 0;
		for (int id : MODULE_IDS) {
			DebugUtils.logD(TAG, "index " + index);
			ModuleView.findViewById(id, activity).setOnClickListener(mModuleOnClickListener);
			index++;
		}
	}
	
	public static int getModelIdFromBundle(Bundle bundle) {
		return R.id.model_my_business;
	}
	
	private View.OnClickListener mModuleOnClickListener  = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			int id = v.getId();
			switch(id) {
			case R.id.model_new_baoxiucard:
			case R.id.model_my_business:
			case R.id.model_my_idcard:
				
				if (MyAccountManager.getInstance().hasLoginned()) {
					if (id == R.id.model_my_idcard) {
						IDCardViewActivity.startActivity(mContext, MyAccountManager.getInstance().getAccountObject().mAccountUid);
					} else if (id == R.id.model_my_business) {
						RelationshipActivity.startActivity(mContext);
					} else if (id == R.id.model_new_baoxiucard) {
						CreateUserBaoxiucardActivity.startActivity(mContext);
					}
				} else {
					MyApplication.getInstance().showNeedLoginMessage();
				}
				
				break;
			case R.id.model_my_messages:
				YMessageListActivity.startActivity(mContext);
				break;
			case R.id.model_my_store:
				MyApplication.getInstance().showUnsupportMessage();
				break;
			}
			
		}
		
	};

}
