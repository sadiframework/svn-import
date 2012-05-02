package org.sadiframework.owl2sparql;

import java.util.Iterator;
import java.util.Set;

import org.sadiframework.decompose.ClassTracker;
import org.sadiframework.decompose.DefaultClassTracker;
import org.sadiframework.decompose.VisitingDecomposer;


import com.hp.hpl.jena.ontology.ComplementClass;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.UnionClass;

public class QueryGeneratingDecomposer extends VisitingDecomposer 
{
	private StringBuilder construct;
	private StringBuilder where;
//	private VariableGenerator generator;
//	private String variable;
	
	public QueryGeneratingDecomposer()
	{
		this(new DefaultClassTracker(), new StringBuilder(), new StringBuilder(), new DefaultVariableGenerator(), "?input");
	}
	
	public String getQuery()
	{
		return String.format(
				"CONSTRUCT { \n" +
				"%s} WHERE { \n" +
				"%s}", construct.toString(), where.toString());
	}
	
	public QueryGeneratingDecomposer(ClassTracker tracker, StringBuilder construct, StringBuilder where, VariableGenerator generator, String variable)
	{
		super(tracker, new QueryGeneratingClassVisitor(construct, where, variable), new QueryGeneratingRestrictionVisitor(tracker, construct, where, generator, variable));
		this.construct = construct;
		this.where = where;
//		this.variable = variable.startsWith("?") ? variable : String.format("?%s", variable);
	}

	@Override
	protected void decomposeSuperClasses(Set<OntClass> set)
	{
		// superClasses are necessary but not sufficient...
	}

	@Override
	protected void decomposeUnionClass(UnionClass clazz)
	{
		for (Iterator<? extends OntClass> i = clazz.listOperands(); i.hasNext(); ) {
			OntClass c = i.next();
			where.append("{\n");
			decompose(c);
			where.append("}");
			if (i.hasNext())
				where.append(" UNION ");
		}
		where.append("\n");
	}

	@Override
	protected void decomposeComplementClass(ComplementClass clazz)
	{
		// SPARQL 1.1 only...
		where.append("NOT EXISTS {\n");
		decompose(clazz.getOperand());
		where.append("\n}");
	}
}
