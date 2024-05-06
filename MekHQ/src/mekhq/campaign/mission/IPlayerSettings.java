/*
 * IPlayerSettings.java
 *
 * Copyright (c) 2024 - MegaMek Team. All rights reserved.
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
package mekhq.campaign.mission;

/**
 * This interface allows to identify classes that track player information like deployment, initiative bonuses,
 * minefields, etc., so we can identify getter and setter methods across these types. Currently, that applies to
 * Scenario and BotForce.
 */
public interface IPlayerSettings {

    // deployment information
    int getStartingPos();
    void setStartingPos(int i);
    int getStartWidth();
    void setStartWidth(int i);
    int getStartOffset();
    void setStartOffset(int i);
    int getStartingAnyNWx();
    void setStartingAnyNWx(int i);
    int getStartingAnyNWy();
    void setStartingAnyNWy(int i);
    int getStartingAnySEx();
    void setStartingAnySEx(int i);
    int getStartingAnySEy();
    void setStartingAnySEy(int i);

}
