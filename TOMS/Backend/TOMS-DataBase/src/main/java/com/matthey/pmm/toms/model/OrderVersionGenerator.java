package com.matthey.pmm.toms.model;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class OrderVersionGenerator 
  implements IdentifierGenerator {
    @Override
    public Serializable  generate(SharedSessionContractImplementor session, Object obj) 
    		throws HibernateException {   	
    	if (obj instanceof LimitOrder) {
    		LimitOrder limitOrder = (LimitOrder)obj;
    		if (limitOrder.getOrderId() <= 0) {
    			return 1;
    		} else {
    			return limitOrder.getVersion();
    		}
    	}
    	if (obj instanceof ReferenceOrder) {
    		ReferenceOrder referenceOrder = (ReferenceOrder)obj;
    		if (referenceOrder.getOrderId() <= 0) {
    			return 1;
    		} else {
    			return referenceOrder.getVersion();
    		}
    	}
    	throw new RuntimeException("OrderVersionGenerator.generate applicable on objects of type LimitOrder or ReferenceOrder only");
    }
}