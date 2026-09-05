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
package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.Collection;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.commandGeneration.SupportCarrierDeployment;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

/**
 * The player-facing side of {@link SupportCarrierDeployment}: the senior administrator explains, in character, why
 * support carriers are not going, with the plain detail underneath.
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class SupportCarrierDeploymentDialogs {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SupportPersonnelToTOE";

    private SupportCarrierDeploymentDialogs() {
        // utility class
    }

    /**
     * Tells the player which carriers stayed home when these formations were assigned. Shows nothing if none did.
     *
     * @param campaign   the campaign
     * @param formations the formations that were assigned
     * @param scenario   the scenario they were assigned to
     */
    public static void showStayingHome(Campaign campaign, Collection<Formation> formations,
          @Nullable Scenario scenario) {
        String detail = SupportCarrierDeployment.stayingHomeMessage(campaign, formations, scenario);
        if (detail == null) {
            return;
        }
        String inCharacter = getFormattedTextAt(RESOURCE_BUNDLE, "SupportCarrierDeployment.stayHome.inCharacter",
              campaign.getCommanderAddress());
        new ImmersiveDialogSimple(campaign, seniorAdministrator(campaign), null, inCharacter, null, detail, null,
              false, ImmersiveDialogWidth.SMALL);
    }

    /**
     * Tells the player that what they picked is support staff and nothing was sent.
     *
     * @param campaign the campaign
     * @param name     the name of the formation or carrier the player picked
     */
    public static void showNothingToDeploy(Campaign campaign, String name) {
        String inCharacter = getFormattedTextAt(RESOURCE_BUNDLE,
              "SupportCarrierDeployment.nothingToDeploy.inCharacter", campaign.getCommanderAddress());
        new ImmersiveDialogSimple(campaign, seniorAdministrator(campaign), null, inCharacter, null,
              SupportCarrierDeployment.nothingToDeployMessage(name), null, false, ImmersiveDialogWidth.SMALL);
    }

    private static @Nullable Person seniorAdministrator(Campaign campaign) {
        return campaign.getPlayerForce().getHumanResources().getSeniorAdminPerson(campaign.getCampaignOptions(),
              campaign.getPlayerForce().isClanForce(), campaign.getLocalDate());
    }
}
