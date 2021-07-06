package com.matthey.pmm.toms.model.init;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultAttributeCalculation;
import com.matthey.pmm.toms.enums.v1.DefaultExpirationStatus;
import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultProcessTransition;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.ExpirationStatusTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
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
		allStatements.addAll(attributeCalculationDataInsert(database));
		allStatements.addAll(expirationStatusDataInsert(database));
		allStatements.addAll(orderStatusDataInsert(database));
		allStatements.addAll(processTransitionInsert(database));
		
		return allStatements.toArray(new SqlStatement[allStatements.size()]);
	}
	
	private List<SqlStatement> processTransitionInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (DefaultProcessTransition.values().length*20);
		String insertTemplateMain = 
				"INSERT INTO process_transition (process_transition_id, reference_category_id, from_status_id, to_status_id) VALUES (%s, %s, %s, %s)";
		String insertTemplateAttributeList = 
				"INSERT INTO process_transition_attributes (process_transition_id, unchangeable_attribute) VALUES (%s, '%s')";
		for (ProcessTransitionTo pt : DefaultProcessTransition.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplateMain, 
					pt.id(), pt.referenceCategoryId(), pt.fromStatusId(),
					pt.toStatusId())));
			if (pt.unchangeableAttributes() != null) {
				for (String unchangeableAttribute : pt.unchangeableAttributes()) {
					results.add(new RawSqlStatement(String.format(insertTemplateAttributeList, 
							pt.id(), unchangeableAttribute)));
				}
			}
		}
		return results;
	}

	private List<SqlStatement> orderStatusDataInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (DefaultAttributeCalculation.values().length);
		String insertTemplate = 
				"INSERT INTO order_status (order_status_id, name_reference_id, order_type_reference_id) VALUES (%s, %s, %s)";
		
		for (OrderStatusTo os : DefaultOrderStatus.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplate, 
					os.id(), os.idOrderStatusName(), os.idOrderTypeName())));
		}
		return results;
	}

	private List<SqlStatement> expirationStatusDataInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (DefaultAttributeCalculation.values().length);
		String insertTemplate = 
				"INSERT INTO expiration_status (expiration_status_id, name_reference_id, order_type_reference_id) VALUES (%s, %s, %s)";
		
		for (ExpirationStatusTo es : DefaultExpirationStatus.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplate, 
					es.id(), es.idExpirationStatusName(), es.idOrderTypeName())));
		}
		return results;
	}

	private List<SqlStatement> attributeCalculationDataInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (DefaultAttributeCalculation.values().length*5);
		String insertTemplateMain = 
				"INSERT INTO attribute_calc (attribute_calc_id, class_name, attribute_name, spel_expression) VALUES (%s, '%s', '%s', '%s')";
		String insertTemplateAttributeList = 
				"INSERT INTO attribute_calc_attributes (attribute_calc_id, dependent_attribute_name) VALUES (%s, '%s')";
		for (AttributeCalculationTo act : DefaultAttributeCalculation.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplateMain, 
					act.id(), act.className(), act.attributeName(),
						act.spelExpression())));
			if (act.dependentAttributes() != null) {
				for (String dependentAttribute : act.dependentAttributes()) {
					results.add(new RawSqlStatement(String.format(insertTemplateAttributeList, 
							act.id(), dependentAttribute)));
				}				
			}
		}
		return results;
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
