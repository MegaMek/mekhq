/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.medical.advancedMedical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

import megamek.common.compute.Compute;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PatientLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * Static helper methods implementing the "advanced medical" sub-system
 */
public class InjuryUtil {
    // Fumble and critical success limits for doctor skills levels 0-10, on a d100
    private static final int[] FUMBLE_LIMITS = { 50, 40, 30, 20, 12, 6, 5, 4, 3, 2, 1 };
    private static final int[] CRIT_LIMITS = { 98, 97, 94, 89, 84, 79, 74, 69, 64, 59, 49 };
    /*
    private static AMEventHandler eventHandler = null;

    public synchronized static void registerEventHandler(Campaign c) {
        if (null != eventHandler) {
            MekHQ.EVENT_BUS.unregister(eventHandler);
        }
        MekHQ.EVENT_BUS.register(eventHandler = new AMEventHandler(c));
    }
    */

    /** Run a daily healing check */
    public static void resolveDailyHealing(Campaign c, Person p) {
        Person doc = c.getPerson(p.getDoctorId());
        // TODO: Reporting
        if ((null != doc) && doc.isDoctor()) {
            if (p.getDaysToWaitForHealing() <= 0) {
                genMedicalTreatment(c, p, doc).forEach(GameEffect::apply);
            }
        } else {
            genUntreatedEffects(c, p).forEach(GameEffect::apply);
        }
        genNaturalHealing(c, p).forEach(GameEffect::apply);
        p.decrementDaysToWaitForHealing();
    }

    /** Resolve injury modifications in case of entering combat with active ones */
    public static void resolveAfterCombat(Campaign c, Person p, int hits) {
        // Gather all the injury actions resulting from the combat situation
        final List<GameEffect> effects = new ArrayList<>();
        p.getInjuries().forEach(i -> effects.addAll(i.getType().genStressEffect(c, p, i, hits)));

        // We could do some fancy display-to-the-user thing here, but for now just resolve all actions
        effects.forEach(GameEffect::apply);
    }

    /**
     * Resolves effects of damage suffered during combat by generating and applying injuries.
     *
     * <p>The method used depends on the campaign's medical system setting:</p>
     * <ul>
     *   <li>Standard model: Uses {@link #resolveCombatDamageUsingStandardModel}</li>
     *   <li>Alternate advanced model: Uses {@link #resolveCombatDamageUsingAlternateModel}</li>
     * </ul>
     *
     * @param campaign the current campaign
     * @param person   the person who suffered combat damage
     * @param hits     the number of TW-scale Hits taken
     */
    public static void resolveCombatDamage(Campaign campaign, Person person, int hits) {
        if (campaign.getCampaignOptions().isUseAlternativeAdvancedMedical()) {
            resolveCombatDamageUsingAlternateModel(campaign, person, hits);
        } else {
            resolveCombatDamageUsingStandardModel(campaign, person, hits);
        }
    }

    /**
     * Resolves combat damage using the standard medical model.
     *
     * <p>Generates injuries based on damage taken, adds them to the person, and logs the injuries if any were
     * created.</p>
     *
     * @param campaign the current campaign
     * @param person   the person who suffered combat damage
     * @param hits     the number of TW-scale Hits taken
     */
    public static void resolveCombatDamageUsingStandardModel(Campaign campaign, Person person, int hits) {
        Collection<Injury> newInjuries = genInjuries(campaign, person, hits);
        newInjuries.forEach(person::addInjury);
        if (!newInjuries.isEmpty()) {
            MedicalLogger.returnedWithInjuries(person, campaign.getLocalDate(), newInjuries);
        }
    }

    /**
     * Resolves combat damage using the alternate advanced medical model.
     *
     * <p>This model provides more detailed injury resolution with these additional features:</p>
     * <ul>
     *   <li>Location-specific injuries with severance mechanics</li>
     *   <li>Automatic removal of injuries from severed limbs</li>
     *   <li>Verification that injuries still exist after processing before logging</li>
     * </ul>
     *
     * <p>Injuries may be automatically removed during processing if the body location they affect has been severed
     * by another injury.</p>
     *
     * @param campaign the current campaign
     * @param person   the person who suffered combat damage
     * @param hits     the number of TW-scale Hits taken
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void resolveCombatDamageUsingAlternateModel(Campaign campaign, Person person, int hits) {
        Collection<Injury> newInjuries = AdvancedMedicalAlternate.generateInjuriesFromHits(campaign, person, hits);
        newInjuries.forEach(person::addInjury);

        // Remove injuries from limbs that have been severed
        AdvancedMedicalAlternate.purgeIllogicalInjuries(person);

        // We double-check the injury has been added, as it might have been removed by purgeIllogicalInjuries
        boolean hasNewInjuries = false;
        List<Injury> currentInjuries = person.getInjuries();
        for (Injury injury : newInjuries) {
            if (!hasNewInjuries && currentInjuries.contains(injury)) {
                hasNewInjuries = true;
            }

            if (injury.getType().impliesDead(injury.getLocation())) {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
            }
        }

        if (hasNewInjuries) {
            MedicalLogger.returnedWithInjuries(person, campaign.getLocalDate(), newInjuries);
        }
    }

    private static void addHitToAccumulator(Map<BodyLocation, Integer> acc, BodyLocation loc) {
        if (!acc.containsKey(loc)) {
            acc.put(loc, 1);
        } else {
            acc.put(loc, acc.get(loc) + 1);
        }
    }

    // Generator methods. Those don't change the state of the person.

    /** Generate combat injuries spread through the whole body */
    public static Collection<Injury> genInjuries(Campaign campaign, Person person, int hits) {
        final Unit unit = person.getUnit();
        final Entity entity = (null != unit) ? unit.getEntity() : null;
        final boolean mekOrAero = ((entity instanceof Mek) || (entity instanceof Aero));
        final int critMod = mekOrAero ? 0 : 2;
        final BiFunction<IntUnaryOperator, Function<BodyLocation, Boolean>, BodyLocation> generator = mekOrAero ?
                                                                                                            HitLocationGen::mekAndAsf :
                                                                                                            HitLocationGen::generic;
        final Map<BodyLocation, Integer> hitAccumulator = new HashMap<>();

        for (int i = 0; i < hits; i++) {
            BodyLocation location = generator.apply(Compute::randomInt, (loc) -> !person.isLocationMissing(loc));

            // apply hit here
            addHitToAccumulator(hitAccumulator, location);
            // critical hits add to the amount
            int roll = Compute.d6(2);
            if (roll + hits + critMod > 12) {
                addHitToAccumulator(hitAccumulator, location);
            }
        }
        List<Injury> newInjuries = new ArrayList<>();
        for (Entry<BodyLocation, Integer> accEntry : hitAccumulator.entrySet()) {
            newInjuries.addAll(genInjuries(campaign, person, accEntry.getKey(), accEntry.getValue()));
        }
        return newInjuries;
    }

    /** Generate combat injuries for a specific body location */
    public static Collection<Injury> genInjuries(Campaign c, Person p, BodyLocation loc, int hits) {
        List<Injury> newInjuries = new ArrayList<>();
        final BiFunction<InjuryType, Integer, Injury> gen = (it, severity) -> it.newInjury(c, p, loc, severity);

        switch (loc) {
            case LEFT_ARM:
            case LEFT_HAND:
            case LEFT_LEG:
            case LEFT_FOOT:
            case RIGHT_ARM:
            case RIGHT_HAND:
            case RIGHT_LEG:
            case RIGHT_FOOT:
                switch (hits) {
                    case 1:
                        newInjuries.add(gen.apply(Compute.randomInt(2) == 0 ?
                                                        InjuryTypes.PUNCTURE :
                                                        InjuryTypes.FRACTURE, 1));
                        break;
                    case 2:
                        newInjuries.add(gen.apply(InjuryTypes.TORN_MUSCLE, 1));
                        break;
                    case 3:
                        newInjuries.add(gen.apply(InjuryTypes.BROKEN_LIMB, 1));
                        break;
                    case 4:
                        newInjuries.add(gen.apply(InjuryTypes.LOST_LIMB, 1));
                        break;
                }
                break;
            case HEAD:
                switch (hits) {
                    case 1:
                        newInjuries.add(gen.apply(InjuryTypes.LACERATION, 1));
                        break;
                    case 2:
                    case 3:
                        newInjuries.add(gen.apply(InjuryTypes.CONCUSSION, hits - 1));
                        break;
                    case 4:
                        newInjuries.add(gen.apply(InjuryTypes.CEREBRAL_CONTUSION, 1));
                        break;
                    default:
                        newInjuries.add(gen.apply(InjuryTypes.CTE, 1));
                        break;
                }
                break;
            case CHEST:
                switch (hits) {
                    case 1:
                        newInjuries.add(gen.apply(Compute.randomInt(2) == 0 ?
                                                        InjuryTypes.PUNCTURE :
                                                        InjuryTypes.FRACTURE, 1));
                        break;
                    case 2:
                        newInjuries.add(gen.apply(InjuryTypes.BROKEN_RIB, 1));
                        break;
                    case 3:
                        newInjuries.add(gen.apply(InjuryTypes.BROKEN_COLLAR_BONE, 1));
                        break;
                    case 4:
                        newInjuries.add(gen.apply(InjuryTypes.PUNCTURED_LUNG, 1));
                        break;
                    default:
                        newInjuries.add(gen.apply(InjuryTypes.BROKEN_BACK, 1));
                        if (Compute.randomInt(100) < 15) {
                            newInjuries.add(gen.apply(InjuryTypes.SEVERED_SPINE, 1));
                        }
                        break;
                }
                break;
            case ABDOMEN:
                switch (hits) {
                    case 1:
                        newInjuries.add(gen.apply(InjuryTypes.PUNCTURE, 1));
                        break;
                    case 2:
                        newInjuries.add(gen.apply(InjuryTypes.BRUISED_KIDNEY, 1));
                        break;
                    default:
                        newInjuries.add(gen.apply(InjuryTypes.INTERNAL_BLEEDING, hits - 2));
                        break;
                }
                break;
            case GENERIC:
            case INTERNAL:
                // AM doesn't deal with those
                break;
            default:
                break;
        }
        return newInjuries;
    }

    /**
     * Generates healing time for an existing injury.
     *
     * <p>This is a convenience method that extracts the injury type and severity from the injury object and
     * delegates to {@link #genHealingTime(Campaign, Person, InjuryType, int)}.
     *
     * @param campaign the current campaign
     * @param person   the person who is injured
     * @param injury   the injury to calculate healing time for
     *
     * @return calculated healing time in days (minimum 1)
     */
    public static int genHealingTime(Campaign campaign, Person person, Injury injury) {
        return genHealingTime(campaign, person, injury.getType(), injury.getHits());
    }

    /**
     * Generates healing time for an injury with random variation and personal modifiers.
     *
     * <p>The healing time is calculated by:</p>
     *
     * <ol>
     *   <li>Getting the base recovery time from the injury type and severity</li>
     *   <li>Adding d6 extra days for lacerations</li>
     *   <li>Applying random variation of ±20% (80-120% of base time)</li>
     *   <li>Applying the person's ability time modifier</li>
     * </ol>
     *
     * <p>The result is always at least 1 day.</p>
     *
     * @param campaign   the current campaign
     * @param person     the person who is injured
     * @param injuryType the type of injury sustained
     * @param severity   the severity level of the injury (used for calculating base time)
     *
     * @return calculated healing time in days (minimum 1)
     */
    public static int genHealingTime(Campaign campaign, Person person, InjuryType injuryType, int severity) {
        int baseTime = injuryType.getRecoveryTime(severity);

        // Add extra time for lacerations
        if (injuryType == InjuryTypes.LACERATION) {
            baseTime += Compute.d6();
        }

        // Apply random variation: 80-120% (±20%)
        int variationPercent = 80 + Compute.randomInt(41); // 80 to 120

        // Apply both random variation and person's ability modifier
        int time = (int) Math.round((baseTime * variationPercent * person.getAbilityTimeModifier(campaign)) / 10000.0);

        return Math.max(1, time);
    }

    /** Generate the effects of a doctor dealing with injuries (frequency depends on campaign settings) */
    public static List<GameEffect> genMedicalTreatment(Campaign c, Person p, Person doc) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(p);
        Skill skill = doc.getSkill(SkillType.S_SURGERY);
        int level = skill.getLevel();
        final int fumbleLimit = FUMBLE_LIMITS[(level >= 0) && (level <= 10) ? level : 0];
        final int critLimit = CRIT_LIMITS[(level >= 0) && (level <= 10) ? level : 0];
        int xpGained = 0;
        int mistakeXP = 0;
        int successXP = 0;
        int numTreated = 0;
        int numResting = 0;

        List<GameEffect> result = new ArrayList<>();

        for (Injury i : p.getInjuries()) {
            if (!i.isWorkedOn()) {
                int roll = Compute.randomInt(100);
                // Determine XP, if any
                if (roll < Math.max(1, fumbleLimit / 10)) {
                    mistakeXP += c.getCampaignOptions().getMistakeXP();
                    xpGained += mistakeXP;
                } else if (roll > Math.min(98, 99 - (int) Math.round((99 - critLimit) / 10.0))) {
                    successXP += c.getCampaignOptions().getSuccessXP();
                    xpGained += successXP;
                }
                final int critTimeReduction = i.getTime() - (int) Math.floor(i.getTime() * 0.9);
                // Reroll fumbled treatment check with Edge if applicable
                if (c.getCampaignOptions().isUseSupportEdge() &&
                          (roll < fumbleLimit) &&
                          doc.getOptions().booleanOption(PersonnelOptions.EDGE_MEDICAL) &&
                          (doc.getCurrentEdge() > 0)) {
                    result.add(new GameEffect(String.format(
                          "%s made a mistake in the treatment of %s, but used Edge to reroll.",
                          doc.getHyperlinkedFullTitle(),
                          p.getHyperlinkedName())));
                    doc.changeCurrentEdge(-1);
                    roll = Compute.randomInt(100);
                }

                if (roll < fumbleLimit) {
                    result.add(new GameEffect(String.format(
                          "%s made a mistake in the treatment of %s and caused %s %s to worsen.",
                          doc.getHyperlinkedFullTitle(),
                          p.getHyperlinkedName(),
                          GenderDescriptors.HIS_HER_THEIR.getDescriptor(p.getGender()),
                          i.getName()), rnd -> {
                        int time = i.getTime();
                        i.setTime((int) Math.max(Math.ceil(time * 1.2), time + 5));
                        MedicalLogger.docMadeAMistake(doc, p, i, c.getLocalDate());

                        // TODO: Add in special handling of the critical
                        //if (rnd.applyAsInt(100) < (fumbleLimit / 4)) {
                        // injuries like broken back (make perm),
                        // broken ribs (punctured lung/death chance) internal
                        // bleeding (death chance)
                        //}
                    }));
                } else if ((roll > critLimit) && (critTimeReduction > 0)) {
                    result.add(new GameEffect(String.format(
                          "%s performed some amazing work in treating %s of %s (%d fewer day(s) to heal).",
                          doc.getHyperlinkedFullTitle(),
                          i.getName(),
                          p.getHyperlinkedName(),
                          critTimeReduction), rnd -> {
                        i.setTime(i.getTime() - critTimeReduction);
                        MedicalLogger.docAmazingWork(doc, p, i, c.getLocalDate(), critTimeReduction);
                    }));
                } else {
                    final int xpChance = (int) Math.round(100.0 / c.getCampaignOptions().getNTasksXP());
                    result.add(new GameEffect(String.format("%s successfully treated %s [%d%% chance of gaining %d XP]",
                          doc.getHyperlinkedFullTitle(),
                          p.getHyperlinkedName(),
                          xpChance,
                          c.getCampaignOptions().getTaskXP()), rnd -> {
                        int taskXP = c.getCampaignOptions().getTaskXP();
                        if ((taskXP > 0) && (doc.getNTasks() >= c.getCampaignOptions().getNTasksXP())) {
                            doc.awardXP(c, taskXP);
                            doc.setNTasks(0);
                        } else {
                            doc.setNTasks(doc.getNTasks() + 1);
                        }
                        i.setWorkedOn(true);
                        MedicalLogger.successfullyTreated(doc, p, c.getLocalDate(), i);
                        Unit u = p.getUnit();
                        if (null != u) {
                            u.resetPilotAndEntity();
                        }
                    }));
                }
                i.setWorkedOn(true);
                Unit u = p.getUnit();
                if (null != u) {
                    u.resetPilotAndEntity();
                }
                numTreated++;
            } else {
                result.add(new GameEffect(String.format("%s spent time resting to heal %s %s.",
                      p.getHyperlinkedName(),
                      GenderDescriptors.HIS_HER_THEIR.getDescriptor(p.getGender()),
                      i.getName()), rnd -> {

                }));
                numResting++;
            }
        }
        if (numTreated > 0) {
            final int xp = xpGained;
            final int injuries = numTreated;
            final String treatmentSummary = (xpGained > 0) ?
                                                  String.format("%s successfully treated %s for %d injuries " +
                                                                      "(%d XP gained, %d for mistakes, %d for critical successes, and %d for tasks).",
                                                        doc.getHyperlinkedFullTitle(),
                                                        p.getHyperlinkedName(),
                                                        numTreated,
                                                        xp,
                                                        mistakeXP,
                                                        successXP,
                                                        xp - mistakeXP - successXP) :
                                                  String.format("%s successfully treated %s for %d injuries.",
                                                        doc.getHyperlinkedFullTitle(),
                                                        p.getHyperlinkedName(),
                                                        numTreated);

            result.add(new GameEffect(treatmentSummary, rnd -> {
                if (xp > 0) {
                    doc.awardXP(c, xp);
                }
                PatientLogger.successfullyTreated(doc, p, c.getLocalDate(), injuries);
                p.setDaysToWaitForHealing(c.getCampaignOptions().getHealingWaitingPeriod());
            }));
        }
        if (numResting > 0) {
            result.add(new GameEffect(String.format("%s spent time resting to heal %d injuries.",
                  p.getHyperlinkedName(),
                  numResting)));
        }
        return result;
    }

    /** Generate the effects of "natural" healing (daily) */
    public static List<GameEffect> genNaturalHealing(Campaign c, Person person) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(person);

        List<GameEffect> result = new ArrayList<>();

        person.getInjuries().forEach((i) -> {
            if (i.getTime() <= 1 && !i.isPermanent()) {
                InjuryType type = i.getType();
                if (!i.isWorkedOn() &&
                          ((Objects.equals(type, InjuryTypes.BROKEN_LIMB)) ||
                                 (Objects.equals(type, InjuryTypes.TORN_MUSCLE)) ||
                                 (Objects.equals(type, InjuryTypes.CONCUSSION)) ||
                                 (Objects.equals(type, InjuryTypes.BROKEN_COLLAR_BONE)))) {
                    result.add(new GameEffect(String.format(
                          "83%% chance of %s healing, 17%% chance of it becoming permanent.",
                          i.getName()), rnd -> {
                        i.setTime(0);
                        if (rnd.applyAsInt(6) == 0) {
                            i.setPermanent(true);
                            MedicalLogger.injuryDidntHealProperly(person, c.getLocalDate(), i);
                        } else {
                            person.removeInjury(i);
                            MedicalLogger.injuryHealed(person, c.getLocalDate(), i);
                        }
                    }));
                } else {
                    result.add(new GameEffect(String.format("%s heals", i.getName()), rnd -> {
                        i.setTime(0);
                        person.removeInjury(i);
                        MedicalLogger.injuryHealed(person, c.getLocalDate(), i);
                    }));
                }
            } else if (i.getTime() > 1) {
                result.add(new GameEffect(String.format("%s continues healing", i.getName()),
                      rnd -> i.setTime(Math.max(i.getTime() - 1, 0))));
            } else if ((i.getTime() == 1) && i.isPermanent()) {
                result.add(new GameEffect(String.format("%s becomes permanent", i.getName()), rnd -> {
                    i.setTime(0);
                    MedicalLogger.injuryBecamePermanent(person, c.getLocalDate(), i);
                }));
            }
        });
        if (null != person.getDoctorId()) {
            result.add(new GameEffect("Infirmary health check-up", rnd -> {
                boolean dismissed = false;
                if (person.getStatus().isDead()) {
                    dismissed = true;
                    MedicalLogger.diedInInfirmary(person, c.getLocalDate());
                } else if (person.getStatus().isMIA()) {
                    // What? How?
                    dismissed = true;
                    MedicalLogger.abductedFromInfirmary(person, c.getLocalDate());
                } else if (person.getStatus().isRetired()) {
                    dismissed = true;
                    MedicalLogger.retiredAndTransferredFromInfirmary(person, c.getLocalDate());
                } else if (!person.needsFixing()) {
                    dismissed = true;
                    MedicalLogger.dismissedFromInfirmary(person, c.getLocalDate());
                }

                if (dismissed) {
                    person.setDoctorId(null, c.getCampaignOptions().getHealingWaitingPeriod());
                }
            }));
        }
        return result;
    }

    /** Generate the effects not being under proper treatment (daily) */
    public static List<GameEffect> genUntreatedEffects(Campaign c, Person p) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(p);

        List<GameEffect> result = new ArrayList<>();

        p.getInjuries().forEach((i) -> {
            if ((i.getTime() > 0) && !i.isPermanent() && !i.isWorkedOn()) {
                result.add(new GameEffect(String.format("30%% chance of %s worsening its condition", i.getName()),
                      rnd -> {
                          if (rnd.applyAsInt(100) < 30) {
                              i.setTime(i.getTime() + 1);
                              // TODO: Disabled, too much spam
                          }
                      }));
            }
        });

        return result;
    }
}
