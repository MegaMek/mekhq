/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.annotations.Nullable;
import megamek.common.options.AbstractOptionsInfo;
import megamek.common.options.IBasicOptionGroup;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.IOptionInfo;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.logging.MMLogger;

/**
 * An extension of PilotOptions that adds MekHQ-specific SPAs and edge triggers for support and command actions. Display
 * names and descriptions are taken from SpecialAbility when present, otherwise from the MM option.
 *
 * @author Neoancient
 */
public class PersonnelOptions extends PilotOptions {
    private static final MMLogger LOGGER = MMLogger.create(PersonnelOptions.class);

    public static final String EDGE_MEDICAL = "edge_when_heal_crit_fail";
    public static final String EDGE_REPAIR_BREAK_PART = "edge_when_repair_break_part";
    public static final String EDGE_REPAIR_FAILED_REFIT = "edge_when_fail_refit_check";
    public static final String EDGE_ADMIN_ACQUIRE_FAIL = "edge_when_admin_acquire_fail";

    public static final String TECH_WEAPON_SPECIALIST = "tech_weapon_specialist";
    public static final String TECH_ARMOR_SPECIALIST = "tech_armor_specialist";
    public static final String TECH_INTERNAL_SPECIALIST = "tech_internal_specialist";
    public static final String TECH_ENGINEER = "tech_engineer";
    public static final String TECH_FIXER = "tech_fixer";
    public static final String TECH_MAINTAINER = "tech_maintainer";
    public static final String FLAW_GLASS_JAW = "flaw_glass_jaw";
    public static final String ATOW_TOUGHNESS = "atow_toughness";
    public static final String FLAW_SLOW_LEARNER = "flaw_slow_learner";
    public static final String ATOW_FAST_LEARNER = "atow_fast_learner";
    public static final String ATOW_ALTERNATE_ID = "atow_alternate_id";
    public static final String ATOW_CITIZENSHIP = "atow_citizenship";
    public static final String FLAW_ANIMAL_ANTIPATHY = "flaw_animal_antipathy";
    public static final String ATOW_ANIMAL_EMPATHY = "atow_animal_empathy";
    public static final String ATOW_AMBIDEXTROUS = "atow_ambidextrous";
    public static final String FLAW_UNATTRACTIVE = "flaw_unattractive";
    public static final String ATOW_ATTRACTIVE = "atow_attractive";
    public static final String FLAW_UNFIT = "flaw_unfit";
    public static final String ATOW_FIT = "atow_fit";
    public static final String UNOFFICIAL_GHOST = "unofficial_ghost";
    public static final String UNOFFICIAL_LOUD_MOUTH = "unofficial_loud_mouth";
    public static final String UNOFFICIAL_RANGER = "unofficial_ranger";
    public static final String UNOFFICIAL_IMPLANT_RESISTANCE = "unofficial_implant_resistance";
    public static final String FLAW_POOR_HEARING = "flaw_poor_hearing";
    public static final String ATOW_GOOD_HEARING = "atow_good_hearing";
    public static final String FLAW_POOR_VISION = "flaw_poor_vision";
    public static final String ATOW_GOOD_VISION = "atow_good_vision";
    public static final String FLAW_INTROVERT = "flaw_introvert";
    public static final String ATOW_GREGARIOUS = "atow_gregarious";
    public static final String FLAW_IMPATIENT = "flaw_impatient";
    public static final String ATOW_PATIENT = "atow_patient";
    public static final String ATOW_POISON_RESISTANCE = "atow_poison_resistance";
    public static final String ATOW_SIXTH_SENSE = "atow_sixth_sense";
    public static final String FLAW_GREMLINS = "flaw_gremlins";
    public static final String ATOW_TECH_EMPATHY = "atow_tech_empathy";
    public static final String FLAW_TRANSIT_DISORIENTATION_SYNDROME = "flaw_transit_disorientation_syndrome";
    public static final String FLAW_ILLITERATE = "flaw_illiterate";
    public static final String UNOFFICIAL_HOUDINI = "unofficial_houdini";
    public static final String UNOFFICIAL_MASTER_IMPERSONATOR = "unofficial_master_impersonator";
    public static final String UNOFFICIAL_COUNTERFEITER = "unofficial_counterfeiter";
    public static final String UNOFFICIAL_PICK_POCKET = "unofficial_pick_pocket";
    public static final String UNOFFICIAL_NATURAL_THESPIAN = "unofficial_natural_thespian";
    public static final String UNOFFICIAL_BIOLOGICAL_MACHINIST = "unofficial_biological_machinist";

    public static final String DARK_SECRET_TRIVIAL = "dark_secret_trivial";
    public static final String DARK_SECRET_SIGNIFICANT = "dark_secret_significant";
    public static final String DARK_SECRET_MAJOR = "dark_secret_major";
    public static final String DARK_SECRET_SEVERE = "dark_secret_severe";
    public static final String DARK_SECRET_EXTREME = "dark_secret_extreme";

    public static final String MUTATION_FREAKISH_STRENGTH = "mutation_freakish_strength";
    public static final String MUTATION_EXCEPTIONAL_IMMUNE_SYSTEM = "mutation_exceptional_immune_system";
    public static final String MUTATION_EXOTIC_APPEARANCE = "mutation_exotic_appearance";
    public static final String MUTATION_FACIAL_HAIR = "mutation_facial_hair";
    public static final String MUTATION_SERIOUS_DISFIGUREMENT = "mutation_serious_disfigurement";
    public static final String MUTATION_CAT_GIRL = "mutation_cat_girl";
    public static final String MUTATION_CAT_GIRL_UNOFFICIAL = "mutation_cat_girl_unofficial";

    public static final String EXCEPTIONAL_ATTRIBUTE_STRENGTH = "exceptional_attribute_strength";
    public static final String EXCEPTIONAL_ATTRIBUTE_BODY = "exceptional_attribute_body";
    public static final String EXCEPTIONAL_ATTRIBUTE_REFLEXES = "exceptional_attribute_reflexes";
    public static final String EXCEPTIONAL_ATTRIBUTE_DEXTERITY = "exceptional_attribute_dexterity";
    public static final String EXCEPTIONAL_ATTRIBUTE_INTELLIGENCE = "exceptional_attribute_intelligence";
    public static final String EXCEPTIONAL_ATTRIBUTE_WILLPOWER = "exceptional_attribute_willpower";
    public static final String EXCEPTIONAL_ATTRIBUTE_CHARISMA = "exceptional_attribute_charisma";
    public static final String EXCEPTIONAL_ATTRIBUTE_EDGE = "exceptional_attribute_edge";

    public static final String ADMIN_MEDIATOR = "admin_mediator";
    public static final String ADMIN_LOGISTICIAN = "admin_logistician";
    public static final String ADMIN_COORDINATOR = "admin_coordinator";
    public static final String ADMIN_TETRIS_MASTER = "admin_tetris_master";
    public static final String ADMIN_NETWORKER = "admin_networker";
    public static final String ADMIN_INTERSTELLAR_NEGOTIATOR = "admin_interstellar_negotiator";
    public static final String ADMIN_SCROUNGE = "admin_scrounge";

    public static final String COMPULSION_UNPLEASANT_PERSONALITY = "compulsion_unpleasant_personality";
    public static final String COMPULSION_MILD_PARANOIA = "compulsion_mild_paranoia";
    public static final String COMPULSION_RACISM = "compulsion_racism";
    public static final String COMPULSION_RELIGIOUS_FANATICISM = "compulsion_religious_fanaticism";
    public static final String COMPULSION_TRAUMATIC_PAST = "compulsion_traumatic_past";
    public static final String COMPULSION_FACTION_PRIDE = "compulsion_faction_pride";
    public static final String COMPULSION_GAMBLING = "compulsion_gambling";
    public static final String COMPULSION_ANARCHIST = "compulsion_hatred_authority";
    public static final String COMPULSION_FACTION_LOYALTY = "compulsion_faction_loyalty";
    public static final String COMPULSION_PATHOLOGIC_RACISM = "compulsion_pathologic_racism";
    public static final String COMPULSION_XENOPHOBIA = "compulsion_xenophobia";
    public static final String COMPULSION_ADDICTION = "compulsion_addiction";
    public static final String COMPULSION_PAINKILLER_ADDICTION = "compulsion_addiction_painkillers";
    public static final String COMPULSION_BIONIC_HATE = "compulsion_bionic_hate";
    public static final String COMPULSION_BODY_MOD_ADDICTION = "compulsion_body_mod_addiction";

    public static final String MADNESS_FLASHBACKS = "madness_flashbacks";
    public static final String MADNESS_CONFUSION = "madness_confusion";
    public static final String MADNESS_CLINICAL_PARANOIA = "madness_clinical_paranoia";
    public static final String MADNESS_SPLIT_PERSONALITY = "madness_split_personality";
    public static final String MADNESS_CATATONIA = "madness_catatonia";
    public static final String MADNESS_REGRESSION = "madness_regression";
    public static final String MADNESS_HYSTERIA = "madness_hysteria";
    public static final String MADNESS_BERSERKER = "madness_berserker";

    public static final int COMPULSION_CHECK_MODIFIER_TRIVIAL = 0; // ATOW pg 110
    public static final int COMPULSION_CHECK_MODIFIER_SIGNIFICANT = 2; // ATOW pg 110
    public static final int COMPULSION_CHECK_MODIFIER_MAJOR = 4; // ATOW pg 110
    public static final int COMPULSION_CHECK_MODIFIER_SEVERE = 7; // ATOW pg 110
    public static final int COMPULSION_CHECK_MODIFIER_EXTREME = 10; // ATOW pg 110

    public static final int PAINKILLER_COST = 42; // 7 days of codeine, ATOW pg 319

    // ATOW pg 112 (Reputation, Connections)
    public static final Map<String, int[]> DARK_SECRET_MODIFIERS = Map.of(
          DARK_SECRET_TRIVIAL, new int[] { -1, -1 },
          DARK_SECRET_SIGNIFICANT, new int[] { -2, -1 },
          DARK_SECRET_MAJOR, new int[] { -3, -2 },
          DARK_SECRET_SEVERE, new int[] { -4, -2 },
          DARK_SECRET_EXTREME, new int[] { -5, -3 }
    );

    public static final int ILLITERACY_LANGUAGES_THRESHOLD = 4; // ATOW pg 120

    @Override
    public void initialize() {
        super.initialize();

        IBasicOptionGroup l3a = null;
        IBasicOptionGroup edge = null;
        IBasicOptionGroup md = null;
        for (Enumeration<IBasicOptionGroup> e = getOptionsInfoImp().getGroups(); e.hasMoreElements(); ) {
            final IBasicOptionGroup group = e.nextElement();
            if ((null == l3a) && group.getKey().equals(PilotOptions.LVL3_ADVANTAGES)) {
                l3a = group;
            } else if ((null == edge) && group.getKey().equals(PilotOptions.EDGE_ADVANTAGES)) {
                edge = group;
            } else if ((null == md) && group.getKey().equals(PilotOptions.MD_ADVANTAGES)) {
                md = group;
            }
        }

        if (null == l3a) {
            // This really shouldn't happen.
            LOGGER.warn("Could not find L3Advantage group");
            l3a = addGroup("adv", PilotOptions.LVL3_ADVANTAGES);
        }
        if (null == edge) {
            // This really shouldn't happen.
            LOGGER.warn("Could not find edge group");
            edge = addGroup("edge", PilotOptions.EDGE_ADVANTAGES);
            addOption(edge, OptionsConstants.EDGE, 0);
        }
        if (null == md) {
            // This really shouldn't happen.
            LOGGER.warn("Could not find augmentation (MD) group");
            md = addGroup("md", PilotOptions.MD_ADVANTAGES);
        }

        // Add MekHQ-specific options
        addOption(l3a, TECH_WEAPON_SPECIALIST, false);
        addOption(l3a, TECH_ARMOR_SPECIALIST, false);
        addOption(l3a, TECH_INTERNAL_SPECIALIST, false);
        addOption(l3a, TECH_ENGINEER, false);
        addOption(l3a, TECH_FIXER, false);
        addOption(l3a, TECH_MAINTAINER, false);
        addOption(l3a, FLAW_GLASS_JAW, false);
        addOption(l3a, ATOW_TOUGHNESS, false);
        addOption(l3a, FLAW_SLOW_LEARNER, false);
        addOption(l3a, ATOW_FAST_LEARNER, false);
        addOption(l3a, ATOW_ALTERNATE_ID, false);
        addOption(l3a, ATOW_CITIZENSHIP, false);
        addOption(l3a, FLAW_ANIMAL_ANTIPATHY, false);
        addOption(l3a, ATOW_ANIMAL_EMPATHY, false);
        addOption(l3a, ATOW_AMBIDEXTROUS, false);
        addOption(l3a, FLAW_UNATTRACTIVE, false);
        addOption(l3a, ATOW_ATTRACTIVE, false);
        addOption(l3a, FLAW_UNFIT, false);
        addOption(l3a, ATOW_FIT, false);
        addOption(l3a, UNOFFICIAL_GHOST, false);
        addOption(l3a, UNOFFICIAL_LOUD_MOUTH, false);
        addOption(l3a, UNOFFICIAL_RANGER, false);
        addOption(l3a, FLAW_POOR_HEARING, false);
        addOption(l3a, ATOW_GOOD_HEARING, false);
        addOption(l3a, FLAW_POOR_VISION, false);
        addOption(l3a, ATOW_GOOD_VISION, false);
        addOption(l3a, FLAW_INTROVERT, false);
        addOption(l3a, ATOW_GREGARIOUS, false);
        addOption(l3a, FLAW_IMPATIENT, false);
        addOption(l3a, ATOW_PATIENT, false);
        addOption(l3a, ATOW_POISON_RESISTANCE, false);
        addOption(l3a, ATOW_SIXTH_SENSE, false);
        addOption(l3a, FLAW_GREMLINS, false);
        addOption(l3a, ATOW_TECH_EMPATHY, false);
        addOption(l3a, FLAW_TRANSIT_DISORIENTATION_SYNDROME, false);
        addOption(l3a, FLAW_ILLITERATE, false);
        addOption(l3a, UNOFFICIAL_HOUDINI, false);
        addOption(l3a, UNOFFICIAL_MASTER_IMPERSONATOR, false);
        addOption(l3a, UNOFFICIAL_COUNTERFEITER, false);
        addOption(l3a, UNOFFICIAL_NATURAL_THESPIAN, false);
        addOption(l3a, UNOFFICIAL_BIOLOGICAL_MACHINIST, false);
        addOption(l3a, UNOFFICIAL_PICK_POCKET, false);

        addOption(l3a, DARK_SECRET_TRIVIAL, false);
        addOption(l3a, DARK_SECRET_SIGNIFICANT, false);
        addOption(l3a, DARK_SECRET_MAJOR, false);
        addOption(l3a, DARK_SECRET_SEVERE, false);
        addOption(l3a, DARK_SECRET_EXTREME, false);

        addOption(l3a, MUTATION_FREAKISH_STRENGTH, false);
        addOption(l3a, MUTATION_EXCEPTIONAL_IMMUNE_SYSTEM, false);
        addOption(l3a, MUTATION_EXOTIC_APPEARANCE, false);
        addOption(l3a, MUTATION_FACIAL_HAIR, false);
        addOption(l3a, MUTATION_SERIOUS_DISFIGUREMENT, false);
        addOption(l3a, MUTATION_CAT_GIRL, false);
        addOption(l3a, MUTATION_CAT_GIRL_UNOFFICIAL, false);

        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_STRENGTH, false);
        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_BODY, false);
        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_REFLEXES, false);
        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_DEXTERITY, false);
        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_INTELLIGENCE, false);
        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_WILLPOWER, false);
        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_CHARISMA, false);
        addOption(l3a, EXCEPTIONAL_ATTRIBUTE_EDGE, false);

        addOption(l3a, ADMIN_MEDIATOR, false);
        addOption(l3a, ADMIN_LOGISTICIAN, false);
        addOption(l3a, ADMIN_COORDINATOR, false);
        addOption(l3a, ADMIN_TETRIS_MASTER, false);
        addOption(l3a, ADMIN_NETWORKER, false);
        addOption(l3a, ADMIN_INTERSTELLAR_NEGOTIATOR, false);
        addOption(l3a, ADMIN_SCROUNGE, false);

        addOption(l3a, COMPULSION_UNPLEASANT_PERSONALITY, false);
        addOption(l3a, COMPULSION_MILD_PARANOIA, false);
        addOption(l3a, COMPULSION_RACISM, false);
        addOption(l3a, COMPULSION_RELIGIOUS_FANATICISM, false);
        addOption(l3a, COMPULSION_TRAUMATIC_PAST, false);
        addOption(l3a, COMPULSION_FACTION_PRIDE, false);
        addOption(l3a, COMPULSION_GAMBLING, false);
        addOption(l3a, COMPULSION_ANARCHIST, false);
        addOption(l3a, COMPULSION_FACTION_LOYALTY, false);
        addOption(l3a, COMPULSION_PATHOLOGIC_RACISM, false);
        addOption(l3a, COMPULSION_XENOPHOBIA, false);
        addOption(l3a, COMPULSION_ADDICTION, false);
        addOption(l3a, COMPULSION_PAINKILLER_ADDICTION, false);
        addOption(l3a, COMPULSION_BIONIC_HATE, false);
        addOption(l3a, COMPULSION_BODY_MOD_ADDICTION, false);

        addOption(l3a, MADNESS_FLASHBACKS, false);
        addOption(l3a, MADNESS_CONFUSION, false);
        addOption(l3a, MADNESS_CLINICAL_PARANOIA, false);
        addOption(l3a, MADNESS_SPLIT_PERSONALITY, false);
        addOption(l3a, MADNESS_CATATONIA, false);
        addOption(l3a, MADNESS_REGRESSION, false);
        addOption(l3a, MADNESS_HYSTERIA, false);
        addOption(l3a, MADNESS_BERSERKER, false);

        addOption(edge, EDGE_MEDICAL, true);
        addOption(edge, EDGE_REPAIR_BREAK_PART, true);
        addOption(edge, EDGE_REPAIR_FAILED_REFIT, true);
        addOption(edge, EDGE_ADMIN_ACQUIRE_FAIL, true);

        List<CustomOption> customs = CustomOption.getCustomAbilities();
        for (CustomOption option : customs) {
            switch (option.getGroup()) {
                case PilotOptions.LVL3_ADVANTAGES:
                    addOption(l3a, option.getName(), option.getType(), option.getDefault());
                    break;
                case PilotOptions.EDGE_ADVANTAGES:
                    addOption(edge, option.getName(), option.getType(), option.getDefault());
                    break;
                case PilotOptions.MD_ADVANTAGES:
                    addOption(md, option.getName(), option.getType(), option.getDefault());
                    break;
                default:
                    throw new IllegalStateException(
                          "Unexpected value in mekhq/campaign/personnel/PersonnelOptions.java/initialize: " +
                                option.getGroup());
            }
        }
    }

    /*
     * When an option is added we need to create a custom IOptionInfo instance so we
     * can
     * provide a different source for display name and description.
     */
    @Override
    protected void addOption(IBasicOptionGroup group, String name, int type, Object defaultValue) {
        super.addOption(group, name, type, defaultValue);
        ((PersonnelOptionsInfo) getOptionsInfoImp()).setOptionInfo(name);
    }

    /**
     * Returns the options of the given category that this pilot has
     */
    public Enumeration<IOption> getOptions(String grpKey) {
        for (Enumeration<IOptionGroup> i = getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(grpKey)) {
                return group.getOptions();
            }
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
    }

    public void acquireAbility(final String type, final String name, final @Nullable Object value) {
        if (value == null) {
            return;
        }
        // we might also need to remove some prior abilities
        SpecialAbility spa = SpecialAbility.getAbility(name);
        Vector<String> toRemove = new Vector<>();
        if (null != spa) {
            toRemove = spa.getRemovedAbilities();
        }
        for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(value);
            } else {
                for (String remove : toRemove) {
                    if (ability.getName().equals(remove)) {
                        ability.setValue(ability.getDefault());
                    }
                }
            }
        }
    }

    /**
     * Returns the check modifier associated with a specific compulsion or mental state.
     *
     * <p>This method maps a given compulsion or madness name to its corresponding check modifier, representing the
     * impact of various psychological traits or conditions on compulsion-related rolls. The modifier value reflects the
     * severity of the condition, ranging from trivial to extreme.</p>
     *
     * @param name the name of the compulsion or mental state for which to retrieve the check modifier
     *
     * @return the {@link Integer} value representing the check modifier for the specified state
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static int getCompulsionCheckModifier(String name) {
        return switch (name) {
            case COMPULSION_ADDICTION -> COMPULSION_CHECK_MODIFIER_SIGNIFICANT;
            case MADNESS_FLASHBACKS, MADNESS_CONFUSION, MADNESS_CLINICAL_PARANOIA, MADNESS_SPLIT_PERSONALITY ->
                  COMPULSION_CHECK_MODIFIER_MAJOR;
            case MADNESS_REGRESSION, MADNESS_HYSTERIA -> COMPULSION_CHECK_MODIFIER_SEVERE;
            case MADNESS_BERSERKER, MADNESS_CATATONIA -> COMPULSION_CHECK_MODIFIER_EXTREME;
            default -> {
                LOGGER.warn("Unexpected compulsion name provided: {}", name);
                yield COMPULSION_CHECK_MODIFIER_TRIVIAL;
            }
        };
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PersonnelOptionsInfo.getInstance();
    }

    /**
     * Custom IOptionsInfo class that allows adding additional options to the base MegaMek options before finalizing and
     * also holds a hash of IOptionInfo objects for the abilities so we can provide names and descriptions for the
     * MekHQ-specific options.
     *
     * @author Neoancient
     */
    private static class PersonnelOptionsInfo extends AbstractOptionsInfo {
        private static boolean initialized = false;
        private static final AbstractOptionsInfo instance = new PersonnelOptionsInfo();

        private final Hashtable<String, IOptionInfo> optionsHash = new Hashtable<>();

        public static AbstractOptionsInfo getInstance() {
            if (!initialized) {
                initialized = true;
                // Create a new dummy PilotOptions; ensures values initialized
                // Otherwise, could have issues when loading saved games
                new PersonnelOptions();
            }
            return instance;
        }

        protected PersonnelOptionsInfo() {
            super("PersonnelOptionsInfo");
        }

        @Override
        public IOptionInfo getOptionInfo(String name) {
            return optionsHash.get(name);
        }

        private void setOptionInfo(String name) {
            optionsHash.put(name, new PersonnelOptionInfo(name));
        }
    }

    /**
     * Access to ability names and descriptions from <code>SpecialAbility</code> if the ability has an entry, otherwise
     * checks for the ability the MM PilotOptions class. If not found in either place, returns the lookup key instead.
     *
     * @author Neoancient
     */
    private record PersonnelOptionInfo(String name) implements IOptionInfo {
        private static final PilotOptions mmOptions = new PilotOptions();

        @Override
        public String getDisplayableName() {
            SpecialAbility spa = SpecialAbility.getOption(name);
            if (null != spa) {
                return spa.getDisplayName();
            } else if (null != mmOptions.getOption(name)) {
                return mmOptions.getOption(name).getDisplayableName();
            } else {
                return name;
            }
        }

        @Override
        public String getDisplayableNameWithValue() {
            SpecialAbility spa = SpecialAbility.getOption(name);
            if (null != spa) {
                return spa.getDisplayName();
            } else if (null != mmOptions.getOption(name)) {
                return mmOptions.getOption(name).getDisplayableName();
            } else {
                return name;
            }
        }

        @Override
        public String getDescription() {
            SpecialAbility spa = SpecialAbility.getOption(name);
            if (null != spa) {
                return spa.getDescription();
            } else if (null != mmOptions.getOption(name)) {
                return mmOptions.getOption(name).getDescription();
            } else {
                return name;
            }
        }

        @Override
        public int getTextFieldLength() {
            return 3;
        }

        @Override
        public boolean isLabelBeforeTextField() {
            return false;
        }
    }
}
