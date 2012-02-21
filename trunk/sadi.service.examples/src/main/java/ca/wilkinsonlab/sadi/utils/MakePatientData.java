package ca.wilkinsonlab.sadi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ca.elmonline.util.TextFile.TextStreamIterator;
import ca.wilkinsonlab.sadi.vocab.Patients;
import ca.wilkinsonlab.sadi.vocab.Regression;
import ca.wilkinsonlab.sadi.vocab.SIO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.ResourceUtils;

public class MakePatientData
{
	private static final Logger log = Logger.getLogger(MakePatientData.class);
	
	private static final String NS = "http://sadiframework.org/examples/patients.rdf#";

	public static void main(String[] args) throws Exception
	{
		LocationMapper.get().addAltPrefix("http://sadiframework.org/examples/", "file:src/main/webapp/");
		LocationMapper.get().addAltPrefix("http://sadiframework.org/ontologies/", "file:../sadiframework.org/ontologies/");
		
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("patients", "http://sadiframework.org/ontologies/patients.owl#");
		model.setNsPrefix("sio", "http://semanticscience.org/resource/");
		
		File patientsDAT = getFile("/fakepatient/patients.dat");
		File patientsRDF = getFile("/fakepatient/patients.rdf");
		if (!patientsRDF.exists() || patientsRDF.lastModified() < patientsDAT.lastModified()) {
			log.info(String.format("building %s from %s", patientsRDF, patientsDAT));
			buildRDF(model, new FileInputStream(patientsDAT));
			writeRDF(model, patientsRDF);
			patientsRDF.setLastModified(System.currentTimeMillis());
		} else {
			log.info(String.format("%s is newer than %s", patientsRDF, patientsDAT));
			model.read(new FileInputStream(patientsRDF), "");
		}
		
		Model inputModel = ModelFactory.createDefaultModel();
		inputModel.setNsPrefix("patients", "http://sadiframework.org/ontologies/patients.owl#");
		inputModel.setNsPrefix("sio", "http://semanticscience.org/resource/");
		inputModel.setNsPrefix("regress", "http://sadiframework.org/examples/regression.owl#");
		
		File regressionInput = getFile("src/main/webapp/t/linear.input.1.rdf");
		if (!regressionInput.exists() || regressionInput.lastModified() < patientsRDF.lastModified()) {
			log.info(String.format("building %s from %s", regressionInput, patientsRDF));
			buildRDF(inputModel, model);
			writeRDF(inputModel, regressionInput);
			regressionInput.setLastModified(System.currentTimeMillis());
		} else {
			log.info(String.format("%s is newer than %s", regressionInput, patientsRDF));
			inputModel.read(new FileInputStream(regressionInput), "");
		}
	}

	private static File getFile(String path) throws URISyntaxException
	{
		URL resource = MakePatientData.class.getResource(path);
		if (resource != null)
			return new File(resource.toURI());
		if (path.startsWith("/")) {
			return new File(String.format("src/main/resources%s", path));
		} else {
			return new File(path);
		}
	}
	
	private static void buildRDF(Model model, InputStream in)
	{
		TextStreamIterator i = null;
		try {
			i = new TextStreamIterator(in);
			while (i.hasNext()) {
				String[] fields = StringUtils.split(i.next());
				Resource patient = getPatient(model, fields[0]);	// f[0] = id
				addCreatinineMeasurement(patient, fields[1], fields[3]);	// f[1] = offset, f[3] = creatinine
				addBUNMeasurement(patient, fields[1], fields[2]);	// f[1] = offset, f[2] = BUN
			}
		} catch (Exception e) {
			log.error("error reading fake data stream", e);
		} finally {
			if (i != null) {
				try {
					i.close();
				} catch (IOException e) {
					log.error("error closing fake data stream", e);
				}
			}
		}
	}

	private static Resource getPatient(Model model, String id)
	{
		String uri = String.format("%s%s", NS, id);
		return model.createResource(uri, Patients.Patient);
	}

	private static void addCreatinineMeasurement(Resource patient, String offset, String creatinine)
	{
		Resource collection = getCollection(patient, Patients.creatinineLevels);
		addMeasurement(collection, Integer.valueOf(offset), Double.valueOf(creatinine));
	}
	
	private static void addBUNMeasurement(Resource patient, String offset, String bun)
	{
		Resource collection = getCollection(patient, Patients.bunLevels);
		addMeasurement(collection, Integer.valueOf(offset), Double.valueOf(bun));
	}

	private static Resource getCollection(Resource patient, Property p)
	{
		Resource collection = RdfUtils.getPropertyValue(patient, p, null);
		if (collection == null) {
			collection = patient.getModel().createResource(RdfUtils.createUniqueURI());
			patient.addProperty(p, collection);
		}
		return collection;
	}

	private static void addMeasurement(Resource collection, int offset, double value)
	{
		Resource measurementEvent = collection.getModel().createResource(Patients.MeasurementEvent);
		SIOUtils.createAttribute(measurementEvent, Patients.Offset, offset);
		SIOUtils.createAttribute(measurementEvent, Patients.Measurement, value);
		collection.addProperty(SIO.has_member, measurementEvent);
	}

	private static void buildRDF(Model inputModel, Model patientModel)
	{
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		ontModel.read("http://sadiframework.org/examples/regression.owl");
		ontModel.read("http://sadiframework.org/ontologies/patients.owl");
		ontModel.addSubModel(patientModel);	
		ontModel.rebind();
		verify(ontModel);
		OntClass inputClass = ontModel.getOntClass("http://sadiframework.org/examples/regression.owl#InputClass");
		for (Resource input: ontModel.listIndividuals(inputClass).toList()) { // avoid ConcurrentModificationException
			if (!input.isURIResource()) {
				input = ResourceUtils.renameResource(input, RdfUtils.createUniqueURI());
			}
			new MinimalModelDecomposer(inputModel, input, inputClass).decompose();
		}
	}
	
	private static void verify(OntModel ontModel)
	{
		Iterator<Individual> i = ontModel.listIndividuals(Patients.Patient);
		assertTrue(String.format("no instances of %s", Patients.Patient), i.hasNext());
		while (i.hasNext()) {
			Individual patient = i.next();
			log.debug(String.format("checking %s", patient));
			Individual creats = patient.getPropertyResourceValue(Patients.creatinineLevels).as(Individual.class);
			Individual creat = creats.getPropertyResourceValue(SIO.has_member).as(Individual.class);
			Resource x = RdfUtils.getPropertyValue(creat, SIO.has_attribute, Regression.X);
			assertNotNull(String.format("%s's first creat value has no X", patient), x);
			Resource y = RdfUtils.getPropertyValue(creat, SIO.has_attribute, Regression.Y);
			assertNotNull(String.format("%s's first creat value has no Y", patient), y);
			assertTrue(String.format("%s's first creat value is not a PairedValue", patient), creat.hasOntClass(Regression.PairedValue));
			assertTrue(String.format("%s's creat collection has fewer than two values", patient), creats.listPropertyValues(SIO.has_member).toList().size()>=2);
			OntClass inputClass = ontModel.getOntClass("http://sadiframework.org/examples/regression.owl#InputClass");
			assertTrue(String.format("%s's creat collection is not an instance of %s", patient, inputClass),
					creats.hasOntClass(inputClass));
		}
	}
	private static void assertTrue(String msg, boolean b)
	{
		if (!b)
			log.error(msg);
	}
	private static void assertNotNull(String msg, Object o)
	{
		assertTrue(msg, o != null);
	}

	private static void writeRDF(Model model, File out)
	{
		try {
			ContentType.RDF_XML.writeModel(model, new FileOutputStream(out), "");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
