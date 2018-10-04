/*
 * Force.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.force;

import static java.util.stream.Collectors.toList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.util.ForceIconId;
import mekhq.util.dom.DomProcessor;

/**
 * This is a snapshot of a {@linkplain Force} object containing just the
 * information needed to display it in a (completed) scenario.
 */
public class ForceStub implements Serializable {

    private static final long serialVersionUID = 1L;

    public ForceStub(Force force, Campaign c) {
        name        = force.getFullName();
        forceIconId = force.getForceIconId().orElse(null);

        subForces   =  force.getSubForces().stream()
                            .map(sf -> new ForceStub(sf, c))
                            .collect(toList());

        units = force.getUnits().stream()
                     .map(c::getUnit)
                     .filter(Objects::nonNull)
                     .map(UnitStub::new)
                     .collect(toList());
    }

    private ForceStub(String name, ForceIconId forceIconId, List<ForceStub> subForces, List<UnitStub> units) {
        this.name = name;
        this.forceIconId = forceIconId;
        this.subForces = subForces;
        this.units = units;
    }

    private String name;
    private ForceIconId forceIconId; // nullable

    private List<ForceStub> subForces; // treat as immutable (see cachedChildren)
    private List<UnitStub> units;      // treat as immutable (see cachedChildren)

    @SuppressWarnings("javadoc")
    public Optional<ForceIconId> getForceIconId() {
        return Optional.ofNullable(forceIconId);
    }

    private transient List<Object> cachedChildren = null;

    /**
     * @return the concatenation of this force stub's subforces and units
     */
    public List<Object> getAllChildren() {
        if (cachedChildren == null) {
            List<Object> children = new ArrayList<>();
            children.addAll(subForces);
            children.addAll(units);
            cachedChildren = Collections.unmodifiableList(children);
        }
        return cachedChildren;
    }

    /**
     * Prints something like:
     * 
     * <pre>{@literal
     * <forceStub>
     *     <name>bla bla bla</name>
     *     <!-- see ForceIconId for how the icon layers are printed -->
     *     <units>
     *         <!-- see UnitStub for how units are printed -->
     *     </units>
     *     <subforces>
     *         <forceStub>
     *             <!-- recurse nested ForceStub -->
     *         </forceStub>
     *     </subforces>
     * </unitStub>
     * }</pre> 
     */
    @SuppressWarnings("nls")
    public void printXML(PrintWriter out, int indent) {
        String indent0 = MekHqXmlUtil.indentStr(indent);
        String indent1 = MekHqXmlUtil.indentStr(indent + 1);
        
        out.println(indent0 + "<forceStub>");
        out.println(indent1 + "<name>" + MekHqXmlUtil.escape(name) + "</name>");

        if (forceIconId != null) {
            forceIconId.printXML(out, indent + 1);
        }

        if (!units.isEmpty()) {
            out.println(indent1 + "<units>");
            units.forEach(u -> u.printXML(out, indent +2));
            out.println(indent1 + "</units>");
        }

        if (!subForces.isEmpty()) {
            out.println(indent1 + "<subforces>");
            subForces.forEach(u -> u.printXML(out, indent +2));
            out.println(indent1 + "</subforces>");
        }

        out.println(indent0 + "</forceStub>");
    }

    @SuppressWarnings("nls")
    public static ForceStub generateInstanceFromXML(Node wn) {
        final String METHOD_NAME = "generateInstanceFromXML(Node)"; //$NON-NLS-1$
        try {

            DomProcessor p = DomProcessor.at((Element) wn);

            String      name        = p.text("name", "?");
            ForceIconId forceIconId = ForceIconId.fromXML(p).orElse(null);

            List<UnitStub> units = p.child("units").streamChildren("unitStub")
                                    .map(UnitStub::generateInstanceFromXML)
                                    .collect(toList());

            List<ForceStub> subForces = p.child("subforces").streamChildren("forceStub")
                                         .map(ForceStub::generateInstanceFromXML)
                                         .collect(toList());

            return new ForceStub(name, forceIconId, subForces, units);

        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(ForceStub.class, METHOD_NAME, ex);
            return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
