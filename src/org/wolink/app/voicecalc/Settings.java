package org.wolink.app.voicecalc;

import java.util.List;

import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class Settings extends PreferenceActivity implements OnPreferenceChangeListener{
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
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
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
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
	}	
}
