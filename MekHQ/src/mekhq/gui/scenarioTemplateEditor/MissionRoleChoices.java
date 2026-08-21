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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import megamek.client.ratgenerator.MissionRole;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;

/**
 * Helpers for the scenario template editor's role-choices control. A force template stores its role choices as a list
 * of "role set" strings; each string is a comma-separated set of {@link MissionRole} names that apply together, and one
 * set is chosen at random when the force is generated (see
 * {@link ScenarioForceTemplate#getRequiredRoles()}). This class owns the parse/format and the
 * unit-type filtering so those pieces can be tested without the Swing UI.
 */
public final class MissionRoleChoices {

    private MissionRoleChoices() {
    }

    /**
     * The roles that may be offered for the given allowed unit type. A role is offered only if it both fits the unit
     * type and survives a {@link MissionRole#parseRole(String)} round-trip, so that anything selected can actually be
     * saved and reloaded. Special (negative) unit types - the AtB mixed types - are not restricted by unit type, since
     * the resulting force may contain several unit types.
     *
     * @param allowedUnitType the force's allowed unit type, or a negative special-mix value
     *
     * @return the selectable roles, in enum declaration order
     */
    public static List<MissionRole> selectableRoles(int allowedUnitType) {
        List<MissionRole> selectableRoles = new ArrayList<>();

        for (MissionRole role : MissionRole.values()) {
            if (allowedUnitType < 0 || role.fitsUnitType(allowedUnitType)) {
                if (roundTrips(role)) {
                    selectableRoles.add(role);
                }
            }
        }

        return selectableRoles;
    }

    private static boolean roundTrips(MissionRole role) {
        return role == MissionRole.parseRole(role.name());
    }

    /**
     * Builds the stored role-set string from a collection of roles: their names, comma-separated, in the given order.
     *
     * @param roles the roles in the set
     *
     * @return the stored representation, e.g. {@code "FIRE_SUPPORT,CARGO"}
     */
    public static String toEntry(Collection<MissionRole> roles) {
        return roles.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    /**
     * Parses a stored role-set string back into roles, mirroring {@code getRequiredRoles()}: split on commas, trim, and
     * drop blanks and unrecognized tokens.
     *
     * @param entry a stored role-set string
     *
     * @return the roles it names
     */
    public static List<MissionRole> fromEntry(String entry) {
        List<MissionRole> roles = new ArrayList<>();

        for (String token : entry.split(",")) {
            token = token.trim();

            if (token.isEmpty()) {
                continue;
            }

            MissionRole role = MissionRole.parseRole(token);
            if (role != null) {
                roles.add(role);
            }
        }

        return roles;
    }

    /**
     * Produces a human-readable label for a stored role-set string, e.g. {@code "FIRE_SUPPORT,CARGO"} becomes
     * {@code "fire support, cargo"}. Falls back to the raw entry if it names no recognizable roles.
     *
     * @param entry a stored role-set string
     *
     * @return a display label
     */
    public static String describe(String entry) {
        List<MissionRole> roles = fromEntry(entry);
        StringBuilder described = new StringBuilder();

        for (MissionRole role : roles) {
            if (!described.isEmpty()) {
                described.append(", ");
            }

            described.append(role.toString().replace('_', ' '));
        }

        return described.isEmpty() ? entry : described.toString();
    }
}
