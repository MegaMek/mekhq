/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.familyTree.FormerSpouse;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum Divorce {
    //region Enum Declarations
    ORIGIN_CHANGE_SURNAME("Divorce.ORIGIN_CHANGE_SURNAME.text"),
    SPOUSE_CHANGE_SURNAME("Divorce.SPOUSE_CHANGE_SURNAME.text"),
    BOTH_CHANGE_SURNAME("Divorce.BOTH_CHANGE_SURNAME.text"),
    KEEP_SURNAME("Divorce.KEEP_SURNAME.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    Divorce(String name) {
        this.name = resources.getString(name);
    }

    //region Divorce
    public void divorce(Person origin, Campaign campaign) {
        Person spouse = origin.getGenealogy().getSpouse();
        FormerSpouseReason reason = FormerSpouseReason.WIDOWED;

        switch (this) {
            case ORIGIN_CHANGE_SURNAME:
                if (origin.getMaidenName() != null) {
                    origin.setSurname(origin.getMaidenName());
                }
                break;
            case SPOUSE_CHANGE_SURNAME:
                if (spouse.getMaidenName() != null) {
                    spouse.setSurname(spouse.getMaidenName());
                }
                break;
            case BOTH_CHANGE_SURNAME:
                if (origin.getMaidenName() != null) {
                    origin.setSurname(origin.getMaidenName());
                }
                if (spouse.getMaidenName() != null) {
                    spouse.setSurname(spouse.getMaidenName());
                }
                break;
            case KEEP_SURNAME:
            default:
                break;
        }

        if (spouse.getStatus().isDeadOrMIA() == origin.getStatus().isDeadOrMIA()) {
            reason = FormerSpouseReason.DIVORCE;

            PersonalLogger.divorcedFrom(origin, spouse, campaign.getLocalDate());
            PersonalLogger.divorcedFrom(spouse, origin, campaign.getLocalDate());

            campaign.addReport(String.format("%s has divorced %s!", origin.getHyperlinkedName(),
                    spouse.getHyperlinkedName()));

            spouse.setMaidenName(null);
            origin.setMaidenName(null);

            spouse.getGenealogy().setSpouse(null);
            origin.getGenealogy().setSpouse(null);
        } else if (spouse.getStatus().isDeadOrMIA()) {
            if (spouse.getStatus().isKIA()) {
                PersonalLogger.spouseKia(spouse, origin, campaign.getLocalDate());
            }
            origin.setMaidenName(null);
            origin.getGenealogy().setSpouse(null);
        } else if (origin.getStatus().isDeadOrMIA()) {
            if (origin.getStatus().isKIA()) {
                PersonalLogger.spouseKia(origin, spouse, campaign.getLocalDate());
            }
            spouse.setMaidenName(null);
            spouse.getGenealogy().setSpouse(null);
        }

        // Add to former spouse list
        spouse.getGenealogy().addFormerSpouse(new FormerSpouse(origin, campaign.getLocalDate(), reason));
        origin.getGenealogy().addFormerSpouse(new FormerSpouse(spouse, campaign.getLocalDate(), reason));

        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
        MekHQ.triggerEvent(new PersonChangedEvent(origin));
    }

    @Override
    public String toString() {
        return name;
    }
}
