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
package mekhq.campaign.mission;

import megamek.common.board.Board;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ScenarioTest {
    @Test
    public void testAutoAssignBotStartingPos() {
        Campaign mockCampaign = mock(Campaign.class);

        Scenario scenario = new Scenario("scenario");
        BotForce alliedBotForce = new BotForce("Allies", 1, Board.START_ANY, List.of());
        BotForce enemyBotForce = new BotForce("Enemies", 2, Board.START_ANY, List.of());

        scenario.addBotForce(alliedBotForce, mockCampaign);
        scenario.addBotForce(enemyBotForce, mockCampaign);

        scenario.setStartingPos(Board.START_N);

        scenario.autoAssignBotStartingPos();

        assertEquals(Board.START_N, alliedBotForce.getStartingPos());
        assertEquals(Board.START_S, enemyBotForce.getStartingPos());
    }

    @Test
    public void testAutoAssignBotStartingPosCenterStart() {
        Campaign mockCampaign = mock(Campaign.class);

        Scenario scenario = new Scenario("scenario");
        BotForce alliedBotForce = new BotForce("Allies", 1, Board.START_ANY, List.of());
        BotForce enemyBotForce = new BotForce("Enemies", 2, Board.START_ANY, List.of());

        scenario.addBotForce(alliedBotForce, mockCampaign);
        scenario.addBotForce(enemyBotForce, mockCampaign);

        scenario.setStartingPos(Board.START_CENTER);

        scenario.autoAssignBotStartingPos();

        assertEquals(Board.START_CENTER, alliedBotForce.getStartingPos());
        assertEquals(Board.START_EDGE, enemyBotForce.getStartingPos());
    }

    @Test
    public void testAutoAssignBotStartingPosAnyStart() {
        Campaign mockCampaign = mock(Campaign.class);

        Scenario scenario = new Scenario("scenario");
        BotForce alliedBotForce = new BotForce("Allies", 1, Board.START_S, List.of());
        BotForce enemyBotForce = new BotForce("Enemies", 2, Board.START_ANY, List.of());

        scenario.addBotForce(alliedBotForce, mockCampaign);
        scenario.addBotForce(enemyBotForce, mockCampaign);

        scenario.setStartingPos(Board.START_ANY);

        scenario.autoAssignBotStartingPos();

        // With a start of Any, no bot starts should have been modified
        assertEquals(Board.START_S, alliedBotForce.getStartingPos());
        assertEquals(Board.START_ANY, enemyBotForce.getStartingPos());
    }


    @Test
    public void testAutoAssignBotStartingPosDoesNotOverwrite() {
        Campaign mockCampaign = mock(Campaign.class);

        Scenario scenario = new Scenario("scenario");
        BotForce alliedBotForce = new BotForce("Allies", 1, Board.START_N, List.of());
        BotForce enemyBotForce = new BotForce("Enemies", 2, Board.START_S, List.of());

        scenario.addBotForce(alliedBotForce, mockCampaign);
        scenario.addBotForce(enemyBotForce, mockCampaign);

        scenario.setStartingPos(Board.START_E);

        scenario.autoAssignBotStartingPos();

        // The bot forces which already had defined starting positions should not have had those overwritten
        assertEquals(Board.START_N, alliedBotForce.getStartingPos());
        assertEquals(Board.START_S, enemyBotForce.getStartingPos());
    }
}
