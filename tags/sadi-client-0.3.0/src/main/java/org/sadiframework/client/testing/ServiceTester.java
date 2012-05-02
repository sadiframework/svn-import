package org.sadiframework.client.testing;

import java.util.Collection;

import org.sadiframework.SADIException;
import org.sadiframework.client.Service;
import org.sadiframework.client.ServiceImpl;
import org.sadiframework.utils.LabelUtils;
import org.sadiframework.utils.ModelDiff;
import org.sadiframework.utils.RdfUtils;


import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;

public class ServiceTester
{
	public static void testService(Service service) throws SADIException
	{
		testService(service, true);
	}
	
	public static void testService(Service service, boolean withSanityCheck) throws SADIException
	{
		for (TestCase testCase: ((ServiceImpl)service).getTestCases())
			testService(service, testCase, withSanityCheck);
	}
	
	public static void testService(Service service, TestCase testCase) throws SADIException
	{
		testService(service, testCase, true);
	}
	
	public static void testService(Service service, TestCase testCase, boolean withSanityCheck) throws SADIException
	{
		Model outputModel = ((ServiceImpl)service).invokeServiceUnparsed(testCase.getInputModel());
		if (!outputModel.isIsomorphicWith(testCase.getExpectedOutputModel())) {
			StringBuffer buf = new StringBuffer();
			ModelDiff diff = ModelDiff.diff(outputModel, testCase.getExpectedOutputModel());
			if (!diff.inXnotY.isEmpty()) {
				buf.append("service output had unexpected statements:\n");
				buf.append(RdfUtils.logStatements("\t", diff.inXnotY));
			}
			if (!diff.inYnotX.isEmpty()) {
				buf.append("service output had missing statements:\n");
				buf.append(RdfUtils.logStatements("\t", diff.inYnotX));
			}
			throw new SADIException(buf.toString());
		}
		if (withSanityCheck)
			sanityCheckOutput(service, outputModel);
	}
	
	/**
	 * Checks that the services attaches the predicates that it claims to.
	 */
	public static void sanityCheckOutput(Service service, Model output) throws SADIException
	{
		OntModel ontModel = ((ServiceImpl)service).getOutputClass().getOntModel();
		ontModel.addSubModel(output);
		ontModel.rebind();
		try {
			Collection<Individual> outputs = ontModel.listIndividuals(service.getOutputClass()).toList();
			if (outputs.isEmpty())
				throw new SADIException(String.format("output model doesn't contain any instances of output class %s", service.getOutputClassURI()));
			StringBuffer buf = new StringBuffer();
			for (Restriction restriction: ((ServiceImpl)service).getRestrictions()) {
				for (Individual outputInstance: outputs) {
					if (!outputInstance.hasOntClass(restriction))
						buf.append(String.format("\noutput node %s doesn't match restriction %s", output, LabelUtils.getRestrictionString(restriction)));
				}
			}
			if (buf.length() > 0) {
				buf.insert(0, "service output doesn't match output class:");
				throw new SADIException(buf.toString());
			}
		} finally {
			ontModel.removeSubModel(output);
			ontModel.rebind();
		}
	}
}
