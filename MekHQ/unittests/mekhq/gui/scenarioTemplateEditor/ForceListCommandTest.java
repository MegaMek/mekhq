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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ForceListCommand}, the force-list action-command scheme.
 */
class ForceListCommandTest {

    @Test
    void removeCommandRoundTripsTheForceId() {
        String command = ForceListCommand.removeCommand("Alpha");
        assertTrue(ForceListCommand.isRemove(command));
        assertFalse(ForceListCommand.isEdit(command));
        assertEquals("Alpha", ForceListCommand.removeForceId(command));
    }

    @Test
    void editCommandRoundTripsTheForceId() {
        String command = ForceListCommand.editCommand("Alpha");
        assertTrue(ForceListCommand.isEdit(command));
        assertFalse(ForceListCommand.isRemove(command));
        assertEquals("Alpha", ForceListCommand.editForceId(command));
    }

    @Test
    void forceIdEmbeddingTheOtherPrefixIsNotMisrouted() {
        // A force whose ID contains the remove prefix must still route to edit, not delete. Under a substring match
        // this "Edit" command would have matched the remove branch first.
        String forceId = "REMOVE_FORCE_decoy";
        String command = ForceListCommand.editCommand(forceId);

        assertTrue(ForceListCommand.isEdit(command));
        assertFalse(ForceListCommand.isRemove(command), "Edit command must not be treated as a remove command");
        assertEquals(forceId, ForceListCommand.editForceId(command));
    }
}
