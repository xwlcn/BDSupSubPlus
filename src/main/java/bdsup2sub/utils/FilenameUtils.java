/*
 * Copyright 2013 Miklos Juhasz (mjuhasz)
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
package bdsup2sub.utils;

import java.io.File;

public class FilenameUtils {

    private static final char UNIX_SEPARATOR = '/';
    private static final char WINDOWS_SEPARATOR = '\\';

    private static final char SEPARATOR_CHAR = File.separatorChar;
    private static final char EXTENSION_SEPARATOR_CHAR = '.';

    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfExtension(filename);
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfExtension(filename);
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    public static String separatorsToUnix(String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }
    
    public static String addSeparator(String path) {
        if (path == null || path.lastIndexOf(SEPARATOR_CHAR) == path.length()-1) {
            return path;
        }
        return path + File.separatorChar;
    }

    public static String getName(String filename) {
        return new File(filename).getName();
    }

    public static String getParent(String filename) {
        return new File(filename).getParent();
    }

    private static int indexOfExtension(String filename) {
        if (filename == null) {
            return -1;
        }
        int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR_CHAR);
        int lastSeparator = indexOfLastSeparator(filename);
        return (lastSeparator > extensionPos ? -1 : extensionPos);
    }

    private static int indexOfLastSeparator(String filename) {
        if (filename == null) {
            return -1;
        }
        return filename.lastIndexOf(SEPARATOR_CHAR);
    }
}
