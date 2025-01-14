package mekhq.gui.campaignOptions.factoryClasses;

import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class CampaignOptionsGridBagConstraints extends GridBagConstraints {
    /**
     * Creates a {@link GridBagConstraints} object for the specified {@link JPanel}.
     * <p>
     * Written to be paired with {@code CampaignOptionsStandardPanel}.
     *
     * @param panel the {@link JPanel} for which the {@link GridBagConstraints} is created
     */
    public CampaignOptionsGridBagConstraints(JPanel panel) {
        this(panel, null, null);
    }

    /**
     * Creates a {@link GridBagConstraints} object for the specified {@link JPanel} according to the
     * provided settings.
     * <p>
     * Written to be paired with {@code CampaignOptionsStandardPanel}.
     *
     * @param panel the {@link JPanel} for which the {@link GridBagConstraints} is created
     * @param anchor the anchor setting for the {@link GridBagConstraints}, or {@code null} to use
     *              the default value {@link GridBagConstraints#NORTHWEST}
     * @param fill the fill setting for the {@link GridBagConstraints}, or {@code null} to use the
     *            default value {@link GridBagConstraints#NORTHWEST}
     */
    public CampaignOptionsGridBagConstraints(JPanel panel, @Nullable Integer anchor, @Nullable Integer fill) {
        super();
        panel.setLayout(new GridBagLayout());

        this.anchor = Objects.requireNonNullElse(anchor, GridBagConstraints.NORTHWEST);
        this.fill = Objects.requireNonNullElse(fill, GridBagConstraints.BOTH);

        this.insets = new Insets(5, 5, 5, 5);
    }
}
