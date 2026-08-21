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
package mekhq.campaign.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import megamek.Version;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * End-to-end tests for the data-driven campaign-options serialization: {@link CampaignOptionsMarshaller},
 * {@link CampaignOptionsUnmarshaller}, and the {@link CampaignOptionCodecs} registry keyed by
 * {@link CampaignOption#xmlTag()}.
 */
class CampaignOptionsSerializationTest {
    private static final Version VERSION = new Version("0.50.10");

    // region Helpers
    private static String marshal(final CampaignOptions options) {
        final StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(options, pw, 0);
        }
        return sw.toString();
    }

    private static CampaignOptions reload(final String xml) {
        final Document document = assertDoesNotThrow(() ->
                                                           MHQXMLUtility.parseDocument(new ByteArrayInputStream(xml.getBytes(
                                                                 StandardCharsets.UTF_8))));
        final Node node = document.getElementsByTagName("campaignOptions").item(0);
        return CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);
    }

    private static CampaignOptions reloadTags(final String innerXml) {
        return reload("<campaignOptions>" + innerXml + "</campaignOptions>");
    }

    private static CampaignOptions roundTrip(final CampaignOptions original) {
        return reload(marshal(original));
    }
    // endregion Helpers

    @Test
    void everyOptionResolvesToACodec_andEveryTagIsUnique() {
        final Set<String> tags = new HashSet<>();
        for (final CampaignOption<?> option : CampaignOption.values()) {
            assertDoesNotThrow(() -> CampaignOptionCodecs.codecFor(option),
                  () -> "no codec resolves for option with tag " + option.xmlTag());
            assertTrue(tags.add(option.xmlTag()), () -> "duplicate xmlTag: " + option.xmlTag());
        }
    }

    /**
     * The broad safety net: a default campaign's scalar and enum options must survive a marshal/unmarshal cycle
     * unchanged. This catches any codec whose write form disagrees with its read form.
     */
    @Test
    void defaultOptions_roundTripPreservesEveryScalarAndEnumValue() {
        final CampaignOptions original = new CampaignOptions();
        final CampaignOptions reloaded = roundTrip(original);

        for (final CampaignOption<?> option : CampaignOption.values()) {
            final Class<?> type = option.type();
            final boolean simple = type == Boolean.class
                                         || type == Integer.class
                                         || type == Double.class
                                         || type == String.class
                                         || type.isEnum();
            if (!simple) {
                continue;
            }
            // Derived through client preferences; not a plain stored value.
            if (option == CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME) {
                continue;
            }
            // Read-only legacy marker; intentionally never written.
            if (option == CampaignOption.HAD_AT_B_ENABLED_MARKER) {
                continue;
            }
            assertEquals(original.get(option), reloaded.get(option),
                  () -> "round-trip changed option " + option.xmlTag());
        }
    }

    @Test
    void roundTrip_preservesCollectionArrayAndObjectValues() {
        final CampaignOptions original = new CampaignOptions();
        original.setRoleBaseSalary(PersonnelRole.DOCTOR, 4321);
        original.get(CampaignOption.SALARY_XP_MULTIPLIERS).put(SkillLevel.ELITE, 9.5);
        original.get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).put(SkillLevel.ELITE, 99);
        original.setPlanetTechAcquisitionBonus(9, PlanetarySophistication.A);
        original.setPhenotypeProbability(0, 42);
        original.setAtBBattleChance(0, 77);
        original.setUsePortraitForRole(2, true);
        original.set(CampaignOption.USED_PART_PRICE_MULTIPLIERS, new double[] { 0.11, 0.22, 0.33, 0.44, 0.55, 0.66 });

        final CampaignOptions reloaded = roundTrip(original);

        assertEquals(Money.of(4321), reloaded.get(CampaignOption.ROLE_BASE_SALARIES)[PersonnelRole.DOCTOR.ordinal()]);
        assertEquals(9.5, reloaded.get(CampaignOption.SALARY_XP_MULTIPLIERS).get(SkillLevel.ELITE));
        assertEquals(99, reloaded.get(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS).get(SkillLevel.ELITE));
        assertEquals(9, reloaded.getPlanetTechAcquisitionBonus(PlanetarySophistication.A));
        assertEquals(42, reloaded.get(CampaignOption.PHENOTYPE_PROBABILITIES)[0]);
        assertEquals(77, reloaded.get(CampaignOption.ATB_BATTLE_CHANCE)[0]);
        assertTrue(reloaded.get(CampaignOption.USE_PORTRAIT_FOR_ROLE)[2]);
        assertEquals(0.11, reloaded.get(CampaignOption.USED_PART_PRICE_MULTIPLIERS)[0]);
        assertFalse(reloaded.get(CampaignOption.MRMS_OPTIONS).isEmpty());
        assertNotNull(reloaded.get(CampaignOption.RANDOM_ORIGIN_OPTIONS));
    }

    @Test
    void roundTrip_preservesEnumAndLookupEnumValues() {
        final CampaignOptions original = new CampaignOptions();
        original.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.MAPLESS);
        original.set(CampaignOption.SKILL_LEVEL, SkillLevel.VETERAN);

        final CampaignOptions reloaded = roundTrip(original);

        assertEquals(StratConPlayType.MAPLESS, reloaded.get(CampaignOption.STRAT_CON_PLAY_TYPE));
        assertEquals(SkillLevel.VETERAN, reloaded.get(CampaignOption.SKILL_LEVEL));
    }

    @Test
    void contractPercent_isClampedOnRead() {
        final CampaignOptions reloaded = reloadTags("<dropShipContractPercent>2.0</dropShipContractPercent>");

        assertEquals(1.0, reloaded.get(CampaignOption.DROP_SHIP_CONTRACT_PERCENT));
    }

    @Test
    void atBEnabledMarker_isNotWrittenButReadFromLegacyTag() {
        final CampaignOptions original = new CampaignOptions();
        original.set(CampaignOption.HAD_AT_B_ENABLED_MARKER, true);

        // The marker is a read-only migration flag: it is never emitted, so it does not survive a round-trip.
        assertFalse(roundTrip(original).get(CampaignOption.HAD_AT_B_ENABLED_MARKER));

        // It is still recovered from the legacy "useAtB" tag on load.
        assertTrue(reloadTags("<useAtB>true</useAtB>").get(CampaignOption.HAD_AT_B_ENABLED_MARKER));
    }

    @Test
    void legacyAliasTags_areAppliedToTheirOptions() {
        assertEquals(42, reloadTags("<administrativeStrain>42</administrativeStrain>").get(CampaignOption.HR_CAPACITY));
        assertTrue(reloadTags("<useAdministrativeStrain>true</useAdministrativeStrain>").get(CampaignOption.USE_HR_STRAIN));
        assertEquals(AcquisitionsType.NEGOTIATION,
              reloadTags("<acquisitionSkill>Negotiation</acquisitionSkill>").get(CampaignOption.ACQUISITIONS_TYPE));
        assertEquals(StratConPlayType.MAPLESS,
              reloadTags("<useMaplessStratCon>true</useMaplessStratCon>").get(CampaignOption.STRAT_CON_PLAY_TYPE));
    }
}
