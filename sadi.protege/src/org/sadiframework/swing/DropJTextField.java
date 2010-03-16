/**
 * 
 */
package org.sadiframework.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTextField;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.ui.table.OWLObjectDropTargetListener;
import org.protege.editor.owl.ui.transfer.OWLObjectDropTarget;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * A JTextField that can allows users to drop OWL classes onto it
 * @author Eddie Kawas
 * 
 */
public class DropJTextField extends JTextField implements OWLObjectDropTarget {

	private static final long serialVersionUID = 9218666065001682504L;
	private OWLModelManager mngr;
	// last dropped enitity
	private OWLEntity entity = null;
	private final Color background ;
	
	public DropJTextField(int length, OWLModelManager mngr) {
		super(length);
		this.mngr = mngr;
		DropTarget dt = new DropTarget(this, new DropJTextFieldDropListener(
				this));
		this.setDropTarget(dt);
		// save the for later!
		background = getBackground();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.protege.editor.owl.ui.transfer.OWLObjectDropTarget#dropOWLObjects
	 * (java.util.List, java.awt.Point, int)
	 */
	public boolean dropOWLObjects(List<OWLObject> owlObjects, Point pt, int type) {
		for (OWLObject owl : owlObjects)
			if (owl instanceof OWLEntity) {
				setEntity((OWLEntity) owl);
				return true;
			}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.protege.editor.owl.ui.transfer.OWLObjectDropTarget#getComponent()
	 */
	public JComponent getComponent() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.protege.editor.owl.ui.transfer.OWLObjectDropTarget#getOWLModelManager
	 * ()
	 */
	public OWLModelManager getOWLModelManager() {
		return this.mngr;
	}

	class DropJTextFieldDropListener extends OWLObjectDropTargetListener {

		public DropJTextFieldDropListener(DropJTextField component) {
			super(component);
		}

		@Override
		public void dragOver(DropTargetDragEvent dtde) {
			super.dragOver(dtde);
			if (background != null) {
				DropTargetContext context = dtde.getDropTargetContext();
				if (context != null) {
					Component component = context.getComponent();
					if (component != null && component instanceof DropJTextField) {
						((DropJTextField)component).setBackground(Color.YELLOW);
					}
				}
			}
		}

		@Override
		public void dragExit(DropTargetEvent dte) {
			super.dragExit(dte);
			if (background != null) {
				DropTargetContext context = dte.getDropTargetContext();
				if (context != null) {
					Component component = context.getComponent();
					if (component != null && component instanceof DropJTextField) {
						((DropJTextField)component).setBackground(background);
					}
				}
			}
		}
		@Override
		public void drop(DropTargetDropEvent dtde) {
			super.drop(dtde);
			DropTargetContext context = dtde.getDropTargetContext();
			if (context != null) {
				Component component = context.getComponent();
				if (component != null && component instanceof DropJTextField) {
					if (getEntity() != null) {
						((DropJTextField) component).setText(getEntity()
								.getIRI().toString());
						((DropJTextField)component).setBackground(background);
						setEntity(null);
					}
				}
			}
		}
	}

	/**
	 * Getter
	 * @return an OWLEntity dropped on to this control
	 */
	public OWLEntity getEntity() {
		return entity;
	}

	/**
	 * Setter
	 * @param entity the OWLEntity to set
	 */
	public void setEntity(OWLEntity entity) {
		this.entity = entity;
	}

}
