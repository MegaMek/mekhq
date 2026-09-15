package mekhq.gui.stratCon.deployment;

import static mekhq.gui.stratCon.deployment.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentMode;
import mekhq.campaign.force.Formation;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.unit.Unit;

/**
 * Renders a deployment board row as a HUD card: a leading readiness ring, the force or unit name, and a muted sub-line
 * (battle value and unit count for a formation; status and battle value for a unit). Selected rows get a brighter
 * surface and an accent left bar, matching the interstellar-map chrome.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DeploymentItemRenderer implements ListCellRenderer<Object> {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private final transient Campaign campaign;

    private final JPanel card = new JPanel(new BorderLayout(UIUtil.scaleForGUI(10), 0));
    private final Ring ring = new Ring();
    private final JLabel nameLabel = new JLabel();
    private final JLabel subLabel = new JLabel();

    // A separate cached component for section-divider rows in the staged tray (see SectionHeader).
    private final JPanel header = new JPanel(new BorderLayout());
    private final JLabel headerLabel = new JLabel();

    public DeploymentItemRenderer(Campaign campaign) {
        this.campaign = campaign;

        int pad = UIUtil.scaleForGUI(8);
        card.setBorder(BorderFactory.createEmptyBorder(pad, UIUtil.scaleForGUI(10), pad, UIUtil.scaleForGUI(10)));

        headerLabel.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        headerLabel.setForeground(TEXT_FAINT);
        header.setOpaque(true);
        header.setBackground(SURFACE_DEEP);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(UIUtil.scaleForGUI(1), 0, 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(10), UIUtil.scaleForGUI(10),
                    UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(10))));
        header.add(headerLabel, BorderLayout.CENTER);

        nameLabel.setFont(hudFont(Font.BOLD, 1.0f, 0.02f));
        subLabel.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        subLabel.setForeground(TEXT_MUTED);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(HudStyle.leftAligned(nameLabel));
        text.add(HudStyle.leftAligned(subLabel));

        card.add(ring, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        if (value instanceof SectionHeader sectionHeader) {
            headerLabel.setText(sectionHeader.mode().getLabel().toUpperCase(Locale.ROOT));
            return header;
        }

        card.setOpaque(true);
        card.setBackground(isSelected ? SURFACE : SURFACE_DEEP);
        card.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, UIUtil.scaleForGUI(isSelected ? 3 : 0), 0, 0, ACCENT),
                    BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8),
                          UIUtil.scaleForGUI(isSelected ? 7 : 10),
                          UIUtil.scaleForGUI(8),
                          UIUtil.scaleForGUI(10)))));

        if (value instanceof Formation formation) {
            configureFormation(formation);
        } else if (value instanceof Unit unit) {
            configureUnit(unit);
        } else {
            ring.setColor(TEXT_MUTED);
            nameLabel.setText("");
            subLabel.setText("");
        }
        return card;
    }

    private void configureFormation(Formation formation) {
        List<OperationalStatus> statuses = formation.updateFormationIconOperationalStatus(campaign);
        ring.setColor(statuses.isEmpty() ? TEXT_MUTED : readinessColor(statuses.get(0)));
        nameLabel.setText(formation.getName());
        nameLabel.setForeground(TEXT);
        subLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.row.formationSub",
              formation.getTotalBV(campaign, true), formation.getAllUnits(true).size()));
    }

    private void configureUnit(Unit unit) {
        ring.setColor(ACCENT);
        nameLabel.setText(unit.getName());
        nameLabel.setForeground(TEXT);
        int battleValue = (unit.getEntity() == null) ? 0 : unit.getEntity().calculateBattleValue(true, true);
        subLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "deploymentWizard.row.unitSub",
              unit.getStatus(), battleValue));
    }

    private static Color readinessColor(OperationalStatus status) {
        return switch (status) {
            case FULLY_OPERATIONAL, FACTORY_FRESH -> READY;
            case SUBSTANTIALLY_OPERATIONAL -> CAUTION;
            case MARGINALLY_OPERATIONAL, NOT_OPERATIONAL -> DANGER;
        };
    }

    /**
     * A non-selectable divider row in the staged tray, captioning the group of staged items that follows it (Primary,
     * Reinforcements, Auxiliaries, Utility), so a staged primary force reads apart from staged reinforcements.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record SectionHeader(DeploymentMode mode) {}

    /** A small hollow readiness ring drawn in the status colour. */
    private static final class Ring extends JComponent {
        private Color color = TEXT_MUTED;

        private Ring() {
            int size = UIUtil.scaleForGUI(16);
            Dimension dimension = new Dimension(size, size);
            setPreferredSize(dimension);
            setMinimumSize(dimension);
            setMaximumSize(dimension);
            setOpaque(false);
        }

        private void setColor(Color color) {
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int stroke = UIUtil.scaleForGUI(3);
                g2.setStroke(new BasicStroke(stroke));
                g2.setColor(color);
                int inset = stroke;
                int diameter = Math.min(getWidth(), getHeight()) - (inset * 2);
                int y = (getHeight() - diameter) / 2;
                g2.drawOval(inset, y, diameter, diameter);
            } finally {
                g2.dispose();
            }
        }
    }
}
