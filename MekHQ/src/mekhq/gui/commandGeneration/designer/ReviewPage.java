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
package mekhq.gui.commandGeneration.designer;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.commandGeneration.designer.DesignerUi.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudRing;
import mekhq.gui.baseComponents.hud.HudVerdictBanner;

/**
 * The Command Designer's Review page: what happens when the command is built.
 *
 * <p>The build order, step by step, with what each step will do for this command; anything worth a second look before
 * building; whether the settings changed after the roll; and what the build writes into the campaign's own settings.
 * Build Command is pressed from here, so this is the one place the campaign is committed to.</p>
 */
public final class ReviewPage {
    private static final int WRAP_WIDTH = 460;

    /** Something worth a second look, and the page that deals with it. */
    private record Issue(String title, String detail, Color accent, DesignerPage page) {}

    private final Supplier<ForceSummary> summary;
    private final PeoplePage people;
    private final LogisticsPage logistics;
    private final BooleanSupplier liftRolled;
    private final Consumer<DesignerPage> goTo;
    private Supplier<List<String>> buildWarnings = List::of;
    private BooleanSupplier settingsChangedSinceRoll = () -> false;
    private BooleanSupplier writesAugmentation = () -> false;

    private final JPanel root = new JPanel(new GridBagLayout());
    private final HudVerdictBanner banner = new HudVerdictBanner();
    private final JPanel steps = column();
    private final JPanel issues = column();
    private final JPanel changes = column();
    private final JPanel writes = column();

    /**
     * @param summary   the rolled command, as the Force page sums it up
     * @param people     the People page, for what the support stage will hire and the history settings
     * @param logistics  the Logistics page, for the finance settings
     * @param liftRolled whether the roll is set to bring DropShips
     * @param goTo       shows another page, when an issue's link is followed
     */
    public ReviewPage(Supplier<ForceSummary> summary, PeoplePage people, LogisticsPage logistics,
          BooleanSupplier liftRolled, Consumer<DesignerPage> goTo) {
        this.summary = summary;
        this.people = people;
        this.logistics = logistics;
        this.liftRolled = liftRolled;
        this.goTo = goTo;

        root.setOpaque(true);
        root.setBackground(GROUND);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        constraints.gridy = 0;

        JPanel left = stack(banner,
              section(text("Designer.review.section.steps"), text("Designer.review.section.steps.aside"), steps));
        padded(left, 12, 16, 18, 16);
        constraints.gridx = 0;
        constraints.weightx = 1.2;
        root.add(scroll(left, GROUND), constraints);

        JPanel right = stack(
              section(text("Designer.review.section.issues"), null, issues),
              section(text("Designer.review.section.changes"), null, changes),
              section(text("Designer.review.section.writes"), null, writes));
        padded(right, 12, 16, 18, 16);
        JComponent rightScroll = scroll(right, SURFACE_DEEP);
        rightScroll.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        root.add(rightScroll, constraints);
    }

    public JComponent getComponent() {
        return root;
    }

    /**
     * Supplies what only the dialog knows: the warnings about the build's cost, and whether the settings moved after
     * the roll.
     *
     * @param warnings           the build's resource warnings, such as a long generation
     * @param settingsChanged    whether the settings differ from those the roll was made with
     * @param augmentationWrites whether the build will write the augmentation rules into the campaign
     */
    public void setDialogFacts(Supplier<List<String>> warnings, BooleanSupplier settingsChanged,
          BooleanSupplier augmentationWrites) {
        this.buildWarnings = warnings;
        this.settingsChangedSinceRoll = settingsChanged;
        this.writesAugmentation = augmentationWrites;
    }

    /** @return {@code true} if there is a command to build: something rolled and not all of it struck out */
    public boolean canBuild() {
        ForceSummary rolled = summary.get();
        return rolled.hasRoll() && (rolled.getIncludedUnits() > 0);
    }

    /** Recomputes the page from the roll and the other pages' settings. */
    public void refresh() {
        ForceSummary rolled = summary.get();
        List<Issue> found = findIssues(rolled);
        if (!canBuild()) {
            banner.setVerdict(text("Designer.review.verdict.blocked"),
                  wrapHtml(text(rolled.hasRoll() ? "Designer.review.verdict.blocked.struck"
                        : "Designer.review.verdict.blocked.noRoll"), WRAP_WIDTH),
                  text("Designer.review.verdict.blocked.badge"), DANGER);
        } else if (found.isEmpty()) {
            banner.setVerdict(text("Designer.review.verdict.ready"), readyReason(rolled),
                  formatted("Designer.review.verdict.badge", rolled.getIncludedUnits()), READY);
        } else {
            banner.setVerdict(formatted("Designer.review.verdict.check", found.size()), readyReason(rolled),
                  formatted("Designer.review.verdict.badge", rolled.getIncludedUnits()), AMBER);
        }

        refreshSteps(rolled);

        issues.removeAll();
        if (found.isEmpty()) {
            addRows(issues, faint(text("Designer.review.issues.none")));
        }
        for (Issue issue : found) {
            addRows(issues, issueRow(issue));
        }

        changes.removeAll();
        addRows(changes, wrapped(text(settingsChangedSinceRoll.getAsBoolean() ? "Designer.review.changes.yes"
              : "Designer.review.changes.no"), WRAP_WIDTH - 60));

        writes.removeAll();
        addRows(writes, muted(text("Designer.review.writes.spares")));
        if (writesAugmentation.getAsBoolean()) {
            addRows(writes, muted(text("Designer.review.writes.augmentation")));
        }
        addRows(writes, faint(text("Designer.review.writes.origin")));

        root.revalidate();
        root.repaint();
    }

    private String readyReason(ForceSummary rolled) {
        return wrapHtml(formatted("Designer.review.verdict.reason", rolled.getIncludedUnits(),
              rolled.getCrew() + people.getEstimatedSupportHires()), WRAP_WIDTH);
    }

    private void refreshSteps(ForceSummary rolled) {
        steps.removeAll();
        int hires = people.getEstimatedSupportHires();
        boolean simulation = people.isRunStartingSimulation();
        int number = 1;
        addRows(steps,
              step(number++, text("Designer.review.step.commit"),
                    formatted("Designer.review.step.commit.what", rolled.getIncludedUnits(), rolled.getDropShips()
                          + rolled.getJumpShips() + rolled.getWarShips()),
                    formatted("Designer.review.step.commit.tag", rolled.getIncludedUnits()), true),
              step(number++, text("Designer.review.step.ranks"), text("Designer.review.step.ranks.what"),
                    text("Designer.review.step.ranks.tag"), true),
              step(number++, text("Designer.review.step.support"),
                    formatted("Designer.review.step.support.what", hires, people.getEstimatedPoolHires()),
                    formatted("Designer.review.step.support.tag", hires), true),
              step(number++, text("Designer.review.step.techs"), text("Designer.review.step.techs.what"),
                    text("Designer.review.step.techs.tag"), true),
              step(number++, text("Designer.review.step.spares"), text("Designer.review.step.spares.what"),
                    text(liftRolled.getAsBoolean() ? "Designer.review.step.spares.tag"
                          : "Designer.review.step.spares.tag.hire"), true),
              step(number++, text("Designer.review.step.flags"),
                    formatted("Designer.review.step.flags.what", people.getEstimatedReserve()),
                    text("Designer.review.step.flags.tag"), true),
              step(number++, text("Designer.review.step.cash"),
                    formatted("Designer.review.step.cash.months", logistics.getStartingCashMonths()),
                    text("Designer.review.step.cash.tag"), true),
              step(number, text("Designer.review.step.history"),
                    simulation ? formatted("Designer.review.step.history.what", people.getSimulationYears())
                          : text("Designer.review.step.history.off"),
                    text(simulation ? "Designer.review.step.history.tag" : "Designer.review.step.skipped"),
                    simulation));
    }

    private List<Issue> findIssues(ForceSummary rolled) {
        List<Issue> found = new ArrayList<>();
        if (!rolled.hasRoll()) {
            found.add(new Issue(text("Designer.review.issue.noRoll"), text("Designer.review.issue.noRoll.detail"),
                  DANGER, DesignerPage.FORCE));
        } else if (rolled.getStruckUnits() > 0) {
            found.add(new Issue(formatted("Designer.review.issue.struck", rolled.getStruckUnits()),
                  text("Designer.review.issue.struck.detail"), ACCENT, DesignerPage.FORCE));
        }
        if (people.isOriginStuckAtHome()) {
            found.add(new Issue(text("Designer.review.issue.origin"), text("Designer.review.issue.origin.detail"),
                  AMBER, DesignerPage.PEOPLE));
        }
        if (rolled.hasRoll() && !liftRolled.getAsBoolean()) {
            found.add(new Issue(text("Designer.review.issue.lift"), text("Designer.review.issue.lift.detail"),
                  AMBER, DesignerPage.FORCE));
        }
        if (settingsChangedSinceRoll.getAsBoolean()) {
            found.add(new Issue(text("Designer.review.issue.changed"), text("Designer.review.issue.changed.detail"),
                  AMBER, DesignerPage.FORCE));
        }
        for (String warning : buildWarnings.get()) {
            found.add(new Issue(text("Designer.review.issue.cost"), warning, AMBER, DesignerPage.FORCE));
        }
        return found;
    }

    private JComponent issueRow(Issue issue) {
        HudRing ring = new HudRing();
        ring.setColor(issue.accent(), true);
        JPanel ringHolder = Hud.transparentPanel(new BorderLayout());
        ringHolder.add(ring, BorderLayout.NORTH);
        ringHolder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(2), 0, 0, 0));

        JLabel title = new JLabel(issue.title());
        title.setForeground(TEXT);
        title.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        JPanel text = column();
        text.add(leftAligned(title));
        text.add(leftAligned(wrapped(ForcePage.escape(issue.detail()), WRAP_WIDTH - 140)));

        JLabel link = Hud.link(formatted("Designer.review.issue.link", issue.page().getLabel()),
              () -> goTo.accept(issue.page()));

        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setOpaque(true);
        row.setBackground(SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(8), scaleForGUI(10))));
        row.add(ringHolder, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(link, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent step(int number, String title, String what, String tag, boolean active) {
        JLabel numberLabel = new JLabel(Integer.toString(number), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    if (active) {
                        canvas.setPaint(new GradientPaint(0, 0, translucent(ACCENT, 56), 0, getHeight(),
                              translucent(ACCENT, 20)));
                        canvas.fillRect(0, 0, getWidth(), getHeight());
                    }
                    canvas.setColor(active ? BORDER_CYAN : BORDER);
                    canvas.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                } finally {
                    canvas.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        numberLabel.setForeground(active ? ACCENT_BRIGHT : TEXT_FAINT);
        numberLabel.setFont(hudFont(Font.BOLD, 1.0f, 0.0f));
        Dimension size = new Dimension(scaleForGUI(28), scaleForGUI(28));
        numberLabel.setPreferredSize(size);
        numberLabel.setMinimumSize(size);
        JPanel numberHolder = Hud.transparentPanel(new BorderLayout());
        numberHolder.add(numberLabel, BorderLayout.NORTH);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(active ? TEXT : TEXT_FAINT);
        titleLabel.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        JPanel text = column();
        text.add(leftAligned(titleLabel));
        JLabel whatLabel = wrapped(ForcePage.escape(what), WRAP_WIDTH - 120);
        if (!active) {
            whatLabel.setForeground(TEXT_FAINT);
        }
        text.add(leftAligned(whatLabel));

        JLabel tagLabel = new JLabel(upper(tag));
        Color tagColor = active ? ACCENT : TEXT_FAINT;
        tagLabel.setForeground(tagColor);
        tagLabel.setFont(hudFont(Font.BOLD, 0.68f, 0.12f));
        tagLabel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(translucent(tagColor, 140), scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(2), scaleForGUI(5), scaleForGUI(2), scaleForGUI(5))));
        JPanel tagHolder = Hud.transparentPanel(new BorderLayout());
        tagHolder.add(tagLabel, BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        row.setOpaque(true);
        row.setBackground(SURFACE_DEEP);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(9), scaleForGUI(12), scaleForGUI(9), scaleForGUI(12))));
        row.add(numberHolder, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(tagHolder, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
