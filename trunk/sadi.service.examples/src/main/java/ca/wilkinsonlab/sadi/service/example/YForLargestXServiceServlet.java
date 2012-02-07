package ca.wilkinsonlab.sadi.service.example;

import java.util.Iterator;

import ca.wilkinsonlab.sadi.service.SynchronousServiceServlet;
import ca.wilkinsonlab.sadi.service.annotations.Authoritative;
import ca.wilkinsonlab.sadi.service.annotations.ContactEmail;
import ca.wilkinsonlab.sadi.service.annotations.Description;
import ca.wilkinsonlab.sadi.service.annotations.InputClass;
import ca.wilkinsonlab.sadi.service.annotations.Name;
import ca.wilkinsonlab.sadi.service.annotations.OutputClass;
import ca.wilkinsonlab.sadi.service.annotations.TestCase;
import ca.wilkinsonlab.sadi.service.annotations.TestCases;
import ca.wilkinsonlab.sadi.service.annotations.URI;
import ca.wilkinsonlab.sadi.utils.RdfUtils;
import ca.wilkinsonlab.sadi.vocab.Regression;
import ca.wilkinsonlab.sadi.vocab.SIO;

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
