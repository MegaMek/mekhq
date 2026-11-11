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
package mekhq.gui.dialog.trainingConsole;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;

/**
 * Builds the guide for each page of the {@link TrainingConsole} from the {@code Guide.<page>.} keys of its resource
 * bundle: numbered steps, a worked example, and the page's terms.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class TrainingConsoleGuides {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;

    private TrainingConsoleGuides() {
    }

    static Guide build(final TrainingConsolePage page) {
        String prefix = "Guide." + page.name().toLowerCase(Locale.ROOT) + ".";

        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "step." + number + ".title")); number++) {
            steps.add(new Step(text(prefix + "step." + number + ".title"), text(prefix + "step." + number + ".text")));
        }
        List<ExampleLine> example = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "example." + number + ".who")); number++) {
            example.add(new ExampleLine(text(prefix + "example." + number + ".who"),
                  text(prefix + "example." + number + ".text")));
        }
        List<Term> terms = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text(prefix + "term." + number + ".name")); number++) {
            terms.add(new Term(text(prefix + "term." + number + ".name"), text(prefix + "term." + number + ".text")));
        }
        return new Guide(getFormattedTextAt(RESOURCE_BUNDLE, "Guide.eyebrow", page.getLabel()),
              text(prefix + "title"), steps, null, text("Guide.exampleIntro"), example, terms, null);
    }

    private static String text(final String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }
}
