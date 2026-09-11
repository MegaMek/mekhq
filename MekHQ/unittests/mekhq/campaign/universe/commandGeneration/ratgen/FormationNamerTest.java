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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.ratgenerator.FormationNamingConvention.DesignatorStyle;
import megamek.client.ratgenerator.FormationNamingConvention.Tier;
import megamek.common.units.UnitType;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationNamer.FormationRequest;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationNamer.NamedFormation;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link FormationNamer}: the naming method the player selects drives the designator at every
 * echelon, each echelon restarts its sequence under each parent, and names the faction ruleset already
 * got right - Clan and ComStar in particular - are preserved rather than overwritten.
 *
 * <p>The naming rules are supplied by a stub resolver rather than read from the shipped faction
 * rulesets, so these tests exercise the naming logic without depending on MegaMek's data set or its
 * global loader state. That the shipped data declares the rules used here is covered separately by
 * {@code megamek.client.ratgenerator.FormationNamingConventionTest}.</p>
 */
class FormationNamerTest {

    private static final int ECHELON_LANCE = 3;
    private static final int ECHELON_COMPANY = 4;
    private static final int ECHELON_BATTALION = 5;
    private static final int ECHELON_REGIMENT = 6;
    private static final int ECHELON_TRINARY = 5;
    private static final int ECHELON_CLUSTER = 6;
    private static final int ECHELON_GALAXY = 7;
    private static final int ECHELON_LEVEL_IV = 6;

    private static final String FACTION = "TEST";

    /**
     * Mirrors the shipped Inner Sphere convention: the player's selected alphabet at every echelon,
     * unqualified, restarting under each parent.
     */
    private static Map<Integer, Tier> innerSphereRules() {
        Map<Integer, Tier> rules = new HashMap<>();
        rules.put(ECHELON_REGIMENT, new Tier(ECHELON_REGIMENT, DesignatorStyle.ALPHABET, false));
        rules.put(ECHELON_BATTALION, new Tier(ECHELON_BATTALION, DesignatorStyle.ALPHABET, false));
        rules.put(ECHELON_COMPANY, new Tier(ECHELON_COMPANY, DesignatorStyle.ALPHABET, false));
        rules.put(ECHELON_LANCE, new Tier(ECHELON_LANCE, DesignatorStyle.ALPHABET, false));
        return rules;
    }

    private static FormationNamer namer(ForceNamingMethod method, Map<Integer, Tier> rules,
          String... existingNames) {
        return new FormationNamer(method, List.of(existingNames),
              (faction, echelon) -> rules.get(echelon));
    }

    private static FormationRequest request(String engineName, FormationLevel level, int echelon) {
        return request(engineName, level, echelon, UnitType.MEK);
    }

    private static FormationRequest request(String engineName, FormationLevel level, int echelon,
          int unitType) {
        return new FormationRequest(engineName, level, echelon, unitType, FACTION);
    }

    private static List<String> names(List<NamedFormation> named) {
        List<String> plain = new ArrayList<>(named.size());
        for (NamedFormation formation : named) {
            plain.add(formation.name());
        }
        return plain;
    }

    // region The reported defects

    /**
     * The whole shape of the reported problem in one test: the selected alphabet reaches every level of
     * the tree, and every level restarts it under its own parent.
     */
    @Test
    void theChosenAlphabetIsUsedAtEveryEchelonAndRestartsUnderEachParent() {
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules());

        List<NamedFormation> regiments = namer.nameSiblings(List.of(
              request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT),
              request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT)), null);
        assertEquals(List.of("Alpha Mek Regiment", "Beta Mek Regiment"), names(regiments));

        List<FormationRequest> battalions = List.of(
              request("Battalion", FormationLevel.BATTALION, ECHELON_BATTALION),
              request("Battalion", FormationLevel.BATTALION, ECHELON_BATTALION));
        List<NamedFormation> firstRegimentBattalions =
              namer.nameSiblings(battalions, regiments.get(0).designator());
        List<NamedFormation> secondRegimentBattalions =
              namer.nameSiblings(battalions, regiments.get(1).designator());
        assertEquals(List.of("Alpha Battalion", "Beta Battalion"), names(firstRegimentBattalions));
        assertEquals(List.of("Alpha Battalion", "Beta Battalion"), names(secondRegimentBattalions),
              "the second regiment's battalions restart the alphabet rather than continuing it");

        List<FormationRequest> companies = List.of(
              request("A Company", FormationLevel.COMPANY, ECHELON_COMPANY),
              request("B Company", FormationLevel.COMPANY, ECHELON_COMPANY),
              request("C Company", FormationLevel.COMPANY, ECHELON_COMPANY));

        // The reported defect: the second battalion used to continue into Delta/Epsilon/Zeta.
        assertEquals(List.of("Alpha Company", "Beta Company", "Gamma Company"),
              names(namer.nameSiblings(companies, firstRegimentBattalions.get(0).designator())));
        assertEquals(List.of("Alpha Company", "Beta Company", "Gamma Company"),
              names(namer.nameSiblings(companies, firstRegimentBattalions.get(1).designator())));

        // Lances follow the same scheme rather than switching to a different one.
        assertEquals(List.of("Alpha Command Lance", "Beta Fire Lance"),
              names(namer.nameSiblings(List.of(
                    request("Command Lance", FormationLevel.LANCE, ECHELON_LANCE),
                    request("Fire Lance", FormationLevel.LANCE, ECHELON_LANCE)), "Alpha")));
    }

    @Test
    void twoRegimentsOfTheSameNameAreBothDesignatedRatherThanOneGettingACounter() {
        FormationNamer namer = namer(ForceNamingMethod.CCB_1943, innerSphereRules());

        List<NamedFormation> regiments = namer.nameSiblings(List.of(
              request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT),
              request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT)), null);

        // The reported defect produced "Mek Regiment" and "Mek Regiment (2)".
        assertEquals(List.of("Able Mek Regiment", "Baker Mek Regiment"), names(regiments));
    }

    @Test
    void everyNamingMethodDrivesTheDesignatorAtEveryEchelon() {
        List<FormationRequest> companies = List.of(
              request("A Company", FormationLevel.COMPANY, ECHELON_COMPANY),
              request("B Company", FormationLevel.COMPANY, ECHELON_COMPANY));
        List<FormationRequest> regiments = List.of(
              request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT));

        assertEquals(List.of("Able Company", "Baker Company"),
              names(namer(ForceNamingMethod.CCB_1943, innerSphereRules())
                          .nameSiblings(companies, null)));
        assertEquals(List.of("Able Mek Regiment"),
              names(namer(ForceNamingMethod.CCB_1943, innerSphereRules())
                          .nameSiblings(regiments, null)));

        // ICAO spells the first letter "Alfa", which is what distinguishes it from Greek at position A.
        assertEquals(List.of("Alfa Company", "Bravo Company"),
              names(namer(ForceNamingMethod.ICAO_1956, innerSphereRules())
                          .nameSiblings(companies, null)));
        assertEquals(List.of("Alpha Company", "Beta Company"),
              names(namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules())
                          .nameSiblings(companies, null)));
        assertEquals(List.of("A Company", "B Company"),
              names(namer(ForceNamingMethod.ENGLISH_ALPHABET, innerSphereRules())
                          .nameSiblings(companies, null)));
        assertEquals(List.of("A Mek Regiment"),
              names(namer(ForceNamingMethod.ENGLISH_ALPHABET, innerSphereRules())
                          .nameSiblings(regiments, null)));
    }

    // endregion

    // region Always number regiments

    @Test
    void numberingRegimentsSwitchesThemToOrdinalsAndCountsEachTypeSeparately() {
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules());
        namer.setAlwaysNumberRegiments(true);

        List<NamedFormation> regiments = namer.nameSiblings(List.of(
              request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT, UnitType.MEK),
              request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT, UnitType.MEK),
              request("Armor Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT, UnitType.TANK),
              request("Infantry Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT,
                    UnitType.INFANTRY)), null);

        assertEquals(List.of("1st Mek Regiment", "2nd Mek Regiment",
                    "1st Armor Regiment", "1st Infantry Regiment"),
              names(regiments), "each type of regiment starts again at 1st");
    }

    @Test
    void numberingRegimentsLeavesEveryOtherEchelonOnTheChosenAlphabet() {
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules());
        namer.setAlwaysNumberRegiments(true);

        NamedFormation regiment = namer.nameSiblings(
              List.of(request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT)), null).get(0);
        assertEquals("1st Mek Regiment", regiment.name());

        // The option is scoped to regiments; battalions and below still follow the dropdown.
        assertEquals(List.of("Alpha Battalion", "Beta Battalion"),
              names(namer.nameSiblings(List.of(
                    request("Battalion", FormationLevel.BATTALION, ECHELON_BATTALION),
                    request("Battalion", FormationLevel.BATTALION, ECHELON_BATTALION)),
                    regiment.designator())));
    }

    /**
     * The namer itself requires the caller to ask for numbering; it is
     * {@link mekhq.campaign.universe.commandGeneration.CommandGenerationOptions} that turns the option
     * on by default for new campaigns. Unchecking the box must still fall back to the alphabet.
     */
    @Test
    void regimentsKeepTheAlphabetWhenNumberingIsNotRequested() {
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules());
        assertEquals(List.of("Alpha Mek Regiment"),
              names(namer.nameSiblings(
                    List.of(request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT)), null)));
    }

    @Test
    void freshOptionsTurnRegimentNumberingOn() {
        assertTrue(new CommandGenerationOptions().isAlwaysNumberRegiments(),
              "new campaigns should number regiments unless the player unchecks the box");
    }

    @Test
    void numberingRegimentsDoesNotAffectNavalFormationsSharingTheRegimentEchelon() {
        FormationNamer namer = namer(ForceNamingMethod.ICAO_1956, innerSphereRules());
        namer.setAlwaysNumberRegiments(true);

        // A naval Division sits at the Regiment echelon but carries no unit type. The option is about
        // ground regiments, so these keep the naming alphabet.
        List<NamedFormation> divisions = namer.nameSiblings(List.of(
              new FormationRequest("Division", FormationLevel.REGIMENT, ECHELON_REGIMENT, null, FACTION),
              new FormationRequest("Division", FormationLevel.REGIMENT, ECHELON_REGIMENT, null, FACTION)),
              null);
        assertEquals(List.of("Alfa Division", "Bravo Division"), names(divisions));
    }

    @Test
    void numberingRegimentsDoesNotAffectClanClustersSharingTheRegimentEchelon() {
        Map<Integer, Tier> clanRules = new HashMap<>();
        clanRules.put(ECHELON_CLUSTER,
              new Tier(ECHELON_CLUSTER, DesignatorStyle.NUMERIC_ORDINAL, false));
        FormationNamer namer = namer(ForceNamingMethod.CCB_1943, clanRules);
        namer.setAlwaysNumberRegiments(true);

        // A Clan Cluster shares echelon 6 with an Inner Sphere Regiment. The override is keyed on the
        // formation level, not the echelon number, so the Cluster keeps its own convention.
        assertEquals(List.of("1st Assault Cluster"),
              names(namer.nameSiblings(List.of(new FormationRequest("Assault Cluster",
                    FormationLevel.CLUSTER, ECHELON_CLUSTER, UnitType.MEK, FACTION)), null)));
    }

    // endregion

    // region Lances

    @Test
    void lancesTakeTheSameAlphabetAsEveryOtherEchelon() {
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules());
        NamedFormation company = namer.nameSiblings(
              List.of(request("A Company", FormationLevel.COMPANY, ECHELON_COMPANY)), "Alpha").get(0);
        assertEquals("Alpha Company", company.name());

        List<NamedFormation> lances = namer.nameSiblings(List.of(
              request("Battle Lance", FormationLevel.LANCE, ECHELON_LANCE),
              request("Fire Lance", FormationLevel.LANCE, ECHELON_LANCE)), company.designator());
        assertEquals(List.of("Alpha Battle Lance", "Beta Fire Lance"), names(lances));
    }

    @Test
    void identicallyNamedLancesInOneCompanyStayUniqueThroughTheirDesignator() {
        FormationNamer namer = namer(ForceNamingMethod.CCB_1943, innerSphereRules());
        List<NamedFormation> lances = namer.nameSiblings(List.of(
              request("Battle Lance", FormationLevel.LANCE, ECHELON_LANCE),
              request("Battle Lance", FormationLevel.LANCE, ECHELON_LANCE)), "Able");
        assertEquals(List.of("Able Battle Lance", "Baker Battle Lance"), names(lances));
    }

    // endregion

    // region Clan and ComStar canon preservation

    @Test
    void clanGalaxiesTakeGreekLettersEvenWhenThePlayerChoseAnotherAlphabet() {
        Map<Integer, Tier> clanRules = new HashMap<>();
        clanRules.put(ECHELON_GALAXY, new Tier(ECHELON_GALAXY, DesignatorStyle.GREEK, false));

        // The player picked the 1943 CCB alphabet, but galaxies are canonically Greek and ignore it.
        List<NamedFormation> galaxies = namer(ForceNamingMethod.CCB_1943, clanRules).nameSiblings(
              List.of(request("Galaxy", FormationLevel.GALAXY, ECHELON_GALAXY),
                    request("Galaxy", FormationLevel.GALAXY, ECHELON_GALAXY)), null);
        assertEquals(List.of("Alpha Galaxy", "Beta Galaxy"), names(galaxies));
    }

    @Test
    void clanTrinariesKeepTheirRulesetNames() {
        Map<Integer, Tier> clanRules = new HashMap<>();
        clanRules.put(ECHELON_TRINARY, new Tier(ECHELON_TRINARY, DesignatorStyle.ENGINE, false));

        List<NamedFormation> trinaries = namer(ForceNamingMethod.CCB_1943, clanRules).nameSiblings(
              List.of(request("Trinary [Battle]", FormationLevel.BINARY_OR_TRINARY, ECHELON_TRINARY),
                    request("Trinary [Striker]", FormationLevel.BINARY_OR_TRINARY, ECHELON_TRINARY)),
              "Alpha");

        // No prefix, no rewriting: these names carry canon meaning the namer cannot improve on.
        assertEquals(List.of("Trinary [Battle]", "Trinary [Striker]"), names(trinaries));
    }

    @Test
    void comStarLevelNamesSurviveCompletelyIntact() {
        Map<Integer, Tier> comStarRules = new HashMap<>();
        comStarRules.put(ECHELON_LEVEL_IV, new Tier(ECHELON_LEVEL_IV, DesignatorStyle.ENGINE, false));

        List<NamedFormation> levels = namer(ForceNamingMethod.CCB_1943, comStarRules).nameSiblings(
              List.of(request("IV-alpha", FormationLevel.LEVEL_IV, ECHELON_LEVEL_IV),
                    request("IV-beta", FormationLevel.LEVEL_IV, ECHELON_LEVEL_IV)), null);

        // A ComStar Greek suffix denotes branch specialisation, not sequence position: "IV-alpha" and
        // "IV-beta" are different kinds of formation. Neither may gain a prefix, and neither may be
        // advanced into the other. The previous implementation produced "Able IV-alpha" here.
        assertEquals(List.of("IV-alpha", "IV-beta"), names(levels));
    }

    @Test
    void comStarGreekSuffixIsNeverAdvancedToResolveAClashWithAnExistingName() {
        Map<Integer, Tier> comStarRules = new HashMap<>();
        comStarRules.put(ECHELON_LEVEL_IV, new Tier(ECHELON_LEVEL_IV, DesignatorStyle.ENGINE, false));

        // "IV-alpha" already exists in the campaign, and this one sits at the top of a new command where
        // there is no parent to tell them apart. Resolving that by handing this formation "IV-beta"
        // would silently change its branch specialisation, so a counter is used instead.
        FormationNamer namer = namer(ForceNamingMethod.CCB_1943, comStarRules, "IV-alpha");
        String name = namer.nameSiblings(
              List.of(request("IV-alpha", FormationLevel.LEVEL_IV, ECHELON_LEVEL_IV)), null).get(0).name();

        assertTrue(name.startsWith("IV-alpha"),
              "the branch suffix must not be advanced to another letter, got: " + name);
    }

    // endregion

    // region Collisions and fallbacks

    @Test
    void engineNamedSiblingsThatCollideAreAllDesignatedRatherThanOnlyTheDuplicate() {
        Map<Integer, Tier> rules = new HashMap<>();
        rules.put(ECHELON_TRINARY, new Tier(ECHELON_TRINARY, DesignatorStyle.ENGINE, false));

        List<NamedFormation> trinaries = namer(ForceNamingMethod.CCB_1943, rules).nameSiblings(
              List.of(request("Trinary [Battle]", FormationLevel.BINARY_OR_TRINARY, ECHELON_TRINARY),
                    request("Trinary [Battle]", FormationLevel.BINARY_OR_TRINARY, ECHELON_TRINARY)),
              "Alpha");

        // Leaving the first bare and suffixing the second ("... (2)") would read as a defect rather
        // than a naming scheme, so the whole group is designated with the player's alphabet.
        assertEquals(List.of("Able Trinary [Battle]", "Baker Trinary [Battle]"), names(trinaries));
    }

    @Test
    void anEchelonWithNoDeclaredRuleKeepsTheRulesetName() {
        FormationNamer namer = namer(ForceNamingMethod.CCB_1943, new HashMap<>());
        List<NamedFormation> named = namer.nameSiblings(
              List.of(request("Assault Echelon", null, 99)), null);
        assertEquals(List.of("Assault Echelon"), names(named));
        assertNull(named.get(0).designator(),
              "an unnamed tier contributes no designator for children to qualify against");
    }

    @Test
    void namesRepeatUnderDifferentParentsButNotAtTheTopOfTheCommand() {
        // Inside the tree a repeated name is the scheme working as intended: position identifies the
        // formation, so every battalion has an "Alpha Company".
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules(),
              "Alpha Company");
        assertEquals(List.of("Alpha Company"),
              names(namer.nameSiblings(
                    List.of(request("A Company", FormationLevel.COMPANY, ECHELON_COMPANY)), "Alpha")),
              "an existing company name elsewhere in the campaign must not push this one down the"
              + " alphabet");

        // At the top of the command there is no parent to disambiguate by, so an existing name is
        // stepped over.
        FormationNamer topLevel = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules(),
              "Alpha Mek Regiment");
        assertEquals(List.of("Beta Mek Regiment"),
              names(topLevel.nameSiblings(
                    List.of(request("Mek Regiment", FormationLevel.REGIMENT, ECHELON_REGIMENT)), null)));
    }

    @Test
    void eachArmRestartsTheSequenceRatherThanContinuingTheOneBeforeIt() {
        // A regiment holds its Mek battalions alongside attached armor, aerospace and infantry
        // contingents. Each arm is counted separately, so the infantry battalion is "Alpha", not the
        // "Delta" it would be if it continued the Mek battalions' sequence.
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules());
        List<NamedFormation> named = namer.nameSiblings(List.of(
              request("Battalion", FormationLevel.BATTALION, ECHELON_BATTALION, UnitType.MEK),
              request("Battalion", FormationLevel.BATTALION, ECHELON_BATTALION, UnitType.MEK),
              request("Battalion", FormationLevel.BATTALION, ECHELON_BATTALION, UnitType.MEK),
              request("Aerospace Squadron", FormationLevel.COMPANY, ECHELON_COMPANY,
                    UnitType.AEROSPACE_FIGHTER),
              request("Armor Company", FormationLevel.COMPANY, ECHELON_COMPANY, UnitType.TANK),
              request("Infantry Battalion", FormationLevel.BATTALION, ECHELON_BATTALION,
                    UnitType.BATTLE_ARMOR)),
              "Alpha");

        assertEquals(List.of("Alpha Battalion", "Beta Battalion", "Gamma Battalion",
                    "Alpha Aerospace Squadron", "Alpha Armor Company", "Alpha Infantry Battalion"),
              names(named),
              "each unit type counts from the start of the alphabet, and tree order is preserved");
    }

    @Test
    void sameUnitTypeAtTheSameEchelonStillSharesOneSequence() {
        FormationNamer namer = namer(ForceNamingMethod.GREEK_ALPHABET, innerSphereRules());
        List<NamedFormation> named = namer.nameSiblings(List.of(
              request("Armor Company", FormationLevel.COMPANY, ECHELON_COMPANY, UnitType.TANK),
              request("Armor Company", FormationLevel.COMPANY, ECHELON_COMPANY, UnitType.TANK)),
              "Alpha");
        assertEquals(List.of("Alpha Armor Company", "Beta Armor Company"), names(named));
    }

    @Test
    void namingAnEmptyGroupReturnsNothingRatherThanFailing() {
        assertTrue(namer(ForceNamingMethod.CCB_1943, innerSphereRules())
                         .nameSiblings(List.of(), null).isEmpty());
    }

    @Test
    void engineDesignatorTokensAreStrippedBeforeTheFactionDesignatorIsApplied() {
        FormationNamer namer = namer(ForceNamingMethod.CCB_1943, innerSphereRules());
        // "1/A Company" already carries a designator from the engine; it must not survive alongside the
        // one this namer assigns.
        assertEquals(List.of("Able Company"),
              names(namer.nameSiblings(
                    List.of(request("1/A Company", FormationLevel.COMPANY, ECHELON_COMPANY)), null)));
    }

    // endregion
}
