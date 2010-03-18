package org.sadiframework.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

import org.sadiframework.preferences.PreferenceManager;
import org.sadiframework.swing.listeners.HelpActionListener;


public class UIUtils {

    // copy here some often used constants
    public static final int RELATIVE = GridBagConstraints.RELATIVE;
    public static final int REMAINDER = GridBagConstraints.REMAINDER;
    public static final int PS = GridBagConstraints.PAGE_START;
    public static final int PE = GridBagConstraints.PAGE_END;
    public static final int NONE = GridBagConstraints.NONE;
    public static final int BOTH = GridBagConstraints.BOTH;
    public static final int HORI = GridBagConstraints.HORIZONTAL;
    public static final int VERT = GridBagConstraints.VERTICAL;
    public static final int CENTER = GridBagConstraints.CENTER;
    public static final int NORTH = GridBagConstraints.NORTH;
    public static final int NEAST = GridBagConstraints.NORTHEAST;
    public static final int EAST = GridBagConstraints.EAST;
    public static final int SEAST = GridBagConstraints.SOUTHEAST;
    public static final int SOUTH = GridBagConstraints.SOUTH;
    public static final int SWEST = GridBagConstraints.SOUTHWEST;
    public static final int WEST = GridBagConstraints.WEST;
    public static final int NWEST = GridBagConstraints.NORTHWEST;

    // often used "style" components
    public static final Insets BREATH_TOP = new Insets(10, 0, 0, 0);
    public static final Insets BREATH_TOP_LEFT = new Insets(10, 10, 0, 0);
    public static final Insets BREATH_LEFT = new Insets(0, 10, 0, 0);


    public static void addComponent(Container parent, Component component, int gridx, int gridy,
            int gridwidth, int gridheight, int anchor, int fill, double weightx, double weighty) {
        parent.add(component, new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx,
                weighty, anchor, fill, new Insets(0, 0, 0, 0), 1, 1));
    }

    public static void addComponent(Container parent, Component component, int gridx, int gridy,
            int gridwidth, int gridheight, int anchor, int fill, double weightx, double weighty,
            Insets inset) {
        parent.add(component, new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx,
                weighty, anchor, fill, inset, 1, 1));
    }
    
    public static void addComponent(Container parent, Component component, int gridx, int gridy,
            int gridwidth, int gridheight, int anchor, int fill, double weightx, double weighty,
            Insets inset, int ipadx, int ipady) {
        parent.add(component, new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx,
                weighty, anchor, fill, inset, ipadx, ipady));
    }
    
    public static void addComponent(Container parent, Component component, int gridx, int gridy,
            int gridwidth, int gridheight, int anchor, int fill, double weightx, double weighty,
            int ipadx, int ipady) {
        parent.add(component, new GridBagConstraints(gridx, gridy, gridwidth, gridheight, weightx,
                weighty, anchor, fill, new Insets(0,0,0,0), ipadx, ipady));
    }

    private static PreferenceManager manager = PreferenceManager.newInstance();

    public static JFileChooser getOpenFileChooser(String title, String directory, FileFilter filter) {
        JFileChooser chooser = new JFileChooser(manager.getPreference(directory, System
                .getProperty("user.dir")));
        chooser.setDialogTitle(title);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        // set up a filter for that chooser
        if (filter != null)
            chooser.addChoosableFileFilter(filter);
        return chooser;
    }

    public static JFileChooser getOpenDirectoryChooser(String title, String directory) {
        JFileChooser chooser = new JFileChooser(manager.getPreference(directory, System
                .getProperty("user.dir")));
        chooser.setDialogTitle(title);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return chooser;
    }

    public static JFileChooser getSaveFileChooser(String title, String directory, FileFilter filter) {
        JFileChooser chooser = new JFileChooser(manager.getPreference(directory, System
                .getProperty("user.dir")));
        chooser.setDialogTitle(title);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        // set up a filter for that chooser
        if (filter != null)
            chooser.addChoosableFileFilter(filter);
        return chooser;
    }

    public static JPanel createButtonPanel(JComponent[] buttons) {

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 10));
        buttonPanel.add(Box.createHorizontalGlue());
        for (int i = 0; i < buttons.length; i++)
            buttonPanel.add(buttons[i]);
        return buttonPanel;
    }

//    public static JPanel createFlowPanel(JComponent[] stuff) {
//        JPanel buttonPanel = new JPanel();
//        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
//        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 10));
//        buttonPanel.add(Box.createHorizontalGlue());
//        for (int i = 0; i < stuff.length; i++)
//            buttonPanel.add(stuff[i]);
//        return buttonPanel;
//    }

    public static FileFilter createFileFilter(String description, String[] filters) {
        AbstractFileFilter filter = new AbstractFileFilter();
        filter.setDescription(description);
        for (String f : filters)
            filter.addFilter(f);
        return filter;
    }

    public static JTextArea createArea(String text, boolean isEditable) {
        JTextArea area = new JTextArea(text == null ? "" : text);
        // area.setEnabled(isEnabled);
        area.setEditable(isEditable);
        return area;
    }
    
    public static void showScrollableMsgDialog(String title, String msg, Component parent) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(400, 400));
        panel.setLayout(new BorderLayout());
        JTextArea textArea = createArea(msg==null?"":msg, false);
        JPanel contentPane = new JPanel(new BorderLayout(7, 7));
        contentPane.add(new JScrollPane(textArea));
        panel.add(contentPane, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(parent, panel, title==null ? "" : title, JOptionPane.INFORMATION_MESSAGE);
    }

    
    public static JButton createHelpButton(String help) {
        JButton button = new AbstractButton("?", true, new HelpActionListener(help));
        return button;
    }
    

}
