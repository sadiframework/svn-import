package ca.wilkinsonlab.sadi.service.blast;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

import ca.wilkinsonlab.sadi.ServiceDescription;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceCall;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.utils.blast.AbstractBLASTParser;
import ca.wilkinsonlab.sadi.utils.blast.NCBIBLASTClient;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Luke McCarthy
 */
@URI("http://sadiframework.org/services/blast/")
public class NCBIBLASTServiceServlet extends AsynchronousServiceServlet
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NCBIBLASTServiceServlet.class);
	private static final long serialVersionUID = 1L;
	
	private static final NCBIBLASTClient client = new NCBIBLASTClient();
	private static final BLASTParser parser = new BLASTParser();
	
	private Taxon taxon;
	
	public NCBIBLASTServiceServlet(Taxon taxon)
	{
		super();
		
		this.taxon = taxon;
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
			return model.getResource(uri);
		}
	}
	
	public static class Taxon
	{
		public String id;
		public String name;
		
		@Override
		public int hashCode() 
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Taxon other = (Taxon) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
}
