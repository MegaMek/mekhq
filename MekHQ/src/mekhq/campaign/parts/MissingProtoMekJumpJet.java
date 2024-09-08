/*
 * MissingProtomekJumpJet.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import org.w3c.dom.Node;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.ProtoMek;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingProtoMekJumpJet extends MissingPart {
    public MissingProtoMekJumpJet() {
        this(0, null);
    }

    public MissingProtoMekJumpJet(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "ProtoMek Jump Jet";
    }

    @Override
    public int getBaseTime() {
        return 60;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public double getTonnage() {
        if (getUnitTonnage() <=5) {
            return 0.05;
        } else {
            return 0.1;
        }
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {

    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            int damageJJ = getOtherDamagedJumpJets() + 1;
            if (damageJJ < (int) Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO, 2);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.isLocationBreached(ProtoMek.LOC_TORSO)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_TORSO) + " is breached.";
        }
        if (unit.isLocationDestroyed(ProtoMek.LOC_TORSO)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_TORSO) + " is destroyed.";
        }
        return null;
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            remove(false);
            //assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof ProtomekJumpJet
                && getUnitTonnage() == ((ProtomekJumpJet) part).getUnitTonnage();
    }

    @Override
    public Part getNewPart() {
        return new ProtomekJumpJet(getUnitTonnage(), campaign);
    }

    private int getOtherDamagedJumpJets() {
        int damagedJJ = 0;
        if (null != unit) {
            for (Part p : unit.getParts()) {
                if (p.getId() == this.getId()) {
                    continue;
                }
                if (p instanceof MissingProtoMekJumpJet
                        || (p instanceof ProtomekJumpJet && ((ProtomekJumpJet) p).needsFixing())) {
                    damagedJJ++;
                }
            }
        }
        return damagedJJ;
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtomekJumpJet.TECH_ADVANCEMENT;
    }

}
