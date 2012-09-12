package org.sadiframework.service.nlp;

import gate.Annotation;
import gate.Document;
import gate.FeatureMap;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

public class GateHelperTest
{
	@Test
	@Ignore
	public void testAnnotateText() throws Exception
	{
		String text = IOUtils.toString(getClass().getResourceAsStream("/test.txt"));
		Document doc = GateHelper.getGateHelper().annotateText(text);
		for (Annotation ann: doc.getAnnotations().get("Lookup")) {
			Long s = ann.getStartNode().getOffset();
			Long e = ann.getEndNode().getOffset();
			String drug = doc.getContent().getContent(s, e).toString();
			System.out.println(String.format("found %s at %d:%d", drug, s, e));
			
			FeatureMap features = ann.getFeatures();
			for (Object key: features.keySet()) {
				System.out.println(String.format("\t%s\t%s", key, features.get(key)));
			}
		}
	}
}
