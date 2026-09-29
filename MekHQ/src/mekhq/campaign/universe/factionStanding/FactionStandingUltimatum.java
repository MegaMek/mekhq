/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Factions;
import mekhq.gui.dialog.factionStanding.FactionStandingUltimatumDialog;
import mekhq.gui.dialog.factionStanding.FactionStandingUltimatumDialog.UltimatumSide;

/**
 * Handles the orchestration and presentation of Faction Standing ultimatum events.
 *
 * <p>This class manages the association between significant historical dates and their respective Faction Standing
 * ultimatum scenarios, triggering appropriate in-game dialogs and transitions when such events are detected for the
 * campaign's current faction. It ensures scenarios such as major faction splits or leadership transitions (e.g., the
 * Federated Commonwealth Civil War, ComStar Schism) are detected and surfaced to the player, accompanied by unique
 * dialog sequences involving prominent historical personalities.</p>
 *
 * <p>See also: {@link FactionStandingUltimatumData} and {@link FactionStandingUltimatumSide} for structure of scenario
 * and participant data.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public final class FactionStandingUltimatum {
    private FactionStandingUltimatum() {}

    /**
     * Presents the Faction Standing ultimatum for the given date to the player, if the campaign's faction has one.
     *
     * <p>If an ultimatum applies, the agitators are built as {@link Person} entities and the ultimatum dialog is
     * shown; it resolves the player's choice before returning. Otherwise, this method does nothing.</p>
     *
     * @param date              the date to check for a Faction Standing ultimatum event
     * @param campaign          the current {@link Campaign} context in which the event occurs
     * @param ultimatumsLibrary the data source containing all available Faction Standing ultimatum events; may be
     *                          {@code null} if the library failed to load
     *
     * @return {@code true} if an ultimatum was presented
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean processUltimatum(final LocalDate date, final Campaign campaign,
          @Nullable FactionStandingUltimatumsLibrary ultimatumsLibrary) {
        String campaignFactionCode = campaign.getPlayerForce().getFaction().getShortName();
        FactionStandingUltimatumData ultimatum = findUltimatum(date, campaignFactionCode, ultimatumsLibrary);
        if (ultimatum == null) {
            return false;
        }

        Factions factions = Factions.getInstance();
        List<UltimatumSide> sides = new ArrayList<>();
        for (FactionStandingUltimatumSide side : ultimatum.sides()) {
            sides.add(new UltimatumSide(side.id(), createAgitator(campaign, side),
                  factions.getFaction(side.factionCode())));
        }

        new FactionStandingUltimatumDialog(campaign,
              sides,
              ultimatum.dissenterPreference(),
              ultimatum.isViolentTransition(),
              ultimatum.divisiveness(),
              ultimatum.name(),
              date);
        return true;
    }

    /**
     * Finds the ultimatum, if any, that the given faction receives on the given date.
     *
     * @param date                the date to check
     * @param campaignFactionCode the campaign's current faction code
     * @param ultimatumsLibrary   the library of ultimatums; may be {@code null} if it failed to load
     *
     * @return the matching ultimatum, or {@code null} if there is none
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable FactionStandingUltimatumData findUltimatum(final LocalDate date,
          final String campaignFactionCode, @Nullable FactionStandingUltimatumsLibrary ultimatumsLibrary) {
        if (ultimatumsLibrary == null) {
            return null;
        }

        return ultimatumsLibrary.getUltimatum(date, campaignFactionCode);
    }

    /**
     * Creates a {@link Person} entity within the campaign from the provided agitator data.
     *
     * <p>The agitator will receive the specified name, role, and faction code; surname and bloodname fields are set
     * to empty strings. As this information is pushed into the givenName, instead. The person is never added to the
     * campaign, so dialogs show their faction logo rather than a portrait and their gender has no effect.</p>
     *
     * @param campaign the current campaign
     * @param agitator the side whose leader to create, with their name, role, and faction code
     *
     * @return a new {@link Person} instance initialized with the specified attributes
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static Person createAgitator(Campaign campaign, FactionStandingUltimatumSide agitator) {
        Person person = campaign.getPlayerForce()
                              .getHumanResources()
                              .newPerson(campaign, agitator.role(), agitator.factionCode(), Gender.MALE);

        person.setGivenName(agitator.name());
        person.setSurname("");
        person.setBloodname("");

        return person;
    }
}
