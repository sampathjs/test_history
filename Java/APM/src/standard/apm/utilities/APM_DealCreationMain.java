package standard.apm.utilities;

import java.util.Random;

import com.olf.openjvs.Debug;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

public class APM_DealCreationMain implements IScript {
	int dealsList[] = { 23006, 23061 };   // these deal number will be used if no input from user. 
	int floor = 100, ceiling = 100000, delay = 5, repeats =1;
	boolean clearDeals = true;
	Table newDeals;
	
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table dealTbl = Util.NULL_TABLE;
		boolean releaseDealTable = false;
		
		if (argt.getNumRows() < 1 || argt.getColNum("repeats") < 1)
		{
			OConsole.print("Using default setting to run APM_DealCreation.\n");
			dealTbl = Table.tableNew();
			dealTbl.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
			releaseDealTable = true;			
			for (int i = 0; i < dealsList.length; i++)
			{
				int row = dealTbl.addRow();
				dealTbl.setInt("deal_num", row, dealsList[i]);
			}
		}
		else
		{
			floor = argt.getInt( "floor", 1);
			ceiling = argt.getInt( "ceiling", 1);
			delay = argt.getInt("delay", 1);
			repeats = argt.getInt("repeats", 1);
			clearDeals = argt.getInt("clear", 1) == 1;
			dealTbl = argt.getTable("deal_list", 1);
		}
		
		if (floor < 1 || floor > ceiling)
			floor = 100;
		if (ceiling < floor || ceiling > 1000000000)
			ceiling = 100000;
		if (delay < 0 || delay > 60)
			delay = 5;
		if (repeats > 50 || repeats < 1)
			repeats = 1;
		
		try
		{		
			Random rand = new Random(OCalendar.today());
			int limit = ceiling - floor +1;
			getTranNum(dealTbl);
			for (int j = 1; j <= repeats; j++)
			{
				for (int i = 1; i <= dealTbl.getNumRows(); i++)
				{
					int tranNum = dealTbl.getInt("tran_num", i);					
					double position = rand.nextInt(limit) + floor;
					if (tranNum < 1)
					{
						OConsole.print("Can't find tran for Deal number: " + dealTbl.getInt("deal_num", i));
						continue;
					}
					createNewDeal(tranNum, position);
					Thread.sleep(delay * 1000);
				}
			}
			
			if (clearDeals)
			{
				Thread.sleep(delay * 1000);
				clearNewDeals();
			}
		}
		catch (OException | InterruptedException e)
		{
			OConsole.print(e.getMessage());
		}
		finally
		{
			if (Table.isTableValid(newDeals) == 1)
				newDeals.destroy();
			if (releaseDealTable)
				dealTbl.destroy();
		}
	}

	public int createNewDeal(int tmpDeal, double position) throws OException
	{
		Transaction tran = Transaction.retrieveCopy(tmpDeal);
		tran.setField(TRANF_FIELD.TRANF_DEAL_VOLUME.toInt(), 1, "", Str.doubleToStr(position));
		int retval = tran.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED);
		
		if (clearDeals)
		{
			int ver = tran.getFieldInt(TRANF_FIELD.TRANF_VERSION_NUM.toInt());
			int tranNum = tran.getFieldInt(TRANF_FIELD.TRANF_TRAN_NUM.toInt());
			recordNewDeals(tranNum, ver);
		}
		
		return retval;
	}
	
	private void recordNewDeals(int tranNum, int vers) throws OException
	{
		if (Table.isTableValid(newDeals) == 0)
		{
			newDeals = Table.tableNew();
			newDeals.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
			newDeals.addCol("vers", COL_TYPE_ENUM.COL_INT);
		}
		int row = newDeals.addRow();
		newDeals.setInt("tran_num", row, tranNum);
		newDeals.setInt("vers", row, vers);		
	}
	
	private void clearNewDeals() throws OException
	{
		try
		{
			for (int row = newDeals.getNumRows(); row > 0; row--)
			{
				int tranNum = newDeals.getInt("tran_num", row);
				int vers = newDeals.getInt("vers", row);
				Transaction.cancel(tranNum, vers);
			}
		}
		catch (OException e)
		{
			OConsole.print("ERROR: " + e.getMessage());
		}
	}
	
	private void getTranNum(Table tbl) throws OException
	{
		Table dataTable;
		int qid = getQueryID(tbl, "deal_num");
		
		if (qid > 0)
		{
			dataTable = Table.tableNew();	
			try
			{
				String sql = "select tran_num, deal_tracking_num deal_num from ab_tran ab, query_result qr " +
					  "where ab.tran_status = 3 and qr.query_result = ab.deal_tracking_num and qr.unique_id = " + qid; 
		   
				com.olf.openjvs.DBase.runSqlFillTable(sql, dataTable);
      
				tbl.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
				tbl.select(dataTable, "tran_num", "deal_num EQ $deal_num");				
			}
			finally
			{
				Query.clear(qid);
				dataTable.destroy();
			}
		}		
	}
	
	private int getQueryID( Table tTable, String sColumn ) throws OException
	{
       final int nAttempts = 10;
       
       int iQueryId = 0;
       
       int numberOfRetriesThusFar = 0;
       do {
          try {
	            // db call
             iQueryId = Query.tableQueryInsert( tTable, sColumn );
          } catch (OException exception) {
             OLF_RETURN_CODE olfReturnCode = exception.getOlfReturnCode();
             
             if (olfReturnCode == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR) {
                numberOfRetriesThusFar++;
	                
                String message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, nAttempts, exception.getMessage());
                OConsole.oprint(message);
	                
                Debug.sleep(numberOfRetriesThusFar * 1000);
             } else {
                // it's not a retryable error, so leave
                break;
             }
          }
       } while (iQueryId == 0 && numberOfRetriesThusFar < nAttempts);
       
       return iQueryId;
	}
}

