package com.olf.jm.tradeListing;

import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;




@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class SetExecutionBrokerFieldDefaults extends AbstractTransactionListener {
      public static final String CONST_REPO_CONTEXT = "FrontOffice";
      public static final String CONST_REPO_SUBCONTEXT = "AutoExecutionBrokerPopulation";

      @Override
      public void notify(final Context context, final Transaction tran) {
            Field brokerIdField;
            String brokerIdFieldValue;
            try {
                  Logging.init(this.getClass(), "", "");
                  Logging.info("Plugin processing transactions #"+ tran.getTransactionId());
                  ConstRepository repository = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

            String brokerId = repository.getStringValue("Broker");
            brokerIdField = tran.getField(EnumTransactionFieldId.Broker);
            brokerIdFieldValue = brokerIdField.getValueAsString();
            if (!brokerIdFieldValue.isEmpty() && brokerIdFieldValue.equalsIgnoreCase("None")){
                  tran.setValue(EnumTransactionFieldId.Broker,brokerId);
            }
           
            } catch (Throwable t) {
                  Logging.error("Error executing plugin " + this.getClass()
                              + ": " + t.toString());
                  for (StackTraceElement ste : t.getStackTrace()) {
                        Logging.error(ste.toString());
                  }
            }finally{
            	Logging.close();
            }
      }
}
