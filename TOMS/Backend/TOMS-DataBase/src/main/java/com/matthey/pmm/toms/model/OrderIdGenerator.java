package com.matthey.pmm.toms.model;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import com.matthey.pmm.toms.model.hibernate.SequenceValueGetter;

public class OrderIdGenerator 
  implements IdentifierGenerator {
    @Override
    public Serializable  generate(SharedSessionContractImplementor session, Object obj) 
    		throws HibernateException {
    	SequenceValueGetter sequenceGetter = new SequenceValueGetter();
    	
    	if (obj instanceof LimitOrder) {
    		LimitOrder limitOrder = (LimitOrder)obj;
    		if (limitOrder.getOrderId() <= 0) {
    			return sequenceGetter.getID(session, "order_id_seq");
    		} else {
    			return limitOrder.getOrderId();
    		}
    	}
    	if (obj instanceof ReferenceOrder) {
    		ReferenceOrder referenceOrder = (ReferenceOrder)obj;
    		if (referenceOrder.getOrderId() <= 0) {
    			return sequenceGetter.getID(session, "order_id_seq");
    		} else {
    			return referenceOrder.getOrderId();
    		}
    	}
    	throw new RuntimeException("OrderIdGenerator.generate applicable on objects of type LimitOrder or ReferenceOrder only");
    }
}