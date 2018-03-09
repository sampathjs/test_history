package com.jm.accountingfeed.udsr.ledgers;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;

/**
 * Deal Calculator Interface declares method that are required to populate Ledgers
 * Implementation of this Interface shall work on an underlying Transaction pointer
 * @author jains03
 *
 */
public interface IDealCalculator 
{
	int getDealNum(int leg) throws OException;
	int getTranNum(int leg) throws OException;
    int getPhySideNum(int leg) throws OException;
    int getFinSideNum(int leg) throws OException;
	int getTranGroup() throws OException;
	int getNumLegs() throws OException;
	int getTradeDate() throws OException;
	String getDealReference() throws OException;
	int getTranStatusId() throws OException;
    int getPreciousMetal(int leg) throws OException;
	int getInternalBunit() throws OException;
	int getExternalBunit() throws OException;
	int getInternalLentity() throws OException;
	int getInternalPortfolio() throws OException;
	int getInsNum() throws OException;
	String getInsType() throws OException;
    String getTradeType(int leg) throws OException;
	int getInsTypeId() throws OException;
	int getInsSubType(int leg) throws OException;
	int getBuySell(int leg) throws OException;
	int getFixedFloat() throws OException;
	String getTradeLocation(int leg) throws OException;
	int getFromCurrency(int leg) throws OException;
	int getToCurrency(int leg) throws OException;
    int getSettlementDate(int leg) throws OException;
	double getPositionUom(int leg) throws OException;
	double getPositionToz(int leg) throws OException;
	double getCashAmount(int leg) throws OException;
	double getExchangeRate() throws OException;
	int getUom(int leg) throws OException;
	double getTradePrice(int leg) throws OException;
	double getInterestRate() throws OException;
	int getPaymentDate(int leg) throws OException;
	String getIsCoverage() throws OException;
	String getCoverageText() throws OException;
    String getLocation(int leg) throws OException;
	int getForm(int leg) throws OException;
    int getPurity(int leg) throws OException;
    int getEndDate() throws OException;
	
	/**
	 * Implement this method to initialise state of the Calculator object 
	 * (on change in underlying Transaction object) 
	 */
	void initialize(Transaction tran);
	boolean isProcessed() throws OException;
}
