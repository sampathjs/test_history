package com.jm.eod.reports;

import java.util.ArrayList;
import java.util.List;

import com.jm.eod.common.*;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class EOD_JM_UK_MissingResetsToday  implements IScript {

	    public void execute (IContainerContext context) throws OException
		{
			//Utils.setParams(context.getArgumentsTable(), Const.MISSING_RESETS_QRY_NAME, RegionEnum.UK.description().trim());
			List<Parameters> parameters = new ArrayList<>(0);
			parameters.add(new Parameters(Const.REGION_COL_NAME, COL_TYPE_ENUM.COL_STRING, RegionEnum.UK.description().trim()));
			String regionCode = RegionEnum.UK.description().trim();
			String qryRegion = regionCode.length() > 0 ? regionCode + "_" : "";
			parameters.add( new Parameters(Const.QUERY_COL_NAME, COL_TYPE_ENUM.COL_STRING, String.format(Const.MISSING_RESETS_QRY_NAME, qryRegion)));

			parameters.add( new Parameters(Const.QUERY_DATE, COL_TYPE_ENUM.COL_STRING, OCalendar.formatJdForDbAccess(OCalendar.today())));
			parameters.add( new Parameters(Const.QUERY_REPORT, COL_TYPE_ENUM.COL_STRING, "Missed_Resets_AfterFixing.eod"));
			
			Utils.addParams(context.getArgumentsTable(), parameters);
			Util.exitSucceed();
		}
	

}
