package com.olf.jm.taxconfiguration.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.taxconfiguration.model.PartyInfoFields;
import com.olf.jm.taxconfiguration.model.RetrievalLogic;
import com.olf.jm.taxconfiguration.model.RowMatch;
import com.olf.jm.taxconfiguration.model.TranInfoFields;
import com.olf.jm.taxconfiguration.persistence.DBHelper;
import com.olf.openjvs.OException;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumFeeFieldId;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Fee;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2015-06-17	V1.0 	jwaechter	- Initial version.
 * 2015-06-25   V1.1	jwaechter	- "Not Found" for info fields = wildcard
 *                                  - ext_jm_group == yes -> skip assignment of tax 
 *                                  - IF ext country = int country THEN int/ext_le_region = country ISO + -Domestic
 * 2015-07-29	V1.2	jwaechter	- Defect fix in processing of NOT in method "checkRuleColumn"
 * 2015-07-29	V1.2	jwaechter	- Enhancement: complex selection logics in case of more than one rule 
 *                                    matches deal.
 * 2015-09-18	V1.3	jwaechter 	- added defaults for info fields taken from ConstantsRepository
 * 2015-09-21	V1.4	jwaechter	- changed check for "Active" column from "Y" to "Yes"
 *                                  - removed temporary check for user ids so the plugin is now triggered for every user
 *                                  - fixed defect in retrieveTaxSubtypeOfLeastRow causing the tax subtype to be retrieved 
 *                                    from wrong rule table row 
 * 2015-09-28	V1.5	jwaechter	- added processing of offset deals.
 * 2015-09-30	V1.6	jwaechter	- added special processing logic for FX swap deals
 * 2015-10-06	V1.7	jwaechter	- fixed programming issue retrieving tax sub type for metal deals in case a unique row
 * 									  in rule table is found
 * 2015-10-08	V1.8	jwaechter	- adapted names of tran info fields "from account" and 
 *                                    "to account" to the final names on Golden
 * 2015-10-09	V1.9	jwaechter	- adapted retrieval of metal transfer related config data to not throw
 *                                    exceptions.
 * 2015-10-10	V1.10	jwaechter	- added or modified the following drop out cases:
 * 									  a) general drop out in case the external party has the field
 *                                    "ext_int" set to internal, applied to both normal deals and cash transfers
 *                                    b)cash transfers that have buy_sell flag set to buy are skipped
 *                                    c) the drop out logic for external partys having JM Group field set to "yes"
 *                                    is now no longer applied to cash transactions.
 * 2015-10-16	V1.11	jwaechter	- added special logic for COMM-PHYS deals:
 * 								      a) leg level based logic, retrieving metal leg based
 *                                    b) leg level based tax assignment. Tax type/subtype is assigned to legs
 * 2015-10-18	V1.12	jwachter	- changed leg logic to work on fee level instead:
 * 									  a) cash flow is now retrieved on fee level for COMM-PHYS deals
 * 									  b) tax type and tax subtype for COMM-PHYS deals are set on fee level and not on leg level
 * 									  c) metal retrieval adapted to match special structure of COMM-PHYS deals with having
 * 									  additional legs for the fees by using param_group field to identify "root" legs.
 * 2015-10-20	V1.13	jwaechter	- added fix to process cash transfer pass thru deals without exception.
 *                                  - note that those deals are currently not being set with a tax type / tax subtype
 *                                    correctly.
 * 2015-10-29	V1.14	jwaechter	- added fix to process set tax type / tax subtype on deal level for
 *                                    pass thru of cash transfers 
 * 2015-11-18	V1.15	jwaechter	- added logic for cash transfer charges 
 * 2015-11-24	V1.16	jwaechter	- added drop out logic for the case, there are no relevant fees on a COMM-PHYS deal
 * 2015-11-25	V1.17	jwaechter	- removed special logic about setting tax type and sub type for FX swap deals
 * 									- Added Cash Transfer pass through to drop out logic mentioned in version V1.10
 * 2015-12-09	V1.18	jwaechter	- cash transfer charges are now processed using the logic for normal trades
 *                                  - buy/sell is switched in case of transfer charges
 * 2016-03-16	V1.19	jwaechter	- fixed defect in join criteria in method "getLocationSql"
 *                                  - added DISTINCT to select statement in method getLocationSql
 * 2016-03-17	V1.20	jwaechter	- now using location of internal and external units for comparison with 
 *                                    "from_account_country", "to_account_country", "to_account_region"
 *                                    in method getTaxSubTypeMetalTransfer
 * 2016-03-21	V1.21	jwaechter	- fixed fix of version 1.20 to retrieve BU from strategy
 *                                  - now also skipping cash transfer pass through buy deals.
 * 2016-03-24	V1.22	jwaechter	- split SQL for data retrieval into two parts
 *                                  - changed data retrieval for transfer deals to retrieve 
 *                                    the party related fields from the parties mentioned in the strategy
 * 2016-03-30	V1.23	jwaechter	- fixed data retrieval: for metal transfer charges it uses the
 *                                    normal retrieval logic instead of the metal transfer logic again 
 *                                    like in version before V1.22. 
 * 2016-04-08	V1.24	jwaechter	- reintroduced the drop out case to skip cash transfer pass through deals 
 *                                    (see V1.21)
 * 2016-04-19	V1.25	jwaechter	- modified drop out logic for jm group (now using list provided in const rep)
 * 									- now clearing tax data before doing anything.
 * 2016-05-09	V1.26	jwaechter	- now switching buy/sell always (also in case of transfer charges)
 * 2016-07-28	V1.27	jwaechter	- change for manually booked metal transfers: now skipping processing
 *                                    in case the strategy num is 0. See method processTransaction.
 *                                    
 * */


/**
 * This plugins contains the logic to assign the tax type and tax sub type to deals of certain instrument types.
 * It has to be run as an OPS of type trading in pre process.
 * The logic is applying the following process:
 * <ol>
 *   <li>
 *     Clear all existing Tax Types and Tax Sub Types.
 *   </li>
 *   <li> 
 *      Retrieve all data that <i>might</i> be relevant for the transaction being processed. An error message is shown to the user 
 *      in case the address of the internal party does not contain a country. Note that the metals is retrieved from currency 
 *      and there is a list of the "currencies" that are metals and being processed by the plugin 
 *      (TaxTypeDetermination.RELEVANT_METALS_ARRAY). 
 *      There are two different ways to retrieve data: one for metal transfers and one for all other metal deals.
 *    </li>
 *    <li>
 *   	Select the tax type based on the data retrieved for the processed transaction (decision criteria is the relevant country of the deal). 
 *    </li>
 *    <li>
 *      Select the tax sub type based on the following logic:
 *      <ol type="a">
 *        <li> 
 *          Check the transaction level cash flow. If it is of type "Cash Transfer" use the transfer logic. If this it not, continue with the deal logic. 
 *        </li>
 *        <li>
 *          Check the user tables "USER_jm_tax_sub_type_trades" or "USER_jm_tax_sub_type_transfers" if they match
 *          the data retrieved from the transaction. Note that there is a semi complex meta language allowed in the
 *          columns of the user tables. 
 *        </li>
 *        <li>
 *          In case, there is no match, continue without raising an error to the user but note a warning in the log file.
 *        </li>
 *        <li>
 *          In case, there is a single match, retrieve the tax subtype from the table.
 *        </li>
 *        <li>
 *          In case, there is more than one match, do the following based on {@link #ruleSelectionLogic} retrieved
 *          from Constants Repository:
 *          <table border = "1"> 
 *            <caption> Rule Selection Logic Constants </caption>
 *            <tr>
 *              <th> RuleSelectionLogic constant (value in ConstRepository) </th> 
 *              <th> Description </th> 
 *            </tr>
 *            <tr>
 *              <td>
 *              	BLOCK 
 *              </td>
 *              <td>
 *              	block the deal from being booked and show an error message to 
 *          		the user containing the relevant data of the transaction and the matching rows.
 *              </td>
 *            </tr>
 *            <tr>
 *              <td>
 *              	WEIGHTED_BLOCK
 *              </td>
 *              <td>
 *                  Select the row having the least weight.
 *                  In case there is more than one row having the same least weight, block
 *                  and show error message to user 
 *              </td>
 *            </tr>
 *            <tr>
 *              <td>
 *              	BY_ORDER
 *              </td>
 *              <td>
 *              	Select the row having the smallest row num.
 *              </td>
 *            </tr>
 *           </table>
 *          </ul>
 *        </li>
 *      </ol>
 *    </li>
 *    <li>
 *      Validate the calculated tax type and tax sub type by checking if they are present in the
 *      system tables containing tax type / sub type (tax_tran_type / tax_tran_subtype).
 *    </li>
 *    <li>
 *      Update the transaction to contain the new tax type and tax sub type.
 *    </li>
 *  </ol>
 *  <br/>
 *  The tax type and tax sub type being modified can be one or more of the following:
 *  <ul>
 *    <li>
 *      Tran level
 *    </li>
 *    <li>
 *      Fee level on legs (tax type and subtype on a certain fee associated to a leg)
 *    </li>
 *  </ul>
 *  <br/>
 *  
 *  The Meta language for tax sub type selection that can be used in the user tables "USER_jm_tax_sub_type_trades" or "USER_jm_tax_sub_type_transfers".
 *  The implementation can be found in method {@link #checkRuleColumn(String, Table, int, String)}
 *  <table border = "1"> 
 *    <caption> Metal Language for Rule Definition </caption>
 *    <tr>
 *      <th> Language Element </th> 
 *      <th> Description </th> 
 *      <th> Example </th> 
 *      <th> Description of Example </th> 
 *    </tr>
 *    <tr>
 *      <td>
 *         Atom
 *      </td>
 *      <td>
 *         Denotes a single allowed value that is accepted if matched with transaction data. 
 *         Note that this comparison is case insensitive. Leading and trailing whitespace is removed.
 *      </td>
 *      <td>
 *         UK
 *      </td>
 *      <td>
 *         The data on the transaction side has to be UK
 *      </td>
 *    </tr>
 *    <tr>
 *      <td>
 *        Literal = {@value #NOT}Atom
 *      </td>
 *      <td>
 *         The transaction data matches this column if it is not the designated atom. 
 *      </td>
 *      <td>
 *        !UK
 *      </td>
 *      <td>
 *         The transaction data matched this column if it not UK.
 *      </td>
 *    </tr>
 *    <tr>
 *      <td>
 *         Literal{@value #SEPARATOR}Literal{@value #SEPARATOR}Literal...
 *      </td>
 *      <td>
 *         The transaction data matches this column if it matches one of the literals
 *         in the list.
 *      </td>
 *      <td>
 *         !UK, !US
 *      </td>
 *      <td>
 *         The data on the transaction side can be anything except UK and US. 
 *      </td>
 *    </tr>
 *    <tr>
 *      <td>
 *         {@value #ALL}
 *      </td>
 *      <td>
 *      	The data on the transaction side can be anything.
 *      </td>
 *      <td>
 *         {@value #ALL}
 *      </td>
 *      <td>
 *         There is no restriction on the transaction side of data.
 *      </td>
 *    </tr>
 *  </table>
 *  <br/>
 *  <br/>
 *  Weight Logic:
 *  <br/>
 *  While matching rows, the weight of a match is calculated by summing up the weight of the matches
 *  between a single cell in the rule table and the corresponding datum of the transaction.
 *  The weight of a match between a single cell in the rule table and the corresponding transaction
 *  datum is classified by the following logic:
 *  <table border = "1"> 
 *     <caption> Weight Calculation Logic </caption>
 *     <tr>
 *       <th> Match type </th> 
 *       <th> Weight assigned </th> 
 *     </tr>
 *     <tr>
 *       <td> Exact Match, e.g. Country = "UK" both in rule table and transaction data </td> 
 *       <td> 0 </td> 
 *     </tr>
 *     <tr>
 *       <td> Matching Not e.g. Country = "UK" in transaction and "!SA" in rule table </td> 
 *       <td> 1 </td> 
 *     </tr>
 *     <tr>
 *       <td> Matching All e.g. Country = "UK" in transaction and "*" in rule table </td> 
 *       <td> 2 </td> 
 *     </tr>
 *  </table>
 *  General remarks on the weight: high weight = bad match.
 *  If two rows have the same weight, one common explanation is they are set up "symmetrical":
 *  transaction: metal = "XPT", lbma = "Yes"
 *  rule 1: metal = "*", lbma = "Yes"
 *  rule 2: metal = "XPT", lbma = "*"
 *  solution to fix setup: enhance at least one of the rows to exclude the matching case of the other
 *  rule. e.g. change rule 1 to "!XPT" and "Yes".
 *  
 * The plugin logic utilizes the following party info types those actual names are saved in constants of the
 * TaxTypeDetermination class:
 * <ul>
 *   <li> LBMA Member (TaxTypeDetermination.PARTY_INFO_LBMA_MEMBER) </li>
 *   <li> LPPM Member (TaxTypeDetermination.PARTY_INFO_LPPM_MEMBER) </li>
 *   <li> JM Group (TaxTypeDetermination.PARTY_INFO_JM_GROUP) </li>
 * </ul>
 * The tran info field "Linked Deal" (actual name taken from TaxTypeDetermination.TRAN_INFO_LINKED_DEAL)
 * is used to retrieve the strategy deal containing the actual tax assignment relevant data in case of transfers.
 * <br/>
 * Assumptions/Preconditions:
 * <ol>
 *   <li> The party info fields are set up correctly for the parties involved. </li>
 *   <li> The relevant instruments (LOAN-ML, DEPO-ML, CASH, METAL-SWAP, FX) are set up correctly. </li>
 *   <li> The strategy deals are set up and linked via the linked deal tran info </li>
 *   <li> 
 *     The retrieval of the metal type is different for each instrument types. Check method extractMetal() for details 
 *     about assumptions where the metal is retrieved and which assumptions are made about instrument setup.
 *   </li>
 * <ol>
 * <br/>
 * <table border ="1">
 *   <caption>Constants repository variables used</caption> 
 *   <tr>
 *     <th>
 *       Variable
 *     </th>
 *     <th>
 *       Semantics
 *     </th>
 *     <th>
 *       Default Value if not specified explicitly in Constants Repository
 *     </th>
 *   </tr>
 *   <tr>
 *     <td>
 *       logLevel
 *     </td>
 *     <td>
 *       The log level to be used for PluginLog. See PluginLog Manual for possible values
 *     </td>
 *     <td>
 *       Error
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       logFile
 *     </td>
 *     <td>
 *       The name of the file the logs are written to. May not be a full path.
 *     </td>
 *     <td>
 *       (Name of Class).log -> TaxTypeDetermination.log
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       logDir
 *     </td>
 *     <td>
 *       The directory the log file is created within.
 *     </td>
 *     <td>
 *       none
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       useCache
 *     </td>
 *     <td>
 *       Yes or no to indicate whether some rudimentary plugin level caching of static data should occur or not.
 *     </td>
 *     <td>
 *       No
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       viewTablesInDebugMode
 *     </td>
 *     <td>
 *       yes or no to indicate if the table containing the retrieved transaction data should be shown 
 *       or not. Only works in case logLevel is Debug.
 *     </td>
 *     <td>
 *       No
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>
 *       ruleSelectionLogic
 *     </td>
 *     <td>
 *     What should be done in case more than than rule in either {@value #USER_TABLE_RULES_TRADES} or
 *     {@value #USER_TABLE_RULES_TRANSFERS} matches.
 *      <ul>
 *        <li> BLOCK </li>
 *        <li> WEIGHTED_BLOCK </li>
 *        <li> BY_ORDER </li>
 *      </ul>
 *     </td>
 *     <td>
 *       BLOCK
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>  
 *     	 Default_LBMA
 *     </td>
 *     <td>  
 *       The value to be assumed as default for the party info field "LBMA Member"
 *     </td>
 *     <td>
 *       No
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>  
 *     	 Default_LPPM
 *     </td>
 *     <td>  
 *       The value to be assumed as default for the party info field "LPPM Member"
 *     </td>
 *     <td>
 *       No
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>  
 *     	 Default_JM_Group
 *     </td>
 *     <td>  
 *       The value to be assumed as default for the party info field "JM Group"
 *     </td>
 *     <td>
 *       No
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>  
 *     	 Default_Metal
 *     </td>
 *     <td>  
 *       The value to be assumed as default for the tran info field "metal" that can be found on strategies
 *     </td>
 *     <td>
 *      (no default value)
 *     </td>
 *   </tr>
 * </table>
 * <br/>
 * Offset processing logic: Offset deals are being processed like normal deals if they are present.
 * <br/>
 * Drop out logic: due to organic grow of the plugin there are several drop out logics distributed among
 * the plugin:
 * <ul>
 *   <li> 
 *     Cash Transfer buy deals are skipped at the beginning of {@link #processTransaction(Transaction, Context)}
 *   </li>
 *   <li> 
 *     Cash Transfer deals having no strategy are skipped at the beginning of {@link #processTransaction(Transaction, Context)}
 *   </li>
 *   <li>
 *     Deals having default retrieval logic with an external business unit that is actually an internal
 *     are being skipped in method {@link #processTransaction(Transaction, Context)} (roughly center of source of
 *     this method)
 *   </li>
 *   <li>
 *     Deals having default retrieval logic and the internal business unit's party info field 
 *     {link PartyInfoFields#JM_GROUP} set to "Yes" and that are not within the constants repository
 *     list if business units having forced VAT are skipped in method {@link #processTransaction(Transaction, Context)} 
 *     (nearly at the end of the method).
 *   </li>
 * </ul>
 *  
 * 
 * @author jwaechter
 * @version 1.27
 *  */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class TaxTypeDetermination extends AbstractTradeProcessListener {
	/**
	 * Contains a classification of the input transaction that decides
	 * both how data is retrieved and processed from the input transaction
	 * as well as which tax type / subtype (tran or fee level) is to set.
	 * See {@link TaxTypeDetermination#getRetrievalLogic(Transaction)}
	 * for details about the classification.
	 * @author jwaechter
	 *
	 */
	private static enum RuleSelectionLogic {
		BLOCK, WEIGHTED_BLOCK, BY_ORDER
	}

	private static final String CONTEXT = "BackOffice";
	private static final String SUBCONTEXT = "TaxAssignment";	

	private static final String LOAN_ML_INS_TYPE = "LOAN-ML";
	private static final String DEPO_ML_INS_TYPE = "DEPO-ML";
	private static final String CASH_INSTRUMENT_TYPE = "CASH";
	private static final String METAL_SWAP_INS_TYPE = "METAL-SWAP";
	private static final String FX_INS_TYPE = "FX";
	private static final String COMM_PHYS_INS_TYPE = "COMM-PHYS";
	
	private static final String TRANSFER_CHARGE_CASH_FLOW = "Transfer Charge";
	private static final String DOMESTIC = "-Domestic";

	/**
	 * Columns(all String): active, internal_entity, buy_sell, ext_le_region, metal, lbma, lppm, 
	 * cash_flow_type, tax_subtype
	 */
	public static final String USER_TABLE_RULES_TRANSFERS = "USER_jm_tax_sub_type_transfers";

	/**
	 * Columns( all String) active, from_account_country, to_account_country, to_account_region,
	 * to_bu_internal, metal, lbma, lppm, tax_subtype
	 */
	public static final String USER_TABLE_RULES_TRADES = "USER_jm_tax_sub_type_trades";

	/**
	 * <b>relevant</b> columns (all String) loco_name, country
	 */
	public static final String USER_TABLE_LOCO = "USER_jm_loco";

	/**
	 * The separator to allow definition of more than one value per column in the two rules tables
	 * {@link #USER_TABLE_RULES_TRADES} and {@link #USER_TABLE_RULES_TRANSFERS}
	 */
	private static final String SEPARATOR = ";";

	/**
	 * The character(s) denoting to exclude a certain element in one of the columns of the two rules
	 * tables {@link #USER_TABLE_RULES_TRADES} and {@link #USER_TABLE_RULES_TRANSFERS}
	 */
	private static final String NOT = "!";

	/**
	 * The character(s) denoting to accept all values in of the the columns of the two rules tables
	 * {@link #USER_TABLE_RULES_TRADES} and {@link #USER_TABLE_RULES_TRANSFERS}
	 */
	private static final String ALL = "*";

	/**
	 * Names of all currencies that have the column precious_metal flag set to 1.
	 */
	private Set<String> relevantMetals;

	private ConstRepository constRep;
	private boolean viewTables;
	private boolean useCache;

	private int loanMLInsType=-1;
	private int depoMLInsType=-1;
	private int cashInsType=-1;
	private int metalSwapInsType=-1;
	private int fxInsType=-1;
	private int commPhysInsType=-1;
	
	private int cflowTypeTransferCharge=-1;

	private String defaultJMGroup;
	private String defaultLPPMMember;
	private String defaultLBMAMember;
	private String defaultMetal;

	private String defaultFromAccount;
	private String defaultToAccount;
	private String defaultForceVat;
	private String defaultToAcBu;
	private String defaultFromAcBu;
	private List<String> cpForceVAT;
	
	
	/**
	 * Maps fee types to cash flow types
	 */
	private Map<Integer, String> feeTypeToCashFlow;

	private RuleSelectionLogic ruleSelectionLogic = RuleSelectionLogic.BLOCK;

	@Override
	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		try {
			constRep = new ConstRepository(CONTEXT, SUBCONTEXT);
			initPluginLog ();
			relevantMetals = DBHelper.retrievePreciousMetalList(context);
			feeTypeToCashFlow = DBHelper.retrieveFeeToCashFlowMap(context);
			getTypeIds (context);
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				clearTaxData (ppi.getTransaction(), context);
				processTransaction (ppi.getTransaction(), context);
				if (ppi.getOffsetTransaction() != null) {
					clearTaxData (ppi.getOffsetTransaction(), context);
					processTransaction (ppi.getOffsetTransaction(), context);
				}
			}
		} catch (OException ex) {
			PluginLog.error(ex.toString());
			throw new RuntimeException (ex);
		} catch (Throwable t) {
			PluginLog.error(t.toString());
			throw t;
		}
		return PreProcessResult.succeeded();
	}

	private void clearTaxData(Transaction transaction, Context context2) {
		if (transaction.getValueAsInt(EnumTransactionFieldId.InstrumentType) != commPhysInsType) {
			setTaxTypeAndSubType(transaction, "", "", -1, -1);
		} else {
			for (Leg leg : transaction.getLegs()) {
				if (isCommPhysLegRelevant(leg)) {
					
					for (Fee fee : leg.getFees()) {
						if (!fee.getField(EnumFeeFieldId.Definition).isWritable()) {
							continue;
						}
						if (transaction.getValueAsInt(EnumTransactionFieldId.InstrumentType) != commPhysInsType) {
							setTaxTypeAndSubType(transaction, "", "", leg.getLegNumber(), fee.getFeeNumber());
						}
					}
				}
			}			
		}
	}

	private void processTransaction(final Transaction transaction, final Context context) {
		Table transactionData = null;
		PluginLog.info ("Processing transaction no #" + transaction.getTransactionId());

		try {
			RetrievalLogic rl = getRetrievalLogic(transaction);
			if (rl == RetrievalLogic.CASH_TRANSFER 
					) {
				Field buySell = transaction.getField(EnumTransactionFieldId.BuySell);
				if (buySell != null && buySell.isApplicable() && buySell.isReadable() 
						&& buySell.getValueAsString().equalsIgnoreCase("Buy")) {
					PluginLog.info("Transaction #" + transaction.getTransactionId() 
							+ " is cash transfer and buy deal. No tax is needed."
							+ " Exiting.");
					return;
				}
				Field strategyNum = transaction.getField(TranInfoFields.STRATEGY_NUM.getName());
				if (strategyNum != null && strategyNum.isApplicable() && strategyNum.isReadable() &&
						strategyNum.getValueAsInt() == 0) {
					PluginLog.warn("Transaction #" + transaction.getTransactionId() 
							+ " is cash transfer but does not have a strategy num > 0."
							+ " Assuming this deal to be a manual processed metal transfer."
							+ " No tax needed.");
					return;					
				}
			}
			if (rl == RetrievalLogic.CASH_TRANSFER_PASS_THROUGH) {
				Field strategyNum = transaction.getField(TranInfoFields.STRATEGY_NUM.getName());
				if (strategyNum != null && strategyNum.isApplicable() && strategyNum.isReadable() &&
						strategyNum.getValueAsInt() == 0) {
					PluginLog.warn("Transaction #" + transaction.getTransactionId() 
							+ " is cash transfer pass thru but does not have a strategy num > 0."
							+ " Assuming this deal to be a manual processed metal transfer."
							+ " No tax needed.");
					return;					
				}				
			}

			Field linkedTranInfoField = transaction.getField(TranInfoFields.STRATEGY_NUM.getName());
			String linkedDealInfo = "";
			if (linkedTranInfoField != null && linkedTranInfoField.isApplicable()) {
				linkedDealInfo = linkedTranInfoField.getDisplayString();				
			}

			int linkedDeal = 0;
			if (linkedDealInfo.matches("\\d+")) {
				linkedDeal = Integer.parseInt(linkedDealInfo);
			}

			if (transaction.getValueAsInt(EnumTransactionFieldId.InstrumentType) != commPhysInsType) {
				String tranLevelCflow = tranLevelCflowRetrieval (transaction, rl);
				transactionData = retrieveTransactionData (transaction, context, transaction.getTransactionId(), linkedDeal, -1,
						tranLevelCflow, -1);	
				if (transactionData == null) {
					PluginLog.info("Skipping Transaction #" + transaction.getTransactionId() );
					return;
				}
				logTableRow (transactionData, 0);
			} else {
				for (Leg leg : transaction.getLegs()) {
					if (isCommPhysLegRelevant(leg)) {
						for (Fee fee : leg.getFees()) {
							if (!fee.getField(EnumFeeFieldId.Definition).isWritable()) {
								continue;
							}
							String feeLevelCflow = feeLevelCflowRetrieval(fee);
							if (transactionData == null) {
								transactionData = retrieveTransactionData (transaction, context, 
										transaction.getTransactionId(), linkedDeal, leg.getLegNumber(), feeLevelCflow,
										fee.getFeeNumber());
							} else {
								Table transactionDataPerLeg = 
										retrieveTransactionData (transaction, context, transaction.getTransactionId(), 
												linkedDeal, leg.getLegNumber(), feeLevelCflow,
												fee.getFeeNumber());
								transactionData.appendRows(transactionDataPerLeg);
								transactionDataPerLeg.dispose();
							}
							logTableRow (transactionData, transactionData.getRowCount()-1);
						}
					}
				}
			}
			if (transactionData == null) {
				PluginLog.info("No relevant fees found on deal #" + transaction.getDealTrackingId());
				return;
			}
			if (rl ==  RetrievalLogic.DEFAULT && transactionData.getInt("ext_int_ext", 0) == 0) {
				PluginLog.info("External Party #" + transactionData.getInt("ext_party_id", 0) 
						+ " of transation #" + transaction.getTransactionId()
						+ " is an internal party. No Taxes are assigned");
				return;
			}


			if (viewTables) {
				context.getDebug().viewTable(transactionData);
			}			
			String extBu="";
			switch (rl) {
			case DEFAULT:
				extBu = transaction.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);
				break;
			case CASH_TRANSFER:
				extBu = transaction.getValueAsString(EnumTransactionFieldId.ToBusinessUnit);
				break;
			case CASH_TRANSFER_PASS_THROUGH:
			case CASH_TRANSFER_CHARGES:
				if (transaction.getField(EnumTransactionFieldId.ExternalBusinessUnit).isApplicable()) {
					extBu = transaction.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);				
				}
				break;			
			}
			
			if (rl == RetrievalLogic.DEFAULT && transactionData.getString("to_bu_internal", 0).trim().equalsIgnoreCase("Yes") 
					&& !cpForceVAT.contains(extBu)) {
				PluginLog.info("Skipping calculation and setting of tax sub type as party info field '" + PartyInfoFields.JM_GROUP.getName() + "' is set to 'yes'");
				return;
			}
			List<Exception> exceptions = new ArrayList<Exception> ();
			for (TableRow row : transactionData.getRows()) {
				try {
					String taxType = transactionData.getString("int_country_iso", row.getNumber());						
					if (taxType == null || taxType.isEmpty()) {
						String countryIntAddress = transactionData.getString("internal_entity", 0);
						throw new RuntimeException ("Could not find country for the Internal Entity "
								+ countryIntAddress + ". Please check the main address.");
					}
					String taxSubType = getTaxSubType (row, context, transaction.getTransactionId(), getRetrievalLogic(transaction));

					verifyTaxType (taxType, context);
					verifyTaxSubtype(taxSubType, context);

					setTaxTypeAndSubType(transaction, taxType, taxSubType, row.getInt("leg_no"), row.getInt ("fee_no"));
				} catch (Exception ex) {
					exceptions.add(ex);
				}
			}
			if (exceptions.size() > 0) {
				StringBuilder message = new StringBuilder ("");
				for (Exception ex : exceptions) {
					message.append("\n").append(ex.toString());
				}
				throw new RuntimeException (message.toString());
			}
			PluginLog.info ("Processed transaction no #" + transaction.getTransactionId());
		} finally {
			if (transactionData != null) {
				transactionData.dispose();
				transactionData = null;
			}
		}
	}

	private boolean isCommPhysLegRelevant(Leg leg) {
		return true;
		//		return leg.getLegNumber() != 0; // !leg.isPhysicalCommodity() && leg.getLegNumber() != 0 && leg.getLegNumber() % 2 == 0;
	}

	private void verifyTaxType(final String taxType, final Context context) {
		String sql = "SELECT 1 FROM tax_tran_type WHERE type_name = '"+taxType+"'";
		Table tbl = null;
		try
		{
			tbl = context.getIOFactory().runSQL(sql);
			if (tbl.getRowCount() <= 0) {
				throw new RuntimeException("Tax Type \"" + taxType + "\" doesn't exist");
			}
		}
		finally { 
			if (tbl != null) {
				tbl.dispose();
				tbl = null;
			}
		}
	}

	private void verifyTaxSubtype(final String taxSubtype, final Context context) {
		if (taxSubtype.isEmpty()) {
			return;
		}
		String sql = "SELECT 1 FROM tax_tran_subtype WHERE subtype_name = '"+taxSubtype+"'";
		Table tbl = null;
		try
		{
			tbl = context.getIOFactory().runSQL(sql);
			if (tbl.getRowCount() <= 0)
				throw new RuntimeException("Tax Subtype \"" + taxSubtype + "\" doesn't exist");
		}
		finally { 	
			if (tbl != null) {
				tbl.dispose();
				tbl = null;
			}
		}
	}

	private String tranLevelCflowRetrieval(final Transaction toBeUsed,
			final RetrievalLogic logic) {
		Field cflowTypeField = null;

		switch (logic) {
		case DEFAULT:
		case CASH_TRANSFER_CHARGES:
			cflowTypeField = toBeUsed.getField(EnumTransactionFieldId.CashflowType);
			break;
		case CASH_TRANSFER:
		case CASH_TRANSFER_PASS_THROUGH:
			cflowTypeField = toBeUsed.getField(EnumTransactionFieldId.FromCashFlowType);			
			break;
		}

		EnumInsType insType = toBeUsed.getInstrument().getInstrumentTypeObject().getInstrumentTypeEnum();
		if (insType != EnumInsType.FxInstrument && insType != EnumInsType.CashInstrument) {
			return "None";
		}
		if (cflowTypeField != null && cflowTypeField.isApplicable()) {
			String cflowType = cflowTypeField.getDisplayString();
			return cflowType;
		}
		return "None";
	}

	private String feeLevelCflowRetrieval(Fee fee ) {
		Field feeDef = fee.getField(EnumFeeFieldId.Definition);
		if (feeDef != null && feeDef.isApplicable() && feeDef.isReadable()) {
			int feeDefId = feeDef.getValueAsInt();
			String cflowOfFee = feeTypeToCashFlow.get(feeDefId);
			return cflowOfFee;
		} else {
			return "None";					
		}
	}

	private String getTaxSubType(final TableRow row, final Context context, final int tranNum, final RetrievalLogic logic) {
		UserTable rulesTransfersU = null;
		UserTable rulesTradesU = null;
		Table rulesTransfers=null;
		Table rulesTrades=null;

		try {
			// exception should be thrown in case the table can't be retrieved.
			rulesTransfersU = context.getIOFactory().getUserTable(USER_TABLE_RULES_TRANSFERS);
			rulesTradesU = context.getIOFactory().getUserTable(USER_TABLE_RULES_TRADES);
			rulesTransfers = rulesTransfersU.retrieveTable();
			rulesTrades = rulesTradesU.retrieveTable();
			if (viewTables) {
				//				context.getDebug().viewTable(rulesTrades);
				//				context.getDebug().viewTable(rulesTransfers);
			}
			switch (logic) {
			case DEFAULT:
			case CASH_TRANSFER_CHARGES:
				return getTaxSubtypeMetalDeal (row, rulesTrades, tranNum, rulesTransfers, row.getInt("leg_no"));
			case CASH_TRANSFER:
			case CASH_TRANSFER_PASS_THROUGH:
				if (TranInfoFields.FROM_ACCOUNT.retrieveId(context) == 0 
				|| TranInfoFields.TO_ACCOUNT.retrieveId(context) == 0) {
					throw new RuntimeException ("Tran info fields necessary for processing of metal transfers have not been set up.");
				}
				return getTaxSubTypeMetalTransfer(row, rulesTransfers, tranNum);
			}

			throw new RuntimeException ("Unknown retrieval logic for getTaxSubType");
		} finally {
			if (rulesTransfersU != null) {
				rulesTransfersU.dispose();
				rulesTransfersU = null;
			}
			if (rulesTradesU != null) {
				rulesTradesU.dispose();
				rulesTradesU = null;
			}
			if (rulesTransfers != null) {
				rulesTransfers.dispose();
				rulesTransfers = null;
			}
			if (rulesTrades != null) {
				rulesTrades.dispose();
				rulesTrades = null;
			}
		}
	}

	private String getTaxSubTypeMetalTransfer(final TableRow row, 
			final Table rulesTransfers, final int tranNum) {
		PluginLog.info("Applying metal transfer tax type determination logic");
		String fromAccountCountry = row.getString("from_account_country");
		String toAccountCountry = row.getString("to_account_country");
		String toAccountRegion = row.getString("to_account_region");

		String jmGroup = row.getString("to_bu_internal");
		String metal = row.getString("metal");
		String extLbma = row.getString("lbma");
		String extLppm = row.getString("lppm");
		Map<Integer, Integer> matchingRows = new HashMap<> ();

		for (int rowId=rulesTransfers.getRowCount()-1; rowId>=0; rowId--) {
			RowMatch isActive = checkRuleColumn ("active", rulesTransfers, rowId, "Yes");
			RowMatch matchesFromAccountCountry = 
					checkRuleColumn ("from_account_country", rulesTransfers, rowId, fromAccountCountry);
			RowMatch matchesToAccountCountry = checkRuleColumn ("to_account_country", rulesTransfers, rowId, toAccountCountry);
			RowMatch matchesToAccountRegion = checkRuleColumn ("to_account_region", rulesTransfers, rowId, toAccountRegion);
			RowMatch matchesBuInternal = checkRuleColumn ("to_bu_internal", rulesTransfers, rowId, jmGroup);
			RowMatch matchesMetal = checkRuleColumn ("metal", rulesTransfers, rowId, metal);
			RowMatch matchesLbma = checkRuleColumn ("lbma", rulesTransfers, rowId, extLbma);
			RowMatch matchesLppm = checkRuleColumn ("lppm", rulesTransfers, rowId, extLppm);
			int weight=sumWeight(isActive, matchesFromAccountCountry, matchesToAccountCountry,
					matchesToAccountRegion, matchesBuInternal, matchesMetal, matchesLbma,
					matchesLppm);			

			if (matchAll (isActive, matchesFromAccountCountry, matchesToAccountCountry, matchesToAccountRegion
					,matchesBuInternal,  matchesMetal, matchesLbma, matchesLppm)) {
				matchingRows.put(rowId, weight);
			}
		}
		if (matchingRows.size() == 0) {
			logNoTaxSubtypeMatchWarningTransfers(tranNum, fromAccountCountry, toAccountCountry,
					toAccountRegion, jmGroup, metal, extLbma, extLppm);
			return "";
		}

		if (matchingRows.size() > 1) {
			switch (ruleSelectionLogic) {
			case BLOCK:
				if (sameTaxSubTypeForAllMatches(rulesTransfers, matchingRows.keySet())) {
					return retrieveTaxSubtypeOfLeastRow(rulesTransfers, matchingRows);
				}				
				showMoreThanOneTaxSubtypeErrorTransfers(rulesTransfers, fromAccountCountry, toAccountCountry,
						toAccountRegion, jmGroup, metal, extLbma, extLppm, matchingRows);
				break;
			case BY_ORDER:
				return retrieveTaxSubtypeOfLeastRow(rulesTransfers, matchingRows);
			case WEIGHTED_BLOCK:
				return retrieveTaxSubtypeOfMinimalWeightTransfers (rulesTransfers, matchingRows, fromAccountCountry,
						toAccountCountry, toAccountRegion, jmGroup, metal, extLbma, extLppm);
			}			
			return "";
		}
		return retrieveTaxSubtypeOfLeastRow(rulesTransfers, matchingRows);
	}


	private String retrieveTaxSubtypeOfMinimalWeightTransfers(
			Table rulesTransfers, Map<Integer, Integer> matchingRows,
			String fromAccountCountry, String toAccountCountry, 
			String toAccountRegion, String jmGroup,
			String metal, String extLbma, String extLppm) {
		int minWeight = Integer.MAX_VALUE;
		List<Integer> minMatchingRowId = new ArrayList<>();
		for (Integer rowId : matchingRows.keySet()) {
			if (matchingRows.get(rowId) < minWeight) {
				minMatchingRowId.clear();
				minMatchingRowId.add(rowId);
				minWeight = matchingRows.get(rowId);
			} else if (matchingRows.get(rowId) == minWeight) {
				minMatchingRowId.add(rowId);						
			}
		}
		if (minMatchingRowId.size() > 1) {
			if (sameTaxSubTypeForAllMatches(rulesTransfers, minMatchingRowId)) {
				logTableRow (rulesTransfers, minMatchingRowId.get(0));
				return rulesTransfers.getString ("tax_subtype", minMatchingRowId.get(0));
			}				

			showMoreThanOneTaxSubtypeErrorTransfers(rulesTransfers, fromAccountCountry, toAccountCountry,
					toAccountRegion, jmGroup, metal, extLbma, extLppm, matchingRows);					
		} else {
			logTableRow (rulesTransfers, minMatchingRowId.get(0));
			return rulesTransfers.getString ("tax_subtype", minMatchingRowId.get(0));
		}		
		return "";

	}

	private void showMoreThanOneTaxSubtypeErrorTransfers(final Table rulesTransfers,
			final String fromAccountCountry, final String toAccountCountry,
			final String toAccountRegion, final String jmGroup, final String metal,
			final String extLbma, final String extLppm, final Map<Integer, Integer> matchingRows) {
		String errorMessage = "No Tax SubType can be assigned:"
				+ " multiple matches in the table " + USER_TABLE_RULES_TRANSFERS;
		errorMessage += "\nDetails from Deal: %-23s";
		errorMessage += " from_account_country = %-15s";
		errorMessage += " to_account_country = %-15s";
		errorMessage += " to_account_region = %-15s";
		errorMessage += " to_bu_internal = %-15s";
		errorMessage += " metal = %-3s";
		errorMessage += " lbma = %-15s";
		errorMessage += " lppm = %-15s";
		errorMessage = String.format(errorMessage, "", fromAccountCountry, toAccountCountry, toAccountRegion, jmGroup, metal, extLbma, extLppm);
		for (Integer rowId : matchingRows.keySet()) {
			errorMessage += "\nMatching RuleData row = %-5d";
			errorMessage += " active = %s-1d";
			errorMessage += " from_account_country = %-15s";
			errorMessage += " to_account_country = %-15s";
			errorMessage += " to_account_region = %-15s";
			errorMessage += " to_bu_internal = %-15s";
			errorMessage += " metal = %-3s";
			errorMessage += " lbma = %-15s";
			errorMessage += " lppm = %-15s";
			errorMessage += " tax_subtype = %-15s";
			errorMessage += " weight = %-5s";
			errorMessage = String.format(errorMessage,  rowId, rulesTransfers.getString("active", rowId), 
					rulesTransfers.getString("active", rowId),  rulesTransfers.getString("from_account_country", rowId),
					rulesTransfers.getString("to_account_country", rowId), rulesTransfers.getString("to_account_region", rowId),
					rulesTransfers.getString("to_bu_internal", rowId), rulesTransfers.getString("metal", rowId), 
					rulesTransfers.getString("lbma", rowId), rulesTransfers.getString("lppm", rowId),
					rulesTransfers.getString("tax_subtype", rowId), matchingRows.get(rowId));
		}
		throw new RuntimeException (errorMessage);		
	}

	private void logNoTaxSubtypeMatchWarningTransfers(final int tranNum,
			final String fromAccountCountry, final String toAccountCountry,
			final String toAccountRegion, final String jmGroup, final String metal,
			final String extLbma, final String extLppm) {
		String warnMessage = "No matching row found for deal " + tranNum + " with:";
		warnMessage += "\nfromAccountCountry = " + fromAccountCountry;
		warnMessage += "\ntoAccountCountry = " + toAccountCountry;
		warnMessage += "\ntoAccountRegion = " + toAccountRegion;
		warnMessage += "\njmGroup = " + jmGroup;
		warnMessage += "\nmetal = " + metal;
		warnMessage += "\nextLbma = " + extLbma;
		warnMessage += "\nextLppm = " + extLppm;						
		PluginLog.warn(warnMessage);
	}

	/**
	 * Returns the tax sub type for the a metal deal. Out of scope is denoted by the empty string ("").
	 * @param row
	 * @param rulesTrades
	 * @param rulesTrades 
	 * @param cashFlowTypesLinked 
	 * @return
	 */
	private String getTaxSubtypeMetalDeal(final TableRow row,
			final Table rulesTrades, final int tranNum,
			final Table rulesTransfers, final int legNo) {
		PluginLog.info("Applying metal tax sub type determination logic");
		String jmGroup = row.getString("int_jmgroup");
		if (jmGroup.equalsIgnoreCase("Yes")) {
			PluginLog.info("Skipping assignment of tax subtype as internal party is in JM Group");
			return "";
		}
		String intPartyShortName = row.getString("internal_entity");
		String buySell = row.getString("buy_sell");
		String extLERegion = row.getString("ext_le_region");
		String metal = row.getString("metal");
		String extLbma = row.getString("lbma");
		String extLppm = row.getString("lppm");
		String cflowType = row.getString("cash_flow_type");
		int feeNo = row.getInt("fee_no");
		Map<Integer, Integer> matchingRows = new HashMap<> ();

		for (int rowId=rulesTrades.getRowCount()-1; rowId>=0; rowId--) {
			int weight=0;
			RowMatch isActive = checkRuleColumn ("active", rulesTrades, rowId, "Yes");
			RowMatch matchesInternalEntity = 
					checkRuleColumn ("internal_entity", rulesTrades, rowId, intPartyShortName);
			RowMatch matchesBuySell = checkRuleColumn ("buy_sell", rulesTrades, rowId, buySell);
			RowMatch matchesExtLeRegion = checkRuleColumn ("ext_le_region", rulesTrades, rowId, extLERegion);
			RowMatch matchesMetal = checkRuleColumn ("metal", rulesTrades, rowId, metal);
			RowMatch matchesLbma = checkRuleColumn ("lbma", rulesTrades, rowId, extLbma);
			RowMatch matchesLppm = checkRuleColumn ("lppm", rulesTrades, rowId, extLppm);
			RowMatch matchesCashFlow = checkRuleColumn ("cash_flow_type", rulesTrades, rowId, cflowType);
			weight = sumWeight (isActive, matchesInternalEntity, matchesBuySell,
					matchesExtLeRegion, matchesMetal, matchesLbma, matchesLppm, matchesCashFlow);
			if (matchAll (isActive, matchesInternalEntity, matchesBuySell, matchesExtLeRegion,
					matchesMetal, matchesLbma, matchesLppm, matchesCashFlow)) {
				matchingRows.put(rowId, weight);
			}
		}
		if (matchingRows.size() == 0) {
			logNoTaxSubtypeMatchWarning(tranNum, intPartyShortName, buySell,
					extLERegion, metal, extLbma, extLppm, legNo, feeNo, cflowType);
			return "";
		}
		if (matchingRows.size() > 1) {
			switch (ruleSelectionLogic) {
			case BLOCK:
				if (sameTaxSubTypeForAllMatches(rulesTrades, matchingRows.keySet())) {
					return retrieveTaxSubtypeOfLeastRow (rulesTrades, matchingRows); 
				}
				showMoreThanOneTaxSubtypeError(rulesTrades, intPartyShortName,
						buySell, extLERegion, metal, extLbma, extLppm, matchingRows, legNo, feeNo, cflowType);
				break;
			case BY_ORDER:
				return retrieveTaxSubtypeOfLeastRow(rulesTrades, matchingRows);
			case WEIGHTED_BLOCK:
				return retrieveTaxSubtypeOfMinimalWeight (rulesTrades, matchingRows, intPartyShortName, buySell,
						extLERegion, metal, extLbma, extLppm, legNo, feeNo, cflowType);
			}			
			return retrieveTaxSubtypeOfLeastRow(rulesTrades, matchingRows);
		}
		return retrieveTaxSubtypeOfLeastRow(rulesTrades, matchingRows);
	}

	private boolean sameTaxSubTypeForAllMatches(Table rulesTrades,
			Collection<Integer> rowIds) {
		Set<String> distinctTaxSubtypes= new HashSet<> ();
		for (Integer rowId : rowIds) {
			distinctTaxSubtypes.add(rulesTrades.getString ("tax_subtype", rowId));
		}
		return  distinctTaxSubtypes.size() == 1;
	}

	private String retrieveTaxSubtypeOfMinimalWeight (final Table rulesTrades,
			Map<Integer, Integer> matchingRows,
			String intPartyShortName, String buySell, String extLERegion, 
			String metal, String extLbma, String extLppm, int legNo, int feeNo,
			String cflowType) {
		int minWeight = Integer.MAX_VALUE;
		List<Integer> minMatchingRowId = new ArrayList<>();
		for (Integer rowId : matchingRows.keySet()) {
			if (matchingRows.get(rowId) < minWeight) {
				minMatchingRowId.clear();
				minMatchingRowId.add(rowId);
				minWeight = matchingRows.get(rowId);
			} else if (matchingRows.get(rowId) == minWeight) {
				minMatchingRowId.add(rowId);
			}
		}
		if (minMatchingRowId.size() > 1) {
			if (sameTaxSubTypeForAllMatches(rulesTrades, minMatchingRowId)) {
				logTableRow (rulesTrades, minMatchingRowId.get(0));
				return rulesTrades.getString ("tax_subtype", minMatchingRowId.get(0)); 
			}

			showMoreThanOneTaxSubtypeError(rulesTrades, intPartyShortName,
					buySell, extLERegion, metal, extLbma, extLppm, matchingRows, legNo, feeNo,
					cflowType);					
		} else {
			logTableRow (rulesTrades, minMatchingRowId.get(0));
			return rulesTrades.getString ("tax_subtype", minMatchingRowId.get(0));
		}		
		return "";
	}

	private String retrieveTaxSubtypeOfLeastRow(final Table rulesTrades,
			Map<Integer, Integer> matchingRows) {
		List<Integer> rows = new ArrayList<>(matchingRows.keySet());
		Collections.sort(rows);
		int rowId = rows.get(0); 
		logTableRow (rulesTrades, rowId);
		return rulesTrades.getString ("tax_subtype", rowId);
	}

	private void logTableRow (Table tab, int rowId) {
		StringBuilder sb = new StringBuilder ();
		for (TableColumn col : tab.getColumns()) {
			sb.append(col.getName());
			sb.append(" = ");
			sb.append(col.getDisplayString(rowId));
			sb.append(";");
		}
		PluginLog.info(sb.toString());

	}

	private boolean matchAll(RowMatch ... matches) {
		for (RowMatch match : matches) {
			if (!match.isMatchRow()) {
				return false;
			}				
		}
		return true;
	}

	private int sumWeight(RowMatch ...  matches) {
		int weight = 0;
		for (RowMatch match : matches) {
			weight += match.getWeight();		
		}
		return weight;
	}

	private void showMoreThanOneTaxSubtypeError(final Table rulesTrades,
			final String intPartyShortName, final String buySell, final String extLERegion,
			final String metal, final String extLbma, final String extLppm, 
			final Map<Integer, Integer> matchingRows, final int legNo,
			final int feeNo, final String cflowType) {
		String errorMessage = "No Tax SubType can be assigned:"
				+ " multiple matches in the table " +  USER_TABLE_RULES_TRADES;
		errorMessage += "\nDetails from Deal: %-20s";
		errorMessage += " internal_entity = %-15s";
		errorMessage += " buy_sell = %-5s";
		errorMessage += " ext_le_region = %-15s"; 
		errorMessage += " metal = %-15s"; 
		errorMessage += " lbma = %-15s"; 
		errorMessage += " lppm = %-15s"; 
		errorMessage += " cash_flow_type = %-30s"; 
		errorMessage += " legNo = %-5s";
		errorMessage += " feeNo = %-5s";
		errorMessage = String.format(errorMessage, "", intPartyShortName, buySell, 
				extLERegion, metal, extLbma, extLppm, cflowType, "" + legNo, "" + feeNo);
		for (Integer rowId: matchingRows.keySet()) {
			errorMessage += "\nMatching RuleData row = %-3d";
			errorMessage += " active = %-2s"; 
			errorMessage += " internal_entity = %-15s"; 
			errorMessage += " buy_sell = %-5s"; 
			errorMessage += " ext_le_region = %-15s"; 
			errorMessage += " metal = %-15s"; 
			errorMessage += " lbma = %-15s"; 
			errorMessage += " lppm = %-15s";
			errorMessage += " cash_flow_type = %-15s";
			errorMessage += " tax_subtype = %-15s";
			errorMessage += " weight = %-5s";
			errorMessage = String.format(errorMessage, rowId, rulesTrades.getString("active", rowId), rulesTrades.getString("internal_entity", rowId),
					rulesTrades.getString("buy_sell", rowId), rulesTrades.getString("ext_le_region", rowId), 
					rulesTrades.getString("metal", rowId), rulesTrades.getString("lbma", rowId), 
					rulesTrades.getString("lppm", rowId), rulesTrades.getString("cash_flow_type", rowId), 
					rulesTrades.getString("tax_subtype", rowId), matchingRows.get(rowId));
		}
		throw new RuntimeException (errorMessage);
	}

	private void logNoTaxSubtypeMatchWarning(final int tranNum,
			final String intPartyShortName, final String buySell, final String extLERegion,
			final String metal, final String extLbma, final String extLppm,
			final int legNo, final int feeNo, final String cashFlowType) {
		String warnMessage = "No matching row found for deal " + tranNum + " with:";
		warnMessage += "\nleg_no = " + legNo;
		warnMessage += "\nfee_no = " + feeNo;
		warnMessage += "\nint_short_name = " + intPartyShortName;
		warnMessage += "\nbuySell = " + buySell;
		warnMessage += "\nextLERegion = " + extLERegion;
		warnMessage += "\nmetal = " + metal;
		warnMessage += "\nextLbma = " + extLbma;
		warnMessage += "\nextLppm = " + extLppm;						
		warnMessage += "\ncash_flow_type = " + cashFlowType;						
		PluginLog.warn(warnMessage);
	}

	private RowMatch checkRuleColumn(final String colName, final Table rules, final int rowId,
			final String allowedValue) {
		String columnValue = rules.getString(colName, rowId);
		if (allowedValue.equalsIgnoreCase("Not found")) {
			return new RowMatch(true, 0);
		}
		boolean meetsNot=false;
		for (String token : columnValue.split(SEPARATOR)) {
			String trimmedToken = token.trim();

			if (trimmedToken.equalsIgnoreCase(ALL) || trimmedToken.isEmpty()) {
				return new RowMatch(true, 2);
			}
			if (trimmedToken.startsWith(NOT)) {
				String forbiddenValue = trimmedToken.substring(NOT.length());
				if (forbiddenValue.equalsIgnoreCase(allowedValue)) {
					return new RowMatch(false, 1);
				}
				else {
					meetsNot = true;
				}
			}
			if (trimmedToken.equalsIgnoreCase(allowedValue)) {
				return new RowMatch(true, 0);
			}
		}
		if (meetsNot) {
			return new RowMatch(true, 1);
		}
		return new RowMatch(false, 0);
	}

	/**
	 * Given the pointer to the Transaction, set the Tax Type and Sub-Type
	 * @param tran
	 * @param taxType
	 * @param taxSubType
	 * @param legNo 
	 * @throws OException
	 */
	private void setTaxTypeAndSubType(final Transaction tran, final String taxType, final String taxSubType, 
			final int legNo, final int feeNo) {
		PluginLog.info("Ensuring Tax Type to be " + taxType + " and Tax Sub Type to be " + taxSubType);
		Field cflowTypeField = tran.getField(EnumTransactionFieldId.CashflowType);
		String cflowType = (cflowTypeField != null && cflowTypeField.isApplicable())?cflowTypeField.getDisplayString():"";

		if (cflowType.equalsIgnoreCase("Location Swap") || cflowType.equalsIgnoreCase("Quality Swap")) {
			normalTaxAssignment(tran, taxType, taxSubType, legNo, feeNo);
		} else {
			normalTaxAssignment(tran, taxType, taxSubType, legNo, feeNo);
		}
	}

	private void normalTaxAssignment(final Transaction tran,
			final String taxType, final String taxSubType, int legNo, int feeNo) {
		PluginLog.info("Applying default (non swap) logic to set Tax Type / Tax Sub Type");

		if (legNo == -1) {
			normalTaxAssignmentTranLevel(tran, taxType, taxSubType);
		} else {
			normalTaxAssignmentLegLevel(tran, taxType, taxSubType, legNo, feeNo);			
		}
	}

	private void normalTaxAssignmentLegLevel(Transaction tran, String taxType,
			String taxSubType, int legNo, int feeNo) {
		String test = tran.getField(EnumTransactionFieldId.TaxTransactionType).getDisplayString();
		if (test == null || !test.equals(taxType)) {
			Field taxTypeField = tran.getLeg(legNo).getFee(feeNo).getField(EnumFeeFieldId.TaxType);
			if (taxTypeField != null && taxTypeField.isApplicable() && taxTypeField.isWritable()){ 
				taxTypeField.setValue(taxType);
			} else {
				PluginLog.warn("Could not set Tax Type on transaction #" + tran.getTransactionId()
						+ " to value " + taxType + " because the field is not applicable");
			}
		}
		test = tran.getField(EnumTransactionFieldId.TaxTransactionSubtype).getDisplayString();
		if (test == null || !test.equals(taxType))
		{
			Field taxSubTypeField = tran.getLeg(legNo).getFee(feeNo).getField(EnumFeeFieldId.TaxSubType);

			if (taxSubTypeField != null && taxSubTypeField.isApplicable() && taxSubTypeField.isWritable()) {
				taxSubTypeField.setValue(taxSubType);				
			} else {
				PluginLog.warn("Could not set Tax Sub Type on transaction #" + tran.getTransactionId()
						+ " leg #" + legNo + " fee #" + feeNo
						+ " to value " + taxSubType + " because the field is not applicable");				
			}
		}		
	}

	private void normalTaxAssignmentTranLevel(final Transaction tran,
			final String taxType, final String taxSubType) {
		String test = tran.getField(EnumTransactionFieldId.TaxTransactionType).getDisplayString();
		if (test == null || !test.equals(taxType))
		{
			Field taxTypeField = tran.getField(EnumTransactionFieldId.TaxTransactionType);
			if (taxTypeField != null && taxTypeField.isApplicable() && taxTypeField.isWritable()){ 
				tran.setValue(EnumTransactionFieldId.TaxTransactionType, taxType);
			} else {
				PluginLog.warn("Could not set Tax Type on transaction #" + tran.getTransactionId()
						+ " to value " + taxType + " because the field is not applicable");
			}
		}
		test = tran.getField(EnumTransactionFieldId.TaxTransactionSubtype).getDisplayString();
		if (test == null || !test.equals(taxType))
		{
			Field taxSubTypeField = tran.getField(EnumTransactionFieldId.TaxTransactionSubtype);

			if (taxSubTypeField != null && taxSubTypeField.isApplicable() && taxSubTypeField.isWritable()) {
				tran.setValue(EnumTransactionFieldId.TaxTransactionSubtype, taxSubType);				
			} else {
				PluginLog.warn("Could not set Tax Sub Type on transaction #" + tran.getTransactionId()
						+ " to value " + taxSubType + " because the field is not applicable");				
			}
		}
	}

	private Table retrieveTransactionData(final Transaction tran, final Context context, final int origTran,
			final int linkedDeal, final int legNo, final String cashFlowType,
			final int feeId) {	
		int intLe = 0;
		int extLe = 0;
		String buySell = tran.getValueAsString(EnumTransactionFieldId.BuySell);
		RetrievalLogic rl = getRetrievalLogic(tran);
		
		switch (rl) {
		case DEFAULT:
			intLe = tran.getValueAsInt (EnumTransactionFieldId.InternalLegalEntity);
			extLe = tran.getValueAsInt(EnumTransactionFieldId.ExternalLegalEntity);
			break;	
		case CASH_TRANSFER:
			intLe = tran.getValueAsInt (EnumTransactionFieldId.FromLegalEntity);
			extLe = tran.getValueAsInt(EnumTransactionFieldId.ToLegalEntity);
			break;
		case CASH_TRANSFER_CHARGES:
			buySell = "Sell";
		case CASH_TRANSFER_PASS_THROUGH:
			if (tran.getField(EnumTransactionFieldId.InternalLegalEntity).isApplicable()) {
				intLe = tran.getValueAsInt (EnumTransactionFieldId.InternalLegalEntity);
				extLe = tran.getValueAsInt(EnumTransactionFieldId.ExternalLegalEntity);				
			} else {
				return null;
			}
			break;			
		}

		int idLbma = PartyInfoFields.LBMA_MEMBER.retrieveId(context);
		int idLppm = PartyInfoFields.LPPM_MEMBER.retrieveId(context);
		int idJmGroup = PartyInfoFields.JM_GROUP.retrieveId(context);
		int insType = tran.getValueAsInt(EnumTransactionFieldId.InstrumentType);
		int idToAccountBU = TranInfoFields.TO_ACCOUNT_BU.retrieveId(context);
		int idFromAccountBU = TranInfoFields.FROM_ACCOUNT_BU.retrieveId(context);
		int idForceVat = TranInfoFields.FORCE_VAT.retrieveId(context);

		String metal = extractMetal(context, tran, insType, legNo, linkedDeal);
		String sql = "";
		
		switch (rl) {
		case DEFAULT:			
		case CASH_TRANSFER_CHARGES:
			sql = getLocationSqlDefaultLogic(intLe, extLe, buySell, idLbma, idLppm, 
					idJmGroup, cashFlowType, metal, idForceVat, linkedDeal, legNo,
					feeId);
			break;
		case CASH_TRANSFER:
		case CASH_TRANSFER_PASS_THROUGH:
			sql = getLocationSqlTransferLogic(buySell, idLbma, idLppm, 
					idJmGroup, cashFlowType, metal, idToAccountBU, idFromAccountBU, idForceVat, linkedDeal, legNo,
					feeId);
			break;
		}
		
		Table transactionData = context.getIOFactory().runSQL(sql);
		if (transactionData.getRowCount() == 0) {
			
			transactionData.dispose();
			throw new RuntimeException ("Could not retrieve relevant transaction data."
					+ " This is can be due to missing or wrong party configuration."
					+ " Please check party address configration, especially country assignment,"
					+ " party info fields " + PartyInfoFields.getNameList() + " and the country in the party address field"
					+ " and the country / region configuration. Also mind the transaction info"
					+ " fields " + TranInfoFields.getNameList() 
					+ " for cash transfers. SQL:\n" + sql);
		}
		if (transactionData.getRowCount() > 1) {
			transactionData.dispose();
			throw new RuntimeException ("Retrieved more than 1 relevant row of transaction data."
					+ sql);
		}
		switch (rl) {
		case DEFAULT:
		case CASH_TRANSFER_CHARGES:
			processDefaultTransactionData(rl, transactionData);
			break;
		case CASH_TRANSFER:
		case CASH_TRANSFER_PASS_THROUGH:
			processTransferTransactionData(context, rl, idLppm, idLbma, buySell, transactionData);
			break;
		}

		return transactionData;
	}

	private void processTransferTransactionData(final Context context, final RetrievalLogic rl,
			final int idLppm, final int idLbma, String buySell, 
			final Table transactionData) {
		Map<String, String> buToLPPM = DBHelper.loadBUToLEInfoMap(context, idLppm, defaultLPPMMember);
		Map<String, String> buToLBMA = DBHelper.loadBUToLEInfoMap (context, idLbma, defaultLPPMMember);
		Map<String, String> buToCountryMap = DBHelper.loadPartyToCountryMap(context);
		Map<String, String> countryToRegionMap = DBHelper.loadCountryToRegionMap (context);

		String mappedLPPM = null;
		String mappedLBMA = null;
		for (TableRow row : transactionData.getRows()) {
			String toAccountBu = row.getString ("to_account_bu");
			String fromAccountBu = row.getString ("from_account_bu");
			if (rl != RetrievalLogic.CASH_TRANSFER_CHARGES) {
				mappedLPPM = (buToLPPM.get(toAccountBu) != null && !buToLPPM.get(toAccountBu).equals(""))?
						buToLPPM.get(toAccountBu):"";
				mappedLBMA = (buToLBMA.get(toAccountBu) != null && !buToLBMA.get(toAccountBu).equals(""))?
						buToLBMA.get(toAccountBu):"";
			} else {
				if (buySell.trim().equalsIgnoreCase("buy")) {
					buySell = "Sell";
				} else {
					buySell = "Buy";
				}
				transactionData.setString("buy_sell", row.getNumber(), buySell);	
				mappedLPPM = (buToLPPM.get(toAccountBu) != null && !buToLPPM.get(toAccountBu).equals(""))?
						buToLPPM.get(toAccountBu):"";
				mappedLBMA = (buToLBMA.get(toAccountBu) != null && !buToLBMA.get(toAccountBu).equals(""))?
						buToLBMA.get(toAccountBu):"";
			}
			transactionData.setString("lppm", row.getNumber(), mappedLPPM);
			transactionData.setString("lbma", row.getNumber(), mappedLBMA);
			
			String mappedFromCountryBu = (buToCountryMap.get(fromAccountBu) != null &&
					!buToCountryMap.get(fromAccountBu).equals("") )?
					 buToCountryMap.get(fromAccountBu):"";

			String mappedToCountryBu = (buToCountryMap.get(toAccountBu) != null &&
					!buToCountryMap.get(toAccountBu).equals("") )?
					 buToCountryMap.get(toAccountBu):"";
			String mappedToRegionBu = countryToRegionMap.get(mappedToCountryBu);										    

			String forceVAT = row.getString("force_vat");
			if (forceVAT.equalsIgnoreCase("Yes")) {
				mappedFromCountryBu = "US";
			}
			
			transactionData.setString("from_account_country", row.getNumber(), mappedFromCountryBu);
			transactionData.setString("to_account_country", row.getNumber(), mappedToCountryBu);
			transactionData.setString("to_account_region", row.getNumber(), mappedToRegionBu);

		}
	}

	private void processDefaultTransactionData(RetrievalLogic rl,
			Table transactionData) {
		
		
		
		for (TableRow row : transactionData.getRows()) {
			String intCountryIso = row.getString("int_country_iso");
			String extCountryIso = row.getString("ext_country_iso");

			if (intCountryIso.equals(extCountryIso)) {
				transactionData.setString("int_geographic_zone", row.getNumber(), intCountryIso + DOMESTIC);
				transactionData.setString("ext_le_region", row.getNumber(), extCountryIso + DOMESTIC);
			}
		}
	}
	
	private String extractMetal(final Context context, final Transaction tran, final int insType, 
			final int legNo, final int linkedDeal) {
		String metal ="Not Found";
		switch (getRetrievalLogic(tran)) {
		default:
			if (insType == loanMLInsType || insType == depoMLInsType) {
				metal = tran.getLeg(1).getDisplayString(EnumLegFieldId.NotionalCurrency);
			} else if (insType == cashInsType){
				metal = tran.getDisplayString(EnumTransactionFieldId.Currency);
			} else if (insType == metalSwapInsType) {
				metal = tran.getLeg(0).getDisplayString(EnumLegFieldId.Currency);
				if (!relevantMetals.contains(metal)) { // check other leg if necessary
					metal = tran.getLeg(1).getDisplayString(EnumLegFieldId.Currency);
				}
			} else if (insType == fxInsType) {
				metal = tran.getLeg(0).getDisplayString(EnumLegFieldId.BaseCurrency);
				if (metal == null || metal.isEmpty() || metal.length() < 3 || 
						!relevantMetals.contains(metal.substring(0, 3))) {
					metal = tran.getLeg(1).getDisplayString(EnumLegFieldId.BaseCurrency);
				}
				if (metal.length() > 3) { // for FX deals, metal 
					metal = metal.substring(0,3);
				}
			} else if (insType == commPhysInsType) {
				int commodityLegNo = tran.getLeg(legNo).getValueAsInt(EnumLegFieldId.ParamGroup);
				if (commodityLegNo == 0) {
					return "None";
				}
				commodityLegNo++;
				metal = tran.getLeg(commodityLegNo).getDisplayString(EnumLegFieldId.Currency);
				if (metal.length() > 3) { // for FX deals, metal 
					metal = metal.substring(0,3);
				}
			}
			break;
		case CASH_TRANSFER_CHARGES:
			int idMetalInfoType = TranInfoFields.METAL.retrieveId(context);
			metal = DBHelper.loadMetalFromStrategy (context, linkedDeal, 
					idMetalInfoType, defaultMetal);
			break;
		}
		return metal;
	}

	private void getTypeIds(final Context context) {
		if (useCache) {
			if (loanMLInsType == -1) {
				loanMLInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
						LOAN_ML_INS_TYPE);
			}
			if (depoMLInsType == -1) {
				depoMLInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
						DEPO_ML_INS_TYPE);
			}
			if (cashInsType == -1) {
				cashInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
						CASH_INSTRUMENT_TYPE);
			}
			if (metalSwapInsType == -1) {
				metalSwapInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
						METAL_SWAP_INS_TYPE);
			}
			if (fxInsType == -1) {
				fxInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
						FX_INS_TYPE);
			}
			if (commPhysInsType == -1) {
				commPhysInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
						COMM_PHYS_INS_TYPE);
			}
			if (cflowTypeTransferCharge == -1) {
				cflowTypeTransferCharge = context.getStaticDataFactory().getId(
						EnumReferenceTable.CflowType, TRANSFER_CHARGE_CASH_FLOW);
			}

		} else { // no cache? always retrieve IDs 
			loanMLInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
					LOAN_ML_INS_TYPE);
			depoMLInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
					DEPO_ML_INS_TYPE);
			cashInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
					CASH_INSTRUMENT_TYPE);
			metalSwapInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
					METAL_SWAP_INS_TYPE);
			fxInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
					FX_INS_TYPE);		
			commPhysInsType = context.getStaticDataFactory().getId(EnumReferenceTable.InsType, 
					COMM_PHYS_INS_TYPE);
			cflowTypeTransferCharge = context.getStaticDataFactory().getId(
					EnumReferenceTable.CflowType, TRANSFER_CHARGE_CASH_FLOW);
		}
	}

	private RetrievalLogic getRetrievalLogic(Transaction tran) {
		int insTypeId = tran.getField(EnumTransactionFieldId.InstrumentType).getValueAsInt();
		EnumInsType insType = EnumInsType.retrieve(insTypeId);
		if (insType == EnumInsType.CashInstrument) {
			int insSubTypeId = tran.getField(EnumTransactionFieldId.InstrumentSubType).getValueAsInt();				
			EnumInsSub insSubType = EnumInsSub.retrieve(insSubTypeId);
			if (insSubType == EnumInsSub.CashTransfer) {
				Field passThrough = tran.getField(EnumTransactionFieldId.PassThroughId);
				if (passThrough.isApplicable()) {
					return RetrievalLogic.CASH_TRANSFER_PASS_THROUGH;
				}
				return RetrievalLogic.CASH_TRANSFER;
			} else if (insSubType == EnumInsSub.CashTransaction) { 
				Field cflowTypeField = tran.getField(EnumTransactionFieldId.CashflowType);
				if (cflowTypeField.isApplicable() && cflowTypeField.isReadable()) {
					int cflowTypeId = cflowTypeField.getValueAsInt();
					if (cflowTypeId == cflowTypeTransferCharge) {
						return RetrievalLogic.CASH_TRANSFER_CHARGES;
					}
				}
				return RetrievalLogic.DEFAULT;				
			} else {
				return RetrievalLogic.DEFAULT;
			}
		} else {
			return RetrievalLogic.DEFAULT;
		} 
	}

	private String getLocationSqlDefaultLogic(final int intLe, final int extLe, 
			final String buySell, final int idLbma, final int idLppm, 
			final int idJmGroup, final String cashFlowType, 
			final String metal,
			final int idForceVat, final int linkedDealNum, final int legNo,
			final int feeId) {		
		return "SELECT DISTINCT"
				+ "\n   p_ext.party_id AS ext_party_id"
				+ "\n , p_ext.short_name AS ext_short_name"
				+ "\n , p_ext.long_name AS ext_long_name"
				+ "\n , p_ext.int_ext AS ext_int_ext"
				+ "\n , c_ext.name AS ext_name_country"
				+ "\n , c_ext.iso_code AS ext_country_iso"
				+ "\n , gz_ext.name AS ext_le_region"
				+ "\n , p_int.party_id AS int_party_id"
				+ "\n , p_int.short_name AS internal_entity"
				+ "\n , p_int.long_name AS int_long_name"
				+ "\n , c_int.name AS int_name_country"
				+ "\n , c_int.iso_code AS int_country_iso"
				+ "\n , gz_int.name AS int_geographic_zone" 	
				+ "\n , '" + buySell + "' AS buy_sell"
				+ "\n , '" + cashFlowType + "' AS cash_flow_type "
				+ "\n , '" + metal + "' AS metal" 
				+ "\n , " + legNo + " AS leg_no"
				+ "\n , " + feeId + " AS fee_no"
				+ "\n , ISNULL(pi_lbma.value, '" + defaultLBMAMember + "') AS lbma"
				+ "\n , ISNULL(pi_lppm.value, '" + defaultLPPMMember + "') AS lppm"
				+ "\n , ISNULL(pi_jmgroup.value, '" + defaultJMGroup + "') AS int_jmgroup"
				+ "\n , ISNULL(pi_jmgroup_to.value, '" + defaultJMGroup + "') AS to_bu_internal"
				+ "\n , ISNULL(ti_force_vat.value, '" + defaultForceVat + "') AS force_vat"
				+ "\n FROM "
				+ "\n   party p_ext"
				+ "\n   INNER JOIN party_address pa_ext"
				+ "\n     ON pa_ext.party_id = p_ext.party_id"
				+ "\n 	  AND pa_ext.default_flag = 1"
				+ "\n   INNER JOIN country c_ext"
				+ "\n     ON c_ext.id_number = pa_ext.country"
				+ "\n   INNER JOIN geographic_zone gz_ext"
				+ "\n     ON c_ext.geographic_zone = gz_ext.id_number"
				+ "\n   INNER JOIN party p_int"
				+ "\n     ON p_int.party_id = " + intLe
				+ "\n   INNER JOIN party_address pa_int"
				+ "\n     ON pa_int.party_id = p_int.party_id"
				+ "\n 	  AND pa_int.default_flag = 1"
				+ "\n   INNER JOIN country c_int"
				+ "\n     ON c_int.id_number = pa_int.country"
				+ "\n   INNER JOIN geographic_zone gz_int"
				+ "\n     ON c_int.geographic_zone = gz_int.id_number"
				+ "\n   LEFT OUTER JOIN party_info pi_lbma"
				+ "\n     ON pi_lbma.party_id = " + extLe
				+ "\n     AND pi_lbma.type_id = " + idLbma
				+ "\n   LEFT OUTER JOIN party_info pi_lppm"
				+ "\n     ON pi_lppm.party_id = " + extLe
				+ "\n     AND pi_lppm.type_id = " + idLppm
				+ "\n   LEFT OUTER JOIN party_info pi_jmgroup"
				+ "\n     ON pi_jmgroup.party_id = " + intLe
				+ "\n     AND pi_jmgroup.type_id = " + idJmGroup			
				+ "\n   LEFT OUTER JOIN party_info pi_jmgroup_to"
				+ "\n     ON pi_jmgroup_to.party_id = " + extLe
				+ "\n     AND pi_jmgroup_to.type_id = " + idJmGroup
				+ "\n   LEFT OUTER JOIN ab_tran strat" 
				+ "\n     ON strat.deal_tracking_num = " + linkedDealNum
				+ "\n     AND strat.current_flag = 1"
				+ "\n   LEFT OUTER JOIN ab_tran_info ti_force_vat"
				+ "\n     ON ti_force_vat.tran_num = strat.tran_num"
				+ "\n     AND ti_force_vat.type_id = " + idForceVat
				+ "\n 	WHERE"
				+ "\n 	  p_ext.party_id =" + extLe 
				;
	}
	
	private String getLocationSqlTransferLogic(
			final String buySell, final int idLbma, final int idLppm, 
			final int idJmGroup, final String cashFlowType, 
			final String metal, 
			final int idToAcBU, final int idFromAcBU,
			final int idForceVat, final int linkedDealNum, final int legNo,
			final int feeId) {		
		return "SELECT DISTINCT"
				+ "\n   c_int.iso_code AS int_country_iso"
				+ "\n , '" + buySell + "' AS buy_sell"
				+ "\n , '" + cashFlowType + "' AS cash_flow_type "
				+ "\n , '" + metal + "' AS metal" 
				+ "\n , " + legNo + " AS leg_no"
				+ "\n , " + feeId + " AS fee_no"
				+ "\n,  '' AS lppm"
				+ "\n,  '' AS lbma"
				+ "\n , ISNULL(pi_jmgroup.value, '" + defaultJMGroup + "') AS int_jmgroup"
				+ "\n , ISNULL(pi_jmgroup_to.value, '" + defaultJMGroup + "') AS to_bu_internal"
				+ "\n , ISNULL(ti_to_account_bu.value, '" + defaultToAcBu + "') AS to_account_bu"
				+ "\n , ISNULL(ti_from_account_bu.value, '" + defaultFromAcBu + "') AS from_account_bu"
				+ "\n , ISNULL(ti_force_vat.value, '" + defaultForceVat + "') AS force_vat"
				+ "\n , '' AS from_account_country"
				+ "\n , '' AS to_account_country"
				+ "\n,  '' AS to_account_region"
				+ "\n FROM "
				+ "\n   ab_tran strat" 
				+ "\n   INNER JOIN party p_int"
				+ "\n     ON p_int.party_id = strat.internal_bunit"
				+ "\n   INNER JOIN party_address pa_int"
				+ "\n     ON pa_int.party_id = p_int.party_id"
				+ "\n 	  AND pa_int.default_flag = 1"
				+ "\n   INNER JOIN country c_int"
				+ "\n     ON c_int.id_number = pa_int.country"
				+ "\n   LEFT OUTER JOIN ab_tran_info ti_to_account_bu"
				+ "\n     ON ti_to_account_bu.tran_num = strat.tran_num"
				+ "\n     AND ti_to_account_bu.type_id = " + idToAcBU
				+ "\n   LEFT OUTER JOIN ab_tran_info ti_from_account_bu"
				+ "\n     ON ti_from_account_bu.tran_num = strat.tran_num"
				+ "\n     AND ti_from_account_bu.type_id = " + idFromAcBU
				+ "\n	LEFT OUTER JOIN party p_to "
				+ "\n	  ON p_to.short_name = ti_to_account_bu.value"
				+ "\n	LEFT OUTER JOIN party p_from "
				+ "\n	  ON p_from.short_name = ti_from_account_bu.value"
				+ "\n   LEFT OUTER JOIN party_relationship pr_to"
				+ "\n	  ON pr_to.business_unit_id = p_to.party_id"
				+ "\n   LEFT OUTER JOIN party_relationship pr_from"
				+ "\n	  ON pr_from.business_unit_id = p_from.party_id"
				+ "\n   LEFT OUTER JOIN ab_tran_info ti_force_vat"
				+ "\n     ON ti_force_vat.tran_num = strat.tran_num"
				+ "\n     AND ti_force_vat.type_id = " + idForceVat
				+ "\n   LEFT OUTER JOIN party_info pi_jmgroup"
				+ "\n     ON pi_jmgroup.party_id = pr_from.legal_entity_id" //+ intLe
				+ "\n     AND pi_jmgroup.type_id = " + idJmGroup			
				+ "\n   LEFT OUTER JOIN party_info pi_jmgroup_to"
				+ "\n     ON pi_jmgroup_to.party_id = pr_to.legal_entity_id" //+ extLe
				+ "\n     AND pi_jmgroup_to.type_id = " + idJmGroup
				+ "\n 	WHERE"
				+ "\n     strat.deal_tracking_num = " + linkedDealNum
				+ "\n     AND strat.current_flag = 1"
				;
	}



	private final void initPluginLog() {
		String logLevel = "Error"; 
		String logFile  = getClass().getSimpleName() + ".log"; 
		String logDir   = null;
		String useCache = "No";
		String ruleSelectionLogic = "BLOCK";

		try
		{
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile  = constRep.getStringValue("logFile", logFile);
			logDir   = constRep.getStringValue("logDir", logDir);
			useCache = constRep.getStringValue("useCache", "No");
			ruleSelectionLogic = constRep.getStringValue("ruleSelectionLogic", "BLOCK");
			this.ruleSelectionLogic = RuleSelectionLogic.valueOf(ruleSelectionLogic);
			defaultJMGroup = constRep.getStringValue("Default_JM_Group", "No");
			defaultLBMAMember = constRep.getStringValue("Default_LBMA", "No");
			defaultLPPMMember = constRep.getStringValue("Default_LPPM", "No");
			defaultFromAccount = constRep.getStringValue("Default_From_Account", "");
			defaultToAccount = constRep.getStringValue("Default_To_Account", "<no default>");
			defaultToAcBu = constRep.getStringValue("Default_To_Account_BU", "<no default>");
			defaultFromAcBu = constRep.getStringValue("Default_From_Account_BU", "<no default>");
			defaultForceVat = constRep.getStringValue("Default_Force_Vat", "No");
			defaultMetal = constRep.getStringValue("Default_Metal", "<no default>");
			String csvCPForceVATList = constRep.getStringValue("cpForceVAT", "JM SOUTH AFRICA ECT - BU, JM ROY ECT (RUSSIA) - BU");
			cpForceVAT = new ArrayList<>();
			if (csvCPForceVATList != null) {
				for (String counterparty : csvCPForceVATList.split(",")) {
					cpForceVAT.add(counterparty.trim());
				}
			} 

			if (logDir == null)
				PluginLog.init(logLevel);
			else
				PluginLog.init(logLevel, logDir, logFile);
		}
		catch (Exception e)
		{
			// do something
		}

		try
		{
			viewTables = logLevel.equalsIgnoreCase(PluginLog.LogLevel.DEBUG) && 
					constRep.getStringValue("viewTablesInDebugMode", "no").equalsIgnoreCase("yes");
			this.useCache = "yes".equalsIgnoreCase(useCache);
		}
		catch (Exception e)
		{
			// do something
		}
	}
}
