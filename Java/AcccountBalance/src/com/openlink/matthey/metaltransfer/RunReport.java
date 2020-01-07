package com.openlink.matthey.metaltransfer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.generators.IRequiredParameters;
import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.matthey.openlink.utilities.DataAccess;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeInputScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OConsole;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

/*
 * History:
 * 2016-MM-DD	V1.0	pwallace	- Initial Version
 * 2016-11-15	V1.1	jwaechter	- fixed issue with data format
 *                                    not matching MSSQL expectations                                  
 */

/**
 * Retrieves the account balance for for the data inputted by the user
 * and sets it to the fields "FromACBalAfter" and "FromACBalBefore"
 * @author pwallace
 * @version 1.1
 */
@ScriptCategory({ EnumScriptCategory.TradeInput })
public class RunReport extends AbstractTradeInputScript {
	@Override
	public Table execute(Context context, Transactions transactions) {
		
		Logging.init(context, this.getClass(), "Report", "Generic");
		for (Transaction transaction : transactions) {
			String fromAccount = transaction.getField("From A/C").getValueAsString();
			String metalCode = transaction.getField("Metal").getValueAsString();
			String unit = transaction.getField("Unit").getValueAsString();
			Field settleDate = transaction.getField(EnumTransactionFieldId.SettleDate);
			Field transferQuantity = transaction.getField("Qty");
			Field fromAccountBalance = transaction.getField("FromACBalBefore");
			Field fromAccountFinalBalance = transaction.getField("FromACBalAfter");
			Field negativeThreshold = transaction.getField("NegThreshold");
			
			negativeThreshold.setValue("");
			fromAccountBalance.setValue("");
			fromAccountFinalBalance.setValue("");
			{
				Table thresholdLookup = DataAccess.getDataFromTable(context, "SELECT * from USER_JM_negative_threshold");
				if (null!=thresholdLookup && thresholdLookup.getRowCount() ==1) {
					negativeThreshold.setValue(thresholdLookup.getDouble("value", 0));
				}
			}
			
			if (null == fromAccount || "".equalsIgnoreCase(fromAccount) 
					|| null == metalCode  || unit.isEmpty()
					|| null == unit  || metalCode.isEmpty()
					|| null == settleDate ) {
				Logging.info("NOT enough information to check balance!");

			} else {

				String  accountId ="";
				Table accountLookup = DataAccess.getDataFromTable(context, String.format("SELECT account_number FROM account where account_name='%s'",fromAccount.trim()));
				if (null!=accountLookup && accountLookup.getRowCount() ==1) {
					accountId = accountLookup.getString("account_number", 0);
				}
				Logging.info(
						String.format("From %s>%s<, %s, in %s  on %s",
								accountId,fromAccount,
								metalCode,
								unit,
								settleDate.getDisplayString()));
				Date stlDate = settleDate.getValueAsDate();
				SimpleDateFormat sdf = new SimpleDateFormat ("dd-MMM-yyyy");
				Map<String, String> params = new HashMap<String, String>();
				params.put("report_name", "Account Balance Retrieval");
				params.put("acct_num", accountId);
				params.put("ReportDate", sdf.format(stlDate));
				params.put("ReportingUnit", unit);
				params.put("metalCode", metalCode);
				
				double settleDateBalance = 0.0d;
				Table answer = processRBprocessCustomRequest(context, null, params);
				//context.getDebug().viewTable(answer);
				if (null != answer && answer.getRowCount() == 1)
					settleDateBalance = answer.getDouble("balance", 0);
				Logging.info(
						String.format("Xfer From %s balance(%f) on SettleDate %s in %s",
								accountId, 
								settleDateBalance,
								settleDate.getValueAsString(),
								unit));
				fromAccountBalance.setValue(settleDateBalance);
				fromAccountFinalBalance.setValue(settleDateBalance-transferQuantity.getValueAsDouble());
			}
		}
		
		Logging.close();
		return null;
	}

	
	/**
	 * wrapper to submit request to ReportBuilder and return the results of that
	 * processing
	 */
	private Table processRBprocessCustomRequest(Context context, Table argumentsTable,
			Map<String, String> parameters) {

		Table output = null;
		Table resultData = null;
		int result = 0;
		try {

			// System.out.println("Params..." + parameters.toString());
			IReportParameters newParameters = new ReportParameters(context, parameters);

			GenerateAndOverrideParameters balances = new GenerateAndOverrideParameters(context, newParameters);
			if (balances.generate()) {
				output = balances.getResults();
				if (null != output && output.getRowCount() > 0)
					result = 1;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (result < 1) {
			Logging.info(String.format("%s Didn't return any results!!!", parameters.get(IRequiredParameters.NAME_PARAMETER)));
			return output;
			
		} else {
			Logging.info(String.format("%s OK!!!", parameters.get(IRequiredParameters.NAME_PARAMETER)));
			try {
				resultData = output.cloneData();
				resultData.addRow();
				Logging.info(String.format("RESULT:" + resultData.asXmlString()));
				
			} catch (Exception e) {
				Logging.info(String.format("\t Report Dispatcher error cloning result!"), e);
				e.printStackTrace();
			}
			return output;
		}
	}
}
