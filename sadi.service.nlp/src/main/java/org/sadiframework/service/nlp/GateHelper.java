package org.sadiframework.service.nlp;

import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

import java.io.File;
import java.net.URI;

import org.apache.log4j.Logger;

public class GateHelper
{
	private static final Logger log = Logger.getLogger(GateHelper.class);
	private static GateHelper theInstance = null;

	public static GateHelper getGateHelper()
	{
		if (theInstance == null) {
			theInstance = new GateHelper();
		}
		return theInstance;
	}
	
	private SerialAnalyserController annieController;
	
	private GateHelper()
	{
		if (!Gate.isInitialised()) {
			File gateDirectory = new File(URI.create(GateHelper.class.getResource("/gate").toExternalForm()));
			try {
				Gate.setGateHome(gateDirectory);
				Gate.setPluginsHome(new File(gateDirectory, "plugins"));
				Gate.init();
			} catch (IllegalStateException e) {
				log.error("error configuring gate", e);
			} catch (GateException e) {
				log.error("error initialising gate", e);
			}
		}
		try {
			Gate.getCreoleRegister().registerDirectories(getClass().getResource("/gate/plugins/ANNIE"));
			annieController = (SerialAnalyserController)Factory.createResource(
					"gate.creole.SerialAnalyserController", Factory.newFeatureMap(), 
					Factory.newFeatureMap(), "ANNIE_" + Gate.genSym());
			FeatureMap owParams = Factory.newFeatureMap();
			owParams.put("caseSensitive", true);
			owParams.put("longestMatchOnly", true);
			owParams.put("encoding", "UTF-8");
			owParams.put("gazetteerFeatureSeparator", "@");
			owParams.put("listsURL", getClass().getResource("/gate/Gazetteers/DrugNames/lists.def"));
			ProcessingResource owPr = (ProcessingResource)Factory.createResource(
					"gate.creole.gazetteer.DefaultGazetteer", owParams);
			annieController.add(owPr);
			Corpus gateCorpus = Factory.newCorpus("corpus");
			annieController.setCorpus(gateCorpus);
		} catch (GateException e) {
			log.error("error configuring ANNIE plugin", e);
		}
	}

	@SuppressWarnings("unchecked") // shut up generics warnings related to Corpus
	public synchronized Document annotateText(String text) throws GateException
	{
		Document doc = Factory.newDocument(text);
		doc.setName("documents");
		Corpus gateCorpus = annieController.getCorpus();
		gateCorpus.clear();
		gateCorpus.add(doc);
		annieController.execute();
		return doc;
	}
}
