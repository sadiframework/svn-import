using System;
using System.Collections.Generic;
using System.Text;

namespace SADI.KEPlugin
{
    class PropertyList
    {
        public PropertyList()
        {
            Properties = new List<string>();
        }

        public List<string> Properties { get; set; }
    }
}
