/*
 * UnitStub.java
 * 
 * Copyright (c) 2018 the MegaMek Team.
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

package mekhq.campaign.force;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Optional;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.util.ImageId;
import mekhq.util.dom.DomProcessor;

/**
 * This is a snapshot of a {@linkplain Unit} object containing just the
 * information needed to display it in a (completed) scenario.
 */
public class UnitStub implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("javadoc")
    public UnitStub(Unit u) {
        desc = getUnitDescription(u);
        Person commander = u.getCommander();
        if (commander != null) {
            portraitId = commander.getPortraitId().orElse(null);
        }
    }

    private UnitStub(String desc, ImageId portraitId) {
        this.desc = desc;
        this.portraitId = portraitId;
    }

    private String desc;
    private ImageId portraitId; // nullable

    @Override
    public String toString() {
        return desc;
    }

    @SuppressWarnings("javadoc")
    public Optional<ImageId> getPortraitId() {
        return Optional.ofNullable(portraitId);
    }

    private static String getUnitDescription(Unit u) {
        String name = "<font color='red'>No Crew</font>";
        String uname = "";
        Person pp = u.getCommander();
        if (null != pp) {
            name = pp.getFullTitle();
            name += " (" + u.getEntity().getCrew().getGunnery() + "/" + u.getEntity().getCrew().getPiloting() + ")";
            if (pp.needsFixing()) {
                name = "<font color='red'>" + name + "</font>";
            }
        }
        uname = "<i>" + u.getName() + "</i>";
        if (u.isDamaged()) {
            uname = "<font color='red'>" + uname + "</font>";
        }
        return "<html>" + name + ", " + uname + "</html>";
    }

    /**
     * Prints something like:
     * 
     * <pre>{@literal
     * <unitStub>
     *     <desc>bla bla bla</desc>
     *     <portraitCategory>some category/</portraitCategory>
     *     <portraitFileName>whatever.jpg</portraitFileName>
     * </unitStub>
     * }</pre> 
     */
    @SuppressWarnings("nls")
    public void printXML(PrintWriter out, int indent) {
        String indent0 = MekHqXmlUtil.indentStr(indent);
        out.println(indent0 + "<unitStub>");
        MekHqXmlUtil.writeSimpleXmlTag(out, indent + 1, "desc", desc);
        if (portraitId != null) {
            MekHqXmlUtil.writeSimpleXmlTag(out, indent + 1, "portraitCategory", portraitId.getCategory());
            MekHqXmlUtil.writeSimpleXmlTag(out, indent + 1, "portraitFileName", portraitId.getFileName());
        }
        out.println(indent0 + "</unitStub>");
    }

    @SuppressWarnings("nls")
    public static UnitStub generateInstanceFromXML(Node wn) {
        final String METHOD_NAME = "generateInstanceFromXML(Node)";
        try {
            
            DomProcessor p = DomProcessor.at((Element) wn);

            String desc = p.text("desc", "?");
            ImageId portraitId = ImageId.cleanupLegacyPortraitId(p.text("portraitCategory", null), p.text("portraitFileName", null)).orElse(null);

            return new UnitStub(desc, portraitId);

        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh! (programmer panic?)
            MekHQ.getLogger().error(UnitStub.class, METHOD_NAME, ex);
            return null;
        }
    }

}
