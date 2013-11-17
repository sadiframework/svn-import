package org.sadiframework.service.example;

import java.util.Iterator;

import org.sadiframework.service.SynchronousServiceServlet;
import org.sadiframework.service.annotations.Authoritative;
import org.sadiframework.service.annotations.ContactEmail;
import org.sadiframework.service.annotations.Description;
import org.sadiframework.service.annotations.InputClass;
import org.sadiframework.service.annotations.Name;
import org.sadiframework.service.annotations.OutputClass;
import org.sadiframework.service.annotations.TestCase;
import org.sadiframework.service.annotations.TestCases;
import org.sadiframework.service.annotations.URI;
import org.sadiframework.utils.RdfUtils;
import org.sadiframework.vocab.Regression;
import org.sadiframework.vocab.SIO;


import com.hp.hpl.jena.rdf.model.Resource;

@URI("http://sadiframework.org/examples/y4x")
@Name("Y-for-largest-X")
@Description("Given a collection of paired values, return the y value corresponding to the largest x value.")
@ContactEmail("info@sadiframework.org")
@InputClass("http://sadiframework.org/examples/regression.owl#PairedValueCollection")
@OutputClass("http://sadiframework.org/examples/regression.owl#YForLargestX")
@Authoritative(true)
@TestCases({
		@TestCase(
				input = "http://sadiframework.org/examples/t/y4x.input.1.rdf", 
				output = "http://sadiframework.org/examples/t/y4x.output.1.rdf"
		)
})
public class YForLargestXServiceServlet extends SynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	public void processInput(Resource input, Resource output)
	{
		Resource maxX = null, yForMaxX = null;
		for (Iterator<Resource> pairs = RdfUtils.getPropertyValues(input, SIO.has_member, Regression.PairedValue); pairs.hasNext(); ) {
			Resource pair = pairs.next();
			Resource x = RdfUtils.getPropertyValue(pair, SIO.has_attribute, Regression.X);
			Resource y = RdfUtils.getPropertyValue(pair, SIO.has_attribute, Regression.Y);
			if (maxX == null || maxX.getRequiredProperty(SIO.has_value).getDouble() < 
					x.getRequiredProperty(SIO.has_value).getDouble()) {
				maxX = x;
				yForMaxX = y;
			}
		}
		if (yForMaxX != null) {
			output.addProperty(Regression.yForLargestX, yForMaxX.getRequiredProperty(SIO.has_value).getObject());
		}
	}
}
