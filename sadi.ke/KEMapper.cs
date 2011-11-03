using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;
using SemWeb;

namespace SADI.KEPlugin
{
    public class KEMapper
    {
        private KEStore KE;
        private Store Store;
        private Dictionary<string, IEntity> map;
        private Dictionary<string, Entity> FromKE;

        public KEMapper(KEStore ke, Store store)
        {
            KE = ke;
            Store = store;
            map = new Dictionary<string, IEntity>();
            FromKE = new Dictionary<string, Entity>();
        }

        public IStatement toKE(Statement statement)
        {
            return KE.Factory.CreateStatement(
                        toKE(statement.Subject),
                        toKE(statement.Predicate),
                        toKE(statement.Object));
        }

        public IResource toKE(Resource from)
        {
            String key;
            if (from is BNode)
            {
                key = (from as BNode).ToString();
                if (!map.ContainsKey(key))
                {
                    Guid guid = Guid.NewGuid();
                    Uri uri = new Uri(String.Format("urn:uuid:{0}", guid));
                    map.Add(key, KE.Factory.CreateEntity(uri));
                }
            }
            else if (from is Literal)
            {
                Literal l = from as Literal;
                return KE.Factory.CreateLiteral(l.Value, l.Language, l.DataType);
            }
            else
            {
                key = from.Uri;
                if (!map.ContainsKey(key))
                {
                    Uri uri = new Uri(from.Uri);
                    map.Add(key, KE.Factory.CreateEntity(uri));
                }
            }
            return map[key];
        }

        public Resource fromKE(IResource from)
        {
            return null;
        }
    }
}
