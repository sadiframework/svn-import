using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace SADI.KEPlugin
{
    class LSRNHelper
    {
        public static readonly Regex LSRN_PATTERN = new Regex("http://lsrn.org/([^:]+):([^?]+)");

        public static LSRNEntity getEntity(Uri uri)
        {
            return getEntity(uri.ToString());
        }

        public static LSRNEntity getEntity(string uri)
        {
            Match match = LSRN_PATTERN.Match(uri);
            if (match != null)
            {
                return new LSRNEntity(match.Groups[1].ToString(), match.Groups[2].ToString());
            }
            else
            {
                return null;
            }
        }
    }

    class LSRNEntity
    {
        public string DB, ID;
        public string EntityType
        {
            get { return SemWebVocab.LSRN + DB + "_Record"; }
        }
        public string IdentifierType
        {
            get { return SemWebVocab.LSRN + DB + "_Identifier"; }
        }

        public LSRNEntity(string db, string id)
        {
            DB = db;
            ID = id;
        }
    }
}
