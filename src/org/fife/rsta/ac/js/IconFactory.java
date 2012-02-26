/*
 * 01/29/2012
 *
 * Copyright (C) 2012 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This code is licensed under the LGPL.  See the "license.txt" file included
 * with this project.
 */
package org.fife.rsta.ac.js;

import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 * Holds icons used by JavaScript auto-completion.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class IconFactory {

	public static final int FUNCTION_ICON		= 0;
	public static final int VARIABLE_ICON		= 1;

	private Icon[] icons;

	private static final IconFactory INSTANCE = new IconFactory();


	private IconFactory() {
		icons = new Icon[2];
		icons[FUNCTION_ICON]	= loadIcon("methdef_obj.gif");
		icons[VARIABLE_ICON]	= loadIcon("field_default_obj.gif");
	}


	/**
	 * Returns the singleton instance of this class.
	 *
	 * @return The singleton instance.
	 */
	public static IconFactory get() {
		return INSTANCE;
	}


	/**
	 * Returns the specified icon.
	 *
	 * @param key The icon to retrieve.
	 * @return The icon.
	 */
	public Icon getIcon(int key) {
		return icons[key];
	}


	/**
	 * Loads an icon.
	 *
	 * @param name The file name of the icon to load.
	 * @return The icon.
	 */
	private Icon loadIcon(String name) {
		URL res = getClass().getResource("img/" + name);
		if (res==null) { // Never happens
			// IllegalArgumentException is what would be thrown if res
			// was null anyway, we're just giving the actual arg name to
			// make the message more descriptive
			throw new IllegalArgumentException("icon not found: img/" + name);
		}
		return new ImageIcon(res);
	}


}