package org.wolink.app.voicecalc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChineseDigit {
	private static String num1[] = {"Áã", "Ò¼", "·¡", "Èþ", "ËÁ", "Îé", "Â½","Æâ","°Æ","¾Á",};  
	private static String num2[] = {"", "Ê°", "°Û", "Çª", "Íò", "ÒÚ", }; // "Õ×", "¼ª", "Ì«", "ÅÄ", "°¬"
	private static String num3[] = {"½Ç", "·Ö"};
	private static String YUAN = "Ôª";
	private static String ZHENG = "Õû";

	public static String toChineseDigit(String number) throws IllegalArgumentException
	{		
		Pattern regex = Pattern.compile("^[-+]?(\\d*)\\.?(\\d*)$");
		Matcher m = regex.matcher(number);
		String ret = "";

		if (m.find()) {
			String number1 = toChineseDigit1(m.group(1));
			String number2 = toChineseDigit2(m.group(2));
			if (number1.equals("") && number2.equals("") ) {
				ret = num1[0] + YUAN + ZHENG;	
			} else if (number1.equals("") ) {
				ret = removeZero(number2);	
			} else if (number2.equals("") ) {
				ret = removeZero(number1) + YUAN + ZHENG;	
			} else {
				ret = removeZero(number1) + YUAN + number2;	
			}
				
		} else {
			throw new IllegalArgumentException();
		}
		
		return ret;
	}
	
	private static String removeZero(String chinese)
	{		
		if (chinese.substring(0,1).equals(num1[0])) {
			return chinese.substring(1);	
		}
		
		return chinese;
	}
	
	private static String toChineseDigit2(String n) {
		String ret = "";
		int len = Math.min(n.length(), num3.length);
		
		for (int i = 0; i < len; i++)
		{
			if (n.charAt(i) == '0') {
				int j = i + 1;  
				while (j < len && n.charAt(j) == '0') ++j;  
				if (j < len)  
					ret += num1[0];  
				i = j - 1;
			} else {
				ret += num1[n.charAt(i) - '0'] + num3[i];				
			}
		}
		
		return ret;
	}
	
	private static String toChineseDigit1(String n) {  
		int len = n.length();  
		   
		if (len <= 5) {  
			String ret = "";  
			for (int i = 0; i < len; ++i) {  
				if (n.charAt(i) == '0') {  
					int j = i + 1;  
					while (j < len && n.charAt(j) == '0') ++j;  
					if (j < len)  
						ret += num1[0];  
					i = j - 1;  
				} else {
					ret = ret + num1[n.substring(i, i + 1).charAt(0) - '0'] + num2[len - i - 1];  
				}
			}  
			return ret;  
		} else if (len <= 8) {  
			String ret = toChineseDigit1(n.substring(0, len - 4));  
			if (ret.length() != 0)  
				ret += num2[4];  
			return ret + toChineseDigit1(n.substring(len - 4));  
		} else {  
			String ret = toChineseDigit1(n.substring(0, len - 8));  
			if (ret.length() != 0)  
				ret += num2[5];  
			return ret + toChineseDigit1(n.substring(len - 8));  
		}  
	}
}