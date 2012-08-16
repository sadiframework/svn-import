package org.sadiframework.service;

import org.sadiframework.SADIException;
import org.sadiframework.beans.ServiceBean;
import org.sadiframework.service.ServiceServlet;

public class ServiceServletHelper
{
	public static ServiceBean getServiceDescription(ServiceServlet servlet) throws SADIException
	{
		return (ServiceBean)servlet.createServiceDescription();
	}
}
