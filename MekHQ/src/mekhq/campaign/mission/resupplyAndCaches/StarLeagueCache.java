/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.resupplyAndCaches;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.apache.commons.math3.util.Pair;

import java.time.LocalDate;
import java.util.*;

import static megamek.common.EntityWeightClass.WEIGHT_ASSAULT;
import static megamek.common.EntityWeightClass.WEIGHT_HEAVY;
import static megamek.common.EntityWeightClass.WEIGHT_LIGHT;
import static megamek.common.EntityWeightClass.WEIGHT_MEDIUM;
import static megamek.common.UnitType.AEROSPACEFIGHTER;
import static megamek.common.UnitType.INFANTRY;
import static megamek.common.UnitType.MEK;
import static megamek.common.UnitType.TANK;
import static mekhq.campaign.mission.BotForceRandomizer.UNIT_WEIGHT_UNSPECIFIED;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.buildPool;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.getDropWeight;
import static mekhq.campaign.unit.Unit.getRandomUnitQuality;

public class StarLeagueCache {
    private final Campaign campaign;
    private final AtBContract contract;
    private final Random random = new Random();
    private Faction originFaction;
    private boolean didGenerationFail = false;
    private Map<Part, Integer> potentialParts;
    private List<Part> partsPool;
    private List<Unit> intactUnits;

    // We use year -1 as otherwise MHQ considers the SL to no longer exist.
    private final LocalDate FALL_OF_STAR_LEAGUE = LocalDate.of(
        Factions.getInstance().getFaction("SL").getEndYear() - 1, 1, 1);
    private final static MMLogger logger = MMLogger.create(StarLeagueCache.class);

    public StarLeagueCache(Campaign campaign, AtBContract contract) {
        this.campaign = campaign;
        this.contract = contract;

        getOriginFaction();

        if (!didGenerationFail) {
            intactUnits = getCacheContents();

            potentialParts = new HashMap<>();
            processUnits();
            partsPool = buildPool(potentialParts);
        }

        if (potentialParts.isEmpty() && intactUnits.isEmpty()) {
            didGenerationFail = true;
        }
    }

    public boolean didGenerationFail() {
        return didGenerationFail;
    }

    private void processUnits() {
        int intactUnitCount = 0;

        for (int lance = 0; lance < contract.getRequiredLances(); lance++) {
            // This will generate a number between 1 and 4 with an average roll of 3
            intactUnitCount += Compute.randomInt(3) + Compute.randomInt(3);
        }

        intactUnitCount = Math.min(intactUnitCount, intactUnits.size());

        int ruinedChance = campaign.getGameYear() - FALL_OF_STAR_LEAGUE.getYear();

        for (int individualUnit = 0; individualUnit < ruinedChance; individualUnit++) {
            if (Compute.randomInt(500) < ruinedChance) {
                intactUnitCount--;
            }
        }

        List<Unit> actuallyIntactUnits = new ArrayList<>();
        for (int i = 0; i < intactUnitCount; i++) {
            int randomIndex = random.nextInt(intactUnits.size());
            actuallyIntactUnits.add(intactUnits.get(randomIndex));
            intactUnits.remove(randomIndex);
        }

        // This uses the end state of intact units as the list of units too ruined for salvage
        collectParts();

        // We then replace 'intactUnits' with the actually intact units.
        intactUnits = actuallyIntactUnits;
    }

    private void collectParts() {
        potentialParts = new HashMap<>();

        try {
            for (Unit unit : intactUnits) {
                List<Part> parts = unit.getParts();
                for (Part part : parts) {
                    if (part instanceof MekLocation) {
                        if (((MekLocation) part).getLoc() == Mek.LOC_CT) {
                            continue;
                            // If the unit itself is extinct, it's impossible to find replacement locations
                        }
                    }

                    if (part instanceof TankLocation) {
                        continue;
                    }

                    Pair<Unit, Part> pair = new Pair<>(unit, part);
                    int weight = getDropWeight(pair.getValue());
                    potentialParts.merge(part, weight, Integer::sum);
                }
            }
        } catch (Exception exception) {
            logger.error("Aborted parts collection.", exception);
        }
    }

    private void getOriginFaction() {
        final int sphereOfInfluence = 650;
        final CurrentLocation location = campaign.getLocation();
        final double distanceToTerra = location.getCurrentSystem().getDistanceTo(campaign.getSystemById("Terra"));

        // This is a fallback to better ensure something drops, even if it isn't a SLDF Depot
        // This value was reached by 'eye-balling' the map of the Inner Sphere
        if (distanceToTerra > sphereOfInfluence) {
            List<String> factions = location.getPlanet().getFactions(FALL_OF_STAR_LEAGUE);

            if (factions.isEmpty()) {
                didGenerationFail = true;
            } else {
                Collections.shuffle(factions);
                originFaction = Factions.getInstance().getFaction(factions.get(0));
            }
        } else {
            originFaction = Factions.getInstance().getFaction("SL");
        }
    }

    private List<Unit> getCacheContents() {
        Map<Integer, List<Integer>> unitsPresent = buildUnitWeightMap();
        List<MekSummary> unitSummaries = getUnitSummaries(unitsPresent);

        List<Unit> units = new ArrayList<>();
        for (MekSummary summary : unitSummaries) {
            Entity entity = getEntity(summary);

            if (entity == null) {
                continue;
            }

            Unit unit = getUnit(entity);

            if (unit != null) {
                units.add(unit);
            }
        }

        return units;
    }

    @Nullable
    private Entity getEntity(MekSummary unitData) {
        try {
            return new MekFileParser(unitData.getSourceFile(), unitData.getEntryName()).getEntity();
        } catch (Exception ex) {
            logger.error("Unable to load entity: {}: {}",
                unitData.getSourceFile(),
                unitData.getEntryName(), ex);
            return null;
        }
    }

    public Unit getUnit(Entity entity) {
        PartQuality quality = getRandomUnitQuality(0);
        Unit unit = new Unit(entity, campaign);
        unit.initializeParts(true);
        unit.setQuality(quality);

        return unit;
    }

    private List<MekSummary> getUnitSummaries(Map<Integer, List<Integer>> unitsPresent) {
        final List<Integer> potentialUnitTypes = List.of(INFANTRY, TANK, MEK, AEROSPACEFIGHTER);

        List<MekSummary> unitSummaries = new ArrayList<>();
        for (int unitType : potentialUnitTypes) {
            for (int unitWeight : unitsPresent.get(unitType)) {
                unitSummaries.add(campaign.getUnitGenerator().generate(originFaction.getShortName(), unitType, unitWeight,
                    FALL_OF_STAR_LEAGUE.getYear(), getRandomUnitQuality(0).toNumeric()));
            }
        }

        return unitSummaries;
    }

    private Map<Integer, List<Integer>> buildUnitWeightMap() {
        final int COMPANY_COUNT = 3;
        Map<Integer, List<Integer>> unitsPresent = new HashMap<>();

        for (int company = 0; company < COMPANY_COUNT; company++) {
            int unitType = getCompanyUnitType();

            if (unitType == INFANTRY) {
                unitsPresent.put(INFANTRY, List.of(UNIT_WEIGHT_UNSPECIFIED, UNIT_WEIGHT_UNSPECIFIED,
                    UNIT_WEIGHT_UNSPECIFIED));
            } else {
                for (int lance : getCompanyLances()) {
                    if (unitsPresent.containsKey(unitType)) {
                        unitsPresent.get(unitType).addAll(getUnitWeights(lance));
                    } else {
                        unitsPresent.put(unitType, getUnitWeights(lance));
                    }
                }
            }
        }
        return unitsPresent;
    }

    private int getCompanyUnitType() {
        int roll = Compute.d6();
        // This table is based on the one found on p265 of Total Warfare.
        // We increased the chance of rolling 'Meks, because players will expect to get 'Meks out
        // of these caches
        return switch (roll) {
            case 1 -> INFANTRY;
            case 2, 3 -> TANK;
            case 4, 5 -> MEK;
            case 6 -> AEROSPACEFIGHTER;
            default -> throw new IllegalStateException("Unexpected value in getCompanyUnitType: "
                + roll);
        };

    }

    private List<Integer> getCompanyLances() {
        List<Integer> companyLances = new ArrayList<>();

        int roll = Compute.d6(1);
        // This table is based on the one found on p265 of Total Warfare
        switch (roll) {
            case 1 -> {
                companyLances.add(WEIGHT_LIGHT);
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_MEDIUM);
            }
            case 2 -> {
                companyLances.add(WEIGHT_LIGHT);
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 3 -> {
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_MEDIUM);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 4 -> {
                companyLances.add(WEIGHT_LIGHT);
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 5 -> {
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
            }
            case 6 -> {
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_HEAVY);
                companyLances.add(WEIGHT_ASSAULT);
            }
            default -> throw new IllegalStateException("Unexpected value in getCompanyLances(): "
                + roll);
        }

        return companyLances;
    }

    private List<Integer> getUnitWeights(int weight) {
        List<Integer> unitWeights = new ArrayList<>();
        final int[] rollOutcome;

        int roll = Compute.d6(2);
        // This table is based on the one found on p265 of Total Warfare
        switch (roll) {
            case 1 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_LIGHT};
                case WEIGHT_MEDIUM -> new int[]{WEIGHT_LIGHT, WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_HEAVY};
                case WEIGHT_HEAVY -> new int[]{WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            case 2, 3 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_MEDIUM};
                case WEIGHT_MEDIUM, WEIGHT_HEAVY -> new int[]{weight, weight, weight, weight};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            case 4, 5 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_MEDIUM, WEIGHT_MEDIUM};
                case WEIGHT_MEDIUM -> new int[]{WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_HEAVY};
                case WEIGHT_HEAVY -> new int[]{WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_ASSAULT};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_HEAVY, WEIGHT_ASSAULT, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            case 6 -> rollOutcome = switch (weight) {
                case WEIGHT_LIGHT -> new int[]{WEIGHT_LIGHT, WEIGHT_LIGHT, WEIGHT_MEDIUM, WEIGHT_HEAVY};
                case WEIGHT_MEDIUM -> new int[]{WEIGHT_MEDIUM, WEIGHT_MEDIUM, WEIGHT_HEAVY, WEIGHT_HEAVY};
                case WEIGHT_HEAVY -> new int[]{WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_HEAVY, WEIGHT_ASSAULT};
                case WEIGHT_ASSAULT -> new int[]{WEIGHT_ASSAULT, WEIGHT_ASSAULT, WEIGHT_ASSAULT, WEIGHT_ASSAULT};
                default -> throw new IllegalStateException("Unexpected weight: " + weight);
            };
            default -> throw new IllegalStateException("Unexpected value in getlanceWeights(): " + roll);
        }

        for (int outcome : rollOutcome) {
            unitWeights.add(outcome);
        }

        return unitWeights;
    }
}
