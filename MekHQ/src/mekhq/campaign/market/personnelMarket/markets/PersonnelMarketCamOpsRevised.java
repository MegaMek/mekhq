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

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.personnelMarket.records.PersonnelMarketEntry;
import mekhq.campaign.market.personnelMarket.yaml.PersonnelMarketLibraries;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;

public class PersonnelMarketCamOpsRevised extends NewPersonnelMarket {
    private static final MMLogger logger = MMLogger.create(PersonnelMarketCamOpsRevised.class);

    public PersonnelMarketCamOpsRevised(Campaign campaign) {
        super(campaign);

        logger.debug("Initializing PersonnelMarketCamOpsRevised");

        setAssociatedPersonnelMarketStyle(CAMPAIGN_OPERATIONS_REVISED);

        PersonnelMarketLibraries personnelMarketLibraries = new PersonnelMarketLibraries();
        setClanMarketEntries(personnelMarketLibraries.getClanMarketCamOpsRevised());
        setInnerSphereMarketEntries(personnelMarketLibraries.getInnerSphereMarketCamOpsRevised());
    }

    @Override
    public ArrayList<Faction> getApplicantOriginFactions() {
        Set<Faction> systemFactions = getCurrentSystem().getFactionSet(getToday());
        ArrayList<Faction> interestedFactions = new ArrayList<>();

        if (getCampaign().isClanCampaign()) {
            interestedFactions.add(getCampaign().getFaction());
            return interestedFactions;
        }

        for (Faction faction : systemFactions) {
            if (FactionHints.defaultFactionHints().isAtWarWith(getCampaignFaction(), faction, getToday())) {
                continue;
            }

            // Allies are three times as likely to join the campaign as non-allies
            if (FactionHints.defaultFactionHints().isAlliedWith(getCampaignFaction(), faction, getToday())) {
                interestedFactions.add(faction);
                interestedFactions.add(faction);
            }
            interestedFactions.add(faction);
        }

        Faction mercenaryFaction = Factions.getInstance().getFaction("MERC");
        if (mercenaryFaction != null &&
                  !interestedFactions.isEmpty() &&
                  !interestedFactions.contains(mercenaryFaction)) {
            interestedFactions.add(mercenaryFaction);
        }

        return interestedFactions;
    }

    @Override
    public void generateApplicants() {
        calculateNumberOfRecruitmentRolls();
        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = getCampaign().isClanCampaign() ?
                                                                       getClanMarketEntries() :
                                                                       getInnerSphereMarketEntries();

        for (int roll = 0; roll < getRecruitmentRolls(); roll++) {
            Person applicant = generateSingleApplicant(marketEntries);

            if (applicant != null) {
                addApplicant(applicant);
            }
        }
    }

    private void calculateNumberOfRecruitmentRolls() {
        int rolls = getToday().getMonth().length(getToday().isLeapYear());
        logger.debug("Base rolls: {}", rolls);

        setRecruitmentRolls(rolls);
    }
}
