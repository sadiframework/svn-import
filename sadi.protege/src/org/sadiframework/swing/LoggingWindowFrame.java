package org.sadiframework.swing;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.sadiframework.preferences.PreferenceManager;

/**
 * A Logging view that redirects the STDOUT and STDERR streams to a console. 
 * @author Eddie Kawas
 *
 */
public class LoggingWindowFrame extends JDialog {
//public class LoggingWindowFrame extends JFrame {

    
    // preserve old stdout/stderr streams in case they might be useful      
    private final PrintStream stdout = System.out;                                       
    private final PrintStream stderr = System.err;
    
    private static final long serialVersionUID = 6380852214094506047L;
    private PreferenceManager manager = PreferenceManager.newInstance();
    private final ResourceBundle bundle = ResourceBundle.getBundle("org.sadiframework.utils.i18n.EditorResourceBundle");
    private JButton close, cancel;
    private PropertyChangeListener pListener;
    
    private  LogStream stream;
    
    private String preferenceKey = "";
    private String onClosePreferenceKey = "";
    
    /**
     * 
     * @param title the title of the Logging frame
     * @param prefKey the preference key that this window will listen for changes in.
     */
    public LoggingWindowFrame(String title, String prefKey) {
        this(title, prefKey, null);
    }
    
    public LoggingWindowFrame(String title, String prefKey, String onClosePrefKey) {
        super();
        setTitle(title);
        this.onClosePreferenceKey = onClosePrefKey;
        this.preferenceKey = prefKey;
        pListener = new GeneratorPropertyListener();
        manager.addPropertyChangeListener(prefKey, pListener);
        init();
    }
    
    private void init() {        
        setLayout(new BorderLayout(6,6));
        JTextArea area = new JTextArea();
        area.setEditable(false);
        stream = new LogStream(area);
        
        close = new AbstractButton(bundle.getString("close"), false, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
                if (onClosePreferenceKey != null) {
                    manager.saveBooleanPreference(onClosePreferenceKey, true);
                }
            }
        });
        cancel = new AbstractButton(bundle.getString("cancel"), true, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // fire an event on our preference key showing that we were cancelled.
                manager.saveBooleanPreference(preferenceKey, false);
            }
        });
        
        // add our scrollpane
        add(new JScrollPane(area), BorderLayout.CENTER);
        // add the buttons
        JPanel buttons = UIUtils.createButtonPanel(new JButton[]{cancel, close});
        buttons.setAlignmentX(CENTER_ALIGNMENT);
        add(buttons, BorderLayout.PAGE_END);
        
        setPreferredSize(new Dimension(400, 400));
        
        // dont allow the user to use the close button
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (close.isEnabled()) {
                    dispose();
                }
            }
        });

        pack();
    }
    
    @Override
    public void setVisible(boolean b) {
        if (b) {
            setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            // setup STDERR/STDOUT redirection
            System.setOut(new PrintStream(stream,true));
            System.setErr(new PrintStream(stream, true));
            //setAlwaysOnTop(true);
        } else {
            // reset STDERR/STDOUT redirection
            System.setOut(stdout);
            System.setErr(stderr);
        }
        super.setVisible(b);
    }

    public static void main(String args[]) {
        JDialog f = new LoggingWindowFrame("Test log frame", "my-pref-key");
        f.setVisible(true);
    }
    
 // inner class for listening to changes in our properties
    private class GeneratorPropertyListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            // if our property is false, disable cancel, enable close and stop STDOUT/STDERR redirection
            if (!(evt.getNewValue() instanceof Boolean))
                return;
            if ((Boolean)evt.getNewValue()) {
                close.setEnabled(false);
                cancel.setEnabled(true);
            } else {
                close.setEnabled(true);
                cancel.setEnabled(false);
            }
        }
        
    }
    
    class LogStream extends ByteArrayOutputStream {
        
        private JTextArea area;
        public LogStream(JTextArea area) {
            super();
            this.area = area;
        }
        
        /*
         * (non-Javadoc)
         * @see java.io.OutputStream#flush()
         */
        public void flush() throws IOException {     
            String record; 
            synchronized(this) { 
                super.flush(); 
                record = this.toString(); 
                super.reset(); 
     
                if (record.length() == 0) { 
                    // avoid empty records 
                    return; 
                }
                area.append(record);
                area.setCaretPosition(area.getText().length());
            } 
        } 
    }
}
