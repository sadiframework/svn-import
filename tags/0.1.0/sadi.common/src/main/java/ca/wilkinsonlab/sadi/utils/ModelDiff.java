package ca.wilkinsonlab.sadi.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class ModelDiff
{
	Model x;
	Model y;
	public Model inXnotY;
	public Model inBoth;
	public Model inYnotX;
	
	ModelDiff(Model model1, Model model2)
	{
		x = model1;
		y = model2;
		inXnotY = ModelFactory.createDefaultModel();
		inBoth = ModelFactory.createDefaultModel();
		inYnotX = ModelFactory.createDefaultModel();
	}
	
	public String getDiffString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append(RdfUtils.logStatements("", inXnotY));
		buf.append(RdfUtils.logStatements("\t", inBoth));
		buf.append(RdfUtils.logStatements("\t\t", inYnotX));
		return buf.toString();
	}
	
	/* TODO BNODEs are a problem now. I'm not sure how to map BNODEs onto
	 * each other except to look for isomorphic anonymous structures in the
	 * other graph, which is more than I need to do right now...
	 */
	ModelDiff doDiff()
	{
		for (Iterator<Statement> i = x.listStatements(); i.hasNext(); ) {
			Statement statement = i.next();
			if (!y.contains(statement))
				inXnotY.add(statement);
			else
				inBoth.add(statement);
		}
		for (Iterator<Statement> i = y.listStatements(); i.hasNext(); ) {
			Statement statement = i.next();
			if (!x.contains(statement))
				inYnotX.add(statement);
		}
		return this;
	}
	
	public static ModelDiff diff(Model model1, Model model2)
	{
		return new ModelDiff(model1, model2).doDiff();
	}
	
	private static boolean readURL(Model model, String arg)
	{
		try {
			new URL(arg);
			model.read(arg);
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}
	
	private static boolean readFile(Model model, String arg)
	{
		try {
			model.read(new FileInputStream(arg), "");
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}
	}
	
	private static Model createModel(String arg) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		if (readURL(model, arg))
			return model;
		else if (readFile(model, arg))
			return model;
		else
			throw new IOException("error reading from " + arg);
	}
	
	public static void main(String args[])
	{
		if (args.length != 2) {
			System.err.println("usage: ModelDiff URL|FILE URL|FILE");
			System.exit(1);
		}
		Model model1 = null;
		Model model2 = null;
		try {
			model1 = createModel(args[0]);
			model2 = createModel(args[1]);
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		ModelDiff diff = ModelDiff.diff(model1, model2);
		
		System.out.println(String.format("models %s isomorphic", model1.isIsomorphicWith(model2) ? "are" : "are not"));
		System.out.println(RdfUtils.logStatements("", diff.inXnotY));
		System.out.println(RdfUtils.logStatements("\t", diff.inBoth));
		System.out.println(RdfUtils.logStatements("\t\t", diff.inYnotX));
	}
}
