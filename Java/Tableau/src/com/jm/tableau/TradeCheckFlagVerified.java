package com.jm.tableau;

import com.olf.openjvs.*;

public class TradeCheckFlagVerified implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
    	TradeCheckFlagSaver.save(context.getArgumentsTable(), "Verified", context.getReturnTable());
    }
}
