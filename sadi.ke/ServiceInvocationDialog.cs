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
using System.Threading;

namespace SADI.KEPlugin
{
    public partial class ServiceInvocationDialog : Form
    {
        private KEStore KE;
        private IEnumerable<SADIService> Services;
        private IEnumerable<IResource> SelectedNodes;

        public ServiceInvocationDialog(KEStore ke, ICollection<SADIService> services, IEnumerable<IResource> selectedNodes)
        {
            KE = ke;
            Services = services;
            SelectedNodes = selectedNodes;
            InitializeComponent();
        }

        internal void invokeServices()
        {
            // populate the DataGridView
            List<ServiceCallStatus> rows = new List<ServiceCallStatus>();
            foreach (SADIService service in Services)
            {
                int i = dataGridView1.Rows.Add(new string[] {"Waiting", service.name, service.uri});
                dataGridView1.Rows[i].Tag = new StringBuilder();
                rows.Add(new ServiceCallStatus(i, service));
            }
            MasterWorker.RunWorkerAsync(rows);
        }

        private void dataGridView1_CellClick(object sender, DataGridViewCellEventArgs e)
        {
            Object tag = dataGridView1.Rows[e.RowIndex].Tag;
            if (tag != null)
            {
                textBox1.Text = tag.ToString();
            }
        }

        private void MasterWorker_ProgressChanged(object sender, ProgressChangedEventArgs e)
        {
            ServiceCallStatus arg = e.UserState as ServiceCallStatus;
            DataGridViewRow row = dataGridView1.Rows[arg.Row];
            row.Cells[0].Value = arg.Status;
            if (arg.Data is Exception)
            {
                addToTag(row, (arg.Data as Exception).Message);
            }
            else if (arg.Data is string)
            {
                addToTag(row, arg.Data as string);
            }
        }

        const int MAX_WORKERS = 10;
        private static int NumWorkers = 0;
        private void MasterWorker_DoWork(object sender, DoWorkEventArgs e)
        {
            List<ServiceCallStatus> rows = e.Argument as List<ServiceCallStatus>;
            int i=0;
            while (i<rows.Count)
            {
                if (NumWorkers < MAX_WORKERS)
                {
                    Interlocked.Increment(ref NumWorkers);
                    SADIHelper.debug("ServiceInvocation", "queueing worker " + NumWorkers, rows[i].Data);
                    ThreadPool.QueueUserWorkItem(InvokeServicesWorker_DoWork, rows[i++]);
                }
                else
                {
                    SADIHelper.debug("ServiceInvocation", "waiting for some service calls to finish before spawning more than " + MAX_WORKERS, null);
                    Thread.Sleep(30000);
                }
            }
            while (NumWorkers > 0)
            {
                SADIHelper.trace("ServiceInvocation", "waiting for " + NumWorkers + " service calls to finish", null);
                Thread.Sleep(1000);
            }
        }

        private void InvokeServicesWorker_DoWork(object threadContext)
        {
            ServiceCallStatus call = threadContext as ServiceCallStatus;
            SADIService service = call.Data as SADIService;
            try
            {
                call.Status = "Assembling input";
                MasterWorker.ReportProgress(1, call);
                MemoryStore input = assembleInput(SelectedNodes, service);

                call.Status = "Calling service";
                call.Data = "Assembled input:\r\n" + SemWebHelper.storeToString(input);
                MasterWorker.ReportProgress(33, call);
                Store output = service.invokeService(input);

                call.Status = "Storing output";
                call.Data = "Received output:\r\n" + SemWebHelper.storeToString(output);
                MasterWorker.ReportProgress(66, call);
                updateKE(output);

                call.Status = "Done";
                call.Data = service;
                MasterWorker.ReportProgress(100, call);
            }
            catch (Exception err)
            {
                SADIHelper.error("ServiceCall", "error calling service", service, err);
                call.Status = "Error";
                call.Data = service;
                MasterWorker.ReportProgress(100, call);
            }
            finally
            {
                Interlocked.Decrement(ref NumWorkers);
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

        private static readonly Regex LSRN_pattern = new Regex("http://lsrn.org/([^:]+):([^?]+)");
        private static readonly string RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        private static readonly string LSRN = "http://purl.oclc.org/SADI/LSRN/";
        private static readonly string SIO = "http://semanticscience.org/resource/";
        private static readonly Entity rdf_type = RDF + "type";
        private static readonly Entity has_identifier = SIO + "SIO_000671";
        private static readonly Entity has_value = SIO + "SIO_000300";
        private void assembleInput(MemoryStore input, IEntity node, string inputClass)
        {
            String uri = node.Uri.ToString();
            Entity root = new Entity(uri);
            if (KE.HasType(node, inputClass))
            {
                input.Add(new Statement(root, rdf_type, new Entity(inputClass)));
            }
            Match match = LSRN_pattern.Match(uri);
            if (match != null)
            {
                String lsrn_db = match.Groups[1].ToString();
                String lsrn_id = match.Groups[2].ToString();
                String type_uri = LSRN + lsrn_db + "_Record";
                if (type_uri != inputClass)
                {
                    input.Add(new Statement(root, rdf_type, new Entity(type_uri)));
                }
                Entity identifier = new BNode();
                Entity identifier_type = new Entity(LSRN + lsrn_db + "_Identifier");
                input.Add(new Statement(root, has_identifier, identifier));
                input.Add(new Statement(identifier, rdf_type, identifier_type));
                input.Add(new Statement(identifier, has_value, new Literal(lsrn_id)));
            }
        }

        private void updateKE(Store output)
        {
            KEMapper mapper = KE.GetMapper(output);
            ICollection<IStatement> statements = new List<IStatement>();
            foreach (Statement statement in output.Select(SelectFilter.All))
            {
                statements.Add(KE.Factory.CreateStatement(
                        mapper.toKE(statement.Subject),
                        mapper.toKE(statement.Predicate),
                        mapper.toKE(statement.Object)));
            }
            KE.Graph.BeginUpdate();
            KE.Graph.Add(statements);
            KE.Graph.EndUpdate();
            foreach (IStatement statement in statements)
            {
                KE.VisibilityManager.ShowStatement(statement);
            }
        }

        private void addToTag(DataGridViewRow row, object o)
        {
            if (row.Tag == null)
            {
                row.Tag = new StringBuilder();
            }
            StringBuilder buf = (row.Tag as StringBuilder);
            if (buf.Length > 0)
            {
                buf.Append("\r\n");
            }
            buf.Append(o.ToString());
        }

        private class ServiceCallStatus
        {
            public int Row;
            public string Status;
            public Object Data;

            public ServiceCallStatus(int row, Object data)
            {
                Row = row;
                Data = data;
                Status = "";
            }
        }
    }
}
