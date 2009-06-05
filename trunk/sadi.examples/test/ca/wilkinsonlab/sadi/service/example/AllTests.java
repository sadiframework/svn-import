package ca.wilkinsonlab.sadi.service.example;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
	LinearRegressionServiceServletTest.class,
	UniProt2GoServiceServletTest.class,
	ErmineJServiceServletTest.class 
})

public class AllTests
{
}
