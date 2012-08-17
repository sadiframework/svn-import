package org.sadiframework.service.opencitations;

import org.apache.log4j.Logger;

import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.utils.LSRNUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

@Name("Get PubMed document info")
@Description("Fetch various information about a PubMed document")
@ContactEmail("elmccarthy@gmail.com")
@InputClass("http://purl.oclc.org/SADI/LSRN/PMID_Record")
@OutputClass("http://sadiframework.org/ontologies/opencitations.owl#GetDocumentInfoOutputClass")
@TestCases({
	@TestCase(
			input = "/t/getAbstract-input.rdf", 
			output = "/t/getAbstract-output.rdf"
	)
})
public class GetDocumentInfo extends OpenCitationsServiceServlet
{
	private static final Logger log = Logger.getLogger(GetDocumentInfo.class);
	private static final long serialVersionUID = 1L;

	private static final String urlTemplate = "http://opencitations.net/id/expression:pmid/%s";
	
	@Override
	public void processInput(Resource input, Resource output)
	{
		String pmid = LSRNUtils.getID(input);
		if (pmid != null) {
			String url = String.format(urlTemplate, pmid);
			try {
				output.getModel().read(url);
				output.addProperty(OWL.sameAs, url);
			} catch (Exception e) {
				log.error(String.format("error reading RDF from %s", url), e);
			}
		}
	}

	@SuppressWarnings("unused")
	private static final class Vocab
	{
		private static Model m_model = ModelFactory.createDefaultModel();
		
		public static final Property SIO_000145 = m_model.createProperty("http://semanticscience.org/resource/SIO_000145");
		public static final Property exemplar = m_model.createProperty("http://purl.org/vocab/frbr/core#exemplar");
		public static final Property hasPubMedId = m_model.createProperty("http://purl.org/spar/fabio/hasPubMedId");
		public static final Property inScheme = m_model.createProperty("http://www.w3.org/2004/02/skos/core#inScheme");
		public static final Property SIO_000053 = m_model.createProperty("http://semanticscience.org/resource/SIO_000053");
		public static final Property realizationOf = m_model.createProperty("http://purl.org/vocab/frbr/core#realizationOf");
		public static final Property SIO_000563 = m_model.createProperty("http://semanticscience.org/resource/SIO_000563");
		public static final Property hasIntervalEndDate = m_model.createProperty("http://www.ontologydesignpatterns.org/cp/owl/timeinterval.owl#hasIntervalEndDate");
		public static final Property isSchemeOf = m_model.createProperty("http://purl.org/spar/fabio/isSchemeOf");
		public static final Property atTime = m_model.createProperty("http://www.ontologydesignpatterns.org/cp/owl/timeindexedsituation.owl#atTime");
		public static final Property SIO_000028 = m_model.createProperty("http://semanticscience.org/resource/SIO_000028");
		public static final Property SIO_000341 = m_model.createProperty("http://semanticscience.org/resource/SIO_000341");
		public static final Property SIO_000217 = m_model.createProperty("http://semanticscience.org/resource/SIO_000217");
		public static final Property hasIntervalStartDate = m_model.createProperty("http://www.ontologydesignpatterns.org/cp/owl/timeinterval.owl#hasIntervalStartDate");
		public static final Property withRole = m_model.createProperty("http://purl.org/spar/pro/withRole");
		public static final Property SIO_000672 = m_model.createProperty("http://semanticscience.org/resource/SIO_000672");
		public static final Property name = m_model.createProperty("http://xmlns.com/foaf/0.1/name");
		public static final Property publicationDate = m_model.createProperty("http://prismstandard.org/namespaces/basic/2.0/publicationDate");
		public static final Property title = m_model.createProperty("http://purl.org/dc/terms/title");
		public static final Property published = m_model.createProperty("http://purl.org/dc/terms/published");
		public static final Property SIO_000300 = m_model.createProperty("http://semanticscience.org/resource/SIO_000300");
		public static final Property realization = m_model.createProperty("http://purl.org/vocab/frbr/core#realization");
		public static final Property SIO_000008 = m_model.createProperty("http://semanticscience.org/resource/SIO_000008");
		public static final Property partOf = m_model.createProperty("http://purl.org/vocab/frbr/core#partOf");
		public static final Property SIO_000132 = m_model.createProperty("http://semanticscience.org/resource/SIO_000132");
		public static final Property hasIntervalDate = m_model.createProperty("http://www.ontologydesignpatterns.org/cp/owl/timeinterval.owl#hasIntervalDate");
		public static final Property embodimentOf = m_model.createProperty("http://purl.org/vocab/frbr/core#embodimentOf");
		public static final Property embodiment = m_model.createProperty("http://purl.org/vocab/frbr/core#embodiment");
		public static final Property hasPubMedCentralId = m_model.createProperty("http://purl.org/spar/fabio/hasPubMedCentralId");
		public static final Property SIO_000011 = m_model.createProperty("http://semanticscience.org/resource/SIO_000011");
		public static final Property SIO_000218 = m_model.createProperty("http://semanticscience.org/resource/SIO_000218");
		public static final Property holdsRoleInTime = m_model.createProperty("http://purl.org/spar/pro/holdsRoleInTime");
		public static final Property creator = m_model.createProperty("http://purl.org/dc/terms/creator");
		public static final Property forEntity = m_model.createProperty("http://www.ontologydesignpatterns.org/cp/owl/timeindexedsituation.owl#forEntity");
		public static final Property isSettingFor = m_model.createProperty("http://www.ontologydesignpatterns.org/cp/owl/situation.owl#isSettingFor");
		public static final Property part = m_model.createProperty("http://purl.org/vocab/frbr/core#part");
		public static final Property isRelatedToRoleInTime = m_model.createProperty("http://purl.org/spar/pro/isRelatedToRoleInTime");
		public static final Property exemplarOf = m_model.createProperty("http://purl.org/vocab/frbr/core#exemplarOf");
		public static final Property isRoleHeldBy = m_model.createProperty("http://purl.org/spar/pro/isRoleHeldBy");
		public static final Property SIO_000061 = m_model.createProperty("http://semanticscience.org/resource/SIO_000061");
		public static final Property to = m_model.createProperty("http://purl.org/net/pingback/to");
		public static final Property relatesToDocument = m_model.createProperty("http://purl.org/spar/pro/relatesToDocument");
		public static final Property service = m_model.createProperty("http://purl.org/net/pingback/service");
		public static final Resource SIO_000370 = m_model.createResource("http://semanticscience.org/resource/SIO_000370");
		public static final Resource SubjectTerm = m_model.createResource("http://purl.org/spar/fabio/SubjectTerm");
		public static final Resource Manifestation = m_model.createResource("http://purl.org/vocab/frbr/core#Manifestation");
		public static final Resource Work = m_model.createResource("http://purl.org/vocab/frbr/core#Work");
		public static final Resource LSRN_Identifier = m_model.createResource("http://purl.oclc.org/SADI/LSRN/LSRN_Identifier");
		public static final Resource SIO_000026 = m_model.createResource("http://semanticscience.org/resource/SIO_000026");
		public static final Resource SIO_000416 = m_model.createResource("http://semanticscience.org/resource/SIO_000416");
		public static final Resource TermDictionary = m_model.createResource("http://purl.org/spar/fabio/TermDictionary");
		public static final Resource Role = m_model.createResource("http://purl.org/spar/pro/Role");
		public static final Resource PMID_Identifier = m_model.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Identifier");
		public static final Resource SIO_000027 = m_model.createResource("http://semanticscience.org/resource/SIO_000027");
		public static final Resource date = m_model.createResource("http://www.w3.org/2001/XMLSchema#date");
		public static final Resource SIO_000000 = m_model.createResource("http://semanticscience.org/resource/SIO_000000");
		public static final Resource Document = m_model.createResource("http://xmlns.com/foaf/0.1/Document");
		public static final Resource Expression = m_model.createResource("http://purl.org/vocab/frbr/core#Expression");
		public static final Resource Literal = m_model.createResource("http://www.w3.org/2000/01/rdf-schema#Literal");
		public static final Resource GetDocumentInfoOutputClass = m_model.createResource("http://sadiframework.org/ontologies/opencitations.owl#GetDocumentInfoOutputClass");
		public static final Resource Item = m_model.createResource("http://purl.org/vocab/frbr/core#Item");
		public static final Resource PMID_Record = m_model.createResource("http://purl.oclc.org/SADI/LSRN/PMID_Record");
		public static final Resource dateTime = m_model.createResource("http://www.w3.org/2001/XMLSchema#dateTime");
		public static final Resource SIO_000003 = m_model.createResource("http://semanticscience.org/resource/SIO_000003");
		public static final Resource TimeInterval = m_model.createResource("http://www.ontologydesignpatterns.org/cp/owl/timeinterval.owl#TimeInterval");
		public static final Resource SIO_000002 = m_model.createResource("http://semanticscience.org/resource/SIO_000002");
	}
}
