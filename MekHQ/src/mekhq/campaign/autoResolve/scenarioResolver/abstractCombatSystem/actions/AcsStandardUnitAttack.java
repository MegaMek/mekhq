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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions;

import megamek.common.IGame;
import megamek.common.alphaStrike.ASRange;
import megamek.logging.MMLogger;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

import java.util.Optional;

/**
 * @author Luana Coppio
 */
public class AcsStandardUnitAttack extends AbstractAcsAttackAction {
    private static final MMLogger logger = MMLogger.create(AcsStandardUnitAttack.class);

    private final int unitNumber;
    private final ASRange range;
    private final ManueverResult manueverResult;

    public enum ManueverResult {
        SUCCESS,
        FAILURE,
        DRAW
    }

    /**
     * Creates a standard attack of an SBF Unit on another formation.
     * The unit number identifies the SBF Unit making the attack, i.e. 1 for the
     * first of the formation's units,
     * 2 for the second etc.
     *
     * @param formationId The attacker's ID
     * @param unitNumber  The number of the attacking SBF Unit inside the formation
     * @param targetId    The target's ID
     */
    public AcsStandardUnitAttack(int formationId, int unitNumber, int targetId, ASRange range, ManueverResult manueverResult) {
        super(formationId, targetId);
        this.unitNumber = unitNumber;
        this.range = range;
        this.manueverResult = manueverResult;
    }

    /**
     * Returns the number of the SBF Unit making the attack, i.e. 1 for the first of
     * the formation's
     * units, 2 for the second.
     *
     * @return The unit number within the formation
     */
    public int getUnitNumber() {
        return unitNumber;
    }

    public ASRange getRange() {
        return range;
    }

    public ManueverResult getManueverResult() {
        return manueverResult;
    }

    @Override
    public AcsActionHandler getHandler(AcsGameManager gameManager) {
        return new AcsStandardUnitAttackHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(IGame game) {
        var autoResolveGame = (AutoResolveGame) game;
        String message;

        Optional<AcsFormation> possibleAttacker = autoResolveGame.getFormation(getEntityId());
        Optional<AcsFormation> possibleTarget = autoResolveGame.getFormation(getTargetId());
        if (getEntityId() == getTargetId()) {
            message = String.format("Formations cannot attack themselves! %h", this);
            logger.error(message);
            return false;
        } else if (possibleAttacker.isEmpty()) {
            message = String.format("Could not find attacking formation! %h", this);
            logger.error(message);
            return false;
        } else if (possibleTarget.isEmpty()) {
            message = String.format("Could not find target formation! %h", this);
            logger.error(message);
            return false;
        } else if ((getUnitNumber() >= possibleAttacker.get().getUnits().size())
            || (getUnitNumber() < 0)) {
            message = String.format("SBF Unit not found! %h", this);
            logger.error(message);
            return false;
        } else if (possibleTarget.get().getUnits().isEmpty()) {
            message = String.format("Target has no units! %h", this);
            logger.error(message);
            return false;
        }
        return true;
    }
}


