package com.matthey.openlink.pnl;


public class MTL_Position_Enums 
{
	public static class FixedUnfixed
	{
		public static final int FIXED = 0;
		public static final int UNFIXED = 1;
	}
	
	public static class PositionType
	{
		public static final int CURRENCY = 0;
		public static final int METAL = 1;
	}	
	
	// Spread fields on swaps are stored as Tran Info, as pricing logic is more complex than just using core spread functionality
	public static final String s_metalSpreadTranInfoField = "Metal Price Spread";
	public static final String s_usdFXSpreadTranInfoField = "FX Rate Spread";
	public static final String s_cbRateTranInfoField = "CB Rate";
	
}
