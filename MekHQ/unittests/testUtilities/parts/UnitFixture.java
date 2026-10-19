/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package testUtilities.parts;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.SpaceStation;
import megamek.common.units.SuperHeavyTank;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import megamek.common.units.Warship;
import testUtilities.CanonUnits;

/**
 * The library of canon units used by real-object parts, repair and refit tests: one or more per unit family, plus
 * refit pairs.
 *
 * <p>Every fixture is loaded from the game's own unit data through {@link CanonUnits}, found by the UUID in its file
 * and checked against its name, so a test always uses the current version of the unit. Loading a fixture that cannot
 * be found, has been renamed or cannot be parsed fails the test with a message saying why; it never falls back to
 * another unit.</p>
 */
public enum UnitFixture {
    /** Locust LCT-1V, a light Mek. */
    LOCUST_LCT_1V("Locust LCT-1V", "019f583e-c25b-7e1d-a2f6-f2b3763ac718", Mek.class),
    /** Locust LCT-1E, the refit target of the LCT-1V. */
    LOCUST_LCT_1E("Locust LCT-1E", "019f583e-c256-7157-8d3e-adf669917b96", Mek.class),
    /**
     * Locust LCT-5V, an LCT-1V with the same 160 engine but double heat sinks, so a refit into it changes the heat
     * sinks built into the engine.
     */
    LOCUST_LCT_5V("Locust LCT-5V", "019f583e-d190-7093-8385-1e91f2f4f9d8", Mek.class),
    /** Wolverine WVR-6R, a medium Mek. */
    WOLVERINE_WVR_6R("Wolverine WVR-6R", "019f583e-c338-7203-9a51-f587c68e116c", Mek.class),
    /** Longbow LGB-8C, a heavy fire support Mek. */
    LONGBOW_LGB_8C("Longbow LGB-8C", "019f583e-da29-72ed-a3a8-bd2ed4599e81", Mek.class),
    /** Wolverine WVR-6M, the refit target of the WVR-6R. */
    WOLVERINE_WVR_6M("Wolverine WVR-6M", "019f583e-c337-7f18-8eec-2307e641a920", Mek.class),
    /** Hunchback HBK-4G, a medium Mek. */
    HUNCHBACK_HBK_4G("Hunchback HBK-4G", "019f583e-c22f-7b66-ba9e-06356dcc83f0", Mek.class),
    /** Hunchback HBK-4P, the refit target of the HBK-4G. */
    HUNCHBACK_HBK_4P("Hunchback HBK-4P", "019f583e-c235-7439-844e-e01901696cca", Mek.class),
    /** Griffin GRF-1A, a primitive Griffin. */
    GRIFFIN_GRF_1A("Griffin GRF-1A", "019f583e-dff7-7f4d-804d-6fda6a4428f4", Mek.class),
    /** Griffin GRF-1N, the standard Griffin. */
    GRIFFIN_GRF_1N("Griffin GRF-1N", "019f583e-c215-7a66-afd4-259f136b4ab0", Mek.class),
    /** Lumberjack LM1A, an IndustrialMek with a primitive engine rated above its walking MP times its tonnage. */
    LUMBERJACK_LM1A("Lumberjack LM1/A", "019f583e-cecc-7c4a-8d07-d6228115e73b", Mek.class),
    /** Omega SHP-5R, a superheavy Mek with a superheavy gyro. */
    OMEGA_SHP_5R("Omega SHP-5R", "019f583e-d4f4-7966-8c38-1ba20d38e192", Mek.class),
    /** Omega SHP-4X, the earlier superheavy Omega. */
    OMEGA_SHP_4X("Omega SHP-4X", "019f583e-d6e2-7e62-b316-ec8ea3c7e583", Mek.class),
    /** Skinwalker A, a Mek with an interface cockpit and no gyro. */
    SKINWALKER_A("Ryoken III (Skinwalker) A", "019f583e-dab7-7e19-8ccd-289f3f5bf365", Mek.class),
    /** Hoplite C, a Clan Mek (not an OmniMek) with Clan ferro-fibrous armor. */
    HOPLITE_C("Hoplite C", "019f583e-c541-7722-9251-7a8bfb55fed1", Mek.class),
    /** Vedette Medium Tank, a tracked combat vehicle. */
    VEDETTE_MEDIUM_TANK("Vedette Medium Tank", "019f583e-e326-74de-8b26-e3e4e58f06b8", Tank.class),
    /** Zephyros Infantry Support Vehicle (Dual Turret), a tank with front and rear turrets. */
    ZEPHYROS_DUAL_TURRET("Zephyros Infantry Support Vehicle (Dual Turret)",
          "019f583e-e744-7ce3-9bd7-53e836368a02", Tank.class),
    /** Burke II Superheavy Tank, a superheavy tank whose turret is its eighth location. */
    BURKE_II_SUPERHEAVY_TANK("Burke II Superheavy Tank", "019f583e-ebe2-7d45-b348-f7f79619e0b7", SuperHeavyTank.class),
    /** Armored Personnel Carrier (Hover LRM), a hover APC without a turret. */
    APC_HOVER_LRM("Armored Personnel Carrier (Hover LRM)", "019f583e-e243-7aad-b8d9-5d9a23801dec", Tank.class),
    /** Armored Personnel Carrier (Hover MG), the same APC with a turret. */
    APC_HOVER_MG("Armored Personnel Carrier (Hover MG)", "019f583e-e245-745a-a418-61fb15658604", Tank.class),
    /** Cellco Ranger UPU-3000, a tracked support vehicle with BAR 8 armor. */
    CELLCO_RANGER_UPU_3000("Cellco Ranger UPU-3000", "019f583e-eadf-700b-aece-62f18cdb6b58", SupportTank.class),
    /** Epona Pursuit Tank Prime, a Clan hover OmniVehicle with its weapons in the turret. */
    EPONA_PURSUIT_TANK_PRIME("Epona Pursuit Tank Prime", "019f583e-e4e2-7e2f-9598-34f3a57c066b", Tank.class),
    /** Epona Pursuit Tank A, another configuration of the same OmniVehicle, with weapons in the front and turret. */
    EPONA_PURSUIT_TANK_A("Epona Pursuit Tank A", "019f583e-e4d7-71dd-869c-4edb92211530", Tank.class),
    /** Warrior H-7 Attack Helicopter, a VTOL. */
    WARRIOR_H_7_ATTACK_HELICOPTER("Warrior Attack Helicopter H-7", "019f583e-e32e-7053-85ce-1348db5f5195", VTOL.class),
    /** Red Kite Attack VTOL, a VTOL with a chin turret. */
    RED_KITE_ATTACK_VTOL("Red Kite Attack VTOL", "019f583e-e803-7d9b-ae2d-5ac44715526b", VTOL.class),
    /** Inner Sphere Standard battle armor, a squad of four. */
    IS_STANDARD_BATTLE_ARMOR_LASER("IS Standard Battle Armor [Laser](Sqd4)",
          "019f583e-a288-7d7f-a635-6cf21746820c", BattleArmor.class),
    /** Elemental battle armor, a Clan point of five. */
    ELEMENTAL_BATTLE_ARMOR_LASER("Elemental Battle Armor [Laser](Sqd5)",
          "019f583e-a1e0-785d-a983-829ca2dd427c", BattleArmor.class),
    /** Elemental battle armor with flamers instead of lasers, the same point of five. */
    ELEMENTAL_BATTLE_ARMOR_FLAMER("Elemental Battle Armor [Flamer](Sqd5)",
          "019f583e-a1d3-70c5-afe8-17202c49c684", BattleArmor.class),
    /** Elemental battle armor with lasers in a squad of six, one trooper more than the point of five. */
    ELEMENTAL_BATTLE_ARMOR_LASER_SQUAD_OF_SIX("Elemental Battle Armor [Laser](Sqd6)",
          "019f583e-a1e3-773e-aeb4-f468d1481fe8", BattleArmor.class),
    /** A conventional foot platoon. */
    FOOT_PLATOON_LASER("Foot Platoon (DCMS) (Laser 2620+)", "019f583e-bbcf-7edf-809f-865049255592", Infantry.class),
    /** The same DCMS foot platoon armed with machine guns. */
    FOOT_PLATOON_MG("Foot Platoon (DCMS) (MG 2620+)", "019f583e-bbd3-7699-8104-a8478ced0ea1", Infantry.class),
    /** A motorized platoon towing three AC/5 field guns, each with its own ammunition. */
    FIELD_GUN_PLATOON_AC5("Field Gun Platoon (AFFS) (Motorized AC5 3067+)",
          "019f583e-b6a3-7dda-adcf-23634fb04508", Infantry.class),
    /** Minotaur, a ProtoMek. */
    MINOTAUR_PROTOMEK("Minotaur", "019f583e-e0c3-7912-acfd-3d50243f8d51", ProtoMek.class),
    /** Shilone SL-17, an aerospace fighter. */
    SHILONE_SL_17("Shilone SL-17", "019f583e-ad51-79dd-bfb0-19f2362bdf17", AeroSpaceFighter.class),
    /** Shilone SL-17AC, the SL-17 with one heat sink fewer. */
    SHILONE_SL_17AC("Shilone SL-17AC", "019f583e-ad53-7a2b-bc6b-ec2d5a22c96a", AeroSpaceFighter.class),
    /** Shilone SL-17R, the SL-17 with double heat sinks instead of single ones, including those in the engine. */
    SHILONE_SL_17R("Shilone SL-17R", "019f583e-ad54-7411-8749-cb05532c1a62", AeroSpaceFighter.class),
    /** Batu Prime, a Clan OmniFighter with two pod-mounted heat sinks. */
    BATU_PRIME("Batu Prime", "019f583e-ae04-73c0-b230-a6460db9b098", AeroSpaceFighter.class),
    /** Batu D, another configuration of the Batu with four pod-mounted heat sinks. */
    BATU_D("Batu D", "019f583e-ae01-7e35-906e-eede60a0c471", AeroSpaceFighter.class),
    /** Turk A, a Clan OmniFighter whose pod-mounted ammunition sits in the fuselage. */
    TURK_A("Turk A", "019f583e-ae55-7a98-ac36-abba31db757d", AeroSpaceFighter.class),
    /** Shuttle ST-46, a small craft. */
    SHUTTLE_ST_46("Shuttle ST-46", "019f583e-e158-7bdd-8050-3cd9c4fc6ec3", SmallCraft.class),
    /** Leopard (2537), a DropShip. */
    LEOPARD_DROPSHIP("Leopard (2537)", "019f583e-ab1e-7963-8d4e-5ffa82f1fbbf", Dropship.class),
    /** Leopard (3056), the Leopard refitted with double heat sinks and newer weapons and ammunition. */
    LEOPARD_DROPSHIP_3056("Leopard (3056)", "019f583e-ab20-71d5-b96d-4388ed07f17e", Dropship.class),
    /** Leopard CV (2581), the carrier Leopard: three fighter bays in place of the BattleMek bay. */
    LEOPARD_CV_DROPSHIP("Leopard CV (2581)", "019f583e-ab22-7832-bb5f-3845ce1e2151", Dropship.class),
    /** Leopard PA, a pirate Leopard with guns in all six arcs where the standard Leopard has four. */
    LEOPARD_PA_DROPSHIP("Leopard PA-Class Dropship", "019f583e-ac4d-7fb0-87c6-ec026c31e73f", Dropship.class),
    /** Union (3055), a DropShip with Artemis LRM-20 ammunition in its nose and both side arcs. */
    UNION_3055_DROPSHIP("Union (3055)", "019f583e-ab65-779e-a52e-65dea9e33327", Dropship.class),
    /** Union (2708b), which carries the same Artemis LRM-20 ammunition in its nose and aft arcs instead. */
    UNION_2708B_DROPSHIP("Union (2708b)", "019f583e-ab5e-72d0-918d-6d9253de1c0d", Dropship.class),
    /** Invader JumpShip (2631), a JumpShip with docking collars and a grav deck. */
    INVADER_JUMPSHIP("Invader JumpShip (2631)", "019f583e-c0e3-71ab-ab61-023aac9a9b33", Jumpship.class),
    /** Essex II Destroyer (2711), a WarShip with fighter and small craft bays. */
    ESSEX_II_WARSHIP("Essex II Destroyer (2711)", "019f583e-ece7-7dd1-93f6-7032e051221b", Warship.class),
    /** Olympus Recharge Station (3072), a space station with docking collars. */
    OLYMPUS_SPACE_STATION("Olympus Recharge Station (3072)",
          "019f583e-e215-79b6-bd69-54d0c53b7c75", SpaceStation.class);

    /**
     * A canon refit: the unit a campaign starts with and the variant it is refitted into.
     *
     * @param original the unit before the refit
     * @param target   the variant the refit produces
     */
    public record RefitPair(UnitFixture original, UnitFixture target) {}

    /** The refit pairs the parts safety net relies on. */
    public static final List<RefitPair> REFIT_PAIRS = List.of(
          new RefitPair(LOCUST_LCT_1V, LOCUST_LCT_1E),
          new RefitPair(WOLVERINE_WVR_6R, WOLVERINE_WVR_6M),
          new RefitPair(HUNCHBACK_HBK_4G, HUNCHBACK_HBK_4P));

    private final String unitName;
    private final String unitFileUuid;
    private final Class<? extends Entity> unitFamily;

    UnitFixture(String unitName, String unitFileUuid, Class<? extends Entity> unitFamily) {
        this.unitName = unitName;
        this.unitFileUuid = unitFileUuid;
        this.unitFamily = unitFamily;
    }

    /**
     * @return the entity class this fixture must load as, such as {@link Mek} or {@link Dropship}
     */
    public Class<? extends Entity> getUnitFamily() {
        return unitFamily;
    }

    /**
     * Loads a fresh entity for this fixture from the game's unit data. Each call parses the file again, so tests never
     * share an entity.
     *
     * <p>Fails the calling test when the unit cannot be found or parsed, has been renamed, or does not load as
     * {@link #getUnitFamily()}. It never returns {@code null}.</p>
     *
     * @return the loaded entity
     */
    public Entity loadEntity() {
        Entity entity = CanonUnits.loadUnit(unitName, unitFileUuid);
        assertTrue(unitFamily.isInstance(entity),
              "Unit fixture " + name() + " loaded as " + entity.getClass().getSimpleName() + ", expected "
                    + unitFamily.getSimpleName());
        return entity;
    }

    /**
     * @return the text of this fixture's unit file, for a test that edits the file before parsing it
     */
    public String readUnitFile() {
        return CanonUnits.readUnitFile(unitName, unitFileUuid);
    }
}
