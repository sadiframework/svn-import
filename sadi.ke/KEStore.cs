using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;
using IOInformatics.KE.PluginAPI.Providers;
using System.Text.RegularExpressions;
using System.IO;

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

        public static readonly Regex VARIABLE_PATTERN = new Regex("[?$]([^\\s.])+");

        public bool SPARQLConstruct(SemWeb.Store store, IEntity inputRoot, string inputInstanceQuery)
        {
            inputInstanceQuery.Replace("?input", String.Format("<{0}>", inputRoot.ToString()));
            inputInstanceQuery.Replace("$input", String.Format("<{0}>", inputRoot.ToString()));
            string query =
                    "SELECT * WHERE {" + inputInstanceQuery + "}";
            MatchCollection variableMatches = VARIABLE_PATTERN.Matches(inputInstanceQuery);
            bool addedData = false;
            foreach (ISPARQLResult result in Graph.Query(query).Results)
            {
                string rdf = inputInstanceQuery;
                foreach (Match variableMatch in variableMatches)
                {
                    string variable = variableMatch.Captures[0].Value;
                    if (result[variable] != null)
                    {
                        rdf.Replace(String.Format("?{0}", variable), getString(result[variable]));
                        rdf.Replace(String.Format("${0}", variable), getString(result[variable]));
                    }
                }
                try
                {
                    store.Import(new SemWeb.N3Reader(new StringReader(rdf)));
                    addedData = true;
                }
                catch (Exception err)
                {
                    SADIHelper.error("KEStore", "error constructing RDF", rdf, err);
                }
            }
            return addedData;
        }

        private string getString(IResource resource)
        {
            if (resource is IEntity && (resource as IEntity).Uri == null)
            {
                return String.Format("_:{0}", resource.ToString());
            }
            else
            {
                return resource.ToString();
            }
        }
    }
}
