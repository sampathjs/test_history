package com.matthey.utilities;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

public class Utils {
	
	
	public static String convertUserNamesToEmailList(String listOfUsers) throws OException {
		
		listOfUsers = listOfUsers.replaceAll(",", ";");
		String personnelSplit [] = listOfUsers.split(";");
		int personnelSplitCount = personnelSplit.length;
		String SQLlistOfUsers = "";
		String SQLlistOfEmails = "";
		String retEmailValues = "";
		
		for (int iLoop = 0; iLoop<personnelSplitCount;iLoop++){
			String thisUser = personnelSplit[iLoop].trim();
			if (thisUser.length()>0){
				if (thisUser.indexOf("@")>0){
					if (SQLlistOfEmails.length()>0){
						SQLlistOfEmails = SQLlistOfEmails + "," + "'" + thisUser + "'"; 
					} else {
						SQLlistOfEmails = "'" + thisUser + "'";
					}
				} else {
					if (SQLlistOfUsers.length()>0){
						SQLlistOfUsers = SQLlistOfUsers + "," + "'" + thisUser + "'" ; 
					} else {
						SQLlistOfUsers = "'" + thisUser + "'" ;
					}					 
				}
			}
		}
		
		if (SQLlistOfUsers.length()>0 || SQLlistOfEmails.length()>0){
			
			String sqlByUser = "SELECT * FROM personnel per \n" +
							 	 " WHERE per.name IN (" + SQLlistOfUsers + ")\n" +
							 	 " AND per.status = 1";
			
			String sqlByEmail = "SELECT * FROM personnel per \n" +
							 	 " WHERE per.email IN (" + SQLlistOfEmails + ")\n" +
							 	 " AND per.status = 1";
			
			String sqlUnion = "";
			if (SQLlistOfUsers.length()>0){
				sqlUnion = sqlByUser; 
			}
			if (SQLlistOfEmails.length()>0){
				if (sqlUnion.length()>0){
					sqlUnion = sqlUnion  + "UNION " + sqlByEmail;
				} else {
					sqlUnion = sqlByEmail;
				}
				 
			}
			
			Table personnelTable = Table.tableNew();
			DBaseTable.execISql(personnelTable, sqlUnion);
			int personnelTableCount=personnelTable.getNumRows();
			for (int iLoop = 1; iLoop<=personnelTableCount;iLoop++){
				String emailReturned = personnelTable.getString("email",iLoop);
				if (retEmailValues.length()>0){
					retEmailValues = retEmailValues + ";" + emailReturned; 
				} else {
					retEmailValues = emailReturned;
				}
			}
			
			personnelTable.destroy();
		}
		
		if (retEmailValues.length()==0){
			PluginLog.error("Unrecognised email found : " + listOfUsers + " Going to use supports email");
			String sql = "SELECT * FROM personnel per \n" +
					 	 " WHERE per.name ='Endur_Support'\n" +
					 	 " AND per.status = 1";
			Table personnelTable = Table.tableNew();
			DBaseTable.execISql(personnelTable, sql);
			int personnelTableCount=personnelTable.getNumRows();
			for (int iLoop = 1; iLoop<=personnelTableCount;iLoop++){
				String emailReturned = personnelTable.getString("email",iLoop);
				if (retEmailValues.length()>0){
					retEmailValues = retEmailValues + ";" + emailReturned; 
				} else {
					retEmailValues = emailReturned;
				}
			}			
			personnelTable.destroy();			
		}
		
		return retEmailValues;
	}

}
