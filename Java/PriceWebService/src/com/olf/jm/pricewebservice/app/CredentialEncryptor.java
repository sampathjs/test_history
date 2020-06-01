package com.olf.jm.pricewebservice.app;

import com.olf.jm.pricewebservice.model.CryptoInterface;
import com.olf.jm.pricewebservice.persistence.CryptoImpl;
import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.jm.pricewebservice.persistence.TpmHelper;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-11-02	V1.0	jwaechter	- initial version
 */


/**
 * This task applies encryption to both user name and password in the user table
 * USER_jm_price_web_ftp_mapping for all rows having "encrypted" set to 0. It also
 * sets encrypted to 1 for those rows.
 * @author jwaechter
 * @version 1.0
 */
public class CredentialEncryptor implements IScript {

	private static final String USER_JM_PRICE_WEB_FTP_MAPPING = "USER_jm_price_web_ftp_mapping";

	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init (context);
			process ();
		} catch (Throwable t) {
			Logging.error(t.toString());
			throw t;
		} finally {
			Logging.info("************* End of Run **************");	
			Logging.close();
		}
	}
	
	private void process() throws OException {
		Logging.info("************* Start of a new Run **************");
		Table mappingTable = null;
		CryptoInterface ci = new CryptoImpl();
		
		try {
			mappingTable = Table.tableNew(USER_JM_PRICE_WEB_FTP_MAPPING);
			int ret = DBUserTable.load(mappingTable);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException ("Error retrieving " + USER_JM_PRICE_WEB_FTP_MAPPING);
			}
			for (int row=mappingTable.getNumRows(); row>=1; row--) {
				int encrypted = mappingTable.getInt("encrypted", row);
				if (encrypted != 0) {
					continue;
				}
				String unencUserName=mappingTable.getString("ftp_user_name", row);
				String unencPassword=mappingTable.getString("ftp_user_password", row);
				String encPassword = ci.encrypt(unencPassword);
				String encUserName = ci.encrypt(unencUserName);
				
				mappingTable.setString("ftp_user_name", row, encUserName);
				mappingTable.setString("ftp_user_password", row, encPassword);
				mappingTable.setInt("encrypted", row, 1);
			}
			ret = DBUserTable.saveUserTable(mappingTable, 1, 1);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException ("Error saving encrypted credentials to " + USER_JM_PRICE_WEB_FTP_MAPPING);
			}
		} finally {
			mappingTable = TableUtilities.destroy(mappingTable);
		}
	}

	private void init(IContainerContext context) throws OException {	
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			Logging.init(this.getClass(), DBHelper.CONST_REPOSITORY_CONTEXT, DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(this.getClass().getName() + " started");
	}


}