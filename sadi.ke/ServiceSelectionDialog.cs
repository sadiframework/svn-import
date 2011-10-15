using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using IOInformatics.KE.PluginAPI;

namespace SADI.KEPlugin
{
    public partial class ServiceSelectionDialog : Form
    {
        private KEStore KEStore;
        private IEnumerable<IResource> SelectedNodes;

        public ServiceSelectionDialog(KEStore store, IEnumerable<IResource> selectedNodes)
        {
            KEStore = store;
            SelectedNodes = selectedNodes;
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
            if (e.UserState is SADIService)
            {
                ServiceSelectionControl control = new ServiceSelectionControl();
                control.setService(e.UserState as SADIService);
                ResizeLabels(control);
                flowLayoutPanel1.SuspendLayout();
                flowLayoutPanel1.Controls.Add(control);
                flowLayoutPanel1.ResumeLayout(false);
                flowLayoutPanel1.PerformLayout();
            }
            else if (e.UserState is string)
            {
                timer.Stop();
                status.Text = e.UserState as string;
                timer.Start();
            }
        }

        private void FindServicesWorker_RunWorkerCompleted(object sender, RunWorkerCompletedEventArgs e)
        {
            timer.Stop();
            progress.Visible = false;
            status.Text = "Found " + this.flowLayoutPanel1.Controls.Count + " services.";
        }

        private void invoke_Click(object sender, EventArgs e)
        {
            if (FindServicesWorker.IsBusy)
            {
                FindServicesWorker.CancelAsync();
            }

            List<SADIService> services = GetSelectedServices();
            if (services.Count > 0)
            {
                ServiceInvocationDialog dialog = new ServiceInvocationDialog(KEStore.Graph, KEStore.Factory);
                dialog.Show();
                dialog.invokeServices(services, SelectedNodes);
                CleanUp();
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

        private List<SADIService> GetSelectedServices()
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
            CleanUp();
        }

        private static char[] ELLIPSIS = {'.'};
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
            control.sizeLabels(this.Size.Width);
        }

        private void ServiceSelectionDialog_FormClosing(object sender, FormClosingEventArgs e)
        {
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

        private const int SERVICES_PER_QUERY = 25;
        private void FindServicesWorker_DoWork(object sender, DoWorkEventArgs e)
        {
            List<String> types = new List<String>();
            foreach (IResource node in SelectedNodes)
            {
                if (node is IEntity)
                {
                    if (FindServicesWorker.CancellationPending)
                    {
                        return;
                    }
                    else if (!KEStore.HasType(node))
                    {
                        try
                        {
                            KEStore.Import(SemWebHelper.resolveURI((node as IEntity).Uri));
                        }
                        catch (Exception err)
                        {
                            // FIXME set up logging...
                            Console.WriteLine(err.StackTrace);
                        }
                    }

                    foreach (IEntity type in KEStore.GetTypes(node as IEntity))
                    {
                        types.Add(type.Uri.ToString());
                    }
                }
            }

            //ICollection<SADIService> services = new List<SADIService>();
            //services.Add(new SADIService("http://sadiframework.org/examples/blast/human-blast", "NCBI BLAST (human)", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras malesuada, dui eu tempus adipiscing, mauris sapien ultrices lectus, sit amet lobortis leo ipsum id lacus. Morbi porta, mi sit amet pulvinar adipiscing, leo tellus laoreet justo, et suscipit odio nisl nec velit. Pellentesque vel urna risus. Cras magna massa, volutpat at iaculis a, tincidunt et erat. Morbi ullamcorper feugiat augue, at pharetra nulla egestas at. Nunc ac orci ut erat porttitor auctor quis sit amet ipsum. Curabitur bibendum tortor quis libero adipiscing ultricies. Ut tempor rhoncus luctus."));
            //services.Add(new SADIService("http://sadiframework.org/examples/blast/mouse-blast", "NCBI BLAST (mouse)", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras malesuada, dui eu tempus adipiscing, mauris sapien ultrices lectus, sit amet lobortis leo ipsum id lacus. Morbi porta, mi sit amet pulvinar adipiscing, leo tellus laoreet justo, et suscipit odio nisl nec velit. Pellentesque vel urna risus. Cras magna massa, volutpat at iaculis a, tincidunt et erat. Morbi ullamcorper feugiat augue, at pharetra nulla egestas at. Nunc ac orci ut erat porttitor auctor quis sit amet ipsum. Curabitur bibendum tortor quis libero adipiscing ultricies. Ut tempor rhoncus luctus."));

            // find services by exact input class; quick, but misses a lot...
            FindServicesWorker.ReportProgress(0, "Finding services by direct type...");
            ICollection<SADIService> services = SADIRegistry.Instance().findServicesByInputClass(types);
            int i = 0, n = services.Count;
            foreach (SADIService service in services)
            {
                if (FindServicesWorker.CancellationPending)
                {
                    return;
                }
                else
                {
                    SADIRegistry.Instance().addPropertyRestrictions(service);
                    FindServicesWorker.ReportProgress(++i * 100 / n, service);
                }
            }

            // reset progress bar
            FindServicesWorker.ReportProgress(0, "Finding services by input instance query...");

            // find service by input instance SPARQL query; slow, but is complete modulo reasoning...
            i = 0; n = SADIRegistry.Instance().getServiceCount();
            do
            {
                if (FindServicesWorker.CancellationPending)
                {
                    return;
                }
                else
                {
                    services = SADIRegistry.Instance().getAllServices(i, SERVICES_PER_QUERY);
                    foreach (SADIService service in services)
                    {
                        if (FindServicesWorker.CancellationPending)
                        {
                            return;
                        }
                        else if (checkForInputInstances(service, SelectedNodes))
                        {
                            SADIRegistry.Instance().addPropertyRestrictions(service);
                            FindServicesWorker.ReportProgress(++i * 100 / n, service);
                        }
                    }
                }
            } while (services.Count == SERVICES_PER_QUERY);
        }

        private bool checkForInputInstances(SADIService service, IEnumerable<IResource> selectedNodes)
        {
            return false;
        }
    }
}
