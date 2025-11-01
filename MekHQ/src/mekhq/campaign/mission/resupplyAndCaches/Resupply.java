/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.resupplyAndCaches;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.common.enums.SkillLevel.NONE;
import static megamek.common.equipment.MiscType.F_SPONSON_TURRET;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.force.ForceType.CONVOY;
import static mekhq.campaign.force.ForceType.STANDARD;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.EntityUtilities.getEntityFromUnitId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.PartsInUseManager;
import mekhq.campaign.market.procurement.Procurement;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * The {@code Resupply} class manages the resupply process during a campaign. It calculates the required resupply
 * resources, organizes parts pools (e.g., parts, armor, ammo), and handles player convoy logistics and negotiation
 * skills.
 * <p>
 * It supports functionality such as - Calculating target resupply tonnage based on combat unit weight and allowances. -
 * Building pools for parts (e.g., spare parts, ammo, and armor). - Managing player convoy logistics. - Handling
 * procurement and negotiation results for administered resources.
 */
public class Resupply {
    private final Campaign campaign;
    private final AtBContract contract;
    private final ResupplyType resupplyType;
    private final Faction employerFaction;
    private final int currentYear;
    private List<Part> ammoBinPool;
    private double focusAmmo;
    private List<Part> armorPool;
    private double focusArmor;
    private List<Part> partsPool;
    private double focusParts;
    private boolean usePlayerConvoy;
    private Map<Force, Double> playerConvoys;
    private final int targetCargoTonnage;
    private final int targetCargoTonnagePlayerConvoy;
    private double totalPlayerCargoCapacity;
    private int negotiatorSkill;
    private List<Part> convoyContents;
    private Money convoyContentsValueBase;
    private Money convoyContentsValueCalculated;

    public static final int CARGO_MULTIPLIER = 4;
    public static final int CARGO_MINIMUM_WEIGHT = 4;
    public static final int RESUPPLY_AMMO_TONNAGE = 1;
    public static final int RESUPPLY_ARMOR_TONNAGE = 5;

    /**
     * Enum representing the various types of resupply methods available during a campaign.
     */
    public enum ResupplyType {
        RESUPPLY_NORMAL, RESUPPLY_LOOT, RESUPPLY_CONTRACT_END, RESUPPLY_SMUGGLER
    }

    /**
     * Constructs a new {@link Resupply} instance and initializes resupply parameters for the given campaign and
     * contract. This includes setting faction data, calculating target cargo tonnage, and building parts pools.
     *
     * @param campaign The current campaign.
     * @param contract The specific contract under which the resupply process is conducted.
     */
    public Resupply(Campaign campaign, AtBContract contract, ResupplyType resupplyType) {
        this.campaign = campaign;
        this.contract = contract;
        this.resupplyType = resupplyType;

        employerFaction = contract.getEmployerFaction();
        usePlayerConvoy = contract.getCommandRights().isIndependent();
        targetCargoTonnage = calculateTargetCargoTonnage(campaign, contract);
        targetCargoTonnagePlayerConvoy = targetCargoTonnage * CARGO_MULTIPLIER;

        currentYear = campaign.getGameYear();

        focusAmmo = 0.25;
        focusArmor = 0.25;
        focusParts = 0.5;

        calculateNegotiationSkill();
        buildPartsPools(collectParts());
        calculatePlayerConvoyValues();

        convoyContents = new ArrayList<>();
        convoyContentsValueBase = Money.zero();
        convoyContentsValueCalculated = Money.zero();
    }

    /**
     * Retrieves the current campaign.
     *
     * @return A {@link Campaign} representing the current campaign.
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * Retrieves the current contract associated with the resupply operation.
     *
     * @return An {@link AtBContract} representing the current contract.
     */
    public AtBContract getContract() {
        return contract;
    }

    /**
     * Retrieves the current resupply type being used. The resupply type indicates the method by which supplies are
     * obtained.
     *
     * @return A {@link ResupplyType} representing the current method of resupply.
     */
    public ResupplyType getResupplyType() {
        return resupplyType;
    }

    /**
     * Retrieves the target cargo tonnage calculated for the resupply process. This value represents the expected
     * tonnage of supplies to be delivered based on the current campaign and contract conditions.
     *
     * @return The target cargo tonnage.
     */
    public int getTargetCargoTonnage() {
        return targetCargoTonnage;
    }

    /**
     * Retrieves the current focus percentage allocated for ammunition resupply.
     *
     * @return The percentage of resupply focus allocated to ammo.
     */
    public double getFocusAmmo() {
        return focusAmmo;
    }

    /**
     * Sets the focus percentage allocated for ammunition resupply.
     *
     * @param focusAmmo The percentage of resupply focus to allocate to ammo.
     */
    public void setFocusAmmo(double focusAmmo) {
        this.focusAmmo = focusAmmo;
    }

    /**
     * Retrieves the current focus percentage allocated for armor resupply.
     *
     * @return The percentage of resupply focus allocated to armor.
     */
    public double getFocusArmor() {
        return focusArmor;
    }

    /**
     * Sets the focus percentage allocated for armor resupply.
     *
     * @param focusArmor The percentage of resupply focus to allocate to armor.
     */
    public void setFocusArmor(double focusArmor) {
        this.focusArmor = focusArmor;
    }

    /**
     * Retrieves the current focus percentage allocated for general parts resupply.
     *
     * @return The percentage of resupply focus allocated to parts.
     */
    public double getFocusParts() {
        return focusParts;
    }

    /**
     * Sets the focus percentage allocated for general parts resupply.
     *
     * @param focusParts The percentage of resupply focus to allocate to parts.
     */
    public void setFocusParts(double focusParts) {
        this.focusParts = focusParts;
    }

    /**
     * Retrieves the pool of general parts available for resupply. This pool includes non-specific parts that have
     * passed all eligibility checks.
     *
     * @return A list of general parts available for resupply.
     */
    public List<Part> getPartsPool() {
        return partsPool;
    }

    /**
     * Retrieves the pool of armor parts available for resupply. This includes any eligible armor components that have
     * been processed.
     *
     * @return A list of armor parts available for resupply.
     */
    public List<Part> getArmorPool() {
        return armorPool;
    }

    /**
     * Retrieves the pool of ammo bins available for resupply. This includes eligible ammunition bins that have been
     * processed.
     *
     * @return A list of ammo bins available for resupply.
     */
    public List<Part> getAmmoBinPool() {
        return ammoBinPool;
    }

    /**
     * Retrieves the negotiation skill level of the designated negotiator for the resupply process. This skill level is
     * used to influence procurement outcomes and quality.
     *
     * @return The negotiation skill level of the negotiator.
     */
    public int getNegotiatorSkill() {
        return negotiatorSkill;
    }

    /**
     * Retrieves the target cargo tonnage used when the player is providing convoy units. This value represents the
     * desired amount of cargo that the player's convoy aims to carry.
     *
     * @return The target cargo tonnage for the player's convoy.
     */
    public int getTargetCargoTonnagePlayerConvoy() {
        return targetCargoTonnagePlayerConvoy;
    }

    /**
     * Retrieves the total cargo capacity available in the player's convoy. This capacity indicates the maximum amount
     * of cargo that the convoy can hold.
     *
     * @return The total cargo capacity of the player's convoy.
     */
    public double getTotalPlayerCargoCapacity() {
        return totalPlayerCargoCapacity;
    }

    /**
     * Retrieves the current list of parts in the convoy's inventory. This method provides access to the parts being
     * transported by the convoy.
     *
     * @return A list of parts contained within the convoy.
     */
    public List<Part> getConvoyContents() {
        return convoyContents;
    }

    /**
     * Sets the list of parts to be included in the convoy's inventory. This method allows the assignment or
     * modification of the parts being transported by the convoy.
     *
     * @param convoyContents The list of parts to be assigned to the convoy.
     */
    public void setConvoyContents(List<Part> convoyContents) {
        this.convoyContents = convoyContents;
    }

    /**
     * Retrieves the base value of the contents in the convoy. This value typically represents the estimated or
     * predetermined worth of the convoy's cargo.
     *
     * @return A {@link Money} object representing the base value of the convoy contents.
     */
    public Money getConvoyContentsValueBase() {
        return convoyContentsValueBase;
    }

    /**
     * Sets the base value of the contents in the convoy. This is used to define the predetermined or estimated worth of
     * the convoy's cargo.
     *
     * @param convoyContentsValueBase A {@link Money} object representing the base value of the convoy contents.
     */
    public void setConvoyContentsValueBase(Money convoyContentsValueBase) {
        this.convoyContentsValueBase = convoyContentsValueBase;
    }

    /**
     * Retrieves the calculated value of the convoy's contents. This value is typically dynamic and may include various
     * adjustments based on gameplay scenarios.
     *
     * @return A {@link Money} object representing the calculated value of the convoy contents.
     */
    public Money getConvoyContentsValueCalculated() {
        return convoyContentsValueCalculated;
    }

    /**
     * Sets the calculated value of the convoy's contents. This value may be adjusted dynamically during the campaign or
     * based on external factors.
     *
     * @param convoyContentsValueCalculated A {@link Money} object representing the calculated value of the convoy
     *                                      contents.
     */
    public void setConvoyContentsValueCalculated(Money convoyContentsValueCalculated) {
        this.convoyContentsValueCalculated = convoyContentsValueCalculated;
    }

    /**
     * Retrieves the player convoys and their corresponding cargo capacities. This method provides a mapping of
     * player-controlled forces to their available cargo capacity.
     *
     * @return A {@link Map} where the key is a {@link Force} representing the player's convoy, and the value is a
     *       {@link Double} indicating its cargo capacity.
     */
    public Map<Force, Double> getPlayerConvoys() {
        return playerConvoys;
    }

    /**
     * Checks whether the player's convoy is set to be used for resupply purposes. This indicates if resupply operations
     * take into account the player's convoy.
     *
     * @return A {@code boolean} indicating whether the player's convoy is being used. Returns {@code true} if the
     *       player's convoy is used, {@code false} otherwise.
     */
    public boolean getUsePlayerConvoy() {
        return usePlayerConvoy;
    }

    /**
     * Sets whether the player's convoy should be used for resupply purposes. This determines if cargo and resupply
     * operations will consider the player's convoy.
     *
     * @param usePlayerConvoy A {@code boolean} indicating whether the player's convoy should be used. {@code true} to
     *                        use the player's convoy, {@code false} otherwise.
     */
    public void setUsePlayerConvoy(boolean usePlayerConvoy) {
        this.usePlayerConvoy = usePlayerConvoy;
    }

    static int calculateTargetCargoTonnage(Campaign campaign, AtBContract contract) {
        double unitTonnage = 0;

        // First, calculate the total tonnage across all combat units in the campaign.
        // We define a 'combat unit' as any unit not flagged as non-combat who is both in a Combat
        // Team and not in a Force flagged as non-combat
        for (CombatTeam formation : campaign.getCombatTeamsAsMap().values()) {
            Force force = campaign.getForce(formation.getForceId());

            if (force == null) {
                continue;
            }

            if (!force.isForceType(STANDARD)) {
                continue;
            }

            for (UUID unitId : force.getAllUnits(true)) {
                Entity entity = getEntityFromUnitId(campaign.getHangar(), unitId);

                if (entity == null) {
                    continue;
                }

                if (isProhibitedUnitType(entity, false, false)) {
                    continue;
                }

                unitTonnage += entity.getWeight();
            }
        }

        // Next, we determine the tonnage cap. This is the maximum tonnage the employer is willing to support.
        double dropSize = getDropSize(contract, unitTonnage);

        if (campaign.getCampaignOptions().isUseFactionStandingResupplySafe()) {
            FactionStandings standings = campaign.getFactionStandings();
            double regard = standings.getRegardForFaction(contract.getEmployerCode(), true);
            double resupplyMultiplier = FactionStandingUtilities.getResupplyWeightModifier(regard);
            dropSize *= resupplyMultiplier;
        }

        return (int) max(CARGO_MINIMUM_WEIGHT, round(dropSize));
    }

    private static double getDropSize(AtBContract contract, double unitTonnage) {
        final int INDIVIDUAL_TONNAGE_ALLOWANCE = 80; // This is how many tons the employer will budget per unit
        final int tonnageCap = contract.getRequiredCombatElements() * INDIVIDUAL_TONNAGE_ALLOWANCE;

        // Then we determine the size of each individual 'drop'. This uses the lowest of
        // unitTonnage and tonnageCap and divides that by 100
        final double baseTonnage = min(unitTonnage, tonnageCap);

        final int TONNAGE_DIVIDER = 125;
        return baseTonnage / TONNAGE_DIVIDER;
    }

    /**
     * Determines if the given entity is a prohibited unit type based on specific criteria.
     *
     * @param entity                       the entity to check for prohibited unit type
     * @param excludeDropShipsFromCheck    if true, DropShip entities are excluded from being considered prohibited
     * @param excludeSuperHeaviesFromCheck if true, Super Heavy entities are excluded from being considered prohibited
     *
     * @return {@code true} if the entity is a prohibited unit type such as Small Craft, Large Craft, or Conventional
     *       Infantry, and not excluded by the specified parameters; {@code false} otherwise
     */
    public static boolean isProhibitedUnitType(Entity entity, boolean excludeDropShipsFromCheck,
          boolean excludeSuperHeaviesFromCheck) {
        if (entity.isDropShip() && excludeDropShipsFromCheck) {
            return false;
        }

        if (entity.isSuperHeavy() && excludeSuperHeaviesFromCheck) {
            return false;
        }

        return entity.isSmallCraft() || entity.isLargeCraft() || entity.isConventionalInfantry();
    }

    /**
     * Builds the pools of parts (e.g., general parts, ammo bins, and armor) for the resupply drop. Parts are procured,
     * shuffled, and organized into their respective pools for distribution.
     *
     * @param potentialParts A map of potential parts to include in the supply drop, keyed by part name.
     */
    private void buildPartsPools(Map<Part, PartDetails> potentialParts) {
        partsPool = new ArrayList<>();
        armorPool = new ArrayList<>();
        ammoBinPool = new ArrayList<>();

        final int PROHIBITED_BAR_RATING = 0;

        for (PartDetails potentialPart : potentialParts.values()) {
            int weight = (int) Math.round(potentialPart.getWeight());
            for (int entry = 0; entry < weight; entry++) {
                Part part = potentialPart.getPart();

                if (part instanceof Armor armor) {
                    if (armor instanceof SVArmor svArmor) {
                        int bar = svArmor.getBAR();

                        if (bar == PROHIBITED_BAR_RATING) {
                            continue;
                        }
                    }

                    armorPool.add(part);
                    continue;
                }

                if (part instanceof AmmoBin || part instanceof AmmoStorage) {
                    ammoBinPool.add(part);
                    continue;
                }

                partsPool.add(part);
            }
        }

        // Make procurement checks for each of the items in the individual pools
        Procurement procurement = new Procurement(negotiatorSkill, currentYear, employerFaction);

        partsPool = procurement.makeProcurementChecks(partsPool, true, true);
        Collections.shuffle(partsPool);

        armorPool = procurement.makeProcurementChecks(armorPool, true, true);
        Collections.shuffle(armorPool);

        ammoBinPool = procurement.makeProcurementChecks(ammoBinPool, true, true);
        Collections.shuffle(ammoBinPool);
    }

    /**
     * Collects all eligible parts from campaign units and organizes them into a map, where each part is associated with
     * its corresponding details. The collection process considers various factors, including exclusion lists, location
     * validation, and warehouse resources, to determine the eligibility and weight of each part.
     *
     * <p>This method leverages the campaign's existing parts in use, filters them based on specific
     * criteria (e.g., unit exclusion, allowed quality levels), and applies warehouse-specific weight modifiers to
     * calculate the resulting details.</p>
     *
     * @return A {@link Map} where:
     *       <ul>
     *           <li>The key is a {@link Part} object representing the eligible part.</li>
     *           <li>The value is a {@link PartDetails} object that contains detailed information
     *               about the part, such as adjusted weight, based on warehouse modifiers.</li>
     *       </ul>
     */

    private Map<Part, PartDetails> collectParts() {
        PartsInUseManager partsInUseManager = new PartsInUseManager(campaign);
        Set<PartInUse> partsInUse = partsInUseManager.getPartsInUse(true, true, PartQuality.QUALITY_A);

        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();
        boolean removeClan = !campaignFaction.isClan() && today.isBefore(BATTLE_OF_TUKAYYID);

        Set<PartInUse> partsToRemove = new HashSet<>();
        for (PartInUse partInUse : partsInUse) {
            Part part = partInUse.getPartToBuy().getAcquisitionPart();
            if (removeClan && (part.isClan() || part.isMixedTech())) {
                partsToRemove.add(partInUse);
                continue;
            }

            if (isIneligiblePart(part)) {
                partsToRemove.add(partInUse);
            }
        }

        partsInUse.removeAll(partsToRemove);

        return applyWarehouseWeightModifiers(partsInUse);
    }

    /**
     * Checks if a part is ineligible for inclusion in the resupply process. Ineligibility is determined based on
     * exclusion lists, unit structure compatibility, and transporter checks.
     *
     * @param part The part being checked.
     *
     * @return {@code true} if the part is ineligible, {@code false} otherwise.
     */
    private boolean isIneligiblePart(Part part) {
        return checkExclusionList(part) ||
                     checkMekLocation(part) ||
                     checkTankLocation(part) ||
                     checkMotiveSystem(part) ||
                     checkTransporter(part);
    }

    /**
     * Checks if a part is in the exclusion list and should not be considered for resupply. Equipment parts are
     * evaluated for specific flags, such as {@code F_SPONSON_TURRET}, which would disqualify them from inclusion.
     *
     * @param part The part to check.
     *
     * @return {@code true} if the part is in the exclusion list, {@code false} otherwise.
     */
    private boolean checkExclusionList(Part part) {
        if (part instanceof EquipmentPart equipmentPart) {
            return equipmentPart.getType().hasFlag(F_SPONSON_TURRET);
        }
        return false;
    }

    /**
     * Checks if the given part is an instance of {@code MotiveSystem}.
     *
     * @param part the {@link Part} to be checked.
     *
     * @return {@code true} if the part is a {@link MotiveSystem}, {@code false} otherwise.
     */
    private boolean checkMotiveSystem(Part part) {
        return part instanceof MotiveSystem;
    }

    /**
     * Checks if a part belonging to a 'Mek' unit is eligible for resupply, based on its location. For example, parts
     * located in the center torso or parts from extinct units are deemed ineligible.
     *
     * @param part The part to check.
     *
     * @return {@code true} if the part is ineligible due to its location or extinction, {@code false} otherwise.
     */
    private boolean checkMekLocation(Part part) {
        return part instanceof MekLocation && (((MekLocation) part).getLoc() == Mek.LOC_CENTER_TORSO);
    }

    /**
     * Verifies if a vehicle part is eligible for resupply. Parts such as rotors and turrets are always eligible, while
     * other tank locations may be disqualified.
     *
     * @param part The part to check.
     *
     * @return {@code true} if the part is ineligible for resupply, {@code false} otherwise.
     */
    private boolean checkTankLocation(Part part) {
        return part instanceof TankLocation && !(part instanceof Rotor || part instanceof Turret);
    }

    /**
     * Determines whether a part is a transporter-related part, such as a `TransportBayPart`, which makes it ineligible
     * for consideration in the resupply process.
     *
     * @param part The part to check.
     *
     * @return {@code true} if the part is a transporter part, {@code false} otherwise.
     */
    private boolean checkTransporter(Part part) {
        return part instanceof TransportBayPart;
    }

    /**
     * Applies warehouse-based weight modifiers to a set of parts currently in use.
     *
     * <p>Each part will be assigned a weight representing its resupply priority or need, based on its usage count,
     * the current store's supply, and any applicable multipliers.</p>
     *
     * <p>Parts always have a minimum weight of 1, ensuring resupply requests are never empty. If a part cannot be
     * acquired or is invalid, it will be skipped.</p>
     *
     * <p>When not performing a loot or smuggler resupply, all processed parts are marked as brand new.</p>
     *
     * @param partsInUse a set of {@link PartInUse} representing the parts currently needed
     *
     * @return a map of {@link Part} to its {@link PartDetails}, with adjusted weights
     */
    private Map<Part, PartDetails> applyWarehouseWeightModifiers(Set<PartInUse> partsInUse) {
        Map<Part, PartDetails> partDetailsMap = new HashMap<>();

        for (PartInUse partInUse : partsInUse) {
            Part part = getValidAcquisitionPart(partInUse);
            if (part == null) {
                continue;
            }

            int weight = calculateBaseWeight(partInUse);

            // Only mark new for certain resupply types
            part.setBrandNew(!isLootOrSmugglerResupply());

            // Apply multiplier and minimum weight constraint
            weight = Math.max(1, (int) Math.floor(weight * getPartMultiplier(part)));

            partDetailsMap.put(part, new PartDetails(part, weight));
        }

        return partDetailsMap;
    }

    /**
     * Safely retrieves the acquisition part for a given {@link PartInUse}, or returns null if unavailable.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Part getValidAcquisitionPart(PartInUse partInUse) {
        if (partInUse == null || partInUse.getPartToBuy() == null) {
            return null;
        }
        return partInUse.getPartToBuy().getAcquisitionPart();
    }

    /**
     * Calculates the base weight for a given PartInUse, applying a minimum of 1.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int calculateBaseWeight(PartInUse partInUse) {
        // Always at least 1 to avoid empty resupplies
        return Math.max(1, partInUse.getUseCount() - partInUse.getStoreCount());
    }

    /**
     * Checks if the current resupply type is loot or smuggler resupply.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private boolean isLootOrSmugglerResupply() {
        return resupplyType == ResupplyType.RESUPPLY_LOOT ||
                     resupplyType == ResupplyType.RESUPPLY_SMUGGLER;
    }


    /**
     * Retrieves the multiplier value for a specific part type to calculate its priority in the resupply process. This
     * multiplier affects how the part is weighted when included in the pool of available resupply resources.
     *
     * @param part The part to determine the multiplier for.
     *
     * @return A multiplier value for the given part type.
     */
    private static double getPartMultiplier(Part part) {
        double multiplier = 1;

        // This is based on the Mishra Method, found in the Company Generator
        if (part instanceof HeatSink) {
            multiplier = 2.5;
        } else if (part instanceof MekLocation) {
            if (((MekLocation) part).getLoc() == Mek.LOC_HEAD) {
                multiplier = 2;
            }
        } else if (part instanceof MASC ||
                         part instanceof MekGyro ||
                         part instanceof EnginePart ||
                         checkEquipmentSubType(part)) {
            multiplier = 0.5;
        } else if (part instanceof AmmoBin || part instanceof Armor) {
            multiplier = 5;
        }

        return multiplier;
    }

    /**
     * Determines whether a part is an eligible equipment subtype for the resupply process. Excludes certain types such
     * as ammo, ammo storage, heat sinks, and jump jets.
     *
     * @param part The part to check.
     *
     * @return {@code true} if the part is an eligible equipment subtype, {@code false} otherwise.
     */
    private static boolean checkEquipmentSubType(Part part) {
        if (part instanceof EquipmentPart) {
            if (part instanceof AmmoBin) {
                return false;
            }

            if (part instanceof AmmoStorage) {
                return false;
            }

            if (part instanceof BattleArmorEquipmentPart) {
                return false;
            }

            if (part instanceof HeatSink) {
                return false;
            }

            return !(part instanceof JumpJet);
        }

        return true;
    }

    /**
     * Calculates the negotiation skill level by selecting the most qualified negotiator in the current campaign. If the
     * contract type is classified as guerrilla warfare, the flagged commander is prioritized. Otherwise,
     * Admin/Logistics personnel are evaluated.
     */
    private void calculateNegotiationSkill() {
        Person negotiator;
        negotiatorSkill = NONE.ordinal();

        if (contract.getContractType().isGuerrillaWarfare() || PIRATE_FACTION_CODE.equals(contract.getEmployerCode())) {
            negotiator = campaign.getCommander();
        } else {
            negotiator = null;

            for (Person admin : campaign.getAdmins()) {
                if (admin.getPrimaryRole().isAdministratorLogistics() ||
                          admin.getSecondaryRole().isAdministratorLogistics()) {
                    if (negotiator == null || (admin.outRanksUsingSkillTiebreaker(campaign, negotiator))) {
                        negotiator = admin;
                    }
                }
            }
        }

        if (negotiator != null) {
            Skill skill = negotiator.getSkill(SkillType.S_NEGOTIATION);

            if (skill != null) {
                SkillModifierData skillModifierData = negotiator.getSkillModifierData(campaign.getCampaignOptions()
                                                                                            .isUseAgeEffects(),
                      campaign.isClanCampaign(), campaign.getLocalDate());
                int skillLevel = skill.getFinalSkillValue(skillModifierData);
                negotiatorSkill = skill.getType().getExperienceLevel(skillLevel);
            }
        }
    }

    /**
     * Calculates the total cargo capacity available in player-controlled convoys. Convoy forces and their units are
     * evaluated for cargo capacity, and disabled, damaged, or uncrewed units are excluded from the totals.
     */
    private void calculatePlayerConvoyValues() {
        playerConvoys = new HashMap<>();
        totalPlayerCargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (!force.isForceType(CONVOY)) {
                continue;
            }

            // This ensures each convoy is only counted once
            if (force.getParentForce() != null && force.getParentForce().isForceType(CONVOY)) {
                continue;
            }

            double cargoCapacitySubTotal = 0;
            boolean hasCargo = false;
            for (UUID unitId : force.getAllUnits(false)) {
                try {
                    Unit unit = campaign.getUnit(unitId);
                    Entity entity = unit.getEntity();

                    if (unit.isDamaged() || !unit.isFullyCrewed() || isProhibitedUnitType(entity, true, true)) {
                        continue;
                    }

                    double individualCargo = unit.getCargoCapacity();

                    if (individualCargo > 0) {
                        hasCargo = true;
                    }

                    cargoCapacitySubTotal += individualCargo;
                } catch (Exception ignored) {
                    // If we run into an exception, it's because we failed to get Unit or Entity.
                    // In either case, we just ignore that unit.
                }
            }

            if (hasCargo) {
                if (cargoCapacitySubTotal > 0) {
                    totalPlayerCargoCapacity += cargoCapacitySubTotal;
                    playerConvoys.put(force, cargoCapacitySubTotal);
                }
            }
        }
    }

    /**
     * Represents details about a part used during the collection and sorting process for resupply resources. Contains
     * the part itself and its weight value.
     */
    private static class PartDetails {
        private final Part part;
        private double weight;

        /**
         * Constructs a new {@code PartDetails} instance.
         *
         * @param part   The associated part.
         * @param weight The weight or priority of the part.
         */
        public PartDetails(Part part, double weight) {
            this.part = part;
            this.weight = weight;
        }

        /**
         * Gets the associated part.
         *
         * @return The part.
         */
        public Part getPart() {
            return part;
        }

        /**
         * Gets the current weight of the part.
         *
         * @return The weight.
         */
        public double getWeight() {
            return weight;
        }

        /**
         * Sets the current weight of the part.
         *
         * @param weight The new weight to assign.
         */
        public void setWeight(double weight) {
            this.weight = weight;
        }
    }
}
