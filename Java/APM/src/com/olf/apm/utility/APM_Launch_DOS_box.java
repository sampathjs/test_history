package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Launch_DOS_box implements IScript {
   /* Released with version 24-Feb-2017_V17_0_6 of APM */
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   SystemUtil.command("cmd.exe");
}


}
