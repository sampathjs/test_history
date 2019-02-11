package com.openlink.matthey.reportbuilder;
 
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

public abstract class DataSource implements IScript {

    /**
     * OLF entry point to execute custom processing <br>
     * This method defines the processing framework for the ReportBuilder DataSource plugin
     */
	@Override
	public void execute(IContainerContext context) throws OException {

		Logger.log(LogLevel.INFO, 
				LogCategory./*SimResults*/Trading, 
				this, 
				String.format("Starting %s", this.getClass().getSimpleName()));
        Table argt = context.getArgumentsTable();
        
        String prefix = argt.getString("PluginName", 1);
        
        evaluateArguments(prefix, argt.getTable("PluginParameters", 1));
        
		Table returnt = context.getReturnTable();
		
		if (layoutOnly =(argt.getInt("ModeFlag", 1) == 0))
		{			
			generateOutputTable(returnt);
			
		} else {		
			process(argt,returnt);
			
		}
		Logger.log(LogLevel.INFO, 
				LogCategory./*SimResults*/Trading, 
				this, 
				String.format("COMPLETED %s", this.getClass().getSimpleName()));
		return;
	}

	/**
	 * determine implementation behaviour based on the effective arguments passed to plugin from ReportBuilder 
	 */
    protected void evaluateArguments(String prefix, Table argt) {
		// TODO Auto-generated method stub
		
	}

	private void process(Table argt, Table returnt) {
		generateOutputTable(returnt);
	}

	/**
	 * populate supplied table parameter with layout of target table for ReportBuilder
	 */
	protected void generateOutputTable(Table returnt) {
		// TODO override this for implementation
	}

	private boolean layoutOnly = true;
    
    protected boolean isLayoutOnly() {
    	return layoutOnly;
    }


}
