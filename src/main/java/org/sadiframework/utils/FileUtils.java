package org.sadiframework.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class FileUtils 
{
	private static final Logger log = Logger.getLogger(FileUtils.class);
	
	public static void simpleClose(InputStream in) 
	{
		try {
			in.close();
		} catch (IOException e) {
			log.warn("error occurred closing InputStream:", e);
		}
	}
}
