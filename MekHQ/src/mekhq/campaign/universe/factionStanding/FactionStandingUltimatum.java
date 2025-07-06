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
package mekhq.campaign.universe.factionStanding;

import java.time.LocalDate;
import java.util.Map;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.factionStanding.FactionStandingUltimatumDialog;

/**
 * Handles the orchestration and presentation of Faction Standing ultimatum events.
 *
 * <p>This class manages the association between significant historical dates and their respective Faction Standing
 * ultimatum scenarios, triggering appropriate in-game dialogs and transitions when such events are detected for the
 * campaign's current faction. It ensures scenarios such as major faction splits or leadership transitions (e.g., the
 * Federated Commonwealth Civil War, ComStar Schism) are detected and surfaced to the player, accompanied by unique
 * dialog sequences involving prominent historical personalities.</p>
 *
 * <p>Key responsibilities include:</p>
 * <ul>
 *     <li>Tracking historically significant faction ultimatum dates and their context.</li>
 *     <li>Providing static checks for whether an ultimatum event is relevant to a given date and faction.</li>
 *     <li>Validating campaign and faction alignment before presenting a scenario dialog to the player.</li>
 *     <li>Constructing {@link Person} representations of event participants for use in dialogs.</li>
 * </ul>
 *
 * <p>See also: {@link FactionStandingUltimatumData} and {@link FactionStandingAgitatorData} for structure of scenario
 * and participant data.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingUltimatum {
    private final Campaign campaign;

    /**
     * Checks whether a faction ultimatum is associated with the given date and matches the specified campaign faction
     * code.
     *
     * <p>This method provides a fast way to determine if a specific campaign faction has an ultimatum on a given date,
     * without requiring initialization of the full {@link FactionStandingUltimatum} class.</p>
     *
     * @param date                the date to check for a faction ultimatum
     * @param campaignFactionCode the faction code to check for association with the ultimatum
     * @param ultimatumsLibrary   the library of ultimatums. Created in {@link Campaign} during initialization
     *
     * @return {@code true} if an ultimatum for the given date matches the specified faction code, {@code false}
     *       otherwise
     */
    public static boolean checkUltimatumForDate(final LocalDate date, final String campaignFactionCode,
          FactionStandingUltimatumsLibrary ultimatumsLibrary) {
        Map<LocalDate, FactionStandingUltimatumData> ultimatums = ultimatumsLibrary.getUltimatums();
        FactionStandingUltimatumData ultimatum = ultimatums.get(date);
        return ultimatum != null && campaignFactionCode.equals(ultimatum.affectedFactionCode());
    }

    /**
     * Initializes and processes a Faction Standing ultimatum event for the specified date and campaign.
     *
     * <p>This constructor checks if a faction ultimatum is present for the given date and whether it applies to the
     * current campaign's faction. If both checks pass, it instantiates and presents a dialog to the player detailing
     * the scenario, complete with participants represented as {@link Person} entities. If no relevant ultimatum is
     * found or the event does not pertain to the campaign's faction, the constructor exits without further action.</p>
     *
     * <p><b>Usage:</b> Should be preceded by a call to
     * {@link #checkUltimatumForDate(LocalDate, String, FactionStandingUltimatumsLibrary)} to avoid needing to pass
     * around a {@link Campaign} object unnecessarily.</p>
     *
     * @param date              the date for which to check and process a Faction Standing ultimatum event
     * @param campaign          the current {@link Campaign} context in which the event occurs
     * @param ultimatumsLibrary the data source containing all available Faction Standing ultimatum events
     *
     * @author Illiani
     * @see #checkUltimatumForDate(LocalDate, String, FactionStandingUltimatumsLibrary)
     * @since 0.50.07
     */
    public FactionStandingUltimatum(final LocalDate date, final Campaign campaign,
          FactionStandingUltimatumsLibrary ultimatumsLibrary) {
        this.campaign = campaign;
        Map<LocalDate, FactionStandingUltimatumData> ultimatums = ultimatumsLibrary.getUltimatums();

        FactionStandingUltimatumData ultimatum = ultimatums.get(date);

        // We should have used 'checkUltimatumForDate' before initializing 'FactionStandingUltimatum', so we should
        // never return here. However, I opted to include this check as added security.
        if (ultimatum == null) {
            return;
        }

        // Or here...
        String affectedFactionCode = ultimatum.affectedFactionCode();
        String campaignFactionCode = campaign.getFaction().getShortName();
        if (!campaignFactionCode.equals(affectedFactionCode)) {
            return;
        }

        // Security checks out of the way, process the ultimatum
        Person challenger = createAgitator(ultimatum.challenger());
        Person incumbent = createAgitator(ultimatum.incumbent());
        new FactionStandingUltimatumDialog(campaign, challenger, incumbent, ultimatum.isViolentTransition());
    }

    /**
     * Creates a {@link Person} entity within the campaign from the provided agitator data.
     *
     * <p>The agitator will receive the specified name, role, and faction code; surname and bloodname fields are set
     * to empty strings. As this information is pushed into the givenName, instead.</p>
     *
     * @param agitator the data record containing the agitator's name, role, and faction code
     *
     * @return a new {@link Person} instance initialized with the specified attributes
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Person createAgitator(FactionStandingAgitatorData agitator) {
        String name = agitator.name();
        PersonnelRole role = agitator.role();
        String factionCode = agitator.factionCode();

        Person person = campaign.newPerson(role, factionCode, Gender.MALE); // Gender is irrelevant here

        person.setGivenName(name);
        person.setSurname("");
        person.setBloodname("");

        return person;
    }
}
