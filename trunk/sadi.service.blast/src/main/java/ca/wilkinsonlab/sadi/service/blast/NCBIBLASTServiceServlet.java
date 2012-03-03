package ca.wilkinsonlab.sadi.service.blast;

import java.io.InputStream;

import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.ServiceDescription;
import ca.wilkinsonlab.sadi.beans.ServiceBean;
import ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.ServiceCall;
import ca.wilkinsonlab.sadi.utils.blast.BLASTUtils;
import ca.wilkinsonlab.sadi.utils.blast.NCBIBLASTClient;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Luke McCarthy
 */
public class NCBIBLASTServiceServlet extends AsynchronousServiceServlet
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NCBIBLASTServiceServlet.class);
	private static final long serialVersionUID = 1L;
	
	private Taxon taxon;
	private NCBIBLASTClient client;
	
	public NCBIBLASTServiceServlet(Taxon taxon, NCBIBLASTClient client)
	{
		super();
		
		this.taxon = taxon;
		this.client = client;
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.service.ServiceServlet#createServiceDescription()
	 */
	@Override
	protected ServiceDescription createServiceDescription()
	{
		ServiceBean service = new ServiceBean();
		if (System.getProperty(IGNORE_FORCED_URL_SYSTEM_PROPERTY) != null) {
//			log.info("ignoring specified service URL");
			service.setURI("");
		} else {
			service.setURI(String.format("http://sadiframework.org/examples/blast/%s", taxon.name));
		}
		service.setName(String.format("NCBI BLAST for %s genome", taxon.name.replace("+", " ")));
		service.setDescription(String.format("Issues a BLAST query against the %s genome with the default NCBI options.", taxon.name.replace("+", " ")));
		service.setContactEmail("info@sadiframework.org");
		service.setAuthoritative(false);
		service.setInputClassURI("http://semanticscience.org/resource/SIO_010018");
//		if (System.getProperty(IGNORE_FORCED_URL_SYSTEM_PROPERTY) != null) {
////			log.info("ignoring specified service URL");
//			service.setOutputClassURI(String.format("/sadi-blast/%s.owl#BLASTedSequence", taxon.name));
//		} else {
			service.setOutputClassURI(String.format("http://sadiframework.org/examples/blast/%s.owl#BLASTedSequence", taxon.name));
//		}
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
	 * @see ca.wilkinsonlab.sadi.service.AsynchronousServiceServlet#processInputBatch(ca.wilkinsonlab.sadi.service.ServiceCall)
	 */
	@Override
	protected void processInputBatch(ServiceCall call) throws Exception
	{
		// TODO deal with parameters...
//		Resource parameters = call.getParameters();
		
		StringBuilder query = new StringBuilder();
		for (Resource inputNode: call.getInputNodes()) {
			String id = inputNode.getURI();
			String sequence = inputNode.getRequiredProperty(SIO.has_value).getLiteral().getLexicalForm();
			query.append(">");
			query.append(id);
			query.append("\n");
			query.append(sequence);
			query.append("\n");
		}
		
		InputStream result = client.doBLAST("blastn", getDB(), query.toString());
		BLASTUtils.parseBLAST(result, call.getOutputModel());
	}
	
//	public static void main(String[] args) {
//	    try {
//	      //get the Blast input as a Stream
//	      InputStream is = new FileInputStream("ncbiblast-S20101115-151925-0569-78415478.xml.xml");
////	      System.out.println(new BufferedReader(new InputStreamReader(is)).readLine());
////	      System.exit(0);
//	      //make a BlastLikeSAXParser
//	      BlastLikeSAXParser parser = new BlastLikeSAXParser();
//	 
//	 
//	      // try to parse, even if the blast version is not recognized.
//	      parser.setModeLazy();
//	 
//	 
//	      //make the SAX event adapter that will pass events to a Handler.
//	      SeqSimilarityAdapter adapter = new SeqSimilarityAdapter();
//	 
//	      //set the parsers SAX event adapter
//	      parser.setContentHandler(adapter);
//	 
//	      //The list to hold the SeqSimilaritySearchResults
//	      List results = new ArrayList();
//	 
//	      //create the SearchContentHandler that will build SeqSimilaritySearchResults
//	      //in the results List
//	      SearchContentHandler builder = new BlastLikeSearchBuilder(results,
//	          new DummySequenceDB("queries"), new DummySequenceDBInstallation());
//	 
//	      //register builder with adapter
//	      adapter.setSearchContentHandler(builder);
//	 
//	      //parse the file, after this the result List will be populated with
//	      //SeqSimilaritySearchResults
//	      parser.parse(new InputSource(is));
//	 
//	      //output some blast details
//	      for (Iterator i = results.iterator(); i.hasNext(); ) {
//	        SeqSimilaritySearchResult result =
//	            (SeqSimilaritySearchResult)i.next();
//	 
//	        Annotation anno = result.getAnnotation();
//	 
//	        for (Iterator j = anno.keys().iterator(); j.hasNext(); ) {
//	          Object key = j.next();
//	          Object property = anno.getProperty(key);
//	          System.out.println(key+" : "+property);
//	        }
//	        System.out.println("Hits: ");
//	 
//	        //list the hits
//	        for (Iterator k = result.getHits().iterator(); k.hasNext(); ) {
//	          SeqSimilaritySearchHit hit =
//	              (SeqSimilaritySearchHit)k.next();
//	          System.out.print("\tmatch: "+hit.getSubjectID());
//	          System.out.println("\te score: "+hit.getEValue());
//	        }
//	 
//	        System.out.println("\n");
//	      }
//	 
//	    }
//	    catch (SAXException ex) {
//	      //XML problem
//	      ex.printStackTrace();
//	    }catch (IOException ex) {
//	      //IO problem, possibly file not found
//	      ex.printStackTrace();
//	    }
//	  }
	
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
