package com.matthey.openlink.utilities.legacy;

import com.olf.openjvs.OException;

public class Ask {

	
	public static void ok(final String message) {
		
		try {
			com.olf.openjvs.Ask.ok(message);
		} catch (OException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean okOrCancel(final String message) {
		boolean response = false;
		try {
			if (0 == com.olf.openjvs.Ask.okCancel(message))
				return response;
			
			response=true;
		} catch (OException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}

	public static String yesOrNo(final String message) {
		if (okOrCancel(message))
			return "Yes";
		return "No";
	}

}
