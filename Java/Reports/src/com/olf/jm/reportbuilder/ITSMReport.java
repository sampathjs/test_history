package com.olf.jm.reportbuilder;

/**
 * 
 * Description:
 * This script Provides the ITSM monthly data about the user status and other activities
 * This Script can be run from Task and Report builder as well
 * Revision History:
 * 02.02.22  BhardG01  initial version
 *  
 */
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;


public class ITSMReport implements IScript {


	public static final String CONST_REPO_CONTEXT = "Reports";
	public static final String CONST_REPO_SUBCONTEXT = "ITSMReport";
	/** The const repository used to initialise the logging classes. */ 
	private String startDate;
	private String endDate; 
	private String endDate_Param;
	private String countryIDStr; 
	private boolean isReportBuilderReport = false;
	private boolean isSuportUser = false;
	@Override
	public void execute(IContainerContext context) throws OException {

		init();
		Logging.info("Starting script %s", this.getClass().getSimpleName());
		
		Table returnt = context.getReturnTable();
		Table argt = context.getArgumentsTable(); 
		Table inputParam = Table.tableNew();
		inputParam = argt.getTable("PluginParameters", 1);
		if(inputParam != null ){
			retriveInputDataForReportBuiider(inputParam); 
			isReportBuilderReport = true;
		}else {
			retriveInputDataForTask(argt);
		}

		isSuportUser = isUserTypeSupport();
		   
		try {
			processOutputData(context, returnt);
		} catch (Exception e) {
			throw new OException("Failed to load saved sim results: " + e.getMessage());

		} finally {
			Logging.close();
		}
		

	}

	LocalDate getDateFromString(String string,  DateTimeFormatter format)
		{ 
		LocalDate date = LocalDate.parse(string, format);
 
		return date;
		}
 

	private boolean isUserTypeSupport() throws OException {
		Table tempTable1 = Util.NULL_TABLE;
		tempTable1 = Table.tableNew();
		String sql = "Select * from country where id_number ="+countryIDStr;
		int ret1 = DBaseTable.execISql(tempTable1, sql);
		if (ret1 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			Logging.error(DBUserTable.dbRetrieveErrorInfo(ret1, "Failed to get party portfolio"));
			throw new OException("Failed to get party portfolio");
		}
		return (tempTable1.getNumRows()>0);
		
	}




	private void retriveInputDataForReportBuiider(Table inputParam) throws OException {
		
		String paramName = null;

		for(int i=1;i<=inputParam.getNumRows();i++){
			paramName = inputParam.getString("parameter_name", i);
			Logging.info("Param-Name :== "+paramName );
			if(paramName.equalsIgnoreCase("Country_ID")){
				countryIDStr =inputParam.getString("parameter_value", i);
			}
			else if(paramName.equalsIgnoreCase("End_Date")){
				DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
				endDate_Param =inputParam.getString("parameter_value", i);

				LocalDate date= getDateFromString(inputParam.getString("parameter_value", i), dateFormat) ;
			 	LocalDate endDateParam =date.plusDays(1);
			 	endDate = endDateParam.format( dateFormat).toString();   
			 	    
			}
			else if(paramName.equalsIgnoreCase("Start_Date")){
				 startDate =  inputParam.getString("parameter_value", i);
				
			} 
		}
		
	}

	private void retriveInputDataForTask(Table argt) throws OException {
		endDate = OCalendar.formatJd(argt.getDate("End_Date", 1)+1);
		endDate_Param = OCalendar.formatJd(argt.getDate("End_Date", 1) );
		startDate = OCalendar.formatJd(argt.getDate("Start_Date", 1));
		countryIDStr =argt.getString("Country_ID", 1);
	}




	private void processOutputData(IContainerContext context, Table returnt) throws OException {

		Table mainDataTable = Util.NULL_TABLE;

		Table bussUnitDataRaw = Util.NULL_TABLE;
		Table bussUnitDataProcessded = Util.NULL_TABLE;
		Table licenseTypeDataRaw = Util.NULL_TABLE;
		Table licenseTypeDataProcessded = Util.NULL_TABLE;
		Table securityGroupDataRaw  = Util.NULL_TABLE;
		Table securityGroupDataProcessded  = Util.NULL_TABLE;

		Table userTypeDataRaw  = Util.NULL_TABLE; 
		try {
			

			String strMainDataSQL = getMainDataSQL();
			String strBussSQL = getBussSQL();

			String strLicenceTypeSQL = getLicenceTypeSQL();
			String strSecurityGroupSQL = getSecurityGroupSQL();
			String strUserTypeSQL = getUserTypeSQL();

			mainDataTable = Table.tableNew();
			bussUnitDataRaw = Table.tableNew();
			licenseTypeDataRaw = Table.tableNew();
			licenseTypeDataProcessded = Table.tableNew();
			bussUnitDataProcessded = Table.tableNew();
			securityGroupDataRaw = Table.tableNew();
			securityGroupDataProcessded = Table.tableNew();

			userTypeDataRaw = Table.tableNew(); 
			
			

			Logging.info(  "mainDataTable "+strMainDataSQL);
			int ret = DBaseTable.execISql(mainDataTable, strMainDataSQL);

			Logging.info(  "mainDataTable 2 ");
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret, "Failed to get party portfolio"));
				throw new OException("Failed to get party portfolio");
			}
			 
			Logging.info(  "mainDataTable ");
			

			int ret2 = DBaseTable.execISql(bussUnitDataRaw, strBussSQL);
			if (ret2 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret2, "Failed to get party portfolio"));
				throw new OException("Failed to get party portfolio");
			}

			bussUnitDataProcessded = processCombindBusinessData(bussUnitDataRaw);
		  
			mainDataTable.select(bussUnitDataProcessded, "business_units", "personnel_id EQ $personnel_id");
		 
			
			int ret1 = DBaseTable.execISql(licenseTypeDataRaw, strLicenceTypeSQL);
			if (ret1 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret1, "Failed to get party portfolio"));
				throw new OException("Failed to get party portfolio");
			} 

			licenseTypeDataProcessded = processLicenseTypeData(licenseTypeDataRaw);

			mainDataTable.select(licenseTypeDataProcessded, "license_type", "personnel_id EQ $personnel_id");

			
			int ret3 = DBaseTable.execISql(securityGroupDataRaw, strSecurityGroupSQL);
			if (ret3 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret3, "Failed to get party portfolio"));
				throw new OException("Failed to get party portfolio");
			}

			securityGroupDataProcessded = processSecurityGroup(securityGroupDataRaw);

			mainDataTable.select(securityGroupDataProcessded, "security_groups", "personnel_id EQ $personnel_id");
			int ret4 = DBaseTable.execISql(userTypeDataRaw, strUserTypeSQL);
			if (ret4 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				Logging.error(DBUserTable.dbRetrieveErrorInfo(ret4, "Failed to get party portfolio"));
				throw new OException("Failed to get party portfolio");
			}

			mainDataTable.select(userTypeDataRaw, "user_status, user_type", "personnel_id EQ $personnel_id");

			Logging.info("inside isReportBuilderReport 0");
			if(isReportBuilderReport){	 
				Logging.info("inside isReportBuilderReport 1");

				if (context.getArgumentsTable().getInt("ModeFlag", 1) == 0) {
					getOutTableStructure(returnt);
					returnt.viewTable();
					Logging.info("inside isReportBuilderReport 2");
				} else {
					returnt.select(mainDataTable,   "*", "personnel_id GT 0");
				}
			}else {
				returnt.select(mainDataTable,   "*", "personnel_id GT 0");
				returnt.viewTable();
			}


		} catch (Exception e) {
			throw new OException(e.getMessage());
		} finally { 
		}

	}

	private void getOutTableStructure(Table returnt) throws OException {

		returnt.addCol("personnel_id", COL_TYPE_ENUM.COL_INT);
		returnt.addCol("ADID", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("first_name", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("last_name", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("country_name", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("Repository_Access", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("last_login_date", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("phone", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("fax", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("business_units", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("license_type", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("security_groups", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("user_status", COL_TYPE_ENUM.COL_STRING);
		returnt.addCol("user_type", COL_TYPE_ENUM.COL_STRING);     
}




	private String getUserTypeSQL() {
 
		String sql  = " WITH version_data AS (       Select max(ph.personnel_version) max_personnel_version, ph.id_number   from personnel_history ph"
					+  "  WHERE      ph.last_update < '"+endDate+"'  group by id_number      )   "
					+  "  SELECT   ph.id_number personnel_id, ps.name user_status ,pt.name user_type FROM  personnel p   "
					+  "   JOIN personnel_history ph on (ph.id_number = p.id_number  )   "
					+  "    JOIN personnel_status ps  "
					+  "  ON (  ps.id_number =ph.status ) JOIN personnel_type pt ON (pt.id_number =ph.personnel_type and p.fax <> 'Server User')  "  
					+  "  JOIN  version_data vd on (vd.max_personnel_version = ph.personnel_version AND vd.id_number =ph.id_number)  ";
				
			return sql;
	}
	
	private String getSecurityGroupSQL() {
		String sql = " WITH version_data AS ( "+						   
				     " Select max(ugh.version_number) max_personnel_version, ugh.user_number   from users_to_groups_history ugh "+								 
				     " WHERE      ugh.last_update < '"+endDate+"' group by ugh.user_number   )    "+
				     " SELECT  distinct ugh.user_number personnel_id ,g.name   security_groups "+
				     " FROM  users_to_groups_history ugh   "+
				     " JOIN  groups g  ON (g.id_number =ugh.group_number ) "+
				     " JOIN  version_data vd on (vd.max_personnel_version = ugh.version_number and ugh.user_number =vd.user_number    )  ";
						  
						      
			return sql;
	}
	

	private String getLicenceTypeSQL() {
		String sql = " WITH version_data AS ( "+						   
			     " Select max(ph.personnel_version) max_personnel_version,ph.id_number personnel_id, ph.personnel_type  from personnel_history  ph "+								 
			     " JOIN pers_license_types_link_h plt_h on (plt_h.personnel_id = ph.id_number AND plt_h.personnel_version = ph.personnel_version)    "+
			     " WHERE ph.last_update < '"+endDate+"' group by ph.id_number, ph.personnel_type   )    "+			     
   				 " SELECT  distinct a.personnel_id personnel_id, plt.type_name "+
				  " FROM   pers_license_types_link_h a   "+
				  " Join   personnel_license_type plt ON(a.license_type = plt.type_id) "+
				  " JOIN   personnel p1 ON(p1.id_number = a.personnel_id)"+
				  " JOIN   version_data vd ON(a.personnel_id = vd.personnel_id AND  vd.max_personnel_version =a.personnel_version )";
			 
		return sql;
		 
	}

	private Table processSecurityGroup(Table securityGroupData) throws OException {
		Table firstDataTable = getPersonalBaseData(securityGroupData); 
		Table tempTable = Util.NULL_TABLE;
		tempTable = Table.tableNew();
		int rowCount = firstDataTable.getNumRows();
		firstDataTable.addCol("security_groups", COL_TYPE_ENUM.COL_STRING);
		int personnel_id =0;
		for(int row=1;row<=rowCount;row++){
			 personnel_id = firstDataTable.getInt("personnel_id", row);
			 tempTable.select(securityGroupData, "*", "personnel_id EQ "+personnel_id);
			 String tmp1 = getAssociatedData("security_groups", tempTable);
			 firstDataTable.setString("security_groups", row,tmp1);
			 tempTable.clearDataRows();
		 } 
		tempTable.destroy();
		return firstDataTable;
	}

	private Table processLicenseTypeData(Table licenseTypeDataRaw) throws OException {
		Table firstDataTable = getPersonalBaseData(licenseTypeDataRaw);
		Table tempTable = Util.NULL_TABLE;
		tempTable = Table.tableNew();
		Table tempTable1 = Util.NULL_TABLE;
		tempTable1 = Table.tableNew();
		int rowCount = firstDataTable.getNumRows();
		firstDataTable.addCol("license_type", COL_TYPE_ENUM.COL_STRING);
		int personnel_id =0;
		for(int row=1;row<=rowCount;row++){
			 personnel_id = firstDataTable.getInt("personnel_id", row);
			 tempTable.select(licenseTypeDataRaw, "*", "personnel_id EQ "+personnel_id);
			 String tmp1 = getAssociatedData("type_name", tempTable);
			 firstDataTable.setString("license_type", row,tmp1);
			 tempTable.clearDataRows();
		 }
		String additionalSQL = getAdditionalSQL(); 

		int ret1 = DBaseTable.execISql(tempTable1, additionalSQL);
		if (ret1 != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
			Logging.error(DBUserTable.dbRetrieveErrorInfo(ret1, "Failed to get party portfolio"));
			throw new OException("Failed to get party portfolio");
		}

		tempTable1.destroy();
		tempTable.destroy();
		return firstDataTable;
	}

	private String getAdditionalSQL() {
	 String sql = " Select distinct p1.id_number personnel_id,'' license_type"+
			 	  " FROM personnel p1 WHERE p1.id_number not in ("+
			      " SELECT a.personnel_id FROM   pers_license_types_link a  )";
	 return sql;
	}

	private Table getPersonalBaseData(Table bussUnitDataRaw) throws OException {

		Table firstDataTable = Util.NULL_TABLE;
		firstDataTable = Table.tableNew(); 
		firstDataTable.select(bussUnitDataRaw, "DISTINCT, personnel_id", "personnel_id GT 0");		
		return firstDataTable;
	}
 
	private Table processCombindBusinessData(Table bussUnitDataRaw) throws OException {
		Table firstDataTable = getPersonalBaseData(bussUnitDataRaw);
		Table tempTable = Util.NULL_TABLE;
		  tempTable = Table.tableNew();
		 int rowCount = firstDataTable.getNumRows();
		 firstDataTable.addCol("business_units", COL_TYPE_ENUM.COL_STRING);
		 int personnel_id =0;
		 for(int row=1;row<=rowCount;row++){
			 personnel_id = firstDataTable.getInt("personnel_id", row);
			 tempTable.select(bussUnitDataRaw, "*", "personnel_id EQ "+personnel_id);
			 String tmp1 = getAssociatedData("short_name", tempTable);
			 
			 firstDataTable.setString("business_units", row,tmp1);
			 tempTable.clearDataRows();
		 }
		return firstDataTable;
	}

	private String getAssociatedData( String colValue, Table tempTable) throws OException {

		 int rowCount = tempTable.getNumRows();
		 String short_name="", tmpVal="";
		 for(int row=1;row<=rowCount;row++){ 
			 short_name = tempTable.getString(colValue, row);
			 if(row ==1){
				 tmpVal = short_name; 
			 }else{
				 tmpVal = tmpVal+","+short_name;
			 }
			 
		 }
		return tmpVal;
	}

	private String getBussSQL() {
		String sql =  "  WITH version_data AS (    "+
				"   Select distinct max(ph.version_number) max_personnel_version, ph.party_id, ph.personnel_id   from party_personnel_history ph WHERE   "+
				//"  ph.last_update > '"+start_date+"'  AND  ph.last_update <='"+end_date+"'"+
				"   ph.last_update <'"+endDate+"'"+
					" AND link_status=1  group by   ph.personnel_id,ph.party_id    "+
				"   )  SELECT DISTINCT  a.personnel_id    ,prt.short_name  "+
				 "FROM   party_personnel_history a     "+
				 "JOIN   version_data vd  on (vd. max_personnel_version = a.version_number  and vd.party_id =a.party_id and  a.personnel_id  = vd.personnel_id )        "+
				 "JOIN   party prt on (  prt.party_id = a.party_id and  prt.party_class = 1 )     "+ 
				 " group by  a.personnel_id,prt.short_name ";
		return sql;
	}

	 

	private String getMainDataSQL() {
		
		String sql = "  WITH version_data AS (    "+
				"   Select max(ph.personnel_version) max_personnel_version, ph.id_number   from personnel_history ph WHERE   "+
			    "  ph.last_update > '"+startDate+"'  AND "+
				"  ph.last_update <'"+endDate+"'"+
				"   group by id_number    "+
				"   ) ,Recent_Authorized_User_data AS (    "+
				"  select   p.id_number,ps.name user_status ,pt.name user_type   "+
				"   from  personnel p   "+
				"   JOIN personnel_status ps ON ( ps.id_number =p.status)   "+
				"  JOIN personnel_type pt ON (pt.id_number =p.personnel_type and p.fax <> 'Server User')  "+
				"  JOIN  personnel_history ph ON(ph.last_update >='"+endDate_Param+"' AND ph.id_number =p.id_number and ph.personnel_version =1)   "+
				"    )   ,   Recent_UnAuthorized_User_data AS (    "+
				"  select   p.id_number,ps.name user_status ,pt.name user_type      from  personnel p      JOIN personnel_status ps ON (    "+
				"   ps.id_number =p.status)     JOIN personnel_type pt ON (pt.id_number =p.personnel_type and p.fax <> 'Server User')   "+
				"   JOIN  personnel_history ph ON(ph.last_update >='"+endDate_Param+"' AND ph.id_number =p.id_number and ph.personnel_version <>1)   "+
				"  ) ,  version_data_outside AS ( Select max(ph.personnel_version) max_personnel_version, ph.id_number   from personnel_history ph WHERE   "+
				"  ph.last_update <='"+startDate+"'   "+
				"  AND  ph.id_number not in ( Select vd.id_number from version_data vd )   "+
				"  AND  ph.id_number   in ( Select vd.id_number from Recent_UnAuthorized_User_data vd ) group by id_number    "+ 
				"   )   ,    Authorized_User_data as (   "+
				"  select   p.id_number,ps.name user_status ,pt.name user_type "+ 
				"    from  personnel p   "+
				"   JOIN personnel_status ps ON (ps.id_number =1 AND ps.id_number =p.status AND    p.personnel_type =2 and p.status =1 )   "+
				"   JOIN personnel_type pt ON (pt.id_number =p.personnel_type and p.fax <> 'Server User')  "+
				"   where p.id_number not in (Select   id_number from Recent_Authorized_User_data )  "+
				"  )  ,    Recent_Updated_User_Data as (   "+
				"   Select   p.id_number,  ps.name user_status ,pt.name user_type "+
				"   FROM  personnel p   "+
//				"   JOIN personnel_status ps ON (ps.id_number <>1 AND ps.id_number =p.status AND    p.personnel_type <>2 and p.status <>1 )   "+
				"   JOIN personnel_status ps ON (  ps.id_number =p.status )   "+
				"   JOIN personnel_type pt ON (pt.id_number =p.personnel_type and p.fax <> 'Server User')    "+
				"   JOIN version_data vd on ( vd.id_number = p.id_number )    "+				  
				"   ) , Recent_UnAuthorized_Outside_User_Data AS (  "+
				"      Select   p.id_number,  ps.name user_status ,pt.name user_type    FROM  personnel p "+
				"       JOIN personnel_status ps ON (  ps.id_number =p.status )      JOIN personnel_type pt ON (pt.id_number =p.personnel_type and    "+
				"  		 p.fax <> 'Server User')        JOIN version_data_outside vd on ( vd.id_number = p.id_number )  "+
				"  ) ,	  session_data AS (  "+
				"     SELECT max(si.end_time) last_login_date,p.id_number personnel_id  "+
				"         FROM    personnel p    "+
				"  		Join sysaudit_activity_log si ON (si.personnel_id =p.id_number and si.action_id =15  )  "+
				"  		GROUP BY p.id_number  "+
				"  ) ,  combind_personal_data AS (  "+
				"     SELECT aud.id_number,aud.user_type,aud.user_status  "+
				"         FROM    Authorized_User_data aud    "+
				"  	    Union   "+
				"  	SELECT rud.id_number,rud.user_type,rud.user_status  "+
				"     FROM    Recent_Updated_User_Data rud    "+				      
				"  	    Union   "+
				"  	SELECT ruad.id_number,ruad.user_type,ruad.user_status    "+    
				"     FROM    Recent_UnAuthorized_Outside_User_Data ruad    )"+				      
				"  SELECT p.name ADID,p.id_number personnel_id,	p.first_name  ,	p.last_name,   "+
			    " cntr.name country_name,   "+
				"  CASE WHEN (     cntr.name='Support' ) THEN  'Yes' ELSE 'No' END as Repository_Access ,"+
				"  CASE WHEN (     si.last_login_date  IS NULL ) THEN 'User not logged in last Six Months' ELSE CONVERT(VARCHAR(30),  CONVERT(DATE, si.last_login_date),106) END as last_login_date ,p.phone , p.fax   "+
				"   FROM personnel p   "+
				"   JOIN personnel_type pt ON (pt.id_number =p.personnel_type AND pt.id_number >0)   "+
				"    JOIN combind_personal_data cpd  ON (cpd.id_number = p.id_number)    "+
				"  JOIN personnel_status ps ON (ps.id_number =p.status)  "+		   
				"    JOIN country cntr ON (cntr.id_number =p.country )    "+
				"  LEFT OUTER JOIN  session_data si ON(si.personnel_id = p.id_number)  "+
				"  WHERE p.id_number NOT IN (23891, 22840, 23291 )";
			if(isSuportUser){
				sql = sql+" AND cntr.id_number in ("+countryIDStr+") "; 
			}else{
				countryIDStr = "20250";
				sql = sql+" AND cntr.id_number not in ("+countryIDStr+") ";
			}
				
			 
		return sql;
	}

	private void init() throws OException {
		try {
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info("********************* Start of new run ***************************");
		 
	}
}
