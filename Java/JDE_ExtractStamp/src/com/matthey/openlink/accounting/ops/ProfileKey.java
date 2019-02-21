package com.matthey.openlink.accounting.ops;

import java.io.Serializable;

public class ProfileKey implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int insNum;
	private int paramSeqNum;
	private int profileSeqNum;
	
	public ProfileKey() {
	}
	
	public ProfileKey(int insNum, int paramSeqNum, int profileSeqNum) {
		this.insNum = insNum;
		this.paramSeqNum = paramSeqNum;
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
		ProfileKey other = (ProfileKey) obj;
		if (insNum != other.insNum)
			return false;
		if (paramSeqNum != other.paramSeqNum)
			return false;
		if (profileSeqNum != other.profileSeqNum)
			return false;
		return true;
	}
}
