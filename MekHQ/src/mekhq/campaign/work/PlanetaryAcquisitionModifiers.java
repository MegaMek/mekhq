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

import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.TargetRollModifier;
import megamek.common.enums.TechBase;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * The planet's own modifiers to an acquisition roll under Planetary Acquisition: its tech level, industry and output.
 * The daily search adds them for each system it tries, and the screens that show acquisition odds add them for the
 * system the force is in, so a target number shown as 3+ is the one the search rolls against there.
 */
public final class PlanetaryAcquisitionModifiers {
    private static final MMLogger LOGGER = MMLogger.create(PlanetaryAcquisitionModifiers.class);

    private PlanetaryAcquisitionModifiers() {}

    /**
     * @param campaign    the campaign
     * @param acquisition what is being bought
     * @param system      the system the purchase is tried in, or {@code null} when it is not tied to one
     *
     * @return the modifiers buying in that system adds, empty when there is no system
     */
    public static List<TargetRollModifier> forSystem(Campaign campaign, IAcquisitionWork acquisition,
          @Nullable PlanetarySystem system) {
        if (system == null) {
            return List.of();
        }
        boolean isClanItem = acquisition.getTechBase() == TechBase.CLAN;
        return system.getPrimaryPlanet()
                     .getAcquisitionMods(campaign.getLocalDate(), campaign.getCampaignOptions(),
                           campaign.getPlayerForce().getFaction(), isClanItem);
    }

    /**
     * Adds the current system's modifiers to an acquisition check that is only shown to the player, when Planetary
     * Acquisition is on. The daily search tries the current system first.
     *
     * @param campaign    the campaign
     * @param acquisition what is being bought
     * @param check       the check to show
     *
     * @return the same check, with the current system's modifiers when they apply
     */
    public static SkillCheck addForCurrentSystem(Campaign campaign, IAcquisitionWork acquisition, SkillCheck check) {
        return check.withExternalModifiers(forCurrentSystem(campaign, acquisition));
    }

    /**
     * Adds the current system's modifiers to acquisition modifiers that are only shown to the player, when Planetary
     * Acquisition is on. Modifiers that already settle the outcome are left as they are.
     *
     * @param campaign    the campaign
     * @param acquisition what is being bought
     * @param modifiers   the modifiers to show, such as those from {@link Campaign#getAcquisitionModifiers}
     *
     * @return the same modifiers, with the current system's modifiers when they apply
     */
    public static TargetRoll addForCurrentSystem(Campaign campaign, IAcquisitionWork acquisition,
          TargetRoll modifiers) {
        if (modifiers.needsRoll()) {
            forCurrentSystem(campaign, acquisition).forEach(modifiers::addModifier);
        }
        return modifiers;
    }

    private static List<TargetRollModifier> forCurrentSystem(Campaign campaign, IAcquisitionWork acquisition) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_PLANETARY_ACQUISITION)) {
            return List.of();
        }
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        if (currentSystem == null) {
            LOGGER.trace("[Acquisition] {}: no current system, so the shown odds leave out planet modifiers",
                  acquisition.getAcquisitionName());
            return List.of();
        }
        return forSystem(campaign, acquisition, currentSystem);
    }
}
