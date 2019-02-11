
package com.custom;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

//Replace the below class name "MarketDataLoaderScript"
//to match the plugin name in plugin editor.
public class MarketDataLoaderScriptClosing implements IScript {

public void execute(IContainerContext context) throws OException
{
    String loadGroup1;

    loadGroup1 = "Closing";

    Table argt = context.getArgumentsTable();


    argt.addCol("args", COL_TYPE_ENUM.COL_STRING);

    argt.addRow();
    argt.setString(1, 1, loadGroup1);


}

}
