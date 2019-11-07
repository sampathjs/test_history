package com.olf.recon.enums;

public enum JdeMetalLedgerExtractField 
{
    DEAL_NUM("TROTRF"),
    INSTRUMENT_TYPE("TROIST"),
    TRAN_STATUS("TROSTS"),
    INTERNAL_LENTITY_CODE("TROILC"),
    INTERNAL_LENTITY_DESC("TROILD"),
	EXTERNAL_LENTITY_CODE("TROLEC"),
	EXTERNAL_LENTITY_DESC("TROLED"),
    EXTERNAL_BUNIT_CODE("TROBUC"),
	EXTERNAL_BUNIT_DESC("TROMNM"),
    TRADE_DATE("TROTDT"),
    VALUE_DATE("TROVDT"),
    METAL_CURRENCY("TROFCR"),
    POSITION_TOZ("TROFVL"),
    ML_BATCH_NUMBER("TROGBN"),
    ML_TRANSACTION_NUMBER("TROGTN"),
    RECONCILIATION_SOURCE("TRORSC"),
    LOCATION("TROTLC");
    
    private final String name;

    private JdeMetalLedgerExtractField(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
