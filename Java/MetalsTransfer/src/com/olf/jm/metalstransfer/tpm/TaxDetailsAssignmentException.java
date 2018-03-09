package com.olf.jm.metalstransfer.tpm;

/**
 * Exception raised when a taxable metals transfer deal does not have the tax type and sub-type set on the deal.
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 30-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class TaxDetailsAssignmentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TaxDetailsAssignmentException(String message) {
        super(message);
    }
}
