/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.baseComponents;

import java.awt.Color;
import javax.swing.UIManager;

/** Theme-aware colors for {@link FramedCommandButton}. */
public final class FramedCommandButtonStyle {
    private static final Color DARK_THEME_SIGNAL = new Color(86, 208, 197);
    private static final Color LIGHT_THEME_SIGNAL = new Color(23, 112, 112);

    private FramedCommandButtonStyle() {
    }

    /**
     * @return the standard MekHQ command-button colors for the active Swing theme
     */
    public static ButtonColors getDefaultColors() {
        Color panelColor = getPanelColor();
        Color surfaceColor = getSurfaceColor();
        Color signalColor = getSignalColor();
        Color labelColor = getLabelColor();
        return new ButtonColors(
              new ButtonStateColors(withAlpha(surfaceColor, 190),
                    mix(labelColor, signalColor, 0.18f),
                    getSubtleSignalColor()),
              new ButtonStateColors(mix(surfaceColor, signalColor, 0.16f),
                    signalColor,
                    signalColor),
              new ButtonStateColors(mix(surfaceColor, signalColor, 0.27f),
                    signalColor,
                    signalColor),
              new ButtonStateColors(withAlpha(surfaceColor, 120),
                    mix(panelColor, labelColor, 0.36f),
                    mix(panelColor, signalColor, 0.20f)));
    }

    private static Color getSignalColor() {
        return isDarkTheme() ? DARK_THEME_SIGNAL : LIGHT_THEME_SIGNAL;
    }

    private static Color getSurfaceColor() {
        return mix(getPanelColor(), getLabelColor(), isDarkTheme() ? 0.06f : 0.025f);
    }

    private static Color getSubtleSignalColor() {
        return mix(getPanelColor(), getSignalColor(), isDarkTheme() ? 0.42f : 0.30f);
    }

    private static Color getPanelColor() {
        Color color = UIManager.getColor("Panel.background");
        return (color == null) ? Color.DARK_GRAY : color;
    }

    private static Color getLabelColor() {
        Color color = UIManager.getColor("Label.foreground");
        return (color == null) ? Color.WHITE : color;
    }

    private static boolean isDarkTheme() {
        Color background = getPanelColor();
        double luminance = 0.2126 * background.getRed() +
                                 0.7152 * background.getGreen() +
                                 0.0722 * background.getBlue();
        return luminance < 128;
    }

    private static Color mix(Color firstColor, Color secondColor, float secondColorWeight) {
        float firstColorWeight = 1.0f - secondColorWeight;
        int red = Math.round(firstColor.getRed() * firstColorWeight + secondColor.getRed() * secondColorWeight);
        int green = Math.round(firstColor.getGreen() * firstColorWeight + secondColor.getGreen() * secondColorWeight);
        int blue = Math.round(firstColor.getBlue() * firstColorWeight + secondColor.getBlue() * secondColorWeight);
        return new Color(red, green, blue);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** Colors for one command-button interaction state. */
    public record ButtonStateColors(Color background, Color foreground, Color frame) {
    }

    /** Colors for all command-button interaction states. */
    public record ButtonColors(ButtonStateColors idle, ButtonStateColors active,
          ButtonStateColors pressed, ButtonStateColors disabled) {
    }
}
