/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import megamek.common.bays.BayType;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingCubicle;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A transport bay cubicle for a Mek, ProtoMek, vehicle, fighter, or small craft.
 *
 * @author Neoancient
 */
public class Cubicle extends Part {
    private static final MMLogger LOGGER = MMLogger.create(Cubicle.class);

    private BayType bayType;

    public Cubicle() {
        this(0, null, null);
    }

    public Cubicle(int tonnage, BayType bayType, Campaign c) {
        super(tonnage, false, c);
        this.bayType = bayType;
        if (null != bayType) {
            name = bayType.getDisplayName() + " Cubicle";
        }
    }

    public BayType getBayType() {
        return bayType;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECHANICAL);
    }

    @Override
    public String getName() {
        if (null != parentPart) {
            return parentPart.getName() + " Cubicle";
        }
        return super.getName();
    }

    @Override
    public int getBaseTime() {
        // replacement time 1 week
        return 3360;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        // This is handled by the transport bay part to coordinate all the cubicles
    }

    @Override
    public void updateConditionFromPart() {
        // This is handled by the transport bay part to coordinate all the cubicles
    }

    @Override
    public void remove(boolean salvage) {
        // Grab a reference to our parent part so that we don't accidentally NRE
        // when we remove the parent part reference.
        Part parentPart = getParentPart();
        if (null != parentPart) {
            Part spare = getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.changeQuantity(1);
                getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0, false);
            parentPart.removeChildPart(this);
            parentPart.addChildPart(missing);
            parentPart.updateConditionFromPart();
        }
        setUnit(null);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingCubicle(getUnitTonnage(), bayType, campaign);
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        // Per replacement repair tables in SO, cubicles are replaced rather than
        // repaired.
        return false;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    /**
     * Reads the bay type a cubicle was saved with. Saves made before MegaMek renamed its Mek and ProtoMek bay types
     * store the older names, which are translated to {@link BayType#MEK} and {@link BayType#PROTOMEK}. A value that
     * still cannot be read is logged and treated as a Mek bay, so the cubicle loads with its unit and bay rather than
     * broken.
     *
     * @param storedValue the bay type as written in the save
     *
     * @return the bay type, never {@code null}
     */
    public static BayType readStoredBayType(String storedValue) {
        String trimmedValue = storedValue.trim();
        BayType bayType = BayType.parse(trimmedValue);
        if (bayType == null) {
            // the older name only appears in saves made before the rename, so it is matched here and nowhere else
            // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
            bayType = BayType.parse(trimmedValue.toUpperCase().replace("MECH", "MEK"));
        }
        if (bayType == null) {
            LOGGER.error("[Cubicle] Unknown bay type '{}' in the save; treating it as a Mek bay", trimmedValue);
            return BayType.MEK;
        }
        return bayType;
    }

    @Override
    public Money getStickerPrice() {
        return (bayType == null) ? Money.zero() : Money.of(bayType.getCost());
    }

    @Override
    public double getTonnage() {
        return (bayType == null) ? 0 : bayType.getWeight();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof Cubicle) && (((Cubicle) part).getBayType() == bayType);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        if (bayType != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bayType", bayType.toString());
        }
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("bayType")) {
                bayType = readStoredBayType(wn2.getTextContent());
                name = bayType.getDisplayName() + " Cubicle";
            }
        }
    }

    @Override
    public Part clone() {
        Cubicle part = new Cubicle(getUnitTonnage(), bayType, campaign);
        part.copyBaseData(this);
        return part;
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public ITechnology getTechAdvancement() {
        return bayType;
    }
}
