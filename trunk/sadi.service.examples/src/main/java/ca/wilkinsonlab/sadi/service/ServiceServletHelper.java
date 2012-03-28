package ca.wilkinsonlab.sadi.service;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.beans.ServiceBean;

public class ServiceServletHelper
{
	public static ServiceBean getServiceDescription(ServiceServlet servlet) throws SADIException
	{
		return (ServiceBean)servlet.createServiceDescription();
	}
}
