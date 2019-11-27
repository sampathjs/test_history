package com.jm.shanghai.accounting.udsr.model.fixed;

import java.util.List;

/**
 * Interface providing meta data about user tables.
 * @author jwaechter
 * @version 1.0
 * @deprecated  no longer used.
 */
public interface UserTable {
	public String getName();
	
	public List<UserTableColumn> getColumns ();
	
	public String getDescription ();
}
