package com.bestjoy.app.haierstartservice.account;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;

import com.bestjoy.app.utils.DebugUtils;

public class MyAccountManager {
	private static final String TAG = "HaierAccountManager";
	private AccountObject mHaierAccount;
	private Context mContext;
	SharedPreferences mSharedPreferences;
	private static MyAccountManager mInstance = new MyAccountManager();
	
	private MyAccountManager() {}
	
	public static MyAccountManager getInstance() {
		return mInstance;
	}
	
	public void setContext(Context context) {
		mContext = context; 
		mHaierAccount = null;
		mSharedPreferences = mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE);
		initAccountObject();
	}
	
	public void initAccountObject() {
		if (mHaierAccount == null) {
			mHaierAccount = AccountObject.getHaierAccountFromDatabase(mContext);
			initAccountHomes();
		}
	}
	
	public void initAccountHomes() {
		if (mHaierAccount != null) {
			mHaierAccount.mAccountHomes = HomeObject.getAllHomeObjects(mContext.getContentResolver(), mHaierAccount.mAccountUid);
			//XXX 如果保修卡数据太多，这里太耗时了，我们不做加载,在我的家的时候再做加载
//			for(HomeObject homeObject : mHaierAccount.mAccountHomes) {
//				homeObject.initBaoxiuCards(mContext.getContentResolver());
//			}
			mHaierAccount.mAccountHomeCount = mHaierAccount.mAccountHomes.size();
		}
	}
	
	public void deleteDefaultAccount() {
		if (mHaierAccount != null) {
			DebugUtils.logD(TAG, "start deleteDefaultAccount() for uid " + mHaierAccount.mAccountUid);
			//删除全部保修卡数据
			int deleted = BaoxiuCardObject.deleteAllBaoxiuCardsInDatabaseForAccount(mContext.getContentResolver(), mHaierAccount.mAccountUid);
			DebugUtils.logD(TAG, "deleted " + deleted + " BaoxiuCards");
			//删除全部家数据
			deleted = HomeObject.deleteAllHomesInDatabaseForAccount(mContext.getContentResolver(), mHaierAccount.mAccountUid);
			DebugUtils.logD(TAG, "deleted " + deleted + " Homes");
			//删除账户数据
			deleted = AccountObject.deleteAccount(mContext.getContentResolver(), mHaierAccount.mAccountUid);
			mHaierAccount = null;
			DebugUtils.logD(TAG, "deleted " + deleted + " Account");
			DebugUtils.logD(TAG, "end deleteDefaultAccount()");
		} else {
			DebugUtils.logD(TAG, "deleteDefaultAccount() nothing to do");
		}
	}
	
	public AccountObject getAccountObject() {
		return mHaierAccount;
	}
	public long getHomeAIdAtPosition(int position) {
		long homeAid = -1;
		if (mHaierAccount != null && mHaierAccount.mAccountHomes.size() > 0) {
			homeAid = mHaierAccount.mAccountHomes.get(position).mHomeAid;
		}
		DebugUtils.logD(TAG, "getHomeAIdAtPosition() position=" + position + ", mHaierAccount=" + mHaierAccount + ", mAccountHomes=" + mHaierAccount.mAccountHomes.size() + ", homeAid=" + homeAid);
		return homeAid;
	}
	public long getCurrentAccountId() {
		return mHaierAccount != null ? mHaierAccount.mAccountUid : -1; 
	}
	public String getCurrentAccountUid() {
		return mHaierAccount != null ? String.valueOf(mHaierAccount.mAccountUid) : null; 
	}
	public boolean hasLoginned() {
		return mHaierAccount != null && mHaierAccount.mAccountId > 0;
	}
	/**是否有保修卡*/
	public boolean hasBaoxiuCards() {
		if (mHaierAccount != null) {
			return mHaierAccount.mAccountBaoxiuCardCount > 0;
		}
		return false;
	}
	/**新建保修卡后都需要调用该方法来更新家*/
	public void updateHomeObject(long aid) {
		if (mHaierAccount != null) {
			mHaierAccount.mAccountBaoxiuCardCount = 0;
			for(HomeObject home : mHaierAccount.mAccountHomes) {
				if (home.mHomeAid  == aid) {
					home.initBaoxiuCards(mContext.getContentResolver());
				}
				//重新初始化保修卡数据
				mHaierAccount.mAccountBaoxiuCardCount += home.mHomeCardCount;
			}
		}
	}
	
	public boolean hasHomes() {
		return mHaierAccount != null && mHaierAccount.mAccountHomeCount > 0;
	}
	/**
	 * 返回上一次登陆时候使用的用户名
	 * @return
	 */
	public String getLastUsrTel() {
		return mSharedPreferences.getString("lastUserTel", "");
	}
	
    public void saveLastUsrTel(String userName) {
    	mSharedPreferences.edit().putString("lastUserTel", (userName == null ? "" : userName)).commit();
	}
    
    public boolean saveAccountObject(ContentResolver cr, AccountObject accountObject) {
    	
    	if (mHaierAccount != accountObject) {
    		boolean success = accountObject.saveInDatebase(cr, null);
    		if (success) {
    			updateAccount();
    			return true;
    		}
    	}
    	return false;
    	
    }
    /**
     * 更新账户，每当我们增删家和保修卡数据的时候，调用该方法可以同步当前账户信息.
     */
    public void updateAccount() {
    	mHaierAccount = null;
    	initAccountObject();
    }
    
    /**返回默认的手机号码*/
    public String getDefaultPhoneNumber() {
		return mHaierAccount != null ? mHaierAccount.mAccountTel : null;
	}
}
