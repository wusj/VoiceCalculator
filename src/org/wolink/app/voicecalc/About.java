package org.wolink.app.voicecalc;

import com.umeng.analytics.MobclickAgent;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;


public class About extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
        TextView txtv_main_title = (TextView)findViewById(R.id.txtv_main_title);
        try {
        	txtv_main_title.setText(String.format(getString(R.string.app_title), 
        		getString(R.string.app_name),
        		this.getPackageManager().getPackageInfo("org.wolink.app.voicecalc", 0).versionName));
        }
        catch (Throwable t) {
        	
        }
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
	
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
}
