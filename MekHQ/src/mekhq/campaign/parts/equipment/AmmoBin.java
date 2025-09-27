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
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.Objects;

import megamek.common.CriticalSlot;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Jumpship;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Availability;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class AmmoBin extends EquipmentPart implements IAcquisitionWork {
    private static final MMLogger LOGGER = MMLogger.create(AmmoBin.class);

    protected int shotsNeeded;
    protected boolean oneShot;

    public AmmoBin() {
        this(0, null, -1, 0, false, false, null);
    }

    public AmmoBin(int tonnage, @Nullable AmmoType et, int equipNum, int shotsNeeded, boolean singleShot,
          boolean omniPodded, @Nullable Campaign c) {
        super(tonnage, et, equipNum, 1.0, omniPodded, c);
        this.shotsNeeded = shotsNeeded;
        this.oneShot = singleShot;
        if (name != null) {
            this.name += " Bin";
        }
    }

    @Override
    public AmmoBin clone() {
        AmmoBin clone = new AmmoBin(getUnitTonnage(),
              getType(),
              getEquipmentNum(),
              shotsNeeded,
              oneShot,
              omniPodded,
              campaign);
        clone.copyBaseData(this);
        clone.shotsNeeded = this.shotsNeeded;
        return clone;
    }

    @Override
    public AmmoType getType() {
        return (AmmoType) super.getType();
    }

    /*
     * Per TM, ammo for fighters is stored in the fuselage. This makes a difference for OmniFighter pod space, so we're
     * going to stick them in LOC_NONE where the heat sinks are.
     */
    @Override
    public String getLocationName() {
        if (unit != null &&
                  unit.getEntity() instanceof Aero &&
                  !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))) {
            return "Fuselage";
        }
        return super.getLocationName();
    }

    @Override
    public int getLocation() {
        if (unit != null &&
                  unit.getEntity() instanceof Aero &&
                  !((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))) {
            return Aero.LOC_NONE;
        }
        return super.getLocation();
    }

    @Override
    public double getTonnage() {
        return getFullShots() / (double) getType().getShots();
    }

    public int getFullShots() {
        if (oneShot) {
            return 1;
        }

        int fullShots = getType().getShots();

        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            if (mounted.getOriginalShots() > 0) {
                fullShots = mounted.getOriginalShots();
            }

            if (unit.getEntity() instanceof ProtoMek) {
                // If ProtoMeks are using alternate munitions then cut in half
                if (!EnumSet.of(AmmoType.Munitions.M_STANDARD).containsAll(getType().getMunitionType())) {
                    fullShots = fullShots / 2;
                }
            }
        }

        return fullShots;
    }

    protected int getCurrentShots() {
        return getFullShots() - shotsNeeded;
    }

    public Money getValueNeeded() {
        if ((getShotsPerTon() <= 0) || (shotsNeeded <= 0)) {
            return Money.zero();
        }

        return adjustCostsForCampaignOptions(getPricePerTon().multipliedBy(shotsNeeded).dividedBy(getShotsPerTon()));
    }

    protected Money getPricePerTon() {
        Mounted<?> mounted = getMounted();

        // If on a unit, then use the ammo type on the existing entity,
        // to avoid getting it wrong due to ammo swaps
        EquipmentType curType = (mounted != null) ? mounted.getType() : getType();

        return Money.of(curType.getRawCost());
    }

    protected int getShotsPerTon() {
        AmmoType ammoType = getType();
        if (ammoType.getKgPerShot() > 0) {
            return (int) Math.floor(1000.0 / ammoType.getKgPerShot());
        }

        // if not listed by kg per shot, we assume this is a single ton increment
        return getType().getShots();
    }

    @Override
    public Money getStickerPrice() {
        if (getShotsPerTon() <= 0) {
            return Money.zero();
        }

        return getPricePerTon().multipliedBy(getCurrentShots()).dividedBy(getShotsPerTon());
    }

    @Override
    public Money getBuyCost() {
        return getNewPart().getActualValue();
    }

    public int getShotsNeeded() {
        return ammoTypeChanged() ? getFullShots() : shotsNeeded;
    }

    public boolean canChangeMunitions(final AmmoType type) {
        return getType().equalsAmmoTypeOnly(type) && (getType().getRackSize() == type.getRackSize());
    }

    public void changeMunition(final AmmoType type) {
        this.type = type;
        this.name = type.getName();
        this.typeName = type.getInternalName();
        updateConditionFromEntity(false);
    }

    protected boolean ammoTypeChanged() {
        Mounted<?> mounted = getMounted();
        return (mounted != null) && !getType().equals(mounted.getType());
    }

    @Override
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        // CAW: InfantryAmmoBin may have negative shots needed
        if (shotsNeeded != 0) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shotsNeeded", shotsNeeded);
        }

        if (oneShot) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "oneShot", true);
        }

        super.writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("shotsNeeded")) {
                    shotsNeeded = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("oneShot")) {
                    oneShot = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception ex) {
                LOGGER.error("", ex);
            }
        }

        super.loadFieldsFromXmlNode(wn);
    }

    @Override
    public String getStatus() {
        String toReturn = "Fully Loaded";
        if (shotsNeeded >= getFullShots()) {
            toReturn = "Empty";
        } else if (shotsNeeded > 0) {
            toReturn = "Partially Loaded";
        }
        if (isReservedForRefit()) {
            toReturn += " (Reserved for Refit)";
        }
        return toReturn;
    }

    @Override
    public void fix() {
        loadBin();
    }

    public void loadBin() {
        AmmoMounted mounted = (AmmoMounted) getMounted();
        if (mounted == null) {
            return;
        }

        // Try to remove the ammo needed.
        int shots = requisitionAmmo(getType(), getShotsNeeded());
        if (!ammoTypeChanged()) {
            // just a simple reload
            mounted.setShotsLeft(mounted.getBaseShotsLeft() + shots);
        } else {
            // loading a new type of ammo
            unload();
            mounted.changeAmmoType(getType());
            mounted.setShotsLeft(shots);
        }

        shotsNeeded -= shots;
    }

    /**
     * Gets the underlying {@link Mounted} which manages this {@code AmmoBin} on the {@link Unit}.
     *
     * @return The {@code Mounted} or {@code null} if no valid piece of equipment exists on the {@code Unit}.
     */
    @Override
    protected @Nullable Mounted<?> getMounted() {
        if ((getUnit() != null) && (getUnit().getEntity() != null)) {
            Mounted<?> mounted = getUnit().getEntity().getEquipment(getEquipmentNum());
            if ((mounted != null) && (mounted.getType() instanceof AmmoType)) {
                return mounted;
            }

            LOGGER.warn("Missing valid equipment for {} to manage ammo on unit {}", getName(), getUnit().getName());
        }

        return null;
    }

    /**
     * Requisitions ammo of a given type from the quartermaster.
     *
     * @param ammoType    The {@code AmmoType} being requisitioned.
     * @param shotsNeeded The number of shots needed from the quartermaster.
     *
     * @return The number of shots requisitioned. This may be less than {@code shotsNeeded}.
     */
    protected int requisitionAmmo(AmmoType ammoType, int shotsNeeded) {
        Objects.requireNonNull(ammoType);
        int shotsLoaded = 0;
        while (shotsLoaded < shotsNeeded) {
            int shots = campaign.getQuartermaster().removeAmmo(ammoType, shotsNeeded - shotsLoaded);
            if (shots < 1) {
                return shotsLoaded;
            } else {
                shotsLoaded += shots;
            }
        }

        return shotsLoaded;
    }

    /**
     * Sets the number of shots needed in the {@code AmmoBin}.
     *
     * @param shots The number of shots needed.
     */
    public void setShotsNeeded(int shots) {
        this.shotsNeeded = Math.max(0, shots);
    }

    @Override
    public String find(int transitDays) {
        return "<font color='" +
                     ReportingUtilities.getNegativeColor() +
                     "'> You shouldn't be here (AmmoBin.find()).</font>";
    }

    @Override
    public String failToFind() {
        return "<font color='" +
                     ReportingUtilities.getNegativeColor() +
                     "'> You shouldn't be here (AmmoBin.failToFind()).</font>";
    }

    public void unload() {
        // FIXME: the following won't work for proto and Dropper bins if they
        // are not attached to a unit. Currently the only place AmmoBins are loaded
        // off of units is for refits, which neither of those units can do, but we
        // may want to think about not having refits load ammo bins but rather reserve
        // some AmmoStorage instead if we implement customization of these units
        int shots = getFullShots() - shotsNeeded;

        Mounted<?> mounted = getMounted();
        AmmoType ammoType = (mounted != null) ? ((AmmoType) mounted.getType()) : getType();
        if (mounted != null) {
            shots = mounted.getBaseShotsLeft();
            mounted.setShotsLeft(0);
        }

        shotsNeeded = getFullShots();

        // Return ammo to the campaign
        returnAmmo(ammoType, shots);
    }

    /**
     * Returns ammo unloaded from the bin to the quartermaster.
     *
     * @param ammoType      The {@code AmmoType} unloaded.
     * @param shotsUnloaded The number of shots of ammo unloaded.
     */
    protected void returnAmmo(AmmoType ammoType, int shotsUnloaded) {
        Objects.requireNonNull(ammoType);

        if (shotsUnloaded > 0) {
            getCampaign().getQuartermaster().addAmmo(ammoType, shotsUnloaded);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (salvage) {
            unload();
        }
        super.remove(salvage);

        // We don't keep around ammo bins anymore
        getCampaign().getWarehouse().removePart(this);
    }

    @Override
    public MissingAmmoBin getMissingPart() {
        return new MissingAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(), isOneShot(), omniPodded, campaign);
    }

    public boolean isOneShot() {
        return oneShot;
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        if (isSalvaging()) {
            return super.getAllMods(tech);
        }
        return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "ammo loading");
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            if (mounted.isMissing() || mounted.isDestroyed()) {
                mounted.setShotsLeft(0);
                remove(false);
                return;
            }

            if (getType().equals(mounted.getType())) {
                shotsNeeded = getFullShots() - mounted.getBaseShotsLeft();
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return isOmniPodded() ? 30 : 120;
        }

        Mounted<?> mounted = getMounted();
        if ((mounted != null) && !getType().equals(mounted.getType())) {
            // If we're not the same ammo type as our unit, it takes longer
            // to do work on the AmmoBin.
            return 30;
        }

        return 15;
    }

    @Override
    public int getActualTime() {
        if (isOmniPodded()) {
            return (int) Math.ceil(getBaseTime() * mode.timeMultiplier * 0.5);
        }
        return (int) Math.ceil(getBaseTime() * mode.timeMultiplier);
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return -2;
        }
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            mounted.setHit(false);
            mounted.setDestroyed(false);
            mounted.setRepairable(true);
            getUnit().repairSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
            mounted.setShotsLeft(getFullShots() - shotsNeeded);
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        // AmmoBins are the same type of part if they can hold the same
        // AmmoType and number of rounds of ammo (i.e. they are the same
        // irrespective of "munition type" or "bomb type").
        return (getClass() == part.getClass()) &&
                     ((getType().isCompatibleWith(((AmmoBin) part).getType())) ||
                            (getType().equals(((AmmoBin) part).getType()))) &&
                     (((AmmoBin) part).getFullShots() == getFullShots());
    }

    @Override
    public boolean needsFixing() {
        return (shotsNeeded > 0) || ammoTypeChanged();
    }

    @Override
    public String getDesc() {
        if (isSalvaging()) {
            return super.getDesc();
        }

        return "<html>" +
                     "<b>Reload " +
                     getName() +
                     "</b><br/>" +
                     getDetails() +
                     "<br/>" +
                     getTimeLeft() +
                     " minutes" +
                     (null != getTech() ? " (scheduled) " : "") +
                     "</html>";
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (isSalvaging()) {
            return super.getDetails(includeRepairDetails);
        }
        if (null != unit) {
            int shotsAvailable = getAmountAvailable();
            PartInventory inventories = campaign.getPartInventory(getNewPart());

            StringBuilder toReturn = new StringBuilder();
            toReturn.append(getType().getDesc())
                  .append(", ")
                  .append(getShotsNeeded())
                  .append(" shots needed")
                  .append("<br/>");

            if (shotsAvailable == 0) {
                toReturn.append(ReportingUtilities.messageSurroundedBySpanWithColor(MekHQ.getMHQOptions()
                                                                                          .getFontColorNegativeHexColor(),
                      "None in stock"));
            } else if (shotsAvailable < getShotsNeeded()) {
                toReturn.append(ReportingUtilities.spanOpeningWithCustomColor(MekHQ.getMHQOptions()
                                                                                    .getFontColorNegativeHexColor()))
                      .append("Only ")
                      .append(shotsAvailable)
                      .append(" in stock")
                      .append(ReportingUtilities.CLOSING_SPAN_TAG);
            } else {
                toReturn.append(ReportingUtilities.spanOpeningWithCustomColor(MekHQ.getMHQOptions()
                                                                                    .getFontColorPositiveHexColor()))
                      .append(shotsAvailable)
                      .append(" in stock")
                      .append(ReportingUtilities.CLOSING_SPAN_TAG);
            }

            String orderTransitString = inventories.getTransitOrderedDetails();
            if (!orderTransitString.isEmpty()) {
                toReturn.append(ReportingUtilities.spanOpeningWithCustomColor(MekHQ.getMHQOptions()
                                                                                    .getFontColorWarningHexColor()))
                      .append(" (")
                      .append(orderTransitString)
                      .append(")")
                      .append(ReportingUtilities.CLOSING_SPAN_TAG);
            }
            return toReturn.toString();
        } else {
            return "";
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (!isSalvaging() && (getAmountAvailable() == 0)) {
            return "No ammo of this type is available";
        } else if (null == unit) {
            return "Ammo bins can only be loaded when installed on units";
        } else {
            return null;
        }
    }

    public int getAmountAvailable() {
        return campaign.getQuartermaster().getAmmoAvailable(getType());
    }

    public boolean isEnoughSpareAmmoAvailable() {
        return getAmountAvailable() >= getShotsNeeded();
    }

    @Override
    public String getAcquisitionDesc() {
        String toReturn = "<html><font";

        toReturn += ">";
        toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
        toReturn += getAcquisitionExtraDesc() + "<br/>";
        PartInventory inventories = campaign.getPartInventory(getAcquisitionPart());
        toReturn += inventories.getTransitOrderedDetails() + "<br/>";
        toReturn += getBuyCost().toAmountAndSymbolString() + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String getAcquisitionDisplayName() {
        return getType().getDesc();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return getType().getShots() + " shots (1 ton)";
    }

    @Override
    public String getAcquisitionBonus() {
        String bonus = getAllAcquisitionMods().getValueAsString();
        if (getAllAcquisitionMods().getValue() > -1) {
            bonus = "+" + bonus;
        }

        return "(" + bonus + ")";
    }

    @Override
    public Part getAcquisitionPart() {
        return getNewPart();
    }

    @Override
    public String getAcquisitionName() {
        return getType().getDesc();
    }

    @Override
    public TargetRoll getAllAcquisitionMods() {
        TargetRoll target = new TargetRoll();
        // Faction and Tech mod
        if (isClanTechBase() && campaign.getCampaignOptions().getClanAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getClanAcquisitionPenalty(), "clan-tech");
        } else if (campaign.getCampaignOptions().getIsAcquisitionPenalty() > 0) {
            target.addModifier(campaign.getCampaignOptions().getIsAcquisitionPenalty(), "Inner Sphere tech");
        }
        // availability mod
        AvailabilityValue avail = getAvailability();
        int availabilityMod = Availability.getAvailabilityModifier(avail);
        target.addModifier(availabilityMod, "availability (" + avail.getName() + ")");
        return target;
    }

    @Override
    public AmmoStorage getNewEquipment() {
        return getNewPart();
    }

    public AmmoStorage getNewPart() {
        // Get at least one ton, possibly more, so that when we go
        // to buy ammo for a One Shot bin we don't nickel and dime ourselves.
        int shots = Math.max(getType().getShots(), getFullShots());
        return new AmmoStorage(1, getType(), shots, getCampaign());
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        // FIXME: is this MissingPart or AmmoStorage? Inconsistency between subtypes
        return getNewPart();
    }

    @Override
    public boolean needsMaintenance() {
        return false;
    }

    @Override
    public boolean isPriceAdjustedForAmount() {
        return true;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.AMMUNITION;
    }

    @Override
    public boolean isOmniPoddable() {
        return true;
    }

    /**
     * Since ammo bins aren't real parts they can't be podded in the warehouse, and whether they're podded on the unit
     * depends entirely on the unit they're installed on.
     */
    @Override
    public boolean isOmniPodded() {
        Mounted<?> mounted = getMounted();
        return (mounted != null) && mounted.isOmniPodMounted();
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return getType().getTechAdvancement();
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, Faction techFaction) {
        return getIntroductionDate(clan, techFaction) <= year;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, Faction techFaction) {
        return isExtinct(year, clan, techFaction);
    }
}
