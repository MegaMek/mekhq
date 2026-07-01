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

import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.TORTUGA_DOMINIONS_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Displays a confirmation dialog asking the player whether they want to use a randomly generated starting system or
 * fall back to their faction's canonical starting planet.
 *
 * <p>The dialog presents two choices:
 * <ul>
 *   <li><b>Confirm</b> – accept the randomly generated starting system.</li>
 *   <li><b>Decline</b> – revert to the faction's default starting planet,
 *       whose name is shown inline on the button so the player knows exactly
 *       where they will land.</li>
 * </ul>
 *
 * <p>Pirate factions receive special handling: because the generic pirate
 * faction has no meaningful home world, the Tortuga Dominions faction is used
 * as a proxy when resolving the fallback starting planet. If that faction
 * cannot be found at runtime, the original campaign faction is used instead
 * and a warning is logged.
 *
 * <p>This class is a pure static utility; it is not meant to be instantiated.
 */
public class StartingSystemConfirmationDialog {
    private static final MMLogger LOGGER = MMLogger.create(StartingSystemConfirmationDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StartingSystemConfirmationDialog";

    /** Index of the button that confirms the randomly generated starting system. */
    private static final int CONFIRMATION_OPTION = 0;

    /** Index of the button that declines the random system and falls back to the faction default. */
    private static final int DECLINE_OPTION = 1;

    /**
     * Shows the starting-system confirmation dialog and returns the player's choice.
     *
     * <p>An {@link ImmersiveDialogSimple} is constructed with localised display
     * text and two response buttons (confirm / decline). The method blocks until the player dismisses the dialog, then
     * returns {@code true} if they chose to keep the randomly generated starting system.
     *
     * @param campaign the active {@link Campaign}, used to resolve the current date, the campaign faction, and its
     *                 canonical starting planet
     *
     * @return {@code true} if the player confirmed the randomly generated system; {@code false} if they declined and
     *       want the faction default instead
     */
    public static boolean getStartingSystemConfirmationDialog(Campaign campaign) {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              null,
              null,
              getDisplayText(),
              getResponseOptions(campaign),
              null,
              null,
              false);

        return dialog.getDialogChoice() == CONFIRMATION_OPTION;
    }

    /**
     * Returns the localised body text shown in the confirmation dialog.
     *
     * @return the display text string loaded from {@link #RESOURCE_BUNDLE}
     */
    private static String getDisplayText() {
        return getTextAt(RESOURCE_BUNDLE, "StartingSystemConfirmationDialog.text");
    }

    /**
     * Builds the list of localised button labels for the confirmation dialog.
     *
     * @param campaign the active {@link Campaign}, used to resolve the faction and the campaign's current date
     *
     * @return an unmodifiable {@link List} of exactly two button-label strings: {@code [confirmLabel, declineLabel]}
     */
    private static List<String> getResponseOptions(Campaign campaign) {
        Faction faction = getFaction(campaign);
        LocalDate today = campaign.getLocalDate();
        PlanetarySystem startingPlanet = faction.getStartingPlanet(campaign, campaign.getLocalDate());

        String systemName = getTextAt(RESOURCE_BUNDLE, "StartingSystemConfirmationDialog.button.decline.unknown");
        if (startingPlanet != null) {
            systemName = startingPlanet.getName(today);
        }

        String confirmButtonLabel = getTextAt(RESOURCE_BUNDLE, "StartingSystemConfirmationDialog.button.confirm");

        String declineButtonLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "StartingSystemConfirmationDialog.button.decline",
              systemName);

        return List.of(confirmButtonLabel, declineButtonLabel);
    }

    /**
     * Resolves the {@link Faction} to use when determining the canonical fallback starting planet.
     *
     * <p>Pirate factions ({@code PIRATE_FACTION_CODE}) do not have a well-defined home system, so the Tortuga
     * Dominions ({@code TORTUGA_DOMINIONS_FACTION_CODE}) are substituted as a proxy. If the Tortuga Dominions faction
     * cannot be found in the {@link Factions} registry (which should not happen under normal circumstances), a warning
     * is logged and the original campaign faction is returned as a last resort — this will typically resolve to
     * Terra.</p>
     *
     * @param campaign the active {@link Campaign} whose faction is inspected
     *
     * @return the {@link Faction} to use for starting-planet resolution; never {@code null} because the original
     *       campaign faction is always available as a fallback
     */
    private static Faction getFaction(Campaign campaign) {
        Faction campaignFaction = campaign.getFaction();
        String campaignFactionCode = campaignFaction.getShortName();

        if (campaignFactionCode.equals(PIRATE_FACTION_CODE)) {
            return getPirateFaction(campaign);
        }

        return campaignFaction;
    }

    private static Faction getPirateFaction(Campaign campaign) {
        Faction tortugaDominions = Factions.getInstance().getFaction(TORTUGA_DOMINIONS_FACTION_CODE);
        // This conditional should never be true, but in the event it is, we're just going to use the original
        // campaign faction. This will most likely show 'Terra' as the starting system.
        if (tortugaDominions != null) {
            if (tortugaDominions.validIn(campaign.getLocalDate())) {
                return tortugaDominions;
            } else {
                return Factions.getInstance().getDefaultFaction();
            }
        }

        LOGGER.warn("Failed to find Tortuga Dominions faction for campaign start. " +
                          "Falling back to campaign faction.");
        return Factions.getInstance().getDefaultFaction();
    }
}
