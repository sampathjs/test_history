package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.enums.EndurTranInfoField;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;

/**
 * Base concrete class to Calculate the deal attributes which are common for all Toolsets.
 * Default OR Invalid value is returned by the Toolset specific deal attributes methods of this class. 
 * @author jains03
 *
 */
public class BaseDealCalculator extends AbstractDealCalculator 
{
	public BaseDealCalculator() 
	{
	}
	
	public BaseDealCalculator(Transaction tran) 
	{
		super(tran);
	}

	@Override
    public void initialize(Transaction tran) 
	{
        this.tran = tran;
    }

    /**
     * Defines whether underlying Transaction has already been processed.
     * Returns 'false'.
     * Toolset specific classes may override this method.
     */
	@Override
    public boolean isProcessed() throws OException 
    {
        return false;
    }

    @Override
	public int getDealNum(int leg) throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt());
	}

    @Override
	public int getTranNum(int leg) throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_TRAN_NUM.toInt());
	}
	
	/**
	 * Returns invalid value
	 */
    @Override
    public int getPhySideNum(int leg) throws OException 
    {
        return -1;
    }

    /**
     * Returns invalid value
     */
    @Override
    public int getFinSideNum(int leg) throws OException 
    {
        return -1;
    }

    @Override
    public int getNumLegs() throws OException {
        return 1;
    }

    @Override
	public int getTradeDate() throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_TRADE_DATE.toInt());
	}

	@Override
	public String getDealReference() throws OException 
	{	
		return tran.getField(TRANF_FIELD.TRANF_REFERENCE.toInt());
	}

	@Override
	public int getTranStatusId() throws OException {
		return tran.getFieldInt(TRANF_FIELD.TRANF_TRAN_STATUS.toInt());
	}

	/**
	 * Returns 1 if either FromCurrency or ToCurrency on the @param leg is Precious Metal (as per curreny DB Table)
	 */
	@Override
	public int getPreciousMetal(int leg) throws OException 
	{
		if (Util.isPreciousMetalCurrency(getFromCurrency(leg)) ||
				Util.isPreciousMetalCurrency(getToCurrency(leg))) 
		{
			return 1;
		}
		
		return 0;
	}

  @Override
	public int getInternalBunit() throws OException {
		return tran.getFieldInt(TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt());
	}

	@Override
	public int getExternalBunit() throws OException {
		return tran.getFieldInt(TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt());
	}

	@Override
	public int getInternalPortfolio() throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt());
	}

	@Override
	public int getInsNum() throws OException {
		return tran.getFieldInt(TRANF_FIELD.TRANF_INS_NUM.toInt());
	}

	@Override
	public String getInsType() throws OException 
	{	
		return tran.getField(TRANF_FIELD.TRANF_INS_TYPE.toInt());
	}

    /**
     * Returns value of Tran Info field 'JM_Transaction_Id'
     */
	@Override
    public String getTradeType(int leg) throws OException {
	    return tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, EndurTranInfoField.TRADE_TYPE.toString());
    }

    @Override
	public int getInsTypeId() throws OException {
		return tran.getFieldInt(TRANF_FIELD.TRANF_INS_TYPE.toInt());
	}

	@Override
	public int getInsSubType(int leg) throws OException {
		return tran.getFieldInt(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt());
	}

	@Override
	public int getBuySell(int leg) throws OException {
		return tran.getFieldInt(TRANF_FIELD.TRANF_BUY_SELL.toInt());
	}

	@Override
	public int getFixedFloat() throws OException {
		int fixFloat = 0;

		int numParams = tran.getNumParams();
		for (int i = 0; i < numParams; i++) {
			String fxFloat = tran.getField(TRANF_FIELD.TRANF_FX_FLT.toInt(), i);
			if ("float".equalsIgnoreCase(fxFloat)) {
				fixFloat = 1;
				break;
			}
		}

		return fixFloat;
	}

	/**
	 * Returns value of Tran Info field 'Loco'
	 */
	@Override
	public String getTradeLocation(int leg) throws OException 
	{
		return tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, EndurTranInfoField.LOCATION.toString());
	}

	@Override
	public double getPositionUom(int leg) throws OException 
	{
		return 0.0;
	}

	@Override
	public double getPositionToz(int leg) throws OException 
	{
		return 0.0;
	}

	@Override
	public double getCashAmount(int leg) throws OException 
	{
		return 0.0;
	}

	@Override
	public double getExchangeRate() throws OException 
	{
		return 0.0;
	}

    /**
     * Returns invalid value
     */
	@Override
	public int getUom(int leg) throws OException 
	{
		return -1;
	}

	@Override
	public double getTradePrice(int leg) throws OException 
	{
		return 0.0;
	}

	@Override
	public double getInterestRate() throws OException 
	{
		return 0.0;
	}

    /**
     * Returns invalid date
     */
	@Override
	public int getPaymentDate(int leg) throws OException 
	{
		return 0;
	}

    /**
     * Returns value of Tran Info field 'IsCoverage'
     */
	@Override
	public String getIsCoverage() throws OException 
	{
		String isCoverage = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, EndurTranInfoField.IS_COVERAGE.toString());
		
		if (isCoverage == null || "".equalsIgnoreCase(isCoverage))
		{
			isCoverage = "No";
		}
		
		return isCoverage;
	}

    /**
     * Returns invalid value
     */
	@Override
	public int getFromCurrency(int leg) throws OException 
	{
		return -1;
	}

    /**
     * Returns invalid value
     */
	@Override
	public int getToCurrency(int leg) throws OException 
	{
		return -1;
	}

    /**
     * Returns invalid date
     */
    @Override
    public int getSettlementDate(int leg) throws OException 
    {
        return 0;
    }

    @Override
    public String getLocation(int leg) throws OException 
    {
        return "";
    }

    /**
     * Returns invalid value
     */
    @Override
    public int getForm(int leg) throws OException 
    {
        return -1;
    }

    /**
     * Returns invalid value
     */
    @Override
    public int getPurity(int leg) throws OException 
    {
        return -1;
    }

	@Override
	public int getTranGroup() throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_TRAN_GROUP.toInt());
	}

	@Override
	public int getInternalLentity() throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_INTERNAL_LENTITY.toInt());
	}

	@Override
	public String getCoverageText() throws OException 
	{
		/* Empty until we clarity on what this actually is */
		return "";
	}

	@Override
	public int getEndDate() throws OException
	{
		return 0;
	}
}
