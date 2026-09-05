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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Formation;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.EchelonProfile;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.SupportSection;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationIconBuilder;

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
 * <p><b>Mirror generation.</b> A profession's carriers are always the carriers {@link SupportPersonnelToTOE#packPool}
 * would build for that many people, using the faction's echelon - so 42 Inner Sphere administrators are one platoon
 * and two squads whether they were generated together or hired over a year. Each arrival and departure compares the
 * carriers that exist against what the packer wants for the new total; when they match, the person simply takes or
 * vacates a seat, and only when a packing boundary is crossed is the profession rebuilt.</p>
 *
 * <p><b>Parked carriers.</b> A carrier that is deployed, mothballed or mothballing is left exactly as it is: nothing
 * here seats into it, releases from it, reshapes its profession, prunes its formation, or builds a new carrier beside
 * it. Mothballing matters most in practice - a contract start mothballs the whole force for transit, stripping every
 * carrier's crew and pulling it from the TOE, and {@code MothballInfo} restores both on arrival. Whether carriers may
 * deploy at all is decided by {@link SupportCarrierDeployment} - closed today, to be opened by a future scenario type
 * that pulls support staff into a fight; this class is already built for that day. The status events casualties raise are handled by the engine as usual, and once the survivors come
 * home a {@code DeploymentChangedEvent} triggers the shape check that the departure-side path declined while they
 * were out. A carrier deployed by a save written before the gate existed gets the same treatment.</p>
 *
 * <p><b>Cost.</b> Every entry point opens with a guard that rejects the common case in a handful of field reads with
 * no allocation and no iteration; the first test alone rejects everyone already crewing anything. The shape comparison
 * is a sort of a few unit names. Because the packer sizes the tail squad exactly, that shape changes on most hires
 * while a squad is filling - so a reshape must be cheap, and it is: only carriers whose name is no longer wanted are
 * touched, and only their crew move. Growing by one swaps at most the tail squad and moves at most six people; the
 * platoons holding most of the profession are never rebuilt. The one expensive step is promotion, when a remainder
 * becomes a platoon and roughly a platoon's worth of people move once. Nothing here runs on the daily tick, and no
 * index or cache is kept - the answer is derived from the live object graph each time, which both avoids a new
 * serialization surface and removes any possibility of the cache going stale.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class SupportCarrierReconciler {
    private static final MMLogger LOGGER = MMLogger.create(SupportCarrierReconciler.class);

    /**
     * Set while a re-pack is in progress. Seating and removing crew during a re-pack fires the same crew events this
     * class subscribes to, and without this flag each of those would see a profession mid-rebuild - old and new
     * carriers both present - and start another re-pack from inside the first. Campaign mutation is single-threaded
     * on the EDT, so a plain static is sufficient.
     */
    private static boolean repacking = false;

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

        if (repacking) {
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
        if ((unit == null) || !unit.isCarrier() || isParked(unit)) {
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
     * <p>A departure that leaves the carrier occupied re-checks the profession's shape against what generation would
     * build, so shrinking mirrors generation as growth does. That check is one name comparison unless a packing
     * boundary was crossed, so a mass-casualty event re-packs a profession a few times at most rather than once per
     * casualty.</p>
     *
     * @param campaign the campaign that owns the TOE
     * @param unit     the unit whose crew changed
     */
    public static void onCarrierCrewChanged(@Nullable Campaign campaign, @Nullable Unit unit) {
        // isMothballed is already true while setMothballed strips the crew, so the last removal - the one that would
        // otherwise read as "empty, delete it" - is correctly ignored here.
        if ((campaign == null) || (unit == null) || !unit.isCarrier() || isParked(unit)) {
            return;
        }

        if (!unit.getCrew().isEmpty()) {
            if (!repacking) {
                repackIfMisshapen(campaign, unit);
            }
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

        // Turning the option off leaves existing carriers exactly where they are; they simply stop being managed.
        if (!isEnabled(campaign)) {
            return;
        }

        markLegacyCarriers(campaign);
        recoverSupportCommandId(campaign);

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

        // Empty carriers are normally removed by the crew event that empties them, but a carrier built for a newcomer
        // who was then never seated has no such event. Sweep them here so a campaign that hit that state self-heals.
        int emptied = 0;
        for (Unit unit : new ArrayList<>(campaign.getUnits())) {
            if (unit.isCarrier() && unit.getCrew().isEmpty() && !isParked(unit)) {
                Formation parent = campaign.getPlayerForce().getFormation(unit.getFormationId());
                LOGGER.info("Removing empty support carrier {}", unit.getName());
                campaign.removeUnit(unit.getId());
                removeIfEmptyProfessionFormation(campaign, parent);
                emptied++;
            }
        }

        // Shape is normally kept by the arrival and departure events, but headcount can change with no event at all -
        // the loader drops crew whose roster entry is gone, for one. Check every profession's shape once here, so a
        // platoon that lost enough people to be squads again becomes squads, as generation would have built it.
        int reshaped = reshapeAllProfessions(campaign, supportCommand, true);

        // Headcount decides the support echelons, so re-size them once the shapes are settled.
        SupportPersonnelToTOE.resizeSupportEchelons(campaign);

        // Always reported, even when nothing changed: a second load of the same save should say "seated 0, released
        // 0, removed 0, reshaped 0", and that line is how a playtest confirms the sweep is idempotent.
        LOGGER.info("Support carrier reconciliation: seated {}, released {}, removed {} empty carrier(s), reshaped {}"
                          + " profession(s)", seated, released, emptied, reshaped);
    }

    /**
     * Marks carriers in campaigns saved before the carrier flag existed.
     *
     * <p>A unit qualifies only if it uses a carrier chassis <em>and</em> carries at least one support character, so a
     * support squad a player bought and crewed themselves is not mistaken for a generated carrier.</p>
     */
    /**
     * Recovers the Support Command formation id for a campaign saved before it was persisted.
     *
     * <p>The id is only written by generation. An older save has carriers but no id, and without this the reconciler
     * would treat it as a campaign that never opted in. Support Command is found as the lowest common ancestor of
     * every carrier's formation - the one formation that sits above all of Maintenance, Medical and Command - which
     * needs no name matching and holds for any layout the generator produces. A campaign whose carriers all sit in
     * one section resolves to that section instead, which files any later profession under it; acceptable, and rare.</p>
     */
    /**
     * Whether support teams are switched on for this campaign.
     *
     * @param campaign the campaign
     *
     * @return the value of {@link CampaignOption#USE_SUPPORT_TEAMS}
     */
    public static boolean isEnabled(Campaign campaign) {
        return campaign.getCampaignOptions().get(CampaignOption.USE_SUPPORT_TEAMS);
    }

    /**
     * The support staff who are not in a support team, in roster order.
     *
     * <p>Used to decide whether a campaign has anything to convert, to show the player a count before they commit,
     * and as the pool handed to generation when they accept.</p>
     *
     * @param campaign the campaign
     *
     * @return the eligible support characters who crew no unit; empty when there are none
     */
    public static List<Person> looseSupportStaff(Campaign campaign) {
        List<Person> loose = new ArrayList<>();
        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if ((person.getUnit() == null) && isEligibleForCarrier(person)) {
                loose.add(person);
            }
        }
        return loose;
    }

    /**
     * Whether this campaign could be converted: support teams are on, none exist yet, and there is staff to put in
     * them. This is the question the load-time offer asks.
     *
     * @param campaign the campaign
     *
     * @return {@code true} if there is something to offer
     */
    public static boolean hasStaffToOrganize(@Nullable Campaign campaign) {
        if ((campaign == null) || !isEnabled(campaign)) {
            return false;
        }
        if (campaign.getPlayerForce().getSupportCommandFormation() != null) {
            return false;
        }
        return !looseSupportStaff(campaign).isEmpty();
    }

    /**
     * Organizes a campaign's loose support staff into support teams, exactly as generation would have built them.
     *
     * <p>This is the conversion for a campaign that never generated a command: it creates the Support Command
     * formation and packs the staff into carriers using the faction's echelon, then runs the normal sweep so the
     * result is identical to a generated campaign's. A campaign that already has a Support Command is only swept -
     * its loose staff are seated into the carriers that exist.</p>
     *
     * @param campaign the campaign to convert
     *
     * @return the number of support characters organized; 0 when there was nothing to do
     */
    public static int organizeLooseStaff(@Nullable Campaign campaign) {
        if ((campaign == null) || !isEnabled(campaign)) {
            return 0;
        }

        List<Person> loose = looseSupportStaff(campaign);
        if (loose.isEmpty()) {
            return 0;
        }

        if (campaign.getPlayerForce().getSupportCommandFormation() == null) {
            LOGGER.info("Organizing {} loose support character(s) into new support teams", loose.size());
            // Generation decorates the whole TOE afterwards; a conversion has to decorate what it just built, or the
            // new formations sit in the TOE bare. Which ones those are is the difference between before and after -
            // the HQ formation is created too when the campaign never had one, and the player's own formations must
            // keep the icons they have.
            Set<Integer> before = formationIds(campaign);
            SupportPersonnelToTOE.organize(campaign, loose, campaign.getPlayerForce().isClanForce());
            List<Formation> built = new ArrayList<>();
            for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
                if (!before.contains(formation.getId())) {
                    built.add(formation);
                }
            }
            FormationIconBuilder.applyIconsToFormations(built, campaign);
        } else {
            LOGGER.info("Seating {} loose support character(s) into the existing support teams", loose.size());
        }
        reconcileAll(campaign);
        return loose.size();
    }

    /**
     * @param campaign the campaign
     *
     * @return the ids of every formation in the TOE right now
     */
    private static Set<Integer> formationIds(Campaign campaign) {
        Set<Integer> ids = new HashSet<>();
        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            ids.add(formation.getId());
        }
        return ids;
    }

    private static void recoverSupportCommandId(Campaign campaign) {
        if (campaign.getPlayerForce().getSupportCommandFormationId() != Formation.FORMATION_NONE) {
            return;
        }

        List<List<Integer>> chains = new ArrayList<>();
        for (Unit unit : campaign.getUnits()) {
            if (!unit.isCarrier()) {
                continue;
            }
            Formation formation = campaign.getPlayerForce().getFormation(unit.getFormationId());
            if (formation == null) {
                continue;
            }
            // Root-to-leaf list of formation ids for this carrier.
            List<Integer> chain = new ArrayList<>();
            for (Formation node = formation; node != null; node = node.getParentFormation()) {
                chain.add(0, node.getId());
            }
            chains.add(chain);
        }
        if (chains.isEmpty()) {
            return;
        }

        // Walk the chains in step from the root; the last id they all share is the lowest common ancestor.
        int common = Formation.FORMATION_NONE;
        List<Integer> first = chains.get(0);
        for (int depth = 0; depth < first.size(); depth++) {
            int candidate = first.get(depth);
            boolean shared = true;
            for (List<Integer> chain : chains) {
                if ((chain.size() <= depth) || (chain.get(depth) != candidate)) {
                    shared = false;
                    break;
                }
            }
            if (!shared) {
                break;
            }
            common = candidate;
        }

        // Never settle on the origin node: that would make every campaign look like it opted in.
        if ((common == Formation.FORMATION_NONE) || (common == Formation.FORMATION_ORIGIN)) {
            return;
        }
        Formation supportCommand = campaign.getPlayerForce().getFormation(common);
        if (supportCommand == null) {
            return;
        }
        campaign.getPlayerForce().setSupportCommandFormationId(common);
        LOGGER.info("Recovered Support Command formation '{}' (id {}) from {} carrier(s) in a save that predates the"
                          + " persisted id", supportCommand.getName(), common, chains.size());
    }

    private static void markLegacyCarriers(Campaign campaign) {
        int marked = 0;
        for (Unit unit : new ArrayList<>(campaign.getUnits())) {
            if (unit.isCarrier() || (unit.getEntity() == null)) {
                continue;
            }
            if (!SupportPersonnelToTOE.isCarrierChassis(unit.getEntity().getChassis())) {
                continue;
            }
            // Crewed by support staff, or - for one sitting empty in mothballs - carrying the profession label
            // generation stamps on every carrier. A support squad a player bought themselves has neither.
            if ((professionOf(unit) == null) && !isGenerationLabel(campaign, unit.getFluffName())) {
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
     * Seats a character, keeping the profession's carriers shaped exactly as generation would shape them.
     *
     * <p>The rule is "mirror generation": the carriers for a profession are whatever {@link SupportPersonnelToTOE#packPool}
     * would build for that many people, using the faction's echelon. So the check is a comparison, not a search - pack
     * the profession's current crew plus the newcomer, and compare the carrier names the packer wants against the
     * carrier names that exist. When they match, the newcomer takes a free seat and nothing else moves. When they
     * differ, the profession is re-packed.</p>
     *
     * <p>Because a single arrival only changes the ideal shape at a boundary - every full squad, and the point where a
     * remainder becomes a platoon - a bulk hire re-packs a handful of times, not once per person.</p>
     */
    private static void seat(Campaign campaign, Person person, Formation supportCommand) {
        PersonnelRole profession = person.getPrimaryRole();
        List<Unit> carriers = carriersOf(campaign, supportCommand, profession);
        if (anyParked(carriers) || hasParkedCarrier(campaign, profession)) {
            // A carrier in a battle or in mothballs is left exactly as it is, and no new one is built beside it. The
            // next event after it comes back catches this person up.
            LOGGER.info("Not seating {}: a {} carrier is deployed or mothballed", person.getFullName(),
                  profession.getLabel(campaign.getPlayerForce().isClanForce()));
            return;
        }

        List<Person> people = new ArrayList<>();
        for (Unit carrier : carriers) {
            people.addAll(carrier.getCrew());
        }
        people.add(person);

        boolean isClan = campaign.getPlayerForce().isClanForce();
        EchelonProfile profile = isClan ? SupportPersonnelToTOE.clanProfile() : SupportPersonnelToTOE.innerSphereProfile();
        String label = profession.getLabel(isClan);
        List<SupportPersonnelToTOE.CarrierSpec> ideal = SupportPersonnelToTOE.packPool(people, profile, label);

        if (!shapesMatch(carriers, ideal)) {
            repack(campaign, profession, carriers, ideal, supportCommand);
            // The newcomer held no seat, so the reshape did not move them. The shape was sized for them, though, so
            // a seat exists in the refreshed set.
            carriers = carriersOf(campaign, supportCommand, profession);
        }

        int freeSeatsElsewhere = 0;
        for (Unit carrier : carriers) {
            if (carrier.getTotalCrewSize() >= carrier.getFullCrewSize()) {
                continue;
            }
            // Checked rather than left to the assignment, which writes a campaign report on every rejection. Only
            // campaigns using bases can fail this.
            if (!LocationUtils.areSameEffectiveLocation(carrier, person)) {
                freeSeatsElsewhere++;
                continue;
            }
            SupportPersonnelToTOE.ensureInfantrySkill(person);
            carrier.addPilotOrSoldier(person);
            LOGGER.info("Seated {} in support carrier {} ({}/{})", person.getFullName(), carrier.getName(),
                  carrier.getTotalCrewSize(), carrier.getFullCrewSize());
            return;
        }

        // Leave them loose; their next event tries again. Say which of the two possible reasons applied, because they
        // point at different problems: a location mismatch is a base-using campaign, no free seat at all is a bug.
        if (freeSeatsElsewhere > 0) {
            LOGGER.info("Could not seat {}: the only free {} seats are at another location", person.getFullName(),
                  label);
        } else {
            LOGGER.warn("Could not seat {}: no {} carrier has a free seat, although the shape was sized for them",
                  person.getFullName(), label);
        }
    }

    /**
     * Brings every profession's carriers back into line after something changed headcount without a person event.
     *
     * <p>Called from the load sweep, and whenever a deployment ends. A carrier can be deployed - a base attack may pull
     * support staff into a fight - and while it is, nothing here touches it. Casualties in that fight fire the usual
     * status events, but the departure-side check declines to reshape around a deployed carrier, so the profession's
     * shape can be stale by the time the survivors come home. This catches that up.</p>
     *
     * @param campaign       the campaign
     * @param supportCommand the Support Command formation, already resolved
     * @param resyncEntities {@code true} to also rewrite each carrier's entity from its crew - needed after a load,
     *                       where the loader may have dropped crew without doing so
     *
     * @return how many professions were reshaped
     */
    static int reshapeAllProfessions(Campaign campaign, Formation supportCommand, boolean resyncEntities) {
        int reshaped = 0;
        List<PersonnelRole> checked = new ArrayList<>();
        for (UUID unitId : supportCommand.getAllUnits(false)) {
            Unit carrier = campaign.getUnit(unitId);
            if ((carrier == null) || !carrier.isCarrier()) {
                continue;
            }
            if (resyncEntities && !isParked(carrier)) {
                // The entity's trooper count is written from the crew on every seat and release, but not by the
                // loader when it drops crew whose roster entry is gone.
                carrier.resetPilotAndEntity();
            }
            PersonnelRole profession = professionOf(carrier);
            if ((profession == null) || checked.contains(profession)) {
                continue;
            }
            checked.add(profession);
            if (repackIfMisshapen(campaign, carrier)) {
                reshaped++;
            }
        }
        return reshaped;
    }

    /**
     * Catches carriers up after a deployment ends.
     *
     * <p>Cheap to call on every deployment change: it is one name comparison per profession, and any profession that
     * still has a carrier in the field is skipped.</p>
     *
     * @param campaign the campaign
     */
    public static void onDeploymentChanged(@Nullable Campaign campaign) {
        if ((campaign == null) || repacking) {
            return;
        }
        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        if (supportCommand == null) {
            return;
        }
        int reshaped = reshapeAllProfessions(campaign, supportCommand, false);
        if (reshaped > 0) {
            LOGGER.info("Deployment changed: reshaped {} support profession(s)", reshaped);
        }
    }

    /**
     * Re-checks a profession's carrier shape after a departure, so shrinking mirrors generation as growth does.
     *
     * <p>Guarded by the same shape comparison as arrival, so a departure that does not cross a packing boundary costs
     * one comparison and nothing else. A mass-casualty event therefore re-packs a profession a few times at most.</p>
     *
     * @return {@code true} if the profession was reshaped
     */
    private static boolean repackIfMisshapen(Campaign campaign, Unit carrier) {
        PersonnelRole profession = professionOf(carrier);
        if (profession == null) {
            return false;
        }
        Formation supportCommand = campaign.getPlayerForce().getSupportCommandFormation();
        if (supportCommand == null) {
            return false;
        }

        List<Unit> carriers = carriersOf(campaign, supportCommand, profession);
        if (anyParked(carriers) || hasParkedCarrier(campaign, profession)) {
            return false;
        }
        List<Person> people = new ArrayList<>();
        for (Unit existing : carriers) {
            people.addAll(existing.getCrew());
        }
        if (people.isEmpty()) {
            return false;
        }

        boolean isClan = campaign.getPlayerForce().isClanForce();
        EchelonProfile profile = isClan ? SupportPersonnelToTOE.clanProfile() : SupportPersonnelToTOE.innerSphereProfile();
        List<SupportPersonnelToTOE.CarrierSpec> ideal = SupportPersonnelToTOE.packPool(people, profile,
              profession.getLabel(isClan));
        if (!shapesMatch(carriers, ideal)) {
            repack(campaign, profession, carriers, ideal, supportCommand);
            return true;
        }
        return false;
    }

    /**
     * Reshapes a profession's carriers to match {@code ideal}, touching as little as possible.
     *
     * <p>The packer's output is used for its <em>shape</em> - which carrier names, how many of each - not for who sits
     * where. Every existing carrier whose name is still wanted is kept with its crew untouched; only the mismatches are
     * created or destroyed, and only the crew of a destroyed carrier move, into whatever free seats the kept and new
     * carriers offer, platoons first. So growing a profession by one person swaps at most the tail squad and moves at
     * most six people, and the platoons that hold most of the profession never churn.</p>
     *
     * <p>Order matters for the events this fires. New carriers are built empty and filed before anything moves, so the
     * formation is never empty when old carriers go. Displaced crew are seated in their new carrier <em>before</em>
     * being removed from the old one: {@link Unit#remove} clears a character's unit only when it still points at the
     * unit being left, so nobody is unit-less mid-move. The crew event that follows each removal deletes the emptied
     * carrier and finds its formation still occupied.</p>
     */
    private static void repack(Campaign campaign, PersonnelRole profession, List<Unit> oldCarriers,
          List<SupportPersonnelToTOE.CarrierSpec> ideal, Formation supportCommand) {
        if (repacking) {
            return;
        }
        repacking = true;
        try {
            repackUnguarded(campaign, profession, oldCarriers, ideal, supportCommand);
        } finally {
            repacking = false;
        }
    }

    private static void repackUnguarded(Campaign campaign, PersonnelRole profession, List<Unit> oldCarriers,
          List<SupportPersonnelToTOE.CarrierSpec> ideal, Formation supportCommand) {
        Formation parent = supportCommand;
        if (!oldCarriers.isEmpty()) {
            Formation existing = campaign.getPlayerForce().getFormation(oldCarriers.get(0).getFormationId());
            if (existing != null) {
                parent = existing;
            }
        }

        // Pair each wanted carrier name with an existing carrier of that name. What is left over on the ideal side
        // must be built; what is left over on the existing side must go.
        List<Unit> unmatched = new ArrayList<>(oldCarriers);
        List<Unit> kept = new ArrayList<>();
        List<SupportPersonnelToTOE.CarrierSpec> toBuild = new ArrayList<>();
        for (SupportPersonnelToTOE.CarrierSpec spec : ideal) {
            Unit match = null;
            for (Unit candidate : unmatched) {
                if (spec.unitName().equals(candidate.getEntity().getShortNameRaw())) {
                    match = candidate;
                    break;
                }
            }
            if (match != null) {
                unmatched.remove(match);
                kept.add(match);
            } else {
                toBuild.add(spec);
            }
        }
        List<Unit> toDestroy = unmatched;

        // Build the new carriers empty. Filing them before anything moves keeps the formation occupied throughout.
        List<Unit> built = new ArrayList<>();
        for (SupportPersonnelToTOE.CarrierSpec spec : toBuild) {
            SupportPersonnelToTOE.CarrierSpec empty = new SupportPersonnelToTOE.CarrierSpec(spec.unitName(),
                  List.of(), spec.topTier(), spec.professionLabel());
            Unit carrier = SupportPersonnelToTOE.createCarrierUnit(campaign, empty);
            if (carrier == null) {
                LOGGER.warn("Could not build a support carrier {}; the profession keeps its current shape",
                      spec.unitName());
                return;
            }
            campaign.getPlayerForce().addUnitToFormation(carrier, parent.getId(), campaign);
            built.add(carrier);
        }

        // Everyone whose carrier is going away needs a seat. Platoons first, so fill order matches generation.
        List<Person> displaced = new ArrayList<>();
        for (Unit doomed : toDestroy) {
            displaced.addAll(doomed.getCrew());
        }
        List<Unit> targets = new ArrayList<>(kept);
        targets.addAll(built);
        targets.sort((first, second) -> Integer.compare(second.getFullCrewSize(), first.getFullCrewSize()));

        int moved = 0;
        for (Person member : displaced) {
            Unit target = firstWithSeat(targets, member);
            if (target == null) {
                LOGGER.warn("No seat for {} after reshaping {} carriers; they stay where they are",
                      member.getFullName(), profession);
                continue;
            }
            SupportPersonnelToTOE.ensureInfantrySkill(member);
            target.addPilotOrSoldier(member, member.getUnit(), true);
            moved++;
        }

        // Now drop the stale memberships. Anyone still pointing at a doomed carrier could not be reseated and is left.
        for (Unit doomed : toDestroy) {
            for (Person member : new ArrayList<>(doomed.getCrew())) {
                if (member.getUnit() != doomed) {
                    doomed.remove(member, false);
                }
            }
        }

        LOGGER.info("Re-packed {} carriers: kept {}, built {}, removed {}, moved {} people",
              profession.getLabel(campaign.getPlayerForce().isClanForce()), kept.size(), built.size(),
              toDestroy.size(), moved);
    }

    /**
     * Whether a carrier is somewhere the reconciler must not touch it: assigned to a scenario, or mothballed.
     *
     * <p>Mothballing strips a unit's crew and pulls it out of its formation; {@code MothballInfo} keeps both and puts
     * them back on activation. To the reconciler the mothballed carrier looks empty and orphaned, and without this
     * guard it would delete the carrier and then build new ones for the loose crew - which is what happened when a
     * contract start mothballed the whole force for transit. The restore itself is not atomic, so the guard also
     * holds until it has finished.</p>
     */
    private static boolean isParked(Unit carrier) {
        // hasPendingMothballRestore covers the activation window: mothballed is cleared first, then crew is put back
        // one at a time and the formation last. Without it, an event landing mid-restore sees an un-parked carrier
        // that is out of the TOE and half-crewed, and builds a duplicate beside it.
        return carrier.isDeployed() || carrier.isMothballed() || carrier.isMothballing()
                     || carrier.hasPendingMothballRestore();
    }

    /** Whether any of these carriers is parked. */
    private static boolean anyParked(List<Unit> carriers) {
        for (Unit carrier : carriers) {
            if (isParked(carrier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this profession has a parked carrier anywhere in the hangar.
     *
     * <p>A mothballed carrier has left its formation, so the Support Command walk cannot see it; only a hangar scan
     * can. It also has no crew, so its profession is read from the label generation stamped on it. Reached only on a
     * genuine arrival or departure, never on the guarded fast path.</p>
     */
    private static boolean hasParkedCarrier(Campaign campaign, PersonnelRole profession) {
        String label = profession.getLabel(campaign.getPlayerForce().isClanForce());
        for (Unit unit : campaign.getUnits()) {
            if (!unit.isCarrier() || !isParked(unit)) {
                continue;
            }
            PersonnelRole carried = professionOf(unit);
            if ((carried == profession) || ((carried == null) && label.equals(unit.getFluffName()))) {
                return true;
            }
        }
        return false;
    }

    /** The first carrier with a free seat that the character can actually reach. */
    private static @Nullable Unit firstWithSeat(List<Unit> carriers, Person person) {
        for (Unit carrier : carriers) {
            if (carrier.getTotalCrewSize() >= carrier.getFullCrewSize()) {
                continue;
            }
            if (!LocationUtils.areSameEffectiveLocation(carrier, person)) {
                continue;
            }
            return carrier;
        }
        return null;
    }

    /** Whether the carriers that exist are, by name and count, the carriers the packer wants. */
    private static boolean shapesMatch(List<Unit> carriers, List<SupportPersonnelToTOE.CarrierSpec> ideal) {
        if (carriers.size() != ideal.size()) {
            return false;
        }
        List<String> have = new ArrayList<>();
        for (Unit carrier : carriers) {
            have.add(carrier.getEntity().getShortNameRaw());
        }
        List<String> want = new ArrayList<>();
        for (SupportPersonnelToTOE.CarrierSpec spec : ideal) {
            want.add(spec.unitName());
        }
        java.util.Collections.sort(have);
        java.util.Collections.sort(want);
        return have.equals(want);
    }

    /**
     * The carriers under Support Command that hold this profession, in TOE order.
     *
     * <p>A carrier's profession is read from its crew. A carrier standing empty - freshly built for a newcomer, or
     * drained by departures - has no crew to read, so it falls back to the profession label generation stamped on it as
     * a fluff name. Without that fallback an empty carrier is invisible: the newcomer it was built for cannot find it,
     * and the next event builds another.</p>
     */
    private static List<Unit> carriersOf(Campaign campaign, Formation supportCommand, PersonnelRole profession) {
        String label = profession.getLabel(campaign.getPlayerForce().isClanForce());
        List<Unit> carriers = new ArrayList<>();
        for (UUID unitId : supportCommand.getAllUnits(false)) {
            Unit carrier = campaign.getUnit(unitId);
            if ((carrier == null) || !carrier.isCarrier() || (carrier.getEntity() == null)) {
                continue;
            }
            PersonnelRole carried = professionOf(carrier);
            boolean matches = (carried == profession)
                                    || ((carried == null) && label.equals(carrier.getFluffName()));
            if (matches) {
                carriers.add(carrier);
            }
        }
        return carriers;
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
        // removeFormation on a deployed formation looks its scenario up by the formation's own id, which is unset when
        // only an ancestor is deployed. Leave it until the scenario resolves; the next prune catches it.
        if (formation.isDeployed()) {
            return;
        }

        LOGGER.info("Removing empty support formation {}", formation.getName());
        campaign.getPlayerForce().removeFormation(formation, campaign);
    }

    /** Whether this fluff name is the label generation stamps on a carrier for one of the carried professions. */
    private static boolean isGenerationLabel(Campaign campaign, @Nullable String fluffName) {
        if (fluffName == null) {
            return false;
        }
        boolean isClan = campaign.getPlayerForce().isClanForce();
        for (PersonnelRole role : PersonnelRole.values()) {
            if ((SupportPersonnelToTOE.sectionFor(role) != null) && fluffName.equals(role.getLabel(isClan))) {
                return true;
            }
        }
        return false;
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
