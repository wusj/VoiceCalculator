package org.wolink.app.voicecalc;

import java.util.Calendar;
import java.util.List;

import net.youmi.android.appoffers.YoumiOffersManager;
import net.youmi.android.appoffers.YoumiPointsManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

public class Settings extends PreferenceActivity implements OnPreferenceChangeListener{
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		YoumiOffersManager.init(this, "be8e48d9d8eebbad", "729d721df3655af8");
		
		if (Utils.isVerifyTime()) {
			addPreferencesFromResource(R.xml.settings2);
		} else {
			addPreferencesFromResource(R.xml.settings);			
		}
		ListPreference pref = (ListPreference)findPreference("voice_pkg");
		CharSequence voice = pref.getValue();
		if (voice == null) {
			pref.setValue("default");
		}
		int count = 0;
		
        PackageManager pm = getPackageManager();
        List<ProviderInfo> list = pm.queryContentProviders("org.wolink.app.voicecalc", 
        		getApplicationInfo().uid, 0);
        if (list != null) {
        	count += list.size();
        }
		
        CharSequence[] entries = new CharSequence[count + 1];
        CharSequence[] entryValues = new CharSequence[count + 1];
        entries[0] = getString(R.string.default_voice);
        entryValues[0] = "default";
        
        for (int i = 0; i < count; i++) {
        	ProviderInfo info = list.get(i);
        	entries[i + 1] = info.applicationInfo.loadLabel(pm);
        	entryValues[i + 1] = info.authority;
        }
        
        pref.setEntries(entries);	
        pref.setEntryValues(entryValues);
        
        voice = pref.getValue();
        for (int i = 0; i < count + 1; i++) {
        	if (voice.equals(entryValues[i])) {
        		pref.setSummary(entries[i]);
        		break;
        	}
        }
        pref.setOnPreferenceChangeListener(this);
        
        CheckBoxPreference closeadpref = (CheckBoxPreference)findPreference("closead_on");
        if (closeadpref != null) {
        	closeadpref.setOnPreferenceChangeListener(this);
        }
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("voice_pkg")) {
			ListPreference pref = (ListPreference)preference;
	        CharSequence[] entries = pref.getEntries();
	        CharSequence[] entryValues = pref.getEntryValues();
	        CharSequence voice = (CharSequence)newValue;
	        
	        for (int i = 0; i < entries.length; i++) {
	        	if (voice.equals(entryValues[i])) {
	        		pref.setSummary(entries[i]);
	        		break;
	        	}
	        }
	        return true;
		} else if (preference.getKey().equals("closead_on")) {
			if ((Boolean)newValue == true) {
		    	SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		        int year = prefs.getInt("year", 2000);
		        int month = prefs.getInt("month", 1);
		        int day = prefs.getInt("day", 1);
		        final Calendar c = Calendar.getInstance();
		        int curYear = c.get(Calendar.YEAR); //获取当前年份
		        int curMonth = c.get(Calendar.MONTH);//获取当前月份
		        int curDay = c.get(Calendar.DAY_OF_MONTH);//获取当前月份的日期号码
		        if (year == curYear && month == curMonth && day == curDay){
		          	return true;
		        } 
		        
		        int points = YoumiPointsManager.queryPoints(this);
		        if (points < 15) {
		    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    		builder.setTitle(R.string.point_not_enough);
		    		builder.setIcon(android.R.drawable.ic_dialog_info);
		    		builder.setMessage(getString(R.string.point_not_prompt, points, 15));
		    		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		    			public void onClick(DialogInterface dlg, int sumthin) {
		    				YoumiOffersManager.showOffers(Settings.this, YoumiOffersManager.TYPE_REWARD_OFFERS);
		    			}
		    		});
		    		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		    			public void onClick(DialogInterface dlg, int sumthin) {
		    				dlg.dismiss();
		    			}
		    		});
		    		builder.setCancelable(true);
		    		builder.show();
		    		return false;
		        } else {
		        	YoumiPointsManager.spendPoints(this, 15);
		            SharedPreferences.Editor editor = prefs.edit();
		            editor.putInt("year", curYear);
		            editor.putInt("month", curMonth);
		            editor.putInt("day",curDay);
		            editor.commit();
		            return true;
		        }
			} else {
				return true;
			}
		}
		return true;
	}	
}
