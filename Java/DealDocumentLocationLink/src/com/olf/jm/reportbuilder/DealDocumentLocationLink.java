package com.olf.jm.reportbuilder;


import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class DealDocumentLocationLink implements IScript {

	@Override
	public void execute(IContainerContext arg0) throws OException {
		Table returnt = arg0.getReturnTable();
		Table dealList = Table.tableNew();
		String sql = "SELECT DISTINCT ddi.deal_tracking_num, ab.tran_num FROM deal_document_link ddi \n"
				   + "INNER JOIN ab_tran ab ON ab.deal_tracking_num = ddi.deal_tracking_num and ab.current_flag = 1";
		try {
			DBaseTable.execISql(dealList, sql);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		
		for (int loop = 1; loop <= dealList.getNumRows(); loop++) {
			Transaction tran = Transaction.retrieve(dealList.getInt(2, loop));
			Table temp = tran.getDealDocumentTable();
			temp.addCol("deal_tracking_num", COL_TYPE_ENUM.COL_INT);
			temp.setColValInt("deal_tracking_num", dealList.getInt(1, loop));
			temp.group("reference, doc_id");
			temp.groupSum("reference");
			temp.clearDataRows();
			returnt.select(temp, "*", "deal_tracking_num GT 0");
			temp.destroy();
		}
	}

}
