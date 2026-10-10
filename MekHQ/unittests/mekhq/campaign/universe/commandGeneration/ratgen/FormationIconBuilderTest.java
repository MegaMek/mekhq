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

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.icons.FormationPieceIcon;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
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

        assertEquals(MAINTENANCE_ICON, FormationIconBuilder.purposeIconFor(recoveryOperations, campaign),
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

        assertEquals(MAINTENANCE_ICON, FormationIconBuilder.purposeIconFor(lance, campaign),
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

        assertEquals(SUPPLY_ICON, FormationIconBuilder.purposeIconFor(logistics, campaign),
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

        assertTrue(FormationIconBuilder.refreshEchelonPiece(medical, campaign));
        assertEquals(MHQConstants.LAYERED_FORCE_ICON_FORMATION_COMPANY_FILENAME, echelonFilename(medical),
              "a company carries the company symbol");
        assertFalse(FormationIconBuilder.refreshEchelonPiece(medical, campaign), "and an up to date icon is left as is");
    }

    @Test
    @DisplayName("An icon the player designed is never overwritten")
    void aPlayersOwnIconIsLeftAlone() {
        Campaign campaign = innerSphereCampaign();
        Formation medical = formationWithEchelonPiece(campaign, "Custom/", "My Field Hospital.png");
        medical.setOverrideFormationLevel(FormationLevel.COMPANY);

        assertFalse(FormationIconBuilder.refreshEchelonPiece(medical, campaign));
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
        Assumptions.assumeTrue(mekhq.MHQStaticDirectoryManager.getFormationIcons() != null,
              "the formation icon images are not available");
        Campaign campaign = innerSphereCampaign();
        Formation blank = new Formation("Logistics");
        campaign.getPlayerForce().addFormation(blank,
              campaign.getPlayerForce().getFormation(Formation.FORMATION_ORIGIN), campaign);
        Formation custom = formationWithEchelonPiece(campaign, "Custom/", "My Convoy.png");
        // In play, filing a unit raises the event that sets every formation's level; an icon needs a level.
        Formation.populateFormationLevelsFromOrigin(campaign);

        FormationIconBuilder.decorateUndecorated(blank, campaign);
        FormationIconBuilder.decorateUndecorated(custom, campaign);

        assertTrue(((LayeredFormationIcon) blank.getFormationIcon()).getPieces()
                         .containsKey(LayeredFormationIconLayer.FORMATION), "the blank formation was drawn");
        assertEquals("My Convoy.png", echelonFilename(custom), "the player's icon is untouched");
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
