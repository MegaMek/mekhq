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
package mekhq.gui.dialog.trainingConsole;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.DANGER;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.client.ui.WrapLayout;
import megamek.codeUtilities.MathUtility;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.training.AutoTrainPlanner;
import mekhq.campaign.personnel.training.AutoTrainPlanner.Skip;
import mekhq.campaign.personnel.training.AutoTrainSettings;
import mekhq.campaign.personnel.training.AutoTrainSettings.Order;
import mekhq.campaign.personnel.training.TrainingPresets;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;

/**
 * The Auto-train page: levels many people's skills by rule, as Quick Train and Mass Training did, with a preview of
 * what it will buy and cost before anything is staged. Settings can be saved per campaign as presets, and a preset can
 * run by itself at the start of each month.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class AutoTrainPage {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;
    private static final int MAX_LEVEL = 10;

    private AutoTrainPage() {
    }

    static List<JComponent> build(TrainingConsole console, List<Person> selection, List<Person> shown) {
        TrainingConsoleState state = console.state();
        if (state.autoTrain == null) {
            state.autoTrain = AutoTrainSettings.defaults("", console.campaign().getCampaignOptions()).toBuilder();
        }
        AutoTrainSettings.Builder draft = state.autoTrain;
        List<Person> scope = selection.isEmpty() ? shown : selection;
        List<JComponent> parts = new ArrayList<>();

        parts.add(presets(console, draft));
        parts.add(leftAligned(ConsoleWidgets.wrapped(selection.isEmpty() ?
                                                           getFormattedTextAt(RESOURCE_BUNDLE, "auto.scope.shown",
                                                                 shown.size()) :
                                                           getFormattedTextAt(RESOURCE_BUNDLE,
                                                                 "auto.scope.selected", selection.size()),
              TEXT_MUTED)));

        // What to train: role skills as Quick Train did, or one skill for everyone who has it, as Mass Training did
        TreeSet<String> heldSkills = console.cached(scopeKey(console, selection, "auto.heldSkills"), () -> {
            TreeSet<String> skills = new TreeSet<>();
            for (Person person : scope) {
                skills.addAll(person.getSkills().getSkillNames());
            }
            return skills;
        });
        JComboBox<String> skillPicker = new JComboBox<>();
        skillPicker.addItem("");
        heldSkills.forEach(skillPicker::addItem);
        skillPicker.setSelectedItem((draft.getSpecificSkill() == null) ? "" : draft.getSpecificSkill());
        Hud.styleComboBox(skillPicker);
        skillPicker.setRenderer(labelled((String value) -> value.isEmpty() ?
                                                        getTextAt(RESOURCE_BUNDLE, "auto.train.roleSkills") :
                                                        value));
        skillPicker.addActionListener(event -> {
            String chosen = (String) skillPicker.getSelectedItem();
            draft.setSpecificSkill(((chosen == null) || chosen.isEmpty()) ? null : chosen);
            console.refreshPanel();
        });
        JPanel train = field(getTextAt(RESOURCE_BUNDLE, "auto.train"), skillPicker);
        if (draft.getSpecificSkill() == null) {
            JPanel options = Hud.transparentPanel(new GridLayout(0, 2, scaleForGUI(8), scaleForGUI(2)));
            options.add(check(console, "auto.option.leadership", draft.isLevelLeadership(), draft::setLevelLeadership));
            options.add(check(console, "auto.option.training", draft.isLevelTraining(), draft::setLevelTraining));
            options.add(check(console, "auto.option.command", draft.isLevelOtherCommand(),
                  draft::setLevelOtherCommand));
            options.add(check(console, "auto.option.artillery", draft.isLevelArtillery(), draft::setLevelArtillery));
            options.add(check(console, "auto.option.scouting", draft.isLevelScouting(), draft::setLevelScouting));
            options.add(check(console, "auto.option.escape", draft.isLevelEscape(), draft::setLevelEscape));
            options.setAlignmentX(Component.LEFT_ALIGNMENT);
            train.add(Box.createVerticalStrut(scaleForGUI(4)));
            train.add(options);
        } else {
            train.add(leftAligned(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "auto.train.oneSkill"), 0.8f,
                  TEXT_FAINT)));
        }
        parts.add(train);

        JComboBox<Integer> targetPicker = new JComboBox<>();
        for (int level = 1; level <= MAX_LEVEL; level++) {
            targetPicker.addItem(level);
        }
        targetPicker.setSelectedItem(draft.getTargetLevel());
        Hud.styleComboBox(targetPicker);
        targetPicker.setRenderer(labelled((Integer value) -> getFormattedTextAt(RESOURCE_BUNDLE,
              "auto.target.level", value)));
        targetPicker.addActionListener(event -> {
            if (targetPicker.getSelectedItem() instanceof Integer level) {
                draft.setTargetLevel(level);
                console.refreshPanel();
            }
        });
        parts.add(field(getTextAt(RESOURCE_BUNDLE, "auto.target"), targetPicker));

        parts.add(field(getTextAt(RESOURCE_BUNDLE, "auto.howFar"), chips(List.of(
              chip(console, getTextAt(RESOURCE_BUNDLE, "auto.howFar.single"), !draft.isContinuous(),
                    () -> draft.setContinuous(false)),
              chip(console, getTextAt(RESOURCE_BUNDLE, "auto.howFar.continuous"), draft.isContinuous(),
                    () -> draft.setContinuous(true))))));

        List<JComponent> orderChips = new ArrayList<>();
        for (Order order : Order.values()) {
            orderChips.add(chip(console, order.getLabel(), draft.getOrder() == order, () -> draft.setOrder(order)));
        }
        parts.add(field(getTextAt(RESOURCE_BUNDLE, "auto.order"), chips(orderChips)));

        JTextField reserve = new JTextField(String.valueOf(draft.getReserveXp()), 6);
        Hud.styleField(reserve);
        Runnable readReserve = () -> {
            int value = Math.max(0, MathUtility.parseInt(reserve.getText().trim(), draft.getReserveXp()));
            if (value != draft.getReserveXp()) {
                draft.setReserveXp(value);
                console.refreshPanel();
            }
        };
        reserve.addActionListener(event -> readReserve.run());
        reserve.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                readReserve.run();
            }
        });
        JPanel reserveRow = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        reserveRow.add(reserve);
        parts.add(field(getTextAt(RESOURCE_BUNDLE, "auto.reserve"), reserveRow));

        JPanel guards = ConsoleWidgets.column();
        guards.add(check(console, "auto.skipTraining", draft.isSkipTrainingFormations(),
              draft::setSkipTrainingFormations));
        guards.add(check(console, "auto.respectExclusions", draft.isRespectExclusions(),
              draft::setRespectExclusions));
        if (console.plan().context().isUseSkillTrainingCosts()) {
            guards.add(check(console, "auto.skipFees", draft.isSkipFeeImprovements(),
                  draft::setSkipFeeImprovements));
        }
        guards.setAlignmentX(Component.LEFT_ALIGNMENT);
        parts.add(guards);

        parts.add(preview(console, selection, scope, draft));
        return parts;
    }

    private static JComponent presets(TrainingConsole console, AutoTrainSettings.Builder draft) {
        TrainingPresets presets = console.campaign().getTrainingPresets();
        JPanel holder = ConsoleWidgets.column();
        holder.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "auto.presets"))));
        List<JComponent> chips = new ArrayList<>();
        for (AutoTrainSettings preset : presets.getPresets()) {
            String label = preset.isRunMonthly() ?
                                 getFormattedTextAt(RESOURCE_BUNDLE, "auto.preset.monthly", preset.name()) :
                                 preset.name();
            HudChip chip = new HudChip(label, () -> {
                console.state().autoTrain = preset.toBuilder();
                console.state().presetNameText = null;
                console.refreshPanel();
            });
            chip.setActive(preset.name().equalsIgnoreCase(draft.getName()));
            chips.add(chip);
        }
        if (chips.isEmpty()) {
            holder.add(leftAligned(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "auto.presets.none"), 0.8f,
                  TEXT_FAINT)));
        } else {
            holder.add(chips(chips));
        }

        // What the player types is kept as they type it, so a redraw from another setting doesn't wipe a name that
        // isn't saved yet
        TrainingConsoleState state = console.state();
        JTextField name = new JTextField((state.presetNameText == null) ? draft.getName() : state.presetNameText, 14);
        Hud.styleField(name);
        name.getAccessibleContext().setAccessibleName(getTextAt(RESOURCE_BUNDLE, "auto.preset.name"));
        ConsoleWidgets.onTextChange(name, text -> state.presetNameText = text);
        HudCheckBox monthly = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "auto.preset.runMonthly"));
        monthly.setSelected(draft.isRunMonthly());
        monthly.setToolTipText(getTextAt(RESOURCE_BUNDLE, "auto.preset.runMonthly.toolTipText"));
        monthly.addActionListener(event -> draft.setRunMonthly(monthly.isSelected()));
        HudButton save = ConsoleWidgets.button(getTextAt(RESOURCE_BUNDLE, "auto.preset.save"), false, true, () -> {
            String chosen = name.getText().trim();
            if (chosen.isEmpty()) {
                console.setMessage(getTextAt(RESOURCE_BUNDLE, "auto.preset.needsName"), true);
                return;
            }
            draft.setName(chosen);
            state.presetNameText = null;
            presets.save(draft.build());
            console.setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "auto.preset.saved", chosen), false);
        });
        JPanel saveRow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(6), scaleForGUI(3)));
        saveRow.add(name);
        saveRow.add(save);
        String savedName = draft.getName();
        if (presets.find(savedName) != null) {
            saveRow.add(ConsoleWidgets.button(getTextAt(RESOURCE_BUNDLE, "auto.preset.delete"), false, true, () -> {
                presets.remove(savedName);
                console.setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "auto.preset.deleted", savedName), false);
            }));
        }
        saveRow.add(monthly);
        saveRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        holder.add(saveRow);
        return holder;
    }

    /**
     * @return a cache key for something worked out from the people auto-train would train: the selection, or when no
     *       one is selected, everyone shown
     */
    private static String scopeKey(TrainingConsole console, List<Person> selection, String key) {
        return selection.isEmpty() ? key + ".shown." + console.shownRevision() : key;
    }

    private static JComponent preview(TrainingConsole console, List<Person> selection, List<Person> scope,
          AutoTrainSettings.Builder draft) {
        AutoTrainSettings settings = draft.build();
        AutoTrainPlanner.Plan plan = console.cached(scopeKey(console, selection, "auto.plan." + settings),
              () -> AutoTrainPlanner.plan(console.campaign(), console.plan(), scope, settings));
        JPanel box = ConsoleWidgets.previewBox();
        box.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "auto.preview"))));
        box.add(Box.createVerticalStrut(scaleForGUI(4)));
        box.add(leftAligned(ConsoleWidgets.bold(getFormattedTextAt(RESOURCE_BUNDLE, "auto.preview.summary",
              plan.staged().size(), plan.peopleCount()), 1.05f, ACCENT_BRIGHT)));
        box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "xp.amount", plan.xp()), 0.88f,
              TEXT)));
        if (console.plan().context().isUseSkillTrainingCosts()) {
            Money left = console.plan().fundsLeft().minus(plan.fees());
            box.add(leftAligned(ConsoleWidgets.label(getFormattedTextAt(RESOURCE_BUNDLE, "auto.preview.fees",
                  plan.fees().toAmountAndSymbolString(), left.toAmountAndSymbolString()), 0.86f,
                  left.isPositiveOrZero() ? AMBER : DANGER)));
        }
        if (!plan.bySkill().isEmpty()) {
            String bySkill = plan.bySkill().entrySet().stream().limit(6)
                                   .map(entry -> getFormattedTextAt(RESOURCE_BUNDLE, "abilities.choice.count",
                                         entry.getKey(), entry.getValue()))
                                   .collect(Collectors.joining(", "));
            box.add(leftAligned(ConsoleWidgets.wrapped(bySkill, TEXT_MUTED)));
        } else {
            box.add(leftAligned(ConsoleWidgets.label(getTextAt(RESOURCE_BUNDLE, "auto.preview.nothing"), 0.84f,
                  TEXT_FAINT)));
        }
        List<String> skipped = new ArrayList<>();
        for (Map.Entry<Skip, Integer> entry : plan.skipped().entrySet()) {
            skipped.add(getFormattedTextAt(RESOURCE_BUNDLE, "auto.skipped." + entry.getKey().name(),
                  entry.getValue()));
        }
        if (!skipped.isEmpty()) {
            box.add(leftAligned(ConsoleWidgets.wrapped(getFormattedTextAt(RESOURCE_BUNDLE, "auto.skipped",
                  String.join(", ", skipped)), TEXT_FAINT)));
        }
        box.add(ConsoleWidgets.buttonRow(ConsoleWidgets.button(getTextAt(RESOURCE_BUNDLE, "auto.stage"), true,
              !plan.staged().isEmpty(), () -> console.stage(getFormattedTextAt(RESOURCE_BUNDLE, "auto.step",
                    settings.targetLevel(), (settings.specificSkill() == null) ?
                                                  getTextAt(RESOURCE_BUNDLE, "auto.train.roleSkills") :
                                                  settings.specificSkill()), plan.purchases()))));
        return box;
    }

    private static JPanel field(String label, JComponent control) {
        JPanel field = ConsoleWidgets.column();
        field.add(leftAligned(Hud.eyebrow(label)));
        field.add(Box.createVerticalStrut(scaleForGUI(4)));
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(control, BorderLayout.CENTER);
        holder.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.add(holder);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private static JComponent chips(List<JComponent> chips) {
        JPanel row = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(6), scaleForGUI(3)));
        chips.forEach(row::add);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static HudChip chip(TrainingConsole console, String text, boolean isActive, Runnable onChoose) {
        HudChip chip = new HudChip(text, () -> {
            onChoose.run();
            console.refreshPanel();
        });
        chip.setActive(isActive);
        return chip;
    }

    private static HudCheckBox check(TrainingConsole console, String key, boolean isSelected,
          Consumer<Boolean> onChange) {
        HudCheckBox box = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, key));
        String toolTipText = getTextAt(RESOURCE_BUNDLE, key + ".toolTipText");
        if (isResourceKeyValid(toolTipText)) {
            box.setToolTipText(WrapLayout.wordWrap(toolTipText));
        }
        box.setSelected(isSelected);
        box.addActionListener(event -> {
            onChange.accept(box.isSelected());
            console.refreshPanel();
        });
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    private static <T> DefaultListCellRenderer labelled(Function<T, String> labeler) {
        return new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Component component = Hud.comboRenderer().getListCellRendererComponent(list, value, index,
                      isSelected, cellHasFocus);
                if ((component instanceof JLabel label) && (value != null)) {
                    label.setText(labeler.apply((T) value));
                }
                return component;
            }
        };
    }
}
