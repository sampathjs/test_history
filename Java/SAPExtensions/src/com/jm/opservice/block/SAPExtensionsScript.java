package com.jm.opservice.block;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class SAPExtensionsScript implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
        OConsole.oprint("Hello World!");
    }
}
