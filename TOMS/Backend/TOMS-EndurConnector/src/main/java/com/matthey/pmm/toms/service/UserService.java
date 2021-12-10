package com.matthey.pmm.toms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.service.misc.ReportBuilderHelper;
import com.matthey.pmm.toms.transport.ImmutableUserTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.UserTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public class UserService extends AbstractToDiffService<UserTo> {
	protected final Map<Long, Long> endurPortfolioIdToReferenceId;
	
	public UserService (final Session session, List<ReferenceTo> portfolioList) {
		super(session);
		this.endurPortfolioIdToReferenceId = new HashMap<>();
		portfolioList.forEach(x -> endurPortfolioIdToReferenceId.put(x.endurId(), x.id()));
	}
	
	public String getSyncCategory () {
		return "UserData";		
	}
	

	@Override
	protected void syncEndurSideIds(List<UserTo> knownTos, List<UserTo> endurSideTos) {		
		// nothing to do for UserTo
	}

	@Override
	protected List<UserTo> convertReportToTransferObjects(Table endurSideData) {
		try (Table tradeableParties = ReportBuilderHelper.runReport(session.getTableFactory(), "UserDataTradeableParties");
			Table tradeablePortfolios = ReportBuilderHelper.runReport(session.getTableFactory(), "UserDataTradeablePortfolios")) {
			List<UserTo> convertedEntities = new ArrayList<>(endurSideData.getRowCount());
			for (int row=endurSideData.getRowCount()-1; row >= 0; row--) {
				List<Long> tradeableInternalPartiesForRow = getTradeablePartiesFor (endurSideData.getInt("id_number", row), false, tradeableParties);
				List<Long> tradeableCounterPartiesForRow = getTradeablePartiesFor (endurSideData.getInt("id_number", row), true, tradeableParties);
				List<Long> tradeablePortfoliosForRow = getTradeablePortfoliosFor (endurSideData.getInt("id_number", row), tradeablePortfolios);
				DefaultReference role = getRole(endurSideData, row);
				
				UserTo converted = ImmutableUserTo.builder()
						.id(endurSideData.getInt("id_number", row))
						.email(endurSideData.getString("email", row))
						.firstName(endurSideData.getString("first_name", row))
						.lastName(endurSideData.getString("last_name", row))
						.roleId(role.getEntity().id())
						.tradeableCounterPartyIds(tradeableCounterPartiesForRow)
						.tradeableInternalPartyIds(tradeableInternalPartiesForRow)
						.tradeablePortfolioIds(tradeablePortfoliosForRow)
						 // assumption: The Endur side Report Builder Report is going to retrieve valid users for TOMS only
						.idLifecycleStatus(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity().id())
						.build();
				convertedEntities.add(converted);
			}
			return convertedEntities;			
		}
	}

	private List<Long> getTradeablePartiesFor(int userId, boolean external, Table tradeableParties) {
		List<Long> tradeablePartiesForUser = new ArrayList<>();
		for (int row = tradeableParties.getRowCount()-1; row >= 0; row--) {
			int rowUserId = tradeableParties.getInt("id_number", row);
			if (userId == rowUserId) {
				int partyId = tradeableParties.getInt("party_id", row);
				int intExt = tradeableParties.getInt("int_ext", row);
				if ((external && intExt == 1) || !(external && intExt == 0)) {
					tradeablePartiesForUser.add((long)partyId);					
				}
			}
		}
		return tradeablePartiesForUser;
	}

	private List<Long> getTradeablePortfoliosFor(int userId, Table tradeablePortfolios) {
		List<Long> tradeablePortfolioIds  = new ArrayList<>();
		for (int row = tradeablePortfolios.getRowCount()-1; row >= 0; row--) {
			int rowUserId = tradeablePortfolios.getInt("personnel_id", row);
			if (userId == rowUserId) {
				int endurPortfolioId = tradeablePortfolios.getInt("id_number", row);
				Long referenceId = endurPortfolioIdToReferenceId.get(endurPortfolioId);
				if (referenceId != null) {
					tradeablePortfolioIds.add(referenceId);
				}
			}
		}
		return tradeablePortfolioIds;
	}

	private DefaultReference getRole(Table endurSideData, int row) {
		String funcGroup = endurSideData.getString("func_group", row);
		switch (funcGroup) {
		case "TOMS_Admin":
			return DefaultReference.USER_ROLE_ADMIN;
		case "TOMS_PMM_Traders":
			return DefaultReference.USER_ROLE_PMM_TRADER;
		case "TOMS_External":
			return DefaultReference.USER_ROLE_EXTERNAL;
		case "TOMS_Support_Users ":
			return DefaultReference.USER_ROLE_PMM_SUPPORT;
		case "TOMS_PMM_FO":
			return DefaultReference.USER_ROLE_PMM_FO;
		case "TOMS_Technical":
			return DefaultReference.USER_ROLE_SERVICE_USER;
		default:
			throw new RuntimeException ("Could not decide the type of the user based on the provided input data for user #" + 
					endurSideData.getInt("id_number", row) + " having functional group '" + funcGroup + "'."
					+ " Expected values are either 'TOMS_Admin', 'TOMS_PMM_Traders' or 'TOMS_External'");			
		}
		
	}
	
	@Override
	protected boolean isDiffInAuxFields(UserTo knownTo, UserTo updatedTo) {
		
		return !knownTo.email().equals(updatedTo.email())
				|| !knownTo.firstName().equals(updatedTo.firstName())
				|| !knownTo.lastName().equals(updatedTo.lastName())				
				|| knownTo.roleId() != updatedTo.roleId()
				|| !knownTo.tradeableCounterPartyIds().containsAll(updatedTo.tradeableCounterPartyIds())
				|| !updatedTo.tradeableCounterPartyIds().containsAll(knownTo.tradeableCounterPartyIds())
				|| !knownTo.tradeableInternalPartyIds().containsAll(updatedTo.tradeableInternalPartyIds())
				|| !updatedTo.tradeableInternalPartyIds().containsAll(knownTo.tradeableInternalPartyIds())
				|| !knownTo.tradeablePortfolioIds().containsAll(updatedTo.tradeablePortfolioIds())
				|| !updatedTo.tradeablePortfolioIds().containsAll(knownTo.tradeablePortfolioIds())
				;
	}

	@Override
	protected UserTo updateLifeCycleStatus(UserTo knownTo, DefaultReference lifecycleStatus) {
		return ImmutableUserTo.builder().from(knownTo)
				.idLifecycleStatus(lifecycleStatus.getEntity().id())
				.build();
	}
}
