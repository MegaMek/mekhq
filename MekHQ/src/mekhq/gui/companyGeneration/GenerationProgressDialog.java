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
package mekhq.gui.companyGeneration;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import megamek.client.ratgenerator.Ruleset;
import megamek.logging.MMLogger;

/**
 * Modal dialog with an indeterminate progress bar and a status label that the Company Generation
 * pipeline updates as it moves through its eight stages.
 *
 * <p>A Star League Defense Force Army takes long enough to generate that without feedback, users
 * assume the application has frozen. This dialog opens before {@code CompanyGenerator.generate}
 * begins, the generator runs on a {@link javax.swing.SwingWorker} background thread, and the worker
 * calls {@link #setStatus(String)} (or pushes through the {@link Ruleset.ProgressListener}) as each
 * stage starts. The dialog disposes itself when {@link #finish()} is called.</p>
 *
 * <p>The bar is intentionally indeterminate (no fraction): ratgen's {@code processRoot} reports its
 * own fraction-based progress, but mixing that with the walker / icon / polish stages we run
 * afterwards leads to a jumpy bar that hits 100% mid-pipeline. An indeterminate animation plus a
 * descriptive status line ("Generating units..." / "Building tree..." / "Applying icons...") gives
 * the user the same "I'm not frozen" signal without the misleading numbers.</p>
 */
public class GenerationProgressDialog extends JDialog {

    private static final MMLogger LOGGER = MMLogger.create(GenerationProgressDialog.class);

    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final long createdAtNanos = System.nanoTime();

    public GenerationProgressDialog(JFrame parent) {
        super(parent, "Generating Force", Dialog.ModalityType.APPLICATION_MODAL);
        LOGGER.info("[ProgressDialog] constructed (thread={})", Thread.currentThread().getName());

        statusLabel = new JLabel("Initializing...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        content.add(statusLabel, BorderLayout.NORTH);
        content.add(progressBar, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // No cancel button: mid-pipeline cancellation would leave the campaign with partial state
        // (units added, formations registered, etc.). When polish lands and rollback becomes
        // feasible we can wire a Cancel here.
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setMinimumSize(UIUtil.scaleForGUI(400, 120));
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Sets the status text shown above the progress bar. Safe to call from any thread; the update
     * is dispatched through {@link SwingUtilities#invokeLater(Runnable)}.
     */
    public void setStatus(String message) {
        long elapsedMs = (System.nanoTime() - createdAtNanos) / 1_000_000;
        String callerThread = Thread.currentThread().getName();
        LOGGER.info("[ProgressDialog] setStatus '{}' (caller={} elapsed={}ms)",
              message, callerThread, elapsedMs);
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            LOGGER.info("[ProgressDialog]   statusLabel.setText '{}' applied (EDT)", message);
        });
    }

    /**
     * Adapter for the ratgen {@link Ruleset.ProgressListener} interface — translates ratgen's
     * fraction-plus-message callbacks into status-label updates. The fraction is ignored because the
     * bar is indeterminate.
     */
    public Ruleset.ProgressListener asListener() {
        return (progress, message) -> setStatus(message);
    }

    /**
     * Closes the dialog. Safe to call from any thread.
     */
    public void finish() {
        long elapsedMs = (System.nanoTime() - createdAtNanos) / 1_000_000;
        LOGGER.info("[ProgressDialog] finish requested (caller={} elapsed={}ms)",
              Thread.currentThread().getName(), elapsedMs);
        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
            LOGGER.info("[ProgressDialog] disposed (EDT)");
        });
    }

    /**
     * Overridden so callers can rely on {@code preferredSize.width >= 400}. Without this Swing
     * shrinks the dialog around the (initially short) status label.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Dimension min = getMinimumSize();
        return new Dimension(Math.max(d.width, min.width), Math.max(d.height, min.height));
    }
}
