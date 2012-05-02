package ca.elmonline.util.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class MouseMotionEventLogger implements MouseMotionListener
{
    protected String prefix;
    
    /**
     * Constructs a MouseEventLogger with the specified
     * prefix on logged messages.
     * @param prefix a string to prepend to all log messages
     */
    public MouseMotionEventLogger(String prefix)
    {
        if (prefix.length() > 0)
            this.prefix = prefix + " ";
        else
            this.prefix = prefix;
    }
    
    protected void log(Object message)
    {
        System.out.println(prefix + message);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged( MouseEvent event ) {
        log("mouseDragged: " + event);
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved( MouseEvent event ) {
        log("mouseMoved: " + event);
    }
}
