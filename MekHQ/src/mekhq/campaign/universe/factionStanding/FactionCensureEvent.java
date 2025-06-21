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

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.dialog.factionStanding.FactionCensureDialog;

public class FactionCensureEvent {
    private final Campaign campaign;
    private final FactionCensureLevel censureLevel;
    private final Person mostSeniorCharacter;
    private final int choiceIndex;

    private final static int GO_ROGUE_DIALOG_CHOICE_INDEX = 3;
    private final static int SEPPUKU_DIALOG_CHOICE_INDEX = 4;

    public FactionCensureEvent(Campaign campaign, FactionCensureLevel censureLevel) {
        this.campaign = campaign;
        this.censureLevel = censureLevel;
        mostSeniorCharacter = getMostSeniorCharacter();

        FactionCensureDialog dialog = new FactionCensureDialog(campaign, censureLevel, mostSeniorCharacter);
        choiceIndex = dialog.getDialogChoiceIndex();

        if (choiceIndex == GO_ROGUE_DIALOG_CHOICE_INDEX) {
            // TODO GO ROGUE WOOOO REBEL TIME
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
        if (choiceIndex == GO_ROGUE_DIALOG_CHOICE_INDEX) {
            // TODO GO ROGUE WOO REBEL TIME
            return;
        } else {
            if (mostSeniorCharacter == null) {
                return;
            }

            mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.RETIRED);
        }
    }

    private void processCensureCommanderImprisonment() {
        if (choiceIndex == GO_ROGUE_DIALOG_CHOICE_INDEX) {
            // TODO GO ROGUE WOO REBEL TIME
            return;
        } else {
            if (mostSeniorCharacter == null) {
                return;
            }

            mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.IMPRISONED);
        }
    }

    private void processCensureLeadershipReplacement() {

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

        return person.isDependent();
    }
}
