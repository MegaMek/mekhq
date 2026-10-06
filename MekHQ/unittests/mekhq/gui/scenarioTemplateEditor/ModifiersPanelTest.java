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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.scenarioTemplateEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Phase 4.1 panel test for {@link ModifiersPanel}. Confirms the load/writeInto round-trip (headless-safe, since the
 * available modifier keys are supplied rather than read from data files).
 */
class ModifiersPanelTest {

    private static final List<String> AVAILABLE = List.of("ModifierA", "ModifierB", "ModifierC");

    @Test
    void loadThenWriteIntoRoundTripsSelectedModifiers() {
        ModifiersPanel panel = new ModifiersPanel(AVAILABLE);
        panel.load(List.of("ModifierB", "ModifierC"));

        List<String> target = new ArrayList<>();
        panel.writeInto(target);

        assertEquals(List.of("ModifierB", "ModifierC"), target);
    }

    @Test
    void writeIntoReplacesExistingContents() {
        ModifiersPanel panel = new ModifiersPanel(AVAILABLE);
        panel.load(List.of("ModifierA"));

        List<String> target = new ArrayList<>(List.of("Stale", "Values"));
        panel.writeInto(target);

        assertEquals(List.of("ModifierA"), target);
    }
}
