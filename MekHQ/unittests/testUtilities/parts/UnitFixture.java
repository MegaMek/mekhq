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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.List;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import testUtilities.MHQTestUtilities;

/**
 * The library of canon units used by real-object parts, repair and refit tests: one or more per unit family, plus
 * refit pairs.
 *
 * <p>Every fixture is a pinned copy of an mm-data unit file kept in {@code testresources/data/mekfiles}. The mm-data
 * source path is noted on each constant so a copy can be refreshed deliberately. Loading a fixture that is missing or
 * cannot be parsed fails the test with a message naming the file; it never falls back to another unit.</p>
 */
public enum UnitFixture {
    /** Locust LCT-1V, a light Mek. Source: {@code meks/3039u/Locust LCT-1V.mtf} (older pinned copy). */
    LOCUST_LCT_1V("Locust LCT-1V", false, Mek.class),
    /**
     * Locust LCT-1E, the refit target of the LCT-1V. Source: {@code meks/3039u/Locust LCT-1E.mtf} (older pinned
     * copy).
     */
    LOCUST_LCT_1E("Locust LCT-1E", false, Mek.class),
    /** Wolverine WVR-6R, a medium Mek. Source: {@code meks/3039u/Wolverine WVR-6R.mtf}. */
    WOLVERINE_WVR_6R("Wolverine WVR-6R", false, Mek.class),
    /** Wolverine WVR-6M, the refit target of the WVR-6R. Source: {@code meks/3039u/Wolverine WVR-6M.mtf}. */
    WOLVERINE_WVR_6M("Wolverine WVR-6M", false, Mek.class),
    /** Hunchback HBK-4G, a medium Mek. Source: {@code meks/3039u/Hunchback HBK-4G.mtf}. */
    HUNCHBACK_HBK_4G("Hunchback HBK-4G", false, Mek.class),
    /** Hunchback HBK-4P, the refit target of the HBK-4G. Source: {@code meks/3039u/Hunchback HBK-4P.mtf}. */
    HUNCHBACK_HBK_4P("Hunchback HBK-4P", false, Mek.class),
    /** Vedette Medium Tank, a tracked combat vehicle. Source: {@code vehicles/3039u/Vedette Medium Tank.blk}. */
    VEDETTE_MEDIUM_TANK("Vedette Medium Tank", true, Tank.class),
    /** Warrior H-7 Attack Helicopter, a VTOL. Source: {@code vehicles/3039u/Warrior H-7 Attack Helicopter.blk}. */
    WARRIOR_H_7_ATTACK_HELICOPTER("Warrior H-7 Attack Helicopter", true, VTOL.class),
    /**
     * Inner Sphere Standard battle armor, a squad of four. Source:
     * {@code battlearmor/3058Uu/IS Standard BA [Laser] (Sqd4).blk}.
     */
    IS_STANDARD_BATTLE_ARMOR_LASER("IS Standard BA [Laser] (Sqd4)", true, BattleArmor.class),
    /**
     * Elemental battle armor, a Clan point of five. Source:
     * {@code battlearmor/3058Uu/Elemental BA [Laser] (Sqd5).blk}.
     */
    ELEMENTAL_BATTLE_ARMOR_LASER("Elemental BA [Laser] (Sqd5)", true, BattleArmor.class),
    /**
     * A conventional foot platoon. Source: {@code infantry/DCMS/Foot Platoon (DCMS) (Laser 2620+).blk} (older pinned
     * copy).
     */
    FOOT_PLATOON_LASER("Foot Platoon (DCMS) (Laser 2620+)", true, Infantry.class),
    /** Minotaur, a ProtoMek. Source: {@code protomeks/3060/Minotaur.blk}. */
    MINOTAUR_PROTOMEK("Minotaur", true, ProtoMek.class),
    /** Shilone SL-17, an aerospace fighter. Source: {@code fighters/TRO3039u/Shilone SL-17.blk}. */
    SHILONE_SL_17("Shilone SL-17", true, AeroSpaceFighter.class),
    /** Shuttle ST-46, a small craft. Source: {@code smallcraft/TRO 3057r/Shuttle ST-46.blk}. */
    SHUTTLE_ST_46("Shuttle ST-46", true, SmallCraft.class),
    /** Leopard (2537), a DropShip. Source: {@code dropships/TRO3057R/IS/Leopard (2537).blk}. */
    LEOPARD_DROPSHIP("Leopard (2537)", true, Dropship.class);

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

    private final String fileName;
    private final boolean isBlk;
    private final Class<? extends Entity> unitFamily;

    UnitFixture(String fileName, boolean isBlk, Class<? extends Entity> unitFamily) {
        this.fileName = fileName;
        this.isBlk = isBlk;
        this.unitFamily = unitFamily;
    }

    /**
     * @return the file this fixture is loaded from, relative to the MekHQ project directory
     */
    public File getFile() {
        String extension = isBlk ? MHQTestUtilities.TEST_BLK : MHQTestUtilities.TEST_MTF;
        return new File(MHQTestUtilities.TEST_UNIT_DATA_DIR + fileName + extension);
    }

    /**
     * @return the entity class this fixture must load as, such as {@link Mek} or {@link Dropship}
     */
    public Class<? extends Entity> getUnitFamily() {
        return unitFamily;
    }

    /**
     * Loads a fresh entity from this fixture's unit file. Each call parses the file again, so tests never share an
     * entity.
     *
     * <p>Fails the calling test, naming the file and the reason, when the file is missing, cannot be parsed, or does
     * not load as {@link #getUnitFamily()}. It never returns {@code null}.</p>
     *
     * @return the loaded entity
     */
    public Entity loadEntity() {
        EquipmentType.initializeTypes();

        File file = getFile();
        assertTrue(file.isFile(), "Unit fixture " + name() + " file is missing: " + file.getAbsolutePath());

        Entity entity = null;
        try {
            entity = new MekFileParser(file).getEntity();
        } catch (EntityLoadingException exception) {
            fail("Unit fixture " + name() + " could not be parsed from " + file.getAbsolutePath(), exception);
        }

        assertNotNull(entity, "Unit fixture " + name() + " parsed to no entity from " + file.getAbsolutePath());
        assertTrue(unitFamily.isInstance(entity),
              "Unit fixture " + name() + " loaded as " + entity.getClass().getSimpleName() + ", expected "
                    + unitFamily.getSimpleName());
        return entity;
    }
}
