package com.bestjoy.app.utils;

import android.content.Context;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.ui.LoginActivity;
import com.bestjoy.app.haierstartservice.ui.RegisterActivity;
import com.bestjoy.app.haierstartservice.ui.SettingsPreferenceActivity;
import com.bestjoy.app.haierstartservice.ui.YMessageListActivity;
import com.bestjoy.app.haierstartservice.update.AppAboutActivity;

public class MenuHandlerUtils {
	
    public static void onCreateOptionsMenu(Menu menu) {
    	
        SubMenu subMenu1 = menu.addSubMenu(1000, R.string.menu_more, 1000, R.string.menu_more);
        subMenu1.add(1000, R.string.menu_ymessage, 1000, R.string.menu_ymessage);
        subMenu1.add(1000, R.string.menu_login, 1001, R.string.menu_login);
        subMenu1.add(1000, R.string.menu_register, 1002, R.string.menu_register);
        subMenu1.add(1000, R.string.menu_setting, 1003, R.string.menu_setting);
        subMenu1.add(1000, R.string.menu_about, 1004, R.string.menu_about);
//        subMenu1.add(1000, R.string.menu_exit, 1005, R.string.menu_exit);

        MenuItem subMenu1Item = subMenu1.getItem();
        subMenu1Item.setIcon(R.drawable.ic_menu_moreoverflow);
        subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }
    
    public static boolean onOptionsItemSelected(MenuItem item, Context context) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case R.string.menu_login:
        	LoginActivity.startIntent(context, null);
     	   break;
        case R.string.menu_register:
        	RegisterActivity.startIntent(context, null);
      	   break;
        case R.string.menu_setting:
        	SettingsPreferenceActivity.startActivity(context);
      	   break;
        case R.string.menu_about:
        	context.startActivity(AppAboutActivity.createIntent(context));
      	   break;
        case R.string.menu_ymessage:
        	//Ymessage历史记录
        	YMessageListActivity.startActivity(context);
        	break;
//        case R.string.menu_exit:
//        	HaierAccountManager.getInstance().deleteDefaultAccount();
//        	break;

        }
        return false;
    }
    
    public static boolean onPrepareOptionsMenu(Menu menu, Context context) {
    	//如果已经登陆了，那么我们显示设置菜单
    	MenuItem menuItem = menu.findItem(R.string.menu_setting);
    	//如果没有已经登陆了，我们显示家管理菜单
    	MenuItem menuHomeManagerItem = menu.findItem(R.string.menu_manage_home);
    	if (MyAccountManager.getInstance().hasLoginned()) {
    		if (menuItem != null) {
    			menuItem.setVisible(true);
    		}
    		if (menuHomeManagerItem != null) {
    			menuHomeManagerItem.setVisible(true);
    		}
    	} else {
    		if (menuItem != null) {
    			menuItem.setVisible(false);
    		}
    		if (menuHomeManagerItem != null) {
    			menuHomeManagerItem.setVisible(false);
    		}
    	}
		return true;
	}
}
