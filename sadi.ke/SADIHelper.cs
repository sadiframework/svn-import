using System;
using System.Collections.Generic;
using System.Text;
using System.Net;
using System.IO;
using LitJson;
using IOInformatics.KE.PluginAPI;
using System.Windows.Forms;
using System.Collections;
using System.Threading;
using SemWeb;
using System.Diagnostics;
using System.Collections.Specialized;
using System.Text.RegularExpressions;

namespace SADI.KEPlugin
{
    class SADIHelper
    {
        internal static void debug(String prefix, String message, Object arg)
        {
            Debug.WriteLine(message, prefix);
            if (arg != null)
            {
                Debug.Indent();
                if (arg is string)
                {
                    Debug.WriteLine(arg);
                }
                else if (arg is Store)
                {
                    Debug.WriteLine(SemWebHelper.storeToString(arg as Store));
                }
                else if (arg is IEnumerable)
                {
                    foreach (Object o in arg as IEnumerable)
                    {
                        Debug.WriteLine(o.ToString());
                    }
                }
                else if (arg != null)
                {
                    Debug.WriteLine(arg.ToString());
                }
                Debug.Unindent();
            }
            Debug.Flush();
        }

        internal static void trace(String prefix, String message, Object arg)
        {
            Trace.WriteLine(message, prefix);
            if (arg != null)
            {
                Trace.Indent();
                if (arg is string)
                {
                    Trace.WriteLine(arg);
                }
                else if (arg is Store)
                {
                    Trace.WriteLine(SemWebHelper.storeToString(arg as Store));
                }
                else if (arg is IEnumerable)
                {
                    foreach (Object o in arg as IEnumerable)
                    {
                        Trace.WriteLine(o.ToString());
                    }
                }
                else if (arg != null)
                {
                    Trace.WriteLine(arg.ToString());
                }
                Trace.Unindent();
            }
            Trace.Flush();
        }

        internal static void error(String prefix, String message, Object arg, Exception e)
        {
            Debug.WriteLine(message, prefix);
            if (arg != null)
            {
                Debug.Indent();
                if (arg is string)
                {
                    Debug.WriteLine(arg);
                }
                else if (arg is IEnumerable)
                {
                    foreach (Object o in arg as IEnumerable)
                    {
                        Debug.WriteLine(o.ToString());
                    }
                }
                else
                {
                    Debug.WriteLine(arg.ToString());
                }
                Debug.Write("error: ");
                Debug.WriteLine(e.Message);
                Debug.Write("source: ");
                Debug.WriteLine(e.Source);
                Debug.WriteLine(e.StackTrace);
                Debug.Unindent();
            }
            Debug.Flush();
        }

        internal static Store resolve(Uri uri)
        {
            return resolve(uri.ToString());
        }

        internal static Store resolve(string uri)
        {
            return resolve(new Entity(uri));
        }

        internal static Store resolve(Entity entity)
        {
            Store input = new MemoryStore();
            input.Add(new Statement(entity, SemWebVocab.rdf_type, SemWebVocab.owl_thing));
            return new SADIService("http://sadiframework.org/resolver").invokeService(input);
        }

        private static readonly Regex CONSTRUCT_QUERY_PATTERN = new Regex("CONSTRUCT\\s+\\{(.*)\\}\\s+WHERE\\s+\\{(.*)\\}", RegexOptions.Singleline);
        
        internal static string convertConstructQuery(string constructQuery)
        {
            Match match = CONSTRUCT_QUERY_PATTERN.Match(constructQuery);
            if (match.Success)
            {
                return String.Format("SELECT * WHERE {{ {0} }}", match.Groups[2]);
            }
            else
            {
                return null;
            }
        }
    }
}
