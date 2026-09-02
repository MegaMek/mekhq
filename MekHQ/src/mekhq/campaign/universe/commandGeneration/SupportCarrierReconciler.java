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
package mekhq.campaign.universe.commandGeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.EchelonProfile;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.SupportSection;

/**
 * Keeps the Support Command section of the TOE matching the roster as a campaign runs.
 *
 * <p>{@link SupportPersonnelToTOE} fills carriers once, during command generation. Everyone hired, retrained or
 * returned to duty afterwards would otherwise never appear in the TOE, and carriers would only ever shrink. This class
 * closes that gap by reacting to the events that change whether a character belongs in a carrier.</p>
 *
 * <p><b>Hook the predicate, not the cause.</b> There are many ways a character joins or leaves a support corps -
 * hiring, retraining, graduation, capture, retirement, death, returning from leave. All of them end in a change to one
 * of three fields: primary role, personnel status, or prisoner status. So rather than handling each cause, this class
 * asks one question - <em>should this character hold a carrier seat, and do they?</em> - and acts only when the answer
 * disagrees with reality. A future feature that moves a character through the same setters is handled for free.</p>
 *
 * <p><b>Departures are mostly free.</b> {@link Person#changeStatus} already removes a character from their unit
 * whenever the new status is not active, and {@code setPrisonerStatus} does the same on capture, so death, retirement,
 * leave, capture and the rest need no work here. The one exception this class handles is retraining <em>out</em> of a
 * support role, which changes no status and so leaves the character sitting in a carrier they no longer belong in.</p>
 *
 * <p><b>Cost.</b> Every entry point opens with a guard that rejects the common case in a handful of field reads with
 * no allocation and no iteration; the first test alone rejects everyone already crewing anything. Nothing here runs on
 * the daily tick, and no index or cache is kept - the answer is derived from the live object graph each time, which
 * both avoids a new serialization surface and removes any possibility of the cache going stale.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class SupportCarrierReconciler {
    private static final MMLogger LOGGER = MMLogger.create(SupportCarrierReconciler.class);

    private SupportCarrierReconciler() {
        // utility class
    }

    /**
     * Seats a character in a support carrier when they belong in one and hold no seat.
     *
     * <p>Safe to call for any character on any event: everyone who is already crewing a unit, is not support staff, is
     * not on active duty, is a prisoner, or belongs to a campaign with no support structure is rejected by the opening
     * guard.</p>
     *
     * @param campaign the campaign that owns the TOE
     * @param person   the character to consider
     */
    public static void seatIfEligible(@Nullable Campaign campaign, @Nullable Person person) {
        if ((campaign == null) || (person == null)) {
            return;
        }

        // Cheapest first: rejects everyone already crewing anything, carrier or fighting unit alike. A support
        // character who crews a real unit is already in the TOE by that route and must not be pulled out of it.
        if (person.getUnit() != null) {
            return;
        }

        if (!isEligibleForCarrier(person)) {
            return;
        }

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        if (supportCommand == null) {
            // This campaign has no support structure - it was not built by the Command Generator, or the player
            // deleted the formation. Either way, do not invent one.
            return;
        }

        seat(campaign, person, supportCommand);
    }

    /**
     * Releases a character from their carrier when they no longer belong in one.
     *
     * <p>This covers the single departure case the campaign engine does not already handle: retraining out of a
     * support role, which changes no status and so leaves the character seated in a carrier for a profession that is no
     * longer theirs.</p>
     *
     * @param campaign the campaign that owns the TOE
     * @param person   the character to consider
     */
    public static void releaseIfIneligible(@Nullable Campaign campaign, @Nullable Person person) {
        if ((campaign == null) || (person == null)) {
            return;
        }

        Unit unit = person.getUnit();
        if ((unit == null) || !unit.isCarrier()) {
            return;
        }

        if (isEligibleForCarrier(person) && (professionOf(unit) == person.getPrimaryRole())) {
            return;
        }

        LOGGER.info("Releasing {} from carrier {}: no longer carried by this profession",
              person.getFullName(), unit.getName());
        unit.remove(person, true);
    }

    /**
     * Removes a carrier that has lost its last crew member, and the profession formation it leaves behind if that
     * formation is now empty too.
     *
     * <p>Deliberately does only the cheap case. A mass-casualty event fires one crew-assignment event per character, so
     * this must stay proportional to a single unit; rebalancing across carriers belongs on the arrival path, where the
     * section is being walked anyway.</p>
     *
     * @param campaign the campaign that owns the TOE
     * @param unit     the unit whose crew changed
     */
    public static void onCarrierCrewChanged(@Nullable Campaign campaign, @Nullable Unit unit) {
        if ((campaign == null) || (unit == null) || !unit.isCarrier()) {
            return;
        }

        if (!unit.getCrew().isEmpty()) {
            return;
        }

        Formation parent = campaign.getPlayerForce().getFormation(unit.getFormationId());
        LOGGER.info("Removing empty support carrier {}", unit.getName());
        campaign.removeUnit(unit.getId());

        removeIfEmptyProfessionFormation(campaign, parent);
    }

    /**
     * Brings a whole campaign's carriers into line in one pass.
     *
     * <p>Run when a campaign is loaded. Events are raised while a save is parsed but nothing is subscribed to them
     * yet, so they cannot be relied on to place characters recruited before this feature existed.</p>
     *
     * <p>The pass is idempotent - it computes where each character should be and acts only on disagreement - so
     * running it repeatedly changes nothing and a partially reconciled campaign converges rather than doubling up.</p>
     *
     * @param campaign the campaign to reconcile
     */
    public static void reconcileAll(@Nullable Campaign campaign) {
        if (campaign == null) {
            return;
        }

        markLegacyCarriers(campaign);

        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        if (supportCommand == null) {
            LOGGER.info("Support carrier reconciliation: no Support Command formation, so carriers are not managed"
                              + " in this campaign");
            return;
        }

        // Snapshot before iterating: seating a character mutates unit crews and can create units.
        List<Person> personnel = new ArrayList<>(campaign.getPlayerForce()
                                                       .getHumanResources()
                                                       .getActivePersonnel(false, false));

        int released = 0;
        for (Person person : personnel) {
            Unit unit = person.getUnit();
            if ((unit != null) && unit.isCarrier()) {
                int before = unit.getCrew().size();
                releaseIfIneligible(campaign, person);
                if (unit.getCrew().size() < before) {
                    released++;
                }
            }
        }

        int seated = 0;
        for (Person person : personnel) {
            if (person.getUnit() != null) {
                continue;
            }
            seatIfEligible(campaign, person);
            if (person.getUnit() != null) {
                seated++;
            }
        }

        // Always reported, even when nothing changed: a second load of the same save should say "seated 0, released
        // 0", and that line is how a playtest confirms the sweep is idempotent.
        LOGGER.info("Support carrier reconciliation: seated {}, released {}", seated, released);
    }

    /**
     * Marks carriers in campaigns saved before the carrier flag existed.
     *
     * <p>A unit qualifies only if it uses a carrier chassis <em>and</em> carries at least one support character, so a
     * support squad a player bought and crewed themselves is not mistaken for a generated carrier.</p>
     */
    private static void markLegacyCarriers(Campaign campaign) {
        int marked = 0;
        for (Unit unit : new ArrayList<>(campaign.getUnits())) {
            if (unit.isCarrier() || (unit.getEntity() == null)) {
                continue;
            }
            if (!SupportPersonnelToTOE.isCarrierChassis(unit.getEntity().getChassis())) {
                continue;
            }
            if (professionOf(unit) == null) {
                continue;
            }
            unit.setCarrier(true);
            marked++;
        }
        if (marked > 0) {
            LOGGER.info("Marked {} pre-existing support carrier(s) during load", marked);
        }
    }

    /**
     * Whether this character belongs in a support carrier at all: carried profession, on active duty, and not a
     * prisoner.
     */
    private static boolean isEligibleForCarrier(Person person) {
        if (SupportPersonnelToTOE.sectionFor(person.getPrimaryRole()) == null) {
            return false;
        }
        if (!person.getStatus().isActiveFlexible()) {
            return false;
        }
        return person.getPrisonerStatus().isFreeOrBondsman();
    }

    /**
     * Puts the character in the first carrier of their profession with a free seat, or builds a new carrier when none
     * has room.
     */
    private static void seat(Campaign campaign, Person person, Formation supportCommand) {
        PersonnelRole profession = person.getPrimaryRole();
        boolean isClan = campaign.getPlayerForce().isClanForce();
        EchelonProfile profile = isClan ? SupportPersonnelToTOE.clanProfile() : SupportPersonnelToTOE.innerSphereProfile();

        Unit sibling = null;
        Unit partialSquad = null;

        for (UUID unitId : supportCommand.getAllUnits(false)) {
            Unit carrier = campaign.getUnit(unitId);
            if ((carrier == null) || !carrier.isCarrier()) {
                continue;
            }
            if (professionOf(carrier) != profession) {
                continue;
            }
            // Checked rather than left to the assignment, which writes a campaign report on every rejection. Only
            // campaigns using bases can fail this.
            if (!LocationUtils.areSameEffectiveLocation(carrier, person)) {
                continue;
            }

            // Remembered even when full: a sibling tells us which formation a new carrier of this profession belongs
            // under, without having to match on a localized formation name.
            sibling = carrier;

            if (carrier.getTotalCrewSize() < carrier.getFullCrewSize()) {
                SupportPersonnelToTOE.ensureInfantrySkill(person);
                carrier.addPilotOrSoldier(person);
                LOGGER.info("Seated {} in support carrier {} ({}/{})", person.getFullName(), carrier.getName(),
                      carrier.getTotalCrewSize(), carrier.getFullCrewSize());
                return;
            }

            // A full carrier smaller than a full squad was built undersized at generation to fit a remainder. Now
            // that the profession is growing it should be topped up to a full squad, not left as a tiny full unit
            // beside a fresh one. Prefer the smallest such carrier so the fewest people move.
            boolean isPartialSquad = carrier.getFullCrewSize() < profile.squadUnitSize();
            if (isPartialSquad && ((partialSquad == null) || (carrier.getFullCrewSize() < partialSquad.getFullCrewSize()))) {
                partialSquad = carrier;
            }
        }

        if ((partialSquad != null) && upgradeAndSeat(campaign, partialSquad, person, profile)) {
            return;
        }

        createCarrierFor(campaign, person, supportCommand, sibling, profile);
    }

    /**
     * Replaces a full, undersized squad carrier with a full-size one, moving its crew across and seating the newcomer.
     *
     * <p>Generation builds a "Support Squad (2 person)" for a two-person remainder so the TOE does not show five empty
     * seats. Once a third person of that profession arrives, the right container is a full squad holding all three,
     * with room for the next four - not the two-seat unit kept beside a brand-new one.</p>
     *
     * <p>Order matters. Each crew member is seated in the bigger carrier <em>before</em> being removed from the small
     * one: {@link Unit#remove} only clears a character's unit when it still points at the unit being left, so this
     * sequence never leaves anyone unit-less mid-transfer, and the small carrier is still intact when the transfer is
     * logged against it. When the last member leaves, the crew event removes the emptied small carrier; its formation
     * survives because the bigger carrier already sits in it.</p>
     *
     * @return {@code true} if the newcomer was seated; {@code false} if the bigger carrier could not be built, in which
     *       case nothing was moved
     */
    private static boolean upgradeAndSeat(Campaign campaign, Unit small, Person person, EchelonProfile profile) {
        boolean isClan = campaign.getPlayerForce().isClanForce();
        String professionLabel = person.getPrimaryRole().getLabel(isClan);
        Formation parent = campaign.getPlayerForce().getFormation(small.getFormationId());

        SupportPersonnelToTOE.CarrierSpec spec = new SupportPersonnelToTOE.CarrierSpec(
              profile.squadUnitNameFor(profile.squadUnitSize()), List.of(), false, professionLabel);
        Unit bigger = SupportPersonnelToTOE.createCarrierUnit(campaign, spec);
        if (bigger == null) {
            return false;
        }
        if (parent != null) {
            campaign.getPlayerForce().addUnitToFormation(bigger, parent.getId(), campaign);
        }

        List<Person> crew = new ArrayList<>(small.getCrew());
        for (Person member : crew) {
            SupportPersonnelToTOE.ensureInfantrySkill(member);
            bigger.addPilotOrSoldier(member, small, true);
            small.remove(member, false);
        }

        SupportPersonnelToTOE.ensureInfantrySkill(person);
        bigger.addPilotOrSoldier(person);
        LOGGER.info("Upgraded support carrier {} to {} for {} ({} moved, now {}/{})", small.getName(),
              bigger.getName(), person.getFullName(), crew.size(), bigger.getTotalCrewSize(),
              bigger.getFullCrewSize());
        return true;
    }

    /**
     * Builds a carrier for a character who could not be seated in an existing one.
     *
     * <p>A full-size squad is used rather than the top-tier platoon: it is the smallest carrier that still absorbs the
     * hires that follow, so a growing campaign does not accumulate one tiny unit per recruit, and it wastes at most a
     * squad's worth of seats. A force grown by hiring therefore ends up with more, smaller carriers than the same force
     * would have had if generated in one go; squads are never promoted to platoons.</p>
     */
    private static void createCarrierFor(Campaign campaign, Person person, Formation supportCommand,
          @Nullable Unit sibling, EchelonProfile profile) {
        boolean isClan = campaign.getPlayerForce().isClanForce();
        String professionLabel = person.getPrimaryRole().getLabel(isClan);

        SupportPersonnelToTOE.CarrierSpec spec = new SupportPersonnelToTOE.CarrierSpec(profile.squadUnitNameFor(profile.squadUnitSize()),
              List.of(person), false, professionLabel);

        Unit carrier = SupportPersonnelToTOE.createCarrierUnit(campaign, spec);
        if (carrier == null) {
            LOGGER.warn("Could not build a support carrier for {}; they stay unfiled", person.getFullName());
            return;
        }

        Formation parent = supportCommand;
        if (sibling != null) {
            Formation siblingFormation = campaign.getPlayerForce().getFormation(sibling.getFormationId());
            if (siblingFormation != null) {
                parent = siblingFormation;
            }
        }

        campaign.getPlayerForce().addUnitToFormation(carrier, parent.getId(), campaign);
        LOGGER.info("Built support carrier {} for {} under {}", carrier.getName(), person.getFullName(),
              parent.getName());
    }

    /**
     * Deletes a profession formation left empty by removing its last carrier.
     *
     * <p>Nothing in the campaign engine prunes empty formations - {@code removeFormation} is only ever called from the
     * TOE context menu - so a reconciler that creates formations has to clean up its own. Support Command itself is
     * never removed: it is the anchor the reconciler is found by, and it is expected to sit empty between a section
     * emptying and the next hire.</p>
     */
    private static void removeIfEmptyProfessionFormation(Campaign campaign, @Nullable Formation formation) {
        if (formation == null) {
            return;
        }
        if (formation.getId() == campaign.getPlayerForce().getSupportCommandFormationId()) {
            return;
        }
        if (!formation.getUnits().isEmpty() || !formation.getSubFormations().isEmpty()) {
            return;
        }

        LOGGER.info("Removing empty support formation {}", formation.getName());
        campaign.getPlayerForce().removeFormation(formation, campaign);
    }

    /**
     * The profession a carrier holds, taken from its crew.
     *
     * <p>Read from the crew rather than the unit's fluff name because the fluff name is a localized label, and a
     * campaign whose language changed would no longer match it.</p>
     *
     * @return the carried role, or {@code null} if the carrier is empty
     */
    private static @Nullable PersonnelRole professionOf(Unit carrier) {
        for (Person crew : carrier.getCrew()) {
            if (SupportPersonnelToTOE.sectionFor(crew.getPrimaryRole()) != null) {
                return crew.getPrimaryRole();
            }
        }
        return null;
    }
}
