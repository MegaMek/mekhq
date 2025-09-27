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
import java.util.ArrayList;

import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.IArmorState;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.EntityMovementMode;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.BAArmor;
import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingBattleArmorEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingBattleArmorSuit extends MissingPart {
    protected String chassis;
    protected String model;
    protected int trooper;
    protected boolean clan;
    protected boolean quad;
    protected int groundMP;
    protected int jumpMP;
    protected EntityMovementMode jumpType;
    protected int weightClass;

    // It is costly looking up entity, which is used to compare if two suits
    // are the same even if the chassis name doesn't match. So let's save these
    // values if we've already calculated them once.
    private transient boolean entityDetailsCached = false;
    private transient int suitBV;
    private transient int weaponTypeListHash;

    public MissingBattleArmorSuit() {
        super(0, null);
    }

    public MissingBattleArmorSuit(String ch, String m, int ton, int t, int w, int gmp, int jmp, boolean q, boolean clan,
          EntityMovementMode mode, Campaign c) {
        super(ton, c);
        this.chassis = ch;
        this.model = m;
        this.trooper = t;
        this.quad = q;
        this.weightClass = w;
        this.groundMP = gmp;
        this.jumpMP = jmp;
        this.jumpType = mode;
        this.clan = clan;

        this.name = chassis + " " + model + " Suit";
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
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, trooper);
        }
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    public boolean isQuad() {
        return quad;
    }

    public int getWeightClass() {
        return weightClass;
    }

    public int getGroundMP() {
        return groundMP;
    }

    public int getJumpMP() {
        return jumpMP;
    }

    public String getChassis() {
        return chassis;
    }

    public String getModel() {
        return model;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        refreshEntityDetailsCache();
        if (entityDetailsCached) {
            return part instanceof BattleArmorSuit baSuit
                         && getSuitBV() == baSuit.getSuitBV()
                         && getWeaponTypeListHash() == baSuit.getWeaponTypeListHash();
        }

        // If we didn't successfully cache entity details, use the old method for comparing.
        return part instanceof BattleArmorSuit
                     && chassis.equals(((BattleArmorSuit) part).getChassis())
                     && model.equals(((BattleArmorSuit) part).getModel());
    }

    public int getSuitBV() {
        refreshEntityDetailsCache();
        return suitBV;
    }

    public int getWeaponTypeListHash() {
        refreshEntityDetailsCache();
        return weaponTypeListHash;
    }

    @Override
    public Part getNewPart() {
        return new BattleArmorSuit(chassis,
              model,
              getUnitTonnage(),
              -1,
              weightClass,
              groundMP,
              jumpMP,
              quad,
              clan,
              jumpType,
              campaign);
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "BA suit removal");
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    public int getTrooper() {
        return trooper;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "chassis", chassis);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "model", model);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clan", clan);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trooper", trooper);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quad", quad);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "groundMP", groundMP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "jumpMP", jumpMP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "weightClass", weightClass);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "jumpType", jumpType.name());
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("trooper")) {
                trooper = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("groundMP")) {
                groundMP = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("jumpMP")) {
                jumpMP = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("weightClass")) {
                weightClass = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("quad")) {
                quad = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                clan = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("chassis")) {
                chassis = MHQXMLUtility.unEscape(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("model")) {
                model = MHQXMLUtility.unEscape(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("jumpType")) {
                jumpType = EntityMovementMode.parseFromString(MHQXMLUtility.unEscape(wn2.getTextContent().trim()));
            }
        }
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            BattleArmorSuit newSuit = (BattleArmorSuit) replacement.clone();
            // let's also clone the subparts
            unit.addPart(newSuit);
            newSuit.isReplacement(true);
            campaign.getQuartermaster().addPart(newSuit, 0, false);
            newSuit.isReplacement(false);
            newSuit.setTrooper(trooper);
            newSuit.updateConditionFromPart();
            // cycle through MissingBattleArmorEquipmentPart for trooper and replace
            ArrayList<MissingBattleArmorEquipmentPart> missingStuff = new ArrayList<>();
            BAArmor origArmor = null;
            for (Part p : unit.getParts()) {
                if (p instanceof BAArmor && p.getLocation() == trooper) {
                    origArmor = (BAArmor) p;
                }
                if (!(p instanceof MissingBattleArmorEquipmentPart missingBaEquip)) {
                    continue;
                }
                if (missingBaEquip.getTrooper() != trooper) {
                    continue;
                }
                missingStuff.add(missingBaEquip);
            }
            for (Part childPart : replacement.getChildParts()) {
                if (childPart instanceof BAArmor && null != origArmor) {
                    unit.getEntity().setArmor(((BAArmor) childPart).getAmount(), trooper);
                    origArmor.updateConditionFromEntity(false);
                } else if (childPart instanceof BattleArmorEquipmentPart) {
                    for (MissingBattleArmorEquipmentPart p : missingStuff) {
                        if (null != p.getUnit() && p.isAcceptableReplacement(childPart, false)) {
                            //then add child part and remove current part from unit and campaign
                            Part newPart = childPart.clone();
                            unit.addPart(newPart);
                            ((EquipmentPart) newPart).setEquipmentNum(p.getEquipmentNum());
                            ((BattleArmorEquipmentPart) newPart).setTrooper(trooper);
                            p.remove(false);
                            newPart.updateConditionFromPart();
                            break;
                        }
                    }
                }
            }
            replacement.changeQuantity(1);
            unit.getEntity().setInternal(1, trooper);
            remove(false);
        }
    }


    @Override
    public String getLocationName() {
        return null;
    }


    @Override
    public int getLocation() {
        return trooper;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringBuilder toReturn = new StringBuilder();
        if (null != unit) {
            toReturn.append(unit.getEntity().getLocationName(trooper))
                  .append("<br>");
        }
        toReturn.append(super.getDetails(includeRepairDetails));
        return toReturn.toString();
    }

    @Override
    public Part findReplacement(boolean refit) {
        //check to see if we already have a replacement assigned
        if (hasReplacementPart()) {
            return getReplacementPart();
        }
        // don't just return with the first part if it is damaged
        return campaign.getWarehouse().streamSpareParts()
                     .filter(MissingPart::isAvailableAsReplacement)
                     .reduce(null, (bestPart, part) -> {
                         if (isAcceptableReplacement(part, refit)) {
                             if (bestPart == null) {
                                 return part;
                             } else {
                                 int bestPartArmor = 0;
                                 int currentPartArmor = 0;
                                 int bestPartQuantity = 0;
                                 int currentPartQuantity = 0;
                                 for (Part p : bestPart.getChildParts()) {
                                     if (p instanceof BAArmor) {
                                         bestPartArmor = ((BAArmor) p).getAmount();
                                     } else {
                                         bestPartQuantity++;
                                     }
                                 }
                                 for (Part p : part.getChildParts()) {
                                     if (p instanceof BAArmor) {
                                         currentPartArmor = ((BAArmor) p).getAmount();
                                     } else {
                                         currentPartQuantity++;
                                     }
                                 }
                                 if ((currentPartQuantity > bestPartQuantity) || (currentPartArmor > bestPartArmor)) {
                                     return part;
                                 }
                             }
                         }
                         return bestPart;
                     });
    }

    @Override
    public int getIntroductionDate() {
        return getNewPart().getIntroductionDate();
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return BattleArmor.getConstructionTechAdvancement(weightClass);
    }

    private void refreshEntityDetailsCache() {
        if (!entityDetailsCached) {
            mekhq.campaign.parts.utilities.BattleArmorSuitUtility battleArmorSuitUtility
                  = new mekhq.campaign.parts.utilities.BattleArmorSuitUtility(chassis, model);
            if (battleArmorSuitUtility.hasEntity()) {
                suitBV = battleArmorSuitUtility.getBattleArmorSuitBV();
                weaponTypeListHash = battleArmorSuitUtility.getWeaponTypeListHash();
                entityDetailsCached = true;
            }
        }
    }
}
