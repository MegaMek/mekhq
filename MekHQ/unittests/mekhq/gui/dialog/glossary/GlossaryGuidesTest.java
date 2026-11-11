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
package mekhq.gui.dialog.glossary;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.dialog.glossary.GlossaryConsole.Mode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link GlossaryGuides}: every page of the console has a complete guide, with no missing text.
 */
class GlossaryGuidesTest {
    @ParameterizedTest
    @EnumSource(Mode.class)
    void everyPageHasACompleteGuide(Mode mode) {
        Guide guide = GlossaryGuides.build(mode);

        assertTrue(isResourceKeyValid(guide.title()), guide.title());
        assertTrue(isResourceKeyValid(guide.exampleIntro()), guide.exampleIntro());
        assertTrue(isResourceKeyValid(GlossaryGuides.buttonLabel(mode)), GlossaryGuides.buttonLabel(mode));
        assertFalse(guide.steps().isEmpty(), mode + " has no steps");
        assertFalse(guide.example().isEmpty(), mode + " has no example");
        assertFalse(guide.terms().isEmpty(), mode + " has no terms");
        for (Step step : guide.steps()) {
            assertTrue(isResourceKeyValid(step.body()), step.title() + " has no text");
        }
        for (ExampleLine line : guide.example()) {
            assertTrue(isResourceKeyValid(line.text()), line.who() + " has no line");
        }
        for (Term term : guide.terms()) {
            assertTrue(isResourceKeyValid(term.meaning()), term.name() + " has no meaning");
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void onlyTheGlossaryPageListsTheShortcuts(Mode mode) {
        Guide guide = GlossaryGuides.build(mode);

        if (mode == Mode.GLOSSARY) {
            assertNotNull(guide.stepsExtra());
        } else {
            assertNull(guide.stepsExtra());
        }
    }
}
