package com.olf.recon.enums;

public enum JdeDealExtractFieldAr 
{
	ID("TROTRF"),
	NOTE("TROEXR"),
	GL_DATE("TRODGJ"),
	ACCOUNT("TROANI"),
	TYPE("TRODCT"),
	DOC_NUM("TRODOC"),
	VALUE_DATE("TROVDT"),
	QTY_TOZ("TROU"),
	AMOUNT("TROAA"),
	CURRENCY("TROCRCD"),
	BATCH_NUM("TROEDBT"),
	TRAN_NUM("TROEDTN")
;
	
	private final String name;

    private JdeDealExtractFieldAr(final String text) 
    {
        this.name = text;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
