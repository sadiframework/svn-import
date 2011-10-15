using System;
using System.Collections.Generic;
using System.Text;
using System.Net;
using System.IO;
using LitJson;
using System.Collections.Specialized;

namespace SADI.KEPlugin
{
    public class SADIRegistry
    {
        private String endpoint;
        private String graph;

        public SADIRegistry(String endpoint, String graph)
        {
            this.endpoint = endpoint;
            this.graph = graph;
        }

        private static SADIRegistry instance;
        public static SADIRegistry Instance()
        {
            if (instance == null)
            {
                instance = new SADIRegistry("http://biordf.net/sparql", "http://sadiframework.org/registry/");
            }
            return instance;
        }

        public int getServiceCount()
        {
            string query =
                "PREFIX sadi: <http://sadiframework.org/ontologies/sadi.owl#> \r\n" +
                "SELECT COUNT(*) \r\n" +
                "WHERE { \r\n" +
                "  ?s a sadi:Service . \r\n" +
                "}";
            return getSPARQLBindingAsInteger(executeQuery(query)[0], "callret-0");
        }

        public ICollection<SADIService> getAllServices(int offset, int limit)
        {
            ICollection<SADIService> services = new List<SADIService>();

            string query =
                "PREFIX sadi: <http://sadiframework.org/ontologies/sadi.owl#> \r\n" +
                "PREFIX mygrid: <http://www.mygrid.org.uk/mygrid-moby-service#> \r\n" +
                "SELECT * \r\n" +
                "WHERE {\r\n" +
                "   ?serviceURI a sadi:service . \r\n" +
                "	?serviceURI mygrid:hasServiceNameText ?name . \r\n" +
                "	?serviceURI mygrid:hasServiceDescriptionText ?description . \r\n" +
                "	?serviceURI mygrid:hasOperation ?op . \r\n" +
                "   ?serviceURI sadi:inputInstanceQuery ?query . \r\n" +
                "	?op mygrid:inputParameter ?input . \r\n" +
                "	?input a mygrid:parameter . \r\n" +
                "	?input mygrid:objectType ?inputClassURI . \r\n" +
                "}";

            foreach (JsonData binding in executeQuery(query))
            {
                SADIService service = new SADIService(
                    getSPARQLBindingAsString(binding, "serviceURI"),
                    getSPARQLBindingAsString(binding, "name"),
                    getSPARQLBindingAsString(binding, "description"),
                    getSPARQLBindingAsString(binding, "inputClassURI"));
                service.inputInstanceQuery = getSPARQLBindingAsString(binding, "query");
                services.Add(service);
            }
            return services;
        }

        public ICollection<SADIService> findServicesByInputClass(ICollection<String> types)
        {
            ICollection<SADIService> services = new List<SADIService>();

            if (types.Count == 0)
            {
                return services;
            }

            string query =
                "PREFIX sadi: <http://sadiframework.org/ontologies/sadi.owl#> \n" +
                "PREFIX mygrid: <http://www.mygrid.org.uk/mygrid-moby-service#> \n" +
                "SELECT * \n" +
                "WHERE {\n" +
                "   ?serviceURI a sadi:Service . \n" +
                "	?serviceURI mygrid:hasServiceNameText ?name . \n" +
                "	?serviceURI mygrid:hasServiceDescriptionText ?description . \n" +
                "	?serviceURI mygrid:hasOperation ?op . \n" +
                "	?op mygrid:inputParameter ?input . \n" +
                "	?input a mygrid:parameter . \n" +
                "	?input mygrid:objectType ?inputClassURI . \n";

            int n=0;
            foreach (string type in types)
            {
                if (++n > 1)
                {
                    query += "	UNION \n";
                }
                query += "	{ ?input mygrid:objectType <" + type + "> } \n";
                
            }

            query +=
                "}";

            foreach (JsonData binding in executeQuery(query))
            {
                SADIService service = new SADIService(
                    getSPARQLBindingAsString(binding, "serviceURI"),
                    getSPARQLBindingAsString(binding, "name"),
                    getSPARQLBindingAsString(binding, "description"),
                    getSPARQLBindingAsString(binding, "inputClassURI"));
                services.Add(service);
            }
            return services;
        }

        public void addPropertyRestrictions(SADIService service)
        {
            String query = 
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\r\n" +
                "PREFIX sadi: <http://sadiframework.org/ontologies/sadi.owl#>\r\n" +
                "PREFIX mygrid: <http://www.mygrid.org.uk/mygrid-moby-service#>\r\n" +
                "SELECT DISTINCT ?onPropertyURI ?onPropertyLabel ?valuesFromURI ?valuesFromLabel\r\n" +
                "WHERE {\r\n" +
                "	<" + service.uri + "> sadi:decoratesWith ?decoration .\r\n" +
                "	?decoration owl:onProperty ?onPropertyURI .\r\n" +
                "	OPTIONAL { ?onPropertyURI rdfs:label ?onPropertyLabel } . \r\n" +
                "	OPTIONAL {\r\n" +
                "		?decoration owl:someValuesFrom ?valuesFromURI .\r\n" +
                "		OPTIONAL { ?valuesFromURI rdfs:label ?valuesFromLabel }\r\n" +
                "	} .\r\n" +
                "}";

            foreach (JsonData binding in executeQuery(query))
            {
                service.addProperty(
                    getSPARQLBindingAsString(binding, "onPropertyURI"), 
                    getSPARQLBindingAsString(binding, "onPropertyLabel"), 
                    getSPARQLBindingAsString(binding, "valuesFromURI"), 
                    getSPARQLBindingAsString(binding, "valuesFromLabel"));
            }
        }

        private JsonData executeQuery(string query)
        {
            Console.Out.WriteLine("executing query:\n" + query);
            NameValueCollection data = new NameValueCollection();
            data.Add("", "");
            data.Add("query", query);
            data.Set("format", "JSON");
            if (graph != null)
            {
                data.Set("default-graph-uri", graph);
            }

            WebClient client = new WebClient();
            byte[] response = client.UploadValues(endpoint, data);
            JsonData json = JsonMapper.ToObject(Encoding.UTF8.GetString(response));
            return json["results"]["bindings"];
        }

        private static string getSPARQLBindingAsString(JsonData binding, String variable)
        {
            try
            {
                return binding[variable]["value"].ToString();
            }
            catch (Exception e)
            {
                System.Diagnostics.Debug.WriteLine(e.StackTrace);
                return null;
            }
        }

        private static int getSPARQLBindingAsInteger(JsonData binding, String variable)
        {
            try
            {
                return (int)binding[variable]["value"];
            }
            catch (Exception e)
            {
                System.Diagnostics.Debug.WriteLine(e.StackTrace);
                return 0;
            }
        }
    }
}
