using System;
using System.Collections.Generic;
using System.Text;

namespace SADI.KEPlugin
{
    public class PropertyRestriction
    {
        string onPropertyURI;
        string onPropertyLabel;
        string valuesFromURI;
        string valuesFromLabel;

        public PropertyRestriction(string onPropertyURI, string onPropertyLabel, string valuesFromURI, string valuesFromLabel)
        {
            this.onPropertyURI = onPropertyURI;
            this.onPropertyLabel = onPropertyLabel;
            this.valuesFromURI = valuesFromURI;
            this.valuesFromLabel = valuesFromLabel;
        }

        public override string ToString()
        {
            string s = "";
            s += (onPropertyLabel != null) ? onPropertyLabel : onPropertyURI;
            s += "\r\n        (with values from ";
            if (valuesFromURI != null) {
                s += (valuesFromLabel != null) ? valuesFromLabel : valuesFromURI;
            } else {
                s += "an unknown class";
            }
            s += ")";
            return s;
        }
    }
}
