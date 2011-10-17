package ca.wilkinsonlab.sadi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stringtree.json.JSONReader;
import org.stringtree.json.JSONWriter;

/**
 * This class is just a wrapper around whatever particular JSON library
 * we happen to be using.  It is important that everyone we read and
 * write JSON, we're doing it with the same quirks and assumptions.
 * For instance, StringTree JSON requires that all key names are in
 * quotes or it goes into an infinite loop.
 */
public class JsonUtils
{
	public static Object read(String s)
	{
		return new JSONReader().read(s);
	}
	
	public static String write(Object o)
	{
		return new JSONWriter().write(o);
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, String>> convertJSONToResults(Object result)
	{
		List<Map<String, Map<?, ?>>> virtuosoBindings = (List<Map<String, Map<?, ?>>>)((Map<?, ?>)((Map<?, ?>)result).get("results")).get("bindings");
		List<Map<String, String>> localBindings = new ArrayList<Map<String, String>>(virtuosoBindings.size());
		for (Map<String, Map<?, ?>> virtuosoBinding: virtuosoBindings) {
			Map<String, String> ourBinding = new HashMap<String, String>();
			for (String variable: virtuosoBinding.keySet()) {
				ourBinding.put(variable, (String)virtuosoBinding.get(variable).get("value"));
			}
			localBindings.add(ourBinding);
		}
		return localBindings;
	}
}
