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
 * minefields, etc, so we can identify getter and setter methods across these types. Currently, that applies to
 * Scenario and BotForce.
 */
public interface IPlayerSettings {

    // deployment information
    public int getStartingPos();
    public void setStartingPos(int i);
    public int getStartWidth();
    public void setStartWidth(int i);
    public int getStartOffset();
    public void setStartOffset(int i);
    public int getStartingAnyNWx();
    public void setStartingAnyNWx(int i);
    public int getStartingAnyNWy();
    public void setStartingAnyNWy(int i);
    public int getStartingAnySEx();
    public void setStartingAnySEx(int i);
    public int getStartingAnySEy();
    public void setStartingAnySEy(int i);

}
