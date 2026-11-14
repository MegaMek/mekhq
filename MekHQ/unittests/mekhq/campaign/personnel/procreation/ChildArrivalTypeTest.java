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
package mekhq.campaign.personnel.procreation;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers {@link ChildArrivalType} and checks that every resource key the child arrival, multiple pregnancy, and early
 * birth features look up actually exists.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ChildArrivalTypeTest {
    private static final String MISSING_RESOURCE_TAG = "!";
    private static final String PROCREATION_BUNDLE = "mekhq.resources.AbstractProcreation";
    private static final String LOG_BUNDLE = "mekhq.resources.LogEntries";
    private static final String GUI_BUNDLE = "mekhq.resources.GUI";
    private static final String DIALOG_BUNDLE = "mekhq.resources.AddChildDialog";
    private static final String OPTIONS_BUNDLE = "mekhq.resources.CampaignOptionsDialog";

    private static void assertKeyExists(final String bundle, final String key) {
        final String text = getTextAt(bundle, key);
        assertFalse(text.startsWith(MISSING_RESOURCE_TAG), "Missing resource key " + key + " in " + bundle);
    }

    @Test
    void onlyAdoptedChildrenAreNotBiological() {
        assertTrue(ChildArrivalType.BIOLOGICAL.isBiological());
        assertTrue(ChildArrivalType.PATERNITY.isBiological());
        assertFalse(ChildArrivalType.ADOPTION.isBiological());
    }

    @ParameterizedTest
    @EnumSource(ChildArrivalType.class)
    void everyArrivalTypeHasItsReportsAndLogEntry(final ChildArrivalType arrivalType) {
        final String reportKey = arrivalType.getReportKey();

        assertKeyExists(PROCREATION_BUNDLE, reportKey + ".report");
        assertKeyExists(PROCREATION_BUNDLE, reportKey + ".coParent.report");
        assertKeyExists(LOG_BUNDLE, reportKey + ".text");
    }

    @Test
    void childDescriptorsExist() {
        assertKeyExists(PROCREATION_BUNDLE, "childDescriptor.baby.text");
        assertKeyExists(PROCREATION_BUNDLE, "childDescriptor.child.text");
    }

    @Test
    void addChildDialogHasEveryLabelItUses() {
        final List<String> keys = new ArrayList<>(List.of("AddChildDialog.title",
              "AddChildDialog.description",
              "AddChildDialog.type",
              "AddChildDialog.type.tooltip",
              "AddChildDialog.coParent",
              "AddChildDialog.coParent.none",
              "AddChildDialog.age",
              "AddChildDialog.tooYoung",
              "AddChildDialog.confirm",
              "AddChildDialog.cancel"));
        // The dialog offers the GM biological and adopted children; paternity is only ever random
        keys.add("AddChildDialog.type." + ChildArrivalType.BIOLOGICAL.name());
        keys.add("AddChildDialog.type." + ChildArrivalType.ADOPTION.name());

        for (String key : keys) {
            assertKeyExists(DIALOG_BUNDLE, key);
        }
    }

    @Test
    void personnelMenuHasEveryProcreationLabelItUses() {
        assertKeyExists(GUI_BUNDLE, "addNewChild.text");
        assertKeyExists(GUI_BUNDLE, "changePregnancySize.text");
        assertKeyExists(GUI_BUNDLE, "giveBirthNow.text");
        assertKeyExists(GUI_BUNDLE, "giveBirthNow.tooltip");
        assertKeyExists(GUI_BUNDLE, "pregnancySize.random.text");
        for (int size = 1; size <= AbstractProcreation.MAXIMUM_PREGNANCY_SIZE; size++) {
            assertKeyExists(GUI_BUNDLE, "pregnancySize." + size + ".text");
        }
    }

    @Test
    void conceptionReportCoversEveryPregnancySize() {
        // The conception and multiple birth reports pick their wording by indexing this list with the size
        final String[] babyAmounts = getTextAt(PROCREATION_BUNDLE, "babyAmount.text").split(",");
        assertTrue(babyAmounts.length >= AbstractProcreation.MAXIMUM_PREGNANCY_SIZE);
    }

    @Test
    void newCampaignOptionsHaveLabelsAndTooltips() {
        for (String option : List.of("UseRandomPaternity",
              "RandomPaternityMaximumChildAge",
              "UseRandomSameSexAdoption",
              "RandomAdoptionMaximumChildAge")) {
            assertKeyExists(OPTIONS_BUNDLE, "lbl" + option + ".text");
            assertKeyExists(OPTIONS_BUNDLE, "lbl" + option + ".tooltip");
        }
    }
}
