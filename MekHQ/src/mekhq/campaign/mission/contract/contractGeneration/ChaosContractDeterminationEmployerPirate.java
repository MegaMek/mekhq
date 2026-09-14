package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * Determines the "employer" for an acts-of-piracy Chaos contract. A pirate raid has no paymaster: the employer slot is
 * an underworld contact feeding the raiders a lead on a target. It reuses the shared assembly of
 * {@link AbstractContractDeterminationEmployer} but overrides the per-faction hooks so that both the flavor faction (the
 * contact, drawn from pirate/black-market circles) and the territorial anchor are the generic
 * {@link Faction#PIRATE_FACTION_CODE pirate faction}. The anchor being the pirate faction makes the raiding band the
 * attacker in target selection (firing the pirate-specific location tiers and staging the raid from lawless space),
 * while the actual victim is drawn as the enemy. {@link #checkForSpecialEmployer} is barred (an underworld tip has no
 * ComStar/Word of Blake backer), and the employer type is always {@link ChaosEmployerType#UNDERWORLD_CONTACT}, whose terms
 * reflect a self-funded band that keeps all its plunder; the pirate-side broker who carries the tip is the negotiator.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosContractDeterminationEmployerPirate extends AbstractContractDeterminationEmployer {
    /**
     * The lead comes from an underworld contact, so the flavor faction is always the pirate faction (the contact's
     * circles) regardless of the rolled type.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable Faction resolveFlavorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, Faction playerFaction) {
        return Factions.getInstance().getFaction(PIRATE_FACTION_CODE);
    }

    /**
     * A pirate raid is launched from the band's own lawless holdings rather than the contact's turf, so the anchor is
     * always the pirate faction; this makes the raiders the attacker in target selection.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected Faction resolveAnchorFaction(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, Faction playerFaction, Faction flavor) {
        return Factions.getInstance().getFaction(PIRATE_FACTION_CODE);
    }

    /**
     * An underworld tip has no covert patron fronting, taking over, or bankrolling the raid, so no special employer is
     * ever generated.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable Faction checkForSpecialEmployer(int currentYear, boolean covertViable) {
        return null;
    }

    /**
     * Every acts-of-piracy contract's lead comes from an underworld contact.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected ChaosEmployerType determineEmployerType() {
        return ChaosEmployerType.UNDERWORLD_CONTACT;
    }
}
