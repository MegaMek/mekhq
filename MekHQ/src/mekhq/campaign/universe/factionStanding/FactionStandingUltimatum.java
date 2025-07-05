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
import java.time.Month;
import java.util.Map;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.factionStanding.FactionStandingUltimatumDialog;

/**
 * Handles the orchestration and presentation of faction standing ultimatum events.
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
    /**
     * Represents the earliest possible date for when Lord Espinosa overthrew the legitimate heir of the Arano
     * Coalition
     *
     * <p>This date was determined by comparing dates in the House Arano manual, as well as dates in HBS'
     * Battletech.</p>
     */
    private static final LocalDate ESPINOSA_COUP = LocalDate.of(3022, Month.FEBRUARY, 1);
    private static final FactionStandingAgitatorData SANTIAGO_ESPINOSA =
          new FactionStandingAgitatorData("Lord Santiago Espinosa", PersonnelRole.NOBLE, "ARD");
    private static final FactionStandingAgitatorData KAMEA_ARANO =
          new FactionStandingAgitatorData("High Lady Kamea Arano", PersonnelRole.NOBLE, "ARC");

    /**
     * Represents the date marking the beginning of the Federated Commonwealth Civil War.
     *
     * <p>I opted to go with the month Katherine Steiner-Davion chose to secede from the alliance.</p>
     */
    private static final LocalDate FED_COM_CIVIL_WAR = LocalDate.of(3057, Month.SEPTEMBER, 18);
    private static final FactionStandingAgitatorData KATRINA_STEINER =
          new FactionStandingAgitatorData("Archon Katrina Steiner", PersonnelRole.NOBLE, "ARD");
    private static final FactionStandingAgitatorData VICTOR_STEINER_DAVION =
          new FactionStandingAgitatorData("Archon-Prince Victor Steiner-Davion", PersonnelRole.NOBLE, "FS");
    /**
     * Represents the date commemorating the schism within ComStar.
     *
     * <p>Primus Myndo Waterly was killed June 6th 3025, a week later (June 13th) Sharilar Mori was elected as Primus.
     * This caused Precentor Demona Aziz to head to Atreus (FWL) and kick off the ComStar Schism. It's a 100-day journey
     * from Terra to Atreus, which is how we got this date.</p>
     *
     * <p>There is an argument to be made that Precentor Aziz probably could have traveled via command circuit or
     * similar. I think that's very valid, however, I still went with the below date as it factors time needed to get a
     * meeting with Thomas Marik and then to arrange the HPG message that officially starts the schism.</p>
     */
    private static final LocalDate COMSTAR_SCHISM = LocalDate.of(3052, Month.SEPTEMBER, 21);
    private static final FactionStandingAgitatorData DEMONA_AZIZ =
          new FactionStandingAgitatorData("Precentor Demona Aziz", PersonnelRole.RELIGIOUS_LEADER, "WOB");
    private static final FactionStandingAgitatorData ANASTASIUS_FOCHT =
          new FactionStandingAgitatorData("Precentor Martial Anastasius Focht", PersonnelRole.NOBLE, "CS");

    /**
     * A mapping of specific historical dates to their corresponding faction standing ultimatum events.
     *
     * <p>This map associates significant events in faction history with their respective data regarding the faction
     * affected, the challenger, the incumbent leader, and whether the transition is non-violent.</p>
     *
     * <p>Each entry in the map uses a {@link LocalDate} as the key, representing the date of the faction ultimatum,
     * and a {@link FactionStandingUltimatumData} object as the value, capturing the relevant contextual data for the
     * ultimatum event.</p>
     */
    private final static Map<LocalDate, FactionStandingUltimatumData> FACTION_STANDING_ULTIMATUMS = Map.of(
          ESPINOSA_COUP, new FactionStandingUltimatumData("ARC", SANTIAGO_ESPINOSA, KAMEA_ARANO, true),
          FED_COM_CIVIL_WAR, new FactionStandingUltimatumData("FC", KATRINA_STEINER, VICTOR_STEINER_DAVION, false),
          COMSTAR_SCHISM, new FactionStandingUltimatumData("CS", DEMONA_AZIZ, ANASTASIUS_FOCHT, true)
    );

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
     *
     * @return {@code true} if an ultimatum for the given date matches the specified faction code, {@code false}
     *       otherwise
     */
    public static boolean checkUltimatumForDate(final LocalDate date, final String campaignFactionCode) {
        FactionStandingUltimatumData ultimatum = FACTION_STANDING_ULTIMATUMS.get(date);
        return ultimatum != null && campaignFactionCode.equals(ultimatum.affectedFactionCode());
    }

    public FactionStandingUltimatum(final LocalDate date, final Campaign campaign) {
        this.campaign = campaign;

        FactionStandingUltimatumData ultimatum = FACTION_STANDING_ULTIMATUMS.get(date);

        // We should have used 'checkUltimatumForDate' before initializing 'FactionStandingUltimatum', so we should
        // never return here. However, I opted to include this check as added security.
        if (ultimatum == null) {
            return;
        }

        // Or here...
        String affectedFactionCode = FACTION_STANDING_ULTIMATUMS.get(date).affectedFactionCode();
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
