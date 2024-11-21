package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.IGame;
import megamek.common.alphaStrike.ASRange;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;
import mekhq.campaign.autoResolve.AutoResolveGame;

import java.util.Optional;

public class AcsStandardUnitAttack extends AbstractAcsAttackAction {
    private static final MMLogger logger = MMLogger.create(AcsStandardUnitAttack.class);

    private final int unitNumber;
    private final ASRange range;

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
    public AcsStandardUnitAttack(int formationId, int unitNumber, int targetId, ASRange range) {
        super(formationId, targetId);
        this.unitNumber = unitNumber;
        this.range = range;
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

    @Override
    public AcsActionHandler getHandler(AcsGameManager gameManager) {
        return new AcsStandardUnitAttackHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(IGame game) {
        var autoResolveGame = (AutoResolveGame) game;
        String message;

        Optional<SBFFormation> possibleAttacker = autoResolveGame.getFormation(getEntityId());
        Optional<SBFFormation> possibleTarget = autoResolveGame.getFormation(getTargetId());
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


