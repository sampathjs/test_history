package standard.apm.utilities;

import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class APM_DealCreationParam implements IScript {
    static String delimiter = "[ ,]+";
    
	public void execute(IContainerContext context) throws OException
	{
      Table argt = context.getArgumentsTable();
      int retVal = 0;
      Table askTable = Table.tableNew();
      Table yesNo = getYesNoTable();
      Table dealList = Table.tableNew("deal list");
      dealList.addCol("deal_num", COL_TYPE_ENUM.COL_INT);

      try {
         Ask.setTextEdit(askTable, "Template Deal(s)", "", ASK_TEXT_DATA_TYPES.ASK_STRING, "Enter template deals separated by space or comma.", 1);
         Ask.setTextEdit(askTable, "Deal Position Min");
         Ask.setTextEdit(askTable, "Deal Position Max");
         Ask.setTextEdit(askTable, "Number of Deals to Create", "1", ASK_TEXT_DATA_TYPES.ASK_STRING);
         Ask.setTextEdit(askTable, "Delay Time (seconds)",  "5", ASK_TEXT_DATA_TYPES.ASK_STRING);
         Ask.setAvsTable(askTable, yesNo, "Delete created deals\nAt the end", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 2);
         retVal = Ask.viewTable(askTable, "Deal Creation", "Create New Deals from Template:");

         if(retVal <= 0)
         {
            throw new OException("\nUser cancelled operation. Aborting... ");
         }

         String dealsStr = askTable.getTable("return_value", 1).getString(1, 1);
         int floor = Integer.parseInt(askTable.getTable("return_value", 2).getString(1, 1));
         int ceiling = Integer.parseInt(askTable.getTable("return_value", 3).getString(1, 1));
         int repeats = Integer.parseInt(askTable.getTable("return_value", 4).getString(1, 1));
         int delayTime = Integer.parseInt(askTable.getTable("return_value", 5).getString(1, 1));
         Table ynSelect = askTable.getTable("return_value", 6);
         
         String[] dealArray = dealsStr.split(delimiter);
         
         for (String s : dealArray)
         {
        	 int deal = Integer.parseInt(s);
        	 int row = dealList.addRow();
        	 dealList.setInt("deal_num", row, deal);
         }
         
         if(argt.getNumRows() < 1) 
            argt.addRow();

         argt.addCol("floor", COL_TYPE_ENUM.COL_INT);
         argt.addCol("ceiling", COL_TYPE_ENUM.COL_INT);
         argt.addCol("deal_list", COL_TYPE_ENUM.COL_TABLE);
         argt.addCol("repeats", COL_TYPE_ENUM.COL_INT);
         argt.addCol("delay", COL_TYPE_ENUM.COL_INT);
         argt.addCol("clear", COL_TYPE_ENUM.COL_INT);
         
         argt.setInt("floor", 1, floor);
         argt.setInt("ceiling", 1, ceiling);
         argt.setTable("deal_list", 1, dealList);
         argt.setInt("repeats", 1, repeats);
         argt.setInt("delay", 1, delayTime);
         argt.setInt("clear", 1, ynSelect.getInt("return_value", 1));
      }
      catch (OException ex)
      {
    	 dealList.destroy();
         Util.exitFail(ex.getMessage());
      }
      finally
      {
         askTable.destroy();
         yesNo.destroy();
      }
	}	

	private Table getYesNoTable() throws OException
	{
		Table ynTbl = Table.tableNew();
	
		ynTbl.addCol("choice", COL_TYPE_ENUM.COL_STRING);
		ynTbl.addCol("value", COL_TYPE_ENUM.COL_INT);
	
		ynTbl.addRowsWithValues("(Yes),1");
		ynTbl.addRowsWithValues("(No),0");
		
		ynTbl.colHide("value");
		return ynTbl;
	}

}
