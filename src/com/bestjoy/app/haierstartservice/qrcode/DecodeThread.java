/*
 * Copyright (C) 2008 ZXing authors
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

package com.bestjoy.app.haierstartservice.qrcode;

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.ui.CaptureActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.camera.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.shwy.bestjoy.utils.DebugUtils;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DecodeThread extends Thread {

  public static final String BARCODE_BITMAP = "barcode_bitmap";
  private static final String TAG = "DecodeThread";

  private Handler handler;
  private final CountDownLatch handlerInitLatch;
  private final CaptureActivity activity;
  private final MultiFormatReader multiFormatReader;
  //private final QRCodeReader qrReader;
  private Hashtable<DecodeHintType, Object> hints ;

  public DecodeThread(CaptureActivity activity,
               Vector<BarcodeFormat> decodeFormats,
               String characterSet,
               ResultPointCallback resultPointCallback) {
    this.activity = activity;
    handlerInitLatch = new CountDownLatch(1);

    multiFormatReader = new MultiFormatReader();
    Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
     
    
//qrReader = new QRCodeReader();
//hints = new Hashtable<DecodeHintType, Object>(3);

    // The prefs can't change while the thread is running, so pick them up once here.
    if (decodeFormats == null || decodeFormats.isEmpty()) {
        decodeFormats = new Vector<BarcodeFormat>();
        decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
    }
      hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

    if (characterSet != null) {
      hints.put(DecodeHintType.CHARACTER_SET, characterSet);
    }

    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);

    multiFormatReader.setHints(hints);
    
  }

  public Handler getHandler() {
	  try {
	      handlerInitLatch.await();
	    } catch (InterruptedException ie) {
	      // continue?
	    }
    return handler;
  }

  @Override
  public void run() {
    Looper.prepare();
    handler = new Handler() {
      @Override
      public void handleMessage(Message message) {
        switch (message.what) {
          case R.id.decode:
            decode((byte[]) message.obj, message.arg1, message.arg2);
            break;
          case R.id.quit:
            Looper.myLooper().quit();
            break;
        }
      }
    };
    handlerInitLatch.countDown();
    Looper.loop();
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;
    PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(data, width, height);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    try {
       rawResult = multiFormatReader.decodeWithState(bitmap);

//      rawResult = qrReader.decode(bitmap,hints);//QRCODE������
    } catch (ReaderException re) {
      // continue
    } finally {
       multiFormatReader.reset();
    }

    if (rawResult != null) {

      long end = System.currentTimeMillis();
      if (DebugUtils.DEBUG_DECODE_THREAD) Log.v(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
      Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
      Bundle bundle = new Bundle();
      bundle.putParcelable(BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
      message.setData(bundle);
      message.sendToTarget();
    } else {
      Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
      if (message != null && message.getTarget() != null) message.sendToTarget();
    }
  }
}
