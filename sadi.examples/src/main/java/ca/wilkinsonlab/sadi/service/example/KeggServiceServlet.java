package ca.wilkinsonlab.sadi.service.example;

import javax.xml.rpc.ServiceException;

import keggapi.KEGGLocator;
import keggapi.KEGGPortType;
import ca.wilkinsonlab.sadi.service.simple.SimpleAsynchronousServiceServlet;

@SuppressWarnings("serial")
public abstract class KeggServiceServlet extends SimpleAsynchronousServiceServlet 
{
	KEGGPortType keggService;
	
	protected synchronized KEGGPortType getKeggService() throws ServiceException 
	{
		if(keggService == null) {
			keggService = new KEGGLocator().getKEGGPort();
		}
		return keggService;
	}
}
