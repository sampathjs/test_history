package com.matthey.pmm.toms.service;

import java.util.ArrayList;
import java.util.List;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutablePartyTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public class PartyService extends AbstractToDiffService<PartyTo> {
	
	public PartyService (final Session session) {
		super(session);
	}
	
	public String getSyncCategory () {
		return "PartyData";		
	}
	
	@Override
	protected void syncEndurSideIds(List<PartyTo> knownTos, List<PartyTo> endurSideTos) {
		// nothing to do for parties
	}

	@Override
	protected List<PartyTo> convertReportToTransferObjects(Table endurSideData) {
		List<PartyTo> convertedEntities = new ArrayList<>(endurSideData.getRowCount());
		for (int row=endurSideData.getRowCount()-1; row >= 0; row--) {
			DefaultReference type = getType (endurSideData, row, true);

			PartyTo converted = ImmutablePartyTo.builder() // LE
					.id(endurSideData.getInt("legal_entity_id", row))
					.idLegalEntity(null)
					 // assumption: The Endur side Report Builder Report is going to retrieve valid parties for TOMS only
					.idLifecycle(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity().id())
					.name(endurSideData.getString("short_name_le", row)) 
					.sortColumn(5000000l) // has to be changed to more complex logic potentially 
					.typeId(type.getEntity().id())
					.build();
			convertedEntities.add(converted);
			
			type = getType (endurSideData, row, false);
			
			converted = ImmutablePartyTo.builder() // BU
					.id(endurSideData.getInt("party_id", row))
					.idLegalEntity(endurSideData.getInt("legal_entity_id", row) != 0?(long)endurSideData.getInt("legal_entity_id", row):0l)
					 // assumption: The Endur side Report Builder Report is going to retrieve valid parties for TOMS only
					.idLifecycle(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity().id())
					.name(endurSideData.getString("short_name", row)) 
					.sortColumn(5000000l) // has to be changed to more complex logic potentially 
					.typeId(type.getEntity().id())
					.build();
			convertedEntities.add(converted);
		}
		return convertedEntities;
	}

	private DefaultReference getType(Table endurSideData, int row, boolean isLegal) {
		if (endurSideData.getString("int_ext", row).equalsIgnoreCase("Internal")) {
			if (isLegal) {
				return DefaultReference.PARTY_TYPE_INTERNAL_LE;
			} else {
				return DefaultReference.PARTY_TYPE_INTERNAL_BUNIT;
			}
		} else if (endurSideData.getString("int_ext", row).equalsIgnoreCase("External")) {
			if (isLegal) {
				return DefaultReference.PARTY_TYPE_EXTERNAL_LE;
			} else {
				return DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT;								
			}
		}
		throw new RuntimeException ("Could not decide the type of the party based on the provided input data for party #" + 
				endurSideData.getInt("party_id", row));
	}
	
	@Override
	protected boolean isDiffInAuxFields(PartyTo knownTo, PartyTo updatedTo) {
		return knownTo.idLegalEntity() != updatedTo.idLegalEntity()
				|| !knownTo.name().equals(updatedTo.name())
				|| knownTo.sortColumn() != updatedTo.sortColumn()
				|| knownTo.typeId() != updatedTo.typeId()
				;
	}

	@Override
	protected PartyTo updateLifeCycleStatus(PartyTo knownTo, DefaultReference lifecycleStatus) {
		return ImmutablePartyTo.builder().from(knownTo)
				.idLifecycle(lifecycleStatus.getEntity().id())
				.build();
	}
}
