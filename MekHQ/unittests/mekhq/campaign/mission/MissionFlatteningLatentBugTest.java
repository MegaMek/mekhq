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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Characterization tests for latent defects uncovered while reviewing PR #9417 (the flattening of
 * {@code AtBContract -> Contract -> Mission} into a common {@link AbstractMissionTransition} base).
 *
 * <p><b>Every test in this class is {@link Disabled} on purpose.</b> Each one asserts the behavior the code <i>should</i>
 * have once the corresponding defect is fixed, and currently fails. They are committed disabled so the project's CI
 * stays green while the findings are tracked in code; remove the {@code @Disabled} annotation as each underlying defect
 * is addressed. A companion write-up lives in
 * {@code docs/issues/bug-fixes/9417-mission-flattening-latent-cast-and-npe-sites.md}, which also lists the
 * GUI/manager-path cast sites that are impractical to cover with a focused unit test.</p>
 *
 * @author Claude (test author for PR #9417 review)
 */
class MissionFlatteningLatentBugTest {

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        RandomCallsignGenerator.getInstance(true);
        RandomCompanyNameGenerator.getInstance();
        try {
            Factions.setInstance(Factions.loadDefault(true));
            Systems.setInstance(TestSystems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * {@code Campaign.initAtB(false)} converts every non-{@link Mission} entry in the missions map to an
     * {@link AtBContract} with:
     *
     * <pre>{@code
     * if (!(mission instanceof Mission)) {
     *     missionEntry.setValue(new AtBContract((Contract) mission, this));
     * }
     * }</pre>
     *
     * (Campaign.java, around line 8620). Before the flattening this was safe because every {@link AtBContract} was also
     * a {@link Contract}. After the flattening an {@link AtBContract} is <i>not</i> a {@link Contract}, yet it is still
     * not a {@link Mission}, so it enters the conversion branch and the {@code (Contract)} cast throws a
     * {@link ClassCastException}. This bites any campaign that already contains an AtB contract when AtB is
     * (re)initialized.
     *
     * <p>The fix is to skip entries that are already {@link AtBContract} (e.g. guard with
     * {@code instanceof AtBContract}). Once that guard is in place this test should pass: re-initializing AtB must leave
     * an existing AtB contract intact rather than throwing.</p>
     */
    @Disabled("reveals bug: Campaign.initAtB() casts (Contract) mission ~Campaign.java:8620; an AtBContract is no longer "
                    + "a Contract, so re-init throws ClassCastException. Enable once the conversion branch guards on "
                    + "instanceof AtBContract.")
    @Test
    void initAtBDoesNotClassCastAnAlreadyStoredAtBContract() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        AtBContract existing = new AtBContract("Existing AtB Contract");
        campaign.addMission(existing);

        assertDoesNotThrow(() -> campaign.initAtB(false),
              "initAtB must not ClassCastException when an AtBContract is already present");
        assertInstanceOf(AtBContract.class, campaign.getMission(existing.getId()),
              "the pre-existing AtBContract must remain an AtBContract after AtB initialization");
    }
}
