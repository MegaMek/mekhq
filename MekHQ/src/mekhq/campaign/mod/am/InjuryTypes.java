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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import megamek.common.Compute;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;
import mekhq.campaign.LogEntry;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryLevel;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Modifier;
import mekhq.campaign.personnel.Person;

/** Advanced Medical sub-system injury types */
public final class InjuryTypes {
    // Predefined types
    public static final InjuryType CUT = new InjuryTypes.Cut();
    public static final InjuryType BRUISE = new InjuryTypes.Bruise();
    public static final InjuryType LACERATION = new InjuryTypes.Laceration();
    public static final InjuryType SPRAIN = new InjuryTypes.Sprain();
    public static final InjuryType CONCUSSION = new InjuryTypes.Concussion();
    public static final InjuryType BROKEN_RIB = new InjuryTypes.BrokenRib();
    public static final InjuryType BRUISED_KIDNEY = new InjuryTypes.BruisedKidney();
    public static final InjuryType BROKEN_LIMB = new InjuryTypes.BrokenLimb();
    public static final InjuryType BROKEN_COLLAR_BONE = new InjuryTypes.BrokenCollarBone();
    public static final InjuryType INTERNAL_BLEEDING = new InjuryTypes.InternalBleeding();
    public static final InjuryType LOST_LIMB = new InjuryTypes.LostLimb();
    public static final InjuryType CEREBRAL_CONTUSION = new InjuryTypes.CerebralContusion();
    public static final InjuryType PUNCTURED_LUNG = new InjuryTypes.PuncturedLung();
    public static final InjuryType CTE = new InjuryTypes.Cte();
    public static final InjuryType BROKEN_BACK = new InjuryTypes.BrokenBack();
    // New injury types go here (or extend the class)
    public static final InjuryType SEVERED_SPINE = new InjuryTypes.SeveredSpine();

    private static boolean registered = false;
    
    /** Register all injury types defined here. Don't use them until you called this once! */
    public static synchronized void registerAll() {
        if(!registered) {
            InjuryType.register(0, "am:cut", CUT);
            InjuryType.register(1, "am:bruise", BRUISE);
            InjuryType.register(2, "am:laceration", LACERATION);
            InjuryType.register(3, "am:sprain", SPRAIN);
            InjuryType.register(4, "am:concussion", CONCUSSION);
            InjuryType.register(5, "am:broken_rib", BROKEN_RIB);
            InjuryType.register(6, "am:bruised_kidney", BRUISED_KIDNEY);
            InjuryType.register(7, "am:broken_limb", BROKEN_LIMB);
            InjuryType.register(8, "am:broken_collar_bone", BROKEN_COLLAR_BONE);
            InjuryType.register(9, "am:internal_bleeding", INTERNAL_BLEEDING);
            InjuryType.register(10, "am:lost_limb", LOST_LIMB);
            InjuryType.register(11, "am:cerebral_contusion", CEREBRAL_CONTUSION);
            InjuryType.register(12, "am:punctured_lung", PUNCTURED_LUNG);
            InjuryType.register(13, "am:cte", CTE);
            InjuryType.register(14, "am:broken_back", BROKEN_BACK);
            InjuryType.register("am:severed_spine", SEVERED_SPINE);
            registered = true;
        }
    }
    
    private static class AMInjuryType extends InjuryType {
        protected int modifyInjuryTime(Person p, int time) {
            // Randomize healing time
            int mod = 100;
            int rand = Compute.randomInt(100);
            if(rand < 5) {
                mod += (Compute.d6() < 4) ? rand : -rand;
            }
            return  Math.round(time * mod * p.getAbilityTimeModifier() / 10000);
        }
        
        @Override
        public Injury newInjury(Campaign c, Person p, BodyLocation loc, int severity) {
            Injury result = super.newInjury(c, p, loc, severity);
            final int time = modifyInjuryTime(p, result.getOriginalTime());
            result.setOriginalTime(time);
            result.setTime(time);
            return result;
        }
    }
    
    public static final class SeveredSpine extends AMInjuryType {
        public SeveredSpine() {
            recoveryTime = 180;
            allowedLocations = EnumSet.of(BodyLocation.CHEST, BodyLocation.ABDOMEN);
            permanent = true;
            fluffText = "A severed spine";
            simpleName = "severed spine";
            level = InjuryLevel.CHRONIC;
        }
    
        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            return "A severed spine in " + ((loc == BodyLocation.CHEST) ? "upper" : "lower") + " body";
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Arrays.asList(new Modifier(Modifier.Value.PILOTING, Integer.MAX_VALUE,
                null, InjuryType.MODTAG_INJURY));
        }
    }

    public static final class BrokenBack extends AMInjuryType {
        public BrokenBack() {
            recoveryTime = 150;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A broken back";
            simpleName = "broken back";
            level = InjuryLevel.MAJOR;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            final String METHOD_NAME = "genStressEffect(Campaign,Person,Injury,int"; //$NON-NLS-1$
            
            return Arrays.asList(new GameEffect(
                "20% chance of severing the spine, permanently paralizing the character",
                rnd -> {
                    if(rnd.applyAsInt(100) < 20) {
                        Injury severedSpine = SEVERED_SPINE.newInjury(c, p, BodyLocation.CHEST, 1);
                        p.addInjury(severedSpine);
                        LogEntry entry = new LogEntry(c.getDate(), "Severed " + Person.getGenderPronoun(p.getGender(), Person.PRONOUN_HISHER)
                            + " spine, leaving " + Person.getGenderPronoun(p.getGender(), Person.PRONOUN_HIMHER)
                            + " paralyzed", Person.LOGTYPE_MEDICAL);
                        p.addLogEntry(entry);
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                    }
                }));
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Arrays.asList(
                new Modifier(Modifier.Value.GUNNERY, 3, null, InjuryType.MODTAG_INJURY),
                new Modifier(Modifier.Value.PILOTING, 3, null, InjuryType.MODTAG_INJURY));
        }
    }

    public static final class Cte extends AMInjuryType {
        public Cte() {
            recoveryTime = 180;
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            permanent = true;
            fluffText = "Chronic traumatic encephalopathy";
            simpleName = "CTE";
            level = InjuryLevel.DEADLY;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            final String METHOD_NAME = "genStressEffect(Campaign,Person,Injury,int)"; //$NON-NLS-1$

            int deathchance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
            if(hits > 4) {
                return Arrays.asList(
                    new GameEffect(
                        "certain death",
                        rnd -> {
                            p.setStatus(Person.S_KIA);
                            LogEntry entry = new LogEntry(c.getDate(), "Died due to brain trauma", Person.LOGTYPE_MEDICAL);
                            p.addLogEntry(entry);
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                        }));
            } else {
                // We have a chance!
                return Arrays.asList(
                    newResetRecoveryTimeAction(i),
                    new GameEffect(
                        deathchance + "% chance of death",
                        rnd -> {
                            if(rnd.applyAsInt(6) + hits >= 5) {
                                p.setStatus(Person.S_KIA);
                                LogEntry entry = new LogEntry(c.getDate(), "Died due to brain trauma", Person.LOGTYPE_MEDICAL);
                                p.addLogEntry(entry);
                                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                        }
                    }));
            }
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Arrays.asList(new Modifier(Modifier.Value.PILOTING, Integer.MAX_VALUE, null, InjuryType.MODTAG_INJURY));
        }
    }

    public static final class PuncturedLung extends AMInjuryType {
        public PuncturedLung() {
            recoveryTime = 20;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A punctured lung";
            simpleName = "punctured lung";
            level = InjuryLevel.MAJOR;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Arrays.asList(newResetRecoveryTimeAction(i));
        }
    }

    public static final class CerebralContusion extends AMInjuryType {
        public CerebralContusion() {
            recoveryTime = 90;
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            fluffText = "A cerebral contusion";
            simpleName = "cerebral contusion";
            level = InjuryLevel.MAJOR;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            final String METHOD_NAME = "genStressEffect(Campaign,Person,Injury,int)"; //$NON-NLS-1$

            String secondEffectFluff = "development of a chronic traumatic encephalopathy";
            if(hits < 5) {
                int worseningChance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
                secondEffectFluff = worseningChance + "% chance of " + secondEffectFluff;
            }
            return Arrays.asList(
                newResetRecoveryTimeAction(i),
                new GameEffect(
                    secondEffectFluff,
                    rnd -> {
                        if(rnd.applyAsInt(6) + hits >= 5) {
                            Injury cte = CTE.newInjury(c, p, BodyLocation.HEAD, 1);
                            p.addInjury(cte);
                            p.removeInjury(i);
                            LogEntry entry = new LogEntry(c.getDate(), "Developed a chronic traumatic encephalopathy", Person.LOGTYPE_MEDICAL);
                            p.addLogEntry(entry);
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                        }
                    })
                );
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Arrays.asList(new Modifier(Modifier.Value.PILOTING, 2, null, InjuryType.MODTAG_INJURY));
        }
    }

    public static final class LostLimb extends AMInjuryType {
        public LostLimb() {
            recoveryTime = 28;
            permanent = true;
            simpleName = "lost";
            level = InjuryLevel.CHRONIC;
        }
    
        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb;
        }
        
        @Override
        public boolean impliesMissingLocation(BodyLocation loc) {
            return true;
        }
        
        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Missing " + Utilities.capitalize(loc.readableName);
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            return "Lost " + Person.getGenderPronoun(gender, Person.PRONOUN_HISHER) + " "
                + loc.readableName;
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            BodyLocation loc = inj.getLocation();
            switch(loc) {
                case LEFT_ARM: case LEFT_HAND: case RIGHT_ARM: case RIGHT_HAND:
                    return Arrays.asList(new Modifier(Modifier.Value.GUNNERY, 3, null, InjuryType.MODTAG_INJURY));
                case LEFT_LEG: case LEFT_FOOT: case RIGHT_LEG: case RIGHT_FOOT:
                    return Arrays.asList(new Modifier(Modifier.Value.PILOTING, 3, null, InjuryType.MODTAG_INJURY));
                default:
                    return Arrays.asList();
            }
        }
    }

    public static final class InternalBleeding extends AMInjuryType {
        public InternalBleeding() {
            recoveryTime = 20;
            allowedLocations = EnumSet.of(BodyLocation.ABDOMEN, BodyLocation.INTERNAL);
            maxSeverity = 3;
            simpleName = "internal bleeding";
        }
    
        @Override
        public int getRecoveryTime(int severity) {
            return 20 * severity;
        }
    
        @Override
        public String getName(BodyLocation loc, int severity) {
            return Utilities.capitalize(getFluffText(loc, severity, 0));
        }
        
        @Override
        public InjuryLevel getLevel(Injury i) {
            return (i.getHits() > 2) ? InjuryLevel.DEADLY : InjuryLevel.MAJOR;
        }
        
        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            switch(severity) {
                case 2: return "Severe internal bleeding";
                case 3: return "Critical internal bleeding";
                default: return "Internal bleeding";
            }
        }

        @Override
        public String getSimpleName(int severity) {
            switch(severity) {
                case 2: return "internal bleeding (severe)";
                case 3: return "internal bleeding (critical)";
                default: return "internal bleeding";
            }
        }

        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            final String METHOD_NAME = "genStressEffect(Campaign,Person,Injury,int)"; //$NON-NLS-1$

            String secondEffectFluff = (i.getHits() < 3)
                ? "internal bleeding worsening" : "death";
            if(hits < 5) {
                int worseningChance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
                secondEffectFluff = worseningChance + "% chance of " + secondEffectFluff;
            }
            if(hits >= 5 && i.getHits() >= 3) {
                // Don't even bother doing anything else; we're dead
                return Arrays.asList(
                    new GameEffect(
                        "certain death",
                        rnd -> {
                            p.setStatus(Person.S_KIA);
                            LogEntry entry = new LogEntry(c.getDate(), "Died of critical internal bleeding", Person.LOGTYPE_MEDICAL);
                            p.addLogEntry(entry);
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                        })
                    );
            } else {
                // We have a chance!
                return Arrays.asList(
                    newResetRecoveryTimeAction(i),
                    new GameEffect(
                        secondEffectFluff,
                        rnd -> {
                            if(rnd.applyAsInt(6) + hits >= 5) {
                                if(i.getHits() < 3) {
                                    i.setHits(i.getHits() + 1);
                                    LogEntry entry = new LogEntry(c.getDate(), "Internal bleeding worsened", Person.LOGTYPE_MEDICAL);
                                    p.addLogEntry(entry);
                                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                                } else {
                                    p.setStatus(Person.S_KIA);
                                    LogEntry entry = new LogEntry(c.getDate(), "Died of critical internal bleeding", Person.LOGTYPE_MEDICAL);
                                    p.addLogEntry(entry);
                                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                                }
                            }
                        })
                    );
            }
        }
    }

    public static final class BrokenCollarBone extends AMInjuryType {
        public BrokenCollarBone() {
            recoveryTime = 22;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A broken collar bone";
            simpleName = "broken collar bone";
            level = InjuryLevel.MAJOR;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Arrays.asList(newResetRecoveryTimeAction(i));
        }
    }

    public static final class BrokenLimb extends AMInjuryType {
        public BrokenLimb() {
            recoveryTime = 30;
            simpleName = "broken";
            level = InjuryLevel.MAJOR;
        }
    
        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb;
        }
        
        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Broken " + Utilities.capitalize(loc.readableName);
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            return "A broken " + loc.readableName;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            return Arrays.asList(newResetRecoveryTimeAction(i));
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            BodyLocation loc = inj.getLocation();
            switch(loc) {
                case LEFT_ARM: case LEFT_HAND: case RIGHT_ARM: case RIGHT_HAND:
                    return Arrays.asList(new Modifier(Modifier.Value.GUNNERY, inj.isPermanent() ? 1 : 2,
                        null, InjuryType.MODTAG_INJURY));
                case LEFT_LEG: case LEFT_FOOT: case RIGHT_LEG: case RIGHT_FOOT:
                    return Arrays.asList(new Modifier(Modifier.Value.PILOTING, inj.isPermanent() ? 1 : 2,
                        null, InjuryType.MODTAG_INJURY));
                default:
                    return Arrays.asList();
            }
        }
    }

    public static final class BruisedKidney extends AMInjuryType {
        public BruisedKidney() {
            recoveryTime = 10;
            allowedLocations = EnumSet.of(BodyLocation.ABDOMEN);
            fluffText = "A bruised kidney";
            simpleName = "bruised kidney";
            level = InjuryLevel.MINOR;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            final String METHOD_NAME = "genStressEffect(Campaign,Person,Injury,int)"; //$NON-NLS-1$

            return Arrays.asList(new GameEffect(
                "10% chance of internal bleeding",
                rnd -> {
                    if(rnd.applyAsInt(100) < 10) {
                        Injury bleeding = INTERNAL_BLEEDING.newInjury(c, p, BodyLocation.ABDOMEN, 1);
                        p.addInjury(bleeding);
                        LogEntry entry = new LogEntry(c.getDate(), "Had a broken rib puncturing "
                                + Person.getGenderPronoun(p.getGender(), Person.PRONOUN_HISHER) + " lung",
                            Person.LOGTYPE_MEDICAL);
                        p.addLogEntry(entry);
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                    }
                }));
        }
    }

    public static final class BrokenRib extends AMInjuryType {
        public BrokenRib() {
            recoveryTime = 20;
            allowedLocations = EnumSet.of(BodyLocation.CHEST);
            fluffText = "A broken rib";
            simpleName = "broken rib";
            level = InjuryLevel.MAJOR;
        }
    
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            final String METHOD_NAME = "genStressEffect(Campaign,Person,Injury,int)"; //$NON-NLS-1$

            return Arrays.asList(new GameEffect(
                "1% chance of death; 9% chance of puncturing a lung",
                rnd -> {
                    int rib = rnd.applyAsInt(100);
                    if(rib < 1) {
                        p.changeStatus(Person.S_KIA);
                        LogEntry entry = new LogEntry(c.getDate(), "Had a broken rib puncturing "
                                + Person.getGenderPronoun(p.getGender(), Person.PRONOUN_HISHER) + " heart, dying",
                            Person.LOGTYPE_MEDICAL);
                        p.addLogEntry(entry);
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                    } else if(rib < 10) {
                        Injury puncturedLung = PUNCTURED_LUNG.newInjury(c, p, BodyLocation.CHEST, 1);
                        p.addInjury(puncturedLung);
                        LogEntry entry = new LogEntry(c.getDate(), "Had a broken rib puncturing "
                                + Person.getGenderPronoun(p.getGender(), Person.PRONOUN_HISHER) + " lung",
                            Person.LOGTYPE_MEDICAL);
                        p.addLogEntry(entry);
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                    }
                }));
        }
    }

    public static final class Concussion extends AMInjuryType {
        public Concussion() {
            recoveryTime = 14;
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            maxSeverity = 2;
            fluffText = "A concussion";
        }
    
        @Override
        public int getRecoveryTime(int severity) {
            return severity >= 2 ? 42 : 14;
        }
    
        @Override
        public InjuryLevel getLevel(Injury i) {
            return (i.getHits() > 1) ? InjuryLevel.MAJOR : InjuryLevel.MINOR;
        }
        
        @Override
        public String getSimpleName(int severity) {
            return ((severity == 1) ? "concussion" : "concussion (severe)");
        }
        
        @Override
        public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
            final String METHOD_NAME = "genStressEffect(Campaign,Person,Injury,int)"; //$NON-NLS-1$

            String secondEffectFluff = (i.getHits() == 1)
                ? "concussion worsening" : "development of a cerebral contusion";
            if(hits < 5) {
                int worseningChance = Math.max((int) Math.round((1 + hits) * 100.0 / 6.0), 100);
                secondEffectFluff = worseningChance + "% chance of " + secondEffectFluff;
            }
            return Arrays.asList(
                newResetRecoveryTimeAction(i),
                new GameEffect(
                    secondEffectFluff,
                    rnd -> {
                        if(rnd.applyAsInt(6) + hits >= 5) {
                            if(i.getHits() == 1) {
                                i.setHits(2);
                                LogEntry entry = new LogEntry(c.getDate(), "Concussion worsened", Person.LOGTYPE_MEDICAL);
                                p.addLogEntry(entry);
                                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                            } else {
                                Injury cerebralContusion = CEREBRAL_CONTUSION.newInjury(c, p, BodyLocation.HEAD, 1);
                                p.addInjury(cerebralContusion);
                                p.removeInjury(i);
                                LogEntry entry = new LogEntry(c.getDate(), "Developed a cerebral contusion", Person.LOGTYPE_MEDICAL);
                                p.addLogEntry(entry);
                                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, entry.toString());
                            }
                        }
                    })
                );
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            return Arrays.asList(new Modifier(Modifier.Value.PILOTING, 1, null, InjuryType.MODTAG_INJURY));
        }
    }

    public static final class Sprain extends AMInjuryType {
        public Sprain() {
            recoveryTime = 12;
            simpleName = "sprained";
            level = InjuryLevel.MINOR;
        }
    
        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb;
        }
        
        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Sprained " + Utilities.capitalize(loc.readableName);
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            return "A sprained " + loc.readableName;
        }
        
        @Override
        public Collection<Modifier> getModifiers(Injury inj) {
            BodyLocation loc = inj.getLocation();
            switch(loc) {
                case LEFT_ARM: case LEFT_HAND: case RIGHT_ARM: case RIGHT_HAND:
                    return Arrays.asList(new Modifier(Modifier.Value.GUNNERY, 1, null, InjuryType.MODTAG_INJURY));
                case LEFT_LEG: case LEFT_FOOT: case RIGHT_LEG: case RIGHT_FOOT:
                    return Arrays.asList(new Modifier(Modifier.Value.PILOTING, 1, null, InjuryType.MODTAG_INJURY));
                default:
                    return Arrays.asList();
            }
        }
    }

    public static final class Laceration extends AMInjuryType {
        public Laceration() {
            allowedLocations = EnumSet.of(BodyLocation.HEAD);
            simpleName = "laceration";
            level = InjuryLevel.MINOR;
        }
    
        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Lacerated " + Utilities.capitalize(loc.readableName);
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            return "A laceration on " + Person.getGenderPronoun(gender, Person.PRONOUN_HISHER) + " head";
        }
        
        @Override
        protected int modifyInjuryTime(Person p, int time) {
            return super.modifyInjuryTime(p, time + Compute.d6());
        }
    }

    public static final class Bruise extends AMInjuryType {
        public Bruise() {
            allowedLocations = EnumSet.of(BodyLocation.CHEST, BodyLocation.ABDOMEN);
            simpleName = "bruised";
            level = InjuryLevel.MINOR;
        }
    
        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb || super.isValidInLocation(loc);
        }
        
        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Bruised " + Utilities.capitalize(loc.readableName);
        }

        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            return "A bruise on " + Person.getGenderPronoun(gender, Person.PRONOUN_HISHER) + " "
                + loc.readableName;
        }
        
        @Override
        protected int modifyInjuryTime(Person p, int time) {
            return super.modifyInjuryTime(p, time + Compute.d6());
        }
    }

    public static final class Cut extends AMInjuryType {
        public Cut() {
            allowedLocations = EnumSet.of(BodyLocation.CHEST, BodyLocation.ABDOMEN);
            simpleName = "cut";
            level = InjuryLevel.MINOR;
        }
    
        @Override
        public boolean isValidInLocation(BodyLocation loc) {
            return loc.isLimb || super.isValidInLocation(loc);
        }

        @Override
        public String getName(BodyLocation loc, int severity) {
            return "Cut " + Utilities.capitalize(loc.readableName);
        }
        
        @Override
        public String getFluffText(BodyLocation loc, int severity, int gender) {
            return "Some cuts on " + Person.getGenderPronoun(gender, Person.PRONOUN_HISHER) + " "
                + loc.readableName;
        }
        
        @Override
        protected int modifyInjuryTime(Person p, int time) {
            return super.modifyInjuryTime(p, time + Compute.d6());
        }
    }
}
