package com.matthey.pmm.toms.model.init;

import java.util.List;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;

public class InsertDefaultValues implements CustomSqlChange {

	@Override
	public String getConfirmationMessage() {
		return "Default Values from Enums Stored";
	}

	@Override
	public void setUp() throws SetupException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ValidationErrors validate(Database database) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
		List<SqlStatement> allStatements = referenceDataInsert (database);
		
		return allStatements.toArray(new SqlStatement[allStatements.size()]);
	}
	
	private List<SqlStatement> referenceDataInsert (Database database) {
		
	}

}
