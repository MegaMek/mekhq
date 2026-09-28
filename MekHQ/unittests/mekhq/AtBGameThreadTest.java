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
package mekhq;

import static mekhq.campaign.enums.CampaignTransportType.TOW_TRANSPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.common.Player;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.PotentialTransportsMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests for the tow-train walk that turns a scenario's transport map into the ordered trailer list
 * sent to the server as one build-train request, and for the hand-over of the player's units to the
 * auto-resolve bot.
 *
 * @see AtBGameThread#orderedTrainTrailerIds
 * @see AtBGameThread#handOverPlayerUnitsToBot
 */
public class AtBGameThreadTest {

    private static final int PLAYER_ID = 0;
    private static final int BOT_ID = 2;
    private static final int ENEMY_ID = 1;

    /**
     * A campaign unit with the given entity id, registered with the campaign so the walk can find it.
     */
    private Unit registerUnit(Campaign campaign, int entityId) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(entityId);
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(UUID.randomUUID());
        when(unit.getEntity()).thenReturn(entity);
        when(campaign.getUnit(unit.getId())).thenReturn(unit);
        return unit;
    }

    /**
     * Registers the units as one tow train, tractor first, in a scenario transport map.
     */
    private PotentialTransportsMap trainTransportsMap(List<Unit> trainFrontToBack) {
        PotentialTransportsMap potentialTransports = new PotentialTransportsMap(CampaignTransportType.values());
        for (Unit member : trainFrontToBack) {
            potentialTransports.putNewTransport(TOW_TRANSPORT, member.getId());
        }
        for (int index = 0; index < (trainFrontToBack.size() - 1); index++) {
            potentialTransports.addTransportedUnit(TOW_TRANSPORT,
                  trainFrontToBack.get(index).getId(), trainFrontToBack.get(index + 1).getId());
        }
        return potentialTransports;
    }

    @Test
    public void ordersTheWholeTrainBehindTheTractor() {
        Campaign campaign = mockCampaign();
        Unit tractor = registerUnit(campaign, 1);
        Unit firstTrailer = registerUnit(campaign, 2);
        Unit secondTrailer = registerUnit(campaign, 3);
        PotentialTransportsMap potentialTransports = trainTransportsMap(
              List.of(tractor, firstTrailer, secondTrailer));

        List<Integer> ordered = AtBGameThread.orderedTrainTrailerIds(campaign, potentialTransports, tractor.getId());

        // Trailer entity ids only, front to back - the tractor is not part of its own load
        assertEquals(List.of(2, 3), ordered);
    }

    @Test
    public void stopsAtAGapInTheTrain() {
        Campaign campaign = mockCampaign();
        Unit tractor = registerUnit(campaign, 1);
        Unit firstTrailer = registerUnit(campaign, 2);
        Unit missingTrailer = registerUnit(campaign, 3);
        PotentialTransportsMap potentialTransports = trainTransportsMap(
              List.of(tractor, firstTrailer, missingTrailer));
        // The last trailer is not in the campaign - a save that lost it, or a unit left behind
        when(campaign.getUnit(missingTrailer.getId())).thenReturn(null);

        List<Integer> ordered = AtBGameThread.orderedTrainTrailerIds(campaign, potentialTransports, tractor.getId());

        assertEquals(List.of(2), ordered);
    }

    @Test
    public void doesNotHangOnALoopedHitch() {
        Campaign campaign = mockCampaign();
        Unit tractor = registerUnit(campaign, 1);
        Unit firstTrailer = registerUnit(campaign, 2);
        PotentialTransportsMap potentialTransports = trainTransportsMap(List.of(tractor, firstTrailer));
        // A hitch pointing back at the tractor - the loop a corrupt save could carry
        potentialTransports.addTransportedUnit(TOW_TRANSPORT, firstTrailer.getId(), tractor.getId());

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            List<Integer> ordered = AtBGameThread.orderedTrainTrailerIds(campaign, potentialTransports,
                  tractor.getId());
            assertEquals(List.of(2), ordered);
        });
    }

    @Test
    public void returnsNothingForAFreeTractor() {
        Campaign campaign = mockCampaign();
        Unit tractor = registerUnit(campaign, 1);
        PotentialTransportsMap potentialTransports = trainTransportsMap(List.of(tractor));

        assertEquals(List.of(),
              AtBGameThread.orderedTrainTrailerIds(campaign, potentialTransports, tractor.getId()));
    }

    /**
     * The bot client, connected as a bot player with the given id.
     */
    private BotClient botClient(int botId) {
        Player botPlayer = new Player(botId, "Rust & Ruin@AI");
        BotClient botClient = mock(BotClient.class);
        when(botClient.getLocalPlayer()).thenReturn(botPlayer);
        return botClient;
    }

    /**
     * A unit in the game as the player's client sees it, owned by the given player.
     */
    private Entity unitOwnedBy(int ownerId) {
        Entity entity = mock(Entity.class);
        when(entity.getOwnerId()).thenReturn(ownerId);
        return entity;
    }

    /**
     * The player's own client, connected as the given player and seeing the given units in the game.
     */
    private Client playerClient(Player player, Entity... unitsInGame) {
        Client client = mock(Client.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getEntitiesVector()).thenReturn(new Vector<>(List.of(unitsInGame)));
        return client;
    }

    /**
     * The server refuses a bot that asks to take a human's units, so the request must leave on the player's own
     * connection or the units never reach the bot and the auto-resolve never starts (issue #8989).
     */
    @Test
    public void handsThePlayersUnitsToTheBotOverThePlayersOwnConnection() {
        Player player = new Player(PLAYER_ID, "Rust & Ruin");
        Entity playerUnit = unitOwnedBy(PLAYER_ID);
        Entity secondPlayerUnit = unitOwnedBy(PLAYER_ID);
        Client client = playerClient(player, playerUnit, secondPlayerUnit);
        BotClient botClient = botClient(BOT_ID);

        AtBGameThread.handOverPlayerUnitsToBot(client, botClient);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Entity>> handedOver = ArgumentCaptor.forClass(List.class);
        verify(client).sendChangeOwner(handedOver.capture(), eq(BOT_ID));
        assertEquals(List.of(playerUnit, secondPlayerUnit), handedOver.getValue());
        verify(botClient, never()).sendChangeOwner(anyCollection(), anyInt());
    }

    @Test
    public void handsOverOnlyTheUnitsThePlayerOwns() {
        Player player = new Player(PLAYER_ID, "Rust & Ruin");
        Entity playerUnit = unitOwnedBy(PLAYER_ID);
        Entity enemyUnit = unitOwnedBy(ENEMY_ID);
        Client client = playerClient(player, enemyUnit, playerUnit);
        BotClient botClient = botClient(BOT_ID);

        AtBGameThread.handOverPlayerUnitsToBot(client, botClient);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Entity>> handedOver = ArgumentCaptor.forClass(List.class);
        verify(client).sendChangeOwner(handedOver.capture(), eq(BOT_ID));
        assertEquals(List.of(playerUnit), handedOver.getValue());
    }

    @Test
    public void sendsNothingWhenThePlayerHasNoUnits() {
        Player player = new Player(PLAYER_ID, "Rust & Ruin");
        Client client = playerClient(player, unitOwnedBy(ENEMY_ID));
        BotClient botClient = botClient(BOT_ID);

        AtBGameThread.handOverPlayerUnitsToBot(client, botClient);

        verify(client, never()).sendChangeOwner(anyCollection(), anyInt());
    }
}
