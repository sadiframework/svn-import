using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;
using SemWeb;

namespace SADI.KEPlugin
{
    public class KEMapper
    {
        private readonly KEStore _ke;
        private readonly Dictionary<string, IEntity> _toKE;
        private readonly Dictionary<string, Entity> _fromKE;

        public KEMapper(KEStore ke)
        {
            _ke = ke;
            _toKE = new Dictionary<string, IEntity>();
            _fromKE = new Dictionary<string, Entity>();
        }

        public IStatement toKE(Statement statement)
        {
            return _ke.Factory.CreateStatement(
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
                if (!_toKE.ContainsKey(key))
                {
                    Guid guid = Guid.NewGuid();
                    Uri uri = new Uri(String.Format("urn:uuid:{0}", guid));
                    _toKE.Add(key, _ke.Factory.CreateEntity(uri));
                }
            }
            else if (from is Literal)
            {
                Literal l = from as Literal;
                return _ke.Factory.CreateLiteral(l.Value, l.Language, l.DataType);
            }
            else
            {
                key = from.Uri;
                if (!_toKE.ContainsKey(key))
                {
                    Uri uri = new Uri(from.Uri);
                    _toKE.Add(key, _ke.Factory.CreateEntity(uri));
                }
            }
            return _toKE[key];
        }

        public Statement fromKE(IStatement statement)
        {
            return new Statement(
                        fromKE(statement.Subject) as Entity,
                        fromKE(statement.Predicate) as Entity,
                        fromKE(statement.Object));
        }

        public Resource fromKE(IResource to)
        {
            String key;
            if (to is ILiteral)
            {
                ILiteral l = to as ILiteral;
                return new Literal(l.Value, l.Language.Equals("") ? null : l.Language, l.DataType.Equals("") ? null : l.DataType);
            }
            else
            {
                key = to.ToString();
                if (!_fromKE.ContainsKey(key))
                {
                    if (to is IEntity && (to as IEntity).Uri != null)
                    {
                        _fromKE.Add(key, new Entity((to as IEntity).Uri.ToString()));
                    }
                    else
                    {
                        _fromKE.Add(key, new BNode(key.StartsWith("_:") ? key.Substring(2) : key));
                    }
                }
            }
            return _fromKE[key];
        }
    }
}
