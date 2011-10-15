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

namespace SADI.KEPlugin
{
    class SADIHelper
    {
        private const string SERVLET_URI = "http://biordf.net/sadi/ke";
        //private const string SERVLET_URI = "http://128.189.253.111:8080/sadi.ke/ke";
        //private const string SERVLET_URI = "http://wilkinsondev.elmonline.ca/sadi-ke/ke";
        
        public static string buildUri(Dictionary<string, string> args)
        {
            StringBuilder sb = new StringBuilder();
            foreach (KeyValuePair<string, string> kvp in args)
            {
                if (sb.Length > 0)
                    sb.Append("&");
                sb.Append(kvp.Key);
                sb.Append("=");
                sb.Append(Uri.EscapeDataString(kvp.Value));
            }
            sb.Insert(0, "?");
            sb.Insert(0, SERVLET_URI);
            return sb.ToString();
        }

        public static JsonData fetchUri(string uri)
        {
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[4096];
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(uri);
            HttpWebResponse response = (HttpWebResponse)request.GetResponse();
            Stream stream = response.GetResponseStream();
            int count;
            do
            {
                count = stream.Read(buf, 0, buf.Length);
                if (count != 0)
                {
                    sb.Append(Encoding.ASCII.GetString(buf, 0, count));
                }
            } while (count > 0);
            return JsonMapper.ToObject(sb.ToString());
        }

        public static JsonData fetchUriAsync(string uri)
        {
            while (true)
            {
                JsonData result = fetchUri(uri);
                if (result.IsObject && result["taskId"] != null) {
                    uri = SADIHelper.buildUri(new Dictionary<string, string>() { { "poll", (string)result["taskId"] } });
                    Thread.Sleep(2000);
                } else {
                    return result;
                }
            }
        }

        public static string buildArgumentList(IEnumerable<IResource> entities)
        {
            StringBuilder sb = new StringBuilder();
            foreach (IResource entity in entities)
            {
                if (sb.Length > 0)
                    sb.Append(" ");
                sb.Append((entity as IEntity).Uri);
            }
            return sb.ToString();
        }

        public static string buildArgumentList(IEnumerable items)
        {
            StringBuilder sb = new StringBuilder();
            foreach (ListViewItem item in items)
            {
                string predicate = item.Tag as string;
                if (sb.Length > 0)
                    sb.Append(" ");
                sb.Append(predicate);
            }
            return sb.ToString();
        }

        internal static void debug(String prefix, Object arg)
        {
            String s;
            if (arg is IEnumerable)
            {
                StringBuilder buf = new StringBuilder();
                foreach (Object o in arg as IEnumerable)
                {
                    if (buf.Length > 0)
                    {
                        buf.Append("\n");
                    }
                    buf.Append(o);
                }
                s = buf.ToString();
            }
            else if (arg is string)
            {
                s = arg as string;
            }
            else
            {
                s = arg.ToString();
            }
            Console.Out.WriteLine(prefix + "\n" + s);
        }
    }
}
