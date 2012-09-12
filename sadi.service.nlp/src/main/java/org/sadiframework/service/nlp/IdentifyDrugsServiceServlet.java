package org.sadiframework.service.nlp;

import gate.Annotation;
import gate.Document;
import gate.FeatureMap;

import java.io.IOException;
import java.net.URL;

import org.apache.http.util.EntityUtils;
import org.sadiframework.service.SynchronousServiceServlet;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.service.nlp.vocab.NLP2RDF;
import org.sadiframework.utils.http.HttpUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PropertyNotFoundException;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;

@URI("http://sadiframework.org/services/nlp/identifyDrugs")
@Name("identifyDrugs")
@Description("Extracts drug names from text (based on Gate Annie Gazetteer extracted from DrugBank)")
@ContactEmail("info@sadiframework.org")
@InputClass("http://sadiframework.org/services/nlp/nlp.owl#Document")
@OutputClass("http://sadiframework.org/services/nlp/nlp.owl#DrugAnnotatedDocument")
@TestCases({
	@TestCase(
		input = "http://sadiframework.org/services/nlp/t/identifyDrugs.input.1.rdf",
		output = "http://sadiframework.org/services/nlp/t/identifyDrugs.output.1.rdf"
	)
})
public class IdentifyDrugsServiceServlet extends SynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	
//	@Override
//	public void init() throws ServletException
//	{
//		super.init();
//	}
	
	@Override
	protected Model prepareOutputModel(Model inputModel)
	{
		Model model = super.prepareOutputModel(inputModel);
//		model.setNsPrefix("nlp2rdf", "http://nlp2rdf.lod2.eu/schema/");
		model.setNsPrefix("str", NLP2RDF.str);
		model.setNsPrefix("scms", NLP2RDF.scms); // for scms:means
		model.setNsPrefix("drugbank", "http://www.drugbank.ca/drugs/"); // for output URIs
		model.setNsPrefix("rdfs", RDFS.getURI()); // for rdfs:label
		model.setNsPrefix("dc", DC.getURI()); // for dc:identifer
		model.setNsPrefix("nlp", "http://sadiframework.org/services/nlp/nlp.owl#"); // for output class
		return model;
	}
	
	@Override
	public void processInput(Resource input, Resource output) throws Exception
	{
		String text = extractText(input);
		Document doc = GateHelper.getGateHelper().annotateText(text);
		annotateDocument(doc, output);
	}

	protected String extractText(Resource input) throws Exception
	{
		/* input is a str:Document
		 * use str:sourceString preferentially; if none, fetch the content of
		 * str:sourceURL
		 */
		try {
			return input.getRequiredProperty(NLP2RDF.sourceString).getString();
		} catch (PropertyNotFoundException noSourceString) {
			try {
				String url = input.getRequiredProperty(NLP2RDF.sourceUrl).getString();
				try {
					return EntityUtils.toString(HttpUtils.GET(new URL(url)).getEntity());
				} catch (IOException e) {
					throw new Exception(String.format("error reading text from %s", url), e);
				}
			} catch (PropertyNotFoundException noSourceUrl) {
				throw new Exception("one of str:sourceString or str:sourceUrl must be specified");
			}
		}
	}

	protected void annotateDocument(Document doc, Resource output) throws Exception
	{
		String baseAnnotationURI = output.getURI().concat(
				output.getURI().contains("#") ? "-" : "#");
		output.getModel().setNsPrefix("doc", baseAnnotationURI);
		for (Annotation annot: doc.getAnnotations().get("Lookup")) {
			long start = annot.getStartNode().getOffset();
			long end = annot.getEndNode().getOffset();
			String match = doc.getContent().getContent(start, end).toString();
//			String annotURI = String.format("%soffset_%d_%d_%s", baseAnnotationURI, start, end,
//					URLEncoder.encode(match, "UTF-8").replace("+", "%20"));
			String annotURI = String.format("%soffset_%d_%d", baseAnnotationURI, start, end);
			Resource annotNode = output.getModel().createResource(annotURI);
//			annotNode.addProperty(RDF.type, NLP2RDF.OffsetBasedString);
			annotNode.addLiteral(NLP2RDF.anchorOf, ResourceFactory.createPlainLiteral(match));
			output.addProperty(NLP2RDF.subString, annotNode);
			
			FeatureMap features = annot.getFeatures();
			String dbId = (String)features.get("dbid");
			String originalName = (String)features.get("originalName");
//			Resource dbNode = LSRNUtils.createInstance(output.getModel(), LSRNUtils.getClass("DrugBank"), dbId);
			Resource dbNode = output.getModel().createResource(String.format("http://www.drugbank.ca/drugs/%s", dbId));
			dbNode.addLiteral(RDFS.label, ResourceFactory.createPlainLiteral(originalName));
			dbNode.addProperty(DC.identifier, ResourceFactory.createPlainLiteral(dbId));
			annotNode.addProperty(NLP2RDF.means, dbNode);
		}
	}
}
