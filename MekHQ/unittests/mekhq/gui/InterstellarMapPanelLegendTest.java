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
package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignLocationManager;
import org.junit.jupiter.api.Test;

class InterstellarMapPanelLegendTest {
    private static final List<String> GROUPS = List.of(
          "NAVIGATION", "ROUTES", "LAYERS", "OVERLAYS", "RANGE RINGS", "SYSTEM STATUS");
        private static final List<Integer> GROUP_ENTRY_COUNTS = List.of(5, 8, 12, 8, 4, 4);
    private static final List<String> TITLES = List.of(
                "Selected system", "Hovered system", "Planned route",
                  "Active route", "Reachability shells", "Reachability caution", "Blocked reachability",
                        "Distance measurement", "Current fleet", "Player base", "Waypoint number", "Route caution",
                    "Blocked route leg",
                  "Contract-search radius", "Planetary-acquisition radius", "Jump radius",
                  "50 ly HPG range", "Capital hierarchy", "Operation flag",
        "Restricted system", "GM-edited system", "Faction ownership", "Technology", "Industry", "Raw Materials",
        "Output", "Agriculture", "Population", "HPG", "Recharge Stations", "Academies", "Hiring Halls",
        "Disease Outbreaks", "HPG station classes", "Faction emblem", "HPG network",
        "Administrative boundaries", "Sovereign border", "Disputed territory", "Unclaimed pocket", "Enclave");
    private static final List<String> DESCRIPTIONS = List.of(
        "An amber ring identifies the selected system at distant zoom; corner brackets replace it as navigation detail appears.",
        "A cyan ring identifies the system under the pointer at distant zoom; corner brackets replace it closer in. Selected systems suppress hover.",
        "A cyan dashed path remains visible at distant zoom; complete thin stop rings appear with navigation detail.",
        "Amber paths remain visible at distant zoom; complete stop rings appear with navigation detail.",
        "At navigation zoom, centered shapes show minimum hops: cyan circles mark one, squares mark two, and hexagons mark three.",
        "At navigation zoom, an amber triangle surrounding a system marks it as reachable with a caution.",
        "At navigation zoom, a red diamond surrounding a system marks the blocked frontier.",
        "A pale dash-dot line uses a circle surrounding endpoint A and a diamond surrounding endpoint B for a transient direct measurement.",
        "An amber JumpShip above-right of a system marks the fleet. At distant zoom, an amber ring surrounding the system replaces the ship.",
        "A teal marker identifies a system with player bases. Navigation detail adds the number of bases when more than one share the system.",
        "At navigation zoom, a numbered badge below-right of a system gives each requested route stop's order.",
        "An amber triangle to the right of a system marks an allowed route leg with a grounded caution, such as an abandoned destination.",
        "A red dashed segment and diamond to the right of its destination mark a leg blocked by range, access, avoidance, or recharge constraints.",
        "A configurable-color thick dashed ring centered on the selected system bounds contract searches; campaign and MekHQ options control visibility.",
        "A configurable-color thick dash-dot ring centered on the selected system bounds planetary acquisition; campaign, MekHQ, and zoom options control visibility.",
        "A configurable-color thick solid ring centered on the selected system shows one-jump reach; MekHQ and zoom options control visibility.",
        "A dark-green dotted ring centered on the selected system marks 50 ly; HPG layer visibility controls when it appears.",
        "National capitals use five-point stars, regional capitals use four-ray compass stars, and district capitals use simple pips. Layers controls the visible tiers; districts appear only at navigation detail.",
        "At distant zoom, a centered red diamond marks an urgent active scenario. Closer in, flags show missions, scenarios, and mission counts.",
        "A red prohibition ring marks a system barred by outlaw or restricted-entry standing rules.",
        "At detail zoom, a cyan pencil below a system marks a non-canon override.",
        "Compact contacts use faction color at navigation zoom; detail zoom shows intrinsic stars with crisp ownership rings. Shared systems divide both equally.",
        "Regressed is dark gray; F purple; D blue; C teal; B green; A or Advanced yellow; no population is black.",
        "F is near-black; D purple; C magenta; B coral; A pale yellow; no population is black.",
        "F is blue; D purple; C magenta; B orange; A yellow; no population is black.",
        "F is near-black; D purple; C magenta; B orange; A pale yellow; no population is black.",
        "F is dark blue; D blue-gray; C gray; B tan; A yellow; no population is black.",
        "Purple marks under 1M, then colors progress through violet, blue, teal, and green across 1M, 25M, 100M, 200M, 300M, 500M, 1B, and 1.5B; 3B+ is yellow; none is black.",
        "No HPG is black; D dark gray; C light gray; B pink; A pale yellow.",
        "No station is gray; one station coral; two stations yellow; unavailable data black.",
        "None is black; academy counts 1 through 6 progress from blue-teal through teal and green to yellow.",
        "None is black; Questionable magenta; Minor orange; Standard yellow; Great green.",
        "None is black; one outbreak yellow; two orange; three magenta; four or more purple.",
        "Hexagonal badges identify included HPG stations: A cyan, B blue, C amber, and D red. Network links are drawn only for A and B stations.",
        "A faint emblem watermark identifies territory; its tint identifies the faction.",
        "Class A uses restrained solid cyan links; Class B uses thinner muted dashes. Distant zoom keeps only Class A, while lower-class station badges wait for close detail.",
        "Solid faction-color lines divide regions; quieter dashed lines add districts. Boundaries stop where membership is missing or conflicts.",
        "Translucent faction fill and a solid edge mark territory inferred from dated ownership.",
        "Multiple faction colors, diagonal hatching, and a dashed border mark shared control.",
        "A dark enclosed void with a dotted boundary marks locally unclaimed space.",
        "A closed double border marks one faction's territory enclosed by another.");

    @Test
    void legendTabsAreCompleteStableAndRenderableOffscreen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = InterstellarMapPanel.createMapLegendTabbedPane();
            assertEquals(GROUPS.size(), tabbedPane.getTabCount());
            assertEquals(GROUPS, GROUPS.stream()
                  .map(group -> tabbedPane.getTitleAt(GROUPS.indexOf(group)))
                  .toList());

            List<Component> components = descendantsOf(tabbedPane);
            List<String> labels = components.stream()
                  .filter(JLabel.class::isInstance)
                  .map(JLabel.class::cast)
                  .map(JLabel::getText)
                  .toList();
            List<String> orderedTitles = inDisplayOrder(TITLES);
            List<String> orderedDescriptions = inDisplayOrder(DESCRIPTIONS);
            assertEquals(orderedTitles, labels);
            assertEquals(orderedDescriptions, components.stream()
                .filter(JTextArea.class::isInstance)
                .map(JTextArea.class::cast)
                .map(JTextArea::getText)
                .toList());

            List<JComponent> swatches = components.stream()
                  .filter(JComponent.class::isInstance)
                  .map(JComponent.class::cast)
                .filter(component -> Boolean.TRUE.equals(component.getClientProperty("mapLegendSwatch")))
                  .toList();
            assertEquals(TITLES.size(), swatches.size());
            assertEquals(orderedTitles, swatches.stream()
                .map(swatch -> (String) swatch.getClientProperty("mapLegendTitle"))
                .toList());
            for (JComponent swatch : swatches) {
                assertEquals(swatch.getPreferredSize(), swatch.getMinimumSize());
                assertEquals(swatch.getPreferredSize(), swatch.getMaximumSize());
                renderOffscreen(swatch);
            }

            JButton legendButton = InterstellarMapPanel.createMapLegendButton();
            assertEquals(legendButton.getPreferredSize(), legendButton.getMinimumSize());
            assertEquals(legendButton.getPreferredSize(), legendButton.getMaximumSize());
            assertEquals("Show map symbol legend",
                  legendButton.getAccessibleContext().getAccessibleName());
            assertTrue(legendButton.isFocusable());
            assertEquals(Boolean.TRUE, legendButton.getClientProperty("navigationUtilityButton"));
            renderOffscreen(legendButton);

            Dimension commonViewportSize = null;
            int entryOffset = 0;
            for (int tabIndex = 0; tabIndex < tabbedPane.getTabCount(); tabIndex++) {
                JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(tabIndex);
                assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                      scrollPane.getHorizontalScrollBarPolicy());
                assertTrue(scrollPane.isFocusable());
                    assertEquals(UIUtil.scaleForGUI(10), scrollPane.getVerticalScrollBar().getPreferredSize().width);
                    assertEquals(0, scrollPane.getVerticalScrollBar().getComponent(0).getPreferredSize().height);
                    assertEquals(0, scrollPane.getVerticalScrollBar().getComponent(1).getPreferredSize().height);
                if (commonViewportSize == null) {
                    commonViewportSize = scrollPane.getViewport().getPreferredSize();
                } else {
                    assertEquals(commonViewportSize, scrollPane.getViewport().getPreferredSize());
                }

                int entryCount = GROUP_ENTRY_COUNTS.get(tabIndex);
                List<Component> tabComponents = descendantsOf(scrollPane);
                assertEquals(orderedTitles.subList(entryOffset, entryOffset + entryCount), tabComponents.stream()
                      .filter(JLabel.class::isInstance)
                      .map(JLabel.class::cast)
                      .map(JLabel::getText)
                      .toList());
                assertEquals(orderedDescriptions.subList(entryOffset, entryOffset + entryCount), tabComponents.stream()
                      .filter(JTextArea.class::isInstance)
                      .map(JTextArea.class::cast)
                      .map(JTextArea::getText)
                      .toList());
                entryOffset += entryCount;

                BufferedImage renderedTab = renderOffscreen(scrollPane);
                    assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        scrollPane.getVerticalScrollBarPolicy());
                assertFalse(scrollPane.getHorizontalScrollBar().isVisible());
                assertTrue(hasColorVariation(renderedTab));
            }
            assertEquals(orderedTitles.size(), entryOffset);
        });
    }

    private static <T> List<T> inDisplayOrder(List<T> values) {
        List<Integer> displayOrder = List.of(
              0, 1, 8, 9, 7,
              2, 3, 10, 4, 5, 6, 11, 12,
              21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
              33, 34, 35, 36, 37, 38, 39, 40,
              13, 14, 15, 16,
              17, 18, 19, 20);
        return displayOrder.stream().map(values::get).toList();
    }

    @Test
    void legendSwatchesRemainFixedAndCenteredForWrappedRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = InterstellarMapPanel.createMapLegendTabbedPane();
            for (int tabIndex = 0; tabIndex < tabbedPane.getTabCount(); tabIndex++) {
                renderOffscreen((JScrollPane) tabbedPane.getComponentAt(tabIndex));
            }

            List<Component> components = descendantsOf(tabbedPane);
            List<JTextArea> meanings = components.stream()
                  .filter(JTextArea.class::isInstance)
                  .map(JTextArea.class::cast)
                  .toList();
            assertTrue(meanings.stream().anyMatch(meaning -> meaning.getPreferredSize().height
                  > meaning.getFontMetrics(meaning.getFont()).getHeight()));

            List<JComponent> swatches = components.stream()
                  .filter(JComponent.class::isInstance)
                  .map(JComponent.class::cast)
                  .filter(component -> Boolean.TRUE.equals(component.getClientProperty("mapLegendSwatch")))
                  .toList();
            for (JComponent swatch : swatches) {
                String title = (String) swatch.getClientProperty("mapLegendTitle");
                JComponent swatchCell = componentWithProperty(components, "mapLegendSwatchCell", title);
                JComponent textCell = componentWithProperty(components, "mapLegendTextCell", title);
                JComponent row = componentWithProperty(components, "mapLegendRow", title);

                assertEquals(swatch.getPreferredSize(), swatch.getSize());
                Point swatchCenterInCell = SwingUtilities.convertPoint(swatch,
                      swatch.getWidth() / 2, swatch.getHeight() / 2, swatchCell);
                assertEquals(swatchCell.getHeight() / 2.0, swatchCenterInCell.y, 1.0);
                Point swatchCenterInRow = SwingUtilities.convertPoint(swatch,
                      swatch.getWidth() / 2, swatch.getHeight() / 2, row);
                Point textCenterInRow = SwingUtilities.convertPoint(textCell,
                      textCell.getWidth() / 2, textCell.getHeight() / 2, row);
                assertEquals(textCenterInRow.y, swatchCenterInRow.y, 1.0);
                assertEquals(row.getHeight() / 2.0, swatchCenterInRow.y, 1.5);
            }
        });
    }

        @Test
        void gmEditedLegendShowsMaterialPencilBelowSystem() throws Exception {
          SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = InterstellarMapPanel.createMapLegendTabbedPane();
            JComponent swatch = descendantsOf(tabbedPane).stream()
                .filter(JComponent.class::isInstance)
                .map(JComponent.class::cast)
                .filter(component -> "GM-edited system".equals(
                    component.getClientProperty("mapLegendTitle")))
                .filter(component -> Boolean.TRUE.equals(
                    component.getClientProperty("mapLegendSwatch")))
                .findFirst()
                .orElseThrow();

            BufferedImage image = renderOffscreen(swatch);
            assertTrue(nearColorPixelCount(image, new Color(65, 210, 224), image.getHeight() / 2) > 0,
                "the cyan pencil body must be visible below the reference system");
            assertTrue(maximumNearColorY(image, new Color(65, 210, 224)) < image.getHeight() - 1,
                "the pencil must not be clipped by the bottom edge");
          });
        }

    @Test
    void layerControlDrawerContainsNoCommandButtons() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Campaign campaign = mock(Campaign.class);
            when(campaign.getSystems()).thenReturn(new ArrayList<>());
            when(campaign.getCampaignLocationManager()).thenReturn(mock(CampaignLocationManager.class));
            InterstellarMapPanel mapPanel = new InterstellarMapPanel(campaign, mock(CampaignGUI.class));
            assertTrue(descendantsOf(mapPanel).stream()
                  .filter(JButton.class::isInstance)
                  .map(JButton.class::cast)
                  .map(button -> button.getAccessibleContext().getAccessibleName())
                  .noneMatch(name -> "Show map symbol legend".equals(name)
                        || "Show map layer controls".equals(name)
                        || "Hide map layer controls".equals(name)));
        });
    }

    private static JComponent componentWithProperty(List<Component> components, String property, String title) {
        return components.stream()
              .filter(JComponent.class::isInstance)
              .map(JComponent.class::cast)
              .filter(component -> Boolean.TRUE.equals(component.getClientProperty(property)))
              .filter(component -> title.equals(component.getClientProperty("mapLegendTitle")))
              .findFirst()
              .orElseThrow();
    }

    private static BufferedImage renderOffscreen(JComponent component) {
        Dimension renderSize = component.getPreferredSize();
        component.setSize(renderSize);
        layoutRecursively(component);
        BufferedImage image = new BufferedImage(renderSize.width, renderSize.height,
                  BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        component.printAll(graphics);
        graphics.dispose();
        return image;
    }

    private static void layoutRecursively(Component component) {
        if (component instanceof java.awt.Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }

    private static boolean hasColorVariation(BufferedImage image) {
        int firstPixel = image.getRGB(0, 0);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != firstPixel) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int nearColorPixelCount(BufferedImage image, Color color, int minimumY) {
        int count = 0;
        for (int y = minimumY; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y), true);
                if ((pixel.getAlpha() > 0)
                      && (Math.abs(pixel.getRed() - color.getRed()) <= 30)
                      && (Math.abs(pixel.getGreen() - color.getGreen()) <= 30)
                      && (Math.abs(pixel.getBlue() - color.getBlue()) <= 30)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int maximumNearColorY(BufferedImage image, Color color) {
        int maximumY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            if (nearColorPixelCount(image.getSubimage(0, y, image.getWidth(), 1), color, 0) > 0) {
                maximumY = y;
            }
        }
        return maximumY;
    }

    private static List<Component> descendantsOf(Component root) {
        List<Component> components = new ArrayList<>();
        components.add(root);
        if (root instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                components.addAll(descendantsOf(child));
            }
        }
        return components;
    }
}
