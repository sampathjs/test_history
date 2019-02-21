package com.matthey.openlink.accounting.ops;

import java.io.Serializable;

public class ResetKey implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int insNum;
	private int paramSeqNum;
	private int resetSeqNum;
	private int profileSeqNum;
	
	public ResetKey() {
	}
	
	public ResetKey(int insNum, int paramSeqNum, int resetSeqNum, int profileSeqNum) {
		this.insNum = insNum;
		this.paramSeqNum = paramSeqNum;
		this.resetSeqNum = resetSeqNum;
		this.profileSeqNum = profileSeqNum;
	}

	public int getInsNum() {
		return insNum;
	}

	public void setInsNum(int insNum) {
		this.insNum = insNum;
	}

	public int getParamSeqNum() {
		return paramSeqNum;
	}

	public void setParamSeqNum(int paramSeqNum) {
		this.paramSeqNum = paramSeqNum;
	}

	public int getResetSeqNum() {
		return resetSeqNum;
	}

	public void setResetSeqNum(int resetSeqNum) {
		this.resetSeqNum = resetSeqNum;
	}

	public int getProfileSeqNum() {
		return profileSeqNum;
	}

	public void setProfileSeqNum(int profileSeqNum) {
		this.profileSeqNum = profileSeqNum;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + insNum;
		result = prime * result + paramSeqNum;
		result = prime * result + profileSeqNum;
		result = prime * result + resetSeqNum;
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
		ResetKey other = (ResetKey) obj;
		if (insNum != other.insNum)
			return false;
		if (paramSeqNum != other.paramSeqNum)
			return false;
		if (profileSeqNum != other.profileSeqNum)
			return false;
		if (resetSeqNum != other.resetSeqNum)
			return false;
		return true;
	}

}
