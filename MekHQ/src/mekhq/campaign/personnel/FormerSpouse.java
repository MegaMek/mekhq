/*
 * FormerSpouse.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.personnel;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.UUID;

public class FormerSpouse implements Serializable, MekHqXmlSerializable {
    //region Variables
    //mutable private variables
    private UUID formerSpouseId;
    private int reason; //why the spouse became a former spouse
    //constants
    private final static long serialVersionUID = 19161521195L; //spouse in a letter-number cypher
    public final static int REASON_WIDOWED = 0;
    public final static int REASON_DIVORCE = 1;
    //endregion Variables

    //region Constructors
    public FormerSpouse(UUID formerSpouseId, int reason) {
        this.formerSpouseId = formerSpouseId;
        this.reason = reason;
    }
    public FormerSpouse(UUID formerSpouseId) {
        this.formerSpouseId = formerSpouseId;
        reason = REASON_WIDOWED;
    }

    public FormerSpouse() { }
    //endregion Constructors

    //region getters/setters
    public UUID getFormerSpouseId() {
        return formerSpouseId;
    }

    public void setFormerSpouseId(UUID formerSpouseId) {
        this.formerSpouseId = formerSpouseId;
    }

    public int getReason() {
        return reason;
    }

    public String getReasonString(int reason) {
        switch (reason) {
            case REASON_DIVORCE :
                return "Divorce";
            case REASON_WIDOWED:
            default:
                return "Widowed";
        }
    }

    public String getReasonString() {
        return getReasonString(reason);
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public void setReason(String reason) {
        switch (reason) {
            case "Divorce":
                this.reason = REASON_DIVORCE;
                break;
            case "Widowed":
            default:
                this.reason = REASON_WIDOWED;
        }
    }
    //endregion getters/setters

    //region read from/write to XML
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(String.format("%s<formerSpouse id=\"%s\">", MekHqXmlUtil.indentStr(indent), getFormerSpouseId().toString()));
        indent++;
        pw1.println(String.format("%s<id>%s</id>", MekHqXmlUtil.indentStr(indent), getFormerSpouseId().toString()));
        pw1.println(String.format("%s<reason>%s</reason>", MekHqXmlUtil.indentStr(indent), getReasonString()));
        indent--;
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</formerSpouse>");
    }

    public static FormerSpouse generateInstanceFromXML(Node wn) {
        FormerSpouse retVal = null;

        try {
            retVal = new FormerSpouse();

            // Okay, now load FormerSpouse-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.setFormerSpouseId(UUID.fromString(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("reason")) {
                    retVal.setReason(Integer.parseInt((wn2.getTextContent())));
                }
            }
        } catch (Exception e) {
            // Errrr, apparently either the class name was invalid... Or the listed name doesn't exist.
            MekHQ.getLogger().error(FormerSpouse.class, "generateInstanceFromXML(Node,Campaign,Version)", e); //$NON-NLS-1$
        }

        return retVal;
    }
    //endregion read from/write to XML
}
