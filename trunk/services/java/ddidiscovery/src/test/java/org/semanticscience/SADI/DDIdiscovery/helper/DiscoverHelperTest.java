package org.semanticscience.SADI.DDIdiscovery.helper;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.semanticscience.SADI.DDIdiscovery.Discover;

import ca.wilkinsonlab.sadi.utils.RdfUtils;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import junit.framework.TestCase;

public class DiscoverHelperTest  {

	/*@Test
	public void retrieveSideEffectMapFromPharmGKBTest(){
		String s = "DB00451";
		String q = "DB02648";
		Map <URL, String> aM  = DiscoverHelper.retrieveSideEffectMapFromPharmGKB(s, q);
		System.out.println(aM.size());
		System.out.println(aM);
	}*/

	@Test
	public void getInteractingDBidsTest(){
		List<DrugDrugInteraction> il = DiscoverHelper.getInteractingDrugBankIdentifiersFromPharmGKB("DB02648");
		System.out.println(il.size());
		//System.exit(1);
		
		Iterator <DrugDrugInteraction> itr =il.iterator();
		int c = 0;
		while(itr.hasNext()){
			DrugDrugInteraction ddi = itr.next();
			Map<URL, String> seMap = DiscoverHelper.retrieveSideEffectMapFromPharmGKB(ddi);
			ddi.setSideEffectMap(seMap);
				
		}
		System.out.println(c);
	}
	
	/*@Test
	public void getSideEffectMapTest(){
		Map<URL, String> am = DiscoverHelper.getSideEffectMapFromPharmGKB("DB00451", "DB02648");
		System.out.println(am);
		System.out.println(am.size());
	}*/
}
