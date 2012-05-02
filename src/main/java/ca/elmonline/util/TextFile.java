package ca.elmonline.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Iterator;

public class TextFile extends File implements Iterable<String>
{
	private static final long serialVersionUID = 1L;

	public TextFile(File parent, String child)
	{
		super(parent, child);
	}

	public TextFile(String parent, String child)
	{
		super(parent, child);
	}

	public TextFile(String pathname)
	{
		super(pathname);
	}

	public TextFile(URI uri)
	{
		super(uri);
	}

	@Override
	public TextStreamIterator iterator()
	{
		try {
			return new TextStreamIterator(new FileInputStream(this));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static class TextStreamIterator implements Iterator<String>, Closeable
	{
		BufferedReader reader;
		String next;
		
		public TextStreamIterator(InputStream is)
		{
			reader = new BufferedReader(new InputStreamReader(is));
			try {
				next = reader.readLine();
				if (next == null)
					reader.close();
			} catch(IOException e) { 
				throw new IllegalArgumentException(e); 
			}
		}
		
		@Override
		public boolean hasNext()
		{
			return next != null;
		}

		@Override
		public String next()
		{
	        String result = next;
			try {
				if (next != null) {
					next = reader.readLine();
					if (next == null)
						reader.close();
				}
			} catch (IOException e) { 
				throw new RuntimeException(e);
			}
		    return result;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws IOException
		{
			next = null;
			reader.close();
		}
	}
}
