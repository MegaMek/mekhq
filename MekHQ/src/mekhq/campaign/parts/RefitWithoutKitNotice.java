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
package mekhq.campaign.parts;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;

/**
 * Tells the player how beginning a refit with Customize to Model differs from using a refit kit. The refit list grades
 * every refit as kit work, but work without a kit can be a higher class and costs the unit a quality level
 * (Campaign Operations p.211-212).
 *
 * <p>For example, swapping a standard engine for an XL engine is Class D with a refit kit and Class E without one, so
 * a refit the list offers at a field workshop needs factory conditions once it is customized.</p>
 */
public final class RefitWithoutKitNotice {
    private static final MMLogger LOGGER = MMLogger.create(RefitWithoutKitNotice.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    private RefitWithoutKitNotice() {}

    /**
     * Compares the same refit graded as kit work and as custom work.
     *
     * @param kitRefit    the refit as the refit list grades it, with a refit kit
     * @param customRefit the same refit to the same design, begun with Customize to Model
     *
     * @return what changes without a kit, for the player to read, or {@code null} when customizing is the same class
     *       and keeps the unit's quality
     */
    public static @Nullable String describe(Refit kitRefit, Refit customRefit) {
        String unitName = kitRefit.getOriginalUnit().getName();
        boolean isHigherClassWithoutKit = customRefit.getRefitClass() > kitRefit.getRefitClass();
        boolean isLosingQualityWithoutKit = customRefit.isLosingQualityForNoKit();
        if (!isHigherClassWithoutKit && !isLosingQualityWithoutKit) {
            LOGGER.debug("[RefitWithoutKit] {}: customizing is {} like the kit, with no quality loss", unitName,
                  kitRefit.getRefitClassName());
            return null;
        }

        List<String> sentences = new ArrayList<>();
        if (isHigherClassWithoutKit) {
            sentences.add(getFormattedTextAt(RESOURCE_BUNDLE, "RefitWithoutKit.higherClass",
                  customRefit.getRefitClassName(), RefitFacility.describeRequiredSite(customRefit),
                  describeWhatSetsTheClass(customRefit), kitRefit.getRefitClassName()));
        }
        if (isLosingQualityWithoutKit) {
            sentences.add(getTextAt(RESOURCE_BUNDLE,
                  isHigherClassWithoutKit ? "RefitWithoutKit.qualityToo" : "RefitWithoutKit.quality"));
        }
        LOGGER.debug("[RefitWithoutKit] {}: kit refit is {}, customizing is {}, loses quality {}", unitName,
              kitRefit.getRefitClassName(), customRefit.getRefitClassName(), isLosingQualityWithoutKit);
        return String.join(" ", sentences);
    }

    /**
     * @return the changes that set the refit's class, joined for one sentence
     */
    private static String describeWhatSetsTheClass(Refit refit) {
        List<RefitClassGrader.ReasonGroup> groups = refit.getRefitClassReasonGroups();
        if (groups.isEmpty()) {
            return getTextAt(RESOURCE_BUNDLE, "RefitWithoutKit.unknownReason");
        }
        List<String> reasons = new ArrayList<>();
        for (RefitClassGrader.ReasonItem item : groups.getFirst().items()) {
            reasons.add(item.text());
        }
        return String.join("; ", reasons);
    }
}
