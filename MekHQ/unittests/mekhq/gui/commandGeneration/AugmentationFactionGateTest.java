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
package mekhq.gui.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ratgenerator.FactionRecord;
import org.junit.jupiter.api.Test;

/**
 * The Setup tab only offers the augmentation controls to factions they mean something for. The rule is easy to get
 * subtly wrong in a way nobody notices: gate it on the Shadow Divisions alone and a Clan player quietly loses
 * enhanced imaging, because its stage reads the same Use Implants setting and nothing tells them it is off.
 */
class AugmentationFactionGateTest {

    private static FactionRecord faction(String key, boolean clan) {
        FactionRecord record = new FactionRecord(key);
        record.setClan(clan);
        return record;
    }

    @Test
    void theWordOfBlakeShadowDivisionsAreOffered() {
        assertTrue(CommandGenerationPane.offersAugmentation(faction("WOB.SD", false)),
              "the Shadow Divisions are the one faction the Manei Domini stage fits implants to");
    }

    @Test
    void everyClanIsOffered() {
        assertTrue(CommandGenerationPane.offersAugmentation(faction("CJF", true)),
              "enhanced imaging is the Clans' own, and it reads the same Use Implants setting");
    }

    @Test
    void warriorHouseThuggeeIsOffered() {
        assertTrue(CommandGenerationPane.offersAugmentation(faction("CC.THG", false)),
              "Thuggee's Houses were raised from the cult and the Manei Domini, so their warriors are augmented");
    }

    @Test
    void theRegularWarriorHouseOrdersAreNotOffered() {
        assertFalse(CommandGenerationPane.offersAugmentation(faction("CC.WHO", false)),
              "Thuggee's Houses answer to Kali Liao rather than the Warrior House Orders, who are not augmented");
    }

    @Test
    void theKeyIsMatchedWithoutRegardToCase() {
        assertTrue(CommandGenerationPane.offersAugmentation(faction("wob.sd", false)));
        assertTrue(CommandGenerationPane.offersAugmentation(faction("cc.thg", false)));
    }

    @Test
    void anUnaugmentedInnerSphereFactionIsNotOffered() {
        assertFalse(CommandGenerationPane.offersAugmentation(faction("FS", false)),
              "the Federated Suns fits no augmentation, so the controls would change nothing");
        assertFalse(CommandGenerationPane.offersAugmentation(faction("LA", false)));
    }

    @Test
    void thePlainWordOfBlakeIsNotOffered() {
        assertFalse(CommandGenerationPane.offersAugmentation(faction("WOB", false)),
              "only the Shadow Divisions are Manei Domini, not the Word of Blake at large");
    }

    @Test
    void thePlainCapellanConfederationIsNotOffered() {
        assertFalse(CommandGenerationPane.offersAugmentation(faction("CC", false)),
              "the Warrior Houses are augmented, the Confederation's regular forces are not");
    }

    @Test
    void nothingSelectedIsNotOffered() {
        assertFalse(CommandGenerationPane.offersAugmentation(null),
              "before a faction is reported the controls stay hidden rather than flashing on");
    }
}
