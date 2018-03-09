package com.olf.containermanagement;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.scheduling.AbstractNominationProcessListener;

import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.EnumVolume;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.StaticTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.*;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.scheduling.containermanagement.ContainerManagementUtil;

import java.util.Iterator;
/**
 * Nomination-booking pre-process script that will block if the user has modified a measurement that s/he shouldn't have.  
 * The idea is that the measures that are part of the measure group should not be changed, and they're be read-only from the GUI. 
 * But the user can still modify measures in the GUI with the "-" button, or from script. 
 * So this script needs to get the measure group of of the leg, get the default measures associated with it, and, if the planned or observed measures differ from the default,
 * return true, i.e., read-only.
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking})
public final class TestPreProcessScript extends AbstractNominationProcessListener {
	
	/**
	 * Nomination-booking pre-process script that will block if the user has modified a measurement that s/he shouldn't have.  
	 * The idea is that the measures that are part of the measure group should not be changed, and they're be read-only from the GUI. 
	 * But the user can still modify measures in the GUI with the "-" button, or from script. 
	 * So this script needs to get the measure group of of the leg, get the default measures associated with it, and, if the planned or observed measures differ from the default,
	 * cause the operation to fail by returning PreProcessResult.failed("");.
	 */
	public PreProcessResult preProcess(final Context context, final Nominations nominations,
            final Nominations originalNominations, final Transactions transactions,
            final Table clientData) {
    	@SuppressWarnings("unused")
    	int numNoms = nominations.size();
    	@SuppressWarnings("unused")
		int numTrans = transactions.size();
		try {
			Iterator<Transaction> tranIterator = transactions.iterator();
			boolean isDispatch = this.hasDispatch();
			boolean isCMXfer = this.isContainerizedTransferOperation();
			while (tranIterator.hasNext()) {
				Transaction tran= (Transaction) tranIterator.next();
				
				int baseInsType = tran.getValueAsInt(EnumTransactionFieldId.InstrumentType);
				if (EnumInsType.CommStorage.getValue()!= baseInsType && EnumInsType.CommPhysBatch.getValue() != baseInsType)
					continue;
				
				int searchVolumeType = (EnumInsType.CommStorage.getValue()== baseInsType) ? EnumVolume.Nominated.getValue() :  EnumVolume.Confirmed.getValue();
				Legs legs = tran.getLegs();
				Iterator<Leg> legIterator = legs.iterator();
				while (legIterator.hasNext()) {
					Leg leg = (Leg) legIterator.next();
					if (false == leg.isPhysicalCommodity())
						continue;
					if (EnumInsType.CommStorage.getValue() == baseInsType ){
						String payRec = leg.getValueAsString(EnumLegFieldId.PayOrReceive);
						if (true != payRec.equals("Wth")) {
							//ONLY do withdrawal legs
							continue;
						}
					}
					ScheduleDetails scheduleDetails = leg.getScheduleDetails();
					Iterator<ScheduleDetail> scheduleDetailsIterator = scheduleDetails.iterator();
					while (scheduleDetailsIterator.hasNext()) {
						Nomination nomination = null;
						int measureGroupId;
						ScheduleDetail scheduleDetail = (ScheduleDetail) scheduleDetailsIterator.next();
						int volumeType = scheduleDetail.getValueAsInt(EnumScheduleDetailFieldId.VolumeType);
						if (volumeType != searchVolumeType) 
							continue;
						int deliveryId = scheduleDetail.getValueAsInt(EnumScheduleDetailFieldId.DeliveryId);
						if (baseInsType != EnumInsType.CommPhysBatch.getValue())
						{
							//for storage deal, only process noms that are in nominations; for batch deal, always process
							nomination = findNominationByDeliveryId(nominations, deliveryId);
							if (nomination == null) //nom not found, hence not changed, so move on
								continue;
							measureGroupId = nomination.getValueAsInt(EnumNominationFieldId.MeasureGroupId);
						}
						else {
							measureGroupId = leg.getValueAsInt(EnumLegFieldId.MeasureGroup);
						}
/*						
						StaticTable measureGroupTable = ContainerManagementUtil.getMeasureGroupTable(measureGroupId);
						PlannedMeasures plannedMeasures = scheduleDetail.getPlannedMeasures();
        				checkMeasures(measureGroupTable, plannedMeasures, measureGroupId);
            			DeliveryTickets deliveryTickets = scheduleDetail.getDeliveryTickets();
    					Iterator<DeliveryTicket> deliveryTicketsIterator = deliveryTickets.iterator();
    					while (deliveryTicketsIterator.hasNext()) {
    						DeliveryTicket deliveryTicket = (DeliveryTicket) deliveryTicketsIterator.next();
    						ObservedMeasures observedMeasures = deliveryTicket.getObservedMeasures();
    						checkMeasures(measureGroupTable, observedMeasures, measureGroupId);
    					}
 */   					
					}
				}
			}
		}
		catch (OpenRiskException oe) {
			return PreProcessResult.failed(oe.toString());
		}
		return PreProcessResult./*succeeded();*/failed("NO GOOD reason");
    }
	/**
	 * @date 07-may-2015
	 * @throws OpenRiskException - throws exception if a measure type is changed or missing from the planned measures 
	 */
	private void checkMeasures(StaticTable measureGroupTable, PlannedMeasures measures, int measureGroupId) throws OpenRiskException {
		int measurementTypeColId = measureGroupTable.getColumnId("measurement_type");
    	int unitColId = measureGroupTable.getColumnId("unit");
    	int lowerValueColId = measureGroupTable.getColumnId("lower_value");
    	String objectName = "planned measure";
/*    	for (int row = 0, numRows = measureGroupTable.getRowCount(); row < numRows; row++)
    	{
    		int measurementType = measureGroupTable.getInt(measurementTypeColId, row);
    		int unit = measureGroupTable.getInt(unitColId, row);
    		double lowerValue = measureGroupTable.getDouble(lowerValueColId, row);
    		//now look for this guy
        	Iterator<PlannedMeasure> measureIterator = measures.iterator();
        	boolean notFound = true;
    		while (measureIterator.hasNext()) {
    			PlannedMeasure measure =  (PlannedMeasure) measureIterator.next();
    			if (measure.getValueAsInt(EnumPlannedMeasureFieldId.MeasurementType) == measurementType){
    				notFound = false;
    				if (measure.getValueAsInt(EnumPlannedMeasureFieldId.Unit) != unit){
    					int typeRow = ContainerManagementUtil.measurementTypeTable.findSorted(ContainerManagementUtil.IdColId, measurementType, 1);
    					String typeName = ContainerManagementUtil.measurementTypeTable.getString(ContainerManagementUtil.nameColId, typeRow);
    					Transaction tran = measures.getTransaction();
    					throw new OpenRiskException(objectName + " on transaction " + tran.toString() + " has changed unit for measurement_type " + typeName);
    				}
    				if (!ContainerManagementUtil.doubleEquals(measure.getValueAsDouble(EnumPlannedMeasureFieldId.LowerValue), lowerValue)) {
    					int typeRow = ContainerManagementUtil.measurementTypeTable.findSorted(ContainerManagementUtil.IdColId, measurementType, 1);
    					String typeName = ContainerManagementUtil.measurementTypeTable.getString(ContainerManagementUtil.nameColId, typeRow);
    					Transaction tran = measures.getTransaction();
    					int legNum  = measures.getLegNumber();
    					throw new OpenRiskException(objectName + " on transaction " + tran.toString() + " leg " + legNum + " has changed value for measurement_type " + typeName);
    				}
    				break;//this measure type is OK!
    			}
    		}
    		if (true == notFound){
				int typeRow = ContainerManagementUtil.measurementTypeTable.findSorted(ContainerManagementUtil.IdColId, measurementType, 1);
				String typeName = ContainerManagementUtil.measurementTypeTable.getString(ContainerManagementUtil.nameColId, typeRow);
				Transaction tran = measures.getTransaction();
				int legNum = measures.getLegNumber();
				ScheduleDetail scheduleDetail = (ScheduleDetail)measures.getParent();
				int scheduleId = scheduleDetail.getValueAsInt(EnumScheduleDetailFieldId.Id);
				throw new OpenRiskException(objectName + " on transaction " + tran.toString() + " leg " + legNum + " schedule_id " + scheduleId + " has deleted measurement_type " + typeName);
    		}
    	}*/
	}
	/**
	 * @date 07-may-2015
	 * @throws OpenRiskException - throws exception if a measure type is changed or missing from the observed measures 
	 */
	private void checkMeasures(StaticTable measureGroupTable, ObservedMeasures measures, int measureGroupId) throws OpenRiskException {
		int measurementTypeColId = measureGroupTable.getColumnId("measurement_type");
    	int unitColId = measureGroupTable.getColumnId("unit");
    	int lowerValueColId = measureGroupTable.getColumnId("lower_value");
    	String objectName = "observed measure";
/*    	for (int row = 0, numRows = measureGroupTable.getRowCount(); row < numRows; row++)
    	{
    		int measurementType = measureGroupTable.getInt(measurementTypeColId, row);
    		int unit = measureGroupTable.getInt(unitColId, row);
    		double lowerValue = measureGroupTable.getDouble(lowerValueColId, row);
    		//now look for this guy
        	Iterator<ObservedMeasure> measureIterator = measures.iterator();
        	boolean notFound = true;
    		while (measureIterator.hasNext()) {
    			ObservedMeasure measure =  (ObservedMeasure) measureIterator.next();
    			if (measure.getValueAsInt(EnumObservedMeasureFieldId.MeasurementType) == measurementType){
    				notFound = false;
    				if (measure.getValueAsInt(EnumObservedMeasureFieldId.Unit) != unit){
    					int typeRow = ContainerManagementUtil.measurementTypeTable.findSorted(ContainerManagementUtil.IdColId, measurementType, 1);
    					String typeName = ContainerManagementUtil.measurementTypeTable.getString(ContainerManagementUtil.nameColId, typeRow);
    					Transaction tran = measures.getTransaction();
    					throw new OpenRiskException(objectName + " on transaction " + tran.toString() + " has changed unit for measurement_type " + typeName);
    				}
    				if (measure.getValueAsDouble(EnumObservedMeasureFieldId.Value) != lowerValue) {
    					int typeRow = ContainerManagementUtil.measurementTypeTable.findSorted(ContainerManagementUtil.IdColId, measurementType, 1);
    					String typeName = ContainerManagementUtil.measurementTypeTable.getString(ContainerManagementUtil.nameColId, typeRow);
    					Transaction tran = measures.getTransaction();
    					int legNum  = measures.getLegNumber();
    					throw new OpenRiskException(objectName + " on transaction " + tran.toString() + " leg " + legNum + " has changed value for measurement_type " + typeName);
    				}
    				break;//this measure type is OK!
    			}
    		}
    		if (true == notFound){
				int typeRow = ContainerManagementUtil.measurementTypeTable.findSorted(ContainerManagementUtil.IdColId, measurementType, 1);
				String typeName = ContainerManagementUtil.measurementTypeTable.getString(ContainerManagementUtil.nameColId, typeRow);
				Transaction tran = measures.getTransaction();
				int legNum  = measures.getLegNumber();
				DeliveryTicket deliveryTicket = (DeliveryTicket)measures.getParent();
				int ticketId = deliveryTicket.getValueAsInt(EnumDeliveryTicketFieldId.SystemTicketId);
				String containerNum = deliveryTicket.getValueAsString(EnumDeliveryTicketFieldId.Id);
				throw new OpenRiskException(objectName + " on transaction " + tran.toString() + " leg " + legNum + " ticket_id " + ticketId + " container number " + containerNum + " has deleted measurement_type " + typeName);
    		}
    	}*/
	}
	
	/** 
	 * @param nominations
	 * @param deliveryId
	 * @return Nomination
	 * @throws OpenRiskException
	 * @date 08-may-2015
	 */
	private Nomination findNominationByDeliveryId(Nominations nominations, int deliveryId) throws OpenRiskException {
		Iterator<Nomination> nominationIterator = nominations.iterator();
		while (nominationIterator.hasNext()) {
			Nomination nomination = nominationIterator.next();
			int nomDeliveryId = nomination.getValueAsInt(EnumNominationFieldId.Id); 
			if (nomDeliveryId == deliveryId)
				return nomination;
				
		}
		return (Nomination)null;
	}
}