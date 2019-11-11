/* Released with version 27-Feb-2019_V17_0_7 of APM */

package com.olf.result.APM_PhysVolumes;

import java.util.Vector;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Instrument;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_FORMAT_BASE_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DBTYPE_ENUM;
import com.olf.openjvs.enums.IDX_UNIT_TYPE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.DEAL_VOLUME_TYPE;
import com.olf.openjvs.enums.VOLUME_TYPE;
import com.olf.result.APMUtility.APMUtility;
import com.olf.openjvs.DBase;

import java.lang.Math;

import java.lang.reflect.*;

public class APM_PhysVolumesUtilNAGas {
	
	static enum APM_ConversionStyle {
		DELIVERY_BASED_CONVERSION,
		BALANCE_LEG_CONVERSION
	}

	static String[] doubleValCols = { "total_quantity", "bav_quantity",
			"mass_massunit", "volume_volumeunit", "bav_mass_massunit",
			"bav_volume_volumeunit", "energy", "bav_energy" };

	//These columns must be in sets of TWO
	static String[] quantityColumns = { "total_quantity", "bav_quantity" };
	static String[] energyColumns = { "energy", "bav_energy" };
	static String[] volumeColumns = { "volume_volumeunit",
			"bav_volume_volumeunit" };
	static String[] massColumns = { "mass_massunit", "bav_mass_massunit" };

	// Generate a query for all transactions of applicable instrument types
	// Blank list is considered to be "all ins types are acceptable"
	public static int generateQuery(Table argt, Vector<Integer> baseInsTypes)
			throws OException {
		int myBaseInsType;
		int iQueryID = 0;

		int iResult = argt.getInt("result_type", 1);
		Table tblTrans = argt.getTable("transactions", 1);
		Table validTrans = tblTrans.cloneTable();

		for (int i = 1; i <= tblTrans.getNumRows(); i++) {
			Transaction tranCurrent = tblTrans.getTran("tran_ptr", i);

			if (SimResult.isResultAllowedForTran(iResult, tranCurrent) > 0) {
				myBaseInsType = Instrument.getBaseInsType(tranCurrent
						.getInsType());

				if ((baseInsTypes.size() == 0)
						|| (baseInsTypes.contains(myBaseInsType))) {
					tblTrans.copyRowAdd(i, validTrans);
				}
			}
		}

		if (validTrans.getNumRows() > 0) {
			iQueryID = Query.tableQueryInsert(validTrans, "tran_num");
		}

		return iQueryID;
	}

	public static Table doCalculations(int iQueryID, int massUnit,
			int volumeUnit, int energyUnit, Table tblTrans, boolean flipSigns,
			int calcAllVolumeStatuses, APM_ConversionStyle convStyle,
			int startPoint, int endPoint, String volumeTypesStr)
			throws OException {
		Table dataTable = retrieveData(iQueryID, massUnit, volumeUnit,
				flipSigns, calcAllVolumeStatuses, startPoint, endPoint, volumeTypesStr);
		int iNumRows = dataTable.getNumRows();
		boolean conversionHandled = false;
		int iInsTypeCol = dataTable.getColNum("ins_type");
		int iBuySellCol = dataTable.getColNum("buy_sell");
		int iPayRecCol = dataTable.getColNum("pay_rec");
		int iInjWthCol = dataTable.getColNum("inj_wth");
		int iQuantityCol = dataTable.getColNum("quantity");
		int iParcelIDCol = dataTable.getColNum("parcel_id");

		int iSDTCol = dataTable.getColNum("gmt_start_date_time");
		int iEDTCol = dataTable.getColNum("gmt_end_date_time");

		int iSDCol = dataTable.getColNum("startdate");
		int iEDCol = dataTable.getColNum("enddate");
		int iSTCol = dataTable.getColNum("start_time");
		int iETCol = dataTable.getColNum("end_time");

		int iTotalHoursCol = dataTable.getColNum("total_hours");

		int priorTranNum = -1;
		int balanceLegID = -1;
		Method[] tranMethods = Transaction.class.getMethods();

		// Check if I have a method to do parcel-based conversions in this cut
		Method parcelBasedConversion = null;
		for (int i = 0; i < tranMethods.length; i++) {
			if (tranMethods[i].getName().equals(
					"getUnitConversionFactorByParcelId")) {
				parcelBasedConversion = tranMethods[i];
				break;
			}
		}

		// Check if I have a method to do delivery-based conversions in this cut
		Method deliveryBasedConversion = null;
		for (int i = 0; i < tranMethods.length; i++) {
			if (tranMethods[i].getName().equals(
					"getUnitConversionFactorByDeliveryId")) {
				deliveryBasedConversion = tranMethods[i];
				break;
			}
		}

		// Check if I have a method to do profile-based conversions in this cut
		Method profileBasedConversion = null;
		for (int i = 0; i < tranMethods.length; i++) {
			if (tranMethods[i].getName().equals(
					"getUnitConversionFactorByProfile")) {
				profileBasedConversion = tranMethods[i];
				break;
			}
		}

		// Now do the necessary post-processing
		for (int iRow = 1; iRow <= iNumRows; iRow++) {
			boolean setInjWth = false;
			int iInsType = Instrument.getBaseInsType(dataTable.getInt(
					iInsTypeCol, iRow));
			int iBuySell = dataTable.getInt(iBuySellCol, iRow);
			int iPayRec = dataTable.getInt(iPayRecCol, iRow);

			// Fix up the signage by pay/receive flag
			if (iPayRec == 1) {
				double quantity = dataTable.getDouble(iQuantityCol, iRow);
				quantity = -(quantity);
				dataTable.setDouble(iQuantityCol, iRow, quantity);
			}

			// Fix up Injection/Withdrawal same way as "Gas Info" script
			// This would set incorrect values on financial legs, but this UDSR
			// only gives info on physical amounts
			setInjWth = ((iInsType == INS_TYPE_ENUM.comm_storage.toInt()) || (iInsType == INS_TYPE_ENUM.comm_transit
					.toInt()));

			if (setInjWth && (iBuySell != iPayRec)) {
				dataTable.setInt(iInjWthCol, iRow, 2);
			} else if (setInjWth && (iBuySell == iPayRec)) {
				dataTable.setInt(iInjWthCol, iRow, 1);
			}

			// Fix up the dates by converting to a date+time combo
			int iStartDate = dataTable.getDate(iSDTCol, iRow);
			int iStartTime = dataTable.getTime(iSDTCol, iRow);

			dataTable.setInt(iSDCol, iRow, iStartDate);
			dataTable.setInt(iSTCol, iRow, iStartTime);

			// For end time, we want to change 32767:0 to 32766:86400 for better
			// bucketing on client

			int iEndDate = dataTable.getDate(iEDTCol, iRow);
			int iEndTime = dataTable.getTime(iEDTCol, iRow);

			// Now, calculate the difference between start and end times in
			// hours
			double dHours = (iEndDate - iStartDate) * 24
					+ (iEndTime - iStartTime) / 3600;

			dataTable.setDouble(iTotalHoursCol, iRow, dHours);

			if (iEndTime == 0) {
				iEndDate--;
				iEndTime = 86400;
			}

			dataTable.setInt(iEDCol, iRow, iEndDate);
			dataTable.setInt(iETCol, iRow, iEndTime);
		}

		dataTable.mathMultCol("quantity", "total_hours", "total_quantity");
		dataTable.mathMultCol("total_quantity", "bav_flag", "bav_quantity");
		dataTable.mathMultCol("quantity", "total_hours", "energy");
		dataTable.mathMultCol("total_quantity", "bav_flag", "bav_energy");

		dataTable.setColValInt("mass_unit", massUnit);
		dataTable.setColValInt("volume_unit", volumeUnit);
		dataTable.setColValInt("energy_unit", energyUnit);
		dataTable.setColValInt("to_be_deleted", 0);
		
		String abColName = "tran_num";
		int dataTableTranNumCol = dataTable.getColNum ("tran_num");
		int tranTableTranNumCol = tblTrans.getColNum ("tran_num");
		
		Table duomConversionTable = APMUtility.generateDuomConversionTable (dataTable, dataTableTranNumCol, tblTrans, tranTableTranNumCol, abColName);

			// Now do delivery-level conversions
		for (int iRow = 1; iRow <= iNumRows; iRow++) {
			int tranNum = dataTable.getInt("tran_num", iRow);
			int tranRow = tblTrans.unsortedFindInt("tran_num", tranNum);

			int dealLeg = dataTable.getInt("param_seq_num", iRow);
			int dealProfile = dataTable.getInt("profile_seq_num", iRow);
			int deliveryId = dataTable.getInt("delivery_id", iRow);
			int origUnit = dataTable.getInt("unit", iRow);
			int iParcelID = dataTable.getInt(iParcelIDCol, iRow);
			int iInsType = Instrument.getBaseInsType(dataTable.getInt(iInsTypeCol, iRow));
			int startDate = dataTable.getInt("startdate", iRow);
			int endDate = dataTable.getInt ("enddate", iRow);
			int volumeType = dataTable.getInt ("volume_type", iRow);			
			Transaction tranCurrent = tblTrans.getTran("tran_ptr", tranRow);
			int locationId = dataTable.getInt("location_id", iRow);
			double volume = dataTable.getDouble("total_quantity", iRow);
			double bavVolume = dataTable.getDouble("bav_quantity", iRow);

			double convFactorMass = 0.0, convFactorVolume = 0.0, convFactorEnergy = 0.0;

			if (convStyle == APM_ConversionStyle.DELIVERY_BASED_CONVERSION) {
				boolean bConvertedByParcel = false;

				// If parcel-based conversion is fails, or unavailable, fall
				// back to the old approach
				if (!bConvertedByParcel) {
			 		convFactorMass = APMUtility.getConversionFactor(tranCurrent, dealLeg, dealProfile, deliveryId, volumeType, startDate, endDate, locationId, origUnit, massUnit, duomConversionTable, tranNum, abColName);
					convFactorVolume = APMUtility.getConversionFactor(tranCurrent, dealLeg, dealProfile, deliveryId, volumeType, startDate, endDate, locationId, origUnit, volumeUnit, duomConversionTable, tranNum, abColName);
					convFactorEnergy = APMUtility.getConversionFactor(tranCurrent, dealLeg, dealProfile, deliveryId, volumeType, startDate, endDate, locationId, origUnit, energyUnit, duomConversionTable, tranNum, abColName);
					conversionHandled = true;
				}
			} else if (convStyle == APM_ConversionStyle.BALANCE_LEG_CONVERSION) {
				if (tranCurrent.getTranNum() != priorTranNum) {
					priorTranNum = tranCurrent.getTranNum();
					for (int i = 0; i < tranCurrent.getNumParams(); i++) {
						// tranCurrent.getField(TRANF_FIELD. );
					}
					// balanceLegID =

					// OK, here we'd need to have a way to retrieve the balance
					// leg,
					// and then have a way to retrieve the balance leg's profile
					// level
					// conversion - the required OpenJVS calls do not exist yet
				}
			}

			dataTable.setDouble("mass_massunit", iRow, volume * convFactorMass);
			dataTable.setDouble("volume_volumeunit", iRow, volume
					* convFactorVolume);
			dataTable.setDouble("energy", iRow, volume * convFactorEnergy);

			dataTable.setDouble("bav_mass_massunit", iRow, bavVolume
					* convFactorMass);
			dataTable.setDouble("bav_volume_volumeunit", iRow, bavVolume
					* convFactorVolume);
			dataTable.setDouble("bav_energy", iRow, bavVolume
					* convFactorEnergy);
		}

		// dataTable.viewTable();
		if (!conversionHandled)
			performDetailedEnergyConversion(dataTable);
		return dataTable;
	}

	public static Table retrieveData(int iQueryID, int massUnit,
			int volumeUnit, boolean flipSigns, int calcAllVolumeStatuses,
			int startPoint, int endPoint, String volumeTypesStr)
			throws OException {
		Table dataTable = Table.tableNew();
		Table stdProductsTable = Table.tableNew();
		String sQuery;
		String stdProductsQuery;
		String sVolumeStatusCheck = "";
		String sVolumeTypes = "";
		String strDbStartingPeriodStrComm = "";
		String strDbStoppingPeriodStrComm = "";		
		String strDbStartingPeriodStrProducts = "";
		String strDbStoppingPeriodStrProducts = "";		
		ODateTime dtStartingPeriod, dtStoppingPeriod;     
		
		if ( startPoint != 0 )
		{
			dtStartingPeriod = ODateTime.dtNew();
			dtStartingPeriod.setDateTime(startPoint, 0);
			String strStartingPeriod = dtStartingPeriod.formatForDbAccess();
			strDbStartingPeriodStrComm = "    '" + strStartingPeriod + "' < comm_schedule_detail.gmt_end_date_time and ";
			strDbStartingPeriodStrProducts = "    '" + strStartingPeriod + "' < profile.end_date and ";
		}
		
		if ( endPoint != 0 )
		{
			dtStoppingPeriod = ODateTime.dtNew();
			dtStoppingPeriod.setDateTime(endPoint, 0);
			String strStoppingPeriod = dtStoppingPeriod.formatForDbAccess();
			strDbStoppingPeriodStrComm = "    '" + strStoppingPeriod + "' > comm_schedule_detail.gmt_start_date_time and "; 
			strDbStoppingPeriodStrProducts =  "    '" + strStoppingPeriod + "' > profile.start_date and ";
		}
		
		if (calcAllVolumeStatuses == 0) {
			sVolumeStatusCheck = " comm_schedule_detail.bav_flag = 1 and ";
		}
		else
			sVolumeTypes = GetShowAllVolumeTypesQueryString(volumeTypesStr);
			

		// Note that I am returning data in order that I want the final table to
		// be in, and pre-selecting additional
		// columns here, which will be overwritten - this is so this table can
		// be edited in-situ, rather than
		// having another expensive Table Select later

		sQuery = " select "
				+ "    ab_tran.deal_tracking_num deal_num, "
				+ "    ab_tran.tran_num, "
				+ "    ab_tran.ins_num, "
				+ "    comm_schedule_header.param_seq_num, "
				+ "    comm_schedule_header.param_seq_num param_seq_num_1, " // Needed
																				// for
																				// mapping
																				// param_seq_num's
																				// in
																				// the
																				// PhysInventory
				+ "    comm_schedule_header.profile_seq_num, "
				+ "    comm_schedule_header.schedule_id, "
				+ "    comm_schedule_header.delivery_id, "
				+ "    comm_schedule_delivery.delivery_status delivery_status, "
				+ "    comm_schedule_detail.quantity total_quantity, "
				+ "    comm_schedule_detail.quantity bav_quantity, "
				+ "    comm_schedule_detail.gmt_start_date_time, "
				+ "    comm_schedule_detail.gmt_end_date_time, "
				+ "    comm_schedule_detail.quantity mass_massunit, "
				+ "    comm_schedule_detail.quantity volume_volumeunit, "
				+ "    comm_schedule_detail.quantity bav_mass_massunit, "
				+ "    comm_schedule_detail.quantity bav_volume_volumeunit, "
				+ "	  comm_schedule_detail.quantity energy,"// Will be used for
															// energy
															// conversion.
				+ "	  comm_schedule_detail.quantity bav_energy,"// Will be used
																// for energy
																// conversion.
				+ "    comm_schedule_header.unit, "
				+ "    comm_schedule_header.unit mass_unit, "
				+ "    comm_schedule_header.unit volume_unit, "
				+ "    comm_schedule_header.unit energy_unit, "// Will be used
																// for energy
																// conversion.
				+ "    comm_schedule_header.unit to_be_deleted, "// Will be used
																	// for
																	// energy
																	// conversion.
				+ "    ins_parameter.deal_volume_type,"
				+ "    parameter.pay_rec startdate, "
				+ "    parameter.pay_rec enddate, "
				+ "    parameter.pay_rec start_time, "
				+ "    parameter.pay_rec end_time, "
				+ "    phys_header.service_type, "
				+ "    comm_schedule_detail.quantity, "
				+ "    comm_schedule_detail.quantity total_hours, "
				+ "    comm_schedule_header.volume_type, "
				+ "    comm_schedule_detail.bav_flag, "
				+ "    comm_schedule_header.location_id, "
				+ "    gas_phys_location.zone_id, "
				+ "    gas_phys_location.pipeline_id, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.vpool_id, "
				+ "    gas_phys_location.loc_long_name, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.idx_subgroup, "
				+ "    gas_phys_location.location_type, "
				+ "    gas_phys_param.measure_group_id, "
				+ "    gas_phys_param.tank_id, "
				+ "    gas_phys_param.deal_start_time, "
				+ "    gas_phys_param.time_zone, "
				+ "    gas_phys_param.location_id leg_location, "
				+ getParcelSource()
				+ "    parameter.proj_index, "
				+ "    ab_tran.buy_sell, "
				+ "    ab_tran.ins_type, "
				+ "    parameter.pay_rec, "
				+ "    0 inj_wth, "
				+ "    comm_schedule_header.int_strategy_id int_str, "
				+ "	  comm_schedule_header.ext_strategy_id ext_str, "
				+ "    phys_header.service_type gas_service_type, "
				+ "    phys_header.allow_pathing, "
				+ "    gas_phys_location.index_id, "
				+ "	   ab_tran.position position, "
				+ "	   gas_phys_pipelines.day_start_time gpp_day_start_time "
				+ " from "
				+ "	   gas_phys_pipelines, "
				+ "    comm_schedule_detail, "
				+ "    gas_phys_location, "
				+ "    gas_phys_param, "
				+ "    phys_header, "
				+ "    ab_tran, "
				+ "    parameter, "
				+ "	  ins_parameter, "
				+ "    query_result, "
				+ "    comm_schedule_header "
				+ "    left outer join comm_schedule_delivery on comm_schedule_header.delivery_id = comm_schedule_delivery.delivery_id "
				+ "where "
				+ "    comm_schedule_header.schedule_id = comm_schedule_detail.schedule_id and "
				+ "    gas_phys_location.location_id = comm_schedule_header.location_id and "
				+ "    comm_schedule_header.ins_num = phys_header.ins_num and "
				+ "    ab_tran.ins_num = comm_schedule_header.ins_num and "
				+ "    parameter.ins_num = comm_schedule_header.ins_num and "
				+ "    parameter.param_seq_num = comm_schedule_header.param_seq_num and "
				+ "    ab_tran.ins_num=ins_parameter.ins_num and parameter.param_seq_num=ins_parameter.param_seq_num and  "
				+ "    parameter.ins_num = gas_phys_param.ins_num and parameter.param_seq_num = gas_phys_param.param_seq_num and "
				//+ "    comm_schedule_header.total_quantity != 0.0 and " // Don't
																		// want
																		// zero-valued
																		// entries
																		// cluttering
																		// the
																		// result
				+ sVolumeStatusCheck // If looking for BAV only, add here
				+ sVolumeTypes // if only a subset of the volume types add here
				//+ "	  ab_tran.toolset != 52 and "//limit physvolumes to commodity toolset for now
				+ "	  ab_tran.toolset = 36 and "//limit physvolumes to commodity toolset for now
				+ "    query_result.query_result = ab_tran.tran_num and "
				+ "	gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id and "
                + strDbStartingPeriodStrComm // starting period
                + strDbStoppingPeriodStrComm // stopping period
				+ "    query_result.unique_id = "
				+ iQueryID
				+ " order by "
				+ "    ab_tran.deal_tracking_num, comm_schedule_detail.gmt_start_date_time";

		com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);

		stdProductsQuery = " select "
				+ "	ab_tran.deal_tracking_num deal_num, "
				+ "	ab_tran.tran_num, "
				+ "	ab_tran.ins_num, "
				+ "	profile.param_seq_num, "
				+ "	profile.param_seq_num param_seq_num_1, "
				+ "	profile.profile_seq_num, "
				+ " profile.profile_seq_num schedule_id, "
				+ "  0 delivery_id, "
				+ "  0 delivery_status, "
				+ "	profile.notnl total_quantity, "
				+ "	profile.notnl bav_quantity, "
				+ "	profile.start_date gmt_start_date_time, "
				+ "	profile.end_date gmt_end_date_time, "
				+ "	profile.notnl mass_massunit, "
				+ "	profile.notnl  volume_volumeunit, "
				+ "	profile.notnl bav_mass_massunit, "
				+ "	profile.notnl  bav_volume_volumeunit, "
				+ "	profile.notnl energy, "
				+ "	profile.notnl bav_energy, "
				+ "	ins_parameter.unit, "
				+ "	ins_parameter.unit mass_unit, "
				+ "	ins_parameter.unit volume_unit, "
				+ "	ins_parameter.unit energy_unit, "
				+ "	ins_parameter.unit to_be_deleted, "
				+ "	ins_parameter.deal_volume_type, "
				+ "	ab_tran.buy_sell startdate, "
				+ "	ab_tran.buy_sell enddate, "
				+ "	ab_tran.buy_sell start_time, "
				+ "	ab_tran.buy_sell end_time, "
				+ "	1 service_type, "
				+ "	profile.notnl quantity, "
				+ "	profile.notnl total_hours, "
				+ "	2 volume_type, "
				+ "	1 bav_flag, "
				+ "	gas_phys_param.location_id, "
				+ "	gas_phys_location.zone_id, "
				+ "	gas_phys_location.pipeline_id, "
				+ "	gas_phys_location.meter_id, "
				+ "	gas_phys_location.vpool_id, "
				+ "	gas_phys_location.loc_long_name, "
				+ "	gas_phys_location.meter_id, "
				+ "	gas_phys_location.idx_subgroup, "
				+ "	gas_phys_location.location_type, "
				+ "	gas_phys_param.measure_group_id, "
				+ "	gas_phys_param.tank_id, "
				+ "	gas_phys_param.deal_start_time, "
				+ "	gas_phys_param.time_zone, "
				+ "	gas_phys_param.location_id leg_location, "
				+ "	0 parcel_id, "
				+ "	parameter.proj_index, "
				+ "	ab_tran.buy_sell, "
				+ "	ab_tran.ins_type, "
				+ "	ab_tran.buy_sell pay_rec, "
				+ "	0 inj_wth, "
				+ "	ab_tran.int_trading_strategy int_str, "
				+ "	ab_tran.ext_trading_strategy ext_str, "
				+ "	0 gas_service_type, "
				+ " 0 allow_pathing, "
				+ " gas_phys_location.index_id, "
				+ "	ab_tran.position position, "
				+ "	gas_phys_pipelines.day_start_time gpp_day_start_time "
				+ "	from "
				+ "	gas_phys_location, "
				+ "	gas_phys_param, "
				+ "	ab_tran, "
				+ "	parameter, "
				+ "	ins_parameter, "
				+ "	profile, "
				+ "	gas_phys_pipelines, "
				+ "	query_result "
				+ "	where "
				+ "	ab_tran.ins_num=ins_parameter.ins_num and "
				+ "	ab_tran.ins_num = profile.ins_num and "
				+ "	ab_tran.ins_num=parameter.ins_num and "
				+ "	parameter.ins_num = gas_phys_param.ins_num and "
				+ "	parameter.param_seq_num=ins_parameter.param_seq_num and "
				+ "	parameter.param_seq_num = gas_phys_param.param_seq_num and "
				+ "	gas_phys_location.location_id = gas_phys_param.location_id and "
				+ "	gas_phys_pipelines.pipeline_id = gas_phys_location.pipeline_id and "
				+ "	ab_tran.toolset = 52 and " 
				+ "	ab_tran.tran_type = 0 and "
				+ " profile.param_seq_num = 0 and "
				+ "  query_result.query_result = ab_tran.tran_num and "
                + strDbStartingPeriodStrProducts // starting period
                + strDbStoppingPeriodStrProducts // stopping period
				+ "  query_result.unique_id = " + iQueryID + "	order by "
				+ "	ab_tran.deal_tracking_num";

		com.olf.openjvs.DBase.runSqlFillTable(stdProductsQuery,
				stdProductsTable);

		stdProductsFixQuantity(stdProductsTable);

		stdProductsTable.copyRowAddAll(dataTable);

		// For reasons obscure and archaic, transit and storage deals store
		// their volumes
		// with the reverse sign of what is inside. Sometimes this is good (when
		// the overall
		// volume is taken for a location, for a COMM-PHYS buy and a loading
		// into TRANSIT,
		// having them cancel each other out means that no volume remains at
		// location).
		// Sometimes, that is bad (when want to know how much cargo is on a
		// boat).
		// So, flipSigns parameter exists.
		if (flipSigns) {
			dataTable.mathMultColConst("quantity", -1.0, "quantity");
		}

		return dataTable;
	}

	public static Table stdProductsFixQuantity(Table stdProductsTable)
			throws OException {
		int stdProductsNumRows, iRow;
		int iGmtStartDateTimeCol = stdProductsTable
				.getColNum("gmt_start_date_time");
		int iGmtEndDateTimeCol = stdProductsTable
				.getColNum("gmt_end_date_time");
		int iTimeZoneCol = stdProductsTable.getColNum("time_zone");
		int iTotalQuantityCol = stdProductsTable.getColNum("total_quantity");
		int iDealVolumeTypeCol = stdProductsTable.getColNum("deal_volume_type");
		int iMassMassUnitCol = stdProductsTable.getColNum("mass_massunit");
		int iVolumeVolumeUnitCol = stdProductsTable
				.getColNum("volume_volumeunit");
		int iBavMassMassUnitCol = stdProductsTable
				.getColNum("bav_mass_massunit");
		int iBavVolumeVolumeUnitCol = stdProductsTable
				.getColNum("bav_volume_volumeunit");
		int iEnergyCol = stdProductsTable.getColNum("energy");
		int iBavEnergyCol = stdProductsTable.getColNum("bav_energy");
		int iQuantityCol = stdProductsTable.getColNum("quantity");
		int iTotalHoursCol = stdProductsTable.getColNum("total_hours");
		int iPositionCol = stdProductsTable.getColNum("position");
		int iDayStartTimeCol = stdProductsTable.getColNum("gpp_day_start_time");

		stdProductsTable.mathMultCol(iTotalQuantityCol, iPositionCol,
				iTotalQuantityCol);
		//stdProductsTable.mathABSCol(iTotalQuantityCol);
		stdProductsNumRows = stdProductsTable.getNumRows();

		for (iRow = 1; iRow <= stdProductsNumRows; iRow++) {
			double dNotnlFactor, dNotnlValue = 0;
			ODateTime iStartDate = stdProductsTable.getDateTime(
					iGmtStartDateTimeCol, iRow);
			ODateTime iMatDate = stdProductsTable.getDateTime(
					iGmtEndDateTimeCol, iRow);
			int time_zone = stdProductsTable.getInt(iTimeZoneCol, iRow);
			ODateTime dtGMTStartDateTime = ODateTime.dtNew();
			ODateTime dtGMTEndDateTime = ODateTime.dtNew();
			int day_start_time = 0;
			day_start_time = stdProductsTable.getInt(iDayStartTimeCol, iRow);
			
			if(day_start_time == Util.MAX_DATE)
				day_start_time = 0;

			iStartDate.addSecondsToDateTime(iStartDate, day_start_time);
			iMatDate.addSecondsToDateTime(iMatDate, 86400 + day_start_time);

			iStartDate.convertToGMT(dtGMTStartDateTime, time_zone);
			iMatDate.convertToGMT(dtGMTEndDateTime, time_zone);

			int iNumDays = dtGMTEndDateTime.getDate()
					- dtGMTStartDateTime.getDate();
			double dNotnl = stdProductsTable.getDouble(iTotalQuantityCol, iRow);
			int iDealVolumeType = stdProductsTable.getInt(iDealVolumeTypeCol,
					iRow);

			if (iDealVolumeType == DEAL_VOLUME_TYPE.DEAL_VOLUME_CONTRACT
					.toInt()) {
				if (iNumDays > 1) {
					dNotnlFactor = 1;
				} else if (iNumDays == 1) {
					dNotnlFactor = 1;
					iNumDays = 2;
				} else {
					dNotnlFactor = 0;
				}

				dNotnlValue = (dNotnl * dNotnlFactor) / (iNumDays) / 24;
			} else if (iDealVolumeType == DEAL_VOLUME_TYPE.DEAL_VOLUME_PERIOD
					.toInt()) {
				dNotnlValue = dNotnl / iNumDays / 24;
			} else if (iDealVolumeType == DEAL_VOLUME_TYPE.DEAL_VOLUME_HOURLY
					.toInt()) {
				int iTotalSeconds = 0;
				int iTotalMinutes = 0;
				int iTotalHours = 0;
				iTotalSeconds = dtGMTStartDateTime
						.computeTotalSecondsInGMTDateRange(dtGMTEndDateTime);
				iTotalMinutes = iTotalSeconds / 60;
				iTotalHours = iTotalMinutes / 60;
				dNotnlValue = dNotnl / iTotalHours;
			} else if (iDealVolumeType == DEAL_VOLUME_TYPE.DEAL_VOLUME_DAILY
					.toInt()) {
				dNotnlValue = dNotnl / iNumDays / 24;
			}
			stdProductsTable.setDouble(iTotalQuantityCol, iRow, dNotnlValue);
			stdProductsTable.setDouble(iMassMassUnitCol, iRow, dNotnlValue);
			stdProductsTable.setDouble(iVolumeVolumeUnitCol, iRow, dNotnlValue);
			stdProductsTable.setDouble(iBavMassMassUnitCol, iRow, dNotnlValue);
			stdProductsTable.setDouble(iBavVolumeVolumeUnitCol, iRow,
					dNotnlValue);
			stdProductsTable.setDouble(iEnergyCol, iRow, dNotnlValue);
			stdProductsTable.setDouble(iBavEnergyCol, iRow, dNotnlValue);
			stdProductsTable.setDouble(iQuantityCol, iRow, dNotnlValue);
			stdProductsTable.setDouble(iTotalHoursCol, iRow, dNotnlValue);
			stdProductsTable.setDateTime(iGmtStartDateTimeCol, iRow,
					dtGMTStartDateTime);
			stdProductsTable.setDateTime(iGmtEndDateTimeCol, iRow,
					dtGMTEndDateTime);
		}
		stdProductsTable.delCol(iPositionCol);
		stdProductsTable.delCol(iDayStartTimeCol);
		return stdProductsTable;
	}

	// Retrieve just the informational data
	public static Table retrieveInfo(int iQueryID) throws OException {
		Table dataTable = Table.tableNew();
		Table stdProductsTable = Table.tableNew();
		String sQuery, stdProductsQuery;
		int dbType = DBase.getDbType();

		// Select a set of informational fields here
		// One row per each schedule, whether it is currently BAV or not
		// This is a filtering result, so having extra rows won't hurt
		sQuery = " select"
				+ "    ab_tran.deal_tracking_num deal_num, "
				+ "    ab_tran.tran_num, "
				+ "    ab_tran.ins_num, "
				+ "    comm_schedule_header.param_seq_num, "
				+ "    comm_schedule_header.profile_seq_num, "
				+ "    comm_schedule_header.schedule_id, "
				+ "    comm_schedule_header.delivery_id, "
				+ "    comm_schedule_header.volume_type, "
				+ "    comm_schedule_delivery.delivery_status delivery_status, "
				+ "    comm_schedule_header.location_id, "
				+ "    gas_phys_location.zone_id, "
				+ "    gas_phys_location.pipeline_id, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.vpool_id, "
				+ "    gas_phys_location.loc_long_name, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.idx_subgroup, "
				+ "    gas_phys_location.location_type, ";
		if (dbType == DBTYPE_ENUM.DBTYPE_MSSQL.toInt() || dbType == DBTYPE_ENUM.DBTYPE_MSSQLU.toInt())
			sQuery += "	isnull(comm_sched_delivery_cmotion.measure_group_id, gas_phys_param.measure_group_id) measure_group_id, ";
		else
			/* Oracle */
			sQuery += "    nvl(comm_sched_delivery_cmotion.measure_group_id, gas_phys_param.measure_group_id) measure_group_id, ";
		sQuery += "    gas_phys_param.tank_id, "
				+ getParcelSource()
				+ "    ab_tran.buy_sell, "
				+ "    ab_tran.ins_type, "
				+ "    parameter.pay_rec, "
				+ "    0 inj_wth, "
				+ "    comm_schedule_header.int_strategy_id int_str, "
				+ "	  comm_schedule_header.ext_strategy_id ext_str, "
				+ "	  phys_header.service_type gas_service_type "
				+ " from "
				+ "    query_result, "
				+ "    comm_schedule_detail, "
				+ "    gas_phys_location, "
				+ "    gas_phys_param, "
				+ "    phys_header, "
				+ "    ab_tran, "
				+ "    parameter, "
				+ "    comm_schedule_header "
				+ "    left outer join comm_schedule_delivery on comm_schedule_header.delivery_id = comm_schedule_delivery.delivery_id "
				+ "    left outer join comm_sched_delivery_cmotion on comm_schedule_header.delivery_id = comm_sched_delivery_cmotion.delivery_id "
				+ "where "
				+ "    comm_schedule_header.schedule_id = comm_schedule_detail.schedule_id and "
				+ "    gas_phys_location.location_id = comm_schedule_header.location_id and "
				+ "    comm_schedule_header.ins_num = phys_header.ins_num and "
				+ "    ab_tran.ins_num = comm_schedule_header.ins_num and "
				+ "    parameter.ins_num = comm_schedule_header.ins_num and "
				+ "    parameter.param_seq_num = comm_schedule_header.param_seq_num and "
				+ "    parameter.ins_num = gas_phys_param.ins_num and parameter.param_seq_num = gas_phys_param.param_seq_num and "
				//+ "    comm_schedule_header.total_quantity != 0.0 and " // Don't
																		// want
																		// zero-valued
																		// entries
																		// cluttering
																		// the
																		// result
				+ "    query_result.query_result = ab_tran.tran_num and "
				+ "	  ab_tran.toolset != 52 and "
				+ "    query_result.unique_id = "
				+ iQueryID
				+ " order by "
				+ "    ab_tran.deal_tracking_num, comm_schedule_detail.gmt_start_date_time";

		com.olf.openjvs.DBase.runSqlFillTable(sQuery, dataTable);

		stdProductsQuery = " select"
				+ "    ab_tran.deal_tracking_num deal_num, "
				+ "    ab_tran.tran_num, "
				+ "    ab_tran.ins_num, "
				+ "	  profile.param_seq_num, "
				+ "    profile.profile_seq_num, "
				+ "    profile.profile_seq_num schedule_id, "
				+ "    0 delivery_id, "
				+ "	  ins_parameter.deal_volume_type, "
				+ "    0 delivery_status, "
				+ "	  gas_phys_param.location_id, "
				+ "    gas_phys_location.zone_id, "
				+ "    gas_phys_location.pipeline_id, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.vpool_id, "
				+ "    gas_phys_location.loc_long_name, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.idx_subgroup, "
				+ "    gas_phys_location.location_type, "
				+ "    gas_phys_param.measure_group_id, "
				+ "    gas_phys_param.tank_id, "
				+ "	  0 parcel_id, "
				+ "    ab_tran.buy_sell, "
				+ "    ab_tran.ins_type, "
				+ "    ab_tran.buy_sell pay_rec, "
				+ "    0 inj_wth, "
				+ "    ab_tran.int_trading_strategy int_str, "
				+ "	  ab_tran.ext_trading_strategy ext_str, "
				+ "	  0 gas_service_type "
				+ " from "
				+ "    gas_phys_location, "
				+ "    gas_phys_param, "
				+ "    ab_tran, "
				+ "    ins_parameter, "
				+ "    profile, "
				+ "    query_result "
				+ "where "
				+ "    ab_tran.ins_num = ins_parameter.ins_num and "  
                + "    ins_parameter.ins_num = profile.ins_num and " 
                + "    ins_parameter.param_seq_num = profile.param_seq_num and "  
                + "    ins_parameter.ins_num = gas_phys_param.ins_num and "  
                + "    ins_parameter.param_seq_num = gas_phys_param.param_seq_num and "  
                + "    gas_phys_location.location_id = gas_phys_param.location_id and "  
                + "    ab_tran.toolset = 52  and "                 
                + "    ab_tran.tran_type = 0 and " 
				+ "    query_result.query_result = ab_tran.tran_num and "
				+ "    query_result.unique_id = " + iQueryID + " order by "
				+ "    ab_tran.deal_tracking_num";

		com.olf.openjvs.DBase.runSqlFillTable(stdProductsQuery,
				stdProductsTable);

		stdProductsTable.copyRowAddAll(dataTable);

		int iNumRows = dataTable.getNumRows();

		int iInsTypeCol = dataTable.getColNum("ins_type");
		int iBuySellCol = dataTable.getColNum("buy_sell");
		int iPayRecCol = dataTable.getColNum("pay_rec");
		int iInjWthCol = dataTable.getColNum("inj_wth");

		// Now do the necessary post-processing
		for (int iRow = 1; iRow <= iNumRows; iRow++) {
			boolean setInjWth = false;
			int iInsType = Instrument.getBaseInsType(dataTable.getInt(
					iInsTypeCol, iRow));
			int iBuySell = dataTable.getInt(iBuySellCol, iRow);
			int iPayRec = dataTable.getInt(iPayRecCol, iRow);

			// Fix up Injection/Withdrawal same way as "Gas Info" script
			// This would set incorrect values on financial legs, but this UDSR
			// only gives info on physical amounts
			setInjWth = ((iInsType == INS_TYPE_ENUM.comm_storage.toInt()) || (iInsType == INS_TYPE_ENUM.comm_transit
					.toInt()));

			if (setInjWth && (iBuySell != iPayRec)) {
				dataTable.setInt(iInjWthCol, iRow, 2);
			} else if (setInjWth && (iBuySell == iPayRec)) {
				dataTable.setInt(iInjWthCol, iRow, 1);
			}
		}

		return dataTable;
	}

	public static void formatResult(Table returnt) throws OException {

		returnt.setColTitle("deal_num", "Deal Number");
		returnt.setColTitle("tran_num", "Transaction\nNumber");
		returnt.setColTitle("ins_num", "Instrument\nNumber");
		returnt.setColTitle("param_seq_num", "Deal Side");
		returnt.setColTitle("profile_seq_num", "Deal Profile\nPeriod");
		returnt.setColTitle("schedule_id", "Schedule ID");
		returnt.setColTitle("delivery_id", "Delivery ID");
		returnt.setColTitle("parcel_id", "Parcel\nID");

		returnt.setColTitle("total_quantity", "Original Total\nQuantity");
		returnt.setColFormatAsNotnl("total_quantity", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

		returnt.setColTitle("quantity", "Original Hourly\nQuantity");
		returnt.setColFormatAsNotnl("quantity", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

		returnt.setColTitle("bav_quantity", "BAV Quantity");
		returnt.setColFormatAsNotnl("bav_quantity", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

		returnt.setColTitle("energy", "Energy");
		returnt.setColFormatAsNotnl("energy", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

		returnt.setColTitle("bav_energy", "BAV Energy");
		returnt.setColFormatAsNotnl("bav_energy", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt(), 0);

		returnt.setColTitle("energy_unit", "Energy \n Unit");
		returnt.setColFormatAsRef("energy_unit",
				SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

		// returnt.setColTitle("gmt_start_date_time", "Start Date");
		returnt.setColTitle("startdate", "Start Date");
		returnt.setColFormatAsDate("startdate");

		// returnt.setColTitle("gmt_end_date_time", "End Date");
		returnt.setColTitle("enddate", "End Date");
		returnt.setColFormatAsDate("enddate");

		returnt.setColTitle("start_time", "Start Time");
		returnt.setColFormatAsTime("start_time");

		returnt.setColTitle("end_time", "End Time");
		returnt.setColFormatAsTime("end_time");

		returnt.setColTitle("service_type", "Service Type");
		returnt.setColFormatAsRef("service_type",
				SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);

		returnt.setColTitle("mass_massunit", "Mass");
		returnt.setColFormatAsNotnl("mass_massunit", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		returnt.setColTitle("volume_volumeunit", "Volume");
		returnt.setColFormatAsNotnl("volume_volumeunit", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		returnt.setColTitle("bav_mass_massunit", "BAV Mass");
		returnt.setColFormatAsNotnl("bav_mass_massunit", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		returnt.setColTitle("bav_volume_volumeunit", "BAV Volume");
		returnt.setColFormatAsNotnl("bav_volume_volumeunit", Util.NOTNL_WIDTH,
				Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

		returnt.setColTitle("unit", "Original\nUnit");
		returnt.setColFormatAsRef("unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

		returnt.setColTitle("mass_unit", "Mass\nUnit");
		returnt.setColFormatAsRef("mass_unit",
				SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);

		returnt.setColTitle("volume_unit", "Volume\nUnit");
		returnt.setColFormatAsRef("volume_unit",
				SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		returnt.colHide("to_be_deleted");
		// Note: at present, Oil is traded on whole days only, so the time
		// element is not used
		// Leaving here for the future
		returnt.colHide("gmt_start_date_time");
		returnt.colHide("gmt_end_date_time");
		// returnt.colHide("startdate");
		// returnt.colHide("enddate");
		// returnt.colHide("start_time");
		// returnt.colHide("end_time");
		returnt.colHide("deal_volume_type");
		returnt.colHide("param_seq_num_1");

		// This is the hourly quantity, which is stored in DB, but can confuse
		// Oil users
		// - more of an "hourly gas flow" thing this is. Same goes for the
		// total_hours
		// supplementary column.
		returnt.colHide("quantity");
		returnt.colHide("total_hours");
		returnt.colHide("to_be_deleted");

		returnt.setColTitle("volume_type", "Volume Type");
		returnt.setColFormatAsRef("volume_type",
				SHM_USR_TABLES_ENUM.VOLUME_TYPE_TABLE);

		returnt.setColTitle("delivery_status", "Delivery Status");
		returnt.setColFormatAsRef("delivery_status",
				SHM_USR_TABLES_ENUM.DELIVERY_STATUS_TABLE);

		returnt.setColTitle("bav_flag", "Is BAV\nVolume?");
		returnt.setColFormatAsRef("bav_flag", SHM_USR_TABLES_ENUM.YES_NO_TABLE);

		returnt.setColTitle("location_id", "Location");
		returnt.setColFormatAsRef("location_id",
				SHM_USR_TABLES_ENUM.GAS_PHYS_LOCATION_TABLE);

		returnt.setColTitle("zone_id", "Zone");
		returnt.setColFormatAsRef("zone_id",
				SHM_USR_TABLES_ENUM.GAS_PHYS_ZONE_TABLE);

		returnt.setColTitle("pipeline_id", "Pipeline");
		returnt.setColFormatAsRef("pipeline_id",
				SHM_USR_TABLES_ENUM.GAS_PHYS_PIPELINE_TABLE);

		returnt.setColTitle("meter_id", "Meter");

		returnt.setColTitle("vpool_id", "Valuation\nPool");
		returnt.setColFormatAsRef("vpool_id",
				SHM_USR_TABLES_ENUM.GAS_PHYS_VALUE_POOL_TABLE);

		returnt.setColTitle("loc_long_name", "Location\nLong Name");

		returnt.setColTitle("idx_subgroup", "Index\nSubgroup");

		returnt.setColTitle("location_type", "Location Type");
		returnt.setColFormatAsRef("location_type",
				SHM_USR_TABLES_ENUM.GAS_PHYS_LOC_TYPE);

		returnt.setColTitle("measure_group_id", "Measure Group");
		returnt.setColFormatAsRef("measure_group_id",
				SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE);

		returnt.setColTitle("tank_id", "Tank");
		returnt.setColFormatAsRef("tank_id", SHM_USR_TABLES_ENUM.TANKAGE_TABLE);

		returnt.setColTitle("proj_index", "Projection Index");
		returnt.setColFormatAsRef("proj_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);

		// I'm not interested in displaying any of these - they are just
		// supplementary to deducing
		// the Injection\Withdrawal field value
		returnt.colHide("buy_sell");
		returnt.colHide("pay_rec");
		returnt.colHide("ins_type");

		returnt.colHide("deal_start_time");
		returnt.colHide("time_zone");

		returnt.setColTitle("inj_wth", "Injection\nWithdrawal");

		returnt.setColTitle("int_str", "Internal\nStrategy");
		returnt.setColFormatAsRef("int_str",
				SHM_USR_TABLES_ENUM.STRATEGY_LISTING_TABLE);

		returnt.setColTitle("ext_str", "External\nStrategy");
		returnt.setColFormatAsRef("ext_str",
				SHM_USR_TABLES_ENUM.STRATEGY_LISTING_TABLE);

		returnt.setColTitle("gas_service_type", "Gas\nService Type");
		returnt.setColFormatAsRef("gas_service_type",
				SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);

		returnt.clearGroupBy();
		returnt.addGroupBy("deal_num");
		returnt.addGroupBy("param_seq_num");
		returnt.addGroupBy("profile_seq_num");
		returnt.addGroupBy("startdate");
		returnt.addGroupBy("start_time");
		returnt.groupBy();
	}

	public static Table retrieveBalanceLegsData(int iQueryID) throws OException {
		Table workTable = Table.tableNew(), finalTable;
		String sQuery;

		sQuery = " select "
				+ "    ab_tran.deal_tracking_num deal_num, "
				+ "    ab_tran.tran_num, "
				+ "    gas_phys_param.param_seq_num, "
				+ "    gas_phys_param.param_seq_num mapped_param, "
				+ "    gas_phys_param.location_id, "
				+ "    gas_phys_param.measure_group_id, "
				+ "    gas_phys_param.tank_id, "
				+ "    gas_phys_location.zone_id, "
				+ "    gas_phys_location.pipeline_id, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.vpool_id, "
				+ "    gas_phys_location.loc_long_name, "
				+ "    gas_phys_location.meter_id, "
				+ "    gas_phys_location.idx_subgroup, "
				+ "    gas_phys_location.location_type, "
				+ "    3 inj_wth " // 3 is balance leg identifier
				+ " from "
				+ "    query_result, "
				+ "    gas_phys_location, "
				+ "    gas_phys_param, "
				+ "    ab_tran "
				+ "where "
				+ "    ab_tran.ins_num = gas_phys_param.ins_num and "
				+ "    gas_phys_location.location_id = gas_phys_param.location_id and "
				+ "    query_result.query_result = ab_tran.tran_num and "
				+ "    query_result.unique_id = " + iQueryID + " order by "
				+ "    ab_tran.deal_tracking_num, gas_phys_param.param_seq_num";

		com.olf.openjvs.DBase.runSqlFillTable(sQuery, workTable);

		finalTable = workTable.cloneTable();

		finalTable.select(workTable,
				"deal_num, tran_num, param_seq_num, mapped_param",
				"deal_num GT 0");

		int iNumRows = finalTable.getNumRows();

		int lastDeal = -1, lastProduct = -1, lastMappedParam = -1;

		for (int i = 1; i <= iNumRows; i++) {
			boolean bFound = false;
			int dealNum = finalTable.getInt(1, i);
			int tranNum = finalTable.getInt(2, i);
			int paramSeqNum = finalTable.getInt(3, i);

			Transaction trnTran = Transaction.retrieve(tranNum);
			int product = trnTran.getFieldInt(
					TRANF_FIELD.TRANF_IDX_SUBGROUP.toInt(), paramSeqNum);

			int balanceType = trnTran
					.getFieldInt(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt());

			if (balanceType == INS_SUB_TYPE.comm_deal_phys_balance.toInt()) {
				// Associate the first found deal balance leg
				for (int j = 0; j < trnTran.getNumParams(); j++) {
					String payRec = trnTran.getField(
							TRANF_FIELD.TRANF_PAY_REC.toInt(), j);

					if (payRec.equals("Balance")) {
						lastDeal = dealNum;
						lastMappedParam = j;
						bFound = true;
						break;
					}
				}

				if (bFound) {
					finalTable.setInt("mapped_param", i, lastMappedParam);
				}
			} else if (balanceType == INS_SUB_TYPE.comm_product_phys_balance
					.toInt()) {
				// Associate the first found deal balance leg that has the same
				// product
				for (int j = 0; j < trnTran.getNumParams(); j++) {
					String payRec = trnTran.getField(
							TRANF_FIELD.TRANF_PAY_REC.toInt(), j);
					int curProduct = trnTran.getFieldInt(
							TRANF_FIELD.TRANF_IDX_SUBGROUP.toInt(), j);

					if (payRec.equals("Balance") && (curProduct == product)) {
						lastDeal = dealNum;
						lastMappedParam = j;
						lastProduct = product;
						bFound = true;
						break;
					}
				}

				if (bFound) {
					finalTable.setInt("mapped_param", i, lastMappedParam);
				}
			} else {
				// Associate the first found withdrawal leg
				for (int j = 0; j < trnTran.getNumParams(); j++) {
					String payRec = trnTran.getField(
							TRANF_FIELD.TRANF_PAY_REC.toInt(), j);

					if (payRec.equals("Discharge")
							|| payRec.equals("Withdrawal")) {
						lastDeal = dealNum;
						lastMappedParam = j;
						bFound = true;
						break;
					}
				}

				if (bFound) {
					finalTable.setInt("mapped_param", i, lastMappedParam);
				}
			}
		}
		finalTable
				.select(workTable,
						"location_id, measure_group_id, tank_id, zone_id, pipeline_id, meter_id, vpool_id, loc_long_name, meter_id, idx_subgroup, location_type, inj_wth",
						"deal_num EQ $deal_num AND param_seq_num EQ $mapped_param");

		return finalTable;
	}

	public static String getParcelSource() throws OException {
		String sParcelSource;

		// Parcels are supported in 10.2 and above
		Table version = Ref.getVersion();

		if ((version.getInt(1, 1) >= 11)
				|| ((version.getInt(1, 1) == 10) && (version.getInt(2, 1) >= 2))) {
			sParcelSource = " comm_schedule_header.parcel_id, ";
		} else {
			sParcelSource = " 0 parcel_id, ";
		}

		return sParcelSource;
	}

	public static Table splitIntoDays(Table tblData) throws OException {

		Table splitTable, rowTable, output;
		Integer iRow;

		rowTable = tblData.cloneTable();
		splitTable = tblData.cloneTable();

		for (iRow = 1; iRow <= tblData.getNumRows(); iRow++) {
			tblData.copyRowAdd(iRow, rowTable);
			output = splitRowsDaily(rowTable);// This method will split the rows
												// into a daily rows.
			output.copyRowAddAll(splitTable);
			output.destroy();
			rowTable.clearDataRows();
		}

		rowTable.destroy();

		int iSDTCol = splitTable.getColNum("gmt_start_date_time");
		int iEDTCol = splitTable.getColNum("gmt_end_date_time");
		int iSDCol = splitTable.getColNum("startdate");
		int iEDCol = splitTable.getColNum("enddate");
		int iSTCol = splitTable.getColNum("start_time");
		int iETCol = splitTable.getColNum("end_time");

		// Now do the necessary post-processing
		for (iRow = 1; iRow <= splitTable.getNumRows(); iRow++) {
			// Fix up the dates by converting to a date+time combo
			int iStartDate = splitTable.getDate(iSDTCol, iRow);
			int iStartTime = splitTable.getTime(iSDTCol, iRow);

			splitTable.setInt(iSDCol, iRow, iStartDate);
			splitTable.setInt(iSTCol, iRow, iStartTime);

			// For end time, we want to change 32767:0 to 32766:86400 for better
			// bucketing on client

			int iEndDate = splitTable.getDate(iEDTCol, iRow);
			int iEndTime = splitTable.getTime(iEDTCol, iRow);

			if (iEndTime == 0) {
				iEndDate--;
				iEndTime = 86400;
			}

			splitTable.setInt(iEDCol, iRow, iEndDate);
			splitTable.setInt(iETCol, iRow, iEndTime);
		}

		return splitTable;
	}

	static Table splitRowsDaily(Table rowTable) throws OException {
		Table output = rowTable.cloneTable();

		int timezone = rowTable.getInt("time_zone", 1);

		// Get the start and end date and convert from GMT to CED
		ODateTime gmtSDT, gmtEDT, localSDT = ODateTime.dtNew(), localEDT = ODateTime
				.dtNew();

		gmtSDT = rowTable.getDateTime("gmt_start_date_time", 1);
		gmtSDT.convertFromGMT(localSDT, timezone);

		gmtEDT = rowTable.getDateTime("gmt_end_date_time", 1);
		gmtEDT.convertFromGMT(localEDT, timezone);

		// Get number of seconds, then get number of days, rounding upwards
		Integer numSeconds = gmtSDT.computeTotalSecondsInGMTDateRange(gmtEDT);
		Integer numDays = (int) Math.ceil((double) localSDT
				.computeTotalSecondsInGMTDateRange(localEDT) / 86400.0);

		// Pre-create the requisite number of rows for efficiency
		output.addNumRows(numDays);

		ODateTime currentLocalSDT = ODateTime.dtNew();
		ODateTime currentgmtSDT = ODateTime.dtNew();
		ODateTime currentLocalEDT = ODateTime.dtNew();
		ODateTime currentgmtEDT = ODateTime.dtNew();

		for (int testCounter = 0; testCounter < numDays; testCounter++) {
			currentLocalSDT.setDateTime(localSDT.getDate() + testCounter,
					localSDT.getTime());
			currentLocalEDT.setDateTime(localSDT.getDate() + testCounter + 1,
					localSDT.getTime());

			if (currentLocalEDT.getDate() > localEDT.getDate()) {
				currentLocalEDT.setDateTime(localEDT.getDate(),
						localEDT.getTime());
			} else if ((currentLocalEDT.getDate() == localEDT.getDate())
					&& (currentLocalEDT.getTime() > localEDT.getTime())) {
				currentLocalEDT.setDateTime(localEDT.getDate(),
						localEDT.getTime());
			}

			// OConsole.print("Counter: " + testCounter +
			// ", current local start time: " + currentLocalSDT.getDate() + ", "
			// + currentLocalSDT.getTime());
			// OConsole.print(", end time: " + currentLocalEDT.getDate() + ", "
			// + currentLocalEDT.getTime() + "\n");

			currentLocalSDT.convertToGMT(currentgmtSDT, timezone);
			currentLocalEDT.convertToGMT(currentgmtEDT, timezone);

			// OConsole.print("Counter: " + testCounter +
			// ", current GMT start time: " + currentgmtSDT.getDate() + ", " +
			// currentgmtSDT.getTime());
			// OConsole.print(", end time: " + currentgmtEDT.getDate() + ", " +
			// currentgmtEDT.getTime() + "\n");

			// Use counter+1, as table offsets are 1-based
			rowTable.copyRow(1, output, testCounter + 1);

			Integer dealVolumeType = output.getInt("deal_volume_type",
					testCounter + 1);

			output.setDateTime("gmt_start_date_time", testCounter + 1,
					currentgmtSDT);
			output.setDateTime("gmt_end_date_time", testCounter + 1,
					currentgmtEDT);

			double factor = 1.0;

			// If deal volume type is hourly, check number of seconds
			if (dealVolumeType == 0) {
				factor = (double) currentgmtSDT
						.computeTotalSecondsInGMTDateRange(currentgmtEDT)
						/ (double) numSeconds;
			} else {
				// The factor is 1 divided by total number of days in schedule
				factor = 1 / (double) numDays;
			}

			for (int i = 0; i < doubleValCols.length; i++) {
				output.setDouble(doubleValCols[i], testCounter + 1,
						output.getDouble(doubleValCols[i], testCounter + 1)
								* factor);
			}

		}
		return output;
	}

	public static Table performDetailedEnergyConversion(Table dataTable) throws OException {
		Table splitDealTable = dataTable.cloneTable(), massConversionTable, volConversionTable, energyConversionTable;
		int iStartDate,iEndDate,iNumRows,iRow,origUnit, fromUnit, toMassUnit, toVolUnit, toEnergyUnit, iLocationID;
		double dMassConvFactor, dVolConvFactor, dEnergyConvFactor;
		int iNumRowsMassConversionTable, iNumRowsVolConversionTable, iNumRowsEnergyConversionTable;
		//ODateTime dtGMTStartDateTime = ODateTime.dtNew();
		//ODateTime dtGMTEndDateTime = ODateTime.dtNew();
		iNumRows = dataTable.getNumRows();				

		String[] fromColumns, toMassColumns, toVolColumns, toEnergyColumns;
		
		for (iRow = 1; iRow <= iNumRows; iRow ++) 
		{
			//Identify if it will be an energy->volume conversion or a volume->energy conversion
			origUnit = dataTable.getInt("unit",iRow);
			iLocationID = dataTable.getInt("location_id",iRow);
			
			iStartDate = dataTable.getInt("startdate", iRow);
			iEndDate = dataTable.getInt("enddate", iRow);	
			
			toMassColumns = massColumns;
			toVolColumns = volumeColumns;
			toEnergyColumns = energyColumns;
			
			toMassUnit = dataTable.getInt("mass_unit",iRow);
			toVolUnit = dataTable.getInt("volume_unit",iRow);
			toEnergyUnit = dataTable.getInt("energy_unit",iRow);

			fromUnit = dataTable.getInt("unit",iRow);
			fromColumns = quantityColumns;
			
			massConversionTable = Transaction.utilGetEnergyConversionFactors(iStartDate, iEndDate, iLocationID, fromUnit, toMassUnit);
			volConversionTable = Transaction.utilGetEnergyConversionFactors(iStartDate, iEndDate, iLocationID, fromUnit, toVolUnit);
			energyConversionTable = Transaction.utilGetEnergyConversionFactors(iStartDate, iEndDate, iLocationID, fromUnit, toEnergyUnit);
			
			iNumRowsMassConversionTable = massConversionTable.getNumRows();
			iNumRowsVolConversionTable= volConversionTable.getNumRows();			
			iNumRowsEnergyConversionTable= energyConversionTable.getNumRows();
										 
			if (iNumRowsMassConversionTable == 1 && iNumRowsVolConversionTable == 1 && iNumRowsEnergyConversionTable == 1)
			{
				// A single unit conversion factor applies for the whole time period
				// Use this in favor of the static unit conversion factor
				dMassConvFactor = massConversionTable.getDouble("Conv Factor",1);
				dVolConvFactor = volConversionTable.getDouble("Conv Factor",1);
				dEnergyConvFactor = energyConversionTable.getDouble("Conv Factor",1);
				
				for (int i = 0; i < fromColumns.length; i++)
				{
					dataTable.setDouble(toMassColumns[i], iRow, 
							dMassConvFactor * dataTable.getDouble(fromColumns[i], iRow));
					dataTable.setDouble(toVolColumns[i], iRow, 
							dVolConvFactor * dataTable.getDouble(fromColumns[i], iRow));
					dataTable.setDouble(toEnergyColumns[i], iRow, 
							dEnergyConvFactor * dataTable.getDouble(fromColumns[i], iRow));
				}
			}
			else if (iNumRowsMassConversionTable > 1 || iNumRowsVolConversionTable > 1 || iNumRowsEnergyConversionTable > 1)
			{
				// Multiple unit conversion factors apply - need to split the row into
				// multiple ones, one per each applicable unit conversion factor
				// For table memory usage reasons, keep the new rows in a separate table
				// and delete this one from the main table
				dataTable.setInt("to_be_deleted", iRow, 1);		
				// energyConversionTable.viewTable();
				performEnergyConversionForRow(dataTable, iRow, massConversionTable, volConversionTable, energyConversionTable, splitDealTable, 
											  fromColumns, toMassColumns, toVolColumns, toEnergyColumns, fromUnit,toMassUnit, toVolUnit, toEnergyUnit);
			}
			
			massConversionTable.destroy();
			volConversionTable.destroy();
			energyConversionTable.destroy();
		}

		dataTable.deleteWhereValue("to_be_deleted", 1);
		dataTable.tuneGrowth(splitDealTable.getNumRows());
		dataTable.select(splitDealTable, "*", "tran_num GT 0");

		return dataTable;
	}	
	   
   private static void performEnergyConversionForRow(
		Table dataTable, int iRow, Table massConvFacTable, Table volConvFacTable, Table energyConvFacTable, Table output, 
		String[] fromColumns, String[] massColumns,String[] volColumns, String[] energyColumns,
		int fromUnit, int toMassUnit, int toVolUnit, int toEnergyUnit )throws OException   
   {  	   
	   double defaultMassConversionFactor = Transaction.getUnitConversionFactor(fromUnit, toMassUnit);
	   double defaultVolConversionFactor = Transaction.getUnitConversionFactor(fromUnit, toVolUnit);
	   double defaultEnergyConversionFactor = Transaction.getUnitConversionFactor(fromUnit, toEnergyUnit);
	   int timezone = dataTable.getInt("time_zone", iRow);	
	   int numOfDaysInCurrentPeriod = 0;
	   int realMassConvValue = 0 , realVolConvValue = 0, realEnergyConvValue = 0;
	   
	   //Get the start and end date and convert from GMT to CED
	   ODateTime gmtSDT, gmtEDT, localSDT = ODateTime.dtNew(), localEDT = ODateTime.dtNew();
	   
	   gmtSDT = dataTable.getDateTime("gmt_start_date_time", iRow);
	   gmtSDT.convertFromGMT(localSDT, timezone);
	   
	   gmtEDT = dataTable.getDateTime("gmt_end_date_time", iRow);
	   gmtEDT.convertFromGMT(localEDT, timezone);
	   	   	  	   
	   // Get number of seconds, then get number of days, rounding upwards
	   Integer numSeconds = gmtSDT.computeTotalSecondsInGMTDateRange(gmtEDT);	   
	   Integer numDays = (int)Math.ceil((double) localSDT.computeTotalSecondsInGMTDateRange(localEDT) / 86400.0); 
	   
	   // Pre-create the requisite number of rows for efficiency	      	   	   
	   ODateTime currentLocalSDT = ODateTime.dtNew();
	   ODateTime currentgmtSDT = ODateTime.dtNew();
	   ODateTime currentLocalEDT = ODateTime.dtNew();
	   ODateTime currentgmtEDT = ODateTime.dtNew();	   
	   
	   int iSDTCol = dataTable.getColNum("gmt_start_date_time");
	   int iEDTCol = dataTable.getColNum("gmt_end_date_time");	   
	   int iSDCol = dataTable.getColNum("startdate");
	   int iEDCol = dataTable.getColNum("enddate");
	   int iSTCol = dataTable.getColNum("start_time");
	   int iETCol = dataTable.getColNum("end_time");	   
	   
	   currentLocalSDT.setDateTime(localSDT.getDate(), localSDT.getTime());
	   
	   for (int testCounter = 1; testCounter <= numDays; testCounter++)		   
	   {	   
			double thisMassConvFactor = massConvFacTable.getDouble("Conv Factor", testCounter);
			double thisVolConvFactor = volConvFacTable.getDouble("Conv Factor", testCounter);
			double thisEnergyConvFactor = energyConvFacTable.getDouble("Conv Factor", testCounter);
			double nextMassConvFactor = (testCounter < massConvFacTable.getNumRows()) ?
					massConvFacTable.getDouble("Conv Factor", testCounter + 1) :
					0.0;
            double nextVolConvFactor = (testCounter < volConvFacTable.getNumRows()) ?
					volConvFacTable.getDouble("Conv Factor", testCounter + 1) :
					0.0;
			double nextEnergyConvFactor = (testCounter < energyConvFacTable.getNumRows()) ?
					energyConvFacTable.getDouble("Conv Factor", testCounter + 1) :
					0.0;
			
			numOfDaysInCurrentPeriod++;		
					
			// If the current conversion factor differs from the next, or we at the end,
			// create a new entry
			if (((thisMassConvFactor != nextMassConvFactor)  || 
				(thisVolConvFactor != nextVolConvFactor) || 
				(thisEnergyConvFactor != nextEnergyConvFactor)) || 
				(testCounter == numDays))
				{
				currentLocalEDT.setDateTime(localSDT.getDate() + testCounter, localSDT.getTime());
		   	   	   
				if (currentLocalEDT.getDate() > localEDT.getDate())
				{			   
					currentLocalEDT.setDateTime(localEDT.getDate(), localEDT.getTime());
				}
				else if ((currentLocalEDT.getDate() == localEDT.getDate()) &&
						 (currentLocalEDT.getTime() > localEDT.getTime()))
				{
					currentLocalEDT.setDateTime(localEDT.getDate(), localEDT.getTime());
				}
				   
				//OConsole.print("Counter: " + testCounter + ", current local start time: " + currentLocalSDT.getDate() + ", " + currentLocalSDT.getTime());
				//OConsole.print(", end time: " + currentLocalEDT.getDate() + ", " + currentLocalEDT.getTime() + "\n");				
				
				currentLocalSDT.convertToGMT(currentgmtSDT, timezone);
				currentLocalEDT.convertToGMT(currentgmtEDT, timezone);
				     
				//OConsole.print("Counter: " + testCounter + ", current GMT start time: " + currentgmtSDT.getDate() + ", " + currentgmtSDT.getTime());
				//OConsole.print(", end time: " + currentgmtEDT.getDate() + ", " + currentgmtEDT.getTime() + "\n");
				   								   
				int lastOutputRow = output.addRow();
				dataTable.copyRow(iRow, output, lastOutputRow);
				   
				Integer dealVolumeType = output.getInt("deal_volume_type", lastOutputRow);
				   
				output.setDateTime("gmt_start_date_time", lastOutputRow, currentgmtSDT);
				output.setDateTime("gmt_end_date_time", lastOutputRow, currentgmtEDT);
				   		   
				// Fix up the dates by converting to a date+time combo
				int iStartDate = output.getDate(iSDTCol, lastOutputRow);
				int iStartTime = output.getTime(iSDTCol, lastOutputRow);

				output.setInt(iSDCol, lastOutputRow, iStartDate);
				output.setInt(iSTCol, lastOutputRow, iStartTime);
				  
				// For end time, we want to change 32767:0 to 32766:86400 for better bucketing on client
				  
				int iEndDate = output.getDate(iEDTCol, lastOutputRow);
				int iEndTime = output.getTime(iEDTCol, lastOutputRow);

				if (iEndTime == 0)
				{
					iEndDate--;
					iEndTime = 86400;
				}

				output.setInt(iEDCol, lastOutputRow, iEndDate);
				output.setInt(iETCol, lastOutputRow, iEndTime);								

				double factor = 1.0;
				   
				//If deal volume type is hourly, check number of seconds
				if (dealVolumeType == 0)
				{
					factor = (double) currentgmtSDT.computeTotalSecondsInGMTDateRange(currentgmtEDT) / (double) numSeconds;
				}
				else
				{
					//The factor is 1 divided by total number of days in schedule
					factor = numOfDaysInCurrentPeriod / (double) numDays;
				}
				   
				//for (int i = 0; i < doubleValCols.length; i++)
				//{
					//output.setDouble(doubleValCols[i], lastOutputRow, output.getDouble(doubleValCols[i], lastOutputRow) * factor);				   				   
				//}

				// Now apply conversion factor
				if (thisMassConvFactor == 0.0) {thisMassConvFactor = defaultMassConversionFactor;}
				if (thisVolConvFactor == 0.0) {thisVolConvFactor = defaultVolConversionFactor;}
				if (thisEnergyConvFactor == 0.0) {thisEnergyConvFactor = defaultEnergyConversionFactor;}
				
				if(massConvFacTable.getNumRows() > testCounter + 2){
					if(thisMassConvFactor != nextMassConvFactor){realMassConvValue = 1;}else{realMassConvValue = 0;}
					if(thisVolConvFactor != nextVolConvFactor){realVolConvValue = 1;}else{realVolConvValue = 0;}
					if(thisEnergyConvFactor != nextEnergyConvFactor){realEnergyConvValue = 1;}else{realEnergyConvValue = 0;}
				}
				
				for (int i = 0; i < fromColumns.length; i++)
				{
					if(realMassConvValue == 1){
						output.setDouble(massColumns[i], lastOutputRow, output.getDouble(quantityColumns[i], lastOutputRow) * factor);
						output.setDouble(massColumns[i], lastOutputRow, thisMassConvFactor * output.getDouble(massColumns[i], lastOutputRow));
					}
					else{
						output.setDouble(massColumns[i], lastOutputRow, output.getDouble(massColumns[i], lastOutputRow) * factor);
					}

					if(realVolConvValue == 1){
						output.setDouble(volColumns[i], lastOutputRow, output.getDouble(quantityColumns[i], lastOutputRow) * factor);
						output.setDouble(volColumns[i], lastOutputRow, thisVolConvFactor * output.getDouble(volColumns[i], lastOutputRow));
					}
					else{
						output.setDouble(volColumns[i], lastOutputRow, output.getDouble(volColumns[i], lastOutputRow) * factor);
					}
					
					if(realEnergyConvValue == 1){
						output.setDouble(energyColumns[i], lastOutputRow, output.getDouble(quantityColumns[i], lastOutputRow) * factor);
						output.setDouble(energyColumns[i], lastOutputRow, thisEnergyConvFactor * output.getDouble(energyColumns[i], lastOutputRow));
					}
					else{
						output.setDouble(energyColumns[i], lastOutputRow, output.getDouble(energyColumns[i], lastOutputRow) * factor);
					}
					
					//if(realMassConvValue == 1 || realVolConvValue == 1 || realEnergyConvValue == 1 || testCounter == numDays - 1 || testCounter == 0)
						output.setDouble(quantityColumns[i], lastOutputRow, output.getDouble(quantityColumns[i], lastOutputRow) * factor);
				}
				
				// Update current local SDT to be the end of this period\start of the new period
				currentLocalSDT.setDateTime(currentLocalEDT.getDate(), currentLocalEDT.getTime());
				
				// Update number of days for the new period
				numOfDaysInCurrentPeriod = 0;
			}
		}
   }
   
   public static Table GetMasterVolumeTypes(boolean getCrudeRatherThanGasVolumesTypes) throws OException
   {
	   // Prepare the master volume types table from db - this is used to get ID's from specified volume statuses
	   String sCachedTableName = "APM Gas Volume Types";
	   if (getCrudeRatherThanGasVolumesTypes)
		   sCachedTableName = "APM Crude Volume Types";
	   
	   Table tMasterListVolumeTypes = Table.getCachedTable(sCachedTableName);
	   if (Table.isTableValid(tMasterListVolumeTypes) == 0)
	   {
	      tMasterListVolumeTypes = Table.tableNew();
	      int retval = 0;
	      if (getCrudeRatherThanGasVolumesTypes)
	    	  retval = DBaseTable.loadFromDbWithSQL(tMasterListVolumeTypes, "crude_name, id_number",  "volume_type",  "crude_active_flag = 1");
	      else
	    	  retval = DBaseTable.loadFromDbWithSQL(tMasterListVolumeTypes, "gas_name, id_number",  "volume_type",  "gas_active_flag = 1");
	    	  
	      if( retval != 0)
	      {
	         // sort so that the types can be easily found
	         tMasterListVolumeTypes.sortCol(1);
	         // uppercase them so that any case sensitive stuff does not rear its ugly head
	         int numRows = tMasterListVolumeTypes.getNumRows();
	         for (int iRow = 1; iRow <= numRows; iRow++)
	         {
	            String volumeTypeStr = Str.toUpper(tMasterListVolumeTypes.getString(1, iRow));
	            tMasterListVolumeTypes.setString( 1, iRow, volumeTypeStr);
	         }

	         Table.cacheTable(sCachedTableName, tMasterListVolumeTypes);
	      }
	   }	
	   
	   return tMasterListVolumeTypes;
   }
   
   private static String GetShowAllVolumeTypesQueryString(String volumeTypes) throws OException
   {
	   Table volumeTypeList;
	   int startStringPosition, endStringPosition, iFoundRow, iRow;
	   int numRowsAdded = 0;
	   String temporaryVolumeTypes, volumeTypeStr, volumeTypesStripped;
	   Table tMasterListCrudeVolumeTypes, tMasterListGasVolumeTypes;
	   String sql_query = "";
	   String volTypesSQL = "";
	   
       volumeTypeList = Table.tableNew( "volume_types" );
       volumeTypeList.addCol("volume_type", COL_TYPE_ENUM.COL_STRING );
       volumeTypeList.addCol("volume_type_ID", COL_TYPE_ENUM.COL_INT );
	   
	   tMasterListCrudeVolumeTypes = GetMasterVolumeTypes(true);
	   tMasterListGasVolumeTypes = GetMasterVolumeTypes(false);

	   startStringPosition = 1;
	   endStringPosition = 1;
	   
      // now split the volume types apart so that we can create an ID comma separated string
      volumeTypesStripped = Str.stripBlanks(volumeTypes);
      if( Str.isNull( volumeTypesStripped ) == 0 && Str.isEmpty( volumeTypesStripped ) == 0 )
      {
         while( endStringPosition > 0 )
         {
            startStringPosition = 0;
            endStringPosition = Str.findSubString( volumeTypes, "," );
                  
            numRowsAdded += 1;           
            volumeTypeList.addRow();
                  
            if ( endStringPosition > 0 )
            {
               volumeTypeList.setString(1, numRowsAdded, Str.substr( volumeTypes, startStringPosition, endStringPosition - startStringPosition ) );
                     
               temporaryVolumeTypes = Str.substr( volumeTypes, endStringPosition + 1, Str.len( volumeTypes ) - endStringPosition - 1 );
               volumeTypes = temporaryVolumeTypes;
            }
            else
            {
               volumeTypeList.setString(1, numRowsAdded, Str.substr( volumeTypes, startStringPosition, Str.len( volumeTypes ) - startStringPosition ) );
            }            
         }  

         // if no rows then exit as we should have something
         if ( volumeTypeList.getNumRows() < 1 )
         {
            volumeTypeList.destroy();
            OConsole.print("APM Gas Position Volume Types field populated but no valid values !!! Please correct the simulation result mod\n");
            Util.exitFail();
         }

         // now we have a table of statuses - next job is to convert them into ID's
         for (iRow = 1; iRow <= volumeTypeList.getNumRows(); iRow++)
         {
            volumeTypeStr = Str.toUpper(volumeTypeList.getString(1, iRow));

            // BAV handled separately
            if ( Str.equal(volumeTypeStr, "BAV") == 1)
               continue;

            // try to find them in the crude list first
            iFoundRow = tMasterListCrudeVolumeTypes.findString(1, volumeTypeStr, SEARCH_ENUM.FIRST_IN_GROUP);
            if ( iFoundRow > 0 )
            {
               if ( Str.len(volTypesSQL) > 0 )
                  volTypesSQL = volTypesSQL + ",";

               volTypesSQL = volTypesSQL + Str.intToStr(tMasterListCrudeVolumeTypes.getInt(2, iFoundRow));
            }
            else
            {
            	// not found there - so now try in the Gas list
                iFoundRow = tMasterListGasVolumeTypes.findString(1, volumeTypeStr, SEARCH_ENUM.FIRST_IN_GROUP);
                if ( iFoundRow > 0 )
                {
                    if ( Str.len(volTypesSQL) > 0 )
                        volTypesSQL = volTypesSQL + ",";

                     volTypesSQL = volTypesSQL + Str.intToStr(tMasterListGasVolumeTypes.getInt(2, iFoundRow));                
                }
                else
                {
	               volumeTypeList.destroy();
	               OConsole.print("Cannot find volume type from APM Phys Volume Volume Types field: '" + volumeTypeStr + "'. Please correct the simulation result mod\n");
	               Util.exitFail();
                }
            }
 
         }

         // filter on volume type in comm_schedule_header - note this does not include the BAV type
         if ( Str.len(volTypesSQL) > 0 )
            sql_query += " (comm_schedule_header.volume_type in (" + volTypesSQL + ")";

         // check for BAV
         volumeTypeList.sortCol(1);
         iFoundRow = volumeTypeList.findString(1, "BAV", SEARCH_ENUM.FIRST_IN_GROUP);
         if ( Str.len(volTypesSQL) > 0 ) // if BAV required then we need an OR on the bav_flag
         {
            if ( iFoundRow > 0 )
            {
               sql_query += " or comm_schedule_detail.bav_flag = 1) and ";
            }
            else
               sql_query += ") and ";
         }
         else  // only value is BAV - same effect as setting show all statuses = 0
            sql_query += " comm_schedule_detail.bav_flag = 1 and ";
	   }

	   if ( volumeTypeList != null && Table.isTableValid(volumeTypeList) == 1)
	      volumeTypeList.destroy();

	   return sql_query;
   }
   
   /**
    * Determine which location to use (leg location or nom location) for calculating the overscheduled quantity. 
    * If the nom loc has a corresponding trading sched loc then use the nom location, otherwise use leg location.
    * @param tblTradingRows Table of VOLUME_TYPE_TRADING rows.
    * @param iDealNum       Deal Number      
    * @param iStartDate     Start Date
    * @param iEndDate       End Date
    * @param iParamSeqNum   Side
    * @param iLocation      Nomination Location
    * @return
    * @throws OException
    */
   private static int getTradingVolumeLocation(Table tblTradingRows, int iDealNum, int iStartDate, int iEndDate, int iParamSeqNum, int iLocation) throws OException
   {
	  int iTradingVolumeLocation;
	  
	  Table tblTemp = Table.tableNew();
	  Table tblTempTradingRows = tblTradingRows.copyTable();
	  
	  tblTemp.select(tblTempTradingRows, "*", "deal_num EQ "+ iDealNum + 
               " and startdate EQ " + iStartDate + 
               " and enddate EQ " + iEndDate + 
               " and param_seq_num EQ " + iParamSeqNum);
	  
	  if(tblTemp.getNumRows() <= 0)
	  {
		  iTradingVolumeLocation = -1;
	  }
	  else
	  {
		  iTradingVolumeLocation = (tblTemp.unsortedFindInt("location_id", iLocation) < 0 ? tblTemp.getInt("leg_location", 1) : iLocation);
	  }
	  
	  tblTemp.destroy();
	  tblTempTradingRows.destroy();

   	  return iTradingVolumeLocation;
   }

   public static Table calculateOverScheduledQuantity(Table outputTable) throws OException
   {
	   String strWhere=null;
	   int outer_row = 0;
	   double nomQtyTotal = 0, nomVolumeTotal = 0, nomEnergyTotal = 0;
	   int iDealNum = -1;
	   int iStartDate = -1;
	   int iEndDate = -1;
	   int iParamSeqNum = 1;
	   int iLocation = 1;
	   int row = 0, daily_nom_row = 0;
	   Table tblTradingRows = Table.tableNew();
	   Table tblNominatedRows = Table.tableNew();
	   Table tblDailyNominatedRows = Table.tableNew();
	   boolean find_matching_daily_nom_record = false;

	   strWhere = "volume_type EQ " + VOLUME_TYPE.VOLUME_TYPE_TRADING.toInt();
	   tblTradingRows.select( outputTable, "*", strWhere);
	   tblTradingRows.addGroupBy("deal_num");
	   tblTradingRows.addGroupBy("startdate");
	   tblTradingRows.addGroupBy("param_seq_num");
	   tblTradingRows.groupBy();
	   tblTradingRows.mathABSCol("total_quantity");
	   
	   strWhere = "volume_type EQ " + VOLUME_TYPE.VOLUME_TYPE_NOMINATED.toInt();
	   tblNominatedRows.select( outputTable, "*", strWhere);
	   tblTradingRows.addCol("nominated_quantity", COL_TYPE_ENUM.COL_DOUBLE);
	   tblTradingRows.addCol("nominated_volume", COL_TYPE_ENUM.COL_DOUBLE);
	   tblTradingRows.addCol("nominated_energy", COL_TYPE_ENUM.COL_DOUBLE);  
	   tblNominatedRows.addGroupBy("deal_num");
	   tblNominatedRows.addGroupBy("startdate");
	   tblNominatedRows.addGroupBy("param_seq_num");
	   tblNominatedRows.groupBy();
	   tblNominatedRows.mathABSCol("total_quantity");

	   strWhere = "volume_type EQ " + VOLUME_TYPE.VOLUME_TYPE_DAILY_NOMINATED.toInt();
	   tblDailyNominatedRows.select( outputTable, "*", strWhere);
	   tblDailyNominatedRows.addGroupBy("deal_num");
	   tblDailyNominatedRows.addGroupBy("startdate");
	   tblDailyNominatedRows.addGroupBy("param_seq_num");
	   tblDailyNominatedRows.groupBy();
	   tblDailyNominatedRows.mathABSCol("total_quantity");

	   for(outer_row = 1; outer_row <= tblTradingRows.getNumRows(); outer_row++)
	   {
		   iDealNum = tblTradingRows.getInt("deal_num", outer_row);
		   iStartDate = tblTradingRows.getInt("startdate", outer_row);
		   iEndDate = tblTradingRows.getInt("enddate", outer_row);
		   iParamSeqNum = tblTradingRows.getInt("param_seq_num", outer_row);
		   iLocation = tblTradingRows.getInt("location_id", outer_row);
		   find_matching_daily_nom_record = false;
		   nomQtyTotal = 0;
		   nomVolumeTotal = 0;
		   nomEnergyTotal = 0;

  		   for(daily_nom_row = 1; daily_nom_row <=tblDailyNominatedRows.getNumRows(); daily_nom_row++)
		   {
			   int iDealNumInLoop = tblDailyNominatedRows.getInt("deal_num", daily_nom_row);
			   int iStartDateInLoop = tblDailyNominatedRows.getInt("startdate", daily_nom_row);
			   int iEndDateInLoop = tblDailyNominatedRows.getInt("enddate", daily_nom_row);
			   int iParamSeqNumInLoop = tblDailyNominatedRows.getInt("param_seq_num", daily_nom_row);
			   int iLocationInLoop = tblDailyNominatedRows.getInt("location_id", row);
			   
			   if(iDealNum != iDealNumInLoop)
			   {
			      break;
			   }
			   
			   if(iStartDate != iStartDateInLoop || iEndDate != iEndDateInLoop || iParamSeqNum != iParamSeqNumInLoop)
			   {
				   continue;
			   }
			   
		   	   int iTradingVolumeLocation = getTradingVolumeLocation(tblTradingRows, iDealNumInLoop, iStartDateInLoop, iEndDateInLoop, iParamSeqNumInLoop, iLocationInLoop);

			   if(iLocation != iTradingVolumeLocation)
			   {
				   continue;
			   }
			   
			   find_matching_daily_nom_record = true;
			   double temp_qty = tblDailyNominatedRows.getDouble("total_quantity",daily_nom_row);
			   double temp_volume = tblDailyNominatedRows.getDouble("volume_volumeunit",daily_nom_row);
			   double temp_energy = tblDailyNominatedRows.getDouble("energy",daily_nom_row);
			   nomQtyTotal += temp_qty;
			   nomVolumeTotal += temp_volume;
			   nomEnergyTotal += temp_energy;
		   }
		   
  		   tblTradingRows.setDouble("nominated_quantity", outer_row, nomQtyTotal);
  		   tblTradingRows.setDouble("nominated_volume", outer_row, nomVolumeTotal);
  		   tblTradingRows.setDouble("nominated_energy", outer_row, nomEnergyTotal);
  		   
  		   if(find_matching_daily_nom_record == true)
  		   {
  			   continue; //find the daily nominated records, no need to look for nominated records
  		   }

  		   for(row = 1; row <=tblNominatedRows.getNumRows(); row++)
		   {
			   int iDealNumInLoop = tblNominatedRows.getInt("deal_num", row);
			   int iStartDateInLoop = tblNominatedRows.getInt("startdate", row);
			   int iEndDateInLoop = tblNominatedRows.getInt("enddate", row);
			   int iParamSeqNumInLoop = tblNominatedRows.getInt("param_seq_num", row);
			   int iLocationInLoop = tblNominatedRows.getInt("location_id", row);
			   
			   if(iDealNum != iDealNumInLoop)
			   {
			      break;
			   }
			   
			   if(iStartDate != iStartDateInLoop || iEndDate != iEndDateInLoop || iParamSeqNum != iParamSeqNumInLoop)
			   {
				   continue;
			   }

		   	   int iTradingVolumeLocation = getTradingVolumeLocation(tblTradingRows, iDealNumInLoop, iStartDateInLoop, iEndDateInLoop, iParamSeqNumInLoop, iLocationInLoop);

			   if(iLocation != iTradingVolumeLocation)
			   {
				   continue;
			   }

			   double temp_qty = tblNominatedRows.getDouble("total_quantity",row);
			   double temp_volume = tblNominatedRows.getDouble("volume_volumeunit",row);
			   double temp_energy = tblNominatedRows.getDouble("energy",row);
			   nomQtyTotal += temp_qty;
			   nomVolumeTotal += temp_volume;
			   nomEnergyTotal += temp_energy;
		   }
		   
  		   tblTradingRows.setDouble("nominated_quantity", outer_row, nomQtyTotal);
  		   tblTradingRows.setDouble("nominated_volume", outer_row, nomVolumeTotal);
  		   tblTradingRows.setDouble("nominated_energy", outer_row, nomEnergyTotal);
	   }
	   
	   outputTable.clearDataRows();
	   
	   tblTradingRows.addCol("over_sched_quantity", COL_TYPE_ENUM.COL_DOUBLE);
	   tblTradingRows.addCol("over_sched_volume", COL_TYPE_ENUM.COL_DOUBLE);
	   tblTradingRows.addCol("over_sched_energy", COL_TYPE_ENUM.COL_DOUBLE);
           	    
	   tblTradingRows.mathSubCol( "nominated_quantity", "total_quantity", "over_sched_quantity");
	   tblTradingRows.mathSubCol( "nominated_volume", "volume_volumeunit", "over_sched_volume");
	   tblTradingRows.mathSubCol( "nominated_energy", "energy", "over_sched_energy");
	   
	   strWhere = "over_sched_quantity GT 0.00";
	   outputTable.select( tblTradingRows, "*", strWhere);
	   tblTradingRows.destroy();
	   tblNominatedRows.destroy();
	   return outputTable;
   }
}
