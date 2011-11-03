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
        private DataGridViewRow SelectedRow;

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

        private void dataGridView1_SelectionChanged(object sender, EventArgs e)
        {
            SelectedRow = dataGridView1.SelectedRows[0];
            updateText();
        }

        private void updateText()
        {
            if (SelectedRow != null)
            {
                Object tag = SelectedRow.Tag;
                if (tag != null)
                {
                    textBox1.Text = tag.ToString();
                }
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

        private void addToTag(DataGridViewRow row, object o)
        {
            if (row.Tag == null)
            {
                row.Tag = new StringBuilder();
            }
            StringBuilder buf = (row.Tag as StringBuilder);
            if (buf.Length > 0)
            {
                buf.Append(Environment.NewLine);
            }
            buf.Append(o.ToString());
            if (row == SelectedRow)
            {
                updateText();
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
                ICollection<IStatement> statements = KE.Import(output);
                showNewStatements(statements);


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
                    service.assembleInput(input, node as IEntity, KE);
                }
            }
            return input;
        }

        private void showNewStatements(ICollection<IStatement> statements)
        {
            foreach (IStatement statement in statements)
            {
                foreach (IResource input in SelectedNodes)
                {
                    if (statement.Subject.Equals(input) &&
                        !isBNode(statement.Object) &&
                        !isTypeStatement(statement))
                    {
                        KE.VisibilityManager.ShowStatement(statement);
                    }
                }
                if (statement.Object is ILiteral)
                {
                    KE.VisibilityManager.ShowResource(statement.Object);
                }
            }
        }

        private bool isBNode(IResource resource)
        {
            if (resource is IEntity)
            {
                Uri uri = (resource as IEntity).Uri;
                return uri != null && uri.Scheme == "urn";
            }
            else
            {
                return false;
            }
        }

        private bool isTypeStatement(IStatement statement)
        {
            if (statement.Predicate is IEntity)
            {
                Uri uri = (statement.Predicate as IEntity).Uri;
                return uri == SemWebVocab.rdf_type_uri;
            }
            else
            {
                return false;
            }

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
