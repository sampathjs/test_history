package com.matthey.pmm.toms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.service.misc.ReportBuilderHelper;
import com.matthey.pmm.toms.transport.ImmutableUserTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.UserTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public class UserService extends AbstractToDiffService<UserTo> {
    private static final Logger logger = LogManager.getLogger(UserService.class);
	
	
	protected final Map<Long, Long> endurPortfolioIdToReferenceId;
	
	public UserService (final Session session, List<ReferenceTo> portfolioList) {
		super(session);
		this.endurPortfolioIdToReferenceId = new HashMap<>();
		portfolioList.forEach(x -> endurPortfolioIdToReferenceId.put(x.endurId(), x.id()));
		logger.info ("Endur to TOMS ID map for portfolios: " +  endurPortfolioIdToReferenceId);
	}
	
	public String getSyncCategory () {
		return "UserData";		
	}
	
	@Override
	protected void syncEndurSideIds(List<UserTo> knownTos, List<UserTo> endurSideTos) {		
		// nothing to do for UserTo as IDs of users are Endur IDs directly
	}

	@Override
	protected List<UserTo> convertReportToTransferObjects(Table endurSideData) {
		logger.info("Converting user objects from report builder definitions to TOs");
		String tradeablePartiesReport = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory (session.getIOFactory(), "UserDataTradeableParties");
		logger.info("Retrieving tradeable parties for user from ReportBuilder report '" + tradeablePartiesReport + "'");
		String tradeablePortfoliosReport = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory (session.getIOFactory(), "UserDataTradeablePortfolios");
		logger.info("Retrieving tradeable portfolios for user from ReportBuilder report '" + tradeablePortfoliosReport + "'");
		
		try (Table tradeableParties = ReportBuilderHelper.runReport(session.getTableFactory(), tradeablePartiesReport);
			Table tradeablePortfolios = ReportBuilderHelper.runReport(session.getTableFactory(), tradeablePortfoliosReport)) {
			logger.info("Successfully executed the two additional report builder definitions '" + tradeablePartiesReport + "' and '" + tradeablePortfoliosReport + "'");
			logger.info("Tradeable Portfolio Report Row Count: " + tradeablePortfolios.getRowCount());
			logger.info("Tradeable Parties Report Row Count: " + tradeableParties.getRowCount());
			
			List<UserTo> convertedEntities = new ArrayList<>(endurSideData.getRowCount());
			for (int row=endurSideData.getRowCount()-1; row >= 0; row--) {
				List<Long> tradeableInternalPartiesForRow = getTradeablePartiesFor (endurSideData.getInt("id_number", row), false, tradeableParties);
				List<Long> tradeableCounterPartiesForRow = getTradeablePartiesFor (endurSideData.getInt("id_number", row), true, tradeableParties);
				List<Long> tradeablePortfoliosForRow = getTradeablePortfoliosFor (endurSideData.getInt("id_number", row), tradeablePortfolios);
				Long defaultInternalBu = (endurSideData.getInt("default_party_id", row) != 0)?(Long.valueOf(endurSideData.getInt("default_party_id", row))):null;
				Long defaultInternalPortfolio = (endurSideData.getInt("default_portfolio_id", row) != 0)?(Long.valueOf(endurSideData.getInt("default_portfolio_id", row))):null;
				defaultInternalPortfolio = defaultInternalPortfolio != null?endurPortfolioIdToReferenceId.get(defaultInternalPortfolio):null;
				DefaultReference role = getRole(endurSideData, row);
				logger.debug("New tradeableCounterPartyIds: " + tradeableCounterPartiesForRow);
				logger.debug("New tradeablePortfolioIds: " + tradeablePortfoliosForRow);
				
				UserTo converted = ImmutableUserTo.builder()
						.id(endurSideData.getInt("id_number", row))
						.email(endurSideData.getString("email", row))
						.firstName(endurSideData.getString("first_name", row))
						.lastName(endurSideData.getString("last_name", row))
						.systemName(endurSideData.getString("name", row))
						.roleId(role.getEntity().id())
						.idDefaultInternalBu(defaultInternalBu)
						.idDefaultInternalPortfolio(defaultInternalPortfolio)
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
			int partyId = tradeableParties.getInt("party_id", row);
			int intExt = tradeableParties.getInt("int_ext", row);
			if (userId == rowUserId) {
				if ((external && intExt == 1) || !(external && intExt == 0)) {
					tradeablePartiesForUser.add((long)partyId);					
				}
			} else {
				if (external && intExt == 1) {
					tradeablePartiesForUser.add((long)partyId);					
				}				
			}
		}
		return tradeablePartiesForUser;
	}

	private List<Long> getTradeablePortfoliosFor(int userId, Table tradeablePortfolios) {
		List<Long> tradeablePortfolioIds  = new ArrayList<>(40);
		for (int row = tradeablePortfolios.getRowCount()-1; row >= 0; row--) {
			int rowUserId = tradeablePortfolios.getInt("personnel_id", row);
			if (userId == rowUserId) {
				long endurPortfolioId = (long)tradeablePortfolios.getInt("id_number", row);
				logger.debug("Looking up TOMS ID for Endur Portfolio ID #" + endurPortfolioId + " for user #" + userId);
				Long referenceId = endurPortfolioIdToReferenceId.get(endurPortfolioId);
				logger.debug("TOMS ID by map.get: " + referenceId + " for Endur Portfolio ID #" + endurPortfolioId + " for user #" + userId);				
				tradeablePortfolioIds.add(referenceId);
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
		case "TOMS_Support_Users":
			return DefaultReference.USER_ROLE_PMM_SUPPORT;
		case "TOMS_PMM_FO":
			return DefaultReference.USER_ROLE_PMM_FO;
		case "TOMS_Technical":
			return DefaultReference.USER_ROLE_SERVICE_USER;
		default:
			throw new RuntimeException ("Could not decide the type of the user based on the provided input data for user #" + 
					endurSideData.getInt("id_number", row) + " having functional group '" + funcGroup + "'."
					+ " Expected values are either 'TOMS_Admin', 'TOMS_PMM_Traders' or 'TOMS_External'"
					+ " or 'TOMS_Admin' or 'TOMS_Technical' or 'TOMS_Support_Users' or 'TOMS_PMM_FO'");			
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
				|| !updatedTo.systemName().equals(knownTo.systemName())
				;
	}

	@Override
	protected UserTo updateLifeCycleStatus(UserTo knownTo, DefaultReference lifecycleStatus) {
		return ImmutableUserTo.builder().from(knownTo)
				.idLifecycleStatus(lifecycleStatus.getEntity().id())
				.build();
	}
	
	@Override
	protected long getLifeCycleStatusId(UserTo knownTo) {
		return knownTo.idLifecycleStatus();
	}
}
