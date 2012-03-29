package ca.wilkinsonlab.sadi.service.proxy;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.Parser;
import org.htmlparser.visitors.TextExtractingVisitor;
import org.stringtree.json.JSONWriter;

import ca.wilkinsonlab.sadi.client.ServiceFactory;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@URI("http://sadiframework.org/services/proxy/er-proxy")
public class EntityRecognitionProxyServlet extends GETProxyServlet
{
	private static final Logger log = Logger.getLogger(EntityRecognitionProxyServlet.class);
	private static final long serialVersionUID = 1L;
	
	@Override
	protected Model createOutputModel()
	{
		Model model = super.createOutputModel();
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("ie", "http://unbsj.biordf.net/information-extraction/ie-sadi-service-ontology.owl#");
		return model;
	}

	@Override
	protected String getProxiedServiceURL(HttpServletRequest request)
	{
		// TODO cache ServiceImpl before starting task...
		try {
			return new PropertiesConfiguration("erproxy.properties").getString("proxiedServiceURL");
		} catch (ConfigurationException e) {
			log.error("error loading configuration from erproxy.properties", e);
			return "http://unbsj.biordf.net/ie-sadi/extractDrugNamesFromText";
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException
	{
		if (request.getServletPath().endsWith("/ping")) {
			if (log.isDebugEnabled())
				log.debug(String.format("pinging service %s", getProxiedServiceURL(request)));
			Map<String, Object> output = new HashMap<String, Object>();
			try {
				ServiceFactory.createService(getProxiedServiceURL(request));
				output.put("alive", true);
			} catch (Exception e) {
				output.put("alive", false);
				output.put("error", e.toString());
			}
			JSONWriter jsonWriter = new JSONWriter();
			response.setContentType("text/javascript");
			response.getWriter().format("%s", jsonWriter.write(output));
		} else {
			super.doGet(request, response);
		}
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.proxy.GETProxyServlet#assembleInput(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Model assembleInput(HttpServletRequest request) throws IOException
	{
		Model inputModel = super.createInputModel();
		for (String inputURL: getParameterValues(request, "inputURL")) {
			if (!StringUtils.isEmpty(inputURL)) {
				try {
					String text = extractText(inputURL);
					Resource inputNode = inputModel.createResource(inputURL, Artjom.InputClass);
					inputNode.addLiteral(Bibo.content, text);
				} catch (Exception e) {
					log.error(String.format("error extracting text from %s", inputURL), e);
				}
			}
		}
		for (String text: getParameterValues(request, "inputText")) {
			if (!StringUtils.isEmpty(text)) {
				Resource inputNode = inputModel.createResource(RdfUtils.createUniqueURI(), Artjom.InputClass);
				inputNode.addLiteral(Bibo.content, text);
			}
		}
		return inputModel;
	}

	protected String extractText(String url) throws Exception
	{
		URL cached = EntityRecognitionProxyServlet.class.getResource(
				String.format("/cache/%s", URLEncoder.encode(url, "UTF-8")));
		if (log.isDebugEnabled())
			log.debug(String.format("reading %s from %s", url, cached != null ? "cache" : "source"));
		Parser parser = new Parser(cached != null ? cached.toExternalForm() : url);
		TextExtractingVisitor visitor = new TextExtractingVisitor();
		parser.visitAllNodesWith(visitor);
		return visitor.getExtractedText();
	}
	
	private static class Artjom
	{
		public static final String NS = "http://unbsj.biordf.net/information-extraction/ie-sadi-service-ontology.owl#";
		public static final Resource InputClass = ResourceFactory.createResource(NS + "extractDrugNamesFromText_Input");
	}
	
	private static class Bibo
	{
		public static final String NS = "http://purl.org/ontology/bibo/";
		public static final Property content = ResourceFactory.createProperty(NS, "content");
	}
}