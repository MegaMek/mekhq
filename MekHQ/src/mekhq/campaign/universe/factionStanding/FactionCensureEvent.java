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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.dialog.factionStanding.FactionCensureDialog;
import mekhq.gui.dialog.factionStanding.SeppukuDialog;

public class FactionCensureEvent {
    private final Campaign campaign;
    private final Person mostSeniorCharacter;

    private final static int GO_ROGUE_DIALOG_CHOICE_INDEX = 3;
    private final static int SEPPUKU_DIALOG_CHOICE_INDEX = 4;

    public FactionCensureEvent(Campaign campaign, FactionCensureLevel censureLevel) {
        this.campaign = campaign;
        mostSeniorCharacter = getMostSeniorCharacter();

        // There is nobody to censure
        if (mostSeniorCharacter == null) {
            return;
        }

        FactionCensureDialog dialog = new FactionCensureDialog(campaign, censureLevel, mostSeniorCharacter);
        int choiceIndex = dialog.getDialogChoiceIndex();

        if (choiceIndex == GO_ROGUE_DIALOG_CHOICE_INDEX) {
            // TODO GO ROGUE WOOOO REBEL TIME
        } else if (choiceIndex == SEPPUKU_DIALOG_CHOICE_INDEX) {
            new SeppukuDialog(campaign, mostSeniorCharacter);
            mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.SEPPUKU);
            return;
        }

        switch (censureLevel) {
            case WARNING -> {}
            case COMMANDER_RETIREMENT -> processCensureCommanderRetirement();
            case COMMANDER_IMPRISONMENT -> processCensureCommanderImprisonment();
            case LEADERSHIP_REPLACEMENT -> processCensureLeadershipReplacement();
            case DISBAND -> processCensureDisband();
        }
    }

    private void processCensureCommanderRetirement() {
        mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.RETIRED);
        processMassLoyaltyChange(false);
    }

    private void processMassLoyaltyChange(boolean isMajor) {
        for (Person remaningPerson : campaign.getPersonnel()) {
            if (remaningPerson.getStatus().isDepartedUnit()) {
                continue;
            }

            if (!remaningPerson.isEmployed()) {
                continue;
            }

            remaningPerson.performForcedDirectionLoyaltyChange(campaign, false, isMajor, false);
        }
    }

    private void processCensureCommanderImprisonment() {
        mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.IMPRISONED);
        processMassLoyaltyChange(true);
    }

    private void processCensureLeadershipReplacement() {
        processMassLoyaltyChange(true);

        List<Force> forces = campaign.getAllForces();

        Set<Person> replacedPersonnel = new HashSet<>();
        replacedPersonnel.add(mostSeniorCharacter);

        for (Force force : forces) {
            UUID commanderId = force.getForceCommanderID();
            if (commanderId == null) {
                continue;
            }

            Person commander = campaign.getPerson(commanderId);
            if (commander != null) {
                replacedPersonnel.add(commander);
            }
        }

        LocalDate today = campaign.getLocalDate();
        for (Person officer : campaign.getPersonnel()) {
            if (isExempt(officer, today)) {
                continue;
            }

            replacedPersonnel.add(officer);
        }

        for (Person seniorPerson : replacedPersonnel) {
            // We shouldn't end up with any exempt people, but in case we do...
            if (isExempt(seniorPerson, today)) {
                continue;
            }

            final int level = seniorPerson.getRankLevel();
            final int rank = seniorPerson.getRankNumeric();

            seniorPerson.changeStatus(campaign, today, PersonnelStatus.SACKED);

            Person replacement = campaign.newPerson(seniorPerson.getPrimaryRole(), seniorPerson.getSecondaryRole());
            replacement.changeRank(campaign, rank, level, false);
            campaign.recruitPerson(replacement, true, true);
        }
    }

    private void processCensureDisband() {

    }

    public Person getMostSeniorCharacter() {
        Person flaggedCommander = campaign.getFlaggedCommander();
        if (flaggedCommander != null) {
            return flaggedCommander;
        }

        LocalDate today = campaign.getLocalDate();

        Collection<Person> personnel = campaign.getPersonnel();
        Person highestRankedPerson = null;
        for (Person person : personnel) {
            if (isExempt(person, today)) {
                continue;
            }

            if (highestRankedPerson == null) {
                highestRankedPerson = person;
                continue;
            }

            if (person.outRanksUsingSkillTiebreaker(campaign, highestRankedPerson)) {
                highestRankedPerson = person;
            }
        }

        return highestRankedPerson;
    }

    private static boolean isExempt(Person person, LocalDate today) {
        if (person.getStatus().isDepartedUnit()) {
            return true;
        }

        if (person.isChild(today)) {
            return true;
        }

        if (!person.isEmployed()) {
            return true;
        }

        if (!person.getPrisonerStatus().isFreeOrBondsman()) {
            return false;
        }

        return person.isDependent();
    }
}
