package org.wolink.app.voicecalc;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.UMFeedbackService;

public class Settings extends PreferenceActivity implements Preference.OnPreferenceClickListener{
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);		
		
		Preference fb_bug = findPreference("fb_bug");  
		fb_bug.setOnPreferenceClickListener(this);   		
	}
	
	public void onResume() {
	    super.onResume();
	    MobclickAgent.onResume(this);
	}
	
	public void onPause() {
	    super.onPause();
	    MobclickAgent.onPause(this);
	}
	
	public boolean onPreferenceClick(Preference preference) {  
		if (preference.getKey().equals("fb_bug")) {
			UMFeedbackService.openUmengFeedbackSDK(this);
			return true;
		}
        return false;  
    } 
}
