package com.matthey.pmm.toms.service.live.logic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.transport.PartyTo;

@Component
public class MasterDataSynchronisation {
    private static final Logger logger = LoggerFactory.getLogger(MasterDataSynchronisation.class);
	
	@Autowired
	private EndurConnector endurConnector;
	
	@Autowired
	private PartyRepository partyRepo;
	
	@Autowired
	private PartyConverter partyConverter;
	
	public void syncMasterdataWithEndur () {
		logger.info("Starting Master Data Synchronisation with Endur");
		boolean partyDataSyncSuccess = syncPartyData();
		logger.info("Finished Master Data Synchronisation with Endur");
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
}
