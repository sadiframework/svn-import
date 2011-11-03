using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;

namespace SADI.KEPlugin
{
    class KEVocab
    {
        public const string RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        public const string RDFS = "http://www.w3.org/2000/01/rdf-schema#";
        public const string LSRN = "http://purl.oclc.org/SADI/LSRN/";
        public const string SIO = "http://semanticscience.org/resource/";
        
        private ITripleStoreFactory Factory;

        public KEVocab(ITripleStoreFactory factory)
        {
            Factory = factory;
        }

        private static readonly Uri _rdf_type_uri = new Uri(RDF + "type");
        private IEntity _rdf_type;
        public IEntity rdf_type
        {
            get
            {
                if (_rdf_type == null)
                {
                    _rdf_type = Factory.CreateEntity(_rdf_type_uri);
                }
                return _rdf_type;
            }
        }
    }
}

