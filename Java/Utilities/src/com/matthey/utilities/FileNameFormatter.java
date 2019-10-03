package com.matthey.utilities;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Util;
import com.openlink.util.logging.PluginLog;

public class FileNameFormatter {

	public static String FileName (String strFileName) throws OException {
					String strFilename;
					StringBuilder fileName = new StringBuilder();

					try {

						ODateTime.getServerCurrentDateTime().toString().split(" ");
						Ref.getInfo();
						fileName.append(Util.reportGetDirForToday()).append("\\");
						fileName.append(strFileName);
						fileName.append("_");
						fileName.append(OCalendar.formatDateInt(OCalendar.today()));
						fileName.append(".csv");
					} catch (OException e) {
						PluginLog.info("Unable to format name of  Report for the day");
						throw e;
					}
					strFilename = fileName.toString();

					return strFilename;
				}

		
	}

