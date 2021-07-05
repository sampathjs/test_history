package com.matthey.pmm.toms.model.init;

import java.util.ArrayList;
import java.util.List;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;

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
		List<SqlStatement> allStatements = referenceTypeDataInsert (database);
		
		return allStatements.toArray(new SqlStatement[allStatements.size()]);
	}
	
	private List<SqlStatement> referenceTypeDataInsert (Database database) {
		List<SqlStatement> results = new ArrayList<> (DefaultReferenceType.values().length);
		String insertTemplate = 
				"INSERT INTO reference_type (reference_type_id, name) VALUES (%s, '%s')";
		for (ReferenceTypeTo refType : DefaultReferenceType.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplate, refType.id(), refType.name())));
		}
		return results;
	}

}
