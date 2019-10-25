package com.openlink.jm.bo.docoutput;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.logging.PluginLog;

class TokenHandler
{
	private final static String TOKEN_DELIMITER = "%";

	private Table tblUserData = null;
	private HashMap<String, String> mapDateTimeToken = null;
	private int _date = -1, _time = -1;
	private boolean isCancellationConfigSet = false, isCancellation = false;
	private String strCancelledSuffix = "C_%olfStlDocInfo_CancelDocNum%";

	void setCancellationParams(boolean isCancellation, String strCancelledSuffix)
	{
		this.isCancellation = isCancellation; //this.strCancelledSuffix = strCancelledSuffix;
		isCancellationConfigSet = true;
	}
	void setUserDataTable(Table userDataTable)
	{
		tblUserData = userDataTable;
	}

	HashMap<String, String> getDateTimeTokenMap() throws OException
	{
		createDateTimeMap();
		return mapDateTimeToken;
	}

	HashMap<String, String> getDateTimeTokenMap(int date, int time) throws OException
	{
		createDateTimeMap(date, time);
		return mapDateTimeToken;
	}

	void createDateTimeMap() throws OException
	{
		int date, time;
		date = OCalendar.getServerDate();
		time = Util.timeGetServerTime();
		createDateTimeMap(date, time);
	}

	void createDateTimeMap(int date, int time) throws OException
	{
		if (mapDateTimeToken == null)
		{
			_date = date; _time = time;
			mapDateTimeToken = createDateTimeReplaceMap(date, time);
		}
		else if (date != _date || time != _time)
		{
			mapDateTimeToken.clear();
			mapDateTimeToken = null;
			_date = date; _time = time;
			mapDateTimeToken = createDateTimeReplaceMap(date, time);
		}
	}

	// collect in-between results for determining endless-loops
//	private LinkedHashMap<String, String> map = null;
	final String enhanceTokens(String str, String name)
	{
		if (str != null && str.contains(TOKEN_DELIMITER))
		{
//			map = new LinkedHashMap<String, String>();
			String enh = str, bak;
			while (true)
			{
				try
				{
					bak = enh;
					if (!enh.contains(TOKEN_DELIMITER)) break; else enh = enhanceDateTimeTokens(enh);
					if (!enh.contains(TOKEN_DELIMITER)) break; else enh = enhanceUserDataTokens(enh);
					if (!enh.contains(TOKEN_DELIMITER)) break; else enh = enhanceEnvVarTokens(enh);
					if (!enh.contains(TOKEN_DELIMITER)) break; else enh = enhanceConstRepoTokens(enh);
					if (bak.toString().equals(enh.toString())) throw new Exception("Couldn't enhance all tokens");
				}
				catch (Throwable t) { PluginLog.error(t.getMessage()); break; }
			}
			PluginLog.debug(name+":\n"+str+"\n"+enh);
			str = enh;
		}
		return str;
	}
	private String enhanceDateTimeTokens(String str) throws OException
	{
		if (mapDateTimeToken == null)
			createDateTimeMap();
		HashMap<String, String> map = mapDateTimeToken;

		if (str != null)
			for (String key : map.keySet())
				if (str.contains(key))
					str = str.replaceAll(key, map.get(key));

		return str;
	}
	private String enhanceUserDataTokens(String str) throws OException
	{
		return replaceSpecialFields(str, tblUserData);
	}
	private String enhanceEnvVarTokens(String str) throws OException
	{
		int beginIndex, endIndex = -1;
		String strRet, strToken;
		if (str != null)
			while ((beginIndex = str.indexOf("%", endIndex + 1)) >= 0)
				if ((endIndex = str.indexOf("%", beginIndex + 1)) >= 0)
				{
					strToken = str.substring(beginIndex, endIndex + 1);
					try
					{
						strRet = Util.getEnv(str.substring(beginIndex + 1, endIndex));
						if (strRet != null)
						{
							str = str.replaceFirst(str.substring(beginIndex, endIndex + 1), strRet);
						//	PluginLog.debug("Replaced '" + strToken + "' with '" + strRet + "'");
							endIndex = beginIndex + strRet.length();
						}
//						else
//							endIndex = beginIndex + strToken.length();
					}
					catch (OException oe)
					{
						throw new OException(strToken + ": " + oe.getMessage());
					}
				}
				else
					break;
		return str;
	}
	private String enhanceConstRepoTokens(String str)
	{
		return str;
	}
	@SuppressWarnings("unused")
	private String getToken(String str, boolean keepTokenIdentifier, int index) throws OException
	{
		int beginIndex = index, endIndex = index;
		beginIndex = str.indexOf(TOKEN_DELIMITER, endIndex + 1);
		endIndex = str.indexOf(TOKEN_DELIMITER, beginIndex + 1);
		if (beginIndex <= 0)
			return null;
		if (endIndex <= beginIndex)
			throw new OException("Token must be surrounded with '"+TOKEN_DELIMITER+"' - second '"+TOKEN_DELIMITER+"' is missing: "+str.substring(beginIndex));
		if (keepTokenIdentifier)
			return str.substring(beginIndex, endIndex + 1);
		return str.substring(beginIndex + 1, endIndex);
	}

	final String replaceTokens(String str, Table tbl, HashMap<String,String> map, String comment) throws OException
	{
		if (comment == null) comment = "";
		else if (comment.length() > 0) comment += ": '";

		if (str != null)
		{
			if (str.length() > 0 && str.indexOf("%") >= 0)
			{
				PluginLog.info("Handling:  " + comment + str + "'");
			//	str = replaceDateTimeTokens(str, map);
				str = enhanceDateTimeTokens(str);
			//	PluginLog.debug("Handling:  " + comment + str + "'");
				str = enhanceEnvVarTokens(str);
			//	PluginLog.debug("Handling:  " + comment + str + "'");
				str = replaceSpecialFields(str, tbl);
				PluginLog.info("Returning: " + comment + str + "'");
			}
			else
				PluginLog.debug("Nothing to do for: '" + str + "'");
		}
		return str;
	}

	private final String replaceTokens(String str, HashMap<String,String> map, String comment) throws OException
	{
		if (comment == null) comment = "";
		else if (comment.length() > 0) comment += ": '";

		if (str != null)
		{
			if (str.length() > 0 && str.indexOf("%") >= 0)
			{
			//	PluginLog.info("Handling:  " + comment + str + "'");
			//	str = replaceDateTimeTokens(str, map);
				str = enhanceDateTimeTokens(str);
			//	PluginLog.info("Returning: " + comment + str + "'");
			}
//			else
//				PluginLog.debug("Nothing to do for: '" + str + "'");
		}
		return str;
	}

	private final String replaceTokens(String str, Table tbl, String comment) throws OException
	{
		if (comment == null) comment = "";
		else if (comment.length() > 0) comment += ": '";

		if (str != null)
		{
			if (str.length() > 0 && str.indexOf("%") >= 0)
			{
				PluginLog.info("Handling:  " + comment + str + "'");
				str = replaceSpecialFields(str, tbl);
				PluginLog.info("Returning: " + comment + str + "'");
			}
			else
				PluginLog.debug("Nothing to do for: '" + str + "'");
		}
		return str;
	}


	private String replaceDateTimeTokens(String strVal, HashMap<String, String> map)
	{
		/*
		if (strVal != null)
			for (Map.Entry<String, String> entry : map.entrySet())
				strVal = strVal.replaceAll(entry.getKey(), entry.getValue());
		*/

		if (strVal != null)
			for (String key : map.keySet())
				if (strVal.contains(key))
					strVal = strVal.replaceAll(key, map.get(key));

		return strVal;
	}

	private String replaceSpecialFields(String strVal, Table tbl) throws OException
	{
		int beginIndex, endIndex = -1;
		String strRet, strToken;
		if (strVal != null)
			if (isCancellation)
				while ((beginIndex = strVal.indexOf("%", endIndex + 1)) >= 0)
					if ((endIndex = strVal.indexOf("%", beginIndex + 1)) >= 0)
					{
						strToken = strVal.substring(beginIndex, endIndex + 1);
						try
						{
							strRet = strVal.substring(beginIndex + 1, endIndex);
							if ("OutputSeqNum".equalsIgnoreCase(strRet))
							{
								PluginLog.info("Doc is Cancellation - overwrite OutputSeqNum(" + getUserData(tbl, strRet) + ") with '" + strCancelledSuffix + "'");
								strRet = strCancelledSuffix;
							}
							else
								strRet = getUserData(tbl, strRet);
							strVal = strVal.replaceFirst(strVal.substring(beginIndex, endIndex + 1), strRet);
//							PluginLog.debug("Replaced '" + strToken + "' with '" + strRet + "'");
							endIndex = beginIndex + strRet.length();
						}
						catch (OException oe)
						{
							PluginLog.warn(strToken + ": " + oe.getMessage());
						}
					}
					else
						break;
			else
				while ((beginIndex = strVal.indexOf("%", endIndex + 1)) >= 0)
					if ((endIndex = strVal.indexOf("%", beginIndex + 1)) >= 0)
					{
						strToken = strVal.substring(beginIndex, endIndex + 1);
						try
						{
							strRet = getUserData(tbl, strVal.substring(beginIndex + 1, endIndex));
							strVal = strVal.replaceFirst(strVal.substring(beginIndex, endIndex + 1), strRet);
//							PluginLog.debug("Replaced '" + strToken + "' with '" + strRet + "'");
							endIndex = beginIndex + strRet.length();
						}
						catch (OException oe)
						{
							PluginLog.warn(strToken + ": " + oe.getMessage());
						}
					}
					else
						break;
		return strVal;
	}

	private void verifyPlaceHolders(String strVal, String strDelimiter) throws OException
	{
		if (strVal == null)
			throw new OException("String value is [null].");
		if (strDelimiter == null)
			throw new OException("Delimiter string is [null].");
		if (strDelimiter.length() == 0)
			throw new OException("Delimiter string is empty.");

		int beginIndex, endIndex = -1;
		while ((beginIndex = strVal.indexOf(strDelimiter, endIndex + 1)) >= 0)
			if ((endIndex = strVal.indexOf(strDelimiter, beginIndex + 1)) < 0)
				throw new OException("'" + strVal + "' has not only pairs of '" + strDelimiter + "'");
	}

	protected final String getUserData(Table tblUserData, String paramName) throws OException
	{
		try
		{
			int row = tblUserData.findString("col_name", paramName, SEARCH_ENUM.FIRST_IN_GROUP);
			if (row < 1)
				throw new OException("parameter not found");
			String value;
			if (tblUserData.getInt("col_type", row) != 2)
				value = tblUserData.getString("col_data", row);
			else
				value = tblUserData.getTable("doc_table", row).getString(1, 1);
			return value;
		}
		catch (OException e)
		{
			throw new OException("Failed to get value for parameter '" + paramName + "' from user data table: " + e.getMessage());
		}
	}

	private HashMap<String, String> createDateTimeReplaceMap(int date, int time) throws OException
	{
		HashMap<String, String> map = new HashMap<String, String>();

		String strYear4, 
			   strYear2, 
			   strMonth, 
			   strDay, 
			   strHour, 
			   strMinute, 
			   strSecond;

		strMonth = Str.intToStr(OCalendar.getMonth(date));
		map.put("%mm%", strMonth.length() > 1 ? strMonth : "0" + strMonth);

		strDay = Str.intToStr(date - OCalendar.getSOM(date) + 1);//why this way?
		strDay = Str.intToStr(OCalendar.getDay(date));
		map.put("%dd%", strDay.length() > 1 ? strDay : "0" + strDay);

		strYear4 = Str.intToStr(OCalendar.getYear(date));
		map.put("%yyyy%", strYear4);
		strYear2 = Str.substr(Str.intToStr(OCalendar.getYear(date)), 2, 2);
		map.put("%yy%", strYear2);

		int intSecond = time % 60;
		strSecond = (intSecond < 10 ? "0" : "") + intSecond;
		map.put("%ss%", strSecond);

		time -= intSecond; time /= 60;

		int intMinute = time % 60;
		strMinute = (intMinute < 10 ? "0" : "") + intMinute;
		map.put("%mi%", strMinute);

		time -= intMinute; time /= 60;

	//	strHour = Str.intToStr(time);
		strHour = (time < 10 ? "0" : "") + time;
		map.put("%hh%", strHour);

//		for (Map.Entry<String, String> entry : map.entrySet())
//			PluginLog.debug(entry.getKey() + "=" + entry.getValue());
		PluginLog.debug("TokenMap: " + map.toString());

		return map;
	}

}
