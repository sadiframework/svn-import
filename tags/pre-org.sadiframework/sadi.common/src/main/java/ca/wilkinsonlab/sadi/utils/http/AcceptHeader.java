package ca.wilkinsonlab.sadi.utils.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

public class AcceptHeader
{
	private static final Logger log = Logger.getLogger(AcceptHeader.class);
	
	private List<ContentTypeQualityPair> contentTypes;

	public AcceptHeader()
	{
		contentTypes = new ArrayList<ContentTypeQualityPair>();
	}
	
	public AcceptHeader(String accept)
	{
		this();
		merge(accept);
	}
	
	public void merge(String accept)
	{
		String[] types = accept.split("\\s*,\\s*");
		for (String type: types) {
			try{ 
				contentTypes.add(new ContentTypeQualityPair(type));
			} catch (NumberFormatException e) {
				log.error(String.format("can't parse quality value in %s", type));
			}
		}
		Collections.sort(contentTypes);
	}
	
	public List<ContentTypeQualityPair> getContentTypes()
	{
		return contentTypes;
	}
	
	public static class ContentTypeQualityPair implements Comparable<ContentTypeQualityPair>
	{
		private String contentType;
		private double quality;
		
		public ContentTypeQualityPair(String s)
		{
			String[] fields = s.split("\\s*;\\s*");
			if (fields.length < 2) {
				contentType = s;
				quality = 1.0;
			} else {
				contentType = fields[0];
				if (fields[1].startsWith("q="))
					fields[1] = fields[1].substring(2); // TODO deal with whitespace?
				quality = Double.parseDouble(fields[1]);
			}
		}

		@Override
		public int compareTo(ContentTypeQualityPair that)
		{
			if (this.quality < that.quality)
				return -1;
			else if (this.quality > that.quality)
				return 1;
			else
				return 0;
		}

		public String getContentType()
		{
			return contentType;
		}

		public void setContentType(String contentType)
		{
			this.contentType = contentType;
		}

		public double getQuality()
		{
			return quality;
		}

		public void setQuality(double quality)
		{
			this.quality = quality;
		}
	}
}
