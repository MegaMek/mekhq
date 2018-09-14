/*
 * MekHQ - Copyright (C) 2018 - The MekHQ Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekhq.util;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Simple, immutable class holding the identifiers for an image
 * (category and filename)
 */
public class ImageId implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("javadoc")
    public static ImageId of(String category, String fileName) {
        return new ImageId( requireNonNull(category),
                            requireNonNull(fileName) );
    }

    private ImageId(String category, String fileName) {
        this.category = category;
        this.fileName = fileName;
    }

    private final String category;
    private final String fileName;

    @SuppressWarnings("javadoc")
    public String getCategory() {
        return category;
    }

    @SuppressWarnings("javadoc")
    public String getFileName() {
        return fileName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, fileName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ImageId other = (ImageId) obj;
        return fileName.equals(other.fileName)
            && category.equals(other.category);
    }

    @Override
    public String toString() {
        return category + "/" + fileName; //$NON-NLS-1$
    }

    /**
     * Up to v0.45.1, magic values were saved for people missing portraits.
     */
    @SuppressWarnings("nls")
    public static Optional<ImageId> cleanupLegacyPortraitId(String portraitCategory, String portraitFileName) {

        String category = portraitCategory;
        String fileName = portraitFileName;

        // This cleanup logic is derived from code originally in mekhq.gui.BasicInfo.setPortrait()

        if ("-- General --".equals(category)) {
            // Crew.ROOT_PORTRAIT ("-- General --") is what the UI shows for the
            // root category and should really not make it into the model.
            category = "";
        }

        if ("None".equals(fileName)) {
            // Crew.PORTRAIT_NONE ("None") is a magic value to signify there is
            // no portrait. Let's replace it with a much more clear null.
            category = null;
            fileName = null;
        }

        if ("".equals(category) && "default.gif".equals(fileName)) {
            // this is the default/fallback portrait and should not have been saved
            category = null;
            fileName = null;
        }

        return (category != null) && (fileName != null)
             ? Optional.of(ImageId.of(category, fileName))
             : Optional.empty();

    }

}
