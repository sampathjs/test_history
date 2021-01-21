package com.matthey.openlink.trading.opsvc;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldEventListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.EventNotification })
public class PassThroughPFEventNotification extends AbstractFieldEventListener {
	Session session =null;
	
	@Override
	public ReferenceChoices getChoices(Session session, Field field, ReferenceChoices choices) {

		Table temp = null; 
		try{
			this.session =  session;
			ReferenceChoices referenceChoices1 = null;
		Logging.init(this.getClass(), "", "");
		Transaction tran = field.getTransaction();
		if (field.getName().equalsIgnoreCase("PassThrough Legal")){
			return  getReferenceChoiceDataForLE(tran, choices);
			 //return null;
		}else {
			return  getReferenceChoiceDataForPF(tran, choices);
		}
		 
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}finally{
			//temp.dispose();
			Logging.close();
		}
		
	}
	private ReferenceChoices getReferenceChoiceDataForPF(Transaction tran, ReferenceChoices choices) {
		String tickerStr = tran.getValueAsString(EnumTransactionFieldId.Ticker); 
		int buIntValue =  tran.getField("PassThrough Unit").getValueAsInt();
		ReferenceChoices rcs = session.getStaticDataFactory().createReferenceChoices();
		String sqlStr = "Select distinct p.id_number, p.name, p.long_name from portfolio p " +
				   "\n JOIN party_portfolio pp on ( pp.portfolio_id=p.id_number  and pp.party_id = "+buIntValue +")"  +
				   "\n JOIN portfolio_group_to_port pgpt  on (  pgpt.port_id=p.id_number)"  +
				   "\n JOIN portfolio_groups pg   on (pg.portfolio_group_id =pgpt.portfolio_group_id )"  +
				   "\n JOIN header h   on (h.ticker='"+ tickerStr+"' "+
				   "\n and h.portfolio_group_id=pgpt.portfolio_group_id "  +
				   "\n and  h.portfolio_group_id <> 20005 ) " ;
		Table leTable = session.getIOFactory().runSQL(sqlStr);
		 for (TableRow row : leTable.getRows()) {
             int id = row.getInt("id_number");
             String name = row.getString("name");
             String description = row.getString("long_name");
             rcs.add(id, name, description);
         }
         return rcs;
		 
	}
	 
	private ReferenceChoices getReferenceChoiceDataForLE(  Transaction tran, ReferenceChoices choices) {
		int buIntValue =  tran.getField("PassThrough Unit").getValueAsInt();
		ReferenceChoices rcs = session.getStaticDataFactory().createReferenceChoices();
		String sqlStr = "SELECT p.party_id, p.short_name, p.long_name from party p "+
						"\n JOIN  party_relationship  pr"+ 
						"\n ON (pr.business_unit_id= "+buIntValue +
						"\n AND pr.legal_entity_id = p.party_id )"; 
		Table leTable = session.getIOFactory().runSQL(sqlStr);
		 for (TableRow row : leTable.getRows()) {
             int id = row.getInt("party_id");
             String name = row.getString("short_name");
             String description = row.getString("long_name");
             rcs.add(id, name, description);
         }
         return rcs;
		 
	}
	private ReferenceChoice findChoiseIgnoreCase(ReferenceChoices choices, int extBU) {
		session.getDebug().viewTable(choices.asTable());
		ReferenceChoice rc = choices.findChoice(extBU);
		if (rc != null){
			return rc;
		}
		return null;
	}	
}
