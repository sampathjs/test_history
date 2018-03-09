package com.olf.jm.storageDealManagement.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.openlink.util.logging.PluginLog;

public class StorageDeals {
	
	private Context context;
	
	public StorageDeals(Context currentContext) {
		context = currentContext;
	}

	public List<StorageDeal> getStorageDeal(Date currentDate) {
		
		String sql = DbHelper.buildSqlCommStoreMaturingOnDate(context, null, null, currentDate);
		
		ArrayList<StorageDeal> storageDeals = new ArrayList<StorageDeal>();
		try (Table storageDealdData = DbHelper.runSql(context, sql)) {
		
			for (TableRow storageDeal : storageDealdData.getRows()) {
				storageDeals.add(new StorageDeal(storageDeal));
			}
		}
		return storageDeals;
		
	}
	

}

