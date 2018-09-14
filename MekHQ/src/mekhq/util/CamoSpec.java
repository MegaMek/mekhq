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

import java.awt.Color;
import java.util.Optional;

import megamek.client.ui.swing.util.PlayerColors;

/**
 * A simple, immutable class holding information on a camo.
 */
public class CamoSpec {

    @SuppressWarnings("javadoc")
    public static CamoSpec ofTint(int tintIndex) {
        PlayerColors.getColorRGB(tintIndex); // this throws if invalid
        return new CamoSpec(tintIndex, Optional.empty());
    }

    @SuppressWarnings("javadoc")
    public static CamoSpec of(int tintIndex, ImageId imageId) {
        PlayerColors.getColorRGB(tintIndex); // this throws if invalid
        return new CamoSpec(tintIndex, Optional.of(imageId));
    }

    @SuppressWarnings("javadoc")
    public static CamoSpec of(int tintIndex, Optional<ImageId> imageId) {
        PlayerColors.getColorRGB(tintIndex); // this throws if invalid
        return new CamoSpec(tintIndex, requireNonNull(imageId));
    }

    private CamoSpec(int tintIndex, Optional<ImageId> imageId) {
        this.tintIndex = tintIndex;
        this.imageId = imageId;
    }

    private final int tintIndex;
    private final Optional<ImageId> imageId;

    @SuppressWarnings("javadoc")
    public int getTintIndex() {
        return tintIndex;
    }

    @SuppressWarnings("javadoc")
    public Optional<ImageId> getImageId() {
        return imageId;
    }

    @SuppressWarnings("javadoc")
    public Color getTint() {
        return new Color(PlayerColors.getColorRGB(tintIndex), false);
    }

    @Override
    public int hashCode() {
        return (1217 * tintIndex) + imageId.hashCode();
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
        CamoSpec other = (CamoSpec) obj;
        return (tintIndex == other.tintIndex)
            && imageId.equals(other.imageId);
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return String.format( "%s(%06x)/(%s)",
                              tintIndex,
                              getTint().getRGB(),
                              imageId.map(ImageId::toString).orElse("no-camo") );
    }

}
