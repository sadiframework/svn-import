using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;

namespace SADI.KEPlugin
{
    public class KEStore
    {
        public IPluginGraph Graph;
        public ITripleStoreFactory Factory;

        private const string RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        private IEntity rdf_type;

        public KEStore(IPluginGraph graph, ITripleStoreFactory factory)
        {
            Graph = graph;
            Factory = factory;

            rdf_type = factory.CreateEntity(new Uri(RDF + "type"));
        }

        public ICollection<IEntity> GetTypes(IEntity node)
        {
            List<IEntity> types = new List<IEntity>();
            foreach (IStatement statement in Graph.Select(node, rdf_type, null))
            {
                if (statement.Object is IEntity)
                {
                    types.Add(statement.Object as IEntity);
                }
            }
            return types;
        }

        public bool HasType(IResource node)
        {
            foreach (IStatement statement in Graph.Select(node, rdf_type, null))
            {
                return true;
            }
            return false;
        }

        public static void Import(SemWeb.Store store)
        {
            throw new NotImplementedException();
        }
    }
}
