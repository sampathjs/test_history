package com.jm.ftp;

import java.io.File;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author FernaI01
 * 
 */


@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public abstract class FTP 
{
	
	public static ConstRepository repository = null;

	public abstract void put(String strFilePathFileName) throws Exception;
	
	public abstract void get() throws Exception;
	
	public abstract void ls() throws Exception;
	
}
