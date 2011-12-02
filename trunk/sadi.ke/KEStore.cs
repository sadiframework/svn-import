using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;
using IOInformatics.KE.PluginAPI.Providers;
using System.Text.RegularExpressions;
using System.IO;
using SemWeb.Query;
using SemWeb;

namespace SADI.KEPlugin
{
    public class KEStore : SelectableSource
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

        private readonly Store MappedStore;
        private readonly KEMapper Mapper;
        private readonly KEVocab Vocab;

        public KEStore(ITripleStoreProvider storeProvider, ITripleStoreFactory factory, IVisibilityManager visibilityManager)
        {
            StoreProvider = storeProvider;
            Factory = factory;
            VisibilityManager = visibilityManager;
            MappedStore = new MemoryStore();
            Mapper = new KEMapper(this);
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

        public ICollection<IStatement> Import(Store store)
        {
            cleanupBNodes(store);
            KEMapper mapper = new KEMapper(this);
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

        private const int MAX_LABEL_LENGTH = 30;
        private void cleanupBNodes(Store store)
        {
            foreach (Entity entity in store.GetEntities())
            {
                if (entity is BNode && !store.Contains(new Statement(entity, SemWebVocab.rdfs_label, null)))
                {
                    Resource label = getLabel(store, entity);
                    if (label != null)
                    {
                        if (label is Literal)
                        {
                            Literal l = label as Literal;
                            if (l.DataType == null || l.DataType.EndsWith("string"))
                            {
                                string value = l.Value;
                                if (value.Length > MAX_LABEL_LENGTH)
                                {
                                    label = new Literal(value.Substring(0, MAX_LABEL_LENGTH) + "...", l.Language, l.DataType);
                                }
                            }
                        }
                        store.Add(new Statement(entity, SemWebVocab.rdfs_label, label));
                    }
                }
            }
        }

        private Resource getLabel(Store store, Entity entity)
        {
            Resource value;
            value = getPropertyValue(store, entity, SemWebVocab.has_value);
            if (value != null)
            {
                return value;
            }
            value = getPropertyValue(store, entity, SemWebVocab.rdf_type);
            if (value != null)
            {
                return new Literal(getLocalName(value));
            }
            return null;
        }

        private string getLocalName(Resource value)
        {
            if (value is Entity && (value as Entity).Uri != null)
            {
                string uri = (value as Entity).Uri;
                return uri.Substring(uri.LastIndexOf(uri.Contains("#") ? "#" : "/"));
            }
            else
            {
                return value.ToString();
            }
        }

        private Resource getPropertyValue(Store store, Entity entity, Entity property)
        {
            foreach (Statement statement in store.Select(new Statement(entity, property, null)))
            {
                return statement.Object;
            }
            return null;
        }

        public bool SPARQLConstruct(Store store, IEntity inputRoot, string inputInstanceQuery)
        {
            int incomingStatementCount = store.StatementCount;
            SparqlEngine sparql = new SparqlEngine(inputInstanceQuery);
            sparql.Construct(this, store);
            return store.StatementCount != incomingStatementCount;
        }

        bool SelectableSource.Contains(Statement template)
        {
            foreach (IStatement statement in Graph.Select(Mapper.toKE(template.Subject), Mapper.toKE(template.Predicate), Mapper.toKE(template.Object)))
            {
                return true;
            }
            return false;
        }

        bool SelectableSource.Contains(Resource resource)
        {
            IResource r = Mapper.toKE(resource);
            if (r is IEntity)
            {
                foreach (IStatement statement in Graph.Select(r, null, null))
                {
                    return true;
                }
                foreach (IStatement statement in Graph.Select(null, r, null))
                {
                    return true;
                }
            }
            foreach (IStatement statement in Graph.Select(null, null, r))
            {
                return true;
            }
            return false;
        }

        void SelectableSource.Select(SelectFilter filter, StatementSink sink)
        {
            foreach (ISPARQLResult result in Graph.Query("SELECT DISTINCT * WHERE { ?s ?p ?o }").Results)
            {
                Resource s = Mapper.fromKE(result["s"]);
                Resource p = Mapper.fromKE(result["p"]);
                Resource o = Mapper.fromKE(result["o"]);
                if (passesFilter(filter, s, p, o))
                {
                    sink.Add(new Statement(s as Entity, p as Entity, o));
                }
            }
        }

        private bool passesFilter(SelectFilter filter, Resource subject, Resource predicate, Resource obj)
        {
            bool subjectFilter = passesFilter(filter.Subjects, subject);
            bool predicateFilter = passesFilter(filter.Predicates, predicate);
            bool objectFilter = passesFilter(filter.Objects, obj);
            bool literalFilter = passesFilter(filter.LiteralFilters, obj);
            return subjectFilter && predicateFilter && objectFilter && literalFilter;
        }

        private bool passesFilter(Resource[] haystack, Resource needle)
        {
            if (haystack == null)
            {
                return true;
            }
            else
            {
                foreach (Resource resource in haystack)
                {
                    if (resource.Equals(needle))
                    {
                        return true;
                    }
                }
                return false;
            }
        }

        private bool passesFilter(LiteralFilter[] literalFilters, Resource obj)
        {
            if (literalFilters == null)
            {
                return true;
            }
            else
            {
                foreach (LiteralFilter lf in literalFilters)
                {
                    if (obj is Literal && lf.Filter(obj as Literal, MappedStore))
                    {
                        return true;
                    }
                }
                return false;
            }
        }

        void SelectableSource.Select(Statement template, StatementSink sink)
        {
            foreach (IStatement statement in Graph.Select(Mapper.toKE(template.Subject), Mapper.toKE(template.Predicate), Mapper.toKE(template.Object)))
            {
                sink.Add(Mapper.fromKE(statement));
            }
        }

        bool StatementSource.Distinct
        {
            get { return false; }
        }

        void StatementSource.Select(StatementSink sink)
        {
            foreach (IStatement statement in Graph.Select(null, null, null))
            {
                sink.Add(Mapper.fromKE(statement));
            }
        }
    }
}
