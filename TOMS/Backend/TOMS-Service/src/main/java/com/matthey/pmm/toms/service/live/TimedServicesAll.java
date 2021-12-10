package com.matthey.pmm.toms.service.live;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.matthey.pmm.toms.service.live.logic.MasterDataSynchronisation;

@Component
public class TimedServicesAll {
	@Autowired
	private MasterDataSynchronisation masterDataSynchronisation;
	

	@Scheduled(cron = "${toms.endur.service.masterdata.synchronisation.cron}")
	public void synchroniseReferenceData () {
		masterDataSynchronisation.syncMasterdataWithEndur();
	}
}
