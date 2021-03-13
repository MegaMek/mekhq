/*
 * Copyright (c) 2020 The MegaMek Team. All rights reserved.
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
package mekhq.gui.preferences;

import java.awt.Color;
import java.util.Optional;

import megamek.common.annotations.Nullable;
import megamek.client.ui.preferences.PreferenceElement;

/**
 * Represents a preference which can manage a color and
 * an optional alternate color.
 */
public class ColorPreference extends PreferenceElement {

    private final Color defaultColor;
    private final Color defaultAlternateColor;
    private Optional<Color> color = Optional.empty();
    private Optional<Color> alternateColor = Optional.empty();

    /**
     * Creates a new {@code ColorPreference} with a default {@link Color}.
     */
    public ColorPreference(String name, Color defaultColor) {
        super(name);

        this.defaultColor = defaultColor;
        this.defaultAlternateColor = null;
    }

    /**
     * Creates a new {@code ColorPreference} with a default {@link Color}
     * and a default alternate color.
     */
    public ColorPreference(String name, Color defaultColor, Color defaultAlternateColor) {
        super(name);

        this.defaultColor = defaultColor;
        this.defaultAlternateColor = defaultAlternateColor;
    }

    /**
     * Gets the main color.
     * @return The main color.
     */
    public Optional<Color> getColor() {
        return Optional.ofNullable(color.orElse(defaultColor));
    }

    /**
     * Sets the main color.
     * @param color The main color.
     */
    public void setColor(Optional<Color> color) {
        this.color = color;
    }

    /**
     * Gets the alternate color.
     * @return The alternate color.
     */
    public Optional<Color> getAlternateColor() {
        return Optional.ofNullable(alternateColor.orElse(defaultAlternateColor));
    }

    /**
     * Sets the alternate color.
     * @param alternateColor The alternate color.
     */
    public void setAlternateColor(Optional<Color> alternateColor) {
        this.alternateColor = alternateColor;
    }

    @Override
    protected String getValue() {
        if (alternateColor.isPresent()) {
            return String.format("%s|%s", format(color), format(alternateColor));
        } else {
            return format(color);
        }
    }

    @Nullable
    private static String format(Optional<Color> color) {
        return color.isPresent() ? "#" + Integer.toHexString(color.get().getRGB()) : null;
    }

    @Override
    protected void initialize(String value) {
        if (value != null) {
            String[] values = value.split("\\|");
            if (values.length > 0) {
                color = decode(values[0]);
            }
            if (values.length > 1) {
                alternateColor = decode(values[1]);
            }
        }
    }

    private static Optional<Color> decode(String value) {
        try {
            return Optional.ofNullable(Color.decode(value));
        } catch (NumberFormatException ex0) {
            try {
                return Optional.ofNullable(Color.getColor(value));
            } catch (NumberFormatException ex1) {
                // CAW: Ignored.
            }
        }
        return Optional.empty();
    }

    @Override
    protected void dispose() {
    }
}
