package ca.elmonline.util.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class MouseEventLogger implements MouseListener
{
	protected String prefix;
	
	/**
	 * Constructs a MouseEventLogger with the specified
	 * prefix on logged messages.
	 * @param prefix a string to prepend to all log messages
	 */
	public MouseEventLogger(String prefix)
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
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent event)
	{
		log("mouseClicked: " + event);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent event)
	{
		log("mouseEntered: " + event);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent event)
	{
		log("mouseExited: " + event);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent event)
	{
		log("mousePressed: " + event);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent event)
	{
		log("mouseReleased: " + event);
	}
}
