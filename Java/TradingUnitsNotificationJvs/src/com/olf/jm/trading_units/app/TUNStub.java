package com.olf.jm.trading_units.app;


import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;
/*
 * HISTORY
 * 1.0 - 2015-08-04 - jwaechter - initial version
 */
/**
 * 
 * @author jwaechter
 *
 */
public class TUNStub implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {
		// TODO Auto-generated method stub
		OConsole.oprint ("\n\n\n\n\n\n\n\n " + this.getClass().getSimpleName() + " STARTED.");
		Logging.info("\n" + this.getClass().getSimpleName() + " finished successfully.");
	}
	
}