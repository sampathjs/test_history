package com.matthey.openlink.trading.opsvc;


import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Notification;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.Logger;

/**
 * 
 * @version $Revision:  $
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class LockBack2BackTrades extends AbstractTradeProcessListener {

	@Override
	public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus,
			PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {
		
		
		 for (PreProcessingInfo<?> activeItem : infoArray) {

	            Transaction transaction = null;
	            try {
	                transaction = activeItem.getTransaction();
	                
	                int futureDeal;
					if ((futureDeal=Back2BackForwards.back2backTransaction(transaction.getField(Back2BackForwards.LINKED_INFO)))>0 
							&& activeItem.getInitialStatus() != EnumTranStatus.Pending
							&& !transaction.getField(EnumTransactionFieldId.Book).getDisplayString().equals("Back2BackForwards"))
	                	return PreProcessResult.failed(String.format("Tran#%d is read only, please update parent(Future) deal %d to change this deal!", 
	                            transaction.getTransactionId(), futureDeal));
					if (futureDeal>0) {
						transaction.getField(EnumTransactionFieldId.Book).setValue("");
						Transaction offset = activeItem.getOffsetTransaction();
						if (offset!=null)
							offset.getField(EnumTransactionFieldId.Book).setValue("");
					}

	            } catch (Exception e) {
	                String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s",
	                        null != transaction ? transaction.getTransactionId() : -888,
	                        this.getClass().getSimpleName(), e.getLocalizedMessage());
	                Logger.log(com.openlink.endur.utilities.logger.LogLevel.FATAL,
	                        LogCategory.Accounting, 
	                        this, 
	                        reason, e);
	                e.printStackTrace();
	                return PreProcessResult.failed(reason);

	            } finally {
	                if (transaction != null)
	                    transaction.dispose();
	            }
	}
		return PreProcessResult.succeeded();
	}

	
	
}
