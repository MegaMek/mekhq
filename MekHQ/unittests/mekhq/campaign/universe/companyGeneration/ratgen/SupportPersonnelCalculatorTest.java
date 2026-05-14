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
package mekhq.campaign.universe.companyGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.ratgen.SupportPersonnelCalculator.SupportDemand;
import org.junit.jupiter.api.Test;

class SupportPersonnelCalculatorTest {

    @Test
    void compute_nullCampaign_returnsZeros() {
        SupportDemand demand = SupportPersonnelCalculator.compute(null);

        assertEquals(0, demand.mekTechsNeeded());
        assertEquals(0, demand.mechanicsNeeded());
        assertEquals(0, demand.aeroTeksNeeded());
        assertEquals(0, demand.baTechsNeeded());
        assertEquals(0, demand.doctorsNeeded());
        assertEquals(0, demand.administratorsNeeded());
    }

    @Test
    void compute_emptyCampaign_returnsZeros() {
        Campaign campaign = standardCampaign(List.of(), 0);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(0, demand.mekTechsNeeded());
        assertEquals(0, demand.mechanicsNeeded());
        assertEquals(0, demand.aeroTeksNeeded());
        assertEquals(0, demand.baTechsNeeded());
        assertEquals(0, demand.doctorsNeeded());
        assertEquals(0, demand.administratorsNeeded());
    }

    @Test
    void compute_singleMek_oneMekTech() {
        Campaign campaign = standardCampaign(List.of(mek()), 1);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(1, demand.mekTechsNeeded());
        assertEquals(0, demand.mechanicsNeeded());
        assertEquals(0, demand.aeroTeksNeeded());
        assertEquals(0, demand.baTechsNeeded());
    }

    @Test
    void compute_twelveMeks_twelveMekTechs() {
        Campaign campaign = standardCampaign(repeat(this::mek, 12), 36);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(12, demand.mekTechsNeeded());
    }

    @Test
    void compute_fiveProtoMeks_oneMekTech() {
        Campaign campaign = standardCampaign(repeat(() -> protoMek(1), 5), 5);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(1, demand.mekTechsNeeded(), "5 ProtoMeks = 1 Point = 1 Mek Tech");
    }

    @Test
    void compute_sixProtoMeks_twoMekTechs() {
        Campaign campaign = standardCampaign(repeat(() -> protoMek(1), 6), 6);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(2, demand.mekTechsNeeded(), "ceil(6/5) Points = 2 Mek Techs");
    }

    @Test
    void compute_meksAndProtoMeksCombined() {
        List<Unit> units = new ArrayList<>();
        units.addAll(repeat(this::mek, 4));
        units.addAll(repeat(() -> protoMek(1), 10)); // 2 Points
        Campaign campaign = standardCampaign(units, 14);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(6, demand.mekTechsNeeded(), "4 Meks + 2 ProtoMek Points = 6 Mek Techs");
    }

    @Test
    void compute_eightBaSuits_twoBaTechs() {
        Campaign campaign = standardCampaign(List.of(battleArmor(8)), 8);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(2, demand.baTechsNeeded(), "ceil(8/5) squads = 2 BA Techs");
    }

    @Test
    void compute_twelveBaSuits_threeBaTechs() {
        Campaign campaign = standardCampaign(List.of(battleArmor(12)), 12);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(3, demand.baTechsNeeded(), "ceil(12/5) squads = 3 BA Techs");
    }

    @Test
    void compute_tenVehicles_tenMechanics() {
        Campaign campaign = standardCampaign(repeat(this::tank, 10), 30);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(0, demand.mekTechsNeeded());
        assertEquals(10, demand.mechanicsNeeded());
    }

    @Test
    void compute_aeroMix_sumsIntoAeroTek() {
        List<Unit> units = new ArrayList<>();
        units.addAll(repeat(this::aerospaceFighter, 4));
        units.addAll(repeat(this::convFighter, 3));
        units.addAll(repeat(this::smallCraft, 2));
        Campaign campaign = standardCampaign(units, 9);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(9, demand.aeroTeksNeeded(), "ASF + ConvFighter + SmallCraft all sum into Aero Tek");
    }

    @Test
    void compute_convFighterAlone_countedAsAeroTek() {
        // Regression test: SupportRating.calculateTechnicianRequirements at line 219 forgets
        // ConvFighters and the calculator must not inherit that bug. ConvFighters use S_TECH_AERO
        // per Avionics.isRightTechType.
        Campaign campaign = standardCampaign(List.of(convFighter()), 1);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(1, demand.aeroTeksNeeded());
        assertEquals(0, demand.mechanicsNeeded(), "ConvFighters are NOT mechanic work");
    }

    @Test
    void compute_mothballedUnits_excludedFromTechDemand() {
        Unit live = mek();
        Unit mothballed = mek();
        when(mothballed.isMothballed()).thenReturn(true);
        Campaign campaign = standardCampaign(List.of(live, mothballed), 3);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(1, demand.mekTechsNeeded(), "Mothballed Mek does not contribute to demand");
    }

    @Test
    void compute_unitWithoutEntity_skipped() {
        Unit broken = mock(Unit.class);
        when(broken.isMothballed()).thenReturn(false);
        when(broken.getEntity()).thenReturn(null);
        Campaign campaign = standardCampaign(List.of(broken, mek()), 1);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(1, demand.mekTechsNeeded());
    }

    @Test
    void compute_dropShipIgnored_forTechCounts() {
        // DropShips, JumpShips, etc. are maintained by their VESSEL_CREW. The calculator must not
        // count them toward any tech-role demand.
        Unit dropShip = unitWith(e -> {
            // No flags set: not Mek, not Vehicle, not Aero, not BA, not ProtoMek.
            // Matches a DropShip's behavior — none of the isMek/isVehicle/isAerospaceFighter/etc.
            // mock methods return true.
        }, 50);
        Campaign campaign = standardCampaign(List.of(dropShip), 50);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(0, demand.mekTechsNeeded());
        assertEquals(0, demand.mechanicsNeeded());
        assertEquals(0, demand.aeroTeksNeeded());
        assertEquals(0, demand.baTechsNeeded());
    }

    @Test
    void compute_doctorDemand_personnelDividedByMaxPatients() {
        // 100 personnel + 0 techs = 100. ceil(100/25) = 4 doctors.
        Campaign campaign = standardCampaign(List.of(), 100);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(4, demand.doctorsNeeded());
    }

    @Test
    void compute_doctorDemand_includesProjectedTechs() {
        // 30 personnel base + 12 projected Mek Techs = 42 effective. ceil(42/25) = 2 doctors.
        Campaign campaign = standardCampaign(repeat(this::mek, 12), 30);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(12, demand.mekTechsNeeded());
        assertEquals(2, demand.doctorsNeeded(), "Doctor demand sees 30 base + 12 techs = 42");
    }

    @Test
    void compute_doctorDemand_zeroMaxPatientsFallsBackToOnePatientPerDoctor() {
        Campaign campaign = standardCampaign(List.of(), 5);
        when(campaign.getCampaignOptions().getMaximumPatients()).thenReturn(0);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        // Defensive fallback: divisor clamped to 1, so 5 personnel = 5 doctors.
        assertEquals(5, demand.doctorsNeeded());
    }

    @Test
    void compute_adminDemand_standardFaction_oneAdminPerTwentyPersonnel() {
        Campaign campaign = standardCampaign(List.of(), 40);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(2, demand.administratorsNeeded(), "ceil(40/20) for House faction");
    }

    @Test
    void compute_adminDemand_mercenaryFaction_oneAdminPerTenPersonnel() {
        Campaign campaign = standardCampaign(List.of(), 40);
        when(campaign.getFaction().isMercenary()).thenReturn(true);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(4, demand.administratorsNeeded(), "ceil(40/10) for mercenary");
    }

    @Test
    void compute_adminDemand_pirateFaction_oneAdminPerTenPersonnel() {
        Campaign campaign = standardCampaign(List.of(), 40);
        when(campaign.getFaction().isPirate()).thenReturn(true);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(4, demand.administratorsNeeded(), "ceil(40/10) for pirate");
    }

    @Test
    void compute_adminDemand_includesProjectedTechs() {
        // 4 Meks = 4 Mek Techs. 20 base personnel + 4 techs = 24. ceil(24/20) = 2 admins.
        Campaign campaign = standardCampaign(repeat(this::mek, 4), 20);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(2, demand.administratorsNeeded(), "Admin demand sees 20 base + 4 techs = 24");
    }

    @Test
    void compute_houseDavionMekCompany_3025_realisticBaseline() {
        // House Davion Mek Company at 3025: 12 Meks, 36 MekWarriors (3 per lance × 12... wait,
        // 12 Meks = 12 pilots in MekHQ). Standard faction, default maxPatients = 25.
        Campaign campaign = standardCampaign(repeat(this::mek, 12), 12);

        SupportDemand demand = SupportPersonnelCalculator.compute(campaign);

        assertEquals(12, demand.mekTechsNeeded());
        assertEquals(0, demand.mechanicsNeeded());
        assertEquals(0, demand.aeroTeksNeeded());
        assertEquals(0, demand.baTechsNeeded());
        // 12 pilots + 12 techs = 24. ceil(24/25) = 1 doctor, ceil(24/20) = 2 admins.
        assertEquals(1, demand.doctorsNeeded());
        assertEquals(2, demand.administratorsNeeded());
    }

    @Test
    void totalTechsNeeded_sumsTheFourTechRoles() {
        SupportDemand demand = new SupportDemand(3, 2, 1, 4, 99, 99);
        assertEquals(10, demand.totalTechsNeeded());
    }

    @Test
    void applyPercent_at100Percent_returnsBaseline() {
        assertEquals(12, SupportPersonnelCalculator.applyPercent(12, 100));
    }

    @Test
    void applyPercent_at0Percent_returnsZero() {
        assertEquals(0, SupportPersonnelCalculator.applyPercent(12, 0));
    }

    @Test
    void applyPercent_at150Percent_roundsUp() {
        assertEquals(18, SupportPersonnelCalculator.applyPercent(12, 150));
    }

    @Test
    void applyPercent_at50Percent_roundsUp() {
        // ceil(12 * 0.5) = 6 — exact. ceil(7 * 0.5) = 4 — proves rounding up.
        assertEquals(4, SupportPersonnelCalculator.applyPercent(7, 50));
    }

    @Test
    void applyPercent_zeroBaseline_returnsZero() {
        assertEquals(0, SupportPersonnelCalculator.applyPercent(0, 200));
    }

    @Test
    void applyPercent_negativePercent_clampsToZero() {
        assertEquals(0, SupportPersonnelCalculator.applyPercent(5, -50));
    }

    // ===== Mock helpers =====

    /**
     * Builds a Campaign with the given units and personnel headcount, standard (House) faction,
     * default {@code maximumPatients = 25}. Individual tests can override stubs via {@code when()}.
     */
    private Campaign standardCampaign(List<Unit> units, int personnelCount) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getActiveUnits()).thenReturn(units == null ? Collections.emptyList() : units);

        List<Person> personnel = new ArrayList<>();
        for (int i = 0; i < personnelCount; i++) {
            personnel.add(mock(Person.class));
        }
        lenient().when(campaign.getActivePersonnel(false, false)).thenReturn(personnel);

        CampaignOptions options = mock(CampaignOptions.class);
        lenient().when(options.getMaximumPatients()).thenReturn(25);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Faction faction = mock(Faction.class);
        lenient().when(faction.isPirate()).thenReturn(false);
        lenient().when(faction.isMercenary()).thenReturn(false);
        when(campaign.getFaction()).thenReturn(faction);

        return campaign;
    }

    private Unit unitWith(Consumer<Entity> entityConfig, int fullCrewSize) {
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        entityConfig.accept(entity);
        lenient().when(unit.isMothballed()).thenReturn(false);
        lenient().when(unit.getEntity()).thenReturn(entity);
        lenient().when(unit.getFullCrewSize()).thenReturn(fullCrewSize);
        return unit;
    }

    private Unit mek() {
        return unitWith(e -> when(e.isMek()).thenReturn(true), 1);
    }

    private Unit protoMek(int crew) {
        return unitWith(e -> when(e.isProtoMek()).thenReturn(true), crew);
    }

    private Unit tank() {
        return unitWith(e -> when(e.isVehicle()).thenReturn(true), 3);
    }

    private Unit aerospaceFighter() {
        return unitWith(e -> when(e.isAerospaceFighter()).thenReturn(true), 1);
    }

    private Unit convFighter() {
        return unitWith(e -> when(e.isConventionalFighter()).thenReturn(true), 1);
    }

    private Unit smallCraft() {
        return unitWith(e -> when(e.isSmallCraft()).thenReturn(true), 4);
    }

    private Unit battleArmor(int suits) {
        return unitWith(e -> when(e.isBattleArmor()).thenReturn(true), suits);
    }

    private static <T> List<T> repeat(java.util.function.Supplier<T> supplier, int count) {
        List<T> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(supplier.get());
        }
        return out;
    }
}
