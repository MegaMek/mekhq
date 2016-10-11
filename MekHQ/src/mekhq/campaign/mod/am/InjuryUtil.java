/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mod.am;

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

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Mech;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * Static helper methods implementing the "advanced medical" sub-system
 */
public final class InjuryUtil {
    // Fumble and critical success limits for doctor skills levels 0-10, on a d100
    private static final int FUMBLE_LIMITS[] = {50, 40, 30, 20, 12, 6, 5, 4, 3, 2, 1};
    private static final int CRIT_LIMITS[] = {98, 97, 94, 89, 84, 79, 74, 69, 64, 59, 49};
    
    /*
    private static AMEventHandler eventHandler = null;
    
    public synchronized static void registerEventHandler(Campaign c) {
        if(null != eventHandler) {
            MekHQ.EVENT_BUS.unregister(eventHandler);
        }
        MekHQ.EVENT_BUS.register(eventHandler = new AMEventHandler(c));
    }
    */
    
    /** Run a daily healing check */
    public static void resolveDailyHealing(Campaign c, Person p) {
        Person doc = c.getPerson(p.getDoctorId());
        // TODO: Reporting
        if((null != doc) && doc.isDoctor()) {
            if(p.getDaysToWaitForHealing() <= 0) {
                genMedicalTreatment(c, p, doc).stream()
                    .peek(ef -> MekHQ.logMessage(ef.toString())).forEach(GameEffect::apply);
            }
        } else {
            genUntreatedEffects(c, p).stream()
                .peek(ef -> MekHQ.logMessage(ef.toString())).forEach(GameEffect::apply);
        }
        genNaturalHealing(c, p).stream()
            .peek(ef -> MekHQ.logMessage(ef.toString())).forEach(GameEffect::apply);
    }

    /** Resolve injury modifications in case of entering combat with active ones */
    public static void resolveAfterCombat(Campaign c, Person p, int hits) {
        // Gather all the injury actions resulting from the combat situation
        final List<GameEffect> effects = new ArrayList<>();
        p.getInjuries().forEach(i ->
        {
            effects.addAll(i.getType().genStressEffect(c, p, i, hits));
        });
        
        // We could do some fancy display-to-the-user thing here, but for now just resolve all actions
        effects.stream()
            .peek(ef -> MekHQ.logMessage(ef.toString())).forEach(GameEffect::apply);
    }
    
    /** Resolve effects of damage suffered during combat */
    public static void resolveCombatDamage(Campaign c, Person person, int hits) {
        Collection<Injury> newInjuries = genInjuries(c, person, hits);
        newInjuries.forEach((inj) -> person.addInjury(inj));
        if (newInjuries.size() > 0) {
            StringBuilder sb = new StringBuilder("Returned from combat with the following new injuries:");
            newInjuries.forEach((inj) -> sb.append("\n\t\t").append(inj.getFluff()));
            person.addLogEntry(c.getDate(), sb.toString());
        }
    }
    
    private static void addHitToAccumulator(Map<BodyLocation, Integer> acc, BodyLocation loc) {
        if(!acc.containsKey(loc)) {
            acc.put(loc, Integer.valueOf(1));
        } else {
            acc.put(loc, acc.get(loc) + 1);
        }
    }
    
    // Generator methods. Those don't change the state of the person.
    
    /** Generate combat injuries spread through the whole body */
    public static Collection<Injury> genInjuries(Campaign c, Person p, int hits) {
        final Unit u = c.getUnit(p.getUnitId());
        final Entity en = (null != u) ? u.getEntity() : null;
        final boolean mwasf = (null != en) && ((en instanceof Mech) || (en instanceof Aero));
        final int critMod = mwasf ? 0 : 2;
        final BiFunction<IntUnaryOperator, Function<BodyLocation, Boolean>, BodyLocation> generator
            = mwasf ? HitLocationGen::mechAndAsf : HitLocationGen::generic;
        final Map<BodyLocation, Integer> hitAccumulator = new HashMap<>();
        
        for (int i = 0; i < hits; i++) {
            BodyLocation location
                = generator.apply(Compute::randomInt, (loc) -> !p.isLocationMissing(loc));

            // apply hit here
            addHitToAccumulator(hitAccumulator, location);
            // critical hits add to the amount
            int roll = Compute.d6(2);
            if(roll + hits + critMod > 12) {
                addHitToAccumulator(hitAccumulator, location);
            }
        }
        List<Injury> newInjuries = new ArrayList<>();
        for(Entry<BodyLocation, Integer> accEntry : hitAccumulator.entrySet()) {
            newInjuries.addAll(genInjuries(c, p, accEntry.getKey(), accEntry.getValue().intValue()));
        }
        return newInjuries;
    }

    /** Generate combat injuries for a specific body location */
    public static Collection<Injury> genInjuries(Campaign c, Person p, BodyLocation loc, int hits) {
        List<Injury> newInjuries = new ArrayList<Injury>();
        final BiFunction<InjuryType, Integer, Injury> gen = (it, severity) -> {
            return it.newInjury(c, p, loc, severity);
        };
        
        switch(loc) {
            case LEFT_ARM: case LEFT_HAND: case LEFT_LEG: case LEFT_FOOT:
            case RIGHT_ARM: case RIGHT_HAND: case RIGHT_LEG: case RIGHT_FOOT:
                switch(hits) {
                    case 1:
                        newInjuries.add(gen.apply(
                            Compute.randomInt(2) == 0 ? InjuryTypes.CUT : InjuryTypes.BRUISE, 1));
                        break;
                    case 2:
                        newInjuries.add(gen.apply(InjuryTypes.SPRAIN, 1));
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
                switch(hits) {
                    case 1:
                        newInjuries.add(gen.apply(InjuryTypes.LACERATION, 1));
                        break;
                    case 2: case 3:
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
                switch(hits) {
                    case 1:
                        newInjuries.add(gen.apply(
                            Compute.randomInt(2) == 0 ? InjuryTypes.CUT : InjuryTypes.BRUISE, 1));
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
                        if(Compute.randomInt(100) < 15) {
                            newInjuries.add(gen.apply(InjuryTypes.SEVERED_SPINE, 1));
                        }
                        break;
                }
                break;
            case ABDOMEN:
                switch(hits) {
                    case 1:
                        newInjuries.add(gen.apply(
                            Compute.randomInt(2) == 0 ? InjuryTypes.CUT : InjuryTypes.BRUISE, 1));
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
    
    /** Called when creating a new injury to generate a slightly randomized healing time */
    public static int genHealingTime(Campaign c, Person p, Injury i) {
        return genHealingTime(c, p, i.getType(), i.getHits());
    }
    
    /** Called when creating a new injury to generate a slightly randomized healing time */
    public static int genHealingTime(Campaign c, Person p, InjuryType itype, int severity) {
        int mod = 100;
        int rand = Compute.randomInt(100);
        if(rand < 5) {
            mod += (Compute.d6() < 4) ? rand : -rand;
        }
        
        int time = itype.getRecoveryTime(severity);
        if(itype == InjuryTypes.LACERATION) {
            time += Compute.d6();
        }

        time = Math.round(time * mod * p.getAbilityTimeModifier() / 10000);
        return time;
    }
    
    /** Generate the effects of a doctor dealing with injuries (frequency depends on campaign settings) */
    public static List<GameEffect> genMedicalTreatment(Campaign c, Person p, Person doc) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(p);
        Skill skill = doc.getSkill(SkillType.S_DOCTOR);
        int level = skill.getLevel();
        int roll = Compute.randomInt(100);
        final int fumbleLimit = FUMBLE_LIMITS[(level >= 0) && (level <= 10) ? level : 0];
        final int critLimt = CRIT_LIMITS[(level >= 0) && (level <= 10) ? level : 0];
        int xpGained = 0;
        int mistakeXP = 0;
        int successXP = 0;
        int numTreated = 0;
        int numResting = 0;
        
        List<GameEffect> result = new ArrayList<>();

        // Determine XP, if any
        if (roll < Math.max(1, fumbleLimit / 10)) {
            mistakeXP += c.getCampaignOptions().getMistakeXP();
            xpGained += mistakeXP;
        } else if (roll > Math.min(98, 99 - Math.round(99 - critLimt) / 10)) {
            successXP += c.getCampaignOptions().getSuccessXP();
            xpGained += successXP;
        }

        for(Injury i : p.getInjuries()) {
            if(!i.isWorkedOn()) {
                final int critTimeReduction = i.getTime() - (int) Math.floor(i.getTime() * 0.9);
                if(roll < fumbleLimit) {
                    result.add(new GameEffect(
                        String.format("%s made a mistake in the treatment of %s and caused %s %s to worsen.",
                            doc.getHyperlinkedFullTitle(), p.getHyperlinkedName(),
                            p.getGenderPronoun(Person.PRONOUN_HISHER), i.getName()),
                        rnd -> {
                        int time = i.getTime();
                        i.setTime((int) Math.max(Math.ceil(time * 1.2), time + 5));
                        if(rnd.applyAsInt(100) < (fumbleLimit / 4)) {
                            // TODO: Add in special handling of the critical
                            // injuries like broken back (make perm),
                            // broken ribs (punctured lung/death chance) internal
                            // bleeding (death chance)
                        }
                    }));
                } else if((roll > critLimt) && (critTimeReduction > 0)) {
                    result.add(new GameEffect(
                        String.format("%s performed some amazing work in treating %s of %s (%d fewer day(s) to heal)",
                            doc.getHyperlinkedFullTitle(), i.getName(), p.getHyperlinkedName(), critTimeReduction),
                        rnd -> {
                            i.setTime(i.getTime() - critTimeReduction);
                        }));
                } else {
                    final int xpChance = (int) Math.round(100.0 / c.getCampaignOptions().getNTasksXP());
                    result.add(new GameEffect(
                        String.format("%s successfully treated %s [%d%% chance of gaining %d XP]",
                            doc.getHyperlinkedFullTitle(), p.getHyperlinkedName(),
                            xpChance, c.getCampaignOptions().getTaskXP()),
                        rnd -> {
                            if(doc.getNTasks() >= c.getCampaignOptions().getNTasksXP()) {
                                doc.setXp(doc.getXp() + c.getCampaignOptions().getTaskXP());
                                doc.setNTasks(0);
                            } else {
                                doc.setNTasks(doc.getNTasks() + 1);
                            }
                            i.setWorkedOn(true);
                            Unit u = c.getUnit(p.getUnitId());
                            if(null != u) {
                                u.resetPilotAndEntity();
                            }
                        }));
                }
                i.setWorkedOn(true);
                Unit u = c.getUnit(p.getUnitId());
                if(null != u) {
                    u.resetPilotAndEntity();
                }
                numTreated++;
            } else {
                result.add(new GameEffect(
                    String.format("%s spent time resting to heal %s %s.",
                        p.getHyperlinkedName(), p.getGenderPronoun(Person.PRONOUN_HISHER), i.getName()),
                    rnd -> {
                        
                    }));
                numResting++;
            }
        }
        if (numTreated > 0) {
            final int xp = xpGained;
            final String treatmentSummary = (xpGained > 0)
                ? String.format("%s successfully treated %s for %d injuries "
                    + "(%d XP gained, %d for mistakes, %d for critical successes, and %d for tasks).",
                    doc.getHyperlinkedFullTitle(), p.getHyperlinkedName(), numTreated,
                    xp, mistakeXP, successXP, xp - mistakeXP - successXP)
                : String.format("%s successfully treated %s for %d injuries.",
                    doc.getHyperlinkedFullTitle(), p.getHyperlinkedName(), numTreated);
                    
            result.add(new GameEffect(treatmentSummary,
                rnd -> { doc.setXp(doc.getXp() + xp); }));
        }
        if (numResting > 0) {
            result.add(new GameEffect(
                String.format("%s spent time resting to heal %d injuries.",
                    p.getHyperlinkedName(), numResting)));
        }
        return result;
    }
    
    /** Generate the effects of "natural" healing (daily) */
    public static List<GameEffect> genNaturalHealing(Campaign c, Person p) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(p);
        
        List<GameEffect> result = new ArrayList<>();

        p.getInjuries().forEach((i) -> {
            if(i.getTime() <= 1 && !i.isPermanent()) {
                InjuryType type = i.getType();
                if(!i.isWorkedOn() &&
                    ((type == InjuryTypes.BROKEN_LIMB) || (type == InjuryTypes.SPRAIN)
                    || (type == InjuryTypes.CONCUSSION) || (type == InjuryTypes.BROKEN_COLLAR_BONE))) {
                    result.add(new GameEffect(
                        String.format("83%% chance of %s healing, 17%% chance of it becoming permanent.",
                            i.getName()),
                        rnd -> {
                            i.setTime(0);
                            if(rnd.applyAsInt(6) == 0) {
                                i.setPermanent(true);
                                p.addLogEntry(c.getDate(), String.format("%s didn't heal properly", i.getName()));
                            } else {
                                p.removeInjury(i);
                            }
                        }));
                } else {
                    result.add(new GameEffect(
                        String.format("%s heals", i.getName()),
                        rnd -> {
                            i.setTime(0);
                            p.removeInjury(i);
                        }));
                }
            } else if(i.getTime() > 1) {
                result.add(new GameEffect(
                    String.format("%s continues healing", i.getName()),
                    rnd -> {
                        i.setTime(Math.max(i.getTime() - 1, 0));
                    }));
            } else if((i.getTime() == 1) && i.isPermanent()) {
                result.add(new GameEffect(
                    String.format("%s becomes permanent", i.getName()),
                    rnd -> {
                        i.setTime(0);
                    }));
            }
        });
        
        return result;
    }

    /** Generate the effects not being under proper treatment (daily) */
    public static List<GameEffect> genUntreatedEffects(Campaign c, Person p) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(p);
        
        List<GameEffect> result = new ArrayList<>();
        
        p.getInjuries().forEach((i) -> {
            if((i.getTime() > 0) && !i.isPermanent() && !i.isWorkedOn()) {
                result.add(new GameEffect(
                    String.format("30%% chance of %s worsening its condition", i.getName()),
                    rnd -> {
                        if(rnd.applyAsInt(100) < 30) {
                            i.setTime(i.getTime() + 1);
                        }
                    }));
            }
        });
        
        return result;
    }
}
