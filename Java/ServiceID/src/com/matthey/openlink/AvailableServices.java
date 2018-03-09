package com.matthey.openlink;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.control.Service;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;


@ScriptCategory({ EnumScriptCategory.Generic })
public class AvailableServices extends AbstractGenericScript {


	@Override
	public Table execute(Session session, ConstTable table) {

		int buildNumber = session.getBuildNumber();
		Service[] applicationServices = session.getControlFactory().getServices();
		for (Service executionService : applicationServices) {
			// walk the list of known services
			System.out.println( "Service:" + executionService .getName());
			System.out.println( "Running:" + executionService .isRunning());
			System.out.println( "Service:" + executionService .getServiceType().toString());
		}
		
		int id = AvailableServices.findServiceID(session, "Reval");
		return null;
	}

	/**
	 * determine service is active and return Application Service ID to consumner 
	 * @param session
	 * @param service  name of the service to get ID for
	 */
	private int getServiceID(Session session, String service) {
		int serviceID = 0;

		try (Service serviceDetails = session.getControlFactory().getService(service)) {
			System.out.println("Service:" + serviceDetails.getName());
//			if (!serviceDetails.isRunning()) {
//				throw new AvailableServicesException(String.format("%s SERVICE NOT RUNNING", service));
//			}
			Table serviceTable = serviceDetails.getStatusTable();
			serviceID = serviceTable.getInt("service_id", 0);
			
		} catch (OpenRiskException ore) {
			throw new AvailableServicesException(String.format(
					"Unexpected error: %s", ore.getLocalizedMessage()), ore);
		}
		
//		if (serviceID == 0)
//			throw new AvailableServicesException(String.format("Unable to obtain %s service ID", service));

		return serviceID;
	}

/**
 * public interface to locate service id
 * @throws AvailableServicesException if service not found or offline 	
 */
	public static int findServiceID(Session session, String service) {
		return new AvailableServices().getServiceID(session, service);
	}

/**
 * 
 * If request causes Product error this will be re-thrown for the product to capture...
 */
	public static int findServiceID(String service) {
		int serviceID = -1;
		try {
			serviceID = new AvailableServices().getServiceID(Application.getInstance().getCurrentSession(), service);
		} catch (AvailableServicesException e) {
			
			if (e.getCause() != null ) {
				Application.getInstance().getCurrentSession().logStatus(e.getCause().getLocalizedMessage());
				throw new OpenRiskException(e.getCause());
			}
			//System.out.println("ERROR processing findServiceID:" + e.getCause().getLocalizedMessage());
			Application.getInstance().getCurrentSession().logStatus("ERROR processing findServiceID:" + e.getLocalizedMessage());
			throw new OpenRiskException(e.getCause());
		}
		return serviceID;
	}
	
}

