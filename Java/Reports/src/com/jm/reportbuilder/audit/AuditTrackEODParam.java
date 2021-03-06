package com.jm.reportbuilder.audit;

import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Activity_Description;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Activity_Type;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Expected_Duration;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_For_Personnel_ID;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_For_Short_Name;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Ivanti_Identifier;
import static com.jm.reportbuilder.audit.AuditTrackConstants.COL_Role_Requested;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class AuditTrackEODParam implements IScript {
	
	private String defaultIvantiIdentifier = "";

	/** The const repository used to initialise the logging classes. */
	


	/** The Constant CONTEXT used to identify entries in the const repository. */
 
	
	@Override
	public void execute(IContainerContext context) throws OException {


		try {
			
			init();
			Logging.info("Processing AuditTrackEODParam Started:" );
			
			
			Table argt = context.getArgumentsTable();

						
			 
			argt.addCol(COL_Activity_Type, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Role_Requested, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Activity_Description, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Ivanti_Identifier, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_Expected_Duration, COL_TYPE_ENUM.COL_STRING);
			
			argt.addCol(COL_For_Personnel_ID, COL_TYPE_ENUM.COL_INT);
			argt.addCol(COL_For_Short_Name, COL_TYPE_ENUM.COL_STRING);
//			argt.addCol(COL_Personnel_ID, COL_TYPE_ENUM.COL_INT);
//			argt.addCol(COL_Short_Name, COL_TYPE_ENUM.COL_STRING);
			


			
			if(argt.getNumRows() < 1) {
				argt.addRow();
			}
		   
			
			
			 
			Logging.info("Processing AuditTrackEODParam Setting to defaults" );
			
			// no gui so default to the current EOD date. 
			argt.setString(COL_Activity_Type, 1, AuditTrackConstants.ACTIVITY_EOD_PROCESS);
			argt.setString(COL_Activity_Description, 1, "Running EOD " );
			argt.setString(COL_Role_Requested, 1, AuditTrackConstants.ROLE_EOD);
			argt.setString(COL_Ivanti_Identifier, 1, defaultIvantiIdentifier);
			argt.setString(COL_Expected_Duration, 1, "2h");
			

			int personnelID = Ref.getUserId();
			String shortName = Ref.getShortName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, personnelID);
			argt.setInt(COL_For_Personnel_ID, 1, personnelID);
			argt.setString(COL_For_Short_Name, 1, shortName );
//			argt.setInt(COL_Personnel_ID, 1, personnelID);
//			argt.setString(COL_Short_Name, 1, shortName );


			Logging.info("Processing AuditTrackEODParam Ended:" );	
				
			 
		} catch (Exception e) {
			
			e.printStackTrace();
			String msg = e.getMessage();
			throw new OException(msg);
		} finally {
			
		}
	}
	






	 
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		ConstRepository constRep = new ConstRepository(AuditTrackConstants.CONST_REPO_CONTEXT, Ref.getUserName());
		try {			
			defaultIvantiIdentifier = getEODIdentifier();			
			Logging.init(this.getClass(), constRep.getContext(), constRep.getSubcontext());
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	
	private String getEODIdentifier() throws OException {

		String retValue = "EOD Date:" + OCalendar.formatJd(OCalendar.today(),com.olf.openjvs.enums.DATE_FORMAT.DATE_FORMAT_ISO8601);
		return retValue;
	}

}
