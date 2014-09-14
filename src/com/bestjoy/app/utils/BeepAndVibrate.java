package com.bestjoy.app.utils;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.bestjoy.app.haierstartservice.R;

public class BeepAndVibrate {

	private final OnCompletionListener beepListener = new BeepListener();
	private static final long VIBRATE_DURATION = 200L;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean playBeep;
	private boolean vibrate;
	private Context mContext;
	private MediaPlayer mediaPlayer;
	private Vibrator vibrator;
	
    private static BeepAndVibrate INSTANCE = new BeepAndVibrate();
	
	private BeepAndVibrate() {}
	
	public void setContext(Context context) {
		if (mContext == null) {
			mContext = context;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//			playBeep = prefs.getBoolean(PreferencesActivity.KEY_PLAY_BEEP, true);
//		    vibrate = prefs.getBoolean(PreferencesActivity.KEY_VIBRATE, false);
			playBeep = true;
		    vibrate = true;
		    initBeepSound(R.raw.beep);
		}
	}
	
	public static BeepAndVibrate getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Creates the beep MediaPlayer in advance so that the sound can be triggered with the least
	 * latency possible.
	 */
	private void initBeepSound(int resId) {
	   if (playBeep && mediaPlayer == null) {
	     // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
	     // so we now play on the music stream.
	     mediaPlayer = new MediaPlayer();
	     mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	     mediaPlayer.setOnCompletionListener(beepListener);

	     AssetFileDescriptor file = mContext.getResources().openRawResourceFd(resId);
	     try {
	        mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(),
	            file.getLength());
	        file.close();
	        mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
	        mediaPlayer.prepare();
	      } catch (IOException e) {
	        mediaPlayer = null;
	      }
	    }
	  }
	  /**
	   * When the beep has finished playing, rewind to queue up another one.
	   */
	  private static class BeepListener implements OnCompletionListener {
	    public void onCompletion(MediaPlayer mediaPlayer) {
	      mediaPlayer.seekTo(0);
	    }
	  }
	  
	  public void playBeepSoundAndVibrate() {
		    if (playBeep && mediaPlayer != null) {
		      mediaPlayer.start();
		    }
		    if (vibrate) {
		      Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		      vibrator.vibrate(VIBRATE_DURATION);
		    }
		  }
	  
	  /**
	   * �н�����񶯰�
	   * @param pattern��ģʽ��һ��Long���飬��һ��ֵ����ú�ʼ�񶯣����ž���
	   * @param vRepeat off/on/off/on/off/on ģʽ�е��±꣬��1��ʾ��ѭ�������֮�⣬��ģʽ���±�����ʼѭ����
	   */
	  public void playBeepSoundAndVibrate(long[] pattern, int vRepeat) {
		  
		    if (playBeep && mediaPlayer != null) {
		    	mediaPlayer.setLooping(true);
		      mediaPlayer.start();
		    }
		    if (vibrate) {
		      vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		      vibrator.vibrate(pattern,vRepeat);
		    }
		 }
	  
	 public void stopBeepAndSound() {
		 if(mediaPlayer !=null){
			 mediaPlayer.stop();
		 }
		 if(vibrator !=null)vibrator.cancel();
	 }
}
