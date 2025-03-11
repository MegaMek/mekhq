/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
