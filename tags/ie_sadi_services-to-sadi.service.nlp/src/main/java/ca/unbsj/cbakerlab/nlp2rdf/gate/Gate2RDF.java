package ca.unbsj.cbakerlab.nlp2rdf.gate;


import com.hp.hpl.jena.ontology.OntModel;

import gate.util.GateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 */
public class Gate2RDF {
    private static Logger log = LoggerFactory.getLogger(Gate2RDF.class);  

	public static final Map<String,String> GATE2OLIA = createGATE2OLIAMap();	

    private static Map<String, String> createGATE2OLIAMap() {
        Map<String, String> result = new HashMap<String, String>();
        result.put("Token", Utils.oliaOntology+"Token");
        result.put("SpaceToken", Utils.oliaOntology+"SpaceToken");
        result.put("Sentence", Utils.oliaOntology+"Sentence");
        result.put("Split", Utils.oliaOntology+"Split");
        result.put("category", Utils.oliaSystemOntology+"hasTag");
        return Collections.unmodifiableMap(result);
    }
    
    
    public OntModel processTextWithDrugNameAnnieGazetteer(String text, String documentUri) throws FileNotFoundException, IOException, URISyntaxException, GateException{
    	DrugNameAnnieGazetteer2RDF g2r = new DrugNameAnnieGazetteer2RDF();
		return g2r.processText(text, documentUri);
	}
	
	
    /**
     * MAIN
     * @param args
     * @throws Exception
     */
	public static void main(String[] args) throws Exception {

		// String text =
		// "Should it be used with Indinavir? I worked at an HIV clinic for two years. We used St. John's with great results right alongside conventional anti-virals with no adverse interaction. The major side-effects of Indinavir are headaches, nausea, vomiting, diarrhea, dizziness and insomnia. Again, after consulting with your physician, you may consider trying a good quality St. John's Wort with regular monitoring. What about Digoxin and St. John's? This is probably referring to a small (162) study of in which St. John's demonstrated its ability to promote stable heart function while decreasing depression. So, yes, it's an herb trying to stabilize the organism and may tonify the heart. This is a beneficial side effect. Why is Theophylline, primarily an asthmatic drug, listed? Probably for the same reason as digoxin. Theophylline can cause life-threatening ventricular arrythmias. (And frankly, don't use St. John's when using Theophylline because if the ventricular arrythmias occur, St. John's will be blamed.)	Should St. John's be used with immune suppressants such as Cyclosporin? Absolutely not. The overwhelming majority of medicinal herbs should be avoided with immune suppressants. Herbs strengthen the human organism. They would work directly against drugs designed to weaken the human organism. 	What about using St. John's with chemotherapy? In my practice, I tend to avoid most herbs during the actual process of chemotherapy. I use them before hand to build up the body and afterward to repair the damage to the normal cells.   		Periodically, the pharmaceutical industry feels threatened by a natural product and goes after it in a very deliberate way. About 10 years ago, it was L-Tryptophan. L-Tryptophan was an inexpensive amino acid that was extremely effective in increasing seratonin levels and was virtually without side effects. A Japanese company produced a single bad batch after failing to complete the chemical process and several deaths occurred. The FDA was able to trace the bad batch back to the Japanese company, but still decided to ban the supplement.  Two years ago, the media flooded the public with warnings that St. John's Wort would cause birth control pills to fail resulting in a host of unwanted pregnancies. When the pregnancies didn't occur, the pharmaceuticals tried a different angle. If it were patentable and the pharmaceutical companies could make money off of it, this Internet email warning about herbs would not have occurred. Consider Viagra. It has caused over 60 deaths and continues to be on the market.";
		// String text =
		// "Should it be used with Indinavir? I worked at an HIV clinic for two years. We used St. John's with great results right alongside conventional anti-virals with no adverse interaction. The major side-effects of Indinavir are headaches, nausea, vomiting, diarrhea, dizziness and insomnia. Again, after consulting with your physician, you may consider trying a good quality St. John's Wort with regular monitoring. What about Digoxin and St. John's? This is probably referring to a small (162) study of in which St. John's demonstrated its ability to promote stable heart function while decreasing depression. So, yes, it's an herb trying to stabilize the organism and may tonify the heart. This is a beneficial side effect. Why is Theophylline, primarily an asthmatic drug, listed? Probably for the same reason as digoxin. Theophylline can cause life-threatening ventricular arrythmias. (And frankly, don't use St. John's when using Theophylline because if the ventricular arrythmias occur, St. John's will be blamed.) Should St. John's be used with immune suppressants such as Cyclosporin? Absolutely not. The overwhelming majority of medicinal herbs should be avoided with immune suppressants. Herbs strengthen the human organism. They would work directly against drugs designed to weaken the human organism. What about using St. John's with chemotherapy? In my practice, I tend to avoid most herbs during the actual process of chemotherapy. I use them before hand to build up the body and afterward to repair the damage to the normal cells. Periodically, the pharmaceutical industry feels threatened by a natural product and goes after it in a very deliberate way. About 10 years ago, it was L-Tryptophan. L-Tryptophan was an inexpensive amino acid that was extremely effective in increasing seratonin levels and was virtually without side effects. A Japanese company produced a single bad batch after failing to complete the chemical process and several deaths occurred. The FDA was able to trace the bad batch back to the Japanese company, but still decided to ban the supplement. Two years ago, the media flooded the public with warnings that St. John's Wort would cause birth control pills to fail resulting in a host of unwanted pregnancies. When the pregnancies didn't occur, the pharmaceuticals tried a different angle. If it were patentable and the pharmaceutical companies could make money off of it, this Internet email warning about herbs would not have occurred. Consider Viagra. It has caused over 60 deaths and continues to be on the market.";

		String text = "a protease inhibitor used to treat HIVc. Digoxin (Lanoxicaps, Lanoxin), a drug used to increase the force of contraction of heart muscle and to regulate heartbeatsd.";
		String text2 = "St. John's Wort - Avoid mixing with any prescription medications. In particular, avoid taking St. John's Wort (SJW) and:a. Antidepressantsb. B. Indinavir sulfate (Crixivan)";

		Gate2RDF g2r = new Gate2RDF();

		/*
		OntModel model = g2r.processTextWithDrugNameAnnieGazetteer(text);
		log.info("returning " + model.size() + " triples");

		OntModel model2 = g2r.processTextWithDrugNameAnnieGazetteer(text2);
		log.info("returning " + model2.size() + " triples");

		model.add(model2);

		// For debugging.
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("txt2rdf_model.rdf")));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		model.write(out, "RDF/XML");
		try {
			out.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		model.close();
		model2.close();

		Model model3 = ModelFactory.createDefaultModel();
		model3.read(new FileReader(new File("txt2rdf_model.rdf")), "UTF8");

		Gate2RDF.rdf2gateXml(model3);
	

		model3.close();
	*/
		System.out.println("Finished.");

	}

}
