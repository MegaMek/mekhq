/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.io;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Enumeration holding information about the file types that are most relevant for MekHQ
 */
 public enum FileType {

    /**
     * Value for personnel files.
     */
    PRSX("Personnel file", "prsx"), //$NON-NLS-2$

    /**
     * Value for parts files.
     */
    PARTS("Parts file", "parts"), //$NON-NLS-2$

    /**
     * Value for json files.
     */
    JSON("Json file", "json"), //$NON-NLS-2$

    /**
     * Value for csv files.
     */
    CSV("CSV file", "csv"), //$NON-NLS-2$

    /**
     * Value for tsv files.
     */
    TSV("TSV file", "tsv"), //$NON-NLS-2$

    /**
     * Value for xml files.
     */
    XML("XML file", "xml"), //$NON-NLS-2$

    /**
     * Value for png files.
     */
    PNG("PNG file", "png"), //$NON-NLS-2$

    /**
     * Value for mul files.
     */
    MUL("MUL file", "mul"), //$NON-NLS-2$

    /**
     * Value for campaign files.
     */
    CPNX("Campaign file", "cpnx", "xml"); //$NON-NLS-2$ //$NON-NLS-3$

    private FileType(String description, String... extensions) {
        this.description = description;
        this.extensions = Arrays.asList(extensions);
    }

    private final String description;
    private final List<String> extensions;

    /**
     * @return the description of this file type
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return what extensions the files of type usually have
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * @return the recommended extension for files of this type
     */
    public String getRecommendedExtension() {
        return extensions.get(0);
    }

    /**
     * @return a matcher to filter files of this type based on the file name
     */
    public Predicate<String> getNameFilter() {
        return fileName -> {
            int lastDotIdx = fileName.lastIndexOf('.');
            if (lastDotIdx < 0) {
                return true;
            } else {
                return extensions.contains(fileName.substring(lastDotIdx +1).toLowerCase());
            }
        };
    }

}

