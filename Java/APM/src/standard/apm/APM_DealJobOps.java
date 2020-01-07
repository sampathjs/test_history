/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.ServicesBase;

public class APM_DealJobOps
{
	private APM_Utils m_APMUtils;

	private static APM_DealJobOps self;
	
	private APM_DealJobOps() {
		m_APMUtils = new APM_Utils();
	}

	public static APM_DealJobOps instance() throws OException {
		if (self == null) {
			self = new APM_DealJobOps();
		}

		return self;
	}

	public int APM_EnrichDealTable(int iMode, Table tAPMArgumentTable, int iQueryId) throws OException {
		int iRetVal;
		int row;
	   int iOldPortfolio;
	   
		Table tDealInfoDB, tPortfolios = Util.NULL_TABLE;
		String sWhat, sFrom, sWhere;

		if (iMode == m_APMUtils.cModeBatch || iMode == m_APMUtils.cModeDoNothing)
			return 1;

		m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Starting to enrich deal details");

		Table tDealInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
		
		// get required info
		tDealInfoDB = Table.tableNew("Deal Info");
		tDealInfoDB.addCols("I(trannum), I(secondary_entity_num), I(dealnum), I(primary_entity_num), I(status), I(intpfolio), I(entity_group_id), I(version), I(entity_version), I(personnel_id), T(last_update)");

		// Query onto the database
		sWhat = "ab.tran_num trannum, ab.tran_num secondary_entity_num, ab.deal_tracking_num dealnum, ab.deal_tracking_num primary_entity_num, ab.tran_status status, ab.internal_portfolio intpfolio, ab.internal_portfolio entity_group_id, ab.version_number version, ab.version_number entity_version, ab.personnel_id, ab.last_update";
		sFrom = "ab_tran ab, query_result qr";
		sWhere = "qr.query_result = ab.tran_num AND qr.unique_id = " + iQueryId;
		iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tDealInfoDB, sWhat, sFrom, sWhere);
		
		if (Table.isTableValid(tDealInfoDB) == 0) {
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to enrich deal details");
			return 0;
		} 
		
		if (tDealInfoDB.getNumRows() > 0 )
		{
			if ( tDealInfo.getColNum("intpfolio") < 1)
				tDealInfo.addCol("intpfolio", COL_TYPE_ENUM.COL_INT);
			
			if ( tDealInfo.getColNum("oldintpfolio") < 1)
				tDealInfo.addCol("oldintpfolio", COL_TYPE_ENUM.COL_INT);
			
			if ( tDealInfo.getColNum("actstatus") < 1)
				tDealInfo.addCol("actstatus", COL_TYPE_ENUM.COL_INT);
			
			if ( tDealInfo.getColNum("version") < 1)
				tDealInfo.addCol("version", COL_TYPE_ENUM.COL_INT);			

			if ( tDealInfo.getColNum("personnel_id") < 1)
				tDealInfo.addCol("personnel_id", COL_TYPE_ENUM.COL_INT);			

			if ( tDealInfo.getColNum("last_update") < 1)
				tDealInfo.addCol("last_update", COL_TYPE_ENUM.COL_DATE_TIME);			
				
			if ( tDealInfo.getColNum("personnel_id") < 1)
				tDealInfo.addCol("personnel_id", COL_TYPE_ENUM.COL_INT);			

			if ( tDealInfo.getColNum("primary_entity_num") < 1)
				tDealInfo.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);			

			if ( tDealInfo.getColNum("secondary_entity_num") < 1)
				tDealInfo.addCol("secondary_entity_num", COL_TYPE_ENUM.COL_INT);			

			if ( tDealInfo.getColNum("entity_version") < 1)
				tDealInfo.addCol("entity_version", COL_TYPE_ENUM.COL_INT);			

			if ( tDealInfo.getColNum("entity_group_id") < 1)
				tDealInfo.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);			

			if ( tDealInfo.getColNum("old_entity_group_id") < 1)
				tDealInfo.addCol("old_entity_group_id", COL_TYPE_ENUM.COL_INT);			
				
		}
		else
		{
			// in this instance the deal no longer exists in the db so
			// effectively make the script skip it
			tDealInfoDB.destroy();
			return 1;
		}
		
		if (iRetVal != 0) {
			tDealInfo.select(tDealInfoDB, "intpfolio, status(actstatus), version, personnel_id, last_update, primary_entity_num, secondary_entity_num, entity_version, entity_group_id", "trannum EQ $tran_num");

			for (row = 1; row <= tDealInfo.getNumRows(); row++) 
			{
				// correct the intpfolio status if we are going to deleted, cancelled new or cancelled
				// the pfolio in the db is incorrect (its the old one - not the new one) 
				int to_status = tDealInfo.getInt("to_status", row);
				if ( to_status == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt() ||
					 to_status == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() ||
					 to_status == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED_NEW.toInt() )
				{
					int newPfolio = tDealInfo.getInt("internal_portfolio", row);
					tDealInfo.setInt("intpfolio", row, newPfolio);
				}
				
				// enrich the original deal info from the argt with the portfolio and version for these deals - if in deal booking mode
				iOldPortfolio = 0; // default to no previous portfolio - will be filled in by updatetables if left at zero				
				if ( tDealInfo.getColNum("prev_internal_portfolio") > 0 )
					iOldPortfolio = tDealInfo.getInt( "prev_internal_portfolio", row);
					
				tDealInfo.setInt("oldintpfolio", row, iOldPortfolio);	         
				tDealInfo.setInt("old_entity_group_id", row, iOldPortfolio);	         
			}
		}
		
		if (Table.isTableValid(tPortfolios) != 0)
			tPortfolios.destroy();
		tDealInfoDB.destroy();
		
		if (iRetVal == 0)
			m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Failed to enrich deal details");
		else
			m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Finished enriching deal details");
		
		return iRetVal;
	}

	public int APM_FindLaunchType(Table tMainArgt, Table tAPMArgumentTable) throws OException
	// Analyses the table to decide if it is running in batch, or as an ops
	// service script
	{
		Table tEntityInfo;
		String sProcessingMessage;

		// batch
		if (m_APMUtils.APM_CheckColumn(tMainArgt, "selected_criteria", COL_TYPE_ENUM.COL_TABLE.toInt()) == 1
		        && Table.isTableValid(tMainArgt.getTable("selected_criteria", 1)) == 1) {
			if (m_APMUtils.APM_CheckColumn(tMainArgt, "method_id", COL_TYPE_ENUM.COL_INT.toInt()) == 1 && tMainArgt.getInt("method_id", 1) > 0)
				return (m_APMUtils.cModeBatch);
		}

		// updates
		/*
		 * See if this script is running as an Ops Service and if so in what
		 * context ...
		 */
		if (m_APMUtils.APM_CheckColumn(tAPMArgumentTable, "Global Filtered Entity Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0) {
			tEntityInfo = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1);

			if (Table.isTableValid(tEntityInfo) == 0 || (tEntityInfo.getNumRows() < 1)) {
				// When there is no entity info, return "do nothing"
				// This can happen where it is an internal deal, and APM is not
				// interested in this side's portfolio
				return m_APMUtils.cModeDoNothing;
			}

			// handle deal mode - this also handles/logs errors if mode unknown for deals
			return APM_FindDealLaunchType(tMainArgt, tAPMArgumentTable);
		}

		// if we get this far we're confused !
		sProcessingMessage = "ERROR: Unknown APM Script Mode !!!\n";
		m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, sProcessingMessage);

		return (m_APMUtils.cModeUnknown);
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_FindDealLaunchType
	Description:   This function determines run mode for each deal update

	-------------------------------------------------------------------------------*/
	public int APM_FindDealLaunchType(Table tMainArgt, Table tAPMArgumentTable) throws OException {
		Table tDealInfo;
		Table tOpSvcDefn, tOpsCriteria = Util.NULL_TABLE, tTranStatus, tArgtDealInfo;
		int iToStatus, iFromStatus;
		int iDealRow;
		int iDealMode = 0;
		int iNumDealRows;
		int iBlockUpdates = 0;
		String sProcessingMessage;

		// in this instance look to see if we are getting a block from the argt
		// a block of 1 deal is the same as a block of n deals even if the filtered
		// table has 1 compared to n
		tArgtDealInfo = tMainArgt.getTable("Deal Info", 1);
		tDealInfo = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1);

		if (Table.isTableValid(tArgtDealInfo) == 0 || (tArgtDealInfo.getNumRows() < 1)) {
			// When there is no deal info, return "do nothing"
			// This can happen where it is an internal deal, and APM is not
			// interested in this side's portfolio
			return m_APMUtils.cModeDoNothing;
		}

		tOpSvcDefn = tMainArgt.getTable("Operation Service Definition", 1);

		// if we've got more than one deal enter block mode, whether endur cut
		// will give us a block or not, internal deals could also give > 1 deal
		// use the argt deal info to decide whether we have a block but then add col to filtered tran info
		iNumDealRows = tArgtDealInfo.getNumRows();
		if (iNumDealRows > 1) {
			if (tDealInfo.getColNum("update_mode") < 1)
				tDealInfo.addCol("update_mode", COL_TYPE_ENUM.COL_INT);
			iBlockUpdates = 1;
		}

		int iOpsCriteriaColNum = tOpSvcDefn.getColNum("criteria_types_checked");
		if (iOpsCriteriaColNum > 0)
			tOpsCriteria = tOpSvcDefn.getTable(iOpsCriteriaColNum, 1);
		tTranStatus = Util.NULL_TABLE;
		if (Table.isTableValid(tOpsCriteria) != 0) {
			int iTranStatusRow = tOpsCriteria.unsortedFindInt("criteria_type", 4); // tran statuses
			tTranStatus = tOpsCriteria.getTable("selected", iTranStatusRow);
		}

		iNumDealRows = tDealInfo.getNumRows(); // reset to actual number of rows

		// for each deal (or only one if we're not doing a block update)
		for (iDealRow = 1; iDealRow <= iNumDealRows; iDealRow++) {
			iDealMode = 0; // reset the dealmode

			iToStatus = tDealInfo.getInt("to_status", iDealRow);
			iFromStatus = tDealInfo.getInt("from_status", iDealRow);

			/* make sure we are processing an adjustment we really want
			 * for instance we don't want to process a schedule change on an
			 * amended new trade unless we actually care about amended new status */
			if (Table.isTableValid(tTranStatus) != 0) {
				/* if we are moving to an internal status (>= 10000)
				 * then we don't want to do anything if the original status is not of interest
				 * i.e. is not in the tran status list */
				if (iToStatus >= 10000 && tTranStatus.unsortedFindInt("id", iFromStatus) < 1)
					iDealMode = m_APMUtils.cModeDoNothing;

				// we also do not want to do anything if we are at cancelled new status and cancelled new is not being monitored
				// otherwise the position moves from validated to cancelled new in the APM pages
				if (iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED_NEW.toInt() && tTranStatus.unsortedFindInt("id", iToStatus) < 1)
					iDealMode = m_APMUtils.cModeDoNothing;
			}

			/* backout if a deal exists */
			if (iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_TEMPLATE.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_SPLIT_CLOSED.toInt()
			        || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_GIVEUP_CLOSED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED_CLOSED.toInt()
			        || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
				iDealMode = m_APMUtils.cModeBackout;
			}

			/*
			 * NB. Cancellation has been added in here so that we can get cell
			 * updates in APM
			 */
			if (iDealMode == 0
			        && (iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_ROLLOVER_NEW.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_BUYOUT_SPLIT_NEW.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED.toInt() /* must include amended status for 2 step amends */
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED_NEW.toInt() /* must include amended new status in case people add amended new to list of monitored statuses */
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_BUYOUT.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt()
			                || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() || iToStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED_NEW.toInt())) {
				iDealMode = m_APMUtils.cModeBackoutAndApply;
			}

			/*
			 * Internal statuses - got greater than equal test to avoid having
			 * to add when new internal statuses added into Endur (has tripped
			 * us up - causes bad updates)
			 */
			if (iDealMode == 0 && iToStatus >= 10000) {
				iDealMode = m_APMUtils.cModeBackoutAndApply;
			}

			if (iDealMode != 0) {
				if (iBlockUpdates != 0)
					tDealInfo.setInt("update_mode", iDealRow, iDealMode);
				continue;
			}

			// log an error and return as soon as we get an unknown run type as
			// we can't guarantee the whole set of deals
			tDealInfo.setInt("update_mode", iDealRow, m_APMUtils.cModeUnknown);
			sProcessingMessage = "ERROR: Unknown APM Script Mode !!!";
			sProcessingMessage = sProcessingMessage + " Deal Number: " + tDealInfo.getInt("deal_tracking_num", iDealRow);
			sProcessingMessage = sProcessingMessage + " Tran Number: " + tDealInfo.getInt("tran_num", iDealRow);
			sProcessingMessage = sProcessingMessage + " From Status: " + iFromStatus;
			sProcessingMessage = sProcessingMessage + " To Status: " + iToStatus;
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, sProcessingMessage);
			return m_APMUtils.cModeUnknown;
		}

		// if we've got block updates return that as the run mode, if not just
		// return the standard run mode for the single deal
		if (iBlockUpdates == 1)
			return m_APMUtils.cModeBlockUpdate;
		else
			return iDealMode;

	}
	
	/*-------------------------------------------------------------------------------
	Name:          APM_AdjustLaunchType
	Description:   This function determines if a script is in apply type modes
	           whether we actually really care about the insert - it may be to a portfolio
	           that the APM service doesn't actually monitor

	Parameters:   <any parameters it accepts>
	Return Values: The function returns an adjusted mode
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	public int APM_AdjustLaunchType(int mode, Table tAPMArgumentTable, int portfolio) throws OException {
		String iTranStr = "\n";
		int new_intpfolio, old_intpfolio, new_foundrow, old_foundrow, row = 0, actual_status;
		int iOrigMode, tran_num;
		Table tDealInfo = Util.NULL_TABLE;
		QueryRequest qreq = null;
		Table queryTrans = Util.NULL_TABLE;
		int iQueryId = 0;
		
		// save the original mode so if we're in block update mode individual
		// deal modes don't overwrite it
		iOrigMode = mode;

		/*
		 * finally check that the insert is in a portfolio that we actually care
		 * about for deal booking
		 */
		if (mode != m_APMUtils.cModeBatch) {

			Table tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
			tDealInfo = tAPMArgumentTable.getTable("Filtered Entity Info", 1); 
			Table tOpsSvcDefn = tMainArgt.getTable("Operation Service Definition", 1);
			Table opsCriteria = tOpsSvcDefn.getTable("ops_criteria", 1);
			Table portfolioTable = Table.tableNew();
			portfolioTable.select(opsCriteria, "criteria_value", "criteria_category EQ 1019");
			portfolioTable.sortCol(1);
			
			if (tDealInfo.getColNum("intpfolio") < 1) {
				// implies there was no data in the db for this deal(s)...therefore change mode to do nothing
				m_APMUtils.APM_PrintDebugMessage(tAPMArgumentTable, "Deal(s) no longer exist in database. Skipping.");
				return m_APMUtils.cModeDoNothing;
			}

			// if theres a saved query we need to check against it later on to see if deal updates still in query
			boolean savedQueryOnService = false;			
			queryTrans = Table.tableNew();
			if ( tMainArgt.getColNum("query_name") > 0 )
			{
				String query_name = tMainArgt.getString("query_name", 1);
				if(Str.equal(query_name, "None") != 1)
				{
				   savedQueryOnService = true;
				   qreq = APM_ExecuteDealQuery.instance().createQueryIdFromMainArgt(mode, tAPMArgumentTable, tMainArgt, portfolio);
				   iQueryId = qreq.getQueryId();
				   m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, queryTrans, "query_result", "query_result", "unique_id = " + iQueryId + " order by query_result");
				}				
			}
				
			/*
			 * cycle around the deal info & for the current pfolio adjust the
			 * mode
			 */
			/* there will be only one deal entry for every portfolio */
			/*
			 * you only get more than 1 entry in the dealinfo at all if its an
			 * internal deal or a block
			 */
			for (row = 1; row <= tDealInfo.getNumRows(); row++) {
				new_intpfolio = tDealInfo.getInt("intpfolio", row);
				old_intpfolio = tDealInfo.getInt("oldintpfolio", row);
				actual_status = tDealInfo.getInt("actstatus", row);
				tran_num = tDealInfo.getInt("tran_num", row);

				// check whether the new portfolio is in the criteria being monitored
				// deal could be moving out of monitored list
				new_foundrow = portfolioTable.findInt(1, new_intpfolio, com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
				if (new_foundrow > 0 )
					new_foundrow = 1;
				else
					new_foundrow = 0;

				/* make sure we match up the correct row */
				if (new_intpfolio != portfolio)
						continue;

				/*
				 * set here as this is the first time we actually identify the
				 * current trannum
				 */
				if (iOrigMode == m_APMUtils.cModeBlockUpdate) {
					// build up a tran num String for the block of deals
					// tTranNums = Table.tableNew("Tran Nums");
					// tTranNums.select(tDealInfo,"tran_num", "1 EQ 1");
					// tTranNums.makeTableUnique();
					// for (iTranRow=1; iTranRow
					// <=tTranNums.getNumRows();iTranRow++)
					// {
					if ((row > 1) && (Str.len(iTranStr) > 0))
						iTranStr = iTranStr + "\n";
					iTranStr = iTranStr + " " + Str.intToStr(tran_num);
					// }

					// get the mode for this deal
					mode = tDealInfo.getInt("update_mode", row);
				} else {
					// set the trans context
					ConsoleLogging.instance().setSecondaryEntityNumContext(tAPMArgumentTable, tran_num);
					
					int dealNumber = tDealInfo.getInt("deal_tracking_num", row);
					ConsoleLogging.instance().setPrimaryEntityNumContext(tAPMArgumentTable, dealNumber);
					
					int dealVersion = tDealInfo.getInt("version", row);
					ConsoleLogging.instance().setEntityVersionContext(tAPMArgumentTable, dealVersion);
					
					String oldPortfolioName = Table.formatRefInt(old_intpfolio, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
					ConsoleLogging.instance().setPreviousEntityGroupContext(tAPMArgumentTable, oldPortfolioName, old_intpfolio);
				}

		         // if old pfolio = -1 then set it to current pfolio
		         // we can't say for sure what the old pfolio was at this point 
		         // have to wait until the update tables.
		         // BUT we need to make sure we have an apply and backout
		         if ( old_intpfolio == -1 )
		            old_intpfolio  = new_intpfolio;
				
				if (old_intpfolio == 0) {
					/*
					 * must be an apply if its anything as there was nothing
					 * already in the APM tables
					 */
					/*
					 * - UNLESS its being deleted at the same time as its moved
					 * into a portfolio thats being monitored
					 */
					/* if we don't care about the new pfolio then do nothing */
					// if backout & no old pfolio then must be doing nothing
					if (new_foundrow < 1 || mode == m_APMUtils.cModeBackout) 
						mode = m_APMUtils.cModeDoNothing;
					else
					{
						//
						//  Opening APM and putting a page online during dealupdate could in some
						//  cases result in double counting deal update and showing incorrect
						//  numbers in APM. To avoid this issue (see DTS 94151: SR85942: APM client - differing deal update issue on two identical APM pages (two APM sessions)
						// changing:
						//  mode = m_APMUtils.cModeApply;
						// to:
						mode = m_APMUtils.cModeBackoutAndApply;
					}
				} else {
					// check whether the old portfolio is in the criteria being monitored
					// deal could be moving into monitored list					
					old_foundrow = portfolioTable.findInt(1, old_intpfolio, com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
					if (old_foundrow > 0)
						old_foundrow = 1;
					else
						old_foundrow = 0;

					if (new_foundrow >= 1) {
						if (mode != m_APMUtils.cModeBackout)
						{
							/*
							 * must be a backout & apply as we are interested in
							 * the new pfolio
							 */
							mode = m_APMUtils.cModeBackoutAndApply;
						}
					} else if ( old_foundrow >= 1 || tAPMArgumentTable.getInt( "Previous Entity Group Id", 1) == -1 ) {
						/* moving out of monitored portfolio */
						mode = m_APMUtils.cModeBackout;
					} else {
						/* not found for either new or old - we don't care */
						mode = m_APMUtils.cModeDoNothing;
					}

					/*
					 * NB. TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED has been
					 * removed from below so that we can get cell udpates for
					 * cancellations in APM
					 */
					/*
					 * finally override for certain situations where the normal
					 * rules don't apply
					 */
					/*
					 * specifically when a new internal deal has its
					 * counterparty changed
					 */
					if (actual_status == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt() && mode != m_APMUtils.cModeDoNothing)
						mode = m_APMUtils.cModeBackout;
				}

				// if theres a saved query then check whether the deal falls into it
				if ( mode == m_APMUtils.cModeBackoutAndApply && savedQueryOnService )
				{
					if ( queryTrans.findInt(1, tran_num, SEARCH_ENUM.FIRST_IN_GROUP) < 1 )
					   mode = m_APMUtils.cModeBackout;
				}
				
				// if we're in block mode save the mode for each deal
				if (iOrigMode == m_APMUtils.cModeBlockUpdate) {
					tDealInfo.setInt("update_mode", row, mode);
				} else {
					/*
					 * will only be 1 row that matches this portfolio if not in
					 * block mode
					 */
					break;
				}

				if (mode == m_APMUtils.cModeDoNothing)
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Not interested in update for tran number : " + tDealInfo.getInt("tran_num", row));	
			}
		}

		// if we're in deal block mode restore the mode back to block mode so we
		// don't enter into the mode of the last deal in the table
		if (iOrigMode == m_APMUtils.cModeBlockUpdate) {
			ConsoleLogging.instance().setSecondaryEntityNumContext(tAPMArgumentTable, "Block");
			APM_PrintAllDealInfo(tAPMArgumentTable, tAPMArgumentTable.getTable("Main Argt", 1), 
										tAPMArgumentTable.getTable("Filtered Entity Info", 1),"");
			return m_APMUtils.cModeBlockUpdate;
		} else if (iOrigMode != m_APMUtils.cModeBatch)
		{
			if ( row > tDealInfo.getNumRows() )
			{
				// this scenario occurs when a deal is booked, and then moves pfolio before the first incremental update is processed
				// so the loop above fails to find the right row in the dealinfo table
				// in this case do nothing as the later update supercedes this one
				// the effect is a missed update until the second incremental is processed 
				// but the second one should be processed and it is more important to reflect the current state anyway
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Deal moved portfolio.  Update old.  Not interested in update for tran number : " + tDealInfo.getInt("tran_num", 1));
				mode = m_APMUtils.cModeDoNothing;				
			} else {
				int tranNumber = tDealInfo.getInt("tran_num", row);
				ConsoleLogging.instance().setSecondaryEntityNumContext(tAPMArgumentTable, tranNumber);
				int versionNumber = tDealInfo.getInt("version", row);
				ConsoleLogging.instance().setEntityVersionContext(tAPMArgumentTable, versionNumber);
			}

				
			APM_PrintAllDealInfo(tAPMArgumentTable,tAPMArgumentTable.getTable("Main Argt", 1), 
										tAPMArgumentTable.getTable("Filtered Entity Info", 1),"");			
			
		}

		queryTrans.destroy();	
		if ( qreq != null ) // this will only be set if we have the new V11 where we execute the query
		{
			if ( iQueryId > 0 )					
				Query.clear(iQueryId);
			qreq.destroy(); 
		}			
		
		return mode;
	}

	public void APM_PrintAllDealInfo(Table tAPMArgumentTable, Table argt, Table tEntityInfo, String header) throws OException {

		if (Table.isTableValid(tAPMArgumentTable) == 0) {
			OConsole.oprint("Invalid argument table in PrintAllEntityInfo()\n");
			return;
		}

		if (Table.isTableValid(tEntityInfo) == 0) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Invalid entity info table in PrintAllEntityInfo()\n");
			return;
		}

		int numRows = tEntityInfo.getNumRows();

		if (numRows < 1) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Empty entity info table in PrintAllEntityInfo()");
			return;
		}

		for (int row = 1; row <= numRows; row++) {
			APM_PrintDealInfoRow(tAPMArgumentTable, argt, tEntityInfo, header, row);
		}
	}
	
	/*-------------------------------------------------------------------------------
	Name:          APM_CreateQueryIDForPfolio
	Description:  creates a query ID that matches pfolio deals 
	-------------------------------------------------------------------------------*/
	public int APM_CreateQueryIDForPfolio(int mode, Table tAPMArgumentTable, int currPfolio) throws OException {

	   if (mode == m_APMUtils.cModeBatch)
	      return 0;

	   int pfolioQueryID;

	   m_APMUtils.APM_DestroyJobQueryID(mode, tAPMArgumentTable);

	   Table pfolioFilteredDeals = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
	   Table tranNumList = Table.tableNew();
      tranNumList.select(pfolioFilteredDeals, "tran_num", "tran_num GT 0");

	   // set a query ID that matches this
	   pfolioQueryID = 0;
	   if ( tranNumList.getNumRows() > 0 )
	   {
	      pfolioQueryID = m_APMUtils.APM_TABLE_QueryInsertN(tAPMArgumentTable, tranNumList, "tran_num");
	      tAPMArgumentTable.setInt("job_query_id", 1, pfolioQueryID);	   
	   }
	   else
	   {
	      if ( pfolioFilteredDeals.getNumRows() < 1 )
	         m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Skipping update. No currently valid deals in argt for portfolio " + Table.formatRefInt(currPfolio, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE) + " ...");
	   }

	   tranNumList.destroy();
	   return pfolioQueryID;
    }

	public int RemoveDealUpdateBackoutsFromQueryID(int mode, Table tAPMArgumentTable, int currPfolio, int pfolioQueryID)  throws OException {
		
		if ( mode == m_APMUtils.cModeBlockUpdate)
		{
		   Table pfolioFilteredDeals = tAPMArgumentTable.getTable("Filtered Entity Info", 1);
		   Table tranNumList = Table.tableNew();
		   tranNumList.select(pfolioFilteredDeals, "tran_num", "update_mode NE 3"); // no backouts - not needed for sim
		   if ( tranNumList.getNumRows() > 0 && tranNumList.getNumRows() != pfolioFilteredDeals.getNumRows() )
		   {
			   m_APMUtils.APM_DestroyJobQueryID(mode, tAPMArgumentTable);
		      pfolioQueryID = m_APMUtils.APM_TABLE_QueryInsertN(tAPMArgumentTable, tranNumList, "tran_num");			   
		      tAPMArgumentTable.setInt("job_query_id", 1, pfolioQueryID);	   
		   }	
		}
		return pfolioQueryID;
	}

	public int APM_GetSelectedPortfolios(int iMode, Table tAPMArgumentTable) throws OException {
		Table tEntityInfo;
		Table tPortfolios;
		Table tSelectedCriteria;
		Table tSelected;
		Table tMainArgt;
		int iRetVal = 1;
		int iRow;

		tPortfolios = Table.tableNew();
		tPortfolios.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);

		tMainArgt = tAPMArgumentTable.getTable("Main Argt", 1);
		if (iMode == m_APMUtils.cModeBatch) {
			tSelectedCriteria = tMainArgt.getTable("selected_criteria", 1);

			String criteriaTableColName = "filter_table";
			if (tSelectedCriteria.getColNum("criteria_table") > 0)
				criteriaTableColName = "criteria_table";

			String criteriaTypeColName = "filter_type";
			if (tSelectedCriteria.getColNum("criteria_type") > 0)
				criteriaTypeColName = "criteria_type";

			if (tSelectedCriteria.getColNum(criteriaTypeColName) > 0)
				iRow = tSelectedCriteria.unsortedFindInt(criteriaTypeColName, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE.toInt());
			else
				iRow = tSelectedCriteria.unsortedFindString("criteria_name", "Internal Portfolio", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			if (iRow > 0) {
				if (tSelectedCriteria.getColNum(criteriaTableColName) > 0) {
					tSelected = tSelectedCriteria.getTable(criteriaTableColName, iRow);
					tPortfolios.select(tSelected, "id(entity_group_id)", "id GT 0");
				} else {
					tSelected = tSelectedCriteria.getTable("selected", iRow);
					tPortfolios.select(tSelected, "id_number(entity_group_id)", "id_number GT 0");
				}
			}
		} else {
			// use the global table as we are outside the pfolio loop
			tEntityInfo = tAPMArgumentTable.getTable("Global Filtered Entity Info", 1);
			if (tEntityInfo.getColNum("internal_portfolio") > 0) {
				tPortfolios.select(tEntityInfo, "DISTINCT, internal_portfolio (entity_group_id)", "internal_portfolio GT 0");
			}
		}

		if (iRetVal == 1 && (tPortfolios.getNumRows() < 1)) {
			// If we can't find find any portfolios for a batch, fail the batch
			if (iMode == m_APMUtils.cModeBatch) {
				iRetVal = 0;
				m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "No portfolios specified in APM service Selected Criteria. Exiting ...");
			} else {
				// For an update, just log an error message but don't set
				// the error as otherwise the deal will retry forever
				m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "No portfolios found for this deal update");
			}
		} else {
			tPortfolios.makeTableUnique();
			tAPMArgumentTable.setTable("Selected Entity Groups", 1, tPortfolios);
		}

		return (iRetVal);
	}

	public void APM_PrintDealInfoRow(Table tAPMArgumentTable, Table argt, Table tDealInfo, String header, int row) throws OException {

		int dealNum = 0;
		int tranNum = 0;
		int runId;
		int pfolio, oldPfolio, expDefnId = -1;
		String version = "", fromStatus = "", toStatus = "", tranMessage = "";

		if (Table.isTableValid(tAPMArgumentTable) == 0) {
			OConsole.oprint("Invalid argument table in PrintDealInfo()\n");
			return;
		}

		if (Table.isTableValid(tDealInfo) == 0) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Invalid deal info table in PrintDealInfo()");
			return;
		}

		if ((tDealInfo.getNumRows() < 1)) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Empty deal info table in PrintDealInfoRow()");
			return;
		}

		if ((tDealInfo.getNumRows() < row)) {
			m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Invalid row passed to PrintDealInfoRow(). Passed " + row);
			return;
		}

		int dealNumCol = tDealInfo.getColNum("deal_tracking_num");
		int tranNumCol = tDealInfo.getColNum("tran_num");
		int tranVersionCol = tDealInfo.getColNum("version");
		int runIdCol = tDealInfo.getColNum("op_services_run_id");
		int fromStatusCol = tDealInfo.getColNum("from_status");
		int toStatusCol = tDealInfo.getColNum("to_status");
		int pfolioCol = tDealInfo.getColNum("intpfolio");
		int oldPfolioCol = tDealInfo.getColNum("oldintpfolio");
		int opsDefnCol = argt.getColNum("Operation Service Definition");

		dealNum = tDealInfo.getInt(dealNumCol, row);
		tranNum = tDealInfo.getInt(tranNumCol, row);
		runId = tDealInfo.getInt(runIdCol, row);
		fromStatus = Table.formatRefInt(tDealInfo.getInt(fromStatusCol, row), SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);

		// to status can be an internal status (from status is always a proper status)
		if (tDealInfo.getInt(toStatusCol, row) >= 10000)
			toStatus = Table.formatRefInt(tDealInfo.getInt(toStatusCol, row), SHM_USR_TABLES_ENUM.TRAN_STATUS_INTERNAL_TABLE);
		else
			toStatus = Table.formatRefInt(tDealInfo.getInt(toStatusCol, row), SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);

		if (opsDefnCol > 0)
			expDefnId = argt.getTable(opsDefnCol, 1).getInt("exp_defn_id", 1);

		// version number may not be available
		if (tranVersionCol > 0)
			version = ", Version: " + Str.intToStr(tDealInfo.getInt(tranVersionCol, row));

		// build message
		tranMessage += header + "DealNum: " + dealNum + ",  TranNum: " + tranNum + version + ",  RunId: " + runId + ", ExpDefnId: " + expDefnId + ", Status: "
		        + fromStatus + " to " + toStatus;

		// only print portfolios if a transition occured otherwise it's spam
		if (pfolioCol > 0 && oldPfolioCol > 0) {

			pfolio = tDealInfo.getInt(pfolioCol, row);
			oldPfolio = tDealInfo.getInt(oldPfolioCol, row);

			if ((pfolio != oldPfolio) && oldPfolio > 0) {
				tranMessage += ", PfolioChange: " + Table.formatRefInt(tDealInfo.getInt(oldPfolioCol, row), SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE) + " to "
				        + Table.formatRefInt(tDealInfo.getInt(pfolioCol, row), SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
			}
		}

		m_APMUtils.APM_PrintMessage(tAPMArgumentTable, tranMessage);
	}
	
	public Table APM_RetrieveDealInfoCopyFromArgt(Table tAPMArgumentTable, Table argt) throws OException {
		Table dealInfo;
		Table opServicesLog;
		dealInfo = argt.getTable("Deal Info", 1).copyTable();

		// If a single deal update we need to go into the op_services_log table to get the op services run id.
		// For block update we should have the id already (also tables don't match so can't do join anyway!)
		if (dealInfo.getNumRows() == 1) {
			opServicesLog = argt.getTable("op_services_log", 1);
			if (Table.isTableValid(opServicesLog) == 1) {
				if (opServicesLog.getNumRows() > 1) {
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "More than one row in op_services_log for single deal update. Taking first entry!");
				}

				String runIDColName = "run_id";
				if (opServicesLog.getColNum("op_services_run_id") > 0)
					runIDColName = "op_services_run_id";
				dealInfo.setInt("op_services_run_id", 1, opServicesLog.getInt(runIDColName, 1));
			}
		}
		return dealInfo;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_FilterDealInfoTable
	Description:  Removes any duplicates from the deal info block so we don't 
	              process the same deal multiple times (no point)
	-------------------------------------------------------------------------------*/
	public int APM_FilterDealInfoTable(Table tAPMArgumentTable, Table argt, Table tDealInfo) throws OException {

		// get rid of multiple lines for the same transaction - we only want to process the latest
		// note we are operating on a copy of the argt dealinfo - not the original !
		int row, dealRow, dealNo, opsRunID;

		Table uniqueDealNums = Table.tableNew();
		uniqueDealNums.select(tDealInfo, "DISTINCT, deal_tracking_num", "deal_tracking_num GT 0");
		for (row = 1; row <= uniqueDealNums.getNumRows(); row++) {
			dealNo = uniqueDealNums.getInt("deal_tracking_num", row);
			opsRunID = 0;

			// identify the highest ops run ID (i.e. latest entry) for this deal
			for (dealRow = 1; dealRow <= tDealInfo.getNumRows(); dealRow++) {
				if (dealNo == tDealInfo.getInt("deal_tracking_num", dealRow) && tDealInfo.getInt("op_services_run_id", dealRow) > opsRunID) {
					opsRunID = tDealInfo.getInt("op_services_run_id", dealRow);
				}
			}

			// now cycle through the deal info table and remove the older duplicates
			for (dealRow = tDealInfo.getNumRows(); dealRow >= 1; dealRow--) {
				if (dealNo == tDealInfo.getInt("deal_tracking_num", dealRow)) {
					if (tDealInfo.getInt("op_services_run_id", dealRow) < opsRunID) {
						APM_PrintDealInfoRow(tAPMArgumentTable, argt, tDealInfo, "Removing deal with older ops service run id: ", dealRow);
						tDealInfo.delRow(dealRow);
					}
				}
			}
		}

		// check that there is only 1 row per deal
		if (uniqueDealNums.getNumRows() < tDealInfo.getNumRows()) {
			// grrr...why aren't we unique - have to do it manually now and just blow away rows until we are unique
			uniqueDealNums.addCol("found_flag", COL_TYPE_ENUM.COL_INT);
			uniqueDealNums.sortCol(1);
			for (dealRow = tDealInfo.getNumRows(); dealRow >= 1; dealRow--) {
				row = uniqueDealNums.findInt(1, tDealInfo.getInt("deal_tracking_num", dealRow), com.olf.openjvs.enums.SEARCH_ENUM.FIRST_IN_GROUP);
				if (uniqueDealNums.getInt(2, row) == 0)
					uniqueDealNums.setInt(2, row, 1); // not found already, set the found flag
				else {
					APM_PrintDealInfoRow(tAPMArgumentTable, argt, tDealInfo, "Removing tran due to duplicate dealnum: ", dealRow);
					tDealInfo.delRow(dealRow); // found already, delete
				}
			}
		}

		uniqueDealNums.destroy();

		return 1;
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_ReEvaluateUndoAmendedNewTrans
	Description:  re-evaluates any trades at amended new that are rolled back using undo amend
	              It works out the prior tran and resubmits it.
	-------------------------------------------------------------------------------*/
	public int APM_ReEvaluateUndoAmendedNewTrans(Table tAPMArgumentTable, Table argt, Table tDealInfo) throws OException {

		int row, newRow;
		Table undoAmendTranList = Table.tableNew("Undo Amend list");
		undoAmendTranList.addCol("deal_tracking_num", COL_TYPE_ENUM.COL_INT);

		// work out what trades are at amended new and are being rolled back
		for (row = 1; row <= tDealInfo.getNumRows(); row++) {
			if (tDealInfo.getInt("from_status", row) == TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED_NEW.toInt()
			        && tDealInfo.getInt("to_status", row) == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) {
				newRow = undoAmendTranList.addRow();
				undoAmendTranList.setInt(1, newRow, tDealInfo.getInt("deal_tracking_num", row));
			}
		}

		if (undoAmendTranList.getNumRows() == 0) {
			undoAmendTranList.destroy();
			return 1;
		}

		int newQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, undoAmendTranList, 1);

		// work out the latest validated tran nums associated with the deals having an undo amend undertaken 
		Table validatedtranList = Table.tableNew("Validated Tran list");
		int iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, validatedtranList, "max(ab.tran_num), ab.deal_tracking_num", "ab_tran ab, query_result q", "q.unique_id = "
		        + newQueryId + " and ab.deal_tracking_num = q.query_result and ab.tran_status = 3 group by ab.deal_tracking_num");

		// now send all the latest validated transactions into the re-evaluation API
		int validatedQueryId = 0;
		if (iRetVal != 0 && validatedtranList.getNumRows() > 0) {
			validatedQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, validatedtranList, 1);
			TranOpService.reEvaluateTrades(validatedQueryId);
		}

		// cleanup
		Query.clear(newQueryId);
		Query.clear(validatedQueryId);
		validatedtranList.destroy();
		undoAmendTranList.destroy();
		return 1;
	}

	public void SetDealBlockMode(int iMode, Table argt) throws OException {

		/* fix the argt to enable block updates if supported by endur cut */
		if (((iMode == m_APMUtils.cModeApply) || (iMode == m_APMUtils.cModeBackout) || (iMode == m_APMUtils.cModeBackoutAndApply))) {
			int dealGroupingColNum = argt.getColNum("deal_grouping");
			if (dealGroupingColNum > 0)
				argt.setInt("deal_grouping", 1, 1);
		}

	}

	public boolean SetDealArgtReturnStatus(Table argt, int status) throws OException {

		Table tDealInfo = argt.getTable("Deal Info", 1);
		if (Table.isTableValid(tDealInfo) == 1) {
		   tDealInfo.setInt("log_status", 1, OP_SERVICES_LOG_STATUS.OP_SERVICES_LOG_STATUS_FAILED.toInt());
		   return true;
		}

		return false;
	}

	public void SetBlockUpdateStatuses(Table tMainArgt, Table blockFails) throws OException {

		Table tDealInfo = tMainArgt.getTable("Deal Info",1);
		tDealInfo.select(blockFails, "log_status", "tran_num EQ $tran_num AND op_services_run_id EQ $op_services_run_id");
	}

	public Table GetDatasetKeysForService(Table tAPMArgumentTable, int serviceID, String serviceName) throws OException {

		Table pfield_tbl = Services.getServiceMethodProperties(serviceName, "ApmService");

		Table cached_table = null;
		if (pfield_tbl.unsortedFindString("pfield_name", "portfolio_id", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
			cached_table = Services.getMselPicklistTable(pfield_tbl, "portfolio_id");
		else
			cached_table = Services.getMselPicklistTable(pfield_tbl, "internal_portfolio");

		if (cached_table == null)
			return null;

		Table pfolio_list = cached_table.copyTable();
		// make sure the portfolio col name is obvious
		if (pfolio_list.getColNum("id") > 0)
			pfolio_list.setColName("id", "entity_group_id");

		pfolio_list.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
		pfolio_list.addCol("service_id", COL_TYPE_ENUM.COL_INT);
		pfolio_list.addCol("temp_int", COL_TYPE_ENUM.COL_INT);

		pfolio_list.setColValInt("service_id", serviceID);

		int datasetType = 0;
		if (pfield_tbl.unsortedFindString("pfield_name", "dataset_type_id", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0) {
			datasetType = Services.getPicklistId(pfield_tbl, "dataset_type_id");
			pfolio_list.setColValInt("dataset_type_id", datasetType);
		}

		int pkgRow = pfield_tbl.unsortedFindString("pfield_name", "package_name", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		if (pkgRow > 0) {
			Table packageTable = Services.getMselPicklistTable(pfield_tbl, "package_name");
			pfolio_list.select(packageTable, "value(package)", "id GE $temp_int");
		}

		pfolio_list.delCol("temp_int");
		pfield_tbl.destroy();
		return pfolio_list;
	}

	public int GetUnderlyingSimResultRow(int iMode, Table tAPMArgumentTable, String sDataTableName, Table tPackageDataTableCols) throws OException {

		   int iUnderlyingResultRow = tPackageDataTableCols.unsortedFindString( "column_name", "dealnum", SEARCH_CASE_ENUM.CASE_SENSITIVE);
		   if (iUnderlyingResultRow < 1)
		   {
			   iUnderlyingResultRow = tPackageDataTableCols.unsortedFindString( "column_name", "index_id", SEARCH_CASE_ENUM.CASE_SENSITIVE);

			   if (iUnderlyingResultRow < 1)
			   {
				   String sErrMessage = "USER_APM_FillDataTableFromResults failed to identify underlying simulation result for table '" + sDataTableName + "'" ;
				   tAPMArgumentTable.setString( "Error String", 1,  sErrMessage);
			 	   m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, sErrMessage);
				   return 0;
			   }
		   }

		return iUnderlyingResultRow;

	}

	public int CheckSimResultsValid(int iMode, Table tAPMArgumentTable, Table tResults) throws OException {

		int iAPMDealInfoResultID = 0;
		int iRetVal = 1;
		if( Table.isTableValid(tResults) != 0 )
		{
				   Table tScenResult = tResults.getTable( "scenario_results", 1);
				   Table tGenResult = tScenResult.getTable( 1, 4);
				   iAPMDealInfoResultID = tGenResult.unsortedFindInt( "result_type", SimResult.getResultIdFromEnum("USER_RESULT_APM_DEAL_INFO"));
				   tGenResult = Util.NULL_TABLE;
		}
		
		if (Table.isTableValid(tResults) != 0  && iAPMDealInfoResultID < 1)
		{
					// this can be valid when doing a one step cancellation
					// in certain cuts the sim returns nothing for entity info
					m_APMUtils.APM_PrintMessage( tAPMArgumentTable, "No results returned from Sim.runRevalByParamFixed");
					iRetVal = 1;
		}
		else if (Table.isTableValid(tResults) == 0 )
		{
					String sErrMessage = "No results valid from Sim.runRevalByParamFixed";
					m_APMUtils.APM_PrintAndLogErrorMessage( iMode, tAPMArgumentTable, sErrMessage);
					iRetVal = 0;
		}
		return iRetVal;
	}

	public Table EnrichSimulationResults(Table tResults, int iType, int iResultClass, String sWhat) throws OException {
		int iInsTypeCol, iDiscIdxCol, iProjIdxCol;
		int iInsType, iDiscIdx, iProjIdx;
		Table tSelectedResult = Util.NULL_TABLE;
		int iStartGroup, iEndGroup;
		int iNumRows, x, iTotalRows, iRow;

		if (tResults.getNumRows() > 0)
		{
			if (iResultClass == RESULT_CLASS.RESULT_GEN.toInt())
			{ 
				iStartGroup = tResults.findInt( RESULT_GEN_COL_ENUM.GEN_RESULT_TYPE.toInt(), iType, SEARCH_ENUM.FIRST_IN_GROUP);
				if (iStartGroup > 0)
				{
					iEndGroup   = tResults.findInt( RESULT_GEN_COL_ENUM.GEN_RESULT_TYPE.toInt(), iType, SEARCH_ENUM.LAST_IN_GROUP);
					iNumRows    = (iEndGroup - iStartGroup)+1; 

					tSelectedResult = tResults.getTable( RESULT_GEN_COL_ENUM.GEN_RESULT_TABLE.toInt(), iStartGroup).cloneTable();

					/* For efficiency add the ins_type, disc_idx and proj_idx columns before adding rows */
					iInsTypeCol = iDiscIdxCol = iProjIdxCol = -1;

					/* Do we need to copy the ins_type values onto the table of selected result */
					iInsType = tResults.getInt( RESULT_GEN_COL_ENUM.GEN_INS_TYPE.toInt(), iStartGroup);
					if (iInsType > 0)
					{
						/* Note that some results (especially user defined ones) may already have a column for ins_type.
                  As a result we need to check this before adding it ... */
						iInsTypeCol = tSelectedResult.getColNum( "ins_type");
						if ((iInsTypeCol <= 0) || ((iInsTypeCol > 0) && (tSelectedResult.getColType( iInsTypeCol) != COL_TYPE_ENUM.COL_INT.toInt()))) 
						{
							tSelectedResult.addCol( "ins_type", COL_TYPE_ENUM.COL_INT);
							iInsTypeCol = tSelectedResult.getNumCols();
						}
						else iInsTypeCol = -1;
					}

					/* Do we need to copy the disc_idx values onto the table of selected result */
					iDiscIdx = tResults.getInt( RESULT_GEN_COL_ENUM.GEN_DISC_IDX.toInt(), iStartGroup);
					if (iDiscIdx > 0)
					{
						/* Note that some results (especially user defined ones) may already have a column for disc_idx.
                  As a result we need to check this before adding it ... */
						iDiscIdxCol = tSelectedResult.getColNum( "disc_idx");
						if ((iDiscIdxCol <= 0) || ((iDiscIdxCol > 0) && (tSelectedResult.getColType( iDiscIdxCol) != COL_TYPE_ENUM.COL_INT.toInt()))) 
						{
							tSelectedResult.addCol( "disc_idx", COL_TYPE_ENUM.COL_INT);
							iDiscIdxCol = tSelectedResult.getNumCols();
						}
						else iDiscIdxCol = -1;
					}

					/* Do we need to copy the proj_idx values onto the table of selected result */
					iProjIdx = tResults.getInt( RESULT_GEN_COL_ENUM.GEN_PROJ_IDX.toInt(), iStartGroup);
					if (iProjIdx > 0)
					{
						/* Note that some results (especially user defined ones) may already have a column for disc_idx.
                  As a result we need to check this before adding it ... */
						iProjIdxCol = tSelectedResult.getColNum( "proj_idx");
						if ((iProjIdxCol <= 0) || ((iProjIdxCol > 0) && (tSelectedResult.getColType( iProjIdxCol) != COL_TYPE_ENUM.COL_INT.toInt()))) 
						{
							tSelectedResult.addCol( "proj_idx", COL_TYPE_ENUM.COL_INT);
							iProjIdxCol = tSelectedResult.getNumCols();
						}
						else iProjIdxCol = -1;
					}

					/* JIMNOTE: Things perhaps to check for (when I have some time) ...
               1) Sim result may have different cols in the result for each ins_type or index
               These conditions will cause this funciton to screw up 
					 */

					/* De-normalize the data ... */
					iRow = 1;
					for (x = 0; x < iNumRows; x++)
					{
						if (iInsTypeCol > 0) iInsType = tResults.getInt( RESULT_GEN_COL_ENUM.GEN_INS_TYPE.toInt(), x+iStartGroup);
						if (iDiscIdxCol > 0) iDiscIdx = tResults.getInt( RESULT_GEN_COL_ENUM.GEN_DISC_IDX.toInt(), x+iStartGroup);
						if (iProjIdxCol > 0) iProjIdx = tResults.getInt( RESULT_GEN_COL_ENUM.GEN_PROJ_IDX.toInt(), x+iStartGroup);

						tResults.getTable( RESULT_GEN_COL_ENUM.GEN_RESULT_TABLE.toInt(), x+iStartGroup).copyRowAddAll(tSelectedResult);

						iTotalRows = tSelectedResult.getNumRows();
						for (;iRow <= iTotalRows; iRow++)
						{
							if (iInsTypeCol > 0) tSelectedResult.setInt( iInsTypeCol, iRow, iInsType);
							if (iDiscIdxCol > 0) tSelectedResult.setInt( iDiscIdxCol, iRow, iDiscIdx);
							if (iProjIdxCol > 0) tSelectedResult.setInt( iProjIdxCol, iRow, iProjIdx);
						}
					}
				}
			}
			else // only Deal entities will hit this code
			{
				tSelectedResult = Table.tableNew( "Tran Result" );
				tSelectedResult.select( tResults, sWhat, "deal_num GT 0");
			}
		}  

		return tSelectedResult;
	}

	public int SetupRevalParamForDeals(int iMode, Table tAPMArgumentTable, String sJobName, int iQueryId, int entityGroupId, Table tRevalParam) throws OException {
		int run_as_avs_script = m_APMUtils.APM_GetOverallPackageSettingInt(tAPMArgumentTable, "run_as_avs_script", 0, 1, 0);

		// Need to set the portfolio on the reval param, otherwise the simulation results that need prior EODs will not be able
		// to load this data
		tRevalParam.setInt("Portfolio", 1, entityGroupId);

		/* get scenario info */
		Table tSimDef = tRevalParam.getTable("SimulationDef", 1);

		// for results where we don't care about really running the sim
		// add the query ID column & set it so we don't load deals/refresh mktd
		// useful for packages which are not running proper sims, just OpenJvs scripts
		if (run_as_avs_script == 1 ) {
			//don't refresh the market data
			tRevalParam.setInt("RefreshMktd", 1, 0);

			if (tRevalParam.getColNum("PriorRunType") > 0)
				tRevalParam.setInt("PriorRunType", 1, 0);
			if (tRevalParam.getColNum("DoGlobalPortfolioCalcs") > 0)
				tRevalParam.setInt("DoGlobalPortfolioCalcs", 1, 0);

			//find a single valid tran num - cache as the query can take some time
			//don't use the APM specific caching as its not needed for this
			Table querySingleDeal = Table.getCachedTable("APM Single Deal Query");
			if (Table.isTableValid(querySingleDeal) == 0) {
				//find a simple deal for the sim to load up - not a complicated one
				//do this to speed up the reval
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Finding & caching Single Deal Number");
				querySingleDeal = Table.tableNew();

				// tran_status - 1 = Pending
				// tran_status - 2 = New
				// tran_status - 3 = Validated
				// tran_status - 7 = Proposed
				// tran_type   - 0 = Trading

				m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, querySingleDeal, "min(tran_num)", "ab_tran", "tran_status in (1, 2, 3, 7) and tran_type = 0 and toolset not in (33,36,37,10,38)");

				//------------------------------------------------------------
				// CHECK #1
				// Check against a single row with tran_status = 0,				
				int iReceivedTranNum = 0;
				if (querySingleDeal.getNumRows() > 0) {
					iReceivedTranNum = querySingleDeal.getInt(1, 1);
				} else {
					iReceivedTranNum = 0;
				}
				// if we don't get any rows then we have to load up a complicated deal.
				if (querySingleDeal.getNumRows() < 1 || iReceivedTranNum == 0) {
					m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, querySingleDeal, "min(tran_num)", "ab_tran", "tran_status in (1, 2, 3, 7) and tran_type = 0");
				}

				//------------------------------------------------------------
				// CHECK #2
				// Check against a single row with tran_status = 0, 
				// but for all toolsets available				
				if (querySingleDeal.getNumRows() > 0) {
					iReceivedTranNum = querySingleDeal.getInt(1, 1);
				} else {
					iReceivedTranNum = 0;
				}

				if (iReceivedTranNum == 0) {
					// Was unable to retrieve any useful information
					// Failed to retrieve data for the 'APM Single Deal Query' cache. At least one 'Trading' type deal with status 'Pending' or 'Validated' required. Please contact APM Support. 

					String sProcessingMessage = "Failed to retrieve data for the 'APM Single Deal Query' cache. At least one 'Trading' type deal with status 'Pending' or 'Validated' required. Please contact APM Support.";
					m_APMUtils.APM_LogStatusMessage(iMode, 0, m_APMUtils.cStatusMsgTypeProcessing, sJobName, "", entityGroupId, -1, -1, -1, -1, tAPMArgumentTable, Util.NULL_TABLE, sProcessingMessage);
					m_APMUtils.APM_PrintMessage(tAPMArgumentTable, sProcessingMessage);
				}

				Table.cacheTable("APM Single Deal Query", querySingleDeal);
			}
			int newQueryId = m_APMUtils.APM_TABLE_QueryInsert(tAPMArgumentTable, querySingleDeal, 1);

			//now set this to fool the sim
			tRevalParam.setInt("QueryId", 1, newQueryId);

			//add the real query ID into its own column in the sim def
			//this will be accessed by the results
			if (tSimDef.getColNum("APM Single Deal Query") < 1)
				tSimDef.addCol("APM Single Deal Query", COL_TYPE_ENUM.COL_INT);

			tSimDef.setInt("APM Single Deal Query", 1, iQueryId);
		} else {
			if (tSimDef.getColNum("APM Single Deal Query") > 0)
				tSimDef.delCol(tSimDef.getColNum("APM Single Deal Query"));
		}

		if (tSimDef.getColNum("APM Portfolio ID") <= 0) {
			tSimDef.addCol("APM Portfolio ID", COL_TYPE_ENUM.COL_INT);
			tSimDef.setInt("APM Portfolio ID", 1, entityGroupId);
		}

   	 	return 1;
	}

	public int SetUpArgumentTableForDeals(Table tAPMArgumentTable, Table argt) throws OException {
   	 	int iRetVal = 1;

   	 	/* get the market data source param */
   	 	tAPMArgumentTable.setInt("Market Data Source", 1, argt.getInt("market_data_source", 1));

   	 	/* get the closing dataset ID param (if it exists) */
   	 	tAPMArgumentTable.setInt("Closing Dataset ID", 1, argt.getInt("closing_dataset_id", 1));

   	 	/* get the closing dataset date param (if it exists) */
   	 	tAPMArgumentTable.setString("Closing Dataset Date", 1, argt.getString("closing_dataset_date", 1));

   	 	// check the closing dataset date is ok
   	 	if (Str.len(argt.getString("closing_dataset_date", 1)) > 0) {
			if (OCalendar.parseString(argt.getString("closing_dataset_date", 1)) < 0) {
				m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Invalid Date String specified for Closing Dataset Date property in the APM service.  Aborting");
				iRetVal = 0;
			}
   	 	}
   	 	return iRetVal;
	}

}
