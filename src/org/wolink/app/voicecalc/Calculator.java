/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.wolink.app.voicecalc;

import net.youmi.android.appoffers.YoumiOffersManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Config;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.NotificationType;
import com.umeng.fb.UMFeedbackService;
import com.umeng.update.UmengUpdateAgent;

public class Calculator extends Activity {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private PanelSwitcher mPanelSwitcher;

    private static final int CMD_SETTINGS		  = 4;
    private static final int CMD_ABOUT			  = 5;
    private static final int CMD_CAPITAL		  = 6;
    private static final int CMD_MOREAPP		  = 7;
    
    private static final int DIALOG_LOADING_VOICEPKG = 1;

    private static final int HVGA_WIDTH_PIXELS  = 480;

    static final int BASIC_PANEL    = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean LOG_ENABLED = Config.LOGD;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    private SoundManager sm;
    private String mVoicePkg;
    private SoundLoadTask mLoadingTask;
       
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
       	YoumiOffersManager.init(this, "be8e48d9d8eebbad", "729d721df3655af8");

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
 
    	mVoicePkg = "";
    	
        sm = SoundManager.getInstance();
        sm.initSounds(this);
              
        setContentView(R.layout.main);

        mPersist = new Persist(this);
        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mHistory.clear();
        mLogic = new Logic(this, mHistory, mDisplay, (Button) findViewById(R.id.equal));
        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        mPanelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);
        mPanelSwitcher.setCurrentIndex(state==null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));

        mListener.setHandler(mLogic, mPanelSwitcher);    
        
        MobclickAgent.onError(this);
        MobclickAgent.updateOnlineConfig(this);
        UMFeedbackService.enableNewReplyNotification(this, NotificationType.AlertDialog);
        UmengUpdateAgent.update(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item;

      item = menu.add(0, CMD_CAPITAL, 0, R.string.convert_capital);
      item.setIcon(R.drawable.currency_black_yuan);

      item = menu.add(0, CMD_SETTINGS, 0, R.string.setting);
      item.setIcon(R.drawable.setting);
      item.setIntent(new Intent(this, Settings.class));
      
      if (MobclickAgent.getConfigParams(this, "OpenMoreApp").equalsIgnoreCase("1")) {
    	  item = menu.add(0, CMD_MOREAPP, 0, R.string.moreapp);
    	  item.setIcon(R.drawable.ic_menu_recommend);
      }

      item = menu.add(0, CMD_ABOUT, 0, R.string.about);
      item.setIcon(R.drawable.about);
      item.setIntent(new Intent(this, About.class));
      
      return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
   
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case CMD_SETTINGS:
        	break;
        case CMD_ABOUT:
        	break;
        case CMD_CAPITAL:
        	String chineseDigit = "";
        	try {
        		chineseDigit = ChineseDigit.toChineseDigit(mDisplay.getText().toString());
        	} catch (Exception e) {
        	}
        	
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.RMB_CHINESE);
    		if (chineseDigit.equals("")) {
    			builder.setIcon(android.R.drawable.ic_dialog_alert);
    			builder.setMessage(R.string.error_number);
    		} else {
        		builder.setMessage(chineseDigit);	
    		}

    		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dlg, int sumthin) {
    				dlg.dismiss();
    			}
    		});
    		
    		builder.setCancelable(true);
    		builder.show();
        	break;
        case CMD_MOREAPP:
        	YoumiOffersManager.showOffers(this, YoumiOffersManager.TYPE_REWARDLESS_APPLIST);
        	break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(STATE_CURRENT_VIEW, mPanelSwitcher.getCurrentIndex());     
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogic.updateHistory();
        mPersist.save();
        MobclickAgent.onPause(this);
    }
    
    @Override
    public void onResume() {
        super.onResume();
 
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
    	
    	boolean bVoiceOn = prefs.getBoolean("voice_on", true);
    	boolean bHapticOn = prefs.getBoolean("haptic_on", true);
    	String pkg = prefs.getString("voice_pkg", "default");
    	mListener.mbVoice = bVoiceOn;
    	mListener.mbHaptic = bHapticOn;
    	if (bVoiceOn && !pkg.equals(mVoicePkg)) {
    		mVoicePkg = pkg;
    		mLoadingTask = ( SoundLoadTask ) new SoundLoadTask().execute(sm);
    	}
    	MobclickAgent.onResume(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoadingTask != null && mLoadingTask.getStatus() == AsyncTask.Status.RUNNING) {
        	mLoadingTask.cancel(true);
        	mLoadingTask = null;
        }
    }    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK 
            && mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL) {
            mPanelSwitcher.moveRight();
            return true;
        } else {
            return super.onKeyDown(keyCode, keyEvent);
        }
    }

    static void log(String message) {
        if (LOG_ENABLED) {
            Log.v(LOG_TAG, message);
        }
    }

    /**
     * The font sizes in the layout files are specified for a HVGA display.
     * Adjust the font sizes accordingly if we are running on a different
     * display.
     */
    public void adjustFontSize(TextView view) {
        float fontPixelSize = view.getTextSize();
        Display display = getWindowManager().getDefaultDisplay();
        int h = Math.min(display.getWidth(), display.getHeight());
        float ratio = (float)h/HVGA_WIDTH_PIXELS;
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPixelSize*ratio);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
    		case DIALOG_LOADING_VOICEPKG:
    			ProgressDialog dialog = new ProgressDialog(this);
    			dialog.setMessage(getString(R.string.loadingvoice));
    			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    			dialog.setCancelable(false);
    			
    			return dialog;
  
    	}
    	return null;
	}
    
	class SoundLoadTask extends AsyncTask<SoundManager, Void, Void> {  
		SoundLoadTask() {
			super();
		}
		
		@Override  
		protected void onPreExecute() {  
			showDialog(DIALOG_LOADING_VOICEPKG);
		}  		
		
		@Override
		protected Void doInBackground(SoundManager... sm) { 
			sm[0].unloadAll();
			
		    sm[0].addSound("1", R.raw.one, 320);
		    sm[0].addSound("2", R.raw.two, 274);
		    sm[0].addSound("3", R.raw.three, 304);
		    sm[0].addSound("4", R.raw.four, 215);
		    sm[0].addSound("5", R.raw.five, 388);
		    sm[0].addSound("6", R.raw.six, 277);
		    sm[0].addSound("7", R.raw.seven, 447);
		    sm[0].addSound("8", R.raw.eight, 274);
		    sm[0].addSound("9", R.raw.nine, 451);
		    sm[0].addSound("0", R.raw.zero, 404);
		    sm[0].addSound("AC", R.raw.ac, 696);
		    sm[0].addSound("DEL", R.raw.del, 442);
		    sm[0].addSound("+", R.raw.plus, 399);
		    sm[0].addSound(getString(R.string.minus), R.raw.minus, 530);
		    sm[0].addSound(getString(R.string.mul), R.raw.mul, 321);
		    sm[0].addSound(getString(R.string.div), R.raw.div, 321);
		    sm[0].addSound("=", R.raw.equal, 480);
		    sm[0].addSound(".", R.raw.dot, 454);
		        
	        return null;
		}  
		
        @Override
        public void onCancelled() {
        	try {
        		dismissDialog(DIALOG_LOADING_VOICEPKG);
        	} catch (Exception e) {
        		log("" + e);
        	}
        }	
        
		@Override  
		protected void onPostExecute(Void n) {  
			dismissDialog(DIALOG_LOADING_VOICEPKG);
		} 
	}  
}
