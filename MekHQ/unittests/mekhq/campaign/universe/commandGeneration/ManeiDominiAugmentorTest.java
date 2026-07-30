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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.options.OptionsConstants;
import mekhq.campaign.personnel.enums.ManeiDominiRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers the Manei Domini implant availability rules from <i>Jihad Hot Spots: 3072</i>, pp. 121,
 * 123-124.
 *
 * <p>Selection is random, so each case runs many times: a rule that only usually holds is not a rule,
 * and a single draw would let a violation through most runs.</p>
 */
class ManeiDominiAugmentorTest {

    /** Enough draws that a rule broken on an uncommon path still shows up. */
    private static final int DRAWS = 400;

    /** Every rank that carries an allowance - all of the enum except {@code NONE}. */
    @ParameterizedTest
    @EnumSource(value = ManeiDominiRank.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void implantCountStaysWithinTheRanksAllowance(ManeiDominiRank maneiDominiRank) {
        int[] allowance = ManeiDominiAugmentor.allowanceFor(maneiDominiRank);
        assertNotNull(allowance, maneiDominiRank + " must have an allowance");
        int minimum = allowance[0];
        int maximum = allowance[1];

        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiAugmentor.selectImplants(maneiDominiRank);
            assertTrue(issued.size() >= minimum,
                  maneiDominiRank + " drew " + issued.size() + ", fewer than the minimum " + minimum);
            // The chart's maximum is a hard ceiling. A neural interface needed by a multi-modal
            // implant takes another implant's place rather than being granted on top of it.
            assertTrue(issued.size() <= maximum,
                  maneiDominiRank + " drew " + issued.size() + ", beyond the maximum " + maximum);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ManeiDominiRank.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void noImplantExceedsTheRanksLevelCeiling(ManeiDominiRank maneiDominiRank) {
        int maximumLevel = ManeiDominiAugmentor.allowanceFor(maneiDominiRank)[2];

        for (int draw = 0; draw < DRAWS; draw++) {
            for (String option : ManeiDominiAugmentor.selectImplants(maneiDominiRank)) {
                assertTrue(ManeiDominiAugmentor.levelOf(option) <= maximumLevel,
                      maneiDominiRank + " was issued " + option + " at level "
                            + ManeiDominiAugmentor.levelOf(option) + ", above its ceiling " + maximumLevel);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = ManeiDominiRank.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void anImplantIsNeverIssuedTwice(ManeiDominiRank maneiDominiRank) {
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiAugmentor.selectImplants(maneiDominiRank);
            assertTrue(issued.stream().distinct().count() == issued.size(),
                  maneiDominiRank + " was issued a duplicate implant: " + issued);
        }
    }

    /**
     * Beta is the tight case: up to four implants drawn from the five the level 2 ceiling allows. If
     * anything at level 1 or 2 is ever removed from the catalogue, this is the rank that breaks first.
     */
    @Test
    void betaCanStillFillItsAllowanceFromTheLevelTwoCatalogue() {
        boolean everFilledToMaximum = false;
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiAugmentor.selectImplants(ManeiDominiRank.BETA);
            assertTrue(issued.size() >= 3, "Beta must always reach its minimum of 3, drew " + issued);
            everFilledToMaximum |= (issued.size() >= 4);
        }
        assertTrue(everFilledToMaximum,
              "Beta must be able to reach its maximum of 4 from the level 2 catalogue");
    }

    @ParameterizedTest
    @EnumSource(value = ManeiDominiRank.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void anImprovedImplantIsNeverHeldAlongsideTheOneItSupersedes(ManeiDominiRank maneiDominiRank) {
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiAugmentor.selectImplants(maneiDominiRank);
            assertFalse(issued.contains(OptionsConstants.MD_PL_ENHANCED)
                        && issued.contains(OptionsConstants.MD_PL_I_ENHANCED),
                  "both basic and improved prosthetics issued: " + issued);
            assertFalse(issued.contains(OptionsConstants.MD_COMM_IMPLANT)
                        && issued.contains(OptionsConstants.MD_BOOST_COMM_IMPLANT),
                  "both basic and boosted comm implants issued: " + issued);
            assertFalse(issued.contains(OptionsConstants.MD_MM_IMPLANTS)
                        && issued.contains(OptionsConstants.MD_ENH_MM_IMPLANTS),
                  "both basic and enhanced multi-modal implants issued: " + issued);
            assertFalse(issued.contains(OptionsConstants.MD_VDNI)
                        && issued.contains(OptionsConstants.MD_BVDNI),
                  "both plain and buffered neural interfaces issued: " + issued);
        }
    }

    /**
     * A multi-modal sensory implant only syncs with a vehicle's sensors through a neural interface, so
     * issuing one without the other would fit an implant that does nothing for a MekWarrior.
     */
    @ParameterizedTest
    @EnumSource(value = ManeiDominiRank.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void multiModalImplantsAlwaysComeWithANeuralInterface(ManeiDominiRank maneiDominiRank) {
        for (int draw = 0; draw < DRAWS; draw++) {
            List<String> issued = ManeiDominiAugmentor.selectImplants(maneiDominiRank);
            boolean hasMultiModal = issued.contains(OptionsConstants.MD_MM_IMPLANTS)
                                          || issued.contains(OptionsConstants.MD_ENH_MM_IMPLANTS);
            if (!hasMultiModal) {
                continue;
            }
            boolean hasInterface = issued.contains(OptionsConstants.MD_VDNI)
                                         || issued.contains(OptionsConstants.MD_BVDNI);
            assertTrue(hasInterface,
                  maneiDominiRank + " got a multi-modal implant with no neural interface: " + issued);
        }
    }

    /**
     * The explosive charge is a property of being Manei Domini rather than an implant chosen in place
     * of another, so it is fitted separately and must not appear in the selection.
     */
    @ParameterizedTest
    @EnumSource(value = ManeiDominiRank.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void theExplosiveChargeIsNotDrawnFromTheAllowance(ManeiDominiRank maneiDominiRank) {
        for (int draw = 0; draw < DRAWS; draw++) {
            assertFalse(ManeiDominiAugmentor.selectImplants(maneiDominiRank)
                              .contains(OptionsConstants.MD_SUICIDE_IMPLANTS),
                  "the suicide charge must be fitted separately, not drawn as an implant");
        }
    }

    @Test
    void onlyTheShadowDivisionsAreManeiDomini() {
        assertTrue(ManeiDominiAugmentor.isShadowDivision("WOB.SD"));
        assertTrue(ManeiDominiAugmentor.isShadowDivision("wob.sd"), "the key is not case sensitive");
        assertFalse(ManeiDominiAugmentor.isShadowDivision("WOB"),
              "the Militia proper are not Manei Domini");
        assertFalse(ManeiDominiAugmentor.isShadowDivision("CS"));
        assertFalse(ManeiDominiAugmentor.isShadowDivision(null));
    }
}
