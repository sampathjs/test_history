package com.jm.ops.trading;

import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

/**
 * @author TomarR01
 *
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class CurrencyDealBlocker extends AbstractTradeProcessListener {

	private static final String USER_JM_CURRENCY_TRADE_CHECKS = "USER_jm_currency_trade_checks";
	private static final String COUNTERPARTY = "counterparty";
	private static final String INTERNAL_PORTFOLIO = "internal_portfolio";
	private static final String INTERNAL_BUNIT = "internal_bunit";
	private static final String OPSERVICE = "OpService";
	private static final String CURRENCYDEALBLOCKER = "CurrencyDealBlocker";

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {

		PreProcessResult preProcessResult = null;

		try {

			Logging.init(this.getClass(), OPSERVICE, CURRENCYDEALBLOCKER);

			preProcessResult = PreProcessResult.succeeded();
			
			StaticDataFactory sdf = context.getStaticDataFactory();

			IOFactory iof = context.getIOFactory();
			Table userTable = iof.getUserTable(USER_JM_CURRENCY_TRADE_CHECKS).retrieveTable();
			Logging.info("User table loaded");

			for (PreProcessingInfo<?> activeItem : infoArray) {
				Transaction tranPtr = activeItem.getTransaction();

				int extBU = tranPtr.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
				int intBU = tranPtr.getValueAsInt(EnumTransactionFieldId.InternalBusinessUnit);
				int intPfolio = tranPtr.getValueAsInt(EnumTransactionFieldId.InternalPortfolio);

				Logging.info("Trade attributeS -> Ext BU : " + sdf.getName(EnumReferenceTable.Party, extBU) + " and Internal portfolio : " + sdf.getName(EnumReferenceTable.Portfolio, intPfolio));

				for (int row = 0; row < userTable.getRowCount(); row++) {
					
					int portfolio = userTable.getInt(INTERNAL_PORTFOLIO, row);
					int extParty = userTable.getInt(COUNTERPARTY, row);
					int intParty = userTable.getInt(INTERNAL_BUNIT, row);

					if (extParty == extBU && intParty == intBU && portfolio != intPfolio) {
						
						Logging.info("This combination is not allowed. Please check the combination in " + USER_JM_CURRENCY_TRADE_CHECKS);
						preProcessResult = PreProcessResult.failed("Currency trade can only be booked with "+sdf.getName(EnumReferenceTable.Portfolio, portfolio)+" portfolio", true);
						break;
					}

				}

			}
			Logging.info("Checks Completed");
		} catch (Exception e) {
			Logging.error("Error, Reason : " + e.getMessage());
		}
		
		return preProcessResult;

	}

}
