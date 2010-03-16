package org.sadiframework.editor.views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.EditorKit;
import javax.swing.text.StyledEditorKit;


import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.core.ui.view.ViewComponent;
import org.sadiframework.editor.documents.PerlSyntaxDocument;
import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.swing.AbstractButton;
import org.sadiframework.swing.UIUtils;
import org.sadiframework.utils.Checksum;

public class SadiEditorView extends ViewComponent {

	private PreferenceManager manager = PreferenceManager.newInstance();
	
	/**
	 * SADI Editor Last known user directory
	 */
	final public static String EDITOR_DIRECTORY = "editor-directory";

	/**
	 * SADI Editor Last known user filename
	 */
	final public static String EDITOR_FILENAME = "editor-filename";

	/**
	 * SADI Editor Last known user filename checksum
	 */
	final public static String EDITOR_FILENAME_CHKSUM = "editor-filename-checksum";
	final private static String[] FILTERS = { ".xml", ".pm", ".pl", ".rdf",
			".owl" };

	private JTextPane editorTextPane;
	private JButton open, save, close;

	private JLabel currentlyEditing, currentPosition;

	private static final long serialVersionUID = 6011258260093115589L;

	private final ResourceBundle bundle = ResourceBundle
			.getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");

	public void initialise() throws Exception {

		setLayout(new BorderLayout(6, 6));
		// create our editor (has its own scrollpane)
		add(getEditorPanel(), BorderLayout.CENTER);
	}

	public void dispose() throws Exception {

	}

	private JPanel getEditorPanel() {
		JPanel p = new JPanel();
		BorderLayout thisLayout = new BorderLayout();
		p.setLayout(thisLayout);
		p.setBorder(ComponentFactory.createTitledBorder(bundle
				.getString("sadi_editor_title")));

		JPanel footer = new JPanel();
		footer.setLayout(new BorderLayout());
		currentlyEditing = new JLabel("");
		currentPosition = new JLabel("");
		footer.add(currentlyEditing, BorderLayout.WEST);
		footer.add(currentPosition, BorderLayout.EAST);

		editorTextPane = new JTextPane();
		editorTextPane.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				int dot = e.getDot();
				// get current line
				int line = ((JTextPane) e.getSource()).getDocument()
						.getDefaultRootElement().getElementIndex(dot);
				// get the position on the line
				int pos = dot
						- ((JTextPane) e.getSource()).getDocument()
								.getDefaultRootElement().getElement(line)
								.getStartOffset();
				// System.out.println("line: "+line + " column: "+ pos);
				line++;
				pos++;
				currentPosition.setText(line + ":" + pos);

			}
		});

		EditorKit editorKit = new StyledEditorKit() {
			private static final long serialVersionUID = 1L;

			public javax.swing.text.Document createDefaultDocument() {
				return new PerlSyntaxDocument();
			}
		};

		editorTextPane.setEditorKitForContentType("text/perl", editorKit);
		editorTextPane.setContentType("text/perl");
		// editorTextPane.setEditable(false);

//		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK);
//		editorTextPane.registerKeyboardAction(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				System.out.println("Save ...");
//			}
//		}, key, JComponent.WHEN_IN_FOCUSED_WINDOW);
		JScrollPane scrollPane = ComponentFactory
				.createScrollPane(editorTextPane);
		p.add(getSadiButtonPanel(), BorderLayout.PAGE_START);
		p.add(scrollPane, BorderLayout.CENTER);
		p.add(footer, BorderLayout.PAGE_END);

		// set the default font
		Font f = new Font(null, editorTextPane.getFont().getStyle(),
				editorTextPane.getFont().getSize());
		editorTextPane.setFont(f);
		return p;
	}

	private JComponent getSadiButtonPanel() {
		JToolBar p = ComponentFactory.createViewToolBar();
		p.setLayout(new FlowLayout(FlowLayout.LEFT));
		open = new AbstractButton(bundle.getString("open_file"), true,
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// check if we need to save a file first!
						doSave();
						File file = null;
						// get a file chooser
						JFileChooser chooser = UIUtils.getOpenFileChooser(
								bundle.getString("open"), EDITOR_DIRECTORY,
								UIUtils.createFileFilter(bundle
										.getString("open_file_sadi"), FILTERS));

						int returnVal = chooser.showOpenDialog((JComponent) e
								.getSource());
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							file = chooser.getSelectedFile();
							if (!file.canWrite()) {
								JOptionPane.showMessageDialog(null, bundle
										.getString("editor_cannot_write"),
										"Error", JOptionPane.ERROR_MESSAGE);
								save.setEnabled(false);
							} else {
								save.setEnabled(true);
							}
							manager.savePreference(EDITOR_FILENAME, file
									.getAbsolutePath());
							manager.savePreference(EDITOR_DIRECTORY, file
									.getParent());
						}

						if (file == null)
							return;
						try {
							StringBuilder sb = new StringBuilder();
							BufferedReader br = new BufferedReader(
									new FileReader(file));
							String newline = System
									.getProperty("line.separator");
							String line = null;
							while ((line = br.readLine()) != null) {
								sb.append(line + newline);
							}
							// calculate checksum for file and save it
							manager.savePreference(EDITOR_FILENAME_CHKSUM,
									Checksum.getMD5Checksum(sb.toString()));
							// set the editor text
							editorTextPane.setText(sb.toString());
							currentlyEditing.setText(bundle
									.getString("editing")
									+ ": " + file.getAbsolutePath());
							currentPosition.setText("");
							editorTextPane.setCaretPosition(0);
							editorTextPane.setEditable(true);
							close.setEnabled(true);
							open.setEnabled(false);
						} catch (IOException ioe) {
							ErrorLogPanel.showErrorDialog(ioe);
						}
					}
				});
		close = new AbstractButton(bundle.getString("close_file"), false,
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (doSave()) {
							editorTextPane.setText("");
							currentlyEditing.setText("");
							currentPosition.setText("");
							editorTextPane.setCaretPosition(0);
							editorTextPane.setEditable(false);

							// clear the last file name & checksum...
							manager.savePreference(EDITOR_FILENAME, "");
							manager.savePreference(EDITOR_FILENAME_CHKSUM, "");

							close.setEnabled(false);
							save.setEnabled(true);
							open.setEnabled(true);
						}
					}

				});
		save = new AbstractButton(bundle.getString("save_file"), true,
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						doSave();
					}
				});
		p.add(open);
		p.add(close);
		p.add(save);
		return p;
	}

	private boolean doSave() {
		// if checksums do not match, prompt to save
		if (!manager.getPreference(EDITOR_FILENAME_CHKSUM, "").equals(
				Checksum.getMD5Checksum(editorTextPane.getText()))
				&& !editorTextPane.getText().trim().equals("")) {
			int retVal = JOptionPane.showConfirmDialog(null, bundle
					.getString("editor_prompt_save"), bundle
					.getString("editor_prompt_save_title"),
					JOptionPane.YES_NO_OPTION);
			if (retVal == JOptionPane.YES_OPTION) {
				File file = null;
				// get the file chooser
				JFileChooser chooser = UIUtils.getSaveFileChooser(bundle
						.getString("save_file"), EDITOR_DIRECTORY, UIUtils
						.createFileFilter(bundle.getString("save_file_sadi"),
								FILTERS));
				// show the current file if possible
				if (!manager.getPreference(EDITOR_FILENAME, "").equals(""))
					chooser.setSelectedFile(new File(manager.getPreference(
							EDITOR_FILENAME, "")));
				int returnVal = chooser.showSaveDialog(this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = chooser.getSelectedFile();
					manager.savePreference(EDITOR_FILENAME, file
							.getAbsolutePath());
					manager.savePreference(EDITOR_DIRECTORY, file.getParent());
					if (file.exists() && !file.canWrite()) {
						JOptionPane.showMessageDialog(this, bundle
								.getString("editor_cannot_write"), "Error",
								JOptionPane.ERROR_MESSAGE);
						return false;
					}
					try {
						BufferedWriter out = new BufferedWriter(new FileWriter(
								file));
						out.write(editorTextPane.getText());
						out.close();
					} catch (IOException ioe) {
						ErrorLogPanel.showErrorDialog(ioe);
					}
					// save the checksum
					manager.savePreference(EDITOR_FILENAME_CHKSUM, Checksum
							.getMD5Checksum(editorTextPane.getText()));
				} else {
					// user chose not to save the file
					return false;
				}
			}
		}
		return true;
	}
}
