using System;
using System.Collections.Generic;
using System.Text;
using SemWeb;
using IOInformatics.KE.PluginAPI;
using System.Net;
using System.IO;

namespace SADI.KEPlugin
{
    public class SADIService
    {
        public String uri;
        public String name;
        public String description;
        public String inputClass;
        public String inputInstanceQuery;
        public ICollection<PropertyRestriction> properties;

        public SADIService(string uri, string name, string description, string inputClass)
        {
            this.uri = uri;
            this.name = name;
            this.description = description;
            this.inputClass = inputClass;
            this.properties = new List<PropertyRestriction>();
        }

        internal void addProperty(string onPropertyURI, string onPropertyLabel, string valuesFromURI, string valuesFromLabel)
        {
            properties.Add(new PropertyRestriction(onPropertyURI, onPropertyLabel, valuesFromURI, valuesFromLabel));
        }

        public override string ToString()
        {
            return uri;
        }

        public MemoryStore invokeService(MemoryStore input)
        {
            SADIHelper.trace("SADIService", "sending data to  " + uri, input);
            WebRequest request = WebRequest.Create(this.uri);
            request.ContentType = "application/rdf+xml";
            request.Method = "POST";
            Stream stream = request.GetRequestStream();
            StreamWriter writer = new StreamWriter(stream);
            using (RdfWriter rdfWriter = new RdfXmlWriter(writer))
            {
                rdfWriter.Write(input);
            }
            writer.Close();
            stream.Close();

            MemoryStore output = new MemoryStore();
            WebResponse response = request.GetResponse();
            stream = response.GetResponseStream();
            StreamReader reader = new StreamReader(stream);
            using (RdfReader rdfReader = new RdfXmlReader(reader))
            {
                output.Import(rdfReader);
            }
            reader.Close();
            stream.Close();
            response.Close();
            SADIHelper.trace("SADIService", "read data from  " + uri, output);

            if (((HttpWebResponse)response).StatusCode == HttpStatusCode.Accepted)
            {
                resolveAsynchronousData(output);
            }
            return output;
        }

        private const string RDFS = "http://www.w3.org/2000/01/rdf-schema#";
        private static Entity isDefinedBy = RDFS + "isDefinedBy";
        private void resolveAsynchronousData(MemoryStore output)
        {
            Dictionary<string, object> seen = new Dictionary<string, object>();
            foreach (Statement s in output.Select(new Statement(null, isDefinedBy, null)))
            {
                if (s.Object.Uri != null)
                {
                    if (!seen.ContainsKey(s.Object.Uri))
                    {
                        seen.Add(s.Object.Uri, null);
                        resolveAsynchronousData(output, s.Object.Uri);
                    }
                    output.Remove(s);
                }
            }
        }

        private void resolveAsynchronousData(MemoryStore output, string uri)
        {
            while (uri != null)
            {
                SADIHelper.trace("SADIService", "fetching asynchronous data", uri);
                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(uri);
                request.Method = "GET";
                request.AllowAutoRedirect = false;
                HttpWebResponse response = (HttpWebResponse)request.GetResponse();
                if (response.StatusCode == HttpStatusCode.Redirect)
                {
                    String newURL = response.Headers["Location"];
                    if (newURL != null)
                    {
                        uri = newURL;
                    }

                    int toSleep = 5;
                    String retry = response.Headers["Retry-After"];
                    try
                    {
                        if (retry != null)
                        {
                            toSleep = Int16.Parse(retry);
                        }
                    }
                    catch (Exception e)
                    {
                        SADIHelper.error("SADIService", "failed to parse Retry-After header", retry, e);
                    }
                    SADIHelper.trace("SADIService", "sleeping " + toSleep + "s before asynchronous request", null);
                    System.Threading.Thread.Sleep(toSleep * 1000);
                }
                else
                {
                    Stream stream = response.GetResponseStream();
                    StreamReader reader = new StreamReader(stream);
                    using (RdfReader rdfReader = new RdfXmlReader(reader))
                    {
                        output.Import(rdfReader);
                    }
                    reader.Close();
                    stream.Close();
                    uri = null;
                }
                response.Close();
            }
        }
    }
}
