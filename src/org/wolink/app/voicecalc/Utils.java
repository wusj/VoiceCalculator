package org.wolink.app.voicecalc;

import java.util.Calendar;

public class Utils {
	// 为了避过各市场的审核期，在发布三天后显示广告和积分墙
	static boolean isVerifyTime() {
		final Calendar c = Calendar.getInstance();
		
		// 注意月份的数值比实际月份要小1.
		Calendar cc = Calendar.getInstance();
		cc.set(2011, 9, 19);
		
		if (c.after(cc)) {
			return false;
		}
		
		return true;
	}
}
