package com.matthey.openlink.bo.opsvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.matthey.openlink.utilities.DataAccess;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

/**
 * D422 PreProcess dispatch transaction to populate InfoFields
 * <br>If target status is New then perform population of following fields
 * <ul><li>{@value #EnumLegFieldId.Location} set the TranInfo {@code Loco}
 * <li>{@value #EnumLegFieldId.CommodityForm} set the ParamInfo {@code Form-Phys} on each leg
 * </ul>
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.TradeInput })
public class DispatchInfoStamping  extends AbstractTradeProcessListener {
	
	
	static private List<EnumTranStatus> permissableStatus = Arrays.asList(new EnumTranStatus[] { 
			EnumTranStatus.New});
	
	@Override
	public PreProcessResult preProcess(Context context,
			EnumTranStatus targetStatus,
			PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {

		try {
		Logging.init(context, this.getClass(), "", "");
		//if (permissableStatus.contains(targetStatus)) 
		  for (PreProcessingInfo<?> activeItem : infoArray) {

	            Transaction transaction = null;
	            try {
	                transaction = activeItem.getTransaction();

	                PreProcessResult result;
	                if (!(result = handleTransactionStamping(context, transaction)).isSuccessful())
	                    return result;


	            } catch (Exception e) {
	                String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s",
	                        null != transaction ? transaction.getTransactionId() : ValidateDispatchInstructions.DISPATCH_BOOKING_ERROR,
	                        this.getClass().getSimpleName(), e.getLocalizedMessage());
				Logging.error(reason, e);
	                e.printStackTrace();
	                return PreProcessResult.failed(reason);

	            }
		  }
		  return PreProcessResult.succeeded();
		} catch (Exception e) {
			Logging.error(e.getMessage(), e);
			return PreProcessResult.failed(e.getLocalizedMessage());
		} finally {
		Logging.close();
		}
		
	}


	private static final String COMM_PHYS_MAPPING = "USER_jm_form_map";
	private static final String COMM_STOR_MAPPING = "USER_jm_loco_map";
	
	private static final Map<EnumLegFieldId, String> CommPhysMapper = new HashMap<>(0); 
		static {
			CommPhysMapper.put(EnumLegFieldId.Location, "Loco");
			CommPhysMapper.put(EnumLegFieldId.CommodityForm, "Form-Phys");
			
	 }
	private static final Map<String, String> commPhysFormMapping = new HashMap<>(0);
	private static final Map<String, String> commPhysLocoMapping = new HashMap<>(0);
	
	
	PreProcessResult handleTransactionStamping(Context context,
			Transaction transaction) {
/*FPA 10-Dec request trigger for ALL
		if (!EnumBuySell.Sell.toString().equalsIgnoreCase(transaction.getField(EnumTransactionFieldId.BuySell).getDisplayString()))
			return PreProcessResult.succeeded();
*/		
		refreshMapping(context);
		for (Entry<EnumLegFieldId, String> mappedField : CommPhysMapper
				.entrySet()) {
			if (mappedField.getKey() == EnumLegFieldId.Location) {
				Field infoField = transaction.getField(mappedField.getValue());

				Field tranField = transaction.getLeg(1).getField(
						mappedField.getKey());
				if (infoField != null && tranField != null) {
					String value = tranField.getDisplayString();
					infoField.setValue(commPhysLocoMapping.containsKey(value) ? commPhysLocoMapping
									.get(value) : commPhysLocoMapping
									.get("None"));
				}
			} else if (transaction.getLegCount() > 1) {
				Field tranField = null;
				String value = null;
				for (Leg leg : transaction.getLegs()) {
					if (leg.isPhysicalCommodity()) {
						tranField = leg.getField(mappedField.getKey());
						value = tranField.getDisplayString();
						if (!commPhysFormMapping.containsKey(value))
							value = "None";
					}
					leg.getField(mappedField.getValue()).setValue(
							commPhysFormMapping.get(value));
				}
			}
		}

		return PreProcessResult.succeeded();
	}


	/**
	 * update local collections of info field maps 
	 */
	private void refreshMapping(Session session) {
		
		refreshMap( commPhysFormMapping, getMappingReference(session, COMM_PHYS_MAPPING,"src_batch_form","dst_receipt_form"));
	
		refreshMap( commPhysLocoMapping, getMappingReference(session, COMM_STOR_MAPPING,"src_comm_stor_location","dst_loco_info"));
	}
	
	/**
	 * update the results from the db into the supplied collection 
	 */
	private void refreshMap(Map<String, String> map, Table mappingReference) {
		
		map.clear();
		if (mappingReference == null || mappingReference.getRowCount()<1) {
			return;
		}
		
		for(int row=0; row<mappingReference.getRowCount(); row++) {
			map.put(mappingReference.getString("source", row), mappingReference.getString("target", row));
		}
	}

	/**
	 * recovery mapping data from the database 
	 */
	private Table getMappingReference(Session session, final String tableName, final String sourceKey, final String targetValue) {
		
		Table configurationData = DataAccess.getDataFromTable(session, String.format(
				"SELECT %s source, %s target" + 
				"\nFROM %s "  
				, sourceKey, targetValue, tableName));
		
		if (null == configurationData || configurationData.getRowCount() < 1) {
			throw new IllegalArgumentException(String.format(
					"Invalid or missing configuration data(%s)", tableName));
		}
		return configurationData;
	}


}