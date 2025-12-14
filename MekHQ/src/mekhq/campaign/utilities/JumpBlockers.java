/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities;

import static megamek.common.units.Jumpship.DRIVE_CORE_NONE;
import static mekhq.MHQConstants.CONFIRMATION_ABANDON_UNITS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SpaceStation;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

public class JumpBlockers {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.JumpBlockers";

    /**
     * Determines whether the campaign can proceed with a jump given the current hangar contents.
     *
     * <p>This method identifies units that are considered {@link Jumpship Jumpships} but are not jump-capable (for
     * example, lacking a drive core or otherwise unable to jump). If no such units exist, the jump is allowed
     * immediately. Otherwise, the player is notified and can choose how to proceed (cancel, GM override, or abandon the
     * blocking units).</p>
     *
     * @param campaign the current {@link Campaign}; must not be {@code null}
     *
     * @return {@code true} if the jump should proceed (no blockers, GM override chosen, or blockers abandoned);
     *       {@code false} if the player cancels the jump
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static boolean areAllUnitsJumpCapable(Campaign campaign) {
        final Set<Unit> nonJumpCapableUnits = collectNonJumpCapableUnits(campaign);

        if (nonJumpCapableUnits.isEmpty()) {
            return true;
        } else {
            return notifyPlayerAndProcessResponse(campaign, nonJumpCapableUnits);
        }
    }

    /**
     * Collects all units in the campaign that are classified as {@link Jumpship Jumpships} but are not currently able
     * to jump.
     *
     * <p>A unit is considered a “jump blocker” if:</p>
     * <ul>
     *     <li>Its entity is a {@code Jumpship}, and</li>
     *     <li>It has no drive core ({@code DRIVE_CORE_NONE}) <em>or</em> {@link Jumpship#canJump()} returns
     *     {@code false}.</li>
     * </ul>
     *
     * <p>Some {@link SpaceStation SpaceStations} are treated as exempt and are skipped if they have a KF adapter or
     * are modular.</p>
     *
     * @param campaign the current {@link Campaign}; must not be {@code null}
     *
     * @return a set of units that prevent jumping; empty if none are found
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static Set<Unit> collectNonJumpCapableUnits(Campaign campaign) {
        Set<Unit> nonJumpCapableUnits = new HashSet<>();

        for (Unit unit : campaign.getUnits()) {
            Entity entity = unit.getEntity();
            if (entity instanceof Jumpship jumpship) {
                if (jumpship instanceof SpaceStation spaceStation) {
                    if (spaceStation.hasKFAdapter()) {
                        continue;
                    }

                    if (spaceStation.isModular()) {
                        continue;
                    }
                }

                if (jumpship.getDriveCoreType() == DRIVE_CORE_NONE) {
                    nonJumpCapableUnits.add(unit);
                    continue;
                }

                if (!jumpship.canJump()) {
                    nonJumpCapableUnits.add(unit);
                }
            }
        }

        return nonJumpCapableUnits;
    }

    /**
     * Notifies the player that some units are preventing a jump and processes the chosen resolution.
     *
     * <p>The dialog offers three choices:</p>
     * <ul>
     *     <li><b>Cancel</b> — abort the jump ({@code false}).</li>
     *     <li><b>GM override</b> — proceed with the jump despite blockers ({@code true}).</li>
     *     <li><b>Abandon units</b> — remove all blocking units from the campaign, then proceed ({@code true}). This
     *     option may require confirmation depending on campaign nag/confirmation settings.</li>
     * </ul>
     *
     * @param campaign            the current {@link Campaign}; must not be {@code null}
     * @param nonJumpCapableUnits the set of units preventing a jump; must not be {@code null}
     *
     * @return {@code true} if the jump should proceed; {@code false} if the player cancels
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static boolean notifyPlayerAndProcessResponse(Campaign campaign, Set<Unit> nonJumpCapableUnits) {
        String centerMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "JumpBlockers.unableToJump.message.inCharacter", campaign.getCommanderAddress());

        StringJoiner unitDisplay = new StringJoiner(", ");
        for (Unit unit : nonJumpCapableUnits) {
            unitDisplay.add(unit.getName());
        }
        centerMessage += "<p>" + unitDisplay + "</p>";

        final String bottomMessage = getTextAt(RESOURCE_BUNDLE, "JumpBlockers.unableToJump.message.outOfCharacter");

        final String buttonCancel = getTextAt(RESOURCE_BUNDLE, "JumpBlockers.unableToJump.button.cancel");
        final String buttonGM = getTextAt(RESOURCE_BUNDLE, "JumpBlockers.unableToJump.button.gm");
        final String buttonSell = getTextAt(RESOURCE_BUNDLE, "JumpBlockers.unableToJump.button.sell");
        final String buttonAbandon = getTextAt(RESOURCE_BUNDLE, "JumpBlockers.unableToJump.button.abandon");

        final int cancelJumpChoiceIndex = 0;
        final int gmOverrideChoiceIndex = 1;
        final int sellUnits = 2;
        final int abandonUnits = 3;

        int choiceIndex = cancelJumpChoiceIndex;
        boolean wasOverallConfirmed = false;
        while (!wasOverallConfirmed) {
            ImmersiveDialogSimple notice = new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.TRANSPORT),
                  null,
                  centerMessage,
                  List.of(buttonCancel, buttonGM, buttonSell, buttonAbandon),
                  bottomMessage,
                  null,
                  true,
                  ImmersiveDialogWidth.SMALL);

            choiceIndex = notice.getDialogChoice();

            if (choiceIndex == sellUnits || choiceIndex == abandonUnits) {
                if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_ABANDON_UNITS)) {
                    wasOverallConfirmed = new ImmersiveDialogConfirmation(campaign,
                          CONFIRMATION_ABANDON_UNITS).wasConfirmed();
                    continue;
                }
            }

            wasOverallConfirmed = true;
        }

        return switch (choiceIndex) {
            case gmOverrideChoiceIndex -> true;
            case sellUnits -> {
                Quartermaster quartermaster = campaign.getQuartermaster();
                for (Unit unit : nonJumpCapableUnits) {
                    quartermaster.sellUnit(unit);
                }

                yield true;
            }
            case abandonUnits -> {
                for (Unit unit : nonJumpCapableUnits) {
                    campaign.removeUnit(unit.getId());
                }

                yield true;
            }
            default -> false; // Cancel
        };
    }
}
