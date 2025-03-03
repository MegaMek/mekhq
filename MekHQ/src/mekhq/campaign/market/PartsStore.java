/*
 * PartsStore.java
 *
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.market;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import megamek.common.*;
import megamek.common.equipment.ArmorType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.verifier.TestEntity;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;

/**
 * This is a parts store which will contain one copy of every possible
 * part that might be needed as well as a variety of helper functions to
 * acquire parts.
 *
 * We could in the future extend this to different types of stores that have
 * different finite numbers of
 * parts in inventory
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class PartsStore {
    private static final MMLogger logger = MMLogger.create(PartsStore.class);
    private static int EXPECTED_SIZE = 50000;

    private ArrayList<Part> parts;
    private Map<String, Part> nameAndDetailMap;

    public PartsStore(Campaign c) {
        parts = new ArrayList<>(EXPECTED_SIZE);
        nameAndDetailMap = new HashMap<>(EXPECTED_SIZE);
        stock(c);
    }

    public ArrayList<Part> getInventory() {
        return parts;
    }

    public Part getByNameAndDetails(String nameAndDetails) {
        return nameAndDetailMap.get(nameAndDetails);
    }

    public void stock(Campaign c) {
        stockWeaponsAmmoAndEquipment(c);
        stockMekActuators(c);
        stockEngines(c);
        stockGyros(c);
        stockMekComponents(c);
        stockAeroComponents(c);
        stockVeeComponents(c);
        stockArmor(c);
        stockMekLocations(c);
        stockProtomekLocations(c);
        stockProtomekComponents(c);
        stockBattleArmorSuits(c);

        StringBuilder sb = new StringBuilder();
        for (Part p : parts) {
            p.setBrandNew(true);
            sb.setLength(0);
            sb.append(p.getName());
            if (!(p instanceof Armor)) { // ProtoMekArmor and BaArmor are derived from Armor
                String details = p.getDetails();
                if (!details.isEmpty()) {
                    sb.append(" (").append(details).append(")");
                }
            }
            nameAndDetailMap.put(sb.toString(), p);
        }
    }

    private void stockBattleArmorSuits(Campaign c) {
        // this is just a test
        for (MekSummary summary : MekSummaryCache.getInstance().getAllMeks()) {
            if (!summary.getUnitType().equals("BattleArmor")) {
                continue;
            }
            // FIXME: I can't pull entity movement mode and quad shape off of meksummary
            // try loading the full entity, but this might take too long
            Entity newEntity = null;
            try {
                newEntity = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
            } catch (EntityLoadingException e) {
                logger.error("", e);
            }
            if (null != newEntity) {
                BattleArmorSuit ba = new BattleArmorSuit(summary.getChassis(), summary.getModel(),
                        (int) summary.getTons(), 1, summary.getWeightClass(), summary.getWalkMp(), summary.getJumpMp(),
                        newEntity.entityIsQuad(), summary.isClan(), newEntity.getMovementMode(), c);
                parts.add(ba);
            }
        }
    }

    private void stockWeaponsAmmoAndEquipment(Campaign c) {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if (!et.isHittable() &&
                    !(et instanceof MiscType && ((MiscType) et).hasFlag(MiscType.F_BA_MANIPULATOR))) {
                continue;
            }
            // TODO: we are still adding a lot of non-hittable equipment
            if (et instanceof AmmoType) {
                AmmoType ammoType = (AmmoType) et;
                if (ammoType.hasFlag(AmmoType.F_BATTLEARMOR)
                        && (ammoType.getKgPerShot() > 0)) {
                    // BA ammo has one shot listed as the amount. Do it as 1 ton blocks if using
                    // kg/shot.
                    int shots = (int) Math.floor(1000.0 / ammoType.getKgPerShot());
                    parts.add(new AmmoStorage(0, ammoType, shots, c));
                } else {
                    parts.add(new AmmoStorage(0, ammoType, ammoType.getShots(), c));
                }
            } else if (et instanceof MiscType && (((MiscType) et).hasFlag(MiscType.F_HEAT_SINK)
                    || ((MiscType) et).hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
                Part p = new HeatSink(0, et, -1, false, c);
                parts.add(p);
                parts.add(new OmniPod(p, c));
                parts.add(new HeatSink(0, et, -1, true, c));
            } else if (et instanceof MiscType && ((MiscType) et).hasFlag(MiscType.F_JUMP_JET)) {
                // need to do it by rating and unit tonnage
                for (int ton = 10; ton <= 100; ton += 5) {
                    Part p = new JumpJet(ton, et, -1, false, c);
                    parts.add(p);
                    if (!et.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                        parts.add(new OmniPod(p, c));
                        parts.add(new JumpJet(ton, et, -1, true, c));
                    }
                }
            } else if ((et instanceof MiscType && ((MiscType) et).hasFlag(MiscType.F_TANK_EQUIPMENT)
                    && ((MiscType) et).hasFlag(MiscType.F_CHASSIS_MODIFICATION))
                    || et instanceof BayWeapon
                    || et instanceof InfantryAttack) {
                continue;
            } else if (et instanceof MiscType && ((MiscType) et).hasFlag(MiscType.F_BA_EQUIPMENT)
                    && !((MiscType) et).hasFlag(MiscType.F_BA_MANIPULATOR)) {
                continue;
            } else if (et instanceof MiscType && ((MiscType) et).hasFlag(MiscType.F_MASC)) {
                if (et.hasSubType(MiscType.S_SUPERCHARGER)) {
                    for (int rating = 10; rating <= 400; rating += 5) {
                        // eton 0.5 to 10.5 inclusive
                        for (int i = 1; i <= 21; i++) {
                            double eton = i * 0.5;
                            double weight = Engine.ENGINE_RATINGS[(int) Math.ceil(rating / 5.0)];
                            double minweight = weight * 0.5;
                            minweight = Math.ceil(
                                    (TestEntity.ceilMaxHalf(minweight, TestEntity.Ceil.HALFTON) / 10.0) * 2.0) / 2.0;
                            double maxweight = weight * 2.0;
                            maxweight = Math.ceil(
                                    (TestEntity.ceilMaxHalf(maxweight, TestEntity.Ceil.HALFTON) / 10.0) * 2.0) / 2.0;
                            if (eton < minweight || eton > maxweight) {
                                continue;
                            }
                            MASC sp = new MASC(0, et, -1, c, rating, false);
                            sp.setEquipTonnage(eton);
                            parts.add(sp);
                            parts.add(new OmniPod(sp, c));
                            sp = new MASC(0, et, -1, c, rating, true);
                            sp.setEquipTonnage(eton);
                            parts.add(sp);
                        }
                    }
                } else {
                    // need to do it by rating and unit tonnage
                    for (int ton = 20; ton <= 100; ton += 5) {
                        for (int rating = 10; rating <= 400; rating += 5) {
                            if (rating < ton || (rating % ton) != 0) {
                                continue;
                            }
                            Part p = new MASC(ton, et, -1, c, rating, false);
                            parts.add(p);
                            parts.add(new OmniPod(p, c));
                            parts.add(new MASC(ton, et, -1, c, rating, true));
                        }
                    }
                }
            } else {
                boolean poddable = !et.isOmniFixedOnly();
                if (et instanceof MiscType) {
                    poddable &= et.hasFlag(MiscType.F_MEK_EQUIPMENT)
                            || et.hasFlag(MiscType.F_TANK_EQUIPMENT)
                            || et.hasFlag(MiscType.F_FIGHTER_EQUIPMENT);
                } else if (et instanceof WeaponType) {
                    poddable &= (et.hasFlag(WeaponType.F_MEK_WEAPON)
                            || et.hasFlag(Weapon.F_TANK_WEAPON)
                            || et.hasFlag(WeaponType.F_AERO_WEAPON))
                            && !((WeaponType) et).isCapital();
                }
                if (EquipmentPart.hasVariableTonnage(et)) {
                    EquipmentPart epart;
                    for (double ton = EquipmentPart.getStartingTonnage(et); ton <= EquipmentPart
                            .getMaxTonnage(et); ton += EquipmentPart.getTonnageIncrement(et)) {
                        epart = new EquipmentPart(0, et, -1, 1.0, false, c);
                        epart.setEquipTonnage(ton);
                        parts.add(epart);
                        if (poddable) {
                            epart = new EquipmentPart(0, et, -1, 1.0, true, c);
                            epart.setEquipTonnage(ton);
                            parts.add(epart);
                            epart = new EquipmentPart(0, et, -1, 1.0, true, c);
                            epart.setEquipTonnage(ton);
                            parts.add(new OmniPod(epart, c));
                        }
                        // TODO: still need to deal with talons (unit tonnage) and masc (engine rating)
                    }
                } else {
                    Part p = new EquipmentPart(0, et, -1, 1.0, false, c);
                    parts.add(p);
                    if (poddable) {
                        parts.add(new EquipmentPart(0, et, -1, 1.0, true, c));
                        parts.add(new OmniPod(new EquipmentPart(0, et, -1, 1.0, false, c), c));
                    }
                }
            }
        }
        // lets throw aero heat sinks in here as well
        AeroHeatSink hs = new AeroHeatSink(0, Aero.HEAT_SINGLE, false, c);
        parts.add(hs);
        parts.add(new OmniPod(hs, c));
        parts.add(new AeroHeatSink(0, Aero.HEAT_SINGLE, true, c));

        hs = new AeroHeatSink(0, Aero.HEAT_DOUBLE, false, c);
        parts.add(hs);
        parts.add(new OmniPod(hs, c));
        parts.add(new AeroHeatSink(0, Aero.HEAT_DOUBLE, true, c));

        hs = new AeroHeatSink(0, AeroHeatSink.CLAN_HEAT_DOUBLE, false, c);
        parts.add(hs);
        parts.add(new OmniPod(hs, c));
        parts.add(new AeroHeatSink(0, AeroHeatSink.CLAN_HEAT_DOUBLE, true, c));
    }

    private void stockMekActuators(Campaign c) {
        for (int i = Mek.ACTUATOR_UPPER_ARM; i <= Mek.ACTUATOR_FOOT; i++) {
            if (i == Mek.ACTUATOR_HIP) {
                continue;
            }
            int ton = 20;
            while (ton <= 100) {
                parts.add(new MekActuator(ton, i, -1, c));
                ton += 5;
            }
        }
    }

    private double getEngineTonnage(Engine engine) {
        double weight = Engine.ENGINE_RATINGS[(int) Math.ceil(engine.getRating() / 5.0)];
        switch (engine.getEngineType()) {
            case Engine.COMBUSTION_ENGINE:
                weight *= 2.0f;
                break;
            case Engine.NORMAL_ENGINE:
                break;
            case Engine.XL_ENGINE:
                weight *= 0.5f;
                break;
            case Engine.LIGHT_ENGINE:
                weight *= 0.75f;
                break;
            case Engine.XXL_ENGINE:
                weight /= 3f;
                break;
            case Engine.COMPACT_ENGINE:
                weight *= 1.5f;
                break;
            case Engine.FISSION:
                weight *= 1.75;
                weight = Math.max(5, weight);
                break;
            case Engine.FUEL_CELL:
                weight *= 1.2;
                break;
            case Engine.NONE:
                return 0;
        }
        weight = TestEntity.ceilMaxHalf(weight, TestEntity.Ceil.HALFTON);
        if (engine.hasFlag(Engine.TANK_ENGINE) && engine.isFusion()) {
            weight *= 1.5f;
        }
        return TestEntity.ceilMaxHalf(weight, TestEntity.Ceil.HALFTON);
    }

    private void stockEngines(Campaign c) {
        Engine engine;
        int year = c.getGameYear();
        for (int rating = 10; rating <= 400; rating += 5) {
            for (int ton = 5; ton <= 100; ton += 5) {
                for (int i = 0; i <= Engine.FISSION; i++) {
                    if (rating >= ton && rating % ton == 0) {
                        engine = new Engine(rating, i, 0);
                        if (engine.engineValid) {
                            parts.add(new EnginePart(ton, engine, c, false));
                        }
                        if (engine.getTechType(year) != TechConstants.T_ALLOWED_ALL) {
                            engine = new Engine(rating, i, Engine.CLAN_ENGINE);
                            if (engine.engineValid) {
                                parts.add(new EnginePart(ton, engine, c, false));
                            }
                        }
                    }
                    engine = new Engine(rating, i, Engine.TANK_ENGINE);
                    if (engine.engineValid) {
                        parts.add(new EnginePart(ton, engine, c, false));
                    }
                    if ((ton / 5) > getEngineTonnage(engine)) {
                        engine = new Engine(rating, i, Engine.TANK_ENGINE);
                        if (engine.engineValid) {
                            parts.add(new EnginePart(ton, engine, c, true));
                        }
                    }
                    engine = new Engine(rating, i, Engine.TANK_ENGINE | Engine.CLAN_ENGINE);
                    if (engine.getTechType(year) != TechConstants.T_ALLOWED_ALL) {
                        if (engine.engineValid) {
                            parts.add(new EnginePart(ton, engine, c, false));
                        }
                        if ((ton / 5) > getEngineTonnage(engine)) {
                            engine = new Engine(rating, i, Engine.TANK_ENGINE | Engine.CLAN_ENGINE);
                            if (engine.engineValid) {
                                parts.add(new EnginePart(ton, engine, c, true));
                            }
                        }
                    }
                }
            }
        }
    }

    private void stockGyros(Campaign c) {
        // values of 0.5 to 8.0 inclusive
        for (int r = 1; r <= 16; r++) {
            double i = r * 0.5;
            // standard at intervals of 1.0, up to 4
            if (i % 1.0 == 0 && i <= 4.0) {
                parts.add(new MekGyro(0, Mek.GYRO_STANDARD, i, false, c));
                parts.add(new MekGyro(0, Mek.GYRO_STANDARD, i, true, c));
            }
            // compact at intervals of 1.5, up to 6
            if (i % 1.5 == 0 && i <= 6.0) {
                parts.add(new MekGyro(0, Mek.GYRO_COMPACT, i, false, c));
            }
            // XL at 0.5 intervals up to 2
            if (i <= 2.0) {
                parts.add(new MekGyro(0, Mek.GYRO_XL, i, false, c));
            }
            // Heavy duty at 2.0 intervals
            if (i % 2.0 == 0) {
                parts.add(new MekGyro(0, Mek.GYRO_HEAVY_DUTY, i, false, c));
            }

        }
    }

    private void stockMekComponents(Campaign c) {
        parts.add(new MekLifeSupport(0, c));
        for (int ton = 20; ton <= 100; ton += 5) {
            parts.add(new MekSensor(ton, c));
            parts.add(new QuadVeeGear(ton, c));
        }

        for (int type = Mek.COCKPIT_STANDARD; type < Mek.COCKPIT_STRING.length; type++) {
            parts.add(new MekCockpit(0, type, false, c));
            if (type != Mek.COCKPIT_SMALL) {
                parts.add(new MekCockpit(0, type, true, c));
            }
        }
    }

    private void stockAeroComponents(Campaign c) {
        parts.add(new AeroHeatSink(0, Aero.HEAT_SINGLE, false, c));
        parts.add(new AeroHeatSink(0, Aero.HEAT_DOUBLE, false, c));
        parts.add(new AeroHeatSink(0, AeroHeatSink.CLAN_HEAT_DOUBLE, false, c));
        for (int ton = 5; ton <= 200; ton += 5) {
            parts.add(new AeroSensor(ton, false, c));
        }
        parts.add(new AeroSensor(0, true, c));
        parts.add(new Avionics(0, c));
        parts.add(new FireControlSystem(0, Money.zero(), c));
        parts.add(new DropshipDockingCollar(0, c, Dropship.COLLAR_STANDARD));
        parts.add(new DropshipDockingCollar(0, c, Dropship.COLLAR_NO_BOOM));
        parts.add(new KfBoom(0, c, Dropship.BOOM_STANDARD));
        parts.add(new KfBoom(0, c, Dropship.BOOM_PROTOTYPE));
        parts.add(new JumpshipDockingCollar(0, 0, c, Jumpship.COLLAR_STANDARD));
        parts.add(new JumpshipDockingCollar(0, 0, c, Jumpship.COLLAR_NO_BOOM));
        parts.add(new GravDeck(0, 0, c, GravDeck.GRAV_DECK_TYPE_STANDARD));
        parts.add(new GravDeck(0, 0, c, GravDeck.GRAV_DECK_TYPE_LARGE));
        parts.add(new GravDeck(0, 0, c, GravDeck.GRAV_DECK_TYPE_HUGE));
        parts.add(new LandingGear(0, c));
        parts.add(new BayDoor(0, c));
        for (BayType btype : BayType.values()) {
            if (btype.getCategory() == BayType.CATEGORY_NON_INFANTRY) {
                parts.add(new Cubicle(0, btype, c));
            }
        }
    }

    private void stockVeeComponents(Campaign c) {
        parts.add(new VeeSensor(0, c));
        parts.add(new VeeStabilizer(0, -1, c));
        for (int ton = 5; ton <= 100; ton = ton + 5) {
            parts.add(new Rotor(ton, c));
            parts.add(new Turret(ton, ton, c));
        }
    }

    private void stockArmor(Campaign c) {
        int amount;
        for (ArmorType armor : ArmorType.allArmorTypes()) {
            if (armor.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                amount = (int) (5 * armor.getWeightPerPoint());
                parts.add(new BaArmor(0, amount, armor.getArmorType(), -1, armor.isClan(), c));
            } else {
                amount = (int) (5.0 * armor.getPointsPerTon());
                parts.add(new Armor(0, armor.getArmorType(), amount, -1, false, armor.isClan(), c));
            }
        }
        parts.add(new ProtoMekArmor(0, EquipmentType.T_ARMOR_STANDARD_PROTOMEK, 100, -1, true, c));
        parts.add(new ProtoMekArmor(0, EquipmentType.T_ARMOR_EDP, 66, -1, true, c));
    }

    private void stockMekLocations(Campaign c) {
        for (int loc = Mek.LOC_HEAD; loc <= Mek.LOC_CLEG; loc++) {
            for (int ton = 20; ton <= 100; ton = ton + 5) {
                for (int type = 0; type < EquipmentType.structureNames.length; type++) {
                    addMekLocation(c, loc, ton, type, false);
                    // The only structure that differs between IS and Clan versions is Endo-Steel
                    if (EquipmentType.T_STRUCTURE_ENDO_STEEL == type) {
                        addMekLocation(c, loc, ton, type, true);
                    }
                }
            }
        }
    }

    private void addMekLocation(Campaign c, int loc, int ton, int type, boolean clan) {
        if (loc == Mek.LOC_HEAD) {
            parts.add(new MekLocation(loc, ton, type, clan, false, false, true, true, c));
            parts.add(new MekLocation(loc, ton, type, clan, true, false, true, true, c));
            parts.add(new MekLocation(loc, ton, type, clan, false, false, false, false, c));
            parts.add(new MekLocation(loc, ton, type, clan, true, false, false, false, c));
        } else {
            parts.add(new MekLocation(loc, ton, type, clan, false, false, false, false, c));
            parts.add(new MekLocation(loc, ton, type, clan, true, false, false, false, c));
            if (loc > Mek.LOC_LT) {
                parts.add(new MekLocation(loc, ton, type, clan, false, true, false, false, c));
                parts.add(new MekLocation(loc, ton, type, clan, true, true, false, false, c));
            }
        }
    }

    private void stockProtomekLocations(Campaign c) {
        for (int loc = ProtoMek.LOC_HEAD; loc <= ProtoMek.LOC_MAINGUN; loc++) {
            for (int ton = 2; ton <= 15; ton++) {
                parts.add(new ProtoMekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, false, false, c));
                parts.add(new ProtoMekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, true, false, c));
                if (loc == ProtoMek.LOC_LEG) {
                    parts.add(new ProtoMekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, false, true, c));
                    parts.add(new ProtoMekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, true, true, c));
                }
            }
        }
    }

    private void stockProtomekComponents(Campaign c) {
        int ton = 2;
        while (ton <= 15) {
            parts.add(new ProtoMekArmActuator(ton, c));
            parts.add(new ProtoMekLegActuator(ton, c));
            parts.add(new ProtoMekSensor(ton, c));
            parts.add(new ProtoMekJumpJet(ton, c));
            ton++;
        }
    }
}
