package com.matthey.openlink.reporting.runner.generators;

public class DefinitionParameter {

	 private final String dataSource;
	 private final String dataName;
	 private final String dataValue;
	 
	public DefinitionParameter(String dataSource, String dataName, String dataValue) {
		this.dataSource = dataSource;
		this.dataName = dataName;
		this.dataValue = dataValue;
	}

	
	
	public String getDataSource() {
		return dataSource;
	}



	public String getDataName() {
		return dataName;
	}



	public String getDataValue() {
		return dataValue;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataName == null) ? 0 : dataName.hashCode());
		result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
		result = prime * result + ((dataValue == null) ? 0 : dataValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (getClass() != obj.getClass()){
			return false;
		}
		DefinitionParameter other = (DefinitionParameter) obj;
		if (dataName == null) {
			if (other.dataName != null){
				return false;
			}
		} else if (!dataName.equals(other.dataName)){
			return false;
		}
		if (dataSource == null) {
			if (other.dataSource != null){
				return false;
			}
		} else if (!dataSource.equals(other.dataSource)){
			return false;
		}
		if (dataValue == null) {
			if (other.dataValue != null){
				return false;
			}
		} else if (!dataValue.equals(other.dataValue)){
			return false;
		}
		return true;
	}
	 
	 
}
