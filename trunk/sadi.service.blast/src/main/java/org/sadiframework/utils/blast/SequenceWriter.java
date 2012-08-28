package org.sadiframework.utils.blast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.log4j.Logger;

public final class SequenceWriter implements Runnable
{
	private static final Logger log = Logger.getLogger(SequenceWriter.class);
	
	private BufferedWriter out;
	private StringBuilder buf;
	
	public SequenceWriter(OutputStream out)
	{
		this.out = new BufferedWriter(new OutputStreamWriter(out));
		buf = new StringBuilder();
	}
	
	public void addSequence(String id, String sequence)
	{
		buf.append(">");
		buf.append(id);
		buf.append("\n");
		buf.append(sequence);
		buf.append("\n");
	}

	public void run()
	{
		try {
			out.append(buf.toString());
			out.flush();				
			out.close();
		} catch (IOException e) {
			log.error(e.toString(), e);
			throw new RuntimeException("error writing to output stream", e);
		}
	}
}