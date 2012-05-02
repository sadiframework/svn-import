package org.sadiframework.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.sadiframework.utils.JsonUtils;


/**
 * This class is just a wrapper around whatever particular JSON library
 * we happen to be using.  It is important that everwhere we read and
 * write JSON, we're doing it with the same quirks and assumptions.
 * For instance, StringTree JSON requires that all key names are in
 * quotes or it goes into an infinite loop.
 */
public class JsonUtils
{
	private static final Logger log = Logger.getLogger(JsonUtils.class);
	
	private static ObjectMapper mapper = new ObjectMapper();
	
	public static Object read(String s)
	{
		try {
			return mapper.readValue(s, Map.class);
		} catch (JsonParseException e) {
			log.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static String write(Object o)
	{
		StringWriter buf = new StringWriter();
		try {
			mapper.writeValue(buf, o);
		} catch (JsonGenerationException e) {
			log.error(e.getMessage(), e);
		} catch (JsonMappingException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return buf.toString();
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
