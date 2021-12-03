/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
import mekhq.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.io.idReferenceClasses.PersonIdReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class FormerSpouse implements Serializable {
    //region Variables
    private static final long serialVersionUID = 3700554959779939695L;

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
    public FormerSpouse(Person formerSpouse, LocalDate date, FormerSpouseReason reason) {
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
    public void setFormerSpouse(Person formerSpouse) {
        this.formerSpouse = Objects.requireNonNull(formerSpouse);
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
    public void setDate(LocalDate date) {
        this.date = Objects.requireNonNull(date);
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
    public void setReason(FormerSpouseReason reason) {
        this.reason = Objects.requireNonNull(reason);
     }
    //endregion Getters/Setters

    //region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenTag(pw, indent++, "formerSpouse");
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "id", getFormerSpouse().getId());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "date", getDate());
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "reason", getReason().name());
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw, --indent, "formerSpouse");
    }

    public static FormerSpouse generateInstanceFromXML(Node wn) {
        FormerSpouse retVal = null;

        try {
            retVal = new FormerSpouse();

            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.setFormerSpouse(new PersonIdReference(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("date")) {
                    retVal.setDate(MekHqXmlUtil.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("reason")) {
                    retVal.setReason(FormerSpouseReason.parseFromText(wn2.getTextContent().trim()));
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }

        return retVal;
    }
    //endregion File I/O

    //region Overrides
    /**
     * @return a string in the format {{ Reason }}: {{ Full Title }} ({{ Date }})
     */
    @Override
    public String toString() {
        return getReason() + ": " + getFormerSpouse().getFullTitle() + " ("
                + MekHQ.getMekHQOptions().getDisplayFormattedDate(getDate()) + ")";
    }

    /**
     * This is ENTIRELY for unit testing and should NOT be used as we do not demand uniqueness
     * @param object the object to compare to the former spouse
     * @return true if they are equal, otherwise false
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof FormerSpouse)) {
            return false;
        }
        final FormerSpouse formerSpouse = (FormerSpouse) object;
        return getFormerSpouse().equals(formerSpouse.getFormerSpouse())
                && getDate().isEqual(formerSpouse.getDate())
                && (getReason() == formerSpouse.getReason());
    }

    @Override
    public int hashCode() {
        return (getFormerSpouse().getId().toString() + getDate() + getReason()).hashCode();
    }
    //endregion Overrides
}
