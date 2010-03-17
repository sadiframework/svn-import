package org.sadiframework.swing;

import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

public class AbstractFileFilter extends FileFilter {

	private ArrayList<String> filter = new ArrayList<String>();
	private String description = "";

	/**
	 * Default constructor (no description and no filter)
	 */
	public AbstractFileFilter() {
		this(null, "");
	}

	/**
	 * 
	 * @param filter
	 *            a filter for this FileFilter
	 */
	public AbstractFileFilter(String filter) {
		this(filter, "");
	}

	/**
	 * 
	 * @param filter
	 *            a filter for this FileFilter
	 * @param description
	 *            a description for the set of files that we filter
	 */
	public AbstractFileFilter(String filter, String description) {
		this.filter = new ArrayList<String>();
		if (filter != null && !filter.trim().equals(""))
			addFilter(filter);
		description = "";
	}

	@Override
	public boolean accept(File f) {
	    if (f.isDirectory())
	        return true;
		for (String s : filter) {
			if (f != null && f.isFile())
				if (f.getName().toLowerCase().endsWith(s))
					return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Removes all filters from this file filter
	 */
	public void clearFilters() {
		this.filter = new ArrayList<String>();
	}

	/**
	 * 
	 * @param filter
	 *            a filter to add to this File filter
	 */
	public void addFilter(String filter) {
		if (filter != null && !filter.trim().equals(""))
			this.filter.add(filter.toLowerCase());
	}

	/**
	 * 
	 * @return an String[] of file filters for this FileFilter
	 */
	public String[] getFilters() {
		return this.filter.toArray(new String[] {});
	}

	/**
	 * 
	 * @param description
	 *            a description for the set of files we are filtering
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
