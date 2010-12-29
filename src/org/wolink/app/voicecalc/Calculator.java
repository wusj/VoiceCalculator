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

import net.youmi.android.AdManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class Calculator extends Activity {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private PanelSwitcher mPanelSwitcher;

    private static final int CMD_CLEAR_HISTORY  = 1;
    private static final int CMD_BASIC_PANEL    = 2;
    private static final int CMD_ADVANCED_PANEL = 3;

    private static final int HVGA_WIDTH_PIXELS  = 320;

    static final int BASIC_PANEL    = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean DEBUG  = false;
    private static final boolean LOG_ENABLED = DEBUG ? Config.LOGD : Config.LOGV;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    SoundManager sm;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        
        try{
        	String version = this.getPackageManager().
				getPackageInfo("org.wolink.app.voicecalc", 0).versionName;
        	AdManager.init("be8e48d9d8eebbad", "729d721df3655af8", 30, false, version);  
        }
        catch (Throwable t) {
        	
        }
        sm = SoundManager.getInstance();
        sm.initSounds(this);
        sm.addSound("1", R.raw.one, 250);
        sm.addSound("2", R.raw.two, 280);
        sm.addSound("3", R.raw.three, 350);
        sm.addSound("4", R.raw.four, 300);
        sm.addSound("5", R.raw.five, 270);
        sm.addSound("6", R.raw.six, 260);
        sm.addSound("7", R.raw.seven, 350);
        sm.addSound("8", R.raw.eight, 270);
        sm.addSound("9", R.raw.nine, 270);
        sm.addSound("0", R.raw.zero, 340);
        sm.addSound("AC", R.raw.ac, 460);
        sm.addSound("DEL", R.raw.del, 580);
        sm.addSound("+", R.raw.plus, 400);
        sm.addSound(getString(R.string.minus), R.raw.minus, 320);
        sm.addSound(getString(R.string.mul), R.raw.mul, 480);
        sm.addSound(getString(R.string.div), R.raw.div, 460);
        sm.addSound("=", R.raw.equal, 500);
        sm.addSound(".", R.raw.dot, 290);
        
        setContentView(R.layout.main);

        mPersist = new Persist(this);
        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay, (Button) findViewById(R.id.equal));
        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        mPanelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);
        mPanelSwitcher.setCurrentIndex(state==null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));

        mListener.setHandler(mLogic, mPanelSwitcher);

        mDisplay.setOnKeyListener(mListener);

//        View view;
//        if ((view = findViewById(R.id.del)) != null) {
//            view.setOnClickListener(mListener);
//            view.setOnLongClickListener(mListener);
//        }
        /*
        if ((view = findViewById(R.id.clear)) != null) {
            view.setOnClickListener(mListener);
        }
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item;
        
        item = menu.add(0, CMD_CLEAR_HISTORY, 0, R.string.clear_history);
        item.setIcon(R.drawable.clear_history);
        
        item = menu.add(0, CMD_ADVANCED_PANEL, 0, R.string.advanced);
        item.setIcon(R.drawable.advanced);
        
        item = menu.add(0, CMD_BASIC_PANEL, 0, R.string.basic);
        item.setIcon(R.drawable.simple);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(CMD_BASIC_PANEL).setVisible(mPanelSwitcher != null && 
                          mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL);
        
        menu.findItem(CMD_ADVANCED_PANEL).setVisible(mPanelSwitcher != null && 
                          mPanelSwitcher.getCurrentIndex() == BASIC_PANEL);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case CMD_CLEAR_HISTORY:
            mHistory.clear();
            break;

        case CMD_BASIC_PANEL:
            if (mPanelSwitcher != null && 
                mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL) {
                mPanelSwitcher.moveRight();
            }
            break;

        case CMD_ADVANCED_PANEL:
            if (mPanelSwitcher != null && 
                mPanelSwitcher.getCurrentIndex() == BASIC_PANEL) {
                mPanelSwitcher.moveLeft();
            }
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
}
