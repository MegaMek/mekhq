/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.helper;

import megamek.client.AbstractClient;
import megamek.client.IClient;
import megamek.common.IGame;
import megamek.common.Player;

import java.util.Map;

/**
 * @author Luana Coppio
 */
public class AutoResolveClient implements IClient {

    private final IGame game;
    private final Player localPlayer;

    public AutoResolveClient(IGame game, Player player) {
        this.game = game;
        this.localPlayer = player;
    }

    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String getHost() {
        return "";
    }

    @Override
    public void die() {

    }

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public int getLocalPlayerNumber() {
        return localPlayer.getId();
    }

    @Override
    public Player getLocalPlayer() {
        return localPlayer;
    }

    @Override
    public boolean isMyTurn() {
        return false;
    }

    @Override
    public void setLocalPlayerNumber(int localPlayerNumber) {

    }

    @Override
    public Map<String, AbstractClient> getBots() {
        return Map.of();
    }

    @Override
    public void sendDone(boolean done) {

    }

    @Override
    public void sendChat(String message) {

    }
}
