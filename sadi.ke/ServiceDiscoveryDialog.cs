using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using IOInformatics.KE.PluginAPI;
using System.Collections;

namespace SADI.KEPlugin
{
    public partial class ServiceDiscoveryDialog : Form
    {
        private KEStore KE;
        private IEnumerable<IResource> SelectedNodes;
        private Hashtable Seen;

        public ServiceDiscoveryDialog(KEStore ke, IEnumerable<IResource> selectedNodes)
        {
            KE = ke;
            SelectedNodes = new List<IResource>(selectedNodes);
            Seen = new Hashtable();
            InitializeComponent();
        }

        internal void FindServices()
        {
            FindServicesWorker.RunWorkerAsync();
            timer.Start();
        }

        private void FindServicesWorker_ProgressChanged(object sender, ProgressChangedEventArgs e)
        {
            progress.Value = e.ProgressPercentage;
            if (e.UserState is SADIService && !Seen.Contains((e.UserState as SADIService).uri))
            {
                Seen.Add((e.UserState as SADIService).uri, Seen);
                ServiceSelectionControl control = new ServiceSelectionControl();
                control.setService(e.UserState as SADIService);
                ResizeLabels(control);
                flowLayoutPanel1.SuspendLayout();
                int ypos = flowLayoutPanel1.VerticalScroll.Value;
                flowLayoutPanel1.Controls.Add(control);
                int i = 0;
                while ((i < flowLayoutPanel1.Controls.Count) &&
                    ((flowLayoutPanel1.Controls[i] as ServiceSelectionControl).service.name.CompareTo(control.service.name) < 0))
                {
                    ++i;
                }
                flowLayoutPanel1.Controls.SetChildIndex(control, i);
                flowLayoutPanel1.ResumeLayout(false);
                flowLayoutPanel1.PerformLayout();
                flowLayoutPanel1.VerticalScroll.Value = ypos;
            }
            else if (e.UserState is string)
            {
                timer.Stop();
                status.Text = e.UserState as string;
                timer.Start();
            }
            else if (e.UserState is Exception)
            {
                timer.Stop();
                status.Text = (e.UserState as Exception).Message;
                timer.Start();
            }
        }

        private void FindServicesWorker_RunWorkerCompleted(object sender, RunWorkerCompletedEventArgs e)
        {
            timer.Stop();
            // TODO fix this logically; actually kill the background worker when the window is closed...
            if (!progress.IsDisposed)
            {
                progress.Visible = false;
            }
            if (!status.IsDisposed)
            {
                status.Text = "Found " + this.flowLayoutPanel1.Controls.Count + " services.";
            }
        }

        private void invoke_Click(object sender, EventArgs e)
        {
            /*
            if (FindServicesWorker.IsBusy)
            {
                FindServicesWorker.CancelAsync();
            }
            */

            List<SADIService> services = GetSelectedServices(true);
            if (services.Count > 0)
            {
                ServiceInvocationDialog dialog = new ServiceInvocationDialog(KE, services, SelectedNodes);
                dialog.Show();
                dialog.invokeServices();
                //CleanUp();
            }
            else
            {
                MessageBox.Show(this,
                    "No services selected",
                    "Please select one or more services to invoke.",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Warning);
            }
        }

        private List<SADIService> GetSelectedServices(Boolean withClear)
        {
            List<SADIService> services = new List<SADIService>();
            foreach (Control control in flowLayoutPanel1.Controls)
            {
                if (control is ServiceSelectionControl)
                {
                    ServiceSelectionControl ssc = control as ServiceSelectionControl;
                    if (ssc.isSelected())
                    {
                        services.Add(ssc.service);
                        if (withClear)
                        {
                            ssc.deselect();
                        }
                    }
                }
            }
            return services;
        }

        private void cancel_Click(object sender, EventArgs e)
        {
            if (FindServicesWorker.IsBusy)
            {
                FindServicesWorker.CancelAsync();
            }
            //CleanUp();
        }

        private static char[] ELLIPSIS = { '.' };
        private void timer_Tick(object sender, EventArgs e)
        {
            if (status.Text.EndsWith("..."))
            {
                status.Text = status.Text.TrimEnd(ELLIPSIS);
            }
            else
            {
                status.Text += ".";
            }
        }

        private void ServiceSelectionDialog_ResizeEnd(object sender, EventArgs e)
        {
            foreach (Control control in this.flowLayoutPanel1.Controls)
            {
                if (control is ServiceSelectionControl)
                {
                    ResizeLabels(control as ServiceSelectionControl);
                }
            }
        }

        private void ResizeLabels(ServiceSelectionControl control)
        {
            Padding padding = this.flowLayoutPanel1.Padding;
            control.sizeLabels(this.flowLayoutPanel1.Size.Width - padding.Left - padding.Right);
        }

        private void ServiceSelectionDialog_FormClosing(object sender, FormClosingEventArgs e)
        {
            this.FindServicesWorker.RunWorkerCompleted -= this.FindServicesWorker_RunWorkerCompleted;
            this.FindServicesWorker.ProgressChanged -= this.FindServicesWorker_ProgressChanged;
            if (FindServicesWorker.IsBusy)
            {
                FindServicesWorker.CancelAsync();
            }
        }

        private void CleanUp()
        {
            this.Hide();
            this.Dispose();
        }

        // use from _DoWork like so: 
        // if (!ReportProgressIfNotCancelled(...)) { return; } 
        private Boolean ReportProgressIfNotCancelled(int progress, Object message)
        {
            if (FindServicesWorker.CancellationPending)
            {
                return false;
            }
            else
            {
                FindServicesWorker.ReportProgress(progress, message);
                return true;
            }
        }

        private const int SERVICES_PER_QUERY = 25;
        private void FindServicesWorker_DoWork(object sender, DoWorkEventArgs e)
        {
            List<String> types = new List<String>();
            foreach (IResource node in SelectedNodes)
            {
                if (node is IEntity)
                {
                    if (!KE.HasType(node))
                    {
                        Uri uri = (node as IEntity).Uri;
                        if (!ReportProgressIfNotCancelled(0, String.Format("Resolving {0}...", uri)))
                        {
                            return;
                        }
                        try
                        {
                            SADIHelper.debug("ServiceSelection", "resolving URI", node);
                            if (FindServicesWorker.CancellationPending)
                            {
                                return;
                            }
                            KE.Import(SemWebHelper.resolveURI(uri));
                        }
                        catch (Exception err)
                        {
                            SADIHelper.error("ServiceSelection", "error resolving URI", node, err);
                        }
                        try
                        {
                            SADIHelper.debug("ServiceSelection", "resolving against SADI resolver", node);
                            if (FindServicesWorker.CancellationPending)
                            {
                                return;
                            }
                            KE.Import(SADIHelper.resolve(uri));
                        }
                        catch (Exception err)
                        {
                            SADIHelper.error("ServiceSelection", "error resolving against SADI resolver", node, err);
                        }
                    }
                    foreach (IEntity type in KE.GetTypes(node as IEntity))
                    {
                        types.Add(type.Uri.ToString());
                    }
                }
            }

            // find services by exact input class; quick, but misses a lot...
            if (!ReportProgressIfNotCancelled(0, "Finding services by direct type..."))
            {
                return;
            }
            ICollection<SADIService> services = SADIRegistry.Instance().findServicesByInputClass(types);
            int i = 0;
            int n = services.Count;
            foreach (SADIService service in services)
            {
                SADIRegistry.Instance().addPropertyRestrictions(service);
                if (!ReportProgressIfNotCancelled((++i * 100) / n, service))
                {
                    return;
                }
            }

            // reset progress bar
            if (!ReportProgressIfNotCancelled(0, "Finding services by input instance query..."))
            {
                return;
            }

            // find service by input instance SPARQL query; slow, but is complete modulo reasoning...
            i = 0;
            n = SADIRegistry.Instance().getServiceCount();
            do
            {
                if (!ReportProgressIfNotCancelled((i * 100) / n, String.Format("Finding services by input instance query {0}-{1}/{2}",
                    i, Math.Min(i + SERVICES_PER_QUERY, n), n)))
                {
                    return;
                }
                services = SADIRegistry.Instance().getAllServices(i, SERVICES_PER_QUERY);
                foreach (SADIService service in services)
                {
                    //SADIHelper.debug("ServiceDiscoveryDialog", String.Format("checking {0}", service.uri), null);
                    Object rv = null;
                    try
                    {
                        if (checkForInputInstances(service, SelectedNodes))
                        {
                            SADIRegistry.Instance().addPropertyRestrictions(service);
                            rv = service;
                        }
                        else
                        {
                            rv = String.Format("No match to {0}", service.uri);
                        }
                    }
                    catch (Exception ex)
                    {
                        //rv = ex;
                        rv = String.Format("Error executing input instance query for {0}: {1}", service.uri, ex.Message);
                    }
                    if (!ReportProgressIfNotCancelled((++i * 100) / n, rv))
                    {
                        return;
                    }
                }
            } while (services.Count == SERVICES_PER_QUERY);
        }

        private bool checkForInputInstances(SADIService service, IEnumerable<IResource> selectedNodes)
        {
            if (service.inputInstanceQuery != null)
            {
                string query = SADIHelper.convertConstructQuery(service.inputInstanceQuery);
                if (query != null)
                {
                    foreach (ISPARQLResult result in KE.Graph.Query(query).Results)
                    {
                        foreach (IResource node in selectedNodes)
                        {
                            if (node.Equals(result["input"]))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }
}
