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

/**
 * The per-row "Remove"/"Edit" buttons in {@link ScenarioTemplateEditorDialog}'s force list carry an
 * {@link javax.swing.AbstractButton#setActionCommand action command} of the form {@code PREFIX + forceId}. This helper
 * owns that scheme in one place so building, matching and parsing the commands stay consistent.
 *
 * <p>Matching uses a leading-prefix test rather than a substring test: the force ID is arbitrary user text and may
 * itself contain one of the prefixes, which under a {@code contains} check could route an "Edit" click to the delete
 * handler.
 */
public final class ForceListCommand {

    private static final String REMOVE_PREFIX = "REMOVE_FORCE_";
    private static final String EDIT_PREFIX = "EDIT_FORCE_";

    private ForceListCommand() {
    }

    /**
     * @param forceId the ID of the force the button acts on
     *
     * @return the action command for that force's "Remove" button
     */
    public static String removeCommand(String forceId) {
        return REMOVE_PREFIX + forceId;
    }

    /**
     * @param forceId the ID of the force the button acts on
     *
     * @return the action command for that force's "Edit" button
     */
    public static String editCommand(String forceId) {
        return EDIT_PREFIX + forceId;
    }

    /**
     * @param command an action command
     *
     * @return whether {@code command} is a "Remove" command
     */
    public static boolean isRemove(String command) {
        return command.startsWith(REMOVE_PREFIX);
    }

    /**
     * @param command an action command
     *
     * @return whether {@code command} is an "Edit" command
     */
    public static boolean isEdit(String command) {
        return command.startsWith(EDIT_PREFIX);
    }

    /**
     * Extracts the force ID from a "Remove" command. Only call this once {@link #isRemove(String)} is true.
     *
     * @param command a "Remove" command
     *
     * @return the force ID it targets
     */
    public static String removeForceId(String command) {
        return command.substring(REMOVE_PREFIX.length());
    }

    /**
     * Extracts the force ID from an "Edit" command. Only call this once {@link #isEdit(String)} is true.
     *
     * @param command an "Edit" command
     *
     * @return the force ID it targets
     */
    public static String editForceId(String command) {
        return command.substring(EDIT_PREFIX.length());
    }
}
