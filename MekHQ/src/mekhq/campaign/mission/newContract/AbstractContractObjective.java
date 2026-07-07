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
package mekhq.campaign.mission.newContract;

import java.util.UUID;

import mekhq.campaign.mission.enums.AtBContractType;

public abstract class AbstractContractObjective {
    private UUID uuid;
    private transient AbstractContractManager parentContractManager;
    private AtBContractType objectiveType;
    private String enemyFactionCode;
    private int lengthInMonths;

    public UUID getId() {
        return uuid;
    }

    public void setId(UUID uuid) {
        this.uuid = uuid;
    }

    public AbstractContractObjective() {
    }

    public AbstractContractManager getParentContractManager() {
        return parentContractManager;
    }

    public void setParentContractManager(AbstractContractManager parentContractManager) {
        this.parentContractManager = parentContractManager;
    }

    public AtBContractType getObjectiveType() {
        return objectiveType;
    }

    public void setObjectiveType(AtBContractType objectiveType) {
        this.objectiveType = objectiveType;
    }

    public String getEnemyFactionCode() {
        return enemyFactionCode;
    }

    public void setEnemyFactionCode(String enemyFactionCode) {
        this.enemyFactionCode = enemyFactionCode;
    }

    public int getLengthInMonths() {
        return lengthInMonths;
    }

    public void setLengthInMonths(int lengthInMonths) {
        this.lengthInMonths = lengthInMonths;
    }
}
