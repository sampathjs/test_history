package com.jm.tableau;

import com.olf.openjvs.*;

public class TradeCheckFlagToCancel implements IScript
{
    public void execute(IContainerContext context) throws OException
    {
    	TradeCheckFlagSaver.save(context.getArgumentsTable(), "To Cancel & Rebook", context.getReturnTable());
    }
    
}
