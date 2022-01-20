package com.matthey.pmm.toms.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public abstract class AbstractReferenceService extends AbstractToDiffService<ReferenceTo> {
    private static final Logger logger = LogManager.getLogger(AbstractReferenceService.class);
	
	public AbstractReferenceService (final Session session) {
		super(session);
	}
	
	protected abstract DefaultReferenceType getReferenceType();
	
	@Override
	protected void syncEndurSideIds (List<ReferenceTo> knownTos, List<ReferenceTo> endurSideTos) {
		for (ReferenceTo knownTo : knownTos) {
			for (int i=endurSideTos.size()-1; i >= 0; i--) {
				if ((long)endurSideTos.get(i).endurId() == (long)knownTo.endurId()) {
					logger.debug("Matched Endur TO '" + endurSideTos.get(i) + "' with TOMS TO '" + knownTo + "'");
					ReferenceTo oldEntry = endurSideTos.remove(i);
					ReferenceTo endurSideWithUpdatedTomsId = ImmutableReferenceTo.builder()
							.from(oldEntry)
							.id(knownTo.id())
							.build();
					endurSideTos.add(i, endurSideWithUpdatedTomsId);
				}
			}
		}
	}

	@Override
	protected List<ReferenceTo> convertReportToTransferObjects(Table endurSideData) {
		List<ReferenceTo> convertedEntities = new ArrayList<>(endurSideData.getRowCount());
		for (int row=endurSideData.getRowCount()-1; row >= 0; row--) {
			
			ReferenceTo converted = ImmutableReferenceTo.builder()
					.id(0l)
					.displayName(endurSideData.getString("name", row))
					.endurId((long)endurSideData.getInt("reference_id", row))
					.idType(getReferenceType().getEntity().id())
					.name(endurSideData.getString("name", row))
					.sortColumn(0l)
					 // assumption: The Endur side Report Builder Report is going to retrieve data valid for TOMS only
					.idLifecycle(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity().id())
					.build();
			convertedEntities.add(converted);
		}
		return convertedEntities;
	}
	
	@Override
	protected boolean isDiffInAuxFields(ReferenceTo knownTo, ReferenceTo updatedTo) {
		return !knownTo.name().equals(updatedTo.name())
				;
	}

	@Override
	protected ReferenceTo updateLifeCycleStatus(ReferenceTo knownTo, DefaultReference lifecycleStatus) {
		return ImmutableReferenceTo.builder().from(knownTo)
				.idLifecycle(lifecycleStatus.getEntity().id())
				.build();
	}
}
