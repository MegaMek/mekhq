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
package mekhq.campaign.market.personnelMarket.markets;

import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.CAMPAIGN_OPERATIONS_REVISED;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.market.personnelMarket.records.PersonnelMarketEntry;
import mekhq.campaign.market.personnelMarket.yaml.PersonnelMarketLibraries;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Implements the personnel market logic using the Campaign Operations Revised ruleset.
 *
 * <p>Specializes the new personnel market by defining applicant pool sources, recruitment roll calculations, and
 * market data initialization based on revised campaign operations standards.</p>
 *
 * <ul>
 *     <li>Initializes market entry data using Campaign Operations Revised logic.</li>
 *     <li>Determines applicant origin factions, taking into account alliances, wars, and mercenary access.</li>
 *     <li>Calculates the number of recruitment rolls based on the calendar month length.</li>
 *     <li>Generates new applicants using the configured market entries for clan or Inner Sphere campaigns.</li>
 * </ul>
 *
 * <p><b>Extends:</b> {@link NewPersonnelMarket}</p>
 * <p><b>Associated Market Style:</b>
 * {@link mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle#CAMPAIGN_OPERATIONS_REVISED}</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PersonnelMarketCamOpsRevised extends NewPersonnelMarket {
    /**
     * Constructs a personnel market using the Campaign Operations Revised rules.
     *
     * <p>Initializes market styles and loads relevant personnel market libraries.</p>
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelMarketCamOpsRevised() {
        super();

        setAssociatedPersonnelMarketStyle(CAMPAIGN_OPERATIONS_REVISED);

        PersonnelMarketLibraries personnelMarketLibraries = new PersonnelMarketLibraries();
        setClanMarketEntries(personnelMarketLibraries.getClanMarketCamOpsRevised());
        setInnerSphereMarketEntries(personnelMarketLibraries.getInnerSphereMarketCamOpsRevised());
    }

    /**
     * Determines the list of factions from which applicants may originate, based on the planetary system's factions,
     * current alliances, and war states.
     *
     * <p>Clan campaigns only consider the campaign's faction.</p>
     *
     * <ul>
     *     <li>Allies are three times as likely to join compared to non-allies.</li>
     *     <li>Excludes factions at war with the campaign faction.</li>
     *     <li>Ensures the mercenary faction is present if others are eligible.</li>
     * </ul>
     *
     * @return a list of possible applicant origin factions
     *
     * @author Illiani
     * @since 0.50.06
     */
    @Override
    public ArrayList<Faction> getApplicantOriginFactions() {
        Set<Faction> systemFactions = getCurrentSystem().getFactionSet(getToday());
        ArrayList<Faction> interestedFactions = new ArrayList<>();

        if (getCampaign().isClanCampaign()) {
            interestedFactions.add(getCampaign().getFaction());
            return interestedFactions;
        }

        Factions factions = Factions.getInstance();
        Faction mercenaryFaction = factions.getFaction(MERCENARY_FACTION_CODE);
        Faction pirateFaction = factions.getFaction(PIRATE_FACTION_CODE);
        FactionStandings factionStandings = getCampaign().getFactionStandings();

        for (Faction faction : systemFactions) {
            if (FactionHints.defaultFactionHints().isAtWarWith(getCampaignFaction(), faction, getToday())) {
                continue;
            }

            int factionStandingMultiplier = 1;
            if (getCampaign().getCampaignOptions().isUseFactionStandingRecruitmentSafe()) {
                double regard = factionStandings.getRegardForFaction(faction.getShortName(), true);
                factionStandingMultiplier = FactionStandingUtilities.getRecruitmentTickets(regard);
            }

            // Allies are three times as likely to join the campaign as non-allies
            if (getCampaignFaction().equals(faction)
                      || FactionHints.defaultFactionHints().isAlliedWith(getCampaignFaction(), faction, getToday())) {
                factionStandingMultiplier *= 3;
            }

            for (int i = 0; i < factionStandingMultiplier; i++) {
                interestedFactions.add(faction);
            }
        }

        if (mercenaryFaction != null &&
                  !interestedFactions.isEmpty() &&
                  !interestedFactions.contains(mercenaryFaction)) {
            interestedFactions.add(mercenaryFaction);
        }

        if (pirateFaction != null &&
                  !interestedFactions.isEmpty() &&
                  !interestedFactions.contains(pirateFaction)) {
            interestedFactions.add(pirateFaction);
        }

        return interestedFactions;
    }

    /**
     * Generates market applicants for the current creation period, using appropriate clan or Inner Sphere entries, and
     * performs a number of rolls determined by the calendar month length.
     *
     * @author Illiani
     * @since 0.50.06
     */
    @Override
    public void generateApplicants() {
        calculateNumberOfRecruitmentRolls();
        Map<PersonnelRole, PersonnelMarketEntry> unorderedMarketEntries = getCampaign().isClanCampaign() ?
                                                                                getClanMarketEntries() :
                                                                                getInnerSphereMarketEntries();
        unorderedMarketEntries = sanitizeMarketEntries(unorderedMarketEntries);
        List<PersonnelMarketEntry> orderedMarketEntries = getMarketEntriesAsList(unorderedMarketEntries);


        for (int roll = 0; roll < getRecruitmentRolls(); roll++) {
            Person applicant = generateSingleApplicant(unorderedMarketEntries, orderedMarketEntries);
            if (applicant != null) {
                addApplicant(applicant);
            }
        }
    }

    /**
     * Calculates the number of recruitment rolls based on the length of the current month. Sets the result as the
     * recruitment roll count for this period.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void calculateNumberOfRecruitmentRolls() {
        int rolls = getToday().getMonth().length(getToday().isLeapYear());

        if (getCampaign().getCampaignOptions().isAllowMonthlyConnections()) {
            int additionalRecruits = performConnectionsRecruitsCheck();
            rolls += additionalRecruits;
        }

        setRecruitmentRolls(rolls);
    }
}
