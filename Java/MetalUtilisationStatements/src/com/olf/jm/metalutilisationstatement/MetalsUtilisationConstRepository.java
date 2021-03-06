package com.olf.jm.metalutilisationstatement;
 
/**
 * Constant repository entries for the Metals Utilisation Statements.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 16-Dec-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;

public class MetalsUtilisationConstRepository {

    private ConstRepository repo;
    
    /**
     * Initialise.
     */
    public MetalsUtilisationConstRepository() {
        try {
            repo = new ConstRepository("Metals Utilisation Statement");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the metals interest rate index name.
     * 
     * @return index name
     */
    public String getMetalsInterestIndex() {
        try {
            return repo.getStringValue("Metals Interest Index");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the CN metals interest rate index name. 
     * @return
     */
    public String getMetalsInterestIndexCN() {
        try {
            return repo.getStringValue("Metals Interest Index CN");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    

    /**
     * Get the Vostro Account Balances report name
     * 
     * @return report name
     */
    public String getVostroAccountBalanceReport() {
        try {
            return repo.getStringValue("Vostro Account Balance Report");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the Vostro Account Balances report name
     * @return report name
     */
    public String getVostroAccountBalanceReportCN () {
        try {
            return repo.getStringValue("Vostro Account Balance Report CN");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    
    /**
     * Get the Nostro Account Balances report name
     * 
     * @return report name
     */
    public String getNostroAccountBalanceReport() {
        try {
            return repo.getStringValue("Nostro Account Balance Report");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the Nostro Account Balances report name
     * @return report name
     */
    public String getNostroAccountBalanceReportCN() {
        try {
            return repo.getStringValue("Nostro Account Balance Report CN");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the  Back Office definition name for invoices.
     * 
     * @return definition name
     */
    public String getBoInvoiceDefinition() {
        try {
            return repo.getStringValue("BO Invoice Definition");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the  Back Office definition name for invoices.
     * 
     * @return definition name
     */
    public String getCnBoInvoiceDefinition() {
        try {
            return repo.getStringValue("BO Invoice Definition CN");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Get the date for the report run date, should be set to 25 ideally.
     * @return
     */
    public int getCnReportDate()    
    {
    	  try {
              return repo.getIntValue("CN Report Run Date");
          }
          catch (OException e) {
              throw new RuntimeException(e);
          }
    }

}
