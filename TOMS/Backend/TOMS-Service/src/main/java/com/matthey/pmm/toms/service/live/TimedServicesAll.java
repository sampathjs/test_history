package com.matthey.pmm.toms.service.live;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.matthey.pmm.toms.service.live.logic.EndurTradeBooking;
import com.matthey.pmm.toms.service.live.logic.MasterDataSynchronisation;

@Component
public class TimedServicesAll {
	private final static Logger logger = LogManager.getLogger(TimedServicesAll.class);

	@Autowired
	private MasterDataSynchronisation masterDataSynchronisation;
	
	@Autowired 
	private EndurTradeBooking endurTradeBooking;
	

	@Scheduled(cron = "${toms.endur.service.masterdata.synchronisation.cron}")
	public void synchroniseReferenceData () {
		try {
			logger.info("Starting Masterdata Synchronisation on defined cron expression");
			masterDataSynchronisation.syncMasterdataWithEndur();
			logger.info("Successfully finished Masterdata Synchronisation on defined cron expression");			
		} catch (Exception ex) {
			logger.error("Exception while applying masterdata synchronisation: " + ex.toString());
			StringWriter sw = new StringWriter(4000);
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			logger.error(sw.toString());
		}
	}
	
	@Scheduled(cron = "${toms.endur.service.tradebooking.fillservice.cron}")
	public void fillOpenFills() {
		try {
			logger.info("Starting Endur Deal Booking process for fills on defined cron expression");
			endurTradeBooking.processOpenFills();
			logger.info("Successfully finished Endur Deal Booking process for fills on defined cron expression");
		} catch (Exception ex) {
			logger.error("Exception while applying masterdata synchronisation: " + ex.toString());
			StringWriter sw = new StringWriter(4000);
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			logger.error(sw.toString());
		}
	}
}
