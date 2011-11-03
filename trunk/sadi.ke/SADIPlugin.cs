using System;
using System.Collections.Generic;
using System.Text;
using System.Windows.Forms;
using System.Reflection;
using System.IO;

using IOInformatics.KE.PluginAPI;
using IOInformatics.KE.PluginAPI.Providers;
using LitJson;
using System.Diagnostics;

namespace SADI.KEPlugin
{
    public class SADIPlugin : IPlugIn
    {
        private IPlugInInfo PluginInfo = new SADIPluginInfo();
        private KEStore KE;
        private ISelectionProvider SelectionProvider;

        private ToolStripMenuItem callServicesItem;

        public void Register(IServiceProvider serviceProvider)
        {
            ITripleStoreProvider tripleStoreProvider = (ITripleStoreProvider)serviceProvider.GetService(typeof(ITripleStoreProvider));
            ITripleStoreFactory tripleStoreFactory = ((IFactoryProvider)serviceProvider.GetService(typeof(IFactoryProvider))).Factory;
            IVisibilityManager visibilityManager = ((IVisibilityManager)serviceProvider.GetService(typeof(IVisibilityManager)));
            KE = new KEStore(tripleStoreProvider, tripleStoreFactory, visibilityManager);
            SelectionProvider = (ISelectionProvider)serviceProvider.GetService(typeof(ISelectionProvider));
                        
            callServicesItem = new ToolStripMenuItem("Find SADI services...");
            callServicesItem.Click += new EventHandler(callServicesItem_Click);

            IContextMenuProvider contextMenuProvider = (IContextMenuProvider)serviceProvider.GetService(typeof(IContextMenuProvider));
            contextMenuProvider.ContextMenu.Opening += new System.ComponentModel.CancelEventHandler(ContextMenu_Opening);
            contextMenuProvider.ContextMenu.Items.Add(new ToolStripSeparator());
            contextMenuProvider.ContextMenu.Items.Add(callServicesItem);

            try
            {
                string logFolder = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                Trace.Listeners.Add(new TextWriterTraceListener(System.IO.File.AppendText(logFolder + "\\SADI.KE.log")));
                Debug.WriteLine("new logging session at " + DateTime.Now);
                Debug.Flush();
            }
            catch (Exception e)
            {
                MessageBox.Show(
                    "Unable to configure logging;\n" + e.Message, 
                    "SADI error", 
                    MessageBoxButtons.OK, 
                    MessageBoxIcon.Warning);
            }
        }

        //void _showSelectedItemsInfo_Click( object sender, EventArgs e )
        //{
        //    SelecedItemsInfoForm form = new SelecedItemsInfoForm();

        //    form.FillInfo( _selectionProvider.SelectedItems );

        //    form.ShowDialog();
        //}

        //void _selectAll_Click( object sender, EventArgs e )
        //{
        //    foreach ( IStatement statement in _internalStatementList )
        //        _selectionProvider.Select( statement.Subject, true );
        //}

        //void _expandSelectedItem_Click( object sender, EventArgs e )
        //{
        //    ExpandSelectedItemForm form = new ExpandSelectedItemForm(this);
        //    form.FillInfo(new List<IResource>(_selectionProvider.SelectedItems));
        //    form.Show();
        //}

        void callServicesItem_Click(object sender, EventArgs e)
        {
            ServiceSelectionDialog dialog = new ServiceSelectionDialog(KE, SelectionProvider.SelectedItems);
            dialog.Show();
            dialog.FindServices();
        }

        void ContextMenu_Opening(object sender, System.ComponentModel.CancelEventArgs e)
        {
            callServicesItem.Enabled = SelectionProvider.NumberOfSelectedItems > 0;
        }

        public void Unregister()
        {
        }


        public SADIPlugin()
        {
        }

        #region IPlugin Members

        public IPlugInInfo Info
        {
            get { return PluginInfo; }
        }

        #endregion

        private class SADIPluginInfo : IPlugInInfo
        {
            #region IAddInInfo Members

            public IEnumerable<string> Dependencies
            {
                get { return new List<String>(); }
            }

            public string Name
            {
                get { return "SADI Plugin"; }
            }

            public string Description
            {
                get { return "An interface to the Semantic Automated Discovery and Integration (SADI) framework"; }
            }

            #endregion
        }

    }
}
