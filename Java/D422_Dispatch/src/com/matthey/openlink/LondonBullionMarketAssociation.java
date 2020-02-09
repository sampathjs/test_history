package com.matthey.openlink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.matthey.openlink.bo.opsvc.DispatchCollateral;
import com.matthey.openlink.bo.opsvc.DispatchCollateralException;
import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.Context;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

/**
 * London Good Delivery (D137)
 * <br> 
 * Delivery Ticket InfoField  {@value #DISPATCH_INFO}
 * is obtained from a sequence table to ensure integrity and security of the values uniqueness
 * <br>Called by OpSvc {@link DispatchSettlementInstructions.getLGD}
 * <br>Called from TPM {@link com.matthey.openlink.utilites.tpm.RetrieveLGD}
 * <br>
 * <br>
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
 *	<td><font color="blue"><b>{@value #BRAND}</b></font></td>
 *	<td>{@value #JM_BRAND}</td>
 *	<td>The brand name, which can be in the Batch or assigned at the transaction level
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #DELIVERY_INFO}</b></td>
 *	<td>{@value #DISPATCH_INFO} </td>
 *	<td><i>InfoField</i> to be populated with the LGD value</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #ENTITY}</b></td>
 *	<td>{@value #JM_ENTITY}</td>
 *	<td>The transaction LE is checked against this value for a match
 *  </td>
 *	</tr>
 *  <td><b>{@value #DELIVERY_SEQUENCE}</b>
 *  </td>
 *  <td>{@value #LGD_SEQUENCE}
 *  </td>
 *  <td>which is the sequence object in the database
 *  </td>
 *	</tbody>
 *	</table>
 * </p>
 */
public class LondonBullionMarketAssociation {
	

	private static final String CONST_REPO_CONTEXT = "JM_LGD";
	private static final String CONST_REPO_SUBCONTEXT = "Dispatch";
	private static final String BRAND = "Brand";
	private static final String ENTITY = "Legal Entity";
	public static final String DELIVERY_INFO = "Dispatch";
	private static final String JM_BRAND = "Johnson Matthey UK";
	private static final String JM_ENTITY = "JM PLC";
	private static final String DISPATCH_INFO = "LGD Number";
	
	private static final String DELIVERY_SEQUENCE = "Sequence";
	private static final String LGD_SEQUENCE = "JM_lgd";
	
	private static final Map<String, String> configuration;
	    static
	    {
	    	configuration = new HashMap<String, String>();
	    	configuration.put(BRAND,JM_BRAND);
	    	configuration.put(ENTITY, JM_ENTITY);
	    	configuration.put(DELIVERY_INFO, DISPATCH_INFO);
	    	configuration.put(DELIVERY_SEQUENCE, LGD_SEQUENCE);
	    }
		public static Properties properties;
		
		
	private Context context;
		
/**
 * Called via Plugin
 */
	public static LGD getLGD(Context context, Transaction transaction) {

		LondonBullionMarketAssociation lgd = new LondonBullionMarketAssociation(context);
		if (!lgd.isLGDRequired(transaction))
			return new LGD(0);

		return new LGD(lgd.getLGD(context));
	}

/**
 * Called via OpSvc
 */
	public static boolean qualifiesForLGD(Context context, Batch batch) {
		 
		LondonBullionMarketAssociation lgd = new LondonBullionMarketAssociation(context);
		return lgd.isLGDRequired(batch);
	}
	
	public static LGD getLGD(Context context, LondonBullionMarketAssociation lgd) {
	 
			return new LGD(lgd.getLGD(context));

	}
	
	
	private LondonBullionMarketAssociation() {
	}
	public LondonBullionMarketAssociation(Context context) {
		this.context=context;
		this.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
	}


	private static Set<String> forms = new HashSet<String>(0);
	private static Set<String> metals = new HashSet<String>(0);
	private static Map<String,String> metalProduct = new HashMap<>(0); 
    
	/**
	 * The dispatch must meet the following criteria
	 * <ul><li><b>Internal LE:</b> {@value #JM_ENTITY}
	 * <li><b>Brand:</b> {@value #JM_BRAND}
	 * <li><b>Form:</b> Ingot, Plate, etc - {@code any valid configured entry within the Commodity Forms}
	 * <li><b>Metal:</b> Platinum, Palladium, {@code e.g. any precious metal configured under currencies}
	 */
	private boolean isLGDRequired(Transaction transaction) {

		if (existingLGDPresent(context, transaction))
			return false;

		boolean criteria = false;
		Logging.info("Checking if InternalLegalEntity is valid");
		if (properties.getProperty(ENTITY).equalsIgnoreCase(
				transaction.getField(EnumTransactionFieldId.InternalLegalEntity)
				.getDisplayString())) {
			
			criteria = isTransactionValid(transaction);
		}
		return criteria;
	}
	
	/**
	 * 
     * 2016-01-06	Vn.x	pwallace	- added LE check to Batch processing
   	 */
	private boolean isLGDRequired(Batch batch) {
		boolean criteria = false;
		Logging.info("Checking if InternalLegalEntity is valid");
		if (properties.getProperty(ENTITY).equalsIgnoreCase(
				batch.getField(EnumNominationFieldId.InternalLegalEntity).getDisplayString())) {			
		  
			criteria = isBatchValid(batch);

		}
		return criteria;
		
	}
	
	
/**
 * Check if batch meet LGD requirements
 * @see #isLGDRequired(Batch)
 */
	private boolean isBatchValid(Batch batch) {

//		DeliveryTickets containers = batch.getBatchContainers();
	  Logging.info("Checking if batch is valid");
		com.olf.openrisk.scheduling.Field brand = batch.getField(EnumNominationFieldId.CommodityBrand);
		com.olf.openrisk.scheduling.Field batchBrand = batch.getField("Brand");
//		com.olf.openrisk.scheduling.Field activityId = batch.retrieveField(EnumNomfField.NomCmotionCsdActivityId, 0);
		if (null!=brand && properties.getProperty(BRAND)
							.equalsIgnoreCase(brand.getDisplayString())) {
			
			if (LondonBullionMarketAssociation.forms.isEmpty())
				LondonBullionMarketAssociation.forms = refreshCommodityForms();
			
			com.olf.openrisk.scheduling.Field form = batch.getField(EnumNominationFieldId.CommodityForm);
			if (null!=form && forms.contains(form.getDisplayString().trim())) {
				
				if (LondonBullionMarketAssociation.metalProduct.isEmpty()) {
					LondonBullionMarketAssociation.metals = refreshPreciousMetals();
					LondonBullionMarketAssociation.metalProduct = refreshMetalProducts();
				}
				
				com.olf.openrisk.scheduling.Field product = batch.getField(EnumNominationFieldId.CategoryId);
				if (null!=product &&
						metals.contains(metalProduct.get(product.getDisplayString().trim()))) {
					Logging.info("MATCH!");
					return true;
				}

			}
		}

		return false;
	}

/**
 * Check if transaction legs meet LGD requirements
 * @see #isLGDRequired(Transaction)
 */
	private boolean isTransactionValid(Transaction transaction) {
		boolean criteria=false;
		Logging.info("Checking if transaction is valid"); 
		for (Leg leg : transaction.getLegs()) {
			StringBuilder dealLegInfo = new StringBuilder(String.format("%s:", BRAND));
			Field brand = leg.getField(EnumLegFieldId.CommodityBrand);
			dealLegInfo.append(brand.getDisplayString());
			if (brand.isApplicable()
					&&  properties.getProperty(BRAND)
							.equalsIgnoreCase(brand.getDisplayString())) {

				if (LondonBullionMarketAssociation.forms.isEmpty())
					LondonBullionMarketAssociation.forms = refreshCommodityForms();

				dealLegInfo.append(String.format(" \t%s:",EnumLegFieldId.CommodityForm.getName()));
				Field form = leg.getField(EnumLegFieldId.CommodityForm);
				dealLegInfo.append(form.getDisplayString());

				if (form.isApplicable()
						&& forms.contains(form.getDisplayString().trim())) {

					if (LondonBullionMarketAssociation.metals.isEmpty())
						LondonBullionMarketAssociation.metals = refreshPreciousMetals();

					dealLegInfo.append(String.format(" \t%s:",EnumLegFieldId.CommoditySubGroup.getName()));
					Field metal = leg.getField(EnumLegFieldId.CommoditySubGroup);
					dealLegInfo.append(metal.getDisplayString());
					if (metal.isApplicable()
							&& metals.contains(metal.getDisplayString().trim())) {
						criteria = true;
						Logging.info(dealLegInfo.toString() + ">LDG MATCH<");
						break;
					}
				}

			}
			Logging.info(dealLegInfo.toString());
		}
		return criteria;
	}

	
	/**
	 * check if the supplied transaction has an existing {@value #DISPATCH_INFO} populated 
	 * return <b>true</b> otherwise <b>false</b>
	 */
	private boolean existingLGDPresent(Session session, Transaction transaction) {
	
		Table results = DataAccess.getDataFromTable(session, String.format("SELECT dti.info_value" + 
				"\nFROM comm_schedule_header csh " + 
				"\nJOIN delivery_ticket_info_types dtit ON dtit.type_name = '%s' " + 
				"\nJOIN delivery_ticket_info dti ON (dti.type_id = dtit.type_id) " + 
				"\nJOIN tsd_delivery_ticket tdt ON (tdt.id_number = dti.delivery_ticket_id AND tdt.schedule_id = csh.schedule_id) " + 
				"\nWHERE csh.ins_num=%d",
				properties.getProperty(DELIVERY_INFO), 
				transaction.getInstrumentId()));
		
		if (null==results || results.getRowCount()<1)
			return false;
		
		return true;
	}

	/**
	 * obtain <i>London Good Delivery</i> value from database, returning this value to caller 
	 */
	private int getLGD(Session session) {
		//SELECT convert(decimal(10,5),current_value) FROM SYS.sequences WHERE name ='JM_lgb'
		Table bullionNumberLookup = DataAccess.getDataFromTable(session, String
				.format("SELECT %s %s",
						"NEXT VALUE FOR ",properties.getProperty(DELIVERY_SEQUENCE)));
		if (null == bullionNumberLookup || bullionNumberLookup.getRowCount() < 1) {
			throw new DispatchCollateralException("Configuration", DispatchCollateral.ERR_CONFIG,
					"LGD sequence missing!");
		}
		int lgb = bullionNumberLookup.getInt(0, 0);
		return lgb;
		
	}
	
	/**
	 * get the configured values for Inventory Management forms
	 * <br>these are provided in reference tables so can be modified by the client
	 */
	private Set<String> refreshCommodityForms() {
		Set<String> forms = new HashSet<>(0);
		
//		Table commodityForms = DataAccess.getDataFromTable(context,"SELECT comm_form_name FROM comm_form");
//		
//		for(TableRow row :commodityForms .getRows()){
//			forms.add(row.getString(0));
//		}
		forms.add("Ingot");
		forms.add("Plate");
		return forms;
	}
	
	/**
	 * obtain the current precious metals as identified by the reference currency configuration
	 * 
     * 2016-01-06	Vn.x	pwallace	- added restrictions collection to limit supported metals
 	 */
	private Set<String> refreshPreciousMetals() {
		Set<String> metalsRestrictions = new HashSet<>(0);
		metalsRestrictions.add("Palladium");
		metalsRestrictions.add("Platinum");
		
		Set<String> preciousMetals = new HashSet<>(0);
		
		StringBuilder filter = new StringBuilder("1");	
		if (!metalsRestrictions.isEmpty()) {
			filter.append(" AND description in (");
			for (String metal : metalsRestrictions) {
				filter.append("'").append(metal).append("',");
			}
			filter.deleteCharAt(filter.length()-1);
			filter.append(")");
		}
		Table preciousMetalCurrencies = DataAccess.getDataFromTable(context,
				String.format("SELECT name from currency WHERE precious_metal=%s", filter));
		for(TableRow row :preciousMetalCurrencies.getRows()){
			preciousMetals.add(row.getString(0));
		}
		
		return preciousMetals;
	}
	
	/**
	 * get lookup from dispatch product to ccy for precious metal 
	 */
	private Map<String, String> refreshMetalProducts() {
		Map<String,String> preciousMetalProducts = new HashMap<>(0);
		Table preciousMetalCurrencies = DataAccess.getDataFromTable(context,
				"SELECT pm.name, pm.code " +
				"\nFROM idx_subgroup pm " + 
				"\nJOIN idx_group pmg ON pm.idx_group=pmg.id_number AND pmg.name='Pre Metal'");
		for(TableRow row :preciousMetalCurrencies.getRows()){
			preciousMetalProducts.put(row.getString("name"),row.getString("code"));
		}
		return preciousMetalProducts;
	}

	
}
