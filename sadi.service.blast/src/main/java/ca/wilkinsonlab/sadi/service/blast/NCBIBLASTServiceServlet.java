package ca.wilkinsonlab.sadi.service.blast;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ca.wilkinsonlab.sadi.ServiceDescription;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceCall;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.service.blast.MasterServlet.Taxon;
import ca.wilkinsonlab.sadi.utils.LSRNUtils;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.SPARQLStringUtils;
import ca.wilkinsonlab.sadi.utils.blast.AbstractBLASTParser;
import ca.wilkinsonlab.sadi.utils.blast.NCBIBLASTClient;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Luke McCarthy
 */
@URI("http://sadiframework.org/services/blast/")
public class NCBIBLASTServiceServlet extends AsynchronousServiceServlet
{
	private static final Logger log = Logger.getLogger(NCBIBLASTServiceServlet.class);
	private static final long serialVersionUID = 1L;
	
	private static final NCBIBLASTClient client = new NCBIBLASTClient();
	
	Taxon taxon;
	BLASTParser parser;
	
	public NCBIBLASTServiceServlet(Taxon taxon)
	{
		super();
		
		this.taxon = taxon;
		this.parser = new BLASTParser(taxon);
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (request.getServletPath().endsWith(".owl")) {
			try {
				String owl = SPARQLStringUtils.strFromTemplate(MasterServlet.class.getResource("/template.owl"), taxon.name, taxon.id);
				response.setContentType("application/rdf+xml");
				response.getWriter().print(owl);
			} catch (IOException e) {
				log.error(String.format("error send owl for %s", taxon.name), e);
			}
		} else {
			super.doGet(request, response);
		}
	}

	/**
	 * Returns the value of the DATABASE parameter to the NCBI BLAST service.
	 * @return the value of the DATABASE parameter to the NCBI BLAST service
	 */
	protected String getDB()
	{
		return String.format("GPIPE/%s/current/ref_contig", taxon.id);
	}
	
	/**
	 * Returns the NCBI taxon ID of records in the database.
	 * @return the NCBI taxon ID of records in the database
	 */
	protected String getTaxonID()
	{
		return taxon.id;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#getServiceURL()
	 */
	@Override
	protected String getServiceURL()
	{
		String url = super.getServiceURL();
		return url == null ? null : url.concat(taxon.name);
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#createServiceDescription()
	 */
	@Override
	protected ServiceDescription createServiceDescription()
	{
		ServiceBean service = new ServiceBean();
		service.setURI(getServiceURL());
		service.setName(String.format("NCBI BLAST for %s genome", taxon.name.replace("+", " ")));
		service.setDescription(String.format("Issues a BLAST query against the %s genome with the default NCBI options.", taxon.name.replace("+", " ")));
		service.setContactEmail("info@sadiframework.org");
		service.setAuthoritative(false);
		service.setInputClassURI("http://semanticscience.org/resource/SIO_010018");
		service.setOutputClassURI(String.format("http://sadiframework.org/examples/blast/%s.owl#BLASTedSequence", taxon.name));
		return service;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#createOutputModel()
	 */
	@Override
	protected Model createOutputModel()
	{
		Model model = super.createOutputModel();
		model.setNsPrefix("ncbi-blast", "http://sadiframework.org/examples/blast-uniprot.owl#");
		model.setNsPrefix("blast", "http://sadiframework.org/ontologies/blast.owl#");
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		return model;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet#processInputBatch(ca.wilkinsonlab.sadi.service.ServiceCall)
	 */
	@Override
	protected void processInputBatch(ServiceCall call) throws Exception
	{
		// TODO deal with parameters...
		//Resource parameters = call.getParameters();
		
		InputStream result = client.doBLAST("blastn", getDB(), buildQuery(call.getInputNodes()));
		parser.parseBLAST(call.getOutputModel(), result);
	}

	protected String buildQuery(Collection<Resource> inputNodes)
	{
		StringBuilder query = new StringBuilder();
		for (Resource inputNode: inputNodes) {
			String id;
			try {
				/* URI has to be URL-encoded for some reason or the API request
				 * never gets through...
				 * (this is reversed in BLASTParser.getQuerySequence below)
				 */
				id = URLEncoder.encode(inputNode.getURI(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// UTF-8 is unsupported? really?
				id = RdfUtils.createUniqueURI();
			}
			String sequence = inputNode.getRequiredProperty(SIO.has_value).getLiteral().getLexicalForm();
			query.append(">");
			query.append(id);
			query.append("\n");
			query.append(sequence);
			query.append("\n");
		}
		return query.toString();
	}
	
	public static class BLASTParser extends AbstractBLASTParser
	{
		protected Taxon taxon;
		
		public BLASTParser(Taxon taxon)
		{
			this.taxon = taxon;
		}
		
		@Override
		protected Resource getQuerySequence(Model model, Node iteration)
		{
			String query_def = getSingleValue(iteration, "Iteration_query-def");
			if (query_def == null) {
				query_def = getSingleValue(iteration.getParentNode().getParentNode(), "BlastOutput_query-def");
			}
			/* reverse the URI encoding performed above...
			 */
			String uri;
			try {
				uri = URLDecoder.decode(query_def, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// UTF-8 is unsupported? really?
				uri = query_def;
			}
			return model.getResource(uri);
		}

		@Override
		protected Resource getHitSequence(Model model, Node hit)
		{
			String acc = getSingleValue(hit, "Hit_accession");
			// TODO find/create an appropriate LSRN type...
			String uri = String.format("http://www.ncbi.nlm.nih.gov/nuccore/%s", acc);
			Resource seq = model.getResource(uri);
			if (!seq.hasProperty(RDF.type)) { // first time; create it...
				seq.addProperty(RDF.type, SIO.nucleic_acid_sequence);
				seq.addProperty(fromOrganism, getOrganism(model));
			}
			return seq;
		}
		
		private Resource getOrganism(Model model)
		{
			Resource org = model.getResource(LSRNUtils.getURI("taxon", taxon.id));
			if (!org.hasProperty(RDF.type)) { // first time; create it...
				org = LSRNUtils.createInstance(model, 
						model.getResource(LSRNUtils.getClassURI("taxon")), taxon.id);
			}
			return org;
		}
		
		private static final Property fromOrganism = ResourceFactory.createProperty("http://sadiframework.org/ontologies/properties.owl#fromOrganism");
	}
}
