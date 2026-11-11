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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import mekhq.MHQConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.icons.FormationPieceIcon;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.AddSupportUnitsToTOE;
import mekhq.campaign.universe.commandGeneration.SupportTOEFormationTypes;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Covers the icon a generated support formation is given in the TOE.
 *
 * <p>The interesting case is recovery. Every other support capability can be recognised from the equipment its
 * units carry, but a recovery vehicle need not carry any: the BattleMek Recovery Vehicle is an ordinary Tank with a
 * cargo bay, so counting lift hoists and salvage arms finds nothing on the one formation the maintenance icon is
 * meant for.</p>
 */
class FormationIconBuilderTest {
    private static final String MAINTENANCE_ICON = "Maintenance.png";
    private static final String SUPPLY_ICON = "Supply.png";

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @Test
    @DisplayName("Recovery Operations carries the maintenance icon even though its vehicles carry no recovery gear")
    void aRecoveryFormationCarriesTheMaintenanceIcon() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        List<Unit> vehicles = vehiclesInHangar(campaign, 4);
        for (Unit vehicle : vehicles) {
            assertFalse(vehicle.getEntity().hasWorkingMisc(MiscType.F_LIFT_HOIST),
                  "this test is only meaningful while the stand-in carries no lift hoist, as the real recovery "
                        + "vehicle does not");
        }
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, vehicles,
              SupportTOEFormationTypes.SALVAGE_FORMATION, 4, position -> "Lance " + position);

        Formation recoveryOperations = formationNamed(campaign,
              SupportTOEFormationTypes.SALVAGE_FORMATION.getLabel());

        assertEquals(MAINTENANCE_ICON, purposeIconFor(campaign, recoveryOperations),
              "the salvage formation is what the maintenance icon is for");
    }

    @Test
    @DisplayName("Each lance under Recovery Operations carries it too")
    void theLancesUnderItCarryItAsWell() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        // Eight vehicles, so two lances: a formation of one lance's worth is that lance and is not split.
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, vehiclesInHangar(campaign, 8),
              SupportTOEFormationTypes.SALVAGE_FORMATION, 4, position -> "Lance " + position);

        Formation lance = formationNamed(campaign, "Lance 1");

        assertEquals(MAINTENANCE_ICON, purposeIconFor(campaign, lance),
              "a lance of recovery vehicles is still recovery");
    }

    @Test
    @DisplayName("The convoy keeps the supply icon, so the new rule did not capture it")
    void theConvoyStillCarriesTheSupplyIcon() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, vehiclesInHangar(campaign, 4),
              SupportTOEFormationTypes.LOGISTICS_FORMATION, 4, position -> "Lance " + position);

        Formation logistics = formationNamed(campaign,
              SupportTOEFormationTypes.LOGISTICS_FORMATION.getLabel());

        assertEquals(SUPPLY_ICON, purposeIconFor(campaign, logistics),
              "cargo trucks are supply, not maintenance");
    }

    @Test
    @DisplayName("A support formation that grows gets the echelon symbol for its new size")
    void aGrownFormationGetsItsNewEchelonSymbol() {
        // Issue 10375: the Medical section was drawn while it held one MASH truck, so its icon kept the Team symbol
        // after ten more trucks made it a company.
        Campaign campaign = innerSphereCampaign();
        Formation medical = formationWithEchelonPiece(campaign, MHQConstants.LAYERED_FORCE_ICON_FORMATION_INNER_SPHERE_PATH,
              MHQConstants.LAYERED_FORCE_ICON_FORMATION_TEAM_FILENAME);
        medical.setOverrideFormationLevel(FormationLevel.COMPANY);

        assertTrue(FormationIconBuilder.refreshEchelonPiece(medical, faction(campaign)));
        assertEquals(MHQConstants.LAYERED_FORCE_ICON_FORMATION_COMPANY_FILENAME, echelonFilename(medical),
              "a company carries the company symbol");
        assertFalse(FormationIconBuilder.refreshEchelonPiece(medical, faction(campaign)), "and an up to date icon is left as is");
    }

    @Test
    @DisplayName("An icon the player designed is never overwritten")
    void aPlayersOwnIconIsLeftAlone() {
        Campaign campaign = innerSphereCampaign();
        Formation medical = formationWithEchelonPiece(campaign, "Custom/", "My Field Hospital.png");
        medical.setOverrideFormationLevel(FormationLevel.COMPANY);

        assertFalse(FormationIconBuilder.refreshEchelonPiece(medical, faction(campaign)));
        assertEquals("My Field Hospital.png", echelonFilename(medical));
    }

    private static Campaign innerSphereCampaign() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        assertFalse(campaign.getPlayerForce().getFaction().isClan()
                          || campaign.getPlayerForce().getFaction().isComStarOrWoB(),
              "these tests use the Inner Sphere symbols");
        return campaign;
    }

    private static Formation formationWithEchelonPiece(Campaign campaign, String folder, String filename) {
        Formation formation = new Formation("Medical");
        campaign.getPlayerForce().addFormation(formation,
              campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN), campaign);
        LayeredFormationIcon icon = new LayeredFormationIcon();
        icon.getPieces().put(LayeredFormationIconLayer.FORMATION,
              new ArrayList<>(List.of(new FormationPieceIcon(LayeredFormationIconLayer.FORMATION, folder, filename))));
        formation.setFormationIcon(icon);
        return formation;
    }

    private static String echelonFilename(Formation formation) {
        return ((LayeredFormationIcon) formation.getFormationIcon()).getPieces()
                     .get(LayeredFormationIconLayer.FORMATION).get(0).getFilename();
    }

    @Test
    @DisplayName("A granted formation left blank gets its icon; one with any piece of its own is left alone")
    void blankIconsAreDrawnAndOthersAreLeftAlone() {
        assumeFormationIcons();
        Campaign campaign = innerSphereCampaign();
        Formation blank = new Formation("Logistics");
        campaign.getPlayerForce().addFormation(blank,
              campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN), campaign);
        Formation custom = formationWithEchelonPiece(campaign, "Custom/", "My Convoy.png");
        // In play, filing a unit raises the event that sets every formation's level; an icon needs a level.
        Formation.populateFormationLevelsFromOrigin(campaign);

        decorateUndecorated(campaign, blank);
        decorateUndecorated(campaign, custom);

        assertTrue(((LayeredFormationIcon) blank.getFormationIcon()).getPieces()
                         .containsKey(LayeredFormationIconLayer.FORMATION), "the blank formation was drawn");
        assertEquals("My Convoy.png", echelonFilename(custom), "the player's icon is untouched");
    }

    @Test
    @DisplayName("A blank formation carrying only the operational status indicator is still drawn, and keeps it")
    void aBlankIconWithAStatusIndicatorIsStillDrawn() {
        assumeFormationIcons();
        Campaign campaign = innerSphereCampaign();
        Formation blank = new Formation("Lance");
        campaign.getPlayerForce().addFormation(blank,
              campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN), campaign);
        Formation.populateFormationLevelsFromOrigin(campaign);
        // The new day puts the indicator on every layered icon, blank ones included
        FormationPieceIcon status = new FormationPieceIcon(LayeredFormationIconLayer.SPECIAL_MODIFIER,
              MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_BORDER_PATH, OperationalStatus.FULLY_OPERATIONAL.getFilename());
        ((LayeredFormationIcon) blank.getFormationIcon()).getPieces()
              .put(LayeredFormationIconLayer.SPECIAL_MODIFIER, new ArrayList<>(List.of(status)));

        assertEquals(1, decorateUndecorated(campaign, blank), "the indicator does not make the icon decorated");

        Map<LayeredFormationIconLayer, List<FormationPieceIcon>> pieces =
              ((LayeredFormationIcon) blank.getFormationIcon()).getPieces();
        assertTrue(pieces.containsKey(LayeredFormationIconLayer.FORMATION), "the blank formation was drawn");
        assertEquals(List.of(status), pieces.get(LayeredFormationIconLayer.SPECIAL_MODIFIER),
              "and the indicator survives until the next day redraws it");
    }

    @Test
    @DisplayName("A formation whose units changed gets the silhouette for the units it holds now")
    void aChangedFormationGetsItsNewSilhouette() {
        assumeFormationIcons();
        Campaign campaign = innerSphereCampaign();
        Formation lance = formationWithEchelonPiece(campaign, MHQConstants.LAYERED_FORCE_ICON_FORMATION_INNER_SPHERE_PATH,
              MHQConstants.LAYERED_FORCE_ICON_FORMATION_LANCE_FILENAME);
        // Drawn while it was a medium 'Mek lance
        setTypePieces(lance, MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
              MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_LEFT_FILENAME, "Medium.png");
        // ...and now holds tanks
        for (Unit vehicle : vehiclesInHangar(campaign, 4)) {
            campaign.getPlayerForce().addUnitToFormation(vehicle, lance, campaign);
        }

        assertTrue(refreshTypePiece(campaign, lance));
        assertFalse(typeFilenames(lance).contains(MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_LEFT_FILENAME),
              "a lance of tanks no longer carries the 'Mek silhouette");
        assertFalse(refreshTypePiece(campaign, lance), "and an up to date icon is left as is");
    }

    @Test
    @DisplayName("A silhouette the player picked is never redrawn")
    void aPlayersOwnSilhouetteIsLeftAlone() {
        assumeFormationIcons();
        Campaign campaign = innerSphereCampaign();
        Formation lance = formationWithEchelonPiece(campaign, MHQConstants.LAYERED_FORCE_ICON_FORMATION_INNER_SPHERE_PATH,
              MHQConstants.LAYERED_FORCE_ICON_FORMATION_LANCE_FILENAME);
        setTypePieces(lance, "Custom/", "My Mascot.png");
        for (Unit vehicle : vehiclesInHangar(campaign, 4)) {
            campaign.getPlayerForce().addUnitToFormation(vehicle, lance, campaign);
        }

        assertFalse(refreshTypePiece(campaign, lance));
        assertEquals(List.of("My Mascot.png"), typeFilenames(lance));
    }

    @Test
    @DisplayName("Builder silhouettes on an icon with the player's own echelon shape are left alone")
    void aPlayersOwnEchelonProtectsTheSilhouette() {
        assumeFormationIcons();
        Campaign campaign = innerSphereCampaign();
        Formation lance = formationWithEchelonPiece(campaign, "Custom/", "My Lance.png");
        setTypePieces(lance, MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
              MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_LEFT_FILENAME, "Medium.png");
        for (Unit vehicle : vehiclesInHangar(campaign, 4)) {
            campaign.getPlayerForce().addUnitToFormation(vehicle, lance, campaign);
        }

        assertFalse(refreshTypePiece(campaign, lance), "the player built this icon, whatever pieces they used");
        assertEquals(List.of(MHQConstants.LAYERED_FORCE_ICON_BATTLEMEK_LEFT_FILENAME, "Medium.png"),
              typeFilenames(lance));
    }

    @Test
    @DisplayName("Refreshing the TOE draws blank formations and brings the builder's icons up to date")
    void refreshingTheTOEDrawsAndUpdates() {
        assumeFormationIcons();
        Campaign campaign = innerSphereCampaign();
        Formation blank = new Formation("Lance");
        campaign.getPlayerForce().addFormation(blank,
              campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN), campaign);
        Formation grown = formationWithEchelonPiece(campaign, MHQConstants.LAYERED_FORCE_ICON_FORMATION_INNER_SPHERE_PATH,
              MHQConstants.LAYERED_FORCE_ICON_FORMATION_TEAM_FILENAME);
        Formation custom = formationWithEchelonPiece(campaign, "Custom/", "My Company.png");
        Formation.populateFormationLevelsFromOrigin(campaign);
        grown.setOverrideFormationLevel(FormationLevel.COMPANY);
        custom.setOverrideFormationLevel(FormationLevel.COMPANY);

        FormationIconBuilder.refreshIcons(campaign.getPlayerForce().getFormations(), faction(campaign),
              campaign::getUnit, campaign.getPlayerForce().getSupportCommandFormation());

        assertTrue(((LayeredFormationIcon) blank.getFormationIcon()).getPieces()
                         .containsKey(LayeredFormationIconLayer.FORMATION), "the blank formation was drawn");
        assertEquals(MHQConstants.LAYERED_FORCE_ICON_FORMATION_COMPANY_FILENAME, echelonFilename(grown),
              "the grown formation carries its new echelon");
        assertEquals("My Company.png", echelonFilename(custom), "the player's icon is untouched");
    }

    private static void assumeFormationIcons() {
        Assumptions.assumeTrue(MHQStaticDirectoryManager.getFormationIcons() != null,
              "the formation icon images are not available");
    }

    private static Faction faction(Campaign campaign) {
        return campaign.getPlayerForce().getFaction();
    }

    private static String purposeIconFor(Campaign campaign, Formation formation) {
        return FormationIconBuilder.purposeIconFor(formation, campaign::getUnit,
              campaign.getPlayerForce().getSupportCommandFormation());
    }

    private static int decorateUndecorated(Campaign campaign, Formation formation) {
        return FormationIconBuilder.decorateUndecorated(formation, faction(campaign), campaign::getUnit,
              campaign.getPlayerForce().getSupportCommandFormation());
    }

    private static boolean refreshTypePiece(Campaign campaign, Formation formation) {
        return FormationIconBuilder.refreshTypePiece(formation, faction(campaign), campaign::getUnit,
              campaign.getPlayerForce().getSupportCommandFormation());
    }

    private static void setTypePieces(Formation formation, String folder, String... filenames) {
        List<FormationPieceIcon> pieces = new ArrayList<>();
        for (String filename : filenames) {
            pieces.add(new FormationPieceIcon(LayeredFormationIconLayer.TYPE, folder, filename));
        }
        ((LayeredFormationIcon) formation.getFormationIcon()).getPieces().put(LayeredFormationIconLayer.TYPE, pieces);
    }

    private static List<String> typeFilenames(Formation formation) {
        return ((LayeredFormationIcon) formation.getFormationIcon()).getPieces()
                     .get(LayeredFormationIconLayer.TYPE).stream().map(FormationPieceIcon::getFilename).toList();
    }

    /**
     * Adds {@code count} vehicles and returns every unit in the hangar.
     *
     * <p>Built from the hangar rather than from the return of
     * {@link UnitTestUtilities#addAndGetUnit(Campaign, megamek.common.units.Entity)}, which hands back the first unit
     * in the hangar rather than the one it just added.</p>
     */
    private static List<Unit> vehiclesInHangar(Campaign campaign, int count) {
        for (int index = 0; index < count; index++) {
            UnitTestUtilities.addAndGetUnit(campaign, UnitTestUtilities.getHeavyTrackedApcStandard());
        }
        return new ArrayList<>(campaign.getPlayerForce().getHangar().getUnits());
    }

    /** The formation with this name, failing the test when the TOE does not hold one. */
    private static Formation formationNamed(Campaign campaign, String name) {
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            if (formation.getName().equalsIgnoreCase(name)) {
                return formation;
            }
        }
        throw new AssertionError("no formation named '" + name + "' in the TOE");
    }
}
