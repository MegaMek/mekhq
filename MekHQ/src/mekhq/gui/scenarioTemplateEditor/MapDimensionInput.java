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
package mekhq.gui.scenarioTemplateEditor;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Validation and parsing for the four free-text map-dimension inputs in {@link ScenarioTemplateEditorDialog} (base
 * width/height and the width/height scaling increments). The dialog previously fed these straight into
 * {@link Integer#parseInt(String)} while saving, so a blank or non-numeric entry threw an uncaught exception that
 * aborted the save with no feedback. This logic is separated from the dialog so it can be exercised without the Swing
 * UI.
 */
public final class MapDimensionInput {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private MapDimensionInput() {
    }

    /**
     * Validates the four map-dimension inputs. Each must be a non-negative whole number (surrounding whitespace is
     * ignored).
     *
     * @param baseWidth              the base width text
     * @param baseHeight             the base height text
     * @param widthScalingIncrement  the scaled width increment text
     * @param heightScalingIncrement the scaled height increment text
     *
     * @return an empty string if every input is valid, otherwise a newline-separated list of problems suitable for
     *       display to the user
     */
    public static String validate(String baseWidth, String baseHeight, String widthScalingIncrement,
          String heightScalingIncrement) {
        StringBuilder errors = new StringBuilder();
        appendIfInvalid(errors, getTextAt(RESOURCE_BUNDLE, "MapDimensionInput.baseWidth"), baseWidth);
        appendIfInvalid(errors, getTextAt(RESOURCE_BUNDLE, "MapDimensionInput.baseHeight"), baseHeight);
        appendIfInvalid(errors, getTextAt(RESOURCE_BUNDLE, "MapDimensionInput.widthIncrement"), widthScalingIncrement);
        appendIfInvalid(errors,
              getTextAt(RESOURCE_BUNDLE, "MapDimensionInput.heightIncrement"),
              heightScalingIncrement);
        return errors.toString();
    }

    private static void appendIfInvalid(StringBuilder errors, String label, String value) {
        if (!isNonNegativeInteger(value)) {
            if (!errors.isEmpty()) {
                errors.append("\n");
            }
            errors.append(getFormattedTextAt(RESOURCE_BUNDLE, "MapDimensionInput.invalid", label));
        }
    }

    private static boolean isNonNegativeInteger(String value) {
        if (value == null) {
            return false;
        }

        try {
            return Integer.parseInt(value.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
