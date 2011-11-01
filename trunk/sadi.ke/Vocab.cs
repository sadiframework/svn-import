using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;

namespace SADI.KEPlugin
{
    class Vocab
    {
        private ITripleStoreFactory Factory;

        public Vocab(ITripleStoreFactory factory)
        {
            Factory = factory;
        }

        public const string RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

        private IEntity _rdf_type;
        public IEntity rdf_type
        {
            get
            {
                if (_rdf_type == null)
                {
                    _rdf_type = Factory.CreateEntity(new Uri(RDF + "type"));
                }
                return _rdf_type;
            }
        }
    }
}

