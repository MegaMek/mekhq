/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.missing;

import java.io.PrintWriter;

import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.InfantryArmorPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingInfantryArmorPart extends MissingPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingInfantryArmorPart.class);

    private double damageDivisor;
    private boolean encumbering;
    private boolean spaceSuit;
    private boolean dest;
    private boolean sneak_camo;
    private boolean sneak_ir;
    private boolean sneak_ecm;
    /** The armor kit the platoon needs, or {@code null} for armor described only by its properties. */
    private EquipmentType armorKit;

    @Deprecated(since = "0.51.0", forRemoval = true)
    public MissingInfantryArmorPart() {
        this(0, null, 1.0, false, false, false, false, false, false);
    }

    /**
     * Armor described only by its properties, with no armor kit.
     *
     * @param tonnage       the platoon's tonnage
     * @param campaign      the campaign, or {@code null} while the part is being loaded from a save
     * @param damageDivisor the armor's damage divisor
     * @param isEncumbering {@code true} if the armor is encumbering
     * @param hasDEST       {@code true} for a DEST infiltration suit
     * @param hasSneakCamo  {@code true} for a sneak suit with camouflage
     * @param hasSneakIR    {@code true} for a sneak suit with infrared baffling
     * @param hasSneakECM   {@code true} for a sneak suit with ECM
     * @param isSpaceSuit   {@code true} for a space suit
     */
    public MissingInfantryArmorPart(int tonnage, @Nullable Campaign campaign, double damageDivisor, boolean isEncumbering,
          boolean hasDEST, boolean hasSneakCamo, boolean hasSneakIR, boolean hasSneakECM, boolean isSpaceSuit) {
        this(tonnage, campaign, null, damageDivisor, isEncumbering, hasDEST, hasSneakCamo, hasSneakIR, hasSneakECM,
              isSpaceSuit);
    }

    /**
     * @param tonnage       the platoon's tonnage
     * @param campaign      the campaign, or {@code null} while the part is being loaded from a save
     * @param armorKit      the armor kit the platoon needs, or {@code null} for armor described only by its properties
     * @param damageDivisor the armor's damage divisor
     * @param isEncumbering {@code true} if the armor is encumbering
     * @param hasDEST       {@code true} for a DEST infiltration suit
     * @param hasSneakCamo  {@code true} for a sneak suit with camouflage
     * @param hasSneakIR    {@code true} for a sneak suit with infrared baffling
     * @param hasSneakECM   {@code true} for a sneak suit with ECM
     * @param isSpaceSuit   {@code true} for a space suit
     */
    public MissingInfantryArmorPart(int tonnage, @Nullable Campaign campaign, @Nullable EquipmentType armorKit,
          double damageDivisor, boolean isEncumbering, boolean hasDEST, boolean hasSneakCamo, boolean hasSneakIR,
          boolean hasSneakECM, boolean isSpaceSuit) {
        super(tonnage, campaign);
        this.armorKit = armorKit;
        this.damageDivisor = damageDivisor;
        this.encumbering = isEncumbering;
        this.dest = hasDEST;
        this.sneak_camo = hasSneakCamo;
        this.sneak_ecm = hasSneakECM;
        this.sneak_ir = hasSneakIR;
        this.spaceSuit = isSpaceSuit;
        assignName();
    }

    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    private void assignName() {
        if (armorKit != null) {
            this.name = armorKit.getName();
            return;
        }
        String heavyString = "";
        if (damageDivisor > 1) {
            heavyString = "Heavy ";
        }
        String baseName = "Armor Kit";
        if (isDest()) {
            baseName = "DEST Infiltration Suit";
        } else if (isSneakCamo() || isSneakECM() || isSneakIR()) {
            baseName = "Sneak Suit";
        } else if (isSpaceSuit()) {
            baseName = "Space Suit";
        }

        this.name = heavyString + baseName;
    }

    @Override
    public void updateConditionFromPart() {

    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        return new InfantryArmorPart(getUnitTonnage(), campaign, armorKit, damageDivisor, encumbering, dest,
              sneak_camo, sneak_ir, sneak_ecm, spaceSuit);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof InfantryArmorPart
                     && InfantryArmorPart.isSameKit(armorKit, ((InfantryArmorPart) part).getArmorKit())
                     && damageDivisor == ((InfantryArmorPart) part).getDamageDivisor()
                     && dest == ((InfantryArmorPart) part).isDest()
                     && encumbering == ((InfantryArmorPart) part).isEncumbering()
                     && sneak_camo == ((InfantryArmorPart) part).isSneakCamo()
                     && sneak_ecm == ((InfantryArmorPart) part).isSneakECM()
                     && sneak_ir == ((InfantryArmorPart) part).isSneakIR()
                     && spaceSuit == ((InfantryArmorPart) part).isSpaceSuit();
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public void writeToXML(final PrintWriter printWriter, int indent) {
        indent = writeToXMLBegin(printWriter, indent);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "damageDivisor", damageDivisor);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "dest", dest);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "encumbering", encumbering);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "sneak_camo", sneak_camo);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "sneak_ecm", sneak_ecm);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "sneak_ir", sneak_ir);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "spaceSuit", spaceSuit);
        if (armorKit != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "armorKit", armorKit.getInternalName());
        }
        writeToXMLEnd(printWriter, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node node) {
        NodeList childNodes = node.getChildNodes();

        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);
            try {
                if (childNode.getNodeName().equalsIgnoreCase("damageDivisor")) {
                    damageDivisor = Double.parseDouble(childNode.getTextContent());
                } else if (childNode.getNodeName().equalsIgnoreCase("dest")) {
                    dest = Boolean.parseBoolean(childNode.getTextContent().trim());
                } else if (childNode.getNodeName().equalsIgnoreCase("encumbering")) {
                    encumbering = Boolean.parseBoolean(childNode.getTextContent().trim());
                } else if (childNode.getNodeName().equalsIgnoreCase("sneak_camo")) {
                    sneak_camo = Boolean.parseBoolean(childNode.getTextContent().trim());
                } else if (childNode.getNodeName().equalsIgnoreCase("sneak_ecm")) {
                    sneak_ecm = Boolean.parseBoolean(childNode.getTextContent().trim());
                } else if (childNode.getNodeName().equalsIgnoreCase("sneak_ir")) {
                    sneak_ir = Boolean.parseBoolean(childNode.getTextContent().trim());
                } else if (childNode.getNodeName().equalsIgnoreCase("spaceSuit")) {
                    spaceSuit = Boolean.parseBoolean(childNode.getTextContent().trim());
                } else if (childNode.getNodeName().equalsIgnoreCase("armorKit")) {
                    armorKit = EquipmentType.get(childNode.getTextContent().trim());
                }
            } catch (Exception exception) {
                LOGGER.error("", exception);
            }
        }
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public double getDamageDivisor() {
        return damageDivisor;
    }

    public boolean isDest() {
        return dest;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isEncumbering() {
        return encumbering;
    }

    public boolean isSneakCamo() {
        return sneak_camo;
    }

    public boolean isSneakECM() {
        return sneak_ecm;
    }

    public boolean isSneakIR() {
        return sneak_ir;
    }

    public boolean isSpaceSuit() {
        return spaceSuit;
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
        return Part.TA_GENERIC;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ARMOUR;
    }
}
