using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;
using IOInformatics.KE.PluginAPI.Providers;

namespace SADI.KEPlugin
{
    public class KEStore
    {
        public IPluginGraph Graph
        {
            get
            {
                return StoreProvider.Store;
            }
        }
        public ITripleStoreProvider StoreProvider;
        public ITripleStoreFactory Factory;
        public IVisibilityManager VisibilityManager;
        
        private KEVocab Vocab;

        public KEStore(ITripleStoreProvider storeProvider, ITripleStoreFactory factory, IVisibilityManager visibilityManager)
        {
            StoreProvider = storeProvider;
            Factory = factory;
            VisibilityManager = visibilityManager;
            Vocab = new KEVocab(factory);
        }


        public ICollection<IEntity> GetTypes(IEntity node)
        {
            List<IEntity> types = new List<IEntity>();
            foreach (IStatement statement in Graph.Select(node, Vocab.rdf_type, null))
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
            foreach (IStatement statement in Graph.Select(node, Vocab.rdf_type, null))
            {
                return true;
            }
            return false;
        }

        public bool HasType(IResource node, string type)
        {
            foreach (IStatement statement in Graph.Select(node, Vocab.rdf_type, null))
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

        public ICollection<IStatement> Import(SemWeb.Store store)
        {
            KEMapper mapper = GetMapper(store);
            ICollection<IStatement> statements = new List<IStatement>();
            foreach (SemWeb.Statement statement in store.Select(SemWeb.SelectFilter.All))
            {
                statements.Add(mapper.toKE(statement));
            }
            Graph.BeginUpdate();
            Graph.Add(statements);
            Graph.EndUpdate();
            return statements;
        }

        public bool SPARQLConstruct(SemWeb.Store store, IEntity inputRoot, string inputInstanceQuery)
        {
            return false;
        }
    }
}
