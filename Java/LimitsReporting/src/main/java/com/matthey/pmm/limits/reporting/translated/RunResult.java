package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class RunResult {
    public static final String DATE_SEPARATOR = ", ";
    public static final String DATE_PATTERN = "dd-MM-yy";
    
	private LocalDateTime runTime;
	private String runType;
	private String desk;
	private String metal;
	private int liquidityLowerLimit;
	private int liquidityUpperLimit;
	private int liquidityMaxLiability;
	private int positionLimit;
	private boolean breach;
	private String liquidityBreachLimit;
	private double currentPosition;
	private int liquidityDiff;
	private double breachTOz;
	private double breachGBP;
	private boolean critical;
	private String breachDates;	
	
	public RunResult (final LocalDateTime runTime, 
			final String runType, 
			final String desk,
			final String metal, 
			final int liquidityLowerLimit, 
			final int liquidityUpperLimit,
			final int liquidityMaxLiability,
			final int positionLimit,
			final boolean breach,
			final String liquidityBreachLimit,
			final double currentPosition,
			final int liquidityDiff,
			final double breachTOz,
			final double breachGBP,
			final boolean critical,
			final String breachDates) {
		this.runTime = runTime;
		this.runType = runType;
		this.desk = desk;
		this.metal = metal;
		this.liquidityLowerLimit = liquidityLowerLimit;
		this.liquidityUpperLimit = liquidityUpperLimit;
		this.positionLimit = positionLimit;
		this.breach = breach;
		this.liquidityBreachLimit = liquidityBreachLimit;
		this.currentPosition = currentPosition;
		this.liquidityDiff = liquidityDiff;
		this.breachTOz = breachTOz;
		this.breachGBP = breachGBP;
		this.critical = critical;
		this.breachDates = breachDates;
	}
	
	public final static DateTimeFormatter dateFormat;
	static {
		try {
			dateFormat = DateTimeFormat.forPattern(DATE_PATTERN);			
		} catch (Exception ex) {
			throw new RuntimeException ("can't get date time format for dd-MM-yy");
		}
	}

	public static String getBreachDates (final LimitsReportingConnector connector,
			final boolean breach,
			final String runType,
			final String liquidityBreachLimit,
			final String desk,
			final String metal) {
      if (breach) {
    	  String prevBreachDatestRaw = connector.getPreviousBreachDates(runType, connector.getRunDate(), liquidityBreachLimit, desk, metal);
    	  List<String> previousBreachDatesWithoutRunDate = (prevBreachDatestRaw != null)?
    			  Arrays.asList(prevBreachDatestRaw.split(DATE_SEPARATOR)):new ArrayList<String>();
    	  StringBuilder previousBreachDates = new StringBuilder();
    	  boolean first=true;
    	  for (String runDate : previousBreachDatesWithoutRunDate) {
    		  if (!first) {
        		  previousBreachDates.append(", " + runDate + dateFormat.print(connector.getRunDate()));
    		  } else {
    			  first = false;
        		  previousBreachDates.append(runDate + dateFormat.print(connector.getRunDate()));
    		  }
    	  }
    	  return previousBreachDates.toString();
      } else {
    	  return "";
      }
	}

	public LocalDateTime getRunTime() {
		return runTime;
	}

	public void setRunTime(LocalDateTime runTime) {
		this.runTime = runTime;
	}

	public String getRunType() {
		return runType;
	}

	public void setRunType(String runType) {
		this.runType = runType;
	}

	public String getDesk() {
		return desk;
	}

	public void setDesk(String desk) {
		this.desk = desk;
	}

	public String getMetal() {
		return metal;
	}

	public void setMetal(String metal) {
		this.metal = metal;
	}

	public int getLiquidityLowerLimit() {
		return liquidityLowerLimit;
	}

	public void setLiquidityLowerLimit(int liquidityLowerLimit) {
		this.liquidityLowerLimit = liquidityLowerLimit;
	}

	public int getLiquidityUpperLimit() {
		return liquidityUpperLimit;
	}

	public void setLiquidityUpperLimit(int liquidityUpperLimit) {
		this.liquidityUpperLimit = liquidityUpperLimit;
	}

	public int getLiquidityMaxLiability() {
		return liquidityMaxLiability;
	}

	public void setLiquidityMaxLiability(int liquidityMaxLiability) {
		this.liquidityMaxLiability = liquidityMaxLiability;
	}

	public int getPositionLimit() {
		return positionLimit;
	}

	public void setPositionLimit(int positionLimit) {
		this.positionLimit = positionLimit;
	}

	public boolean isBreach() {
		return breach;
	}

	public void setBreach(boolean breach) {
		this.breach = breach;
	}

	public String getLiquidityBreachLimit() {
		return liquidityBreachLimit;
	}

	public void setLiquidityBreachLimit(String liquidityBreachLimit) {
		this.liquidityBreachLimit = liquidityBreachLimit;
	}

	public double getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(double currentPosition) {
		this.currentPosition = currentPosition;
	}

	public int getLiquidityDiff() {
		return liquidityDiff;
	}

	public void setLiquidityDiff(int liquidityDiff) {
		this.liquidityDiff = liquidityDiff;
	}

	public double getBreachTOz() {
		return breachTOz;
	}

	public void setBreachTOz(double breachTOz) {
		this.breachTOz = breachTOz;
	}

	public double getBreachGBP() {
		return breachGBP;
	}

	public void setBreachGBP(double breachGBP) {
		this.breachGBP = breachGBP;
	}

	public boolean isCritical() {
		return critical;
	}

	public void setCritical(boolean critical) {
		this.critical = critical;
	}

	public String getBreachDates() {
		return breachDates;
	}

	public void setBreachDates(String breachDates) {
		this.breachDates = breachDates;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (breach ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(breachGBP);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(breachTOz);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (critical ? 1231 : 1237);
		temp = Double.doubleToLongBits(currentPosition);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((desk == null) ? 0 : desk.hashCode());
		result = prime * result + ((liquidityBreachLimit == null) ? 0 : liquidityBreachLimit.hashCode());
		result = prime * result + liquidityDiff;
		result = prime * result + liquidityLowerLimit;
		result = prime * result + liquidityMaxLiability;
		result = prime * result + liquidityUpperLimit;
		result = prime * result + ((metal == null) ? 0 : metal.hashCode());
		result = prime * result + positionLimit;
		result = prime * result + ((runTime == null) ? 0 : runTime.hashCode());
		result = prime * result + ((runType == null) ? 0 : runType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RunResult other = (RunResult) obj;
		if (breach != other.breach)
			return false;
		if (Double.doubleToLongBits(breachGBP) != Double.doubleToLongBits(other.breachGBP))
			return false;
		if (Double.doubleToLongBits(breachTOz) != Double.doubleToLongBits(other.breachTOz))
			return false;
		if (critical != other.critical)
			return false;
		if (Double.doubleToLongBits(currentPosition) != Double.doubleToLongBits(other.currentPosition))
			return false;
		if (desk == null) {
			if (other.desk != null)
				return false;
		} else if (!desk.equals(other.desk))
			return false;
		if (liquidityBreachLimit == null) {
			if (other.liquidityBreachLimit != null)
				return false;
		} else if (!liquidityBreachLimit.equals(other.liquidityBreachLimit))
			return false;
		if (liquidityDiff != other.liquidityDiff)
			return false;
		if (liquidityLowerLimit != other.liquidityLowerLimit)
			return false;
		if (liquidityMaxLiability != other.liquidityMaxLiability)
			return false;
		if (liquidityUpperLimit != other.liquidityUpperLimit)
			return false;
		if (metal == null) {
			if (other.metal != null)
				return false;
		} else if (!metal.equals(other.metal))
			return false;
		if (positionLimit != other.positionLimit)
			return false;
		if (runTime == null) {
			if (other.runTime != null)
				return false;
		} else if (!runTime.equals(other.runTime))
			return false;
		if (runType == null) {
			if (other.runType != null)
				return false;
		} else if (!runType.equals(other.runType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RunResult [runTime=" + runTime + ", runType=" + runType + ", desk=" + desk + ", metal=" + metal
				+ ", liquidityLowerLimit=" + liquidityLowerLimit + ", liquidityUpperLimit=" + liquidityUpperLimit
				+ ", liquidityMaxLiability=" + liquidityMaxLiability + ", positionLimit=" + positionLimit + ", breach="
				+ breach + ", liquidityBreachLimit=" + liquidityBreachLimit + ", currentPosition=" + currentPosition
				+ ", liquidityDiff=" + liquidityDiff + ", breachTOz=" + breachTOz + ", breachGBP=" + breachGBP
				+ ", critical=" + critical + ", dateFormat=" + dateFormat + "]";
	}
	
    // used by FreeMarker template
	public boolean getCriticalBreachDates() {
		return (   liquidityBreachLimit.equals("Upper Limit") 
				&& breachDates.split(DATE_SEPARATOR).length > 20) 
			|| (   liquidityBreachLimit.equals ("Lower Limit") 
			    && breachDates.split(DATE_SEPARATOR).length > 10);
	}
}
