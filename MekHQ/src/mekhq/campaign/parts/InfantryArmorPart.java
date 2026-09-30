/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.missing.MissingInfantryArmorPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This part represents custom armor kit settings rather than one of the formal armor kits from TacOps.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class InfantryArmorPart extends Part {
    private static final MMLogger LOGGER = MMLogger.create(InfantryArmorPart.class);

    private double damageDivisor;
    private boolean encumbering;
    private boolean spaceSuit;
    private boolean dest;
    private boolean sneak_camo;
    private boolean sneak_ir;
    private boolean sneak_ecm;
    /** The armor kit this part is, or {@code null} for armor described only by its properties. */
    private EquipmentType armorKit;

    @Deprecated(since = "0.51.0", forRemoval = true)
    public InfantryArmorPart() {
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
    public InfantryArmorPart(int tonnage, @Nullable Campaign campaign, double damageDivisor, boolean isEncumbering,
          boolean hasDEST, boolean hasSneakCamo, boolean hasSneakIR, boolean hasSneakECM, boolean isSpaceSuit) {
        this(tonnage, campaign, null, damageDivisor, isEncumbering, hasDEST, hasSneakCamo, hasSneakIR, hasSneakECM,
              isSpaceSuit);
    }

    /**
     * @param tonnage       the platoon's tonnage
     * @param campaign      the campaign, or {@code null} while the part is being loaded from a save
     * @param armorKit      the armor kit this part is, or {@code null} for armor described only by its properties; a kit
     *                      names and prices the part
     * @param damageDivisor the armor's damage divisor
     * @param isEncumbering {@code true} if the armor is encumbering
     * @param hasDEST       {@code true} for a DEST infiltration suit
     * @param hasSneakCamo  {@code true} for a sneak suit with camouflage
     * @param hasSneakIR    {@code true} for a sneak suit with infrared baffling
     * @param hasSneakECM   {@code true} for a sneak suit with ECM
     * @param isSpaceSuit   {@code true} for a space suit
     */
    public InfantryArmorPart(int tonnage, @Nullable Campaign campaign, @Nullable EquipmentType armorKit,
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
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        String details = "";
        if (isEncumbering()) {
            details += "encumbering";
        }

        if (isSneakCamo()) {
            if (!details.isBlank()) {
                details += ", ";
            }
            details += "camo";
        }

        if (isSneakECM()) {
            if (!details.isBlank()) {
                details += ", ";
            }
            details += "ECM";
        }

        if (isSneakIR()) {
            if (!details.isBlank()) {
                details += ", ";
            }
            details += "IR";
        }

        return details;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        //do nothing
    }

    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        //do nothing
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            Part spare = getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                getWarehouse().removePart(this);
            } else if (null != spare) {
                int number = quantity;
                while (number > 0) {
                    spare.changeQuantity(1);
                    number--;
                }
                getWarehouse().removePart(this);
            }
            unit.removePart(this);
        }
        setUnit(null);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingInfantryArmorPart(getUnitTonnage(),
              campaign,
              armorKit,
              damageDivisor,
              encumbering,
              dest,
              sneak_camo,
              sneak_ir,
              sneak_ecm,
              spaceSuit);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return false;
    }

    @Override
    public Money getStickerPrice() {
        if (armorKit != null) {
            // Priced as MegaMek prices the kit for the platoon
            Entity entity = (unit == null) ? null : unit.getEntity();
            return Money.of(armorKit.getCost(entity, false, ConvInfantry.LOC_INFANTRY));
        }
        double price = 0;
        if (damageDivisor > 1) {
            if (isEncumbering()) {
                price += 1600;
            } else {
                price += 4300;
            }
        }
        int nSneak = 0;
        if (isSneakCamo()) {
            nSneak++;
        }
        if (isSneakECM()) {
            nSneak++;
        }
        if (isSneakIR()) {
            nSneak++;
        }

        if (isDest()) {
            price += 50000;
        } else if (nSneak == 1) {
            price += 7000;
        } else if (nSneak == 2) {
            price += 21000;
        } else if (nSneak == 3) {
            price += 28000;
        }

        if (isSpaceSuit()) {
            price += 5000;
        }

        return Money.of(price);
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (getClass() == part.getClass())
                     && isSameKit(armorKit, ((InfantryArmorPart) part).getArmorKit())
                     && damageDivisor == ((InfantryArmorPart) part).getDamageDivisor()
                     && dest == ((InfantryArmorPart) part).isDest()
                     && encumbering == ((InfantryArmorPart) part).isEncumbering()
                     && sneak_camo == ((InfantryArmorPart) part).isSneakCamo()
                     && sneak_ecm == ((InfantryArmorPart) part).isSneakECM()
                     && sneak_ir == ((InfantryArmorPart) part).isSneakIR()
                     && spaceSuit == ((InfantryArmorPart) part).isSpaceSuit();
    }

    public double getDamageDivisor() {
        return damageDivisor;
    }

    /**
     * @return the armor kit this part is, or {@code null} for armor described only by its properties
     */
    public @Nullable EquipmentType getArmorKit() {
        return armorKit;
    }

    /**
     * Makes this part the given armor kit, taking the kit's damage divisor, special properties, name and price, the way
     * MegaMek applies a kit to a platoon. Used for armor parts saved before parts recorded their kit, some of which also
     * had their infrared and ECM sneak properties swapped.
     *
     * @param armorKit the platoon's armor kit
     */
    public void setArmorKit(EquipmentType armorKit) {
        this.armorKit = armorKit;
        if (armorKit instanceof MiscType kit) {
            damageDivisor = kit.getDamageDivisor();
            encumbering = kit.hasFlag(MiscTypeFlag.S_ENCUMBERING);
            spaceSuit = kit.hasFlag(MiscTypeFlag.S_SPACE_SUIT);
            dest = kit.hasFlag(MiscTypeFlag.S_DEST);
            sneak_camo = kit.hasFlag(MiscTypeFlag.S_SNEAK_CAMO);
            sneak_ir = kit.hasFlag(MiscTypeFlag.S_SNEAK_IR);
            sneak_ecm = kit.hasFlag(MiscTypeFlag.S_SNEAK_ECM);
        }
        assignName();
    }

    /**
     * @return {@code true} if both are the same armor kit, or neither is a kit
     */
    public static boolean isSameKit(@Nullable EquipmentType kit, @Nullable EquipmentType otherKit) {
        if ((kit == null) || (otherKit == null)) {
            return kit == otherKit;
        }
        return kit.getInternalName().equals(otherKit.getInternalName());
    }

    public boolean isDest() {
        return dest;
    }

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
                    dest = childNode.getTextContent().equalsIgnoreCase("true");
                } else if (childNode.getNodeName().equalsIgnoreCase("encumbering")) {
                    encumbering = childNode.getTextContent().equalsIgnoreCase("true");
                } else if (childNode.getNodeName().equalsIgnoreCase("sneak_camo")) {
                    sneak_camo = childNode.getTextContent().equalsIgnoreCase("true");
                } else if (childNode.getNodeName().equalsIgnoreCase("sneak_ecm")) {
                    sneak_ecm = childNode.getTextContent().equalsIgnoreCase("true");
                } else if (childNode.getNodeName().equalsIgnoreCase("sneak_ir")) {
                    sneak_ir = childNode.getTextContent().equalsIgnoreCase("true");
                } else if (childNode.getNodeName().equalsIgnoreCase("spaceSuit")) {
                    spaceSuit = childNode.getTextContent().equalsIgnoreCase("true");
                } else if (childNode.getNodeName().equalsIgnoreCase("armorKit")) {
                    armorKit = EquipmentType.get(childNode.getTextContent().trim());
                }
            } catch (Exception exception) {
                LOGGER.error("", exception);
            }
        }
    }

    @Override
    public Part clone() {
        InfantryArmorPart clone = new InfantryArmorPart(getUnitTonnage(),
              campaign,
              armorKit,
              damageDivisor,
              encumbering,
              dest,
              sneak_camo,
              sneak_ir,
              sneak_ecm,
              spaceSuit);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public boolean needsMaintenance() {
        return false;
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
        return TA_GENERIC;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ARMOUR;
    }
}
