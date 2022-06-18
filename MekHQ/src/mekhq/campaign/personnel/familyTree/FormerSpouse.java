/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.familyTree;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.io.idReferenceClasses.PersonIdReference;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;

public class FormerSpouse {
    //region Variables
    private Person formerSpouse;
    private LocalDate date;
    private FormerSpouseReason reason;
    //endregion Variables

    //region Constructors
    /**
     * This creates an empty FormerSpouse object
     * This case should only be used for reading from XML
     */
    private FormerSpouse() {

    }

    /**
     * @param formerSpouse the new former spouse
     * @param date the date the person became a former spouse
     * @param reason the reason the person is a former spouse
     */
    public FormerSpouse(final Person formerSpouse, final LocalDate date,
                        final FormerSpouseReason reason) {
        setFormerSpouse(formerSpouse);
        setDate(date);
        setReason(reason);
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * @return the former spouse
     */
    public Person getFormerSpouse() {
        return formerSpouse;
    }

    /**
     * @param formerSpouse the former spouse to set this
     */
    public void setFormerSpouse(final Person formerSpouse) {
        this.formerSpouse = formerSpouse;
    }

    /**
     * @return the date the person became a former spouse
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * @param date the date the person became a former spouse
     */
    public void setDate(final LocalDate date) {
        this.date = date;
    }

    /**
     * @return the reason the person became a former spouse
     */
    public FormerSpouseReason getReason() {
        return reason;
    }

    /**
     * @param reason the reason the person became a former spouse
     */
    public void setReason(final FormerSpouseReason reason) {
        this.reason = reason;
     }
    //endregion Getters/Setters

    //region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "formerSpouse");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", getFormerSpouse().getId());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "date", getDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reason", getReason().name());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "formerSpouse");
    }

    public static FormerSpouse generateInstanceFromXML(final Node wn) throws Exception {
        final FormerSpouse formerSpouse = new FormerSpouse();
        final NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            switch (wn2.getNodeName()) {
                case "id":
                    formerSpouse.setFormerSpouse(new PersonIdReference(wn2.getTextContent().trim()));
                    break;
                case "date":
                    formerSpouse.setDate(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                    break;
                case "reason":
                    formerSpouse.setReason(FormerSpouseReason.parseFromText(wn2.getTextContent().trim()));
                    break;
                default:
                    break;
            }
        }

        return formerSpouse;
    }
    //endregion File I/O

    /**
     * @return a string in the format {{ Reason }}: {{ Full Title }} ({{ Date }})
     */
    @Override
    public String toString() {
        return getReason() + ": " + getFormerSpouse().getFullTitle() + " ("
                + MekHQ.getMHQOptions().getDisplayFormattedDate(getDate()) + ')';
    }

    /**
     * This equal does not demand uniqueness
     * @param object the object to compare to the former spouse
     * @return true if they are equal, otherwise false
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof FormerSpouse)) {
            return false;
        } else {
            final FormerSpouse formerSpouse = (FormerSpouse) object;
            return getFormerSpouse().equals(formerSpouse.getFormerSpouse())
                    && getDate().isEqual(formerSpouse.getDate())
                    && (getReason() == formerSpouse.getReason());
        }
    }

    @Override
    public int hashCode() {
        return (getFormerSpouse().getId().toString() + getDate() + getReason()).hashCode();
    }
}
