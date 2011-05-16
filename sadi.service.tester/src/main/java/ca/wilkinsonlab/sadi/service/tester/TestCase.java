package ca.wilkinsonlab.sadi.service.tester;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashSet;

import ca.wilkinsonlab.sadi.utils.ContentType;
import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;

public class TestCase
{
	Model inputModel;
	Model expectedModel;
	
	public TestCase(RDFNode input, RDFNode output)
	{
		inputModel = createModel(input);
		expectedModel = createModel(output);
	}
	
	public TestCase(String input, String output)
	{
		inputModel = createModel(input);
		expectedModel = createModel(output);
	}

	private static Model createModel(RDFNode source)
	{
		return createModel(source, null);
	}
	private static Model createModel(RDFNode source, String base)
	{
		if (source.isResource()) {
			Resource sourceResource = source.asResource();
			
			/* if there are statements about this resource, it's inline;
			 * if not, it's the URI of a remote resource...
			 */
			if (sourceResource.listProperties().hasNext()) {
				return ResourceUtils.reachableClosure(source.asResource());
			} else {
				Model model = ModelFactory.createDefaultModel();
				readIntoModel(model, sourceResource.getURI());
				return model;
			}
		} else {
			Model model = ModelFactory.createDefaultModel();
			if (base != null) {
				loadModelFromRDF(model, source.asLiteral().getString(), base);
			} else {
				loadModelFromRDF(model, source.asLiteral().getString());
			}
			return model;
		}
	}
	
	// TODO move to RDFUtils
	private static Model createModel(String pathOrURLOrRDF)
	{
		Model model = ModelFactory.createDefaultModel();
		try {
			RdfUtils.loadModelFromPathOrURL(model, pathOrURLOrRDF);
		} catch (Exception e) {
			loadModelFromRDF(model, pathOrURLOrRDF);
		}
		return model;
	}
	
	// TODO move to RDFUtils
	// possibly use this everywhere we're currently using model.read()...
	public static void readIntoModel(Model model, String url)
	{
		for (String lang: getJenaLanguages()) {
			try {
				model.begin();
				model.read(url, lang);
				model.commit();
				break;
			} catch (Exception e) {
				model.abort();
			}
		}
	}
	
	// TODO move to RDFUtils...
	public static void loadModelFromRDF(Model model, String rdf)
	{
		loadModelFromRDF(model, rdf, "");
	}
	public static void loadModelFromRDF(Model model, String rdf, String base)
	{
		for (String lang: getJenaLanguages()) {
			try {
				model.begin();
				model.read(new ByteArrayInputStream(rdf.getBytes()), base, lang);
				model.commit();
				break;
			} catch (Exception e) {
				model.abort();
			}
		}
		throw new RuntimeException("Couldn't read RDF using any known language");
	}
	
	// TODO move to ContentType...
	private static Collection<String> getJenaLanguages()
	{
		Collection<String> langs = new HashSet<String>();
		for (ContentType type: ContentType.values()) {
			langs.add(type.getJenaLanguage());
		}
		return langs;
	}
}
