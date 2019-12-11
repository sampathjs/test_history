package com.olf.jm.metalswapfixedlegunitsetting.app;

import java.util.HashSet;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.metalswapfixedlegunitsetting.model.ConfigurationItem;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-05-24	V1.0	jwaechter	- Initial Version
 */


/**
 * Plugin to ensure that the price unit on the fixed leg of each side of 
 * metal swap deals is always TOz, except for deals having projection index
 * as defined in {@link ConfigurationItem#COM_SWAP_SKIP_PROJ_INDEX} on the
 * floag leg of the side.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class MetalSwapFixedLegPriceUnitSetting extends
	AbstractTradeProcessListener {

	@Override
	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		try {
			init (context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				process (context, ppi.getTransaction());
				if (ppi.getOffsetTransaction() != null) {
					process (context, ppi.getOffsetTransaction());					
				}
			}
			return PreProcessResult.succeeded();
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			throw t;
		}		
	}
	
	
	private void process(Context context, Transaction transaction) {
		for (Leg fixedSideLeg : transaction.getLegs()) {
			Field fixFloatField = fixedSideLeg.getField(EnumLegFieldId.FixFloat);
			if (!fixFloatField.isApplicable() || !fixFloatField.isReadable()
				||	!fixFloatField.getValueAsString().equals("Fixed")) {
				continue;
			}
			Field paramGroupField = fixedSideLeg.getField(EnumLegFieldId.ParamGroup);
			if (!paramGroupField.isApplicable() || !paramGroupField.isReadable()) {
					continue;
			}
			int paramGroup = paramGroupField.getValueAsInt();
			
			if  (skipSide(transaction, paramGroup)) {
				continue;
			}
			
			Field unitField = fixedSideLeg.getField(EnumLegFieldId.Unit);
			Field priceUnitField = fixedSideLeg.getField(EnumLegFieldId.PriceUnit);
			if (!priceUnitField.isApplicable() || !priceUnitField.isWritable()) {
				continue;
			}
			
			if (!priceUnitField.getValueAsString().equals(unitField.getValueAsString())) {
				priceUnitField.setValue(unitField.getValueAsString());				
			}
			/*if (!priceUnitField.getValueAsString().equals("TOz")) {
				priceUnitField.setValue("TOz");				
			}*/
		}
		
	}


	private boolean skipSide(Transaction transaction, int paramGroup) {
		String[] indexesToSkipArray = ConfigurationItem.COM_SWAP_SKIP_PROJ_INDEX.getValue().split(",");
		Set<String> indexToSkip = new HashSet<>();
		for (String index : indexesToSkipArray) {
			indexToSkip.add(index.trim());
		}
		for (Leg floatSideLeg : transaction.getLegs()) {
			Field fixFloatField = floatSideLeg.getField(EnumLegFieldId.FixFloat);
			if (!fixFloatField.isApplicable() || !fixFloatField.isReadable()
				||	!fixFloatField.getValueAsString().equals("Float")) {
				continue;
			}
			Field paramGroupField = floatSideLeg.getField(EnumLegFieldId.ParamGroup);
			if (!paramGroupField.isApplicable() || !paramGroupField.isReadable()) {
					continue;
			}
			int paramGroupFloat = paramGroupField.getValueAsInt();
			if (paramGroup != paramGroupFloat) {
				continue;
			}
			
			Field projIndexField = floatSideLeg.getField(EnumLegFieldId.ProjectionIndex);
			if (!projIndexField.isApplicable() || !projIndexField.isReadable()) {
				continue;
			}
			String projIndex = projIndexField.getValueAsString();
			if (indexToSkip.contains(projIndex)) {
				return true;
			}
		}
		return false;
	}


	public void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR") + "\\error_logs";
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = abOutdir;	// ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}
}
