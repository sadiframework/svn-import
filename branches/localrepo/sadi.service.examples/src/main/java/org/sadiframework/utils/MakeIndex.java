package org.sadiframework.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.sadiframework.SADIException;
import org.sadiframework.beans.ServiceBean;
import org.sadiframework.service.ServiceServlet;
import org.sadiframework.service.ServiceServletHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


import com.hp.hpl.jena.util.FileUtils;

public class MakeIndex
{
	public static void main(String[] args)
	{
		File webXML = new File("src/main/webapp/WEB-INF/web.xml");
		WebXmlParser parser = new WebXmlParser();
		try {
			parser.parse(webXML);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<ServiceBean> services = new ArrayList<ServiceBean>();
//		Reflections reflections = new Reflections("org.sadiframework.service.example");
//		for (Class<? extends ServiceServlet> c: getDescendants(reflections, ServiceServlet.class)) {
		for (String className: parser.classes) {
			try {
				Class<? extends ServiceServlet> c = (Class<? extends ServiceServlet>) Class.forName(className);
				Constructor<? extends ServiceServlet> constructor = c.getConstructor(new Class<?>[0]);
				ServiceServlet servlet = constructor.newInstance(new Object[0]);
				servlet.init();
				ServiceBean service = ServiceServletHelper.getServiceDescription(servlet);
				service.setURI(String.format(".%s", parser.class2url.get(className)));
				services.add(service);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SADIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		File index = new File(args[0]);
		try {
			createPath(index);
			FileWriter writer = new FileWriter(index);
			String template = FileUtils.readWholeFileAsUTF8(MakeIndex.class.getResourceAsStream("/index.template"));
			VelocityContext context = new VelocityContext();
			context.put("services", services);
			Velocity.init();
			Velocity.evaluate(context, writer, "SADI", template);
			writer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	public static <T> Set<Class<? extends T>> getDescendants(final Reflections reflect, final Class<T> type)
//	{
//		HashSet<Class<? extends T>> types = new HashSet<Class<? extends T>>();
//		getDescendants(reflect, type, types);
//		return types;
//	}
//	
//	public static <T> void getDescendants(final Reflections reflect, final Class<? extends T> subtype2, final Set<Class<? extends T>> accum)
//	{
//		for (Class<? extends T> subtype: reflect.getSubTypesOf(subtype2)) {
//			accum.add(subtype);
//			getDescendants(reflect, subtype, accum);
//		}
//	}
	
	private static void createPath(File outfile) throws IOException
	{
		File parent = outfile.getParentFile();
		if (parent != null && !parent.isDirectory())
			if (!parent.mkdirs())
				throw new IOException(String.format("unable to create directory path ", parent));
	}
	
	private static class WebXmlParser extends DefaultHandler
	{
		Map<String, String> name2class;
		Map<String, String> name2url;
		Map<String, String> class2url;
		List<String> classes;

		public WebXmlParser()
		{
			name2class = new HashMap<String, String>();
			name2url = new HashMap<String, String>();
			class2url = new HashMap<String, String>();
			classes = new ArrayList<String>();
		}

		public void parse(File webxmlPath) throws Exception
		{
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setValidating(false);
			SAXParser sp = spf.newSAXParser();
			InputSource input = new InputSource(new FileReader(webxmlPath));
			input.setSystemId("file:" + webxmlPath.getAbsolutePath());
			sp.parse(input, this);
			for (String name: name2class.keySet()) {
				class2url.put(name2class.get(name), name2url.get(name));
			}
		}

		private StringBuffer accumulator = new StringBuffer();
		private String servletName;
		private String servletClass;
		private String servletUrl;

		@Override
		public void characters(char[] buffer, int start, int length)
		{
			accumulator.append(buffer, start, length);
		}

		@Override
		public void endElement (String uri, String localName, String qName) throws SAXException
		{
			if (localName.equals("servlet-name") || qName.equals("servlet-name")) {
				servletName = accumulator.toString().trim();
			} else if (localName.equals("servlet-class") || qName.equals("servlet-class")) {
				servletClass = accumulator.toString().trim();
				classes.add(servletClass);
			} else if (localName.equals("url-pattern") || qName.equals("url-pattern")) {
				servletUrl = accumulator.toString().trim();
			} else if (localName.equals("servlet") || qName.equals("servlet")) {
				name2class.put(servletName, servletClass);
			} else if (localName.equals("servlet-mapping") || qName.equals("servlet-mapping")) {
				name2url.put(servletName, servletUrl);
			}
			accumulator.setLength(0);
		}
	}
}
