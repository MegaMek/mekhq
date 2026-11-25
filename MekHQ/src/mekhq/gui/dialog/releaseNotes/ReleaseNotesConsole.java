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
package mekhq.gui.dialog.releaseNotes;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import jakarta.annotation.Nullable;
import megamek.SuiteConstants;
import megamek.Version;
import megamek.client.ui.dialogs.releaseNotes.ReleaseNotes;
import megamek.client.ui.dialogs.releaseNotes.ReleaseNotesDialog;
import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudTabStrip;

/**
 * Help &gt; Release Notes in MekHQ: what changed in MekHQ, MegaMek and MegaMekLab since the version the player ran
 * before, one tab each, in the heads-up-display look of the interstellar map. The notes and the versions they cover come
 * from MegaMek's {@link ReleaseNotes}, as they do in the other applications; only the look is MekHQ's.
 *
 * <p>The window is not modal and each tab loads in the background, so a slow or refused GitHub request never holds up
 * the campaign.</p>
 *
 * @since 0.51.01
 */
public class ReleaseNotesConsole extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ReleaseNotesConsole.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.ReleaseNotesConsole";
    /** MekHQ first, then the applications it includes. */
    static final List<String> REPOSITORIES = List.of("mekhq", "megamek", "megameklab");

    private final transient List<Tab> tabs = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);
    private final JLabel subtitle = new JLabel(" ");
    private final JLabel footerSummary = new JLabel(" ");
    private final HudTabStrip tabStrip;
    private int selected;

    /** The notes of one application: its reader, its page on GitHub, the notes once loaded and the work loading them. */
    private static final class Tab {
        private final String repository;
        private final JEditorPane reader = new JEditorPane();
        private URI page;
        private ReleaseNotes notes;
        private boolean failed;
        private SwingWorker<ReleaseNotes, Void> worker;

        private Tab(String repository, Version version) {
            this.repository = repository;
            page = ReleaseNotes.page(repository, version);
        }
    }

    private ReleaseNotesConsole(Window owner, Version version) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.windowTitle"), ModalityType.MODELESS);
        setName("releaseNotesConsole");
        List<String> labels = new ArrayList<>();
        for (String repository : REPOSITORIES) {
            tabs.add(new Tab(repository, version));
            labels.add(productName(repository));
        }
        tabStrip = new HudTabStrip(labels, false, this::select);
        tabStrip.setName("releaseNotesTabs");

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);
        content.add(buildCommandBar(version), BorderLayout.NORTH);
        content.add(buildDeck(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);
        content.registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);

        select(0);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(scaleForGUI(560, 420));
        content.setPreferredSize(scaleForGUI(780, 640));
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Opens what changed in the running version since the version the player ran before it, within the same release
     * channel, and loads the notes in the background.
     *
     * @param owner the window to center on, or {@code null}
     */
    public static void showReleaseNotes(Window owner) {
        Version version = SuiteConstants.VERSION;
        Version previous = ReleaseNotes.previousVersion();
        ReleaseNotesConsole console = new ReleaseNotesConsole(owner, version);
        for (Tab tab : console.tabs) {
            tab.worker = new SwingWorker<>() {
                @Override
                protected ReleaseNotes doInBackground() throws Exception {
                    return ReleaseNotes.load(tab.repository, previous, version);
                }

                @Override
                protected void done() {
                    if (isCancelled() || !console.isDisplayable()) {
                        return;
                    }
                    try {
                        console.showNotes(tab, get());
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException failure) {
                        LOGGER.warn("Could not load the {} release notes: {}", tab.repository,
                              failure.getCause().toString());
                        console.showFailure(tab);
                    }
                }
            };
            tab.worker.execute();
        }
        console.setVisible(true);
    }

    @Override
    public void dispose() {
        for (Tab tab : tabs) {
            if (tab.worker != null) {
                tab.worker.cancel(true);
            }
        }
        super.dispose();
    }

    private JPanel buildCommandBar(Version version) {
        JLabel eyebrow = new JLabel(text("ReleaseNotesConsole.eyebrow"));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        JLabel title = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.title", version.toString())
                                        .toUpperCase(Locale.ROOT));
        title.setName("releaseNotesTitle");
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));
        subtitle.setName("releaseNotesSince");
        subtitle.setText(getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.subtitle"));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));

        JPanel identity = new JPanel();
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setOpaque(false);
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitle));

        JPanel commandBar = new JPanel(new BorderLayout(0, scaleForGUI(12)));
        commandBar.setOpaque(true);
        commandBar.setBackground(GROUND);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));
        commandBar.add(identity, BorderLayout.CENTER);
        commandBar.add(tabStrip, BorderLayout.SOUTH);
        return commandBar;
    }

    private JPanel buildDeck() {
        deck.setOpaque(true);
        deck.setBackground(GROUND);
        for (Tab tab : tabs) {
            tab.reader.setName("releaseNotesContent." + tab.repository);
            tab.reader.setEditable(false);
            tab.reader.setOpaque(true);
            tab.reader.setBackground(GROUND);
            HTMLEditorKit kit = new HTMLEditorKit();
            tab.reader.setEditorKit(kit);
            HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
            applyReaderStyles(document.getStyleSheet());
            tab.reader.setDocument(document);
            tab.reader.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(20), scaleForGUI(20),
                  scaleForGUI(20)));
            tab.reader.getAccessibleContext().setAccessibleName(getFormattedTextAt(RESOURCE_BUNDLE,
                  "ReleaseNotesConsole.reader.accessibleName", productName(tab.repository)));
            tab.reader.addHyperlinkListener(event -> {
                if ((event.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                          && ReleaseNotesDialog.isSafe(event.getDescription())) {
                    UIUtil.browse(event.getDescription(), this);
                }
            });
            showText(tab, paragraph("muted", getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.loading")));

            JScrollPane scroll = new JScrollPane(tab.reader);
            scroll.setName("releaseNotesScroll." + tab.repository);
            Hud.styleScroll(scroll, GROUND, false);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            deck.add(scroll, tab.repository);
        }
        return deck;
    }

    private JPanel buildFooter() {
        footerSummary.setName("releaseNotesSummary");
        footerSummary.setForeground(TEXT_MUTED);
        footerSummary.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));

        HudButton viewOnGitHub = new HudButton(text("ReleaseNotesConsole.footer.viewOnGitHub"), false);
        viewOnGitHub.setName("releaseNotesViewOnGitHub");
        viewOnGitHub.setToolTipText(getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.footer.viewOnGitHub.toolTipText"));
        viewOnGitHub.addActionListener(event -> UIUtil.browse(tabs.get(selected).page.toString(), this));
        HudButton close = new HudButton(text("ReleaseNotesConsole.footer.close"), true);
        close.setName("releaseNotesClose");
        close.addActionListener(event -> dispose());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(viewOnGitHub);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(10)));
        buttons.add(close);

        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(18), scaleForGUI(10), scaleForGUI(18))));
        footer.add(footerSummary, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void select(int index) {
        selected = index;
        tabStrip.setSelected(index);
        cards.show(deck, tabs.get(index).repository);
        refreshSummary();
    }

    private void showNotes(Tab tab, ReleaseNotes notes) {
        tab.notes = notes;
        tab.page = notes.page();
        showText(tab, body(notes));
        refreshSummary();
    }

    private void showFailure(Tab tab) {
        tab.failed = true;
        showText(tab, paragraph("muted", getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.failed")));
        refreshSummary();
    }

    /** Shows the selected tab's range under the title, and its summary in the footer. */
    private void refreshSummary() {
        Tab tab = tabs.get(selected);
        subtitle.setText(subtitle(tab.failed ? null : tab.notes));
        if (tab.failed) {
            footerSummary.setText(getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.summary.failed"));
        } else if (tab.notes == null) {
            footerSummary.setText(getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.summary.loading"));
        } else {
            footerSummary.setText(summary(tab.notes));
        }
    }

    /**
     * The line under the title for one tab: the version its notes start from when they cover a range, and the general
     * text otherwise, including while they load.
     *
     * @param notes the tab's loaded notes, or {@code null} when they are not loaded
     *
     * @return the subtitle
     */
    static String subtitle(@Nullable ReleaseNotes notes) {
        if ((notes == null) || (notes.since() == null)) {
            return getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.subtitle");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.subtitle.since", notes.since());
    }

    private static void showText(Tab tab, String body) {
        tab.reader.setText("<html><body>" + body + "</body></html>");
        tab.reader.setCaretPosition(0);
    }

    /**
     * @param notes the loaded notes of one application
     *
     * @return the footer line for them, for example "MekHQ: 12 changes in 3 releases"
     */
    static String summary(ReleaseNotes notes) {
        int changes = 0;
        for (ReleaseNotes.Release release : notes.releases()) {
            for (ReleaseNotes.Section section : release.sections()) {
                changes += section.changes().size();
            }
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.summary", productName(notes.repository()),
              changes, notes.releases().size());
    }

    /**
     * The notes as the body of the reader's HTML, under a heading for each release when they cover the releases since
     * an older version, even when only one of those releases lists changes.
     *
     * @param notes the loaded notes of one application
     *
     * @return the HTML body
     */
    static String body(ReleaseNotes notes) {
        if (notes.isEmpty()) {
            return paragraph("muted", getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.empty"));
        }
        boolean several = (notes.since() != null) || (notes.releases().size() > 1);
        StringBuilder html = new StringBuilder();
        for (ReleaseNotes.Release release : notes.releases()) {
            if (several) {
                html.append("<h2><a href='").append(escape(release.page().toString())).append("'>")
                      .append(escape(release.version())).append("</a></h2>");
            }
            for (ReleaseNotes.Section section : release.sections()) {
                String heading = ReleaseNotes.UNGROUPED.equals(section.heading())
                      ? getTextAt(RESOURCE_BUNDLE, "ReleaseNotesConsole.changes") : section.heading();
                html.append("<h3>").append(escape(heading.toUpperCase(Locale.ROOT))).append("</h3><ul>");
                for (ReleaseNotes.Change change : section.changes()) {
                    html.append("<li>").append(escape(change.title()));
                    if (change.link() != null) {
                        html.append(" <a class='faint' href='").append(escape(change.link().toString()))
                              .append("'>#").append(change.pullRequest()).append("</a>");
                    }
                    html.append("</li>");
                }
                html.append("</ul>");
            }
        }
        return html.toString();
    }

    private static void applyReaderStyles(StyleSheet styles) {
        Font font = hudFont(Font.PLAIN, 1.0f, 0.0f);
        int size = font.getSize();
        styles.addRule("body { color: " + hex(TEXT) + "; background-color: " + hex(GROUND) + "; font-family: '"
                             + font.getFamily() + "'; font-size: " + size + "pt; margin: 0; }");
        styles.addRule("p { margin-top: 0; margin-bottom: 8px; }");
        styles.addRule("h2 { color: " + hex(ACCENT_BRIGHT) + "; font-size: " + (size + 3)
                             + "pt; margin-top: 12px; margin-bottom: 2px; }");
        styles.addRule("h3 { color: " + hex(AMBER) + "; font-size: " + Math.max(8, size - 1)
                             + "pt; margin-top: 8px; margin-bottom: 4px; }");
        styles.addRule("ul { margin-left: 18px; margin-top: 0; margin-bottom: 8px; }");
        styles.addRule("li { margin-bottom: 3px; }");
        styles.addRule("a { color: " + hex(ACCENT_BRIGHT) + "; }");
        styles.addRule(".muted { color: " + hex(TEXT_MUTED) + "; }");
        styles.addRule(".faint { color: " + hex(ACCENT) + "; font-size: " + Math.max(8, size - 2) + "pt; }");
    }

    static String productName(String repository) {
        return switch (repository) {
            case "megameklab" -> "MegaMekLab";
            case "megamek" -> "MegaMek";
            default -> "MekHQ";
        };
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT);
    }

    private static String paragraph(String cssClass, String text) {
        return "<p class='" + cssClass + "'>" + escape(text) + "</p>";
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
              .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
