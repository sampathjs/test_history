package com.matthey.openlink.trading.opsvc;


import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

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
	            Transaction futureTransaction = null;
	            try {
	                transaction = activeItem.getTransaction();
	                Logging.init(context, this.getClass(), "LockBack2BackTrades", "");
	                Logging.info(String.format("Checking Tran#%d", transaction.getTransactionId()));
	                
	                int futureDeal;
					if ((futureDeal=Back2BackForwards.back2backTransaction(transaction.getField(Back2BackForwards.LINKED_INFO)))>0 
							&& activeItem.getInitialStatus() != EnumTranStatus.Pending
							&& !transaction.getField(EnumTransactionFieldId.Book).getDisplayString().equals("Back2BackForwards")){

						TradingFactory tf = context.getTradingFactory();
						futureTransaction = tf.retrieveTransaction(futureDeal);
						
						EnumTranStatus enumFutureTranStatus = futureTransaction.getTransactionStatus();
						
						if("Validated".equals(enumFutureTranStatus.getName())
							|| "New".equals(enumFutureTranStatus.getName())
							|| "Pending".equals(enumFutureTranStatus.getName())){

		                	return PreProcessResult.failed(String.format("Tran#%d is read only, please update parent(Future) deal %d to change this deal!", 
		                            transaction.getTransactionId(), futureDeal));
						}
						
					}
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
	               Logging.error(reason, e);
	                e.printStackTrace();
	                return PreProcessResult.failed(reason);

	            } finally {
	                if (transaction != null)
	                    transaction.dispose();
	                
	                if (futureTransaction != null)
	                	futureTransaction.dispose();
	                
	                Logging.close();
	            }
	}
		return PreProcessResult.succeeded();
	}

	
	
}
