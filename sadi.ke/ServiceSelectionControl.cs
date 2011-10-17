using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Drawing;
using System.Data;
using System.Text;
using System.Windows.Forms;

namespace SADI.KEPlugin
{
    public partial class ServiceSelectionControl : UserControl
    {
        internal SADIService service;

        public ServiceSelectionControl()
        {
            InitializeComponent();
        }

        internal void setService(SADIService service)
        {
            this.service = service;
            string name = getName(service);
            this.name.Text = getName(service);
            this.description.Text = getDescription(service);
        }

        internal bool isSelected()
        {
            return selected.Checked;
        }

        internal static String getName(SADIService service)
        {
            if (service.name != null)
            {
                return service.name;
            }
            else
            {
                return "No name";
            }
        }

        internal static String getDescription(SADIService service)
        {
            StringBuilder buf = new StringBuilder();
            buf.Append("  (");
            buf.Append(service.uri);
            buf.Append(")\r\n  ");
            if (service.description != null)
            {
                buf.Append(service.description);
            }
            else
            {
                buf.Append("No description.");
            }
            if (service.properties.Count > 0)
            {
                buf.Append("\r\n  properties attached: ");
                foreach (PropertyRestriction r in service.properties)
                {
                    buf.Append("\r\n      ");
                    buf.Append(r.ToString());
                }
            }
            return buf.ToString();
        }

        internal void sizeLabels(int width)
        {
            Padding padding = this.Padding + this.tableLayoutPanel1.Padding + this.name.Padding;
            width = width - padding.Left - padding.Right - this.selected.Size.Width;
            name.MaximumSize = new System.Drawing.Size(width, 0);
            description.MaximumSize = new System.Drawing.Size(width, 0);
        }

        private void selected_CheckedChanged(object sender, EventArgs e)
        {
            if (selected.Checked)
            {
                this.BackColor = System.Drawing.SystemColors.Highlight;
                this.ForeColor = System.Drawing.SystemColors.HighlightText;
            }
            else
            {
                this.BackColor = System.Drawing.SystemColors.Info;
                this.ForeColor = System.Drawing.SystemColors.InfoText;
            }
        }
    }
}
