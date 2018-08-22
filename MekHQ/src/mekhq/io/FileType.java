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
    CPNX("Campaign file", fn -> (fn.toLowerCase().endsWith(".cpnx") || fn.toLowerCase().endsWith(".xml")), "cpnx");   //$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$

    private FileType(String description, String extension) {
        this(description, fn -> fn.toLowerCase().endsWith("." + extension), extension); //$NON-NLS-1$
    }

    private FileType(String description, Predicate<String> nameFilter, String recommendedExtension) {
        this.description = description;
        this.nameFilter = nameFilter;
        this.recommendedExtension = recommendedExtension;
    }

    private final String description;
    private final Predicate<String> nameFilter;
    private final String recommendedExtension;

    /**
     * @return the description of this file type
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return a matcher to filter files of this type based on the file name
     */
    public Predicate<String> getNameFilter() {
        return nameFilter;
    }

    /**
     * @return the recommended extension for files of this type
     */
    public String getRecommendedExtension() {
        return recommendedExtension;
    }

}

