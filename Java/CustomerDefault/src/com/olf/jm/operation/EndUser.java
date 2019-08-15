package com.olf.jm.operation;



/**
 * This is a POJO class for End User.
 * 
 * @author YadavP03
 * @version 1.0
 */
public class EndUser {
	private final String groupCompany;
	private final String endUserName;

	public EndUser(String groupCompany,String endUserName){
		this.groupCompany = groupCompany;
		this.endUserName = endUserName;

	}
 public String getGroupCompany() {
		return groupCompany;
	}
	public String getEndUserName() {
		return endUserName;
	}
	@Override
	public String toString() {
		return groupCompany + "\t" + endUserName;

	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}

		EndUser endUser = (EndUser) obj;
		return ((groupCompany != null && groupCompany.equals(endUser.getGroupCompany())))
				&& ((endUserName != null && endUserName.equals(endUser.getEndUserName())));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((groupCompany == null) ? 0 : groupCompany.hashCode());
		result = prime * result + ((endUserName == null) ? 0 : endUserName.hashCode());
		return result;
	}

	   
}
