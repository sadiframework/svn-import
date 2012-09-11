package ca.elmonline.util;

import java.util.Iterator;

public class IterableIterator<E> implements Iterable<E>
{
	private Iterator<E> i;
	
	public IterableIterator(Iterator<E> i)
	{
		this.i = i;
	}
	
	@Override
	public Iterator<E> iterator()
	{
		return i;
	}
}
