/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.EquipmentType.T_ARMOR_SV_BAR_2;

import java.io.PrintWriter;
import java.util.Objects;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.TechAdvancement;
import megamek.common.equipment.ArmorType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;

/**
 * Standard support vehicle armor, which can differ by BAR and tech rating.
 */
public class SVArmor extends Armor {
    private static final MMLogger logger = MMLogger.create(SVArmor.class);

    private int bar;
    private TechRating techRating;

    /**
     * Constructor used during campaign deserialization
     */

    public SVArmor() {
        this(2, TechRating.D, 0, Entity.LOC_NONE, null);
    }

    /**
     * Create an instance of a support vehicle armor part
     *
     * @param bar        The Barrier Armor Rating for the armor
     * @param techRating The armor tech rating
     * @param points     The number of points of armor
     * @param loc        The location on the unit
     * @param campaign   The campaign instance
     */
    public SVArmor(int bar, TechRating techRating, int points, int loc, Campaign campaign) {
        super(0, EquipmentType.T_ARMOR_STANDARD, points, loc, false, false, campaign);
        this.bar = bar;
        this.techRating = techRating;
        this.name = String.format("BAR %d armor (%s)", bar, techRating.getName());
    }

    public int getBAR() {
        return bar;
    }

    @Override
    public TechRating getTechRating() {
        return techRating;
    }

    @Override
    public SVArmor clone() {
        SVArmor clone = new SVArmor(getBAR(), getTechRating(), getAmount(), getLocation(), campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        return amount * ArmorType.svArmor(bar).getSVWeightPerPoint(techRating);
    }

    @Override
    public Money getActualValue() {
        return adjustCostsForCampaignOptions(
              Money.of(amount * ArmorType.svArmor(bar).getCost()));
    }

    @Override
    public double getTonnageNeeded() {
        return amountNeeded * ArmorType.svArmor(bar).getSVWeightPerPoint(techRating);
    }

    @Override
    public Money getValueNeeded() {
        return adjustCostsForCampaignOptions(
              Money.of(amountNeeded * ArmorType.svArmor(bar).getCost()));
    }

    @Override
    public Money getStickerPrice() {
        // The value of '< T_ARMOR_SV_BAR_2' means that the armor does not exist at that tech level
        // (or it is not SV BAR armor).
        if (bar < T_ARMOR_SV_BAR_2) {
            return Money.zero();
        }

        // always in 5-ton increments
        double weightPerPoint = ArmorType.svArmor(bar).getSVWeightPerPoint(techRating);
        double calculatedAmount = 5.0 / weightPerPoint * ArmorType.svArmor(bar).getCost();
        return Money.of(calculatedAmount);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (getClass() == part.getClass())
                     && (bar == ((SVArmor) part).bar)
                     && (techRating == ((SVArmor) part).techRating);
    }

    @Override
    public double getArmorWeight(int points) {
        return points * ArmorType.svArmor(bar).getSVWeightPerPoint(techRating);
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return new SVArmor(bar, techRating, (int) Math.round(5.0 / getArmorPointsPerTon()), -1, campaign);
    }

    @Override
    public double getArmorPointsPerTon() {
        return 1.0 / ArmorType.svArmor(bar).getSVWeightPerPoint(techRating);
    }

    @Override
    public Part getNewPart() {
        return new SVArmor(bar, techRating, (int) Math.round(5 * getArmorPointsPerTon()), -1, campaign);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ArmorType.svArmor(bar).getTechAdvancement();
    }

    @Override
    public int getAmountAvailable() {
        return campaign.getWarehouse()
                     .streamSpareParts()
                     .filter(this::isSameSVArmorPart)
                     .mapToInt(part -> ((SVArmor) part).getAmount())
                     .sum();
    }

    @Override
    protected int changeAmountAvailableSingle(int amount) {
        SVArmor armor = (SVArmor) campaign.getWarehouse().findSparePart(part -> {
            return isSamePartType(part) && part.isPresent() && Objects.equals(getRefitUnit(), part.getRefitUnit());
        });

        if (null != armor) {
            int amountRemaining = armor.getAmount() + amount;
            armor.setAmount(amountRemaining);
            if (armor.getAmount() <= 0) {
                campaign.getWarehouse().removePart(armor);
                return Math.min(0, amountRemaining);
            }
        } else if (amount > 0) {
            campaign.getQuartermaster().addPart(new SVArmor(bar, techRating, amount, -1, campaign), 0, false);
        }
        return 0;
    }

    @Override
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bar", bar);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techRating", techRating.getName());
        super.writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node node) {
        super.loadFieldsFromXmlNode(node);
        for (int x = 0; x < node.getChildNodes().getLength(); x++) {
            final Node wn = node.getChildNodes().item(x);
            try {
                switch (wn.getNodeName()) {
                    case "bar":
                        bar = Integer.parseInt(wn.getTextContent());
                        break;
                    case "techRating":
                        techRating = TechRating.fromName(wn.getTextContent());
                        break;
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    /**
     * Not sure how true this title is, it was used in {@link SVArmor#getAmountAvailable}
     *
     * @param part is this part the same
     *
     * @return true if the two parts are the same, at least as far as {@link SVArmor#getAmountAvailable} is concerned
     */
    private boolean isSameSVArmorPart(Part part) {
        return (part instanceof SVArmor armor) &&
                     armor.isPresent() &&
                     !armor.isReservedForRefit() &&
                     isClanTechBase() == part.isClanTechBase() &&
                     isSamePartType(armor);
    }
}
