package org.sadiframework.preferences;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * 
 * @author Eddie Kawas
 * 
 */
public class PreferenceManager {

    private static Preferences prefs = Preferences.userRoot().node(
            PreferenceManager.class.getCanonicalName());
    private PropertyChangeSupport support;

    private static PreferenceManager pm;

    /**
     * 
     * @return a shared instance of the preference manager
     */
    public static PreferenceManager newInstance() {
        if (pm == null) {
            pm = new PreferenceManager();
        }
        return pm;
    }

    private PreferenceManager() {
        support = new PropertyChangeSupport(this);
    }

    /**
     * 
     * @param key
     * @param def
     * @return
     */
    public String getPreference(String key, String def) {
        return prefs.get(key, def);
    }

    /**
     * 
     * @param key
     * @param def
     * @return
     */
    public boolean getBooleanPreference(String key, boolean def) {
        return prefs.getBoolean(key, def);
    }

    public void savePreference(String key, String value) {
        if (key != null && value != null && !key.trim().equals("")) {
            prefs.put(key, value);
            fire(key.toString(), value);
        }
    }

    public void saveBooleanPreference(String key, boolean value) {
        if (key != null && !key.trim().equals("")) {
            prefs.putBoolean(key, value);
            fire(key.toString(), value);
        }
    }

    public boolean deletePreference() {
        try {
            prefs.clear();
            return true;
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fire an event to any registered listeners. The source of this event will
     * be this class.
     * <p>
     * 
     * @param key
     *            is a name of the fired event
     * @param value
     *            is a value associated with this event
     * 
     */
    public void fire(String key, Object value) {
        support.firePropertyChange(key.toString(), null, value);
    }

    /*********************************************************************
     * Register listeners.
     ********************************************************************/
    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (support != null)
            support.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        if (support != null)
            support.addPropertyChangeListener(propertyName, l);
    }

    /*********************************************************************
     * Unregister listeners.
     ********************************************************************/
    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (support != null)
            support.removePropertyChangeListener(l);
    }
    

}
