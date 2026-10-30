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
package mekhq.campaign.work;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.skills.SkillCheck;

/**
 * The text the procurement screens show for an acquisition target number. A target the dice cannot reach, such as 13+,
 * reads as impossible instead of as an ordinary number, and its tooltip says why, so a player does not wait for a part
 * that can never be found.
 */
public final class AcquisitionTargetText {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ActionCheck";

    private AcquisitionTargetText() {}

    /**
     * @param acquisitionCheck the acquisition check whose target is shown
     *
     * @return the short target text for a table cell, for example {@code 9+}, {@code Impossible (13+)} or
     *       {@code Automatic Failure}
     */
    public static String describeTarget(SkillCheck acquisitionCheck) {
        TargetRoll target = acquisitionCheck.getTargetNumber();
        if (!target.needsRoll()) {
            return target.getValueAsString();
        }
        if (!acquisitionCheck.canSucceed()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "acquisition.target.outOfReach", target.getValue());
        }
        return target.getValue() + "+";
    }

    /**
     * @param acquisitionCheck the acquisition check whose target is shown
     * @param extraNote        a further line to show under the modifiers, or {@code null} for none
     *
     * @return the modifiers behind the target, followed by why it cannot be rolled when the dice cannot reach it, and
     *       then {@code extraNote}; HTML when there is more than one line
     */
    public static String describeModifiers(SkillCheck acquisitionCheck, @Nullable String extraNote) {
        TargetRoll target = acquisitionCheck.getTargetNumber();
        List<String> lines = new ArrayList<>();
        lines.add(target.getDesc());
        if (target.needsRoll() && !acquisitionCheck.canSucceed()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "acquisition.target.outOfReachReason", target.getValue()));
        }
        if (extraNote != null) {
            lines.add(extraNote);
        }
        if (lines.size() == 1) {
            return lines.getFirst();
        }
        return "<html>" + String.join("<br>", lines) + "</html>";
    }
}
