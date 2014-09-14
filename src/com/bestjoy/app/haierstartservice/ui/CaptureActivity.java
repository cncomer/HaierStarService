package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.util.Vector;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.database.HistoryItem;
import com.bestjoy.app.haierstartservice.qrcode.HistoryActivity;
import com.bestjoy.app.haierstartservice.qrcode.HistoryManager;
import com.bestjoy.app.haierstartservice.qrcode.ViewfinderView;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2;
import com.bestjoy.app.utils.BeepAndVibrate;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.camera.FlashlightManager;
import com.google.zxing.client.result.AddressBookResultHandler;
import com.google.zxing.client.result.HaierResultHandler;
import com.google.zxing.client.result.ResultButtonListener;
import com.google.zxing.client.result.ResultHandler;
import com.google.zxing.client.result.ResultHandlerFactory;
import com.shwy.bestjoy.utils.Intents;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;

public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

  private static final String TAG = "CaptureActivity";
  private static final Pattern COMMA_PATTERN = Pattern.compile(",");

  private static final int INPUT_ID = Menu.FIRST;
  private static final int HISTORY_ID = Menu.FIRST + 1;
  private static final int SETTINGS_ID = Menu.FIRST + 2;
  private static final int HELP_ID = Menu.FIRST + 3;


  private CaptureActivityHandler handler;
  private Result savedResultToShow;

  private ViewfinderView viewfinderView;
  private View statusView;
  private View resultView;
  private ToggleButton status_view_flashlight;
  private Result lastResult;
  private boolean hasSurface;
  private Vector<BarcodeFormat> decodeFormats;
  private String characterSet;
  private HistoryManager historyManager;
  private String BIDUri=null;
  private final int getBIDUriRequest = 10;
  private BeepAndVibrate media;
  
//  private boolean isProductVerify = false;
//  private boolean isEnterpriseCard = false;
  
  private boolean isStartPreview = false;

  public static final int HISTORY_REQUEST_CODE = 0x0000bacc;

  ViewfinderView getViewfinderView() {
    return viewfinderView;
  }
  
  private boolean mScanTask= false;

  public Handler getHandler() {
    return handler;
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    if (icicle != null) {
    	mScanTask = icicle.getBoolean(Intents.EXTRA_SCAN_TASK, false);
    } else {
    	Intent intent = getIntent();
        if (intent != null) {
        	mScanTask = intent.getBooleanExtra(Intents.EXTRA_SCAN_TASK, false);
        }
    }
    
    CameraManager.init(getApplication());
    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    resultView = findViewById(R.id.result_view);
    statusView = findViewById(R.id.status_view);
    status_view_flashlight = (ToggleButton)findViewById(R.id.status_view_flashlight);
    status_view_flashlight.setOnCheckedChangeListener(new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if(isChecked)FlashlightManager.enableFlashlight();
			else FlashlightManager.disableFlashlight();
			
		}
    });
    handler = null;
    lastResult = null;
    hasSurface = false;
    historyManager = new HistoryManager(this);
    historyManager.trimHistory();
    
    media = BeepAndVibrate.getInstance();
    
    PushAgent.getInstance(this).onAppStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    MobclickAgent.onResume(this);

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
//    Intent intent = getIntent();

    // Scan all formats and handle the results ourselves (launched from Home).
    decodeFormats = null;
    //中文汉字显示请设计字符集为UTF-8
    characterSet = "UTF8";

  }


  @Override
  protected void onPause() {
    super.onPause();
    MobclickAgent.onPause(this);
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    CameraManager.get().closeDriver();
    if (!hasSurface) {
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.removeCallback(this);
      }
  }
  
  @Override
  public void onDestroy() {
	  super.onDestroy();
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (lastResult != null) {
        resetStatusView();
        if (handler != null) {
          handler.sendEmptyMessage(R.id.restart_preview);
        }
        return true;
      }
    } else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
      // Handle these events so they don't launch the Camera app
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
//    menu.add(0, INPUT_ID, 0, R.string.menu_input)
//        .setIcon(android.R.drawable.ic_menu_share);
    menu.add(0, HISTORY_ID, 0, R.string.menu_history)
        .setIcon(android.R.drawable.ic_menu_recent_history);
//    menu.add(0, SETTINGS_ID, 0, R.string.menu_setting)
//        .setIcon(android.R.drawable.ic_menu_preferences);
//    menu.add(0, HELP_ID, 0, R.string.menu_help)
//        .setIcon(android.R.drawable.ic_menu_help);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
	  Intent intent = new Intent(Intent.ACTION_VIEW);
	  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      switch (item.getItemId()) {
//      case INPUT_ID: {
//    	Intent getBID = new Intent(this,KeyInputActivity.class);
//    	this.startActivity(getBID);
//        break;
//      }
      case HISTORY_ID: {
    	intent.setClassName(this, HistoryActivity.class.getName());
        startActivityForResult(intent, HISTORY_REQUEST_CODE);
        break;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onConfigurationChanged(Configuration config) {
    // Do nothing, this is to prevent the activity from being restarted when the keyboard opens.
    super.onConfigurationChanged(config);
  }

  public void surfaceCreated(SurfaceHolder holder) {
	  if (holder == null) {
	      Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
	  }
      if (!hasSurface) {
          hasSurface = true;
          initCamera(holder);
      }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show the results.
   *
   * @param rawResult The contents of the barcode.
   * @param barcode   A greyscale bitmap of the camera data which was decoded.
   */
  public void handleDecode(Result rawResult, Bitmap barcode) {
	lastResult = rawResult;
    ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
    if (barcode != null) {
    	media.playBeepSoundAndVibrate();
    	historyManager.addHistoryItem(rawResult, resultHandler);
   	    drawResultPoints(barcode, rawResult);
    }
    handleDecodeInternally(rawResult, barcode, resultHandler);
   
  }

  /**
   * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
   *
   * @param barcode   A bitmap of the captured image.
   * @param rawResult The decoded results which contains the points to draw.
   */
  private void drawResultPoints(Bitmap barcode, Result rawResult) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      Canvas canvas = new Canvas(barcode);
      Paint paint = new Paint();
      paint.setColor(getResources().getColor(R.color.result_image_border));
      paint.setStrokeWidth(3.0f);
      paint.setStyle(Paint.Style.STROKE);
      Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
      canvas.drawRect(border, paint);

      paint.setColor(getResources().getColor(R.color.result_points));
      if (points.length == 2) {
        paint.setStrokeWidth(4.0f);
        canvas.drawLine(points[0].getX(), points[0].getY(), points[1].getX(),
            points[1].getY(), paint);
      } else {
        paint.setStrokeWidth(10.0f);
        for (ResultPoint point : points) {
          canvas.drawPoint(point.getX(), point.getY(), paint);
        }
      }
    }
  }

  // Put up our own UI for how to handle the decoded contents.
  private void handleDecodeInternally(Result rawResult, Bitmap barcode, ResultHandler resultHandler) {
    statusView.setVisibility(View.GONE);
    viewfinderView.setVisibility(View.GONE);
    resultView.setVisibility(View.VISIBLE);

    ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
    barcodeImageView.setVisibility(View.VISIBLE);
//    if (resultHandler instanceof MyDeviceResultHandler) {
//    	MyDeviceResultHandler myhomeResultHandler = (MyDeviceResultHandler) resultHandler;
//    	myhomeResultHandler.setParseOperation(mScanTask);
//    } else {
//    	 barcodeImageView.setImageBitmap((barcode == null?PhotoManagerUtilsV2.getInstance().getDefaultBitmap():barcode));
//    }
    
    if (resultHandler instanceof HaierResultHandler) {
    	HaierResultHandler myhomeResultHandler = (HaierResultHandler) resultHandler;
		myhomeResultHandler.setParseOperation(mScanTask);
	}
    
    if (resultHandler instanceof AddressBookResultHandler) {
    	AddressBookResultHandler myResultHandler = (AddressBookResultHandler) resultHandler;
    	myResultHandler.setParseOperation(mScanTask);
	}
    
    barcodeImageView.setImageBitmap((barcode == null?PhotoManagerUtilsV2.getInstance().getDefaultBitmap():barcode));

    TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
    formatTextView.setVisibility(View.VISIBLE);
    formatTextView.setText(getString(R.string.msg_default_format) + ": " + rawResult.getBarcodeFormat().toString());
    
    TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
    
    String mType = resultHandler.getType().toString();
    typeTextView.setText(getString(R.string.msg_default_type) + ": " + mType);
    
    
    TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
  //getDisplayTitle()��ResultHandler�ж���Ľ�����
    CharSequence title = getString(resultHandler.getDisplayTitle());
    SpannableStringBuilder styled = new SpannableStringBuilder(title + "\n\n");
    styled.setSpan(new UnderlineSpan(), 0, title.length(), 0);
    
    //getDisplayContents()���õ���ResultHandler.getDisplayResult()
    CharSequence displayContents = resultHandler.getDisplayContents();
    styled.append(displayContents);
    contentsTextView.setText(styled);
    
    int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
    contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

    int buttonCount = resultHandler.getButtonCount();
    ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
    buttonView.requestFocus();
    for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
      TextView button = (TextView) buttonView.getChildAt(x);
      if (x < buttonCount) {
        button.setVisibility(View.VISIBLE);
        button.setText(resultHandler.getButtonText(x));
        button.setOnClickListener(new ResultButtonListener(resultHandler, x));
      } else {
        button.setVisibility(View.GONE);
      }
    }

  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    try {
      CameraManager.get().openDriver(surfaceHolder);
      if (handler == null) {
//          boolean beginScanning = lastResult == null;
          handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
      }
      decodeOrStoreSavedBitmap(null, null);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
      return;
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.e(TAG, e.toString());
      displayFrameworkBugMessageAndExit();
      return;
    }
    
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialogInterface, int i) {
        finish();
      }
    });
    builder.show();
  }

  public void resetStatusView() {
    resultView.setVisibility(View.GONE);
    statusView.setVisibility(View.VISIBLE);
//    statusView.setBackgroundColor(getResources().getColor(R.color.status_view));
    viewfinderView.setVisibility(View.VISIBLE);

//    TextView textView = (TextView) findViewById(R.id.status_text_view);
//    textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
//    textView.setTextSize(14.0f);
//    textView.setText(R.string.msg_default_status);
    lastResult = null;
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (resultCode != RESULT_OK) return;
	    if (requestCode == HISTORY_REQUEST_CODE) {
	        int itemNumber = data.getIntExtra(Intents.History.ITEM_NUMBER, -1);
	        if (itemNumber >= 0) {
	          HistoryItem historyItem = historyManager.buildHistoryItem(itemNumber);
	          decodeOrStoreSavedBitmap(null, historyItem.getResult());
	        }
	      }
    }
    
    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
          savedResultToShow = result;
        } else {
          if (result != null) {
            savedResultToShow = result;
          }
          if (savedResultToShow != null) {
            Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
            handler.sendMessage(message);
          }
          savedResultToShow=null;
        }
      }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(Intents.EXTRA_SCAN_TASK, false);
		super.onSaveInstanceState(outState);
	}
    
    
  
}
