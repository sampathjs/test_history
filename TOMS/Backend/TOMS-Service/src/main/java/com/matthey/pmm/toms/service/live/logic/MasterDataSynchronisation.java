package com.matthey.pmm.toms.service.live.logic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.transport.ImmutableTwoListsTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.TwoListsTo;
import com.matthey.pmm.toms.transport.UserTo;

@Component
public class MasterDataSynchronisation {
    private static final Logger logger = LoggerFactory.getLogger(MasterDataSynchronisation.class);
	
	@Autowired
	private EndurConnector endurConnector;
	
	@Autowired
	private PartyRepository partyRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ReferenceRepository refRepo;
	
	@Autowired
	private PartyConverter partyConverter;

	@Autowired
	private UserConverter userConverter;

	@Autowired
	private ReferenceConverter refConverter;
	
	public void syncMasterdataWithEndur () {
		logger.info("Starting Master Data Synchronisation with Endur");
		boolean partyDataSyncSuccess = syncPartyData();
		boolean userDataSyncSuccess = syncUserData();
		boolean refDataSyncSuccess = syncRefData();
		logger.info("Synchronisation status: Party Data: " + partyDataSyncSuccess 
				+ ", User Data: " + userDataSyncSuccess+ "  Reference Data: " + refDataSyncSuccess);		
		logger.info("Finished Master Data Synchronisation with Endur");
	}

	private boolean syncRefData() {
		try {
			logger.info("Synchronisaton of Reference Data");
			List<Reference> allReferenceEntites = refRepo.findByTypeIdIn(Arrays.asList(
					DefaultReferenceType.BUY_SELL.getEntity().id(),
					DefaultReferenceType.CCY_CURRENCY.getEntity().id(),
					DefaultReferenceType.INDEX_NAME.getEntity().id(),
					DefaultReferenceType.METAL_FORM.getEntity().id(),
					DefaultReferenceType.METAL_LOCATION.getEntity().id(),					
					DefaultReferenceType.PORTFOLIO.getEntity().id(),
					DefaultReferenceType.QUANTITY_UNIT.getEntity().id(),
					DefaultReferenceType.REF_SOURCE.getEntity().id(),
					DefaultReferenceType.TICKER.getEntity().id(),
					DefaultReferenceType.YES_NO.getEntity().id()
					));
			logger.debug ("List of all References known to TOMS: " + allReferenceEntites);
			List<ReferenceTo> allReferences = allReferenceEntites.stream()
					.map(x -> refConverter.toTo(x))
					.collect(Collectors.toList());
			List<ReferenceTo> referenceDiff = Arrays.asList(endurConnector.postWithResponse("/toms/endur/references", ReferenceTo[].class, allReferences));
			logger.info ("Successfully retrieved a difference list of " + referenceDiff.size() + " elements from Endur Connector");
			logger.debug ("List of all Reference Diff as returned by Endur Connector: " + referenceDiff);
			referenceDiff.forEach(x -> refConverter.toManagedEntity(x));
			logger.info("Successfully finished reference data synchronisation");			
		} catch (Exception ex) {
			logger.error("Exception while synchronising reference data: " + ex.getMessage());
			for (StackTraceElement ste : ex.getStackTrace()) {
				logger.error(ste.toString());
			}
			// consume exception as we do want to avoid blocking the synchronisation of other master data elemenents
			return false;
		}
		return true;
	}
	
	private boolean syncPartyData() {
		try {
			logger.info("Synchronisaton of Party Data");
			List<Party> allPartyEntities = partyRepo.findAll();
			logger.debug ("List of all Parties known to TOMS: " + allPartyEntities);
			List<PartyTo> allPartyTos = allPartyEntities.stream()
					.map(x -> partyConverter.toTo(x))
					.collect(Collectors.toList());
			List<PartyTo> partyDiff = Arrays.asList(endurConnector.postWithResponse("/toms/endur/parties", PartyTo[].class, allPartyTos));
			logger.info ("Successfully retrieved a difference list of " + partyDiff.size() + " elements from Endur Connector");
			logger.debug ("List of all Party Diff as returned by Endur Connector: " + partyDiff);
			partyDiff.forEach(x -> partyConverter.toManagedEntity(x));
			logger.info("Successfully finished party data synchronisation");			
		} catch (Exception ex) {
			logger.error("Exception while synchronising party data: " + ex.getMessage());
			for (StackTraceElement ste : ex.getStackTrace()) {
				logger.error(ste.toString());
			}
			// consume exception as we do want to avoid blocking the synchronisation of other master data elemenents
			return false;
		}
		return true;
	}
	
	private boolean syncUserData() {
		try {
			logger.info("Synchronisaton of User Data");
			List<User> allUserEntities = userRepo.findAll();
			logger.debug ("List of all Users known to TOMS: " + allUserEntities);
			List<UserTo> allUserTos = allUserEntities.stream()
					.map(x -> userConverter.toTo(x))
					.collect(Collectors.toList());

			List<Reference> allPortfolioEntities = refRepo.findByTypeId(DefaultReferenceType.PORTFOLIO.getEntity().id());
			logger.debug ("List of all Portfolios known to TOMS: " + allPortfolioEntities);
			List<ReferenceTo> allPortfolioTos = allPortfolioEntities.stream()
					.map(x -> refConverter.toTo(x))
					.collect(Collectors.toList());
			
			TwoListsTo<UserTo, ReferenceTo> knownUsersAndPortfolios = ImmutableTwoListsTo.<UserTo, ReferenceTo>builder()
					.listOne(allUserTos)
					.listTwo(allPortfolioTos)
					.build();
			
			List<UserTo> userDiff = Arrays.asList(endurConnector.postWithResponse("/toms/endur/users", UserTo[].class, knownUsersAndPortfolios));
			logger.info ("Successfully retrieved a difference list of " + userDiff.size() + " elements from Endur Connector");
			logger.debug ("List of all User Diff as returned by Endur Connector: " + userDiff);
			userDiff.forEach(x -> userConverter.toManagedEntity(x));
			logger.info("Successfully finished user data synchronisation");			
		} catch (Exception ex) {
			logger.error("Exception while synchronising user data: " + ex.getMessage());
			for (StackTraceElement ste : ex.getStackTrace()) {
				logger.error(ste.toString());
			}
			// consume exception as we do want to avoid blocking the synchronisation of other master data elemenents
			return false;
		}
		return true;
	}

}
