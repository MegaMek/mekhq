/*
 * JumpPath,java
 *
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign;

import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * This is an array list of planets for a jump path, from which we can derive
 * various statistics. We can also add in details about the jump path here, like if
 * the user would like to use recharge stations when available. For XML serialization,
 * this object will need to spit out a list of planet names and then reconstruct
 * the planets from that.
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class JumpPath implements Serializable {
    private static final long serialVersionUID = 708430867050359759L;
    private List<PlanetarySystem> path;

    public JumpPath() {
        path = new ArrayList<>();
    }

    public JumpPath(ArrayList<PlanetarySystem> p) {
        path = p;
    }

    public List<PlanetarySystem> getSystems() {
        return path;
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public PlanetarySystem getFirstSystem() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.get(0);
        }
    }

    public PlanetarySystem getLastSystem() {
        if (path.isEmpty()) {
            return null;
        } else {
            return path.get(path.size() - 1);
        }
    }

    public double getStartTime(double currentTransit) {
        double startTime = 0.0;
        if (null != getFirstSystem()) {
            startTime = getFirstSystem().getTimeToJumpPoint(1.0);
        }
        return startTime - currentTransit;
    }

    public double getEndTime() {
        double endTime = 0.0;
        if (null != getLastSystem()) {
            endTime = getLastSystem().getTimeToJumpPoint(1.0);
        }
        return endTime;
    }

    public double getTotalRechargeTime(LocalDate when) {
        int rechargeTime = 0;
        for (PlanetarySystem system : path) {
            if (system.equals(getFirstSystem())) {
                continue;
            }
            if (system.equals(getLastSystem())) {
                continue;
            }
            rechargeTime += (int) Math.ceil(system.getRechargeTime(when));
        }
        return rechargeTime / 24.0;
    }

    public int getJumps() {
        return size() - 1;
    }

    public double getTotalTime(LocalDate when, double currentTransit) {
        return getTotalRechargeTime(when) + getStartTime(currentTransit) + getEndTime();
    }

    public void addSystem(PlanetarySystem s) {
        path.add(s);
    }

    public void addSystems(List<PlanetarySystem> systems) {
        path.addAll(systems);
    }

    public void removeFirstSystem() {
        if (!path.isEmpty()) {
            path.remove(0);
        }
    }

    public int size() {
        return path.size();
    }

    public PlanetarySystem get(int i) {
        if (i >= size()) {
            return null;
        } else {
            return path.get(i);
        }
    }

    public boolean contains(PlanetarySystem system) {
        return path.contains(system);
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<jumpPath>");
        for (PlanetarySystem p : path) {
            pw1.println(MekHqXmlUtil.indentStr(indent+1)
                    +"<planetName>"
                    +MekHqXmlUtil.escape(p.getId())
                    +"</planetName>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</jumpPath>");
    }

    public static JumpPath generateInstanceFromXML(Node wn, Campaign c) {
        JumpPath retVal = null;

        try {
            retVal = new JumpPath();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("planetName")) {
                    PlanetarySystem p = c.getSystemByName(wn2.getTextContent());
                    if (null != p) {
                        retVal.addSystem(p);
                    } else {
                        MekHQ.getLogger().error("Couldn't find planet named " + wn2.getTextContent());
                    }
                }
            }
        } catch (Exception ex) {
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }
}
