using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;
using IOInformatics.KE.PluginAPI.Providers;

namespace SADI.KEPlugin
{
    public class KEStore
    {
        public IPluginGraph Graph;
        public ITripleStoreFactory Factory;
        public IVisibilityManager VisibilityManager;

        private const string RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        private IEntity rdf_type;

        public KEStore(IPluginGraph graph, ITripleStoreFactory factory, IVisibilityManager visibilityManager)
        {
            Graph = graph;
            Factory = factory;
            VisibilityManager = visibilityManager;

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

        public bool HasType(IResource node, string type)
        {
            foreach (IStatement statement in Graph.Select(node, rdf_type, null))
            {
                if (statement.Object is IEntity && (statement.Object as IEntity).Uri.ToString() == type)
                {
                    return true;
                }
            }
            return false;
        }

        public KEMapper GetMapper(SemWeb.Store store)
        {
            return new KEMapper(this, store);
        }

        public static void Import(SemWeb.Store store)
        {
            throw new NotImplementedException();
        }
    }
}
