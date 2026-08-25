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
package mekhq.gui.scenarioTemplateEditor;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.table.AbstractTableModel;

import megamek.common.units.EntityWeightClass;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceGenerationMethod;

/**
 * Table model backing the "Participating Forces" list in {@link ScenarioTemplateEditorDialog}. Replaces the former
 * hand-built GridBag pseudo-table; the read-only cell values are directly testable.
 *
 * <p>Some columns are intentionally blank for certain force kinds, matching the original rendering: player forces do
 * not show the multiplier, unit type or max weight; enemy-bot and planet-owner forces do not show the BV / unit-count
 * contribution flags.
 */
public class ForceTableModel extends AbstractTableModel {

    static final int COL_ORDER = 0;
    static final int COL_FORCE_ID = 1;
    static final int COL_ALIGNMENT = 2;
    static final int COL_GENERATION = 3;
    static final int COL_MULTIPLIER = 4;
    static final int COL_DEPLOYMENT = 5;
    static final int COL_DESTINATION = 6;
    static final int COL_RETREAT = 7;
    static final int COL_UNIT_TYPE = 8;
    static final int COL_MAX_WEIGHT = 9;
    static final int COL_ARRIVAL = 10;
    static final int COL_REINFORCE = 11;
    static final int COL_CONTRIBUTES_BV = 12;
    static final int COL_CONTRIBUTES_UNIT_COUNT = 13;
    static final int COL_CONTRIBUTES_MAP_SIZE = 14;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private static final String[] COLUMN_NAMES = {
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.order"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.forceId"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.alignment"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.generation"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.multiplier"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.deployment"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.destination"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.retreat"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.unitType"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.maxWeight"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.arrival"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.reinforce"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.contributesBV"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.contributesUnitCount"),
          getTextAt(RESOURCE_BUNDLE, "ForceTableModel.column.contributesMapSize") };

    private final List<ScenarioForceTemplate> forces = new ArrayList<>();

    /**
     * Replaces the model contents with the given forces, sorted in the template's natural order.
     */
    public void setForces(Collection<ScenarioForceTemplate> newForces) {
        forces.clear();
        forces.addAll(newForces);
        Collections.sort(forces);
        fireTableDataChanged();
    }

    /**
     * @return the force at the given row
     */
    public ScenarioForceTemplate getForceAt(int rowIndex) {
        return forces.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return forces.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ScenarioForceTemplate forceTemplate = forces.get(rowIndex);
        boolean isPlayerForce = forceTemplate.isPlayerForce();
        boolean hidesContributionFlags = forceTemplate.isEnemyBotForce()
                                               ||
                                               (forceTemplate.getForceAlignment() ==
                                                      ForceAlignment.PlanetOwner.ordinal());

        return switch (columnIndex) {
            case COL_ORDER -> Integer.toString(forceTemplate.getGenerationOrder());
            case COL_FORCE_ID -> forceTemplate.getForceName();
            case COL_ALIGNMENT -> ScenarioForceTemplate.FORCE_ALIGNMENTS[forceTemplate.getForceAlignment()];
            case COL_GENERATION -> ScenarioForceTemplate.FORCE_GENERATION_METHODS[forceTemplate.getGenerationMethod()];
            case COL_MULTIPLIER -> multiplierText(forceTemplate, isPlayerForce);
            case COL_DEPLOYMENT -> deploymentText(forceTemplate);
            case COL_DESTINATION -> ScenarioForceTemplate.BOT_DESTINATION_ZONES[forceTemplate.getDestinationZone()];
            case COL_RETREAT -> Integer.toString(forceTemplate.getRetreatThreshold());
            case COL_UNIT_TYPE -> isPlayerForce ? "" : forceTemplate.getAllowedUnitTypeName();
            case COL_MAX_WEIGHT ->
                  isPlayerForce ? "" : EntityWeightClass.getClassName(forceTemplate.getMaxWeightClass());
            case COL_ARRIVAL -> arrivalText(forceTemplate);
            case COL_REINFORCE -> yesNo(forceTemplate.getCanReinforceLinked());
            case COL_CONTRIBUTES_BV -> hidesContributionFlags ? "" : yesNo(forceTemplate.getContributesToBV());
            case COL_CONTRIBUTES_UNIT_COUNT ->
                  hidesContributionFlags ? "" : yesNo(forceTemplate.getContributesToUnitCount());
            case COL_CONTRIBUTES_MAP_SIZE -> yesNo(forceTemplate.getContributesToMapSize());
            default -> "";
        };
    }

    private static String multiplierText(ScenarioForceTemplate forceTemplate, boolean isPlayerForce) {
        if (isPlayerForce) {
            return "";
        }
        if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.FixedUnitCount.ordinal()) {
            return forceTemplate.getFixedUnitCount() >= 0 ?
                         Integer.toString(forceTemplate.getFixedUnitCount()) :
                         getTextAt(RESOURCE_BUNDLE, "ForceTableModel.lance");
        }
        return Double.toString(forceTemplate.getForceMultiplier());
    }

    private static String deploymentText(ScenarioForceTemplate forceTemplate) {
        if (!forceTemplate.getDeploymentZones().isEmpty()) {
            return forceTemplate.getDeploymentZones().stream()
                         .map(zone -> ScenarioForceTemplate.DEPLOYMENT_ZONES[zone])
                         .collect(Collectors.joining(", "));
        }
        return ScenarioForceTemplate.FORCE_DEPLOYMENT_SYNC_TYPES[forceTemplate.getSyncDeploymentType().ordinal()]
                     + " as " + forceTemplate.getSyncedForceName();
    }

    private static String arrivalText(ScenarioForceTemplate forceTemplate) {
        return forceTemplate.getArrivalTurn() < 0
                     ? ScenarioForceTemplate.SPECIAL_ARRIVAL_TURNS.get(forceTemplate.getArrivalTurn())
                     : Integer.toString(forceTemplate.getArrivalTurn());
    }

    private static String yesNo(boolean value) {
        return value ?
                     getTextAt(RESOURCE_BUNDLE, "ForceTableModel.yes") :
                     getTextAt(RESOURCE_BUNDLE, "ForceTableModel.no");
    }
}
