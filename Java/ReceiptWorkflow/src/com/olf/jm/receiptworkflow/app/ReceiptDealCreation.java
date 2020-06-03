package com.olf.jm.receiptworkflow.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.jm.receiptworkflow.model.FieldValidationException;
import com.olf.jm.receiptworkflow.model.RelNomField;
import com.olf.jm.receiptworkflow.persistence.BatchUtil;
import com.olf.jm.receiptworkflow.persistence.DBHelper;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Legs;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-10-14	V1.0	jwaechter	- initial version
 * 2015-11-02	V1.1	jwaechter	- added setting of Form-Phys on physical leg
 * 2016-02-17	V1.2	jwaechter	- batch.save instead of batch.saveIncremental
 * 2016-02-18	V1.3	jwaechter   - added block in case of multiple locations found on noms to process.
 * 2016-03-14	V1.4	jwaechter	- fixed defect in logic to remove leg
 */

/**
 * This plugin implements a nomination booking OPS for batches.
 * It creates and links the receipt deals for batches that do not have one.
 * Receipt deals are COMM-PHYS deals with possibly multiple legs (per metal).
 * 
 * 
 * @author jwaechter
 * @version 1.4
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class ReceiptDealCreation extends AbstractNominationProcessListener {
	private static final String LEG_INFO_FIELD_FORM_PHYS = "Form-Phys";

	@Override
	public PreProcessResult preProcess(final Context context, final Nominations nominations,
			final Nominations originalNominations, final Transactions transactions,
			final Table clientData) {
		try {
			if (BatchUtil.isSafeUser(context.getUser()) ) {
				return PreProcessResult.succeeded(false);
			}
			Set<Nomination> nomsThatShouldBeProcessed = new HashSet<>();
			AutomaticDealCreationValidation validator = new AutomaticDealCreationValidation();
			init(context);
			for (Nomination nom : nominations) {
				String activity = RelNomField.ACTIVITY_ID.guardedGetString(nom);
				if (!activity.equals("Warehouse Receipt")) {
					Logging.info ("Skipping nomination " + nom.getId() + " as " + 
							RelNomField.ACTIVITY_ID.getName(nom) + " is not 'Warehouse Receipt'");
					continue;
				}
				if (nom instanceof Batch) {
					Batch batch = (Batch) nom;
					validator.process(batch);
					if (RelNomField.RECEIPT_DEAL.guardedGetInt(batch) == 6) {
						nomsThatShouldBeProcessed.add(batch);	
					}
				}
			}
			Set<String> distinctLocations = getDistinctLocations(context, nominations);
			return decidePreProcessResult(nomsThatShouldBeProcessed, distinctLocations);
		} catch (FieldValidationException ex) {
			Logging.info (ex.getMessageToUser());
			Logging.info ("*************** Pre Process Operation Service run (" + 
					this.getClass().getName() +  " ) has ended blocking nom processing ******************");
			return PreProcessResult.failed(ex.getMessageToUser());
		} catch (Throwable t) {
			Logging.info (t.toString());
			Logging.info ("*************** Pre Process Operation Service run (" + 
					this.getClass().getName() +  " ) has ended with error ******************");
			return PreProcessResult.failed(t.toString());
		}finally{
			Logging.close();
		}
	}

	private PreProcessResult decidePreProcessResult(
			Set<Nomination> nomThatShouldBeProcessed, Set<String> distinctLocations) throws OException {
		if (distinctLocations.size() > 1) {
			String message = "The selected batches contain more than one location." 
					+ " Please restrict the operations on receipt batches to a single location."
				;
			Logging.warn(message);
			return PreProcessResult.failed(message);
		}
		
		if (nomThatShouldBeProcessed.size() > 0) {
			String message = 
					"The following batch(es) does/do not have a receipt deal "
							+	" associated: ";
			boolean first=true;
			for (Nomination batch : nomThatShouldBeProcessed) {
				if (!first) {
					message += ", "; 
				}
				first = false;
				message += RelNomField.BATCH_NUMBER.guardedGetString(batch);					
			}
			message += ".\nWould you like a receipt deal to be generated upon saving?";
			message += "\nSelecting No will save changes without generating a receipt.";

			int userSelection = Ask.yesNoCancel(message);
			if (userSelection == 1) {
				Logging.info ("*************** Pre Process Operation Service run (" + 
						this.getClass().getName() +  " ) has ended successfully ******************");
				return PreProcessResult.succeeded(true);
			} else if (userSelection == 0) {
				return PreProcessResult.failed("Batch Processing cancelled by user",
						false);
			} else if (userSelection == 2) {
				for (Nomination batch : nomThatShouldBeProcessed) {
					RelNomField.COUNTERPARTY.guardedSet(batch, ""); // clear selected counterparty in case user 
				}
				Logging.info ("*************** Pre Process Operation Service run (" + 
						this.getClass().getName() +  " ) has ended successfully ******************");
				return PreProcessResult.succeeded(false);
			}
			throw new RuntimeException ("Unknown user selection");				
		} else {
			Logging.info ("*************** Pre Process Operation Service run (" + 
					this.getClass().getName() +  " ) has ended successfully ******************");
			return PreProcessResult.succeeded();				
		}
	}

	@Override
	public void postProcess(final Session session, final Nominations nominations, final Table clientData) {
		try {
			init (session);
			process (session, nominations);	
			Logging.info ("*************** Post Process Operation Service run (" + 
					this.getClass().getName() +  " ) has ended successfully ******************");
		} catch (Throwable t) {
			Logging.info (t.toString());
			Logging.info ("*************** Post Process Operation Service run (" + 
					this.getClass().getName() +  " ) has ended with error ******************");
			throw t;
		}finally{
			Logging.close();
		}
	}

	private void process(final Session session, final Nominations nominations) {
		Map<String, Map<String, Map<String, Set<Batch>>>> nominationsByCounterpartyByMetalByForm = 
				groupNominationsByCounterpartyAndMetal (session, nominations);
		for (String counterparty : nominationsByCounterpartyByMetalByForm.keySet()) {
			Map<String, Map<String, Set<Batch>>> byMetalByForm = nominationsByCounterpartyByMetalByForm.get(counterparty);
			//			 each counterparty = a single new deal
			Transaction template = null;
			Transaction newCommPhysDeal = null;
			Transaction warehouseDeal = null;
			try {
				int warehouseDealNum = retrieveWarehouseDealNum (byMetalByForm);
				Logging.info ("Using warehouse deal #" + warehouseDealNum);
				warehouseDeal = session.getTradingFactory().retrieveTransactionByDeal(warehouseDealNum);
				String location = getLocationFromWarehouseDeal(warehouseDeal);				
				String templateReference = DBHelper.getTemplateForLocation(session, location);
				template =  DBHelper.retrieveTemplateTranByReference(session, templateReference);
				Logging.info ("Retrieved template having reference '" + templateReference + "'");
				newCommPhysDeal = session.getTradingFactory().createTransactionFromTemplate(template);
				// create new receipt (COMM-PHYS) deal
				Logging.info ("Creating new receipt deal");
				setupNewReceiptDeal (session, newCommPhysDeal, warehouseDeal, byMetalByForm, counterparty);
				if (newCommPhysDeal.getTransactionStatus() != EnumTranStatus.Validated) {
					newCommPhysDeal.process(EnumTranStatus.New);
					Logging.info ("New receipt deal #" + newCommPhysDeal.getDealTrackingId() + " has"
							+ " been created and processed to new.");
				}
			int newCommPhysDealTrackingNum = newCommPhysDeal.getDealTrackingId();
			/*** In v17, deal has to be validated first before attaching to batch. ***/
			newCommPhysDeal.dispose();
			newCommPhysDeal = session.getTradingFactory().retrieveTransactionByDeal(newCommPhysDealTrackingNum);
			newCommPhysDeal.process(EnumTranStatus.Validated);
			Logging.info ("New receipt deal #" + newCommPhysDealTrackingNum + " has"
					+ " been created and processed to validated.");
			
			for (String metal : byMetalByForm.keySet()) { // linking newly created deal
				for (String form : byMetalByForm.get(metal).keySet()) {
					for (Batch batch : byMetalByForm.get(metal).get(form) ) {
						batch.assignBatchToPurchase(newCommPhysDeal);
						batch.save();
						Logging.info ("New receipt deal #" + newCommPhysDeal.getDealTrackingId() + " has"
								+ " been assigned to batch #" + batch.getBatchId());
					}
				}
			}
			newCommPhysDeal.dispose();

		} finally {
				if (template != null) {
					template.dispose();
				}
				if (newCommPhysDeal != null) {
					newCommPhysDeal.dispose();
				}
				if (warehouseDeal != null) {
					warehouseDeal.dispose();
				}
			}
		}
	}

	private void setupNewReceiptDeal(final Session session,
			final Transaction newCommPhysDeal, final Transaction warehouseDeal,
			final Map<String, Map<String, Set<Batch>>> byMetalByForm, final String counterparty) {
		String location = getLocationFromWarehouseDeal (warehouseDeal);
		Date batchReceiveDate = getBatchReceiveDate (byMetalByForm);
		Set<Integer> usedParamGroups = new HashSet<>();
		usedParamGroups.add(0);
		setupTranLevelData (session, newCommPhysDeal, warehouseDeal, batchReceiveDate, byMetalByForm.keySet(), counterparty,
				location);

		for (String metal : byMetalByForm.keySet()) {
			for (String form : byMetalByForm.get(metal).keySet()) {
				addLegAndFillWithData (session, newCommPhysDeal, metal, form, byMetalByForm.get(metal).get(form), location, usedParamGroups);
			}
		}

		Legs legs = newCommPhysDeal.getLegs();	
		boolean deleted=false;
		
		do {
			deleted = false;
			for (Leg leg : legs) {
				int paramGroup = leg.getValueAsInt(EnumLegFieldId.ParamGroup);
				if (!usedParamGroups.contains(paramGroup)) {
					deleted = true;
					legs.remove(leg);
					break;
				}
			}
		} while (deleted);
	}

	private void setupTranLevelData(final Session session,
			final Transaction newCommPhysDeal, final Transaction warehouseDeal, final Date batchReceiveDate,
			final Set<String> metals, final String counterparty, final String location) {
		String reference = createReference (batchReceiveDate, metals);
		newCommPhysDeal.setValue(EnumTransactionFieldId.ReferenceString, reference);

		String intBu = warehouseDeal.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit);
		newCommPhysDeal.setValue(EnumTransactionFieldId.InternalBusinessUnit, intBu);

		String intLE = warehouseDeal.getValueAsString(EnumTransactionFieldId.InternalLegalEntity);
		newCommPhysDeal.setValue(EnumTransactionFieldId.InternalLegalEntity, intLE);

		String portfolio = warehouseDeal.getValueAsString(EnumTransactionFieldId.InternalPortfolio);
		newCommPhysDeal.setValue(EnumTransactionFieldId.InternalPortfolio, portfolio);

		String allocated = warehouseDeal.getField("Allocation Type").getValueAsString();
		newCommPhysDeal.getField("Allocation Type").setValue(allocated);

		String extLE = DBHelper.getLEOfBu (session, counterparty);
		newCommPhysDeal.setValue(EnumTransactionFieldId.ExternalLegalEntity, extLE);
		newCommPhysDeal.setValue(EnumTransactionFieldId.ExternalBusinessUnit, counterparty);

		String mappedLocation = DBHelper.mapCommStorLocoToCommPhysLoco (session, location);
		newCommPhysDeal.getField("Loco").setValue(mappedLocation);
	}

	private void addLegAndFillWithData(final Session session,
			final Transaction newCommPhysDeal, final String metal, String form, final Set<Batch> byMetal, final String location, Set<Integer> usedParamGroups) {
		Leg finLeg = retrieveMatchingLeg (newCommPhysDeal, metal, form, usedParamGroups);
		int legGroup = finLeg.getValueAsInt(EnumLegFieldId.ParamGroup);
		String holidayschedule;
		
		int intBu = newCommPhysDeal.getValueAsInt(EnumTransactionFieldId.InternalBusinessUnit);
		if(intBu==20007)	//JM PMM HK
		{
			holidayschedule="HKD";
		}
		else{
			holidayschedule="USD";
		}
		finLeg.setValue(EnumLegFieldId.StartDate, getBatchReceiveDate(byMetal));
		finLeg.setValue(EnumLegFieldId.MaturityDate, getBatchReceiveDate(byMetal));
		finLeg.setValue(EnumLegFieldId.CommoditySubGroup, metal);
		finLeg.setValue(EnumLegFieldId.HolidaySchedule, holidayschedule);
		String formPhys = DBHelper.mapBatchFormToTransactionForm(session, form);
		Field fieldFormPhys = finLeg.getField(LEG_INFO_FIELD_FORM_PHYS);
		if (fieldFormPhys.isApplicable() && !fieldFormPhys.isReadOnly())
			fieldFormPhys.setValue(formPhys);
		for (Leg otherLeg : newCommPhysDeal.getLegs()) {
			if (otherLeg.getValueAsInt(EnumLegFieldId.ParamGroup) == legGroup) {
				fieldFormPhys = otherLeg.getField(LEG_INFO_FIELD_FORM_PHYS);
				if (fieldFormPhys.isApplicable() && !fieldFormPhys.isReadOnly())
					fieldFormPhys.setValue(formPhys);
				Field fieldLocation = otherLeg.getField(EnumLegFieldId.Location);
				if (fieldLocation.isApplicable() && !fieldLocation.isReadOnly()) {
					fieldLocation.setValue(location);
				}
			}
		}
		finLeg.setValue(EnumLegFieldId.CommodityForm, form);
		String purity = getPurity (byMetal);
		finLeg.setValue(EnumLegFieldId.MeasureGroup, purity);
		finLeg.setValue(EnumLegFieldId.CommodityBrand, getBrand(byMetal));
		double sumOfApplicableVolume = getSumOfApplicableVolume (byMetal);
		finLeg.setValue(EnumLegFieldId.DailyVolume, sumOfApplicableVolume);
		//		String product = getProduct (byMetal);
		//		leg.setValue(EnumLegFieldId.Product, product);		
	}

	private String getBrand(Set<Batch> byMetal) {
		for (Batch batch : byMetal) {
			return RelNomField.BRAND.guardedGetString(batch);
		}
		return "";
	}

	private String getForm(Set<Batch> byMetal) {
		for (Batch batch : byMetal) {
			return RelNomField.FORM.guardedGetString(batch);
		}
		return "";
	}

	private String getPurity(Set<Batch> byMetal) {
		for (Batch batch : byMetal) {
			return RelNomField.PURITY.guardedGetString(batch);
		}
		return "";
	}

	private double getSumOfApplicableVolume(Set<Batch> byMetal) {
		double sumOfApplicableVolume = 0.0d;
		for (Batch batch : byMetal) {
			sumOfApplicableVolume += RelNomField.VOLUME_FIELD.guardedGetDouble(batch);
		}
		return sumOfApplicableVolume;
	}

	private String getProduct(Set<Batch> byMetal) {
		for (Batch batch : byMetal) {
			return RelNomField.PRODUCT.guardedGetString(batch);
		}
		return "";
	}

	private Date getBatchReceiveDate(final Map<String, Map<String, Set<Batch>>> byMetalByForm) {
		for (String metal : byMetalByForm.keySet()) {
			for (String form: byMetalByForm.get(metal).keySet()) {
				return getBatchReceiveDate (byMetalByForm.get(metal).get(form));
			}
		}
		return new Date();
	}

	private Date getBatchReceiveDate(final Set<Batch> byMetal) {
		for (Batch batch : byMetal) {
			return RelNomField.RECEIPT_DATE.guardedGetDate(batch);
		}
		return new Date();
	}

	private Leg retrieveMatchingLeg(Transaction newCommPhysDeal,
			String metal, String form, Set<Integer> usedParamGroups) {
		for (Leg leg : newCommPhysDeal.getLegs()) {
			boolean metalFine=false;
			boolean formFine=false;
			boolean isNone = false;
			
			int paramGroup = leg.getValueAsInt(EnumLegFieldId.ParamGroup);
			
			if (usedParamGroups.contains(paramGroup)) {
				continue;
			}

			Field commSubGroupLeg = leg.getField(EnumLegFieldId.CommoditySubGroup);
			if (commSubGroupLeg == null || !commSubGroupLeg.isApplicable() || !commSubGroupLeg.isReadable() ||
					!commSubGroupLeg.isWritable()) {
				continue;
			}
			String legMetal = commSubGroupLeg.getValueAsString();
			if (legMetal.trim().equals("") || legMetal.equalsIgnoreCase("None")) {
				isNone = true;
			}			
			if (legMetal.equals(metal)) {
				metalFine=true;
			}
			
			Field formLeg = leg.getField(LEG_INFO_FIELD_FORM_PHYS);
			if (commSubGroupLeg == null || !commSubGroupLeg.isApplicable() || !commSubGroupLeg.isReadable() ||
					!commSubGroupLeg.isWritable()) {
				continue;
			}
			String legForm = commSubGroupLeg.getValueAsString();
			if (legMetal.trim().equals("") || legMetal.equalsIgnoreCase("None")) {
				if (isNone) {
					usedParamGroups.add(paramGroup);					
					return leg;
				}
			}			
			if (legMetal.equals(metal)) {
				metalFine=true;
			}
			if (metalFine && formFine) {
				usedParamGroups.add(paramGroup);
				return leg;
			}
		}
		throw new RuntimeException ("Template does not have enough predefined sides. Please ensure there are enough legs on the template.");
	}


	private String getLocationFromWarehouseDeal(Transaction warehouseDeal) {
		Leg leg = warehouseDeal.getLeg(1);		
		return leg.getValueAsString(EnumLegFieldId.Location);
	}

	private String createReference(Date batchReceiveDate, Set<String> metals) {
		SimpleDateFormat formatter = new SimpleDateFormat(ConfigurationItem.REFERENCE_DATE_SYNTAX.getValue());
		StringBuilder metalList = new StringBuilder();
		for (String metal : metals) {
			metalList.append("-").append(metal);
		}
		return formatter.format(batchReceiveDate) + metalList;
	}

	private int retrieveWarehouseDealNum(Map<String, Map<String, Set<Batch>>> byMetalByForm) {
		for (String metal : byMetalByForm.keySet()) {
			for (String form : byMetalByForm.get(metal).keySet()) {
				for (Batch batch: byMetalByForm.get(metal).get(form)) {
					int warehouseDealNum =  RelNomField.WAREHOUSE_DEAL.guardedGetInt(batch); 
					if (warehouseDealNum == 0) {
						throw new RuntimeException ("Error retrieving the  warehouse deal number from batch "
								+ batch.getBatchId());
					}
					return warehouseDealNum;
				}				
			}
		}
		return 0;
	}

	private Set<String> getDistinctLocations(Session session, Nominations nominations) {
	Set<String> distinctLocations = new HashSet<String>();
		for (Nomination nom : nominations) {
			if (isRelevantForProcessing(nom)) {
				String counterparty = RelNomField.WAREHOUSE_LOCATION.guardedGetString(nom);
				distinctLocations.add(counterparty);
			}
		}	
		return distinctLocations;
	}

	private Map<String, Map<String, Map<String, Set<Batch>>>> groupNominationsByCounterpartyAndMetal(
			Session context, Nominations nominations) {
		Map<String, Map<String, Map<String, Set<Batch>>>> groupedNoms = 
				new HashMap<>();
				for (Nomination nom : nominations) {
					if (isRelevantForProcessing(nom)) {
						String counterparty = RelNomField.COUNTERPARTY.guardedGetString(nom);
						String metal = RelNomField.PRODUCT.guardedGetString(nom);
						String form = RelNomField.FORM.guardedGetString(nom);
						if (failsContentCheck(counterparty, metal)) {
							continue;
						}
						Set<Batch> batchesForMetalForForm = getBatchSet(groupedNoms, counterparty, metal, form);
						batchesForMetalForForm.add((Batch)nom);
					}
				}
				return groupedNoms;
	}

	private boolean failsContentCheck(String counterparty, String metal) {
		return counterparty == null || metal == null || counterparty.trim().equals("") || metal.trim().equals("");
	}

	private Set<Batch> getBatchSet(
			Map<String, Map<String, Map<String, Set<Batch>>>> groupedNoms,
			String counterparty, String metal,
			String form) {
		Map<String, Map<String, Set<Batch>>> byCounterparty;
		byCounterparty = getByCounterparty(groupedNoms, counterparty);
		Map<String, Set<Batch>> byCounterPartyByMetal;
		byCounterPartyByMetal = getByCounterPartyByMetal (byCounterparty, metal);
		Set<Batch> byCounterPartyByMetalByForm = getByCounterPartyByMetalByForm (byCounterPartyByMetal, form);

		return byCounterPartyByMetalByForm;
	}

	private Set<Batch> getByCounterPartyByMetalByForm(
			Map<String, Set<Batch>> byCounterPartyByMetal, String form) {
		Set<Batch> byCounterpartyByMetalByForm;
		if (byCounterPartyByMetal.containsKey(form)) {
			byCounterpartyByMetalByForm = byCounterPartyByMetal.get(form);
		} else {
			byCounterpartyByMetalByForm = new HashSet<>();
			byCounterPartyByMetal.put(form, byCounterpartyByMetalByForm);
		}
		return byCounterpartyByMetalByForm;		
	}

	private Map<String, Set<Batch>> getByCounterPartyByMetal(
			Map<String, Map<String, Set<Batch>>> byCounterparty, String metal) {
		Map<String, Set<Batch>> byCounterpartyByMetal;
		if (byCounterparty.containsKey(metal)) {
			byCounterpartyByMetal = byCounterparty.get(metal);
		} else {
			byCounterpartyByMetal = new HashMap<>();
			byCounterparty.put(metal, byCounterpartyByMetal);
		}
		return byCounterpartyByMetal;
	}

	private Map<String, Map<String, Set<Batch>>> getByCounterparty(
			Map<String, Map<String, Map<String, Set<Batch>>>> groupedNoms,
			String counterparty) {
		Map<String, Map<String, Set<Batch>>> byCounterparty;
		if (groupedNoms.containsKey(counterparty)) {
			byCounterparty = groupedNoms.get(counterparty);
		} else {
			byCounterparty = new HashMap<>();
			groupedNoms.put(counterparty, byCounterparty);
		}
		return byCounterparty;
	}

	private boolean isRelevantForProcessing(Nomination nom) {
		if (!(nom instanceof Batch)) {
			return false;
		}
		Batch batch = (Batch) nom;
		if (!batch.isOriginBatch()) {
			return false;
		}

		if (RelNomField.WAREHOUSE_DEAL.guardedGetInt(batch) == 6) {
			return false;
		}

		if (RelNomField.RECEIPT_DEAL.guardedGetInt(nom) != 6) {
			return false;
		}
		return true;
	}


	private void init (Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		String logLevel;
		try {
			logLevel = ConfigurationItem.LOG_LEVEL.getValue();
			String logFile = ConfigurationItem.LOG_FILE.getValue();;
			//String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
			String logDir = abOutdir + "\\error_logs";
			
			Logging.init(this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);
			Logging.info ("*************** Operation Service run (" + 
					this.getClass().getName() +  " ) started ******************");
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}