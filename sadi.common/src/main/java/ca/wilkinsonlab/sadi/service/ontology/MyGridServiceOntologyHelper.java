package ca.wilkinsonlab.sadi.service.ontology;

import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.Authoritative;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.ContactEmail;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.Description;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.InputClass;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.Name;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.OutputClass;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.ParameterClass;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.ParameterInstance;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.ServiceClass;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.ServiceProvider;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.TestCase;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.TestInput;
import ca.wilkinsonlab.sadi.service.ontology.AbstractServiceOntologyHelper.TestOutput;

/**
 * An implementation of ServiceOntologyHelper that reads/writes service
 * configuration according to the myGrid service ontology.
 * 
 * @author Luke McCarthy
 */
@ServiceClass("http://www.mygrid.org.uk/mygrid-moby-service#serviceDescription")
@Name({"http://www.mygrid.org.uk/mygrid-moby-service#hasServiceNameText", "*"})
@Description({"http://www.mygrid.org.uk/mygrid-moby-service#hasServiceDescriptionText", "*"})
@ServiceProvider({"http://www.mygrid.org.uk/mygrid-moby-service#providedBy", "http://www.mygrid.org.uk/mygrid-moby-service#organisation",
	              "http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#publisher", "*"})
@ContactEmail({"http://www.mygrid.org.uk/mygrid-moby-service#providedBy", "http://www.mygrid.org.uk/mygrid-moby-service#organisation", 
	           "http://protege.stanford.edu/plugins/owl/dc/protege-dc.owl#creator", "*"})
@Authoritative({"http://www.mygrid.org.uk/mygrid-moby-service#providedBy", "http://www.mygrid.org.uk/mygrid-moby-service#organisation",
                "http://www.mygrid.org.uk/mygrid-moby-service#authoritative", "*"})
@InputClass({"http://www.mygrid.org.uk/mygrid-moby-service#hasOperation", "http://www.mygrid.org.uk/mygrid-moby-service#operation",
             "http://www.mygrid.org.uk/mygrid-moby-service#inputParameter", "http://www.mygrid.org.uk/mygrid-moby-service#parameter",
             "http://www.mygrid.org.uk/mygrid-moby-service#objectType", "*"})
@OutputClass({"http://www.mygrid.org.uk/mygrid-moby-service#hasOperation", "http://www.mygrid.org.uk/mygrid-moby-service#operation",
              "http://www.mygrid.org.uk/mygrid-moby-service#outputParameter", "http://www.mygrid.org.uk/mygrid-moby-service#parameter",
              "http://www.mygrid.org.uk/mygrid-moby-service#objectType", "*"})
@ParameterClass({"http://www.mygrid.org.uk/mygrid-moby-service#hasOperation", "http://www.mygrid.org.uk/mygrid-moby-service#operation",
                 "http://www.mygrid.org.uk/mygrid-moby-service#inputParameter", "http://www.mygrid.org.uk/mygrid-moby-service#secondaryParameter",
                 "http://www.mygrid.org.uk/mygrid-moby-service#objectType", "*"})
@ParameterInstance({"http://www.mygrid.org.uk/mygrid-moby-service#hasOperation", "http://www.mygrid.org.uk/mygrid-moby-service#operation",
                    "http://www.mygrid.org.uk/mygrid-moby-service#inputParameter", "http://www.mygrid.org.uk/mygrid-moby-service#secondaryParameter",
                    "http://www.mygrid.org.uk/mygrid-moby-service#hasDefaultValue", "*"})
@TestCase({"http://www.mygrid.org.uk/mygrid-moby-service#hasOperation", "http://www.mygrid.org.uk/mygrid-moby-service#operation",
	       "http://www.mygrid.org.uk/mygrid-moby-service#hasUnitTest", "http://www.mygrid.org.uk/mygrid-moby-service#unitTest"})
@TestInput({"http://www.mygrid.org.uk/mygrid-moby-service#exampleInput", "*"})
@TestOutput({"http://www.mygrid.org.uk/mygrid-moby-service#exampleOutput", "*"})
public class MyGridServiceOntologyHelper extends AbstractServiceOntologyHelper
{
	public MyGridServiceOntologyHelper()
	{
	}
}
