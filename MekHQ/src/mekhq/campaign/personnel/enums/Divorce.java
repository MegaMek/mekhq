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
import mekhq.campaign.personnel.FormerSpouse;
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
        Person spouse = origin.getSpouse();
        int reason = FormerSpouse.REASON_WIDOWED;

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

        if (!(spouse.isDeadOrMIA() && origin.isDeadOrMIA())) {
            reason = FormerSpouse.REASON_DIVORCE;

            PersonalLogger.divorcedFrom(origin, spouse, campaign.getDate());
            PersonalLogger.divorcedFrom(spouse, origin, campaign.getDate());

            campaign.addReport(String.format("%s has divorced %s!", origin.getHyperlinkedName(),
                    spouse.getHyperlinkedName()));

            spouse.setMaidenName(null);
            origin.setMaidenName(null);

            spouse.setSpouseId(null);
            origin.setSpouseId(null);
        } else if (spouse.isDeadOrMIA()) {
            origin.setMaidenName(null);
            origin.setSpouseId(null);
        } else if (origin.isDeadOrMIA()) {
            spouse.setMaidenName(null);
            spouse.setSpouseId(null);
        }

        // Output a message for Spouses who are KIA
        if (reason == FormerSpouse.REASON_WIDOWED) {
            PersonalLogger.spouseKia(spouse, origin, campaign.getDate());
        }

        // Add to former spouse list
        spouse.addFormerSpouse(new FormerSpouse(origin.getId(), campaign.getLocalDate(), reason));
        origin.addFormerSpouse(new FormerSpouse(spouse.getId(), campaign.getLocalDate(), reason));

        MekHQ.triggerEvent(new PersonChangedEvent(origin));
        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
    }

    @Override
    public String toString() {
        return name;
    }
}
