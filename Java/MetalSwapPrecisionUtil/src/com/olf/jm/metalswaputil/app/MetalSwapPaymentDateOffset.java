package com.olf.jm.metalswaputil.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SVC_TRANFIELD)
public class MetalSwapPaymentDateOffset implements IScript {
	private static final String CONTEXT = "FrontOffice";
	private static final String SUBCONTEXT = "MetalSwap";

	@Override
	public void execute(IContainerContext context) throws OException {
		Table argt = Util.NULL_TABLE;
		SimpleDateFormat fmt1 = new SimpleDateFormat("dd-MMM-yy");
		SimpleDateFormat fmt2 = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat fmt3 = new SimpleDateFormat("dd-MMM-yyyy");
		
		initLogging();
		argt = context.getArgumentsTable().copyTable();
//		argt.viewTable();
		int side = argt.getInt("side", 1);
		if (side == 0) {
			Transaction tran = argt.getTran("tran", 1);
			if (!isTanakaDeal(tran)) {
				Logging.info("Not Tanakka Deal, skipping");
				return;
			}
			String pymtDtOffset = tran.getField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), side, "Payment Offset Date");
			String strIndex = tran.getField(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), 1, null);
			int indexId = Ref.getValue(com.olf.openjvs.enums.SHM_USR_TABLES_ENUM.INDEX_TABLE, strIndex);
			try {
				Date pymtOffsetDate = fmt1.parse(pymtDtOffset);
				String pymtDtOffset_yyyymmdd = fmt2.format(pymtOffsetDate);
				int julianDate = OCalendar.convertYYYYMMDDToJd(pymtDtOffset_yyyymmdd);
				int pymtOffsetDateJD = OCalendar.jumpGBDForIndex(julianDate, 7, indexId);
				String pymntOffsetDt = OCalendar.formatJd(pymtOffsetDateJD, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
				Date pymtOffsetDate2 = fmt3.parse(pymntOffsetDt);
				tran.setField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), 1, "", ""+fmt1.format(pymtOffsetDate2));
				tran.setField(TRANF_FIELD.TRANF_PYMT_DATE_OFFSET.toInt(), 2, "", ""+fmt1.format(pymtOffsetDate2));
				Logging.info(fmt1.format(pymtOffsetDate2)+",pymntOffsetDt="+pymntOffsetDt+",indexId="+indexId+",pymtDtOffset_yyyymmdd="
				+pymtDtOffset_yyyymmdd+",strIndex="+strIndex+",pymtOffsetDateJD="+pymtOffsetDateJD);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OException oe) {
				Logging.error(oe.getMessage());
			}
		}
		argt.destroy();
	}
	private boolean isTanakaDeal(Transaction tran) {
		boolean blnReturn = false;
		try {
			int extBU = tran.getExternalBunit();
			int tanakaBU = Ref.getValue(com.olf.openjvs.enums.SHM_USR_TABLES_ENUM.PARTY_TABLE, "TANAKA KIKINZOKU KOGYO KK - BU");

			if (extBU == tanakaBU ) {
				blnReturn = true;
			}
		} catch (OException e) {
			Logging.error(e.getMessage(), e);
		}
		return blnReturn;
	}
	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() {
		// Constants Repository Statics
		try {
			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			throw new RuntimeException(errMsg, e);
		}
	}
}
