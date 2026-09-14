package mekhq.campaign.mission.contract.contractGeneration;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.time.LocalDate;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * Determines the "employer" for an acts-of-piracy Chaos contract. A pirate raid has no paymaster: the employer slot is
 * an underworld contact feeding the raiders a lead on a target. Both the flavor faction (the contact, from
 * pirate/black-market circles) and the territorial anchor are the generic {@link Faction#PIRATE_FACTION_CODE pirate
 * faction}, which makes the raiding band the attacker in target selection (firing the pirate-specific location tiers and
 * staging the raid from lawless space) while the actual victim is drawn as the enemy. The employer type is always
 * {@link ChaosEmployerType#UNDERWORLD_CONTACT}, whose terms reflect a self-funded band that keeps all its plunder; the
 * pirate-side broker who carries the tip is the negotiator.
 *
 * <p>Rarely, a real power secretly bankrolls the raid instead (mirroring how ComStar or the Word of Blake can adopt a
 * mercenary contract): see {@link #determineEmployerFactions}. Those raids are always run covert, so the player never
 * learns who really put them onto the target.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosContractDeterminationEmployerPirate extends AbstractContractDeterminationEmployer {
    /**
     * The odds (1-in-this) that an acts-of-piracy contract is secretly sponsored by a real power rather than a plain
     * underworld tip. Deliberately rare - most piracy is opportunistic, not a proxy war.
     */
    private static final int SPONSORED_RAID_ODDS = 12;

    /**
     * Resolves the employer factions for a pirate raid.
     *
     * <p>Most raids are self-directed: an underworld contact's tip, with the pirate faction as both the visible flavor
     * faction and the territorial anchor (so the band is the attacker and the victim is drawn as a nearby legitimate
     * power).</p>
     *
     * <p>Rarely ({@link #SPONSORED_RAID_ODDS}), a real regional power secretly bankrolls the raid to strike one of its
     * own enemies. The pirate faction stays the visible flavor (the anonymous tip the player deals with), but the
     * sponsor becomes both the covert backer - so Faction Standing accrues to it - and the territorial anchor, so the
     * victim is drawn as that sponsor's enemy. The contract is forced covert downstream (see
     * {@link ActsOfPiracyContractGeneration#determineCovertStatus}), concealing the sponsor as "Undisclosed Employer". If no
     * plausible sponsor is in range the raid falls back to the plain self-directed form.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    EmployerFactions determineEmployerFactions(ChaosEmployerType employerType, LocalDate currentDate,
          ILocation currentLocation, boolean covertViable, Faction playerFaction) {
        Faction pirate = Factions.getInstance().getFaction(PIRATE_FACTION_CODE);

        if (randomInt(SPONSORED_RAID_ODDS) == 0) {
            Faction sponsor = pickRegionalOwner(currentDate, currentLocation, null);
            if (sponsor != null) {
                return new EmployerFactions(employerType, pirate, sponsor, sponsor);
            }
        }

        return new EmployerFactions(employerType, pirate, pirate, null);
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
