package ca.wilkinsonlab.sadi.client.virtual.biomoby;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.biomoby.client.CentralImpl;
import org.biomoby.client.MobyRequest;
import org.biomoby.shared.Central;
import org.biomoby.shared.MobySecondaryData;
import org.biomoby.shared.MobyService;
import org.biomoby.shared.data.MobyContentInstance;
import org.biomoby.shared.data.MobyDataInstance;
import org.biomoby.shared.data.MobyDataJob;
import org.biomoby.shared.data.MobyDataObject;
import org.biomoby.shared.data.MobyDataSecondaryInstance;

public class BioMobyHelper
{
	private static final Logger log = Logger.getLogger(BioMobyHelper.class);
	
	public static final String URI_PREFIX = "http://biordf.net/moby/";
	public static final String MOBY_NAMESPACE_PREFIX = "http://biomoby.org/RESOURCES/MOBY-S/Namespaces/";
	public static final String MOBY_DATATYPE_PREFIX = "http://biomoby.org/RESOURCES/MOBY-S/Objects/";
	
	static final Map<String, String> EMPTY_PARAMETER_MAP = new HashMap<String, String>(0);
	
	/**
	 * Returns a string representation of the given MobyService.
	 * @param service the MobyService
	 * @return a string repesentation of the given MobyService
	 */
	public static String serviceToString( MobyService service )
	{
		return getServiceURI(service);
	}
	
	/**
	 * Returns a unique URI for the given MobyService.
	 * This is necessary because apparently signature URLs aren't always
	 * unique.
	 * @param service the MobyService
	 * @return a unique URI for the given MobyService
	 */
	public static String getServiceURI( MobyService service )
	{
		return String.format( "http://biomoby.org/RESOURCES/MOBY-S/ServiceInstances/%s,%s", service.getAuthority(), service.getName() );
	}

	/**
	 * Returns true if the specified BioMoby datatype is a primitive.
	 * @param datatype the name of the datatype
	 * @return true if the datatype is a primitive, false otherwise
	 */
	public static boolean isPrimitive(String datatype)
	{
		if ( datatype.equals("String") )
			return true;
		if ( datatype.equals("Integer") )
			return true;
		if ( datatype.equals("Float") )
			return true;
		if ( datatype.equals("Boolean") )
			return true;
		if ( datatype.equals("DateTime") )
			return true;
		return false;
	}
	
	/**
	 * Returns true if the specified URI can be converted to a BioMoby triple.
	 * @param uri the URI
	 * @return true if the URI can be converted, false otherwise
	 * @deprecated
	 */
	public static boolean canParse(String uri)
	{
		return uri.startsWith(URI_PREFIX) &&
		       uri.substring(URI_PREFIX.length()).contains("/");
	}
	
	/**
	 * Converts a URI to a simple MobyDataObject.
	 * @param uri the URI to convert
	 * @return the converted MobyDataObject
	 * @throws URISyntaxException if the URI cannot be converted
	 * @deprecated use the method on the registry
	 */
	public static MobyDataObject convertUriToMobyDataObject(String uri) throws URISyntaxException
	{
		if (uri.startsWith(URI_PREFIX)) {
			int index = URI_PREFIX.length();
			String[] components = uri.substring(index).split("/");
			if (components.length >= 2) {
				String namespace = components[0];
				String id = components[1];
				return new MobyDataObject(namespace, id);
			} else {
				throw new URISyntaxException(uri, "unable to determine namespace/id");
			}
		} else {
			throw new URISyntaxException(uri, "expected URI prefix " + URI_PREFIX);
		}
	}
	
	/**
	 * Converts a MobyDataObject to a URI.
	 * @param obj the MobyDataObject to convert
	 * @return the converted URI
	 * @deprecated use the method on the registry
	 */
	public static String convertMobyDataObjectToUri(MobyDataObject obj)
	{
		// TODO deal with the issue of multiple namespaces
		return String.format("%s%s/%s", URI_PREFIX, obj.getNamespaces()[0].getName(), obj.getId());
	}
	
	/**
	 * Converts a MobyDataObject to RDF using <code>org.moby2.biomoby.resource.xslt.moby2rdf.xslt</code>.
	 * 
	 * @param obj the MobyDataObject to convert
	 * @return the converted RDF
	 */
	public static String convertMobyDataObjectToRdf( MobyDataObject obj )
			throws TransformerException
	{
		// Transform the output XML to RDF using the Xalan XSLT processor
		TransformerFactory factory = TransformerFactory.newInstance();
	
		URL mobyXslt = BioMobyHelper.class.getResource( "moby2rdf.xslt" );
		Transformer transformer = factory.newTransformer( new StreamSource( mobyXslt.toString() ) );

		obj.setXmlMode( MobyDataInstance.SERVICE_XML_MODE );
		StringReader reader = new StringReader( obj.toXML() );
		StringWriter writer = new StringWriter();
		
		// Indent the RDF for easier debugging
		transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
		transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );	

		transformer.transform( new StreamSource( reader ), new StreamResult( writer ) );
			
		return writer.toString();
	}
	
	/**
	 * Call a MobyService with the specified MobyDataObject as input.
	 * @param service
	 * @param input
	 * @return the MobyContentInstance
	 * @throws Exception
	 */
	public static MobyContentInstance callService(MobyService service, MobyDataObject input) throws Exception
	{
		return callService(service, input, EMPTY_PARAMETER_MAP);
	}
	
	/**
	 * Call a MobyService with the specified MobyContentInstance as input.
	 * The MobyContentInstance can contain multiple jobs (so multiple inputs
	 * can be processed with a single service invocation).
	 * @param service
	 * @param input
	 * @return the MobyContentInstance
	 * @throws Exception
	 */
	public static MobyContentInstance callService(MobyService service, MobyContentInstance input) throws Exception
	{
		return callService(service, input, EMPTY_PARAMETER_MAP);
	}
	
	/**
	 * Call a MobyService with the specified MobyDataObject as input
	 * and the specified secondary parameters.
	 * @param service
	 * @param input
	 * @return the MobyContentInstance
	 * @throws Exception
	 */
	public static MobyContentInstance callService(MobyService service, MobyDataObject input,
			Map<String, String> secondaryParameters) throws Exception
	{
		MobyContentInstance content = new MobyContentInstance();
		MobyDataJob job = new MobyDataJob();
		job.put(input.getId(), input);
		content.put(input.getId(), job);
		return callService(service, content, secondaryParameters);
	}
	
	/**
	 * Call a MobyService with the specified MobyContentInstance as input
	 * and the specified secondary parameters.  The MobyContentInstance can
	 * contain multiple jobs (so multiple inputs can be processed with a
	 * single service invocation).
	 * @param service
	 * @param input
	 * @return the MobyContentInstance
	 * @throws Exception
	 */
	public static MobyContentInstance callService(MobyService service, MobyContentInstance input,
			Map<String, String> secondaryParameters) throws Exception
	{
		log.trace("retrieving triples from " + serviceToString(service));
		
		Central worker = new CentralImpl();
		MobyRequest request = new MobyRequest(worker);
		request.setService(service);
		request.setInput(input);		
		setSecondaryParameters(request, service, secondaryParameters);
		
//		/* call find service on Registry object to supply tracing information for
//		 * Piam's project...
//		 */
//		try {
//			worker.findService(service);
//		} catch (MobyException e) {
//			log.warn("error calling MobyCentral.findService", e);
//		}
		
		if (service.isAsynchronous() ) {
			return invokeAsynchronousService(request);
		} else {
			return invokeSynchronousService(request);
		}
	}
	
	/**
	 * Determine the default values for the secondary parameters of a
	 * service, and set then set these values inside a MobyRequest 
	 * (service call).
	 * 
	 * @param request the MobyRequest representing a service call.
	 * @param service the service that is going to be called.
	 */
	public static void setSecondaryParametersToDefaults(MobyRequest request, MobyService service)
	{
		setSecondaryParameters(request, service, EMPTY_PARAMETER_MAP);
	}

	/**
	 * Set the secondary parameters of a service call, using the default
	 * value where no other value has been specified.
	 * 
	 * @param request the MobyRequest representing a service call.
	 * @param service the service that is going to be called.
	 * @param params a map from parameter name to value
	 */
	public static void setSecondaryParameters(MobyRequest request, MobyService service, Map<String, String> params)
	{
		MobySecondaryData parameters[] = service.getSecondaryInputs();
		MobyDataSecondaryInstance parameterValues[] = new MobyDataSecondaryInstance[parameters.length];
		
		for(int i = 0; i < parameters.length; i++) {
			parameterValues[i] = new MobyDataSecondaryInstance(parameters[i]);
			String parameterName = parameters[i].getName();
			if (params.containsKey(parameterName))
				parameterValues[i].setValue(params.get(parameterName));
			else
				parameterValues[i].setValue(parameters[i].getDefaultValue());
		}
		
		request.setSecondaryInput(parameterValues);
	}
	
	/**
	 * Invokes an asynchronous BioMoby service according to the given MobyRequest.
	 * @param request the MobyRequest
	 * @return a MobyContentInstance returned by the service
	 */
	private static MobyContentInstance invokeAsynchronousService(MobyRequest request) throws Exception
	{
		throw new UnsupportedOperationException( "Support for asynchronous BioMoby services not yet implemented" );
	}
	
	/**
	 * Invokes a synchronous BioMoby service according to the given MobyRequest.
	 * I wish MobyRequest.invokeService threw a more specific exception so we could, too.
	 * @param request the MobyRequest
	 * @return a MobyContentInstance returned by the service
	 */
	private static MobyContentInstance invokeSynchronousService(MobyRequest request) throws Exception
	{
		return request.invokeService();
	}
}
