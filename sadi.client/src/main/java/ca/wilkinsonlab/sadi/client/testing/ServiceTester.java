package ca.wilkinsonlab.sadi.client.testing;

import java.util.Collection;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Model;

import ca.wilkinsonlab.sadi.SADIException;
import ca.wilkinsonlab.sadi.client.Service;
import ca.wilkinsonlab.sadi.client.ServiceImpl;
import ca.wilkinsonlab.sadi.utils.OwlUtils;

public class ServiceTester
{
	public static void testService(Service service)
	{
		
	}
	
	public static void testService(Service service, TestCase testCase)
	{
		
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
						buf.append(String.format("\noutput node %s doesn't match restriction %s", output, OwlUtils.getRestrictionString(restriction)));
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
