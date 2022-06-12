/*
 * Copyright 2013 Volker Oth (0xdeadbeef) / Miklos Juhasz (mjuhasz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bdsup2sub.tools;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * <p>A convenience implementation of FileFilter that filters out
 * all files except for those type extensions that it knows about.</p>
 *
 * <p>Extensions are of the type ".foo", which is typically found on
 * Windows and Unix boxes, but not on Macinthosh. Case is ignored.</p>
 */
public class JFileFilter extends FileFilter {

    private Hashtable<String,JFileFilter> filters;
    private String description;
    private String fullDescription;
    private boolean useExtensionsInDescription = true;

    /**
     * Creates a file filter. If no filters are added, then all
     * files are accepted.
     *
     * @see #addExtension(String)
     */
    public JFileFilter() {
        this.filters = new Hashtable<String,JFileFilter>();
    }

    /**
     * Creates a file filter that accepts files with the given extension.
     * Example: new JFileFilter("jpg");
     *
     * @param extension string containing file extension
     * @see #addExtension(String)
     */
    public JFileFilter(String extension) {
        this(extension, null);
    }

    /**
     * Creates a file filter that accepts the given file type.
     * Example: new JFileFilter("jpg", "JPEG Image Images");
     *
     * Note that the "." before the extension is not needed. If
     * provided, it will be ignored.
     *
     * @param extension string containing file extension
     * @param description string containing file description
     *
     * @see #addExtension(String)
     */
    public JFileFilter(String extension, String description) {
        this();
        if(extension!=null) {
            addExtension(extension);
        }
        if(description!=null) {
            setDescription(description);
        }
    }

    /**
     * Creates a file filter from the given string array.
     * Example: new JFileFilter(String {"gif", "jpg"});
     *
     * Note that the "." before the extension is not needed adn
     * will be ignored.
     *
     * @param filters string array containing extensions
     *
     * @see #addExtension(String)
     */
    public JFileFilter(String[] filters) {
        this(filters, null);
    }

    /**
     * Creates a file filter from the given string array and description.
     * Example: new JFileFilter(String {"gif", "jpg"}, "Gif and JPG Images");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     *
     * @param filters string array containing extensions
     * @param description string containing file description
     *
     * @see #addExtension(String)
     */
    public JFileFilter(String[] filters, String description) {
        this();
        for (String filter : filters) {
            // add filters one by one
            addExtension(filter);
        }
        if(description!=null) {
            setDescription(description);
        }
    }

    /**
     * Return true if this file should be shown in the directory pane,
     * false if it shouldn't.
     *
     * Files that begin with "." are ignored.
     *
     * @see #getExtension(File)
     * @see #accept(File)
     */
    @Override
    public boolean accept(File f) {
        if(f != null) {
            if(f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if(extension != null && filters.get(getExtension(f)) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the extension portion of the file's name .
     *
     * @see #getExtension(File)
     * @see #accept(File)
     * @param f File handle to get extension for
     * @return extension as string (excluding the '.')
     */
    public String getExtension(File f) {
        if(f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if(i > 0 && i < filename.length()-1) {
                return filename.substring(i+1).toLowerCase();
            }
        }
        return null;
    }

    /**
     * Adds a file type "dot" extension to filter against.
     *
     * For example: the following code will create a filter that filters
     * out all files except those that end in ".jpg" and ".tif":
     *
     *   JFileFilter filter = new JFileFilter();
     *   filter.addExtension("jpg");
     *   filter.addExtension("tif");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     * @param extension file extension (e.g. '.sup')
     */
    public void addExtension(String extension) {
        if(filters == null) {
            filters = new Hashtable<String,JFileFilter>(5);
        }
        filters.put(extension.toLowerCase(), this);
        fullDescription = null;
    }


    /**
     * Returns the human readable description of this filter. For
     * example: "JPEG and GIF Image Files (*.jpg, *.gif)"
     *
     * @see #setDescription(String)
     * @see #setExtensionListInDescription(boolean)
     * @see #isExtensionListInDescription()
     * @see #getDescription()
     */
    @Override
    public String getDescription() {
        if(fullDescription == null) {
            if(description == null || isExtensionListInDescription()) {
                fullDescription = description==null ? "(" : description + " (";
                // build the description from the extension list
                Enumeration<String> extensions = filters.keys();
                if(extensions != null) {
                    fullDescription += "." + extensions.nextElement();
                    while (extensions.hasMoreElements()) {
                        fullDescription += ", ." + extensions.nextElement();
                    }
                }
                fullDescription += ")";
            } else {
                fullDescription = description;
            }
        }
        return fullDescription;
    }

    /**
     * Sets the human readable description of this filter. For
     * example: filter.setDescription("Gif and JPG Images");
     *
     * @param description string containing file description
     *
     * @see #setDescription(String)
     * @see #setExtensionListInDescription(boolean)
     * @see #isExtensionListInDescription()
     */
    public void setDescription(String description) {
        this.description = description;
        fullDescription = null;
    }

    /**
     * Determines whether the extension list (.jpg, .gif, etc) should
     * show up in the human readable description.
     *
     * Only relevant if a description was provided in the constructor
     * or using setDescription();
     *
     * @param b true if the extension list should show up in the human readable description
     *
     * @see #getDescription()
     * @see #setDescription(String)
     * @see #isExtensionListInDescription()
     */
    public void setExtensionListInDescription(boolean b) {
        useExtensionsInDescription = b;
        fullDescription = null;
    }

    /**
     * Returns whether the extension list (.jpg, .gif, etc) should
     * show up in the human readable description.
     *
     * Only relevant if a description was provided in the constructor
     * or using setDescription();
     *
     * @return true if the extension list should show up in the human readable description
     *
     * @see #getDescription()
     * @see #setDescription(String)
     * @see #setExtensionListInDescription(boolean)
     */
    public boolean isExtensionListInDescription() {
        return useExtensionsInDescription;
    }
}
