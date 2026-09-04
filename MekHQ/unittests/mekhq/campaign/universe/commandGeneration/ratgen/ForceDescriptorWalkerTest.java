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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.common.universe.Factions2;
import megamek.client.ratgenerator.FormationNamingConvention.DesignatorStyle;
import megamek.client.ratgenerator.FormationNamingConvention.Tier;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies that {@link ForceDescriptorWalker} honors preview exclusions: excluded leaves are skipped,
 * formations whose units are all excluded are dropped, and a single re-included unit keeps its
 * formation. Created formations are named through {@link FormationNamer}.
 */
class ForceDescriptorWalkerTest {

    /**
     * Drops any {@link Factions2} left behind by an earlier test class, so the assertions below read the real
     * universe data rather than whatever the previous class happened to install.
     *
     * <p>{@code Factions2} is a static singleton, and {@code Factions.load(true)} resolves it through
     * {@code Factions2.getInstance(true)}, which caches an instance built from the cut-down test directory. That
     * directory holds factions but no commands, so once any of the dozen test classes that load faction data for
     * testing has run, {@code WOB.PM} and every other subcommand has vanished for the rest of the JVM. Clearing the
     * singleton here forces the no-argument constructor to reload both factions and commands, which makes this class
     * independent of the order it runs in.</p>
     */
    @BeforeAll
    static void useRealUniverseData() {
        Factions2.setInstance(null);
    }

    private static final int ECHELON_LANCE = 3;
    private static final int ECHELON_COMPANY = 4;
    private static final int ECHELON_BATTALION = 5;

    /**
     * The Inner Sphere naming rules, supplied directly rather than read from the faction rulesets: this
     * test suite does not load MegaMek's data set, so the production resolver would find no rule for
     * any echelon and every formation would fall back to keeping its ruleset name. That the shipped
     * rulesets really do declare these rules is covered by
     * {@code megamek.client.ratgenerator.FormationNamingConventionTest}.
     */
    private static FormationNamer namer() {
        Map<Integer, Tier> innerSphereRules = Map.of(
              ECHELON_BATTALION, new Tier(ECHELON_BATTALION, DesignatorStyle.ALPHABET, false),
              ECHELON_COMPANY, new Tier(ECHELON_COMPANY, DesignatorStyle.ALPHABET, false),
              ECHELON_LANCE, new Tier(ECHELON_LANCE, DesignatorStyle.ALPHABET, false));
        return new FormationNamer(ForceNamingMethod.CCB_1943, List.of(),
              (faction, echelon) -> innerSphereRules.get(echelon));
    }

    /** {@code ForceDescriptor.entity} has no setter (set only during generation), so inject via reflection. */
    private static void injectEntity(ForceDescriptor descriptor) throws Exception {
        Field field = ForceDescriptor.class.getDeclaredField("entity");
        field.setAccessible(true);
        field.set(descriptor, mock(Entity.class));
    }

    private static ForceDescriptor group(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }

    private static ForceDescriptor unit(String name) throws Exception {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        injectEntity(descriptor);
        return descriptor;
    }

    /** A platoon-echelon node of {@code unitType} whose children are all leaf units (a "loose platoon"). */
    private static ForceDescriptor loosePlatoon(String name, int unitType) throws Exception {
        ForceDescriptor platoon = new ForceDescriptor();
        platoon.setName(name);
        platoon.setEchelon(3);
        platoon.setUnitType(unitType);
        platoon.addSubForce(unit(name + " Unit A"));
        platoon.addSubForce(unit(name + " Unit B"));
        return platoon;
    }

    private static List<String> namesOfCreatedFormations(Campaign campaign) {
        ArgumentCaptor<Formation> captor = ArgumentCaptor.forClass(Formation.class);
        verify(campaign.getPlayerForce(), atLeastOnce()).addFormation(captor.capture(), any(), any());
        return captor.getAllValues().stream().map(Formation::getName).toList();
    }

    @Test
    void excludedSubtreeIsSkippedAndEmptyFormationDropped() throws Exception {
        ForceDescriptor root = group("Task Force");
        ForceDescriptor firstLance = group("First Lance");
        ForceDescriptor secondLance = group("Second Lance");
        firstLance.addSubForce(unit("Unit A"));
        firstLance.addSubForce(unit("Unit B"));
        secondLance.addSubForce(unit("Unit C"));
        secondLance.addSubForce(unit("Unit D"));
        root.addSubForce(firstLance);
        root.addSubForce(secondLance);

        secondLance.setIncludedRecursively(false);

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        List<String> handledUnits = new ArrayList<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> handledUnits.add(leaf.parseName()));

        assertEquals(List.of("Unit A", "Unit B"), handledUnits, "excluded lance's units must be skipped");

        List<String> createdFormations = namesOfCreatedFormations(campaign);
        assertTrue(createdFormations.contains("First Lance"));
        assertFalse(createdFormations.contains("Second Lance"), "a fully excluded lance must not create a formation");
    }

    @Test
    void reIncludedUnitInsideExcludedFormationIsKept() throws Exception {
        ForceDescriptor root = group("Task Force");
        ForceDescriptor lance = group("Second Lance");
        ForceDescriptor unitC = unit("Unit C");
        ForceDescriptor unitD = unit("Unit D");
        lance.addSubForce(unitC);
        lance.addSubForce(unitD);
        root.addSubForce(lance);

        lance.setIncludedRecursively(false);
        unitC.setIncluded(true);

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        List<String> handledUnits = new ArrayList<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> handledUnits.add(leaf.parseName()));

        assertEquals(List.of("Unit C"), handledUnits, "the re-included unit is kept, its excluded sibling dropped");
        assertTrue(namesOfCreatedFormations(campaign).contains("Second Lance"),
              "a lance with one re-included unit keeps its formation");
    }

    /**
     * The preview names must be exactly the names the build produces: both run the same traversal with
     * equally configured namers, and this test pins that contract.
     */
    @Test
    void previewNamesMatchTheBuiltFormationNames() throws Exception {
        ForceDescriptor root = group("Battalion");
        root.setEchelon(5);
        ForceDescriptor firstCompany = group("A Company");
        firstCompany.setEchelon(4);
        ForceDescriptor battleLance = group("Battle Lance");
        battleLance.setEchelon(3);
        battleLance.addSubForce(unit("Unit A"));
        ForceDescriptor fireLance = group("Fire Lance");
        fireLance.setEchelon(3);
        fireLance.addSubForce(unit("Unit B"));
        firstCompany.addSubForce(battleLance);
        firstCompany.addSubForce(fireLance);
        ForceDescriptor secondCompany = group("A Company");
        secondCompany.setEchelon(4);
        ForceDescriptor secondBattleLance = group("Battle Lance");
        secondBattleLance.setEchelon(3);
        secondBattleLance.addSubForce(unit("Unit C"));
        secondCompany.addSubForce(secondBattleLance);
        root.addSubForce(firstCompany);
        root.addSubForce(secondCompany);

        var previewNames = ForceDescriptorWalker.previewNames(root, namer());

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> { });
        List<String> builtNames = namesOfCreatedFormations(campaign);

        assertEquals(builtNames.size(), previewNames.size(),
              "every built formation (no synthesized companies here) must have a previewed name");
        assertEquals("Able Company", previewNames.get(firstCompany));
        assertEquals("Able Battle Lance", previewNames.get(battleLance));
        assertEquals("Baker Fire Lance", previewNames.get(fireLance));
        assertEquals("Baker Company", previewNames.get(secondCompany));
        assertEquals("Able Battle Lance", previewNames.get(secondBattleLance));
        assertTrue(builtNames.containsAll(previewNames.values()),
              "the preview must show exactly the names the build produces");
    }

    @Test
    void looseAttachedPlatoonsWrapIntoUnitTypeCompany() throws Exception {
        ForceDescriptor root = group("Regiment");
        // Two loose Battle Armor platoons attached directly to the root (no company wrapper).
        root.addAttached(loosePlatoon("Platoon", UnitType.BATTLE_ARMOR));
        root.addAttached(loosePlatoon("Platoon", UnitType.BATTLE_ARMOR));

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        List<String> handledUnits = new ArrayList<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> handledUnits.add(leaf.parseName()));

        assertEquals(4, handledUnits.size(), "all four platoon units are placed");

        List<String> createdFormations = namesOfCreatedFormations(campaign);
        // The synthesized company is company-tier, so the namer prefixes the first CCB
        // designator; its platoons restart the same alphabet inside it.
        String expectedCompany = "Able " + UnitType.getTypeDisplayableName(UnitType.BATTLE_ARMOR) + " Company";
        assertTrue(createdFormations.contains(expectedCompany),
              "loose BA platoons should nest under a synthesized '" + expectedCompany + "'");
        assertTrue(createdFormations.contains("Able Platoon") && createdFormations.contains("Baker Platoon"),
              "platoons take the selected alphabet within their synthesized company");
        assertEquals(1, createdFormations.stream().filter(expectedCompany::equals).count(),
              "both platoons share one synthesized company (grouped by unit type)");
    }
    /**
     * The Word of Blake Protectorate Militia is raised as a conventional planetary force, and its
     * ruleset builds lances, companies, battalions and regiments rather than the Level I to VI ladder.
     * Read as ComStar, those echelon numbers mean something else entirely - a lance and a company both
     * come out as Level II - so the labels contradicted the formations actually built.
     */
    @Test
    void theProtectorateMilitiaIsLabelledOnInnerSphereEchelons() {
        assertEquals(FormationLevel.LANCE,
              ForceDescriptorWalker.mapEchelonToFormationLevel(3, "WOB.PM"));
        assertEquals(FormationLevel.COMPANY,
              ForceDescriptorWalker.mapEchelonToFormationLevel(4, "WOB.PM"));
        assertEquals(FormationLevel.BATTALION,
              ForceDescriptorWalker.mapEchelonToFormationLevel(5, "WOB.PM"));
        assertEquals(FormationLevel.REGIMENT,
              ForceDescriptorWalker.mapEchelonToFormationLevel(6, "WOB.PM"));
    }

    /** The rest of the Word of Blake keeps the ComStar ladder its rulesets are written on. */
    @Test
    void theRestOfTheWordOfBlakeKeepsTheComStarLadder() {
        assertEquals(FormationLevel.LEVEL_III,
              ForceDescriptorWalker.mapEchelonToFormationLevel(5, "WOB"));
        assertEquals(FormationLevel.LEVEL_IV,
              ForceDescriptorWalker.mapEchelonToFormationLevel(6, "WOB"));
        assertEquals(FormationLevel.LEVEL_IV,
              ForceDescriptorWalker.mapEchelonToFormationLevel(6, "WOB.SD"),
              "the Shadow Divisions are organised the Word of Blake way");
        assertEquals(FormationLevel.LEVEL_IV,
              ForceDescriptorWalker.mapEchelonToFormationLevel(6, "CS"));
    }

    /**
     * The family now comes from the command's declared formation base size, so this is the assertion
     * that fails loudly if WOB.PM.yml ever loses its {@code formationBaseSize: 4}.
     */
    @Test
    void theProtectorateMilitiaStillDeclaresItsOwnFormationSize() {
        assertEquals(4,
              Factions2.getInstance().getFaction("WOB.PM")
                    .orElseThrow(() -> new AssertionError("WOB.PM must be a known command"))
                    .getFormationBaseSize(),
              "WOB.PM.yml must keep formationBaseSize: 4 - it is the only thing separating the"
                    + " Protectorate Militia from the ComStar ladder");
    }

    /**
     * The Marian Hegemony builds on fives like the Clans do, but is not one. Reading a five as Clan
     * would have labelled Marian lances and companies as Stars and Clusters.
     */
    @Test
    void theMarianHegemonyIsNotMistakenForAClan() {
        assertEquals(FormationLevel.LANCE,
              ForceDescriptorWalker.mapEchelonToFormationLevel(3, "MH"));
        assertEquals(FormationLevel.COMPANY,
              ForceDescriptorWalker.mapEchelonToFormationLevel(4, "MH"));
        assertEquals(FormationLevel.REGIMENT,
              ForceDescriptorWalker.mapEchelonToFormationLevel(6, "MH"));
    }

    @Test
    void ordinaryInnerSphereAndClanFactionsAreUnchanged() {
        assertEquals(FormationLevel.COMPANY,
              ForceDescriptorWalker.mapEchelonToFormationLevel(4, "FS"));
        assertEquals(FormationLevel.CLUSTER,
              ForceDescriptorWalker.mapEchelonToFormationLevel(6, "CJF"));
        // ratgen sometimes packs extra tokens onto the code; only the first is the faction.
        assertEquals(FormationLevel.COMPANY,
              ForceDescriptorWalker.mapEchelonToFormationLevel(4, "FS,FedSuns,3030"));
    }

    @Test
    void anUnknownOrAbsentFactionFallsBackToTheInnerSphere() {
        assertEquals(FormationLevel.COMPANY,
              ForceDescriptorWalker.mapEchelonToFormationLevel(4, null));
        assertEquals(FormationLevel.COMPANY,
              ForceDescriptorWalker.mapEchelonToFormationLevel(4, "   "));
        assertEquals(FormationLevel.COMPANY,
              ForceDescriptorWalker.mapEchelonToFormationLevel(4, "NOT_A_FACTION"));
    }

    /**
     * A carrier is generated with the fighters it carries nested under it. The ship is still a unit: it
     * is handed to the handler, and its fighters are walked under the same formation the ship is in.
     */
    @Test
    void aShipWithFightersNestedUnderItIsStillAUnit() throws Exception {
        ForceDescriptor root = group("Task Force");
        ForceDescriptor lance = group("First Lance");
        lance.addSubForce(unit("Unit A"));
        root.addSubForce(lance);
        ForceDescriptor transports = group("Naval Units");
        ForceDescriptor ship = unit("Leopard");
        ForceDescriptor flight = group("Flight 1");
        flight.addSubForce(unit("Fighter A"));
        flight.addSubForce(unit("Fighter B"));
        ship.addAttached(flight);
        transports.addSubForce(ship);
        root.addAttached(transports);

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        List<String> handledUnits = new ArrayList<>();
        Map<String, Formation> parentOfUnit = new HashMap<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(), (leaf, parent) -> {
            handledUnits.add(leaf.parseName());
            parentOfUnit.put(leaf.parseName(), parent);
        });

        assertEquals(List.of("Unit A", "Leopard", "Fighter A", "Fighter B"), handledUnits,
              "the ship is handed over as a unit, followed by what it carries");
        List<String> createdFormations = namesOfCreatedFormations(campaign);
        assertFalse(createdFormations.contains("Leopard"), "the ship must not become a formation");
        assertTrue(createdFormations.contains("Flight 1"), "the flight under the ship is a formation");
        assertEquals("Flight 1", parentOfUnit.get("Fighter A").getName());
        assertEquals(parentOfUnit.get("Leopard").getName(), createdFormations.get(createdFormations.indexOf("Flight 1") - 1),
              "the flight hangs off the formation the ship is in");
    }

    @Test
    void anExcludedShipStillLetsTheFightersUnderItThrough() throws Exception {
        ForceDescriptor root = group("Task Force");
        ForceDescriptor lance = group("First Lance");
        lance.addSubForce(unit("Unit A"));
        root.addSubForce(lance);
        ForceDescriptor ship = unit("Leopard");
        ForceDescriptor flight = group("Flight 1");
        flight.addSubForce(unit("Fighter A"));
        ship.addAttached(flight);
        root.addAttached(ship);

        ship.setIncluded(false);

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        List<String> handledUnits = new ArrayList<>();
        ForceDescriptorWalker.walk(root, campaign, new Formation("Headquarters"), namer(),
              (leaf, parent) -> handledUnits.add(leaf.parseName()));

        assertEquals(List.of("Unit A", "Fighter A"), handledUnits);
    }
}
