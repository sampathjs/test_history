
/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * Description: 
 * 		Utilities for metals balance report.
 * 
 * Project : Metals balance sheet
 * Customer : Johnson Matthey Plc. 
 * last modified date : 29/October/2015
 * 
 * @author:  Douglas Connolly /OpenLink International Ltd.
 * @modified by :
 * @version   1.0 // Initial Release
 */

package com.jm.rbreports.BalanceSheet;

import com.olf.openjvs.*;

public class Utils
{
	static public void removeTable(Table data) throws OException 
	{
		if (Table.isTableValid(data) == 1){
			data.destroy();
		}
	}

}