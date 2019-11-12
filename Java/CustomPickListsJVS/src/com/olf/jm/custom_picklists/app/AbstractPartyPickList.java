package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/*
 * History:
 * 2015-10-13	V1.0	jwaechter	-	Initial Version
 * 2016-01-20	V1.1	jwaechter	-   Added party function as filter.
 * 2016-01-21	V1.2	jwaechter	-   now keeping sorting order as configured in pick list configuration
 * 
 */

/**
 * Abstract class filtering a party pick list 
 * to display only the combination of party status (internal or external)
 * and party class (legal or business) that the concrete instance is configured for.
 * It's intended to create subclasses of this class that are being executed there.
 * @author jwaechter
 * @version 1.0
 */
public abstract class AbstractPartyPickList implements IScript {
	public static enum EnumPartyClass {
		LEGAL_ENTITY(0), BUSINESS_UNIT(1);
		private final int value;

		private EnumPartyClass (int value) {
			this.value = value; 
		}

		public int getValue () {
			return value;
		}
	}

	public static enum EnumInternalExternal {
		INTERNAL(0), EXTERNAL(1);
		private final int value;

		private EnumInternalExternal (int value) {
			this.value = value; 
		}

		public int getValue () {
			return value;
		}
	}
	
	private final EnumPartyClass partyClass;
	private final EnumInternalExternal partyStatus;
	private final PARTY_FUNCTION_TYPE functionType;

	/**
	 * Creates an instance of the picklist filtering for parties matching
	 * the criteria provided in the constructor
	 * @param partyClass
	 * @param partyStatus 
	 * @param functionType null = all function types
	 */
	public AbstractPartyPickList (EnumPartyClass partyClass, EnumInternalExternal partyStatus, 
			PARTY_FUNCTION_TYPE functionType) {
		this.partyClass = partyClass;
		this.partyStatus = partyStatus;
		this.functionType = functionType;
	}
	
	/**
	 * Creates an instance of the picklist filtering for parties matching
	 * the party class, party status as provided in the arguments and 
	 * for any function type.
	 * @param partyClass
	 * @param partyStatus 
	 */
	public AbstractPartyPickList (EnumPartyClass partyClass, EnumInternalExternal partyStatus) {
		this(partyClass, partyStatus, null);
	}


	@Override
	public void execute(IContainerContext context) throws OException
	{
		String sql =
				"\nSELECT p.party_id, p.short_name FROM party p"
			+   ((functionType != null)?
					"\nINNER JOIN party_function pf ON pf.party_id = p.party_id AND pf.function_type = " + functionType.jvsValue()
					:"");
		if (partyClass != null || partyStatus != null) {
			sql += "\nWHERE ";
			if (partyClass != null) {
				sql += "\np.party_class = " + partyClass.getValue();
				if (partyStatus != null) {
					sql+= " AND ";
				}
			}
			if (partyStatus != null) {
				sql += "p.int_ext = " + partyStatus.getValue();
			}
		}
		
		Table retValues = context.getReturnTable();
		boolean isV17 = retValues.getColName(1).equalsIgnoreCase("table_value") == false;
		if (!isV17) {
			retValues = retValues.getTable(("table_value"), 1);
		}

		Table sqlResult = Table.tableNew("sql_result");
		int ret = DBaseTable.execISql(sqlResult, sql);

		for (int rowRetTable = retValues.getNumRows(); rowRetTable >= 1; rowRetTable--) {
			int partyIdRetTable = retValues.getInt((isV17 ? "value" : "id"), rowRetTable);
			boolean found = false;
			for (int rowSqlTable = sqlResult.getNumRows(); rowSqlTable >= 1; rowSqlTable--) {
				int partyIdSqlTable = sqlResult.getInt("party_id", rowSqlTable);
				if (partyIdSqlTable == partyIdRetTable) {
					found = true;
					sqlResult.delRow(partyIdSqlTable);
					break;
				}
			}
			if (!found) {
				retValues.delRow(rowRetTable);
			}
		}
		
	}	
}
