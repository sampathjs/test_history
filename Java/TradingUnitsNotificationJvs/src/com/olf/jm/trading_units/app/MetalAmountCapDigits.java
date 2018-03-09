package com.olf.jm.trading_units.app;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/*
 * History:
 * 2016-12-13	V1.0	jwaechter	- Initial Version
 */

/**
 * This Event Notification plugins caps the number of digits after the . to 4.
 * Assumption is that it is triggered on value change only.
 * @author jwaechter
 * @version 1.0
 */
public class MetalAmountCapDigits implements IScript {
	@Override
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		
		String newValue 			= argt.getString("new_value", 1);
		StringBuilder sb 	= new StringBuilder();
		int digitsAfterDot = 0;
		boolean afterDigit =false;
		for (int i=0; i < newValue.length(); i++) {
			if (digitsAfterDot >= 4) {
				break;
			}
			String s = newValue.substring(i, i+1);
			if (s.equals(".")) {
				afterDigit = true;
			} else if (afterDigit) {
				digitsAfterDot++;
			}
			sb.append(s);			
		}
		returnt.setString("string_value", 1, sb.toString());
	}

}
