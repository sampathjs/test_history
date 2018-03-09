package com.jm.eod.mature;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.matthey.openlink.utilities.Repository;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.PluginType;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;


/**
 * The param script for EOD UnMaturing of Trades
 * <br>
 * <br>
 *  <p><table border=0 style="width:15%;">
 *  <caption style="background-color:#0070d0;color:white"><b>ConstRepository</caption>
 *	<th><b>context</b></th>
 *	<th><b>subcontext</b></th>
 *	</tr><tbody>
 *	<tr>
 *	<td align="center">{@value #CONST_REPO_CONTEXT}</td>
 *	<td align="center">{@value #CONST_REPO_SUBCONTEXT}</td>
 *  </tbody></table></p>
 *	<p>
 *	<table border=2 bordercolor=black>
 *	<tbody>
 *	<tr>
 *	<th><b>Variable</b></th>
 *	<th><b>Default</b></th>
 *	<th><b>Description</b></th>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #QUERY_NAME}</b></font></td>
 *	<td>{@value #QUERY_NAME_DEFAULT}</td>
 *	<td>The default query to run
 *	</td>
 *	</tr>
 *	</tbody>
 *	</table>
 * </p>
 */
@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)
public class UnMatureParam implements IScript  {

	private final static String queryName  = "";
	private JM_SavedQueryParam stdNamedQuery;
	

	private static final String CONST_REPO_CONTEXT = "EOD";
	private static final String CONST_REPO_SUBCONTEXT = "UnMature";
	private static final String QUERY_NAME = "Query";
	private static final String QUERY_NAME_DEFAULT = "UnMatureTrades";
	
	private static final Map<String, String> configuration;
	static
	{
		configuration = new HashMap<String, String>();
		configuration.put(QUERY_NAME,QUERY_NAME_DEFAULT);
	}
	
	
	public UnMatureParam() {
		Properties properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
		
		stdNamedQuery = new JM_SavedQueryParam(properties.getProperty(QUERY_NAME)); 
	}

	@Override
	public void execute(IContainerContext context) throws OException {
		stdNamedQuery.execute(context);		
	}

	
}
