package com.matthey.pmm.toms.model.init;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.transport.ReferenceTo;
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
		allStatements.addAll(referenceDataInsert(database));
		
		return allStatements.toArray(new SqlStatement[allStatements.size()]);
	}
	
	private List<SqlStatement> referenceDataInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (DefaultReference.values().length);
		String insertTemplate = 
				"INSERT INTO reference (reference_id, value, display_name, reference_type_id, endur_id) VALUES (%s, '%s', %s, %s, %s)";
		for (ReferenceTo ref : DefaultReference.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplate, 
					ref.id(), ref.name(), ref.displayName()!=null?ref.displayName():"NULL",
							ref.idType(), ref.endurId() != null?ref.endurId():"NULL")));
			List<ReferenceTo> duplicates = 
					DefaultReference.asList()
					   .stream()
					   .filter(x -> x.name().equals(ref.name()) && x.idType() == ref.idType())
					   .collect(Collectors.toList());
			if (duplicates.size() > 1) {
				System.out.println(duplicates);
			}
		}
		return results;
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
