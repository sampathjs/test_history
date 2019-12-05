package com.olf.jm.post.reportbuilder;
 
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Properties;

import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * CR21 generate Cash trade automatically off the back of the SocGen Summary report.
 * If there is an existing entry for the effective SocGen Report date({@value #SOCGEN_RB_REFERENCE_NAME}), notify user and exit.
 * 
 * @version $Revision: $
 * 
 *  <p><table border=0 style="width:15%;">
 *  <caption style="background-color:#0070d0;color:white"><b>ConstRepository</caption>
 *	<th><b>context</b></th>
 *	<th><b>subcontext</b></th>
 *	</tr><tbody>
 *	<tr>
 *	<td align="center">{@value #CONST_REPO_CONTEXT}</td>
 *	<td align="center">{@value #CONST_REPO_SUBCONTEXT}</td>
 *  </tbody></table></p>
 *	<p>
 *	<table border=2 bordercolor=black>
 *	<tbody>
 *	<tr>
 *	<th><b>Variable</b></th>
 *	<th><b>Default</b></th>
 *	<th><b>Description</b></th>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #REPORT_NAME}</b></font></td>
 *	<td>{@value #SOCGEN_RB_DEFINITION_NAME}</td>
 *	<td>The report definition name
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #CASH_TEMPLATE}</b></font></td>
 *	<td>{@value #CASH_TEMPLATE_NAME}</td>
 *	<td>The template from which cash deal should be derived
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #SOCGEN_REFERENCE}</b></font></td>
 *	<td>{@value #SOCGEN_RB_REFERENCE_NAME}</td>
 *	<td>The Report Builder parameter used to populate the new transactions 'Reference' field
 *	</td>
 *	</tr>
  *	<tr>
 *	<td><font color="blue"><b>{@value #SOCGEN_RB_MARGIN}</b></font></td>
 *	<td>{@value #SOCGEN_RB_MARGIN_NAME}</td>
 *	<td>The Report Builder column name to use as the amount for the transaction
 *	</td>
 *	</tr>*	</tbody>
 *	</table> */
@ScriptCategory({ EnumScriptCategory.Generic })
public class CreateSocGenSummaryDeal extends AbstractGenericScript {

	
	private static final String SOCGEN_RB_DEFINITION_NAME = "SocGen";
	private static final String SOCGEN_RB_REFERENCE_NAME = "socgen_ref";
	
	private static final String CASH_TEMPLATE_NAME = "SocGen Nymex Cash Journal";
	private static final String CASHFLOW_NAME = "SocGen Nymex";
	
	private String EXISTING_TRADE_MESSAGE ="Cash trade found for Trade Date = %s, Reference = %s of Value = %f" +
			" Tran Num %d.  Please cancel this trade (if appropriate) and try again";

	/** constants repository .
	 * 
	 */
	public static final String CONST_REPO_CONTEXT = SOCGEN_RB_DEFINITION_NAME; 
	public static final String CONST_REPO_SUBCONTEXT = "ReportBuilder";
	private static final String REPORT_NAME = "REPORT_NAME";
	private static final String CASH_TEMPLATE = "TEMPLATE";
	private static final String TEMPLATE_CASHFLOW = "CASHFLOW";
	private static final String SOCGEN_REFERENCE = "SocGen Reference";
	private static final String SOCGEN_RB_MARGIN_NAME = "margin_call";
	private static final String SOCGEN_RB_MARGIN = "MARGIN";
	
	protected static HashMap<String, String> configuration;
    static
    {
    	configuration = new HashMap<String, String>(0);
    	configuration.put(REPORT_NAME,SOCGEN_RB_DEFINITION_NAME);
    	configuration.put(CASH_TEMPLATE,CASH_TEMPLATE_NAME);
    	configuration.put(TEMPLATE_CASHFLOW,CASHFLOW_NAME);
    	configuration.put(SOCGEN_REFERENCE,SOCGEN_RB_REFERENCE_NAME);
    	configuration.put(SOCGEN_RB_MARGIN,SOCGEN_RB_MARGIN_NAME);
    }

	private Properties properties;
	private SimpleDateFormat SDF = new SimpleDateFormat("dd-MMM-yyyy");
	
	@Override
	public Table execute(Session session, ConstTable reportBuilderData) {
		   try {
			   properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
			Logging.init(session, this.getClass(), "SocGenSummary", "ReportBuilder");
			Logging.info("Started SocGenSummary");
			createDeal(session,
					getReportParameterValue(properties.getProperty(SOCGEN_REFERENCE),
							SDF.format(session.getBusinessDate()), reportBuilderData.getTable(0, 0)),
					reportBuilderData.getTable(1, 0));
	            return null;

			} catch (RuntimeException e) {
			Logging.error(e.getLocalizedMessage(), e);
	            throw e;
	        }
		finally {
			Logging.close();
		}
	}

	private void createDeal(Session session, String reference, ConstTable reportResults) {
		
		   if (reportResults.getRowCount() < 1) {
			Logging.info("No data available to produce generate '" + properties.getProperty(REPORT_NAME) + "' deal");
	            return;
	        }
		   
		    int tranNum = 0;
	        
	        // Only book deal if a cash deal with the same reference does not already exist
            if (!session.getTradingFactory().isValidTransactionReference(reference, EnumTranStatus.Validated)) {

			Logging.info("Booking interest deal using reference " + reference);

                try (Transaction template = session.getTradingFactory().retrieveTransactionByReference(properties.getProperty(CASH_TEMPLATE), EnumTranStatus.Template)) {
                		setDefaultSymbolicDates(session, template);
                        Transaction cash = session.getTradingFactory().createTransactionFromTemplate(template); 
                       //cash.setValue(EnumTransactionFieldId.CashflowType, properties.getProperty(TEMPLATE_CASHFLOW));
                       cash.setValue(EnumTransactionFieldId.ReferenceString, reference);
                       cash.setValue(EnumTransactionFieldId.Position, reportResults.getDouble(properties.getProperty(SOCGEN_RB_MARGIN), 0));
                       cash.process(EnumTranStatus.Validated);
                       //setDefaultSymbolicDates(session, cash); //setDefaultSymbolicDates
                       tranNum = cash.getTransactionId();
                       //return cash.getDealTrackingId();
                   }
            
                
                
			Logging.info("Successfully booked cash interest deal " + tranNum + " with reference " + reference);

                // Add cash deals transaction number to the list of booked deals
                //TableRow newTranRow = tranNums.addRow();
                //newTranRow.setValues(new Object[] {tranNum});
            }  else {
            	Transaction transaction = session.getTradingFactory().retrieveTransactionByReference(reference, EnumTranStatus.Validated);
                String message = String.format(
                        EXISTING_TRADE_MESSAGE,
                        SDF.format(transaction.getField(EnumTransactionFieldId.TradeDate).getValueAsDate()), reference, transaction.getField(EnumTransactionFieldId.Position).getValueAsDouble(), transaction.getTransactionId());
			Logging.info(message);
                throw new OpenRiskException(message);
            }

		   
	}
	
		
	/**
	 * OC workaround for JVS GAP 
	 * <br>populate subset of dates based on legacy JVS behaviour.
	 */
    private void setDefaultSymbolicDates(Session session, Transaction cash) {
    	//retrieveSymbolicMaturityDate = cash.getInstrument().getField(EnumTransactionFieldId.SettleDate);

    	Field setSymbolicMatDate = cash.getLeg(0).getField(EnumLegFieldId.SymbolicMaturityDate);
    	Field maturityDate = cash.getField(EnumTransactionFieldId.MaturityDate);
    	if (null != setSymbolicMatDate && !maturityDate.isReadOnly()) {
    		//Date date = session.getCalendarFactory().createSymbolicDate(setSymbolicMatDate.getDisplayString()).evaluate();
			Logging.info(String.format("%s set to %s", EnumTransactionFieldId.MaturityDate.getName(),
					setSymbolicMatDate.getDisplayString()));
    		maturityDate.setValue(setSymbolicMatDate.getDisplayString());
    	}
    	
    	Field setSymbolicSettleDate = cash.getLeg(0).getField(EnumLegFieldId.SymbolicSettlementDate); 
    	Field settleDate = cash.getField(EnumTransactionFieldId.SettleDate);
    	if (null != setSymbolicSettleDate && !settleDate.isReadOnly()) {
			Logging.info(String.format("%s set to %s", EnumTransactionFieldId.SettleDate.getName(),
					setSymbolicSettleDate.getDisplayString()));
    		settleDate.setValue(setSymbolicSettleDate.getDisplayString());
    	}
    	
    	Field setSymbolicStartDate = cash.getLeg(0).getField(EnumLegFieldId.SymbolicStartDate); 
    	Field startDate = cash.getField(EnumTransactionFieldId.StartDate); 
    	if (null != setSymbolicStartDate && !startDate.isReadOnly()) {
			Logging.info(String.format("%s set to %s", EnumTransactionFieldId.StartDate.getName(),
					setSymbolicStartDate.getDisplayString()));
    		startDate.setValue(setSymbolicStartDate.getDisplayString());
       	}
	}

	/**
     * Get the report parameter value for the parameter name.
     * 
     * @param parameterName Parameter name
     * @param defaultValue 
     * @param reportParams table of RB arguments
     * @return parameter value or empty string if parameter not found
     */
    private String getReportParameterValue(String parameterName, String defaultValue, Table reportParams) {

		String prefixBasedOnVersion = reportParams.getColumnNames().contains("expr_param_name") ? "expr_param"
				: "parameter";

		int row = reportParams.find(reportParams.getColumnId(prefixBasedOnVersion + "_name"), parameterName, 0);
        if (row < 0 ) {
            throw new RuntimeException("The report parameter " + parameterName + " is missing from the Report Builder configuration");
        }
        else if (row < 0 && defaultValue != null) {
            return defaultValue;
        }
        else if (row < 0) {
            return "";
        }
		return reportParams.getString(prefixBasedOnVersion + "_value", row);
    }
}
