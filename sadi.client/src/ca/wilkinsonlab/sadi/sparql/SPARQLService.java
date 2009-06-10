package ca.wilkinsonlab.sadi.sparql;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author Ben Vandervalk
 */
public abstract class SPARQLService extends SPARQLEndpoint implements Service 
{
	public final static Log log = LogFactory.getLog(SPARQLService.class);
	
	public SPARQLService(String endpointURI) {
		this.endpointURI = endpointURI;
	}
	
	public String getServiceURI() {
		return endpointURI;
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<Triple> invokeService(Resource inputNode) throws Exception
	{
		return invokeService(inputNode.getURI());
	}

	public Collection<Triple> invokeService(Resource inputNode, String predicate) throws Exception
	{
		return invokeService(inputNode.getURI(), predicate);
	}

	public Collection<Triple> invokeService(String inputURI) throws Exception
	{
		String query = SPARQLStringUtils.strFromTemplate("CONSTRUCT { %u% ?p ?o } WHERE { %u% ?p ?o }", inputURI, inputURI);
		return constructQuery(query);
	}

	public Collection<Triple> invokeService(String inputURI, String predicate) throws Exception
	{
		String query = SPARQLStringUtils.strFromTemplate("CONSTRUCT { %u% %u% ?o } WHERE { %u% %u% ?o }", 
				inputURI,
				predicate,
				inputURI,
				predicate);
		return constructQuery(query);
	}
	
	@Override
	public String toString()
	{
		return getServiceURI();
	}
}
