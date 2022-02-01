package com.matthey.pmm.toms.service.live;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.matthey.pmm.toms.service.live.logic.EndurTradeBooking;
import com.matthey.pmm.toms.service.live.logic.MasterDataSynchronisation;

import org.tinylog.Logger;


@Component
public class TimedServicesAll {
	@Autowired
	private MasterDataSynchronisation masterDataSynchronisation;
	
	@Autowired 
	private EndurTradeBooking endurTradeBooking;
	

	@Scheduled(cron = "${toms.endur.service.masterdata.synchronisation.cron}")
	public void synchroniseReferenceData () {
		try {
			Logger.info("Starting Masterdata Synchronisation on defined cron expression");
			masterDataSynchronisation.syncMasterdataWithEndur();
			Logger.info("Successfully finished Masterdata Synchronisation on defined cron expression");			
		} catch (Exception ex) {
			Logger.error("Exception while applying masterdata synchronisation: " + ex.toString());
			StringWriter sw = new StringWriter(4000);
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			Logger.error(sw.toString());
		}
	}
	
	@Scheduled(cron = "${toms.endur.service.tradebooking.fillservice.cron}")
	public void fillOpenFills() {
		try {
			Logger.info("Starting Endur Deal Booking process for fills on defined cron expression");
			endurTradeBooking.processOpenFills();
			Logger.info("Successfully finished Endur Deal Booking process for fills on defined cron expression");
		} catch (Exception ex) {
			Logger.error("Exception while applying masterdata synchronisation: " + ex.toString());
			StringWriter sw = new StringWriter(4000);
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			Logger.error(sw.toString());
		}
	}
}
