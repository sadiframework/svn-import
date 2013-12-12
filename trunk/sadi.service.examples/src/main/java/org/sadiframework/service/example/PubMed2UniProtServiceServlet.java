package org.sadiframework.service.example;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.AsynchronousServiceServlet;
import org.sadiframework.service.ServiceCall;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.utils.LSRNUtils;
import org.sadiframework.utils.SPARQLStringUtils;
import org.sadiframework.vocab.LSRN;
import org.sadiframework.vocab.SIO;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@URI("http://sadiframework.org/examples/pubmed2uniprot")
@Name("PubMed2UniProt")
@Description("Maps PubMed IDs to UniProt proteins")
@ContactEmail("info@sadiframework.org")
@InputClass("http://purl.oclc.org/SADI/LSRN/PMID_Record")
@OutputClass("http://sadiframework.org/examples/pubmed2uniprot.owl#OutputClass")
@TestCases(
		@TestCase(
				input = "http://sadiframework.org/examples/t/pubmed2uniprot.input.1.rdf",
				output = "http://sadiframework.org/examples/t/pubmed2uniprot.output.1.rdf"
		)
)
public class PubMed2UniProtServiceServlet extends AsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(PubMed2UniProtServiceServlet.class);
	private static final Property is_reference_for = ResourceFactory.createProperty(SIO.NS, "SIO_000252");

	private static final String SPARQL_ENDPOINT = "http://beta.sparql.uniprot.org/";
	private static final String queryTemplate =
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" + 
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> \n" + 
			"PREFIX owl:<http://www.w3.org/2002/07/owl#> \n" + 
			"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> \n" + 
			"PREFIX up:<http://purl.uniprot.org/core/> \n" + 
			"PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \n" + 
			"SELECT ?p ?cite ?alt\n" + 
			"WHERE {\n" + 
			"  ?p a up:Protein .\n" + 
			"  ?p up:citation ?cite .\n" + 
			"  ?cite skos:exactMatch ?alt .\n" + 
			"  FILTER regex(str(?alt), \"pubmed\", \"i\") .\n" + 
			"  %v%\n" + 
			"}"; 
//			"    ?cite skos:exactMatch <http://purl.uniprot.org/pubmed/9207092>\n" + 
//			"  } UNION {\n" + 
//			"    ?cite skos:exactMatch <http://purl.uniprot.org/pubmed/10490659>\n" + 
//			"  }\n" +
	private static final String whereTemplate = "?cite skos:exactMatch %u%";
	private static final Pattern uniprotPattern = Pattern.compile(".*/(\\S+)");
	
	@Override
	public void processInputBatch(ServiceCall call)
	{
		Map<String, Resource> input2output = new HashMap<String, Resource>();
		for (Resource inputNode: call.getInputNodes()) {
			Resource outputNode = call.getOutputModel().getResource(inputNode.getURI());
			String pubmedID = LSRNUtils.getID(inputNode, LSRNUtils.getIdentifierClass(LSRN.Namespace.PMID));
			if (pubmedID == null) {
				log.warn(String.format("unable to determine PubMed ID for %s", inputNode));
			} else {
				String uniprotPubMedURL = String.format("http://purl.uniprot.org/pubmed/%s", pubmedID);
				input2output.put(uniprotPubMedURL, outputNode);
			}
		}
		
		StringBuilder where = new StringBuilder();
		if (input2output.isEmpty()) {
			return;
		} else if (input2output.size() == 1) {
			String url = input2output.keySet().iterator().next();
			where.append(SPARQLStringUtils.strFromTemplate(whereTemplate, url));
		} else {
			where.append("{ ");
			for (Iterator<String> i = input2output.keySet().iterator(); i.hasNext(); ) {
				String url = i.next();
				where.append(SPARQLStringUtils.strFromTemplate(whereTemplate, url));
				where.append(" }");
				if (i.hasNext()) {
					where.append(" UNION { ");
				}
			}
		}
		String query = SPARQLStringUtils.strFromTemplate(queryTemplate, where.toString());

		QueryExecution qe = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query);
		ResultSet resultSet = qe.execSelect();
		while (resultSet.hasNext()) {
			QuerySolution binding = resultSet.nextSolution();
			RDFNode p = binding.get("p");
			RDFNode cite = binding.get("alt");
			String uniprotPubMedURL = cite.asResource().getURI();
			Resource output = input2output.get(uniprotPubMedURL);
			if (output == null) {
				log.warn(String.format("no output node for %s", uniprotPubMedURL));
			} else {
				if (p.isURIResource()) {
					String uniprotURI = p.asResource().getURI();
					Matcher matcher = uniprotPattern.matcher(uniprotURI);
					if (matcher.matches()) {
						String uniprotID = matcher.group(1);
				    	Resource uniprotNode = LSRNUtils.createInstance(output.getModel(), LSRNUtils.getClass(LSRN.Namespace.UNIPROT), uniprotID);
				    	output.addProperty(is_reference_for, uniprotNode);
					}
				}
			}
		}
	}
}
