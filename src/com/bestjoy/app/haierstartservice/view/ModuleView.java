package com.bestjoy.app.haierstartservice.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bestjoy.app.haierstartservice.R;
import com.shwy.bestjoy.utils.DebugUtils;

public class ModuleView extends RelativeLayout{
	private static final String TAG = "ModuleView";
	
	private TextView mTitleView, mUnreadNumView;

	public ModuleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mTitleView = (TextView) this.findViewById(R.id.title);
	    mUnreadNumView = (TextView) this.findViewById(R.id.new_flag);
	}
	
	
	public void setUnreadNum(int num) {
		if (num > 0) {
			mUnreadNumView.setVisibility(View.GONE);
			mUnreadNumView.setText(String.valueOf(num));
		} else {
			mUnreadNumView.setVisibility(View.GONE);
			mUnreadNumView.setText("");
		}
	}
	
	public void setTitle(CharSequence text) {
		mTitleView.setText(text);
	}
	public void setTitle(int resId) {
		mTitleView.setText(resId);
	}
	
	
	public static ModuleView findViewById(int id, Activity activity) {
		View view = activity.findViewById(id);
		if (view != null && view instanceof ModuleView) {
			return (ModuleView) view;
		}
		DebugUtils.logD(TAG, "findViewById return null for viewId " + id);
		return null;
	}
	
}
