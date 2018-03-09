package com.olf.jm.autosipopulation.persistence;

import java.util.Collection;
import java.util.List;

import com.olf.jm.autosipopulation.model.EnumRunMode;
import com.olf.jm.autosipopulation.model.TranInfoField;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2015-11-27	V1.0	jwaechter	- initial version
 * 2015-12-08	V1.1	jwaechter	- now setting external SI only
 * 2017-05-31	V1.2	jwaechter	- defect fix: only setting the value on the field in 
 * 								      case the field exists
 */

/**
 * This class is doing the same as it's parent but writes the results to leg level  
 * tran info fields instead of on tran level settlement instructions.
 * @author jwaechter
 * @version 1.2
 */
public class LogicResultApplicatorTranInfoField extends LogicResultApplicator{
	protected final List<Integer> preciousMetalList;
	
	public LogicResultApplicatorTranInfoField (final Transaction tran, final Session session, 
			final Collection<LogicResult> results, final EnumRunMode rmode,
			final List<Integer> preciousMetalList) {
		super (tran, session, results, rmode);
		this.preciousMetalList = preciousMetalList;
	}
	
	protected void setInternal (LogicResult result, int intSettleId) {
		int paraSeqNum = result.getDd().getLegNum();
		if (paraSeqNum == -1) {
			return;
		}
		int paramGroup = getParamGroup (paraSeqNum);
		int ccyId = result.getDd().getCcyId();
		if (preciousMetalList.contains(ccyId)) {
			for (Leg leg : tran.getLegs()) {
				if (leg.getValueAsInt(EnumLegFieldId.ParamGroup) == paramGroup) {
					Field siField  = leg.getField(TranInfoField.SI_PHYS_INTERNAL.getName());
					
					if (siField != null && siField.isApplicable() && siField.isWritable()) {
						siField.setValue(intSettleId);					
					}
				}
			}
		}
		
	}

	protected void setExternal (LogicResult result, int extSettleId) {
		int paraSeqNum = result.getDd().getLegNum();
		if (paraSeqNum == -1) {
			return;
		}
		int paramGroup = getParamGroup (paraSeqNum);
		int ccyId = result.getDd().getCcyId();
		if (preciousMetalList.contains(ccyId)) {
			for (Leg leg : tran.getLegs()) {
				if (leg.getValueAsInt(EnumLegFieldId.ParamGroup) == paramGroup) {
					Field siField  = leg.getField(TranInfoField.SI_PHYS.getName());
					
					if (siField != null && siField.isApplicable() && siField.isWritable()) {
						siField.setValue(extSettleId);					
					}
				}
			}
		}
	}
	
	private int getParamGroup(int paraSeqNum) {
		return tran.getLeg(paraSeqNum).getValueAsInt(EnumLegFieldId.ParamGroup);
	}
}
