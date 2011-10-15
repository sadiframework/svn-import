using System;
using System.Collections.Generic;
using System.Text;
using IOInformatics.KE.PluginAPI;

namespace SADI.KEPlugin
{
    class KEHelper
    {
        private const string KE_URI_PREFIX = "http://io-informatics.com/rdf/";
        private const string MOBY_URI_PREFIX = "http://biordf.net/moby/";

        private static string[] getElements(String s)
        {
            if (s.StartsWith(KE_URI_PREFIX))
                return s.Substring(KE_URI_PREFIX.Length).Split(new char[] { '/', '#' });
            else if (s.StartsWith(MOBY_URI_PREFIX))
                return s.Substring(MOBY_URI_PREFIX.Length).Split(new char[] { '/', '#' });
            else
                return new string[0];
        }

        public static string getClass(IResource entity)
        {
            // TODO get the right way to do this from Erich
            if (!(entity is IEntity))
                return "Literal";
            string[] elements = getElements((entity as IEntity).Uri.ToString());
            return elements.Length >= 1 ? elements[0].Replace("%20", " ") : "Unknown";
        }

        public static string getName(IResource entity)
        {
            // TODO get the right way to do this from Erich
            if (!(entity is IEntity))
                return entity.ToString();
            string[] elements = getElements((entity as IEntity).Uri.ToString());
            return elements.Length >= 2 ? elements[1].Replace("%20", " ") : "Unknown";
        }
    }
}
