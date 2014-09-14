/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bestjoy.app.haierstartservice.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.R;
import com.shwy.bestjoy.utils.Intents;

/**
 * An HTML-based help screen with Back and Done buttons at the bottom.
 * 
 * @author yeluosuifeng2005@gmail.com (Eric Chen)
 */
public final class BrowserActivity extends BaseActionbarActivity {

	private static final String TAG = "BrowserActivity";
	private WebView webView;
	private String mUrl;

	private final Button.OnClickListener backListener = new Button.OnClickListener() {
		public void onClick(View view) {
			webView.goBack();
		}
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.w(TAG, "onCreate");
		if (isFinishing()) {
			return;
		}
		String title = getIntent().getStringExtra(Intents.EXTRA_NAME);
		if (!TextUtils.isEmpty(title)) {
			setTitle(title);
		}
		setContentView(R.layout.activity_browser);

		webView = (WebView) findViewById(R.id.webview);
		webView.setWebViewClient(new HelpClient());
		webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				// Activities and WebViews measure progress with different
				// scales.
				// The progress meter will automatically disappear when we reach
				// 100%
				BrowserActivity.this.setProgress(progress * 1000);
			}
		});
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDefaultTextEncodingName("utf-8");
		if (icicle != null) {
			webView.restoreState(icicle);
		} else {
			webView.loadUrl(mUrl);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		webView.saveState(state);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView.canGoBack()) {
				webView.goBack();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private final class HelpClient extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			invalidateOptionsMenu();
		}
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.add(1, R.id.button_back, 0, R.string.button_webview_goback);
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.button_back).setVisible(webView.canGoBack());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.button_back:
			webView.goBack();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean checkIntent(Intent intent) {
		mUrl = intent.getStringExtra(Intents.EXTRA_ADDRESS);
		return !TextUtils.isEmpty(mUrl);
	}
	
	public static void startActivity(Context context, String url, String title) {
		Intent intent = new Intent(context, BrowserActivity.class);
		intent.putExtra(Intents.EXTRA_ADDRESS, url);
		if (!TextUtils.isEmpty(title)) {
			intent.putExtra(Intents.EXTRA_NAME, title);
		}
		context.startActivity(intent);
	}

}
