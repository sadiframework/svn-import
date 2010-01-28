package ca.wilkinsonlab.sadi.utils;

public class DurationUtils
{
	public static long parse(String duration)
	{
		return Long.valueOf(duration);
	}
	
	public static String format(long ms)
	{
		return String.valueOf(ms);
	}
}
