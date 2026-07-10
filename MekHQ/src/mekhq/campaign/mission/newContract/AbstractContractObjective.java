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

/**
 * Base class for a single objective within a generated contract: its identity, parent contract, objective type, enemy
 * faction, and length. Subclasses supply the concrete objective flavor.
 */
public abstract class AbstractContractObjective {
    private UUID uuid;
    private transient AbstractContractManager parentContractManager;
    private AtBContractType objectiveType;
    private String enemyFactionCode;
    private int lengthInMonths;

    /**
     * @return this objective's unique id
     */
    public UUID getId() {
        return uuid;
    }

    /**
     * @param uuid this objective's unique id
     */
    public void setId(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Creates an empty contract objective.
     */
    public AbstractContractObjective() {
    }

    /**
     * @return the contract manager this objective belongs to
     */
    public AbstractContractManager getParentContractManager() {
        return parentContractManager;
    }

    /**
     * @param parentContractManager the contract manager this objective belongs to
     */
    public void setParentContractManager(AbstractContractManager parentContractManager) {
        this.parentContractManager = parentContractManager;
    }

    /**
     * @return the objective's contract type
     */
    public AtBContractType getObjectiveType() {
        return objectiveType;
    }

    /**
     * @param objectiveType the objective's contract type
     */
    public void setObjectiveType(AtBContractType objectiveType) {
        this.objectiveType = objectiveType;
    }

    /**
     * @return the enemy faction's short code for this objective
     */
    public String getEnemyFactionCode() {
        return enemyFactionCode;
    }

    /**
     * @param enemyFactionCode the enemy faction's short code for this objective
     */
    public void setEnemyFactionCode(String enemyFactionCode) {
        this.enemyFactionCode = enemyFactionCode;
    }

    /**
     * @return the objective's length in months
     */
    public int getLengthInMonths() {
        return lengthInMonths;
    }

    /**
     * @param lengthInMonths the objective's length in months
     */
    public void setLengthInMonths(int lengthInMonths) {
        this.lengthInMonths = lengthInMonths;
    }
}
