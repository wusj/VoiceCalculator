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

import java.util.Calendar;
import java.util.List;

import net.youmi.android.AdManager;
import net.youmi.android.AdView;
import net.youmi.android.AdViewListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Config;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class Calculator extends Activity implements AdViewListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private PanelSwitcher mPanelSwitcher;

    private static final int CMD_SETTINGS		  = 4;
    private static final int CMD_ABOUT			  = 5;
    private static final int CMD_CAPITAL		  = 6;

    private static final int HVGA_WIDTH_PIXELS  = 320;

    static final int BASIC_PANEL    = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean LOG_ENABLED = Config.LOGD;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    private SoundManager sm;
    private String mVoicePkg;
    
    private net.youmi.android.AdView adView;
    
    static {
    	//第一个参数为您的应用发布Id
    	//第二个参数为您的应用密码
    	//第三个参数是请求广告的间隔，有效的设置值为30至200，单位为秒
    	//第四个参数是设置测试模式，设置为true时，可以获取测试广告，正式发布请设置此参数为false
    	AdManager.init("be8e48d9d8eebbad", "729d721df3655af8", 30, false);
    }
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
              
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
    	SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);

    	mVoicePkg = "";
        
        sm = SoundManager.getInstance();
        sm.initSounds(this);
              
        setContentView(R.layout.main);
        
        
        //初始化广告视图
        adView = new AdView(this, 0x5A595A, 0xFFFFFFFF, 255);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        //设置广告出现的位置(悬浮于屏幕右上角)
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        //将广告视图加入Activity中
        addContentView(adView, params);
        adView.setAdViewListener(this);

        mPersist = new Persist(this);
        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        boolean history_on = prefs.getBoolean("history_on", false);
        if (!history_on) {
        	mHistory.clear();
        }
        mLogic = new Logic(this, mHistory, mDisplay, (Button) findViewById(R.id.equal));
        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        mPanelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);
        mPanelSwitcher.setCurrentIndex(state==null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));

        mListener.setHandler(mLogic, mPanelSwitcher);       
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
    }
    
    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
    	SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
    	
        boolean ads = prefs.getBoolean("ads", false);
        if (ads) {
        	int year = prefs.getInt("year", 2000);
        	int month = prefs.getInt("month", 1);
        	int day = prefs.getInt("day", 1);
            final Calendar c = Calendar.getInstance();
            int curYear = c.get(Calendar.YEAR); //获取当前年份
            int curMonth = c.get(Calendar.MONTH);//获取当前月份
            int curDay = c.get(Calendar.DAY_OF_MONTH);//获取当前月份的日期号码
            if (year == curYear && month == curMonth && day == curDay){
            	ads = true;
            } else {
            	ads = false;
            }
        }  	
    	
    	boolean bVoiceOn = prefs.getBoolean("voice_on", true);
    	boolean bHapticOn = prefs.getBoolean("haptic_on", true);
    	String pkg = prefs.getString("voice_pkg", "default");
    	mListener.mbVoice = bVoiceOn;
    	mListener.mbHaptic = bHapticOn;
    	if (bVoiceOn && !pkg.equals(mVoicePkg)) {
    		mVoicePkg = pkg;
    		loadSoundTask();
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
    
    private void loadSoundTask() {
    	sm.unloadAll();
        PackageManager pm = getPackageManager();
        List<ProviderInfo> list = null;
        if (!mVoicePkg.equals("default")) { 
        	list = pm.queryContentProviders("org.wolink.app.voicecalc", 
        		getApplicationInfo().uid, 0);
        }
        
        Cursor cursor = null;
        String pkgName = null;
        if (list != null) {        
	        String authority = null;
	        
	        for (int i = 0; i < list.size(); i++) {
	        	ProviderInfo info = list.get(i);
	        	if (mVoicePkg.equals(info.authority)) {
	        		pkgName = info.packageName;
	        		authority = info.authority;
	        	}
	        }
	        
	        if (authority != null) {
	        	cursor = managedQuery(
					Uri.parse("content://" + authority + "/voices"), 
					null, null, null, null);
	        }
        }
		
		if (cursor != null && cursor.moveToFirst()) {
			int keyColumn = cursor.getColumnIndex("key");
			int resIdColumn = cursor.getColumnIndex("resId");
			int timeColumn = cursor.getColumnIndex("time");
			String key;
			int resId;
			int time;
			do {
				key = cursor.getString(keyColumn);
				resId = cursor.getInt(resIdColumn);
				time = cursor.getInt(timeColumn);
				try {
					AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(
						Uri.parse("android.resource://" + pkgName + "/" + resId),
						"r"
						);
					sm.addSound(key, afd, time);
				}
				catch (Throwable t) {
					// Nothing
				}
			} while (cursor.moveToNext());
		} else {
	        sm.addSound("1", R.raw.one, 320);
	        sm.addSound("2", R.raw.two, 274);
	        sm.addSound("3", R.raw.three, 304);
	        sm.addSound("4", R.raw.four, 215);
	        sm.addSound("5", R.raw.five, 388);
	        sm.addSound("6", R.raw.six, 277);
	        sm.addSound("7", R.raw.seven, 447);
	        sm.addSound("8", R.raw.eight, 274);
	        sm.addSound("9", R.raw.nine, 451);
	        sm.addSound("0", R.raw.zero, 404);
	        sm.addSound("AC", R.raw.ac, 696);
	        sm.addSound("DEL", R.raw.del, 442);
	        sm.addSound("+", R.raw.plus, 399);
	        sm.addSound(getString(R.string.minus), R.raw.minus, 530);
	        sm.addSound(getString(R.string.mul), R.raw.mul, 350);
	        sm.addSound(getString(R.string.div), R.raw.div, 350);
	        sm.addSound("=", R.raw.equal, 480);
	        sm.addSound(".", R.raw.dot, 454);
		}    
    }

	@Override
	public void onAdViewSwitchedAd(AdView arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectFailed(AdView arg0) {
		// TODO Auto-generated method stub
		
	}
}
