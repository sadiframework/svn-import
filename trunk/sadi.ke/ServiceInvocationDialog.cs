using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using IOInformatics.KE.PluginAPI;
using SemWeb;
using System.IO;
using System.Text.RegularExpressions;

namespace SADI.KEPlugin
{
    public partial class ServiceInvocationDialog : Form
    {
        private IPluginGraph graph;
        private ITripleStoreFactory factory;
        private IEnumerable<SADIService> services;
        private IEnumerable<IResource> selectedNodes;

        public ServiceInvocationDialog(IPluginGraph graph, ITripleStoreFactory factory)
        {
            this.graph = graph;
            this.factory = factory;
            InitializeComponent();
        }

        private void dataGridView1_CellClick(object sender, DataGridViewCellEventArgs e)
        {
            Object tag = dataGridView1.Rows[e.RowIndex].Tag;
            if (tag is StringBuilder)
            {
                textBox1.Text = tag.ToString();
            }
        }

        internal void invokeServices(List<SADIService> services, IEnumerable<IOInformatics.KE.PluginAPI.IResource> selectedNodes)
        {
            this.services = services;
            this.selectedNodes = selectedNodes;

            // populate the DataGridView
            foreach (SADIService service in services)
            {
                int i = dataGridView1.Rows.Add(new string[] { "Preparing", service.name, service.uri });
                MasterWorker.RunWorkerAsync(new RowObjectPair(i, service));
            }
        }

        private void InvokeServicesWorker_DoWork(object sender, DoWorkEventArgs e)
        {
            RowObjectPair arg = e.Argument as RowObjectPair;
            SADIService service = arg.data as SADIService;
            try
            {
                MemoryStore input = assembleInput(selectedNodes, service);
                arg.data = SemWebHelper.storeToString(input);
                MasterWorker.ReportProgress(50, arg);
                /*
                Store output = service.invokeService(input);
                arg.data = SADIHelper.storeToString(output);
                SADIHelper.debug("output:", arg.data);
                updateKE(output);
                arg.data = SADIHelper.storeToString(output);
                InvokeServicesWorker.ReportProgress(100, arg);
                 */
            }
            catch (Exception err)
            {
                arg.data = err;
                MasterWorker.ReportProgress(100, arg);
            }
        }

        private MemoryStore assembleInput(IEnumerable<IResource> selectedNodes, SADIService service)
        {
            MemoryStore input = new MemoryStore();
            foreach (IResource node in selectedNodes)
            {
                if (node is IEntity)
                {
                    assembleInput(input, node as IEntity, service.inputClass);
                }
            }
            return input;
        }

        private static readonly Regex LSRN_pattern = new Regex(@"http://lsrn.org/([^:]+):([^?]+)");
        private static readonly string RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        private static readonly string LSRN = "http://purl.oclc.org/SADI/LSRN/";
        private static readonly string SIO = "http://semanticscience.org/resource/";
        private static readonly Entity rdf_type = RDF + "type";
        private static readonly Entity has_identifier = SIO + "SIO_000671";
        private static readonly Entity has_value = SIO + "SIO_000300";
        private static void assembleInput(MemoryStore input, IEntity node, string inputClass)
        {
            String uri = node.Uri.ToString();
            Match match = LSRN_pattern.Match(uri);
            if (match != null)
            {
                String lsrn_db = match.Groups[1].ToString();
                String lsrn_id = match.Groups[2].ToString();
                String type_uri = LSRN + lsrn_db + "_Record";
                if (type_uri == inputClass)
                {
                    Entity root = new Entity(uri);
                    Entity root_type = new Entity(LSRN + lsrn_db + "_Record");
                    Entity identifier = new BNode();
                    Entity identifier_type = new Entity(LSRN + lsrn_db + "_Identifier");
                    input.Add(new Statement(root, rdf_type, root_type));
                    input.Add(new Statement(root, has_identifier, identifier));
                    input.Add(new Statement(identifier, rdf_type, identifier_type));
                    input.Add(new Statement(identifier, has_value, new Literal(lsrn_id)));
                }
            }
        }

        private void updateKE(Store output)
        {
            KEMapper mapper = new KEMapper(graph, factory);
            graph.BeginUpdate();
            foreach (Statement statement in output.Select(SelectFilter.All))
            {
                IResource o;
                if (statement.Object is Literal)
                {
                    o = factory.CreateLiteral(statement.Object.ToString());
                } else {
                    o = mapper.toKE(statement.Object as Entity);
                }
                graph.Add(factory.CreateStatement(
                    mapper.toKE(statement.Subject),
                    mapper.toKE(statement.Predicate),
                    o));
            }
            graph.EndUpdate();
        }

        private void InvokeServicesWorker_ProgressChanged(object sender, ProgressChangedEventArgs e)
        {
            RowObjectPair arg = e.UserState as RowObjectPair;
            DataGridViewRow row = dataGridView1.Rows[arg.row];
            if (e.ProgressPercentage == 50)
            {
                row.Cells[0].Value = "Invoking";
                addToTag(row, "Assembled input:\r\n" + arg.data);
            }
            else if (e.ProgressPercentage == 100)
            {
                if (arg.data is Exception)
                {
                    row.Cells[0].Value = "Error";
                    addToTag(row, (arg.data as Exception).StackTrace.ToString());
                }
                else
                {
                    row.Cells[0].Value = "Done";
                    addToTag(row, "Received output:\r\n" + arg.data);
                }
            }
        }

        private void addToTag(DataGridViewRow row, object o)
        {
            if (row.Tag == null)
            {
                row.Tag = new StringBuilder();
            }
            (row.Tag as StringBuilder).Append(o.ToString());
            SADIHelper.debug("addToTag " + row + ":", o);
        }

        private class KEMapper
        {
            private IPluginGraph graph;
            private ITripleStoreFactory factory;
            private Dictionary<string, IEntity> map;

            public KEMapper(IPluginGraph graph, ITripleStoreFactory factory)
            {
                this.graph = graph;
                this.factory = factory;
                map = new Dictionary<string, IEntity>();
            }
            
            public IEntity toKE(Resource from)
            {
                String key;
                if (from is BNode)
                {
                    key = (from as BNode).LocalName;
                    if (!map.ContainsKey(key))
                    {
                        Guid guid = Guid.NewGuid();
                        Uri uri = new Uri(String.Format("urn:uuid:{0}", guid));
                        map.Add(key, factory.CreateEntity(uri));
                    }
                }
                else
                {
                    key = from.Uri;
                    if (!map.ContainsKey(key))
                    {
                        Uri uri = new Uri(from.Uri);
                        map.Add(key, factory.CreateEntity(uri));
                    }
                }
                return map[key];
            }
        }

        private class RowObjectPair
        {
            public int row { get; set; }
            public Object data { get; set; }

            public RowObjectPair(int row, Object data)
            {
                this.row = row;
                this.data = data;
            }
        }
    }
}
