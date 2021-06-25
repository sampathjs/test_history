package com.matthey.pmm.jde.corrections.scripts;

import com.matthey.pmm.EnhancedGenericScript;
import com.matthey.pmm.jde.corrections.Region;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.table.ConstTable;

@ScriptCategory(EnumScriptCategory.Generic)
public class JDECorrectionsGeneratorUK extends EnhancedGenericScript {
    
    @Override
    public void run(Context context, ConstTable table) {
        new JDECorrectionsGenerator(context, Region.UK).run();
    }
}