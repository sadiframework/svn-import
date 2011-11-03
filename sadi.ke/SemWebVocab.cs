using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;
using SemWeb;

namespace SADI.KEPlugin
{
    class SemWebVocab
    {
        public const string RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        public const string RDFS = "http://www.w3.org/2000/01/rdf-schema#";
        public const string OWL = "http://www.w3.org/2002/07/owl";
        public const string LSRN = "http://purl.oclc.org/SADI/LSRN/";
        public const string SIO = "http://semanticscience.org/resource/";

        public static readonly Entity rdf_type = RDF + "type";
        public static readonly Entity isDefinedBy = RDFS + "isDefinedBy";
        public static readonly Entity has_identifier = SIO + "SIO_000671";
        public static readonly Entity has_value = SIO + "SIO_000300";
        public static readonly Entity owl_thing = OWL + "Thing";

        public static readonly Uri rdf_type_uri = new Uri(RDF + "type");
    }
}

