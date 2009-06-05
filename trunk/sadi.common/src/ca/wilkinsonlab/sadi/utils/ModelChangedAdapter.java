package ca.wilkinsonlab.sadi.utils;

import java.util.List;

import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelChangedListener;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.commons.logging.Log;

public abstract class ModelChangedAdapter implements ModelChangedListener
{
	private static final Log log = LogFactory.getLog(ModelChangedAdapter.class);
	
	public abstract void addedStatement(Statement s);

	public abstract void removedStatement(Statement s);

	public void addedStatements(Statement[] statements)
	{
		for (Statement s: statements)
			addedStatement(s);
	}

	public void addedStatements(List statements)
	{
		for (Object o: statements)
			addedStatement((Statement)o);
	}

	public void addedStatements(StmtIterator statements)
	{
		while (statements.hasNext())
			addedStatement(statements.nextStatement());
	}

	public void addedStatements(Model m)
	{
		addedStatements(m.listStatements());
	}

	public void notifyEvent(Model m, Object event)
	{
		if (false)
			log.warn(String.format("unexpected call to notifyEvent(%s, %s)", m, event),
					new UnsupportedOperationException());
	}

	public void removedStatements(Statement[] statements)
	{
		for (Statement s: statements)
			removedStatement(s);
	}

	public void removedStatements(List statements)
	{
		for (Object o: statements)
			removedStatement((Statement)o);
	}

	public void removedStatements(StmtIterator statements)
	{
		while (statements.hasNext())
			removedStatement(statements.nextStatement());
	}

	public void removedStatements(Model m)
	{
		removedStatements(m.listStatements());
	}
}
