package com.matthey.openlink.utilities.tpm;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.matthey.openlink.LondonBullionMarketAssociation;
import com.matthey.openlink.utilities.DispatchDelivery;
import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.Transaction;

/**
 *	@version $Revision: $
 * <p>Retrieve latest LGD value via TPM
 * <br>
 *  The {@code ConstRepository} can be used to redefine variables names and values to suite workflow requirements.
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
 *	<td><font color="blue"><b>{@value #TPM_LGD_VARIABLE}</b></font></td>
 *	<td>{@value #LGD}</td>
 *	<td>The {@code InfoField} that is populated with the....
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #TPM_TRANSACTION_VARIABLE}</b></td>
 *	<td>{@value #TPM_TRANNUM}</td>
 *	<td>This is the variable which be used to identify the transaction number to execute against</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #TPM_STATICVALUE}<font color="red"><sup>*</sup></font></b></td>
 *	<td><p style="font-size:xx-small">N/A</p></td>
 *	<td>If this is populated then the will be returned in place of the actual next {@value #LGD}
 *  <br>This is a <i><b>{@code String}</b></i> field and will be converted to the appropriate type at runtime!
 *	<p style="font-size:xx-small">
 *	*Only for testing/debugging workflow processing.
 *	</p>
 *  </td>
 *	</tr>
 *	</tbody>
 *	</table>
 * </p>
 * <br>
 *  @see LondonBullionMarketAssociation
 *  
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class RetrieveLGD extends AbstractProcessStep {

	private static final String TPM_TRANSACTION_VARIABLE = "TPM_Transaction";
	private static final String TPM_LGD_VARIABLE = "TPM_Variable";
	private static final String TPM_STATICVALUE = "TPM_StaticValue";
	private static final String TPM_TRANNUM = "TranNum";
	private static final String LGD = "Dispatch_LGD";
	private static final String CONST_REPO_CONTEXT = "JM_LGD";
	private static final String CONST_REPO_SUBCONTEXT = "Configuration";

	 private static final Map<String, String> configuration;
	    static
	    {
	    	configuration = new HashMap<String, String>();
	    	configuration.put(TPM_LGD_VARIABLE,LGD);
	    	configuration.put(TPM_TRANSACTION_VARIABLE, TPM_TRANNUM);
	    	configuration.put(TPM_STATICVALUE, "");
	    }

		private Properties properties;
	
	
	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {

		getImplementationConfig();
		TpmVariables tpmVariables = new TpmVariables(context, process, variables);

		Variable currentLGD = tpmVariables.getVariable(properties
				.getProperty(TPM_LGD_VARIABLE));
		Transaction transaction = context.getTradingFactory()
				.retrieveTransactionById(
						tpmVariables.getVariable(
								properties.getProperty(TPM_TRANSACTION_VARIABLE))
								.getValueAsInt());
		
		if (null != properties.getProperty(TPM_STATICVALUE)
				&& properties.getProperty(TPM_STATICVALUE).length() > 0) {
			currentLGD.setValue(Integer.parseInt(properties.getProperty(TPM_STATICVALUE)));
		} else
			currentLGD.setValue(LondonBullionMarketAssociation.getLGD(context, transaction).toString());

		process.setVariable(currentLGD);
		return null;
	}

	void getImplementationConfig() {
        this.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
	}
}
