/*
 * Copyright (C) 2026 The MekHQ Team. All Rights Reserved.
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
 * NOTICE: The MekHQ organization is a non-profit group of volunteers
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
package mekhq.campaign.universe.companyGeneration;

import static mekhq.campaign.force.Formation.FORMATION_ORIGIN;
import static mekhq.campaign.universe.companyGeneration.SupportTOEFormationTypes.HQ_FORMATION;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.unit.Unit;
import org.jspecify.annotations.NonNull;

/**
 * Utility class responsible for inserting support units into a campaign's TOE.
 *
 * <p>Support units are grouped into a new sub-{@link Formation} whose label and type are derived from a
 * {@link SupportTOEFormationTypes} descriptor. The sub-formation is attached as a child of the campaign's HQ formation,
 * which is located or created on demand.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public class AddSupportUnitsToTOE {

    /**
     * Adds the given support {@link Unit} list to the campaign's TOE under the HQ formation.
     *
     * <p>A new sub-{@link Formation} is created using the label and {@link FormationType} supplied by
     * {@code  supportTOEFormationTypes}, and every unit in {@code units} is registered within it. The sub-formation is
     * then attached to the campaign's HQ formation, which is retrieved or created via
     * {@link #getHqFormation(Campaign)}.</p>
     *
     * @param campaign                 the active {@link Campaign} that owns the TOE
     * @param units                    the list of support {@link Unit} objects to add; must not be {@code null}, but
     *                                 may be empty
     * @param supportTOEFormationTypes the {@link SupportTOEFormationTypes} descriptor that provides the formation label
     *                                 and type for the new sub-formation
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static void addSupportUnitsToTOE(Campaign campaign, List<Unit> units,
          SupportTOEFormationTypes supportTOEFormationTypes) {
        if (units.isEmpty()) {
            return;
        }

        Formation hqFormation = getHqFormation(campaign);

        FormationType type = supportTOEFormationTypes.getType();
        String label = supportTOEFormationTypes.getLabel();
        createSubFormation(campaign, label, type, units, hqFormation);
    }

    /**
     * Creates a new {@link Formation} populated with the given units.
     *
     * <p>The formation is assigned the provided {@code label} as its display name and configured with the given
     * {@link FormationType}. Each unit's ID is then registered with the formation. The formation type is largely
     * presentational for support formations and does not affect gameplay calculations.</p>
     *
     * @param campaign    the campaign context, used to register the units with the formation
     * @param label       the display name for the new formation
     * @param type        the {@link FormationType} to assign to the formation
     * @param units       the list of {@link Unit} objects whose IDs will be added to the formation; must not be
     *                    {@code null}, but may be empty
     * @param hqFormation the HQ {@link Formation} to attach the new formation to; must not be {@code null}
     *
     * @return a fully populated, non-{@code null} {@link Formation} ready to be registered with the campaign
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static void createSubFormation(Campaign campaign, String label, FormationType type,
          List<Unit> units, Formation hqFormation) {
        Formation subFormation = new Formation(label);
        subFormation.setFormationType(type, true); // Largely irrelevant

        int subFormationId = subFormation.getId();
        for (Unit unit : units) {
            campaign.addUnitToFormation(unit, subFormationId);
        }

        campaign.addFormation(subFormation, hqFormation);
    }

    /**
     * Retrieves the campaign's HQ {@link Formation}, creating it if one does not already exist.
     *
     * <p>All existing formations are searched for a name that matches {@link SupportTOEFormationTypes#HQ_FORMATION}'s
     * label (case-insensitive). If a match is found, it is returned immediately. Otherwise, a new HQ formation is
     * created, attached to the campaign's origin formation ({@link Formation#FORMATION_ORIGIN}), and returned.</p>
     *
     * @param campaign the active {@link Campaign} whose formation list is searched and potentially modified
     *
     * @return the existing or newly created HQ {@link Formation}; never {@code null}
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static @NonNull Formation getHqFormation(Campaign campaign) {
        final Formation ORIGIN_FORMATION = campaign.getFormation(FORMATION_ORIGIN);

        // I would prefer to not use string comparison here, but we don't have a more reliable option
        final String HQ_FORMATION_NAME = HQ_FORMATION.getLabel();

        List<Formation> formations = campaign.getAllFormations();
        for (Formation formation : formations) {
            if (formation.getName().equalsIgnoreCase(HQ_FORMATION.getLabel())) {
                return formation;
            }
        }

        Formation newFormation = new Formation(HQ_FORMATION_NAME);
        campaign.addFormation(newFormation, ORIGIN_FORMATION);

        return newFormation;
    }
}
