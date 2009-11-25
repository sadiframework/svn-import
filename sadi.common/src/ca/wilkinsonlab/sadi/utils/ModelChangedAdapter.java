package ca.wilkinsonlab.sadi.utils;

import java.util.List;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelChangedListener;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public abstract class ModelChangedAdapter implements ModelChangedListener
{
	private static final Logger log = Logger.getLogger(ModelChangedAdapter.class);
	
	public abstract void addedStatement(Statement s);

	public abstract void removedStatement(Statement s);

	public void addedStatements(Statement[] statements)
	{
		if (log.isTraceEnabled())
			log.trace( String.format("adding %d statements from array ", statements.length) );
		
		for (Statement s: statements)
			addedStatement(s);
	}

	@SuppressWarnings("unchecked")
	public void addedStatements(List statements)
	{
		if (log.isTraceEnabled())
			log.trace( String.format("adding %d statements from list ", statements.size()) );
		
		for (Object o: statements)
			addedStatement((Statement)o);
	}

	public void addedStatements(StmtIterator statements)
	{
		if (log.isTraceEnabled())
			log.trace( "adding statements from iterator" );
		
		while (statements.hasNext())
			addedStatement(statements.nextStatement());
	}

	public void addedStatements(Model m)
	{
		if (log.isTraceEnabled())
			log.trace( "adding statements from model" );
		
		addedStatements(m.listStatements());
	}

	public void notifyEvent(Model m, Object event)
	{
		if (log.isTraceEnabled())
			log.trace( String.format("notified of event %s", event) );
	}

	public void removedStatements(Statement[] statements)
	{
		if (log.isTraceEnabled())
			log.trace( String.format("removing %d statements from array ", statements.length) );
		
		for (Statement s: statements)
			removedStatement(s);
	}

	@SuppressWarnings("unchecked")
	public void removedStatements(List statements)
	{
		if (log.isTraceEnabled())
			log.trace( String.format("removing %d statements from list ", statements.size()) );
		
		for (Object o: statements)
			removedStatement((Statement)o);
	}

	public void removedStatements(StmtIterator statements)
	{
		if (log.isTraceEnabled())
			log.trace( "removing statements from iterator" );
		
		while (statements.hasNext())
			removedStatement(statements.nextStatement());
	}

	public void removedStatements(Model m)
	{
		if (log.isTraceEnabled())
			log.trace( "removing statements from model" );
		
		removedStatements(m.listStatements());
	}
}
