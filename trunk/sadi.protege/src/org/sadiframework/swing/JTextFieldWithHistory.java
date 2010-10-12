package org.sadiframework.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.sadiframework.preferences.PreferenceManager;


public class JTextFieldWithHistory extends JTextField {

    private static final long serialVersionUID = -3263565126334497780L;
    private String preferenceKey = "";
    private PreferenceManager manager = PreferenceManager.newInstance();
    
    private String DEFAULT_TEXT = "";

    public JTextFieldWithHistory(int columns, String preferenceKey) {
        this(columns, null, preferenceKey);
    }
    
    public JTextFieldWithHistory(int columns, String defText, String preferenceKey) {
        super(columns);
        this.DEFAULT_TEXT = (defText == null ? "" : defText);
        setPreferenceKey(preferenceKey);
        init();
    }

    private void init() {
        // set the current text (obtained from history if it exists)
        setText(manager.getPreference(getPreferenceKey(), DEFAULT_TEXT));

        // set up a document listener
        getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                if (isFocusOwner())
                    saveField(getText());
            }

            public void removeUpdate(DocumentEvent e) {

                if (isFocusOwner())
                    saveField(getText());
            }

            public void insertUpdate(DocumentEvent e) {
                if (isFocusOwner())
                    saveField(getText());
            }
        });
        // listen for changes to our property
        manager.addPropertyChangeListener(getPreferenceKey(), new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // ensure that we dont fire events when textfield is being typed in
                if (!isFocusOwner())
                    if (evt.getNewValue() instanceof String) {
                        setText(manager.getPreference(getPreferenceKey(), DEFAULT_TEXT));
                        validate();
                    }
            }
        });
        setMaximumSize(getPreferredSize());
    }

    /**
     * @return the preferenceKey
     */
    public String getPreferenceKey() {
        return preferenceKey;
    }

    /**
     * @param preferenceKey
     *            the preferenceKey to set
     */
    public void setPreferenceKey(String preferenceKey) {
        this.preferenceKey = preferenceKey;
    }

    private void saveField(String t) {
        manager.savePreference(preferenceKey, t);
    }
}
