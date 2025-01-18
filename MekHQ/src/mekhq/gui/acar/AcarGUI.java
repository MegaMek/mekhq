/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package mekhq.gui.acar;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.HeadlessClient;
import megamek.client.bot.princess.Princess;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.IClientGUI;
import megamek.client.ui.swing.ILocalBots;
import megamek.client.ui.swing.MiniReportDisplay;
import megamek.client.ui.swing.audio.AudioService;
import megamek.client.ui.swing.audio.SoundManager;
import megamek.client.ui.swing.audio.SoundType;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.logging.MMLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static java.awt.event.KeyEvent.VK_SPACE;

public class AcarGUI extends Thread implements IClientGUI, ILocalBots {
    private static final MMLogger logger = MMLogger.create(AcarGUI.class);
    private final Client client;
    private final MegaMekController controller;
    private final Map<String, AbstractClient> localBots;
    private final JFrame frame;
    private MiniMap minimap;
    private boolean isLoading;
    private JProgressBar progressBar;
    private boolean alive = true;
    private final AudioService audioService;

    private final TreeMap<Integer, String> splashImages = new TreeMap<>();
    {
        splashImages.put(0, Configuration.miscImagesDir() + "/acar_splash_hd.png");
    }

    public AcarGUI(Client client, MegaMekController controller) {
        this.client = client;
        if (client instanceof HeadlessClient headlessClient) {
            headlessClient.setSendDoneOnVictoryAutomatically(false);
        }

        this.controller = controller;
        this.localBots = new HashMap<>();
        this.isLoading = true;
        this.audioService = new SoundManager();
        this.audioService.loadSoundFiles();
        frame = new JFrame(Messages.getString("ClientGUI.mini.title"));
        frame.setMinimumSize(new Dimension(800, 800));
    }

    @Override
    public void run() {
        initialize();
        loop();
    }

    @Override
    public void setActive(boolean value) {
        frame.setVisible(value);
    }

    private static final long targetFrameTimeNanos = 1_000_000_000 / 60;

    private void loop() {
        long previousNanos = System.nanoTime() - 16_000_000; // artificially say it has passed 1 FPS in the first loop
        long currentNanos;
        long awaitMillis;
        long elapsedNanos;
        while (alive) {
            currentNanos = System.nanoTime();
            elapsedNanos = currentNanos - previousNanos;
            tick(elapsedNanos / 1_000_000);
            previousNanos = currentNanos;
            awaitMillis = (targetFrameTimeNanos - elapsedNanos) / 1_000_000;
            try {
                Thread.sleep(Math.max(0, awaitMillis));
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for next frame", e);
                alive = false;
            }
        }
    }

    private void tick(long deltaTime) {
        // nothing to do here for now
    }

    @Override
    public void initialize() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Center: Splash image with progress bar
        JPanel centerPanel = new JPanel(new BorderLayout());
        JLabel splashImage = UIUtil.createSplashComponent(splashImages, getFrame());
        MiniReportDisplay miniReportDisplay = new MiniReportDisplay(this);
        miniReportDisplay.setMinimumSize(new Dimension(600, 600));
        miniReportDisplay.setPreferredSize(new Dimension(600, 600));

        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setVisible(true);
        minimap = new MiniMap(client);
        var chatOverlay = new ChatOverlay(8);
        minimap.addOverlay(chatOverlay);
        centerPanel.add(splashImage, BorderLayout.CENTER);
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        // Right: List of current entities with their status
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setMinimumSize(new Dimension(600, frame.getHeight()));

        JPanel entityListEntries = new JPanel();
        JLabel entitiesHeader = new JLabel("Entities in game");
        entityListEntries.add(entitiesHeader);
        entityListEntries.setLayout(new BoxLayout(entityListEntries, BoxLayout.Y_AXIS));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 3, 2, 2));
        buttonPanel.setMinimumSize(new Dimension(600, 80));
        buttonPanel.setPreferredSize(new Dimension(-1, 80));
        buttonPanel.setMaximumSize(new Dimension(-1, 80));
        JButton endGame = new JButton("Ready");
        JButton retreat = new JButton("Retreat");
        JButton pauseContinue = new JButton("Pause Game");
        JButton maneuver = new JButton("Maneuver");
        JButton priorityTarget = new JButton("Priority Target");
        JButton ignoreTarget = new JButton("Ignore Target");
        endGame.setEnabled(false);
        // Add them to the buttonPanel
        buttonPanel.add(endGame);
        buttonPanel.add(pauseContinue);
        buttonPanel.add(retreat);
        buttonPanel.add(maneuver);
        buttonPanel.add(priorityTarget);
        buttonPanel.add(ignoreTarget);
        maneuver.addActionListener(e -> {
            JPopupMenu popup = createManeuverPopup();
            popup.show(maneuver, 0, maneuver.getHeight());
        });
        priorityTarget.addActionListener(e -> {
            JPopupMenu popup = createPriorityTargetPopup();
            popup.show(priorityTarget, 0, priorityTarget.getHeight());
        });
        ignoreTarget.addActionListener(e -> {
            JPopupMenu popup = createIgnoreTargetPopup();
            popup.show(ignoreTarget, 0, ignoreTarget.getHeight());
        });
        retreat.addActionListener(e -> {
            JPopupMenu popup = createRetreatPopup();
            popup.show(retreat, 0, retreat.getHeight());
        });

        pauseContinue.addActionListener(e -> {
            pauseUnpause(pauseContinue);
        });

        var jScroll = new JScrollPane(entityListEntries);
        jScroll.setMinimumSize(new Dimension(-1, 20));
        jScroll.setPreferredSize(new Dimension(-1, 300));

        rightPanel.add(jScroll, BorderLayout.NORTH);
        rightPanel.add(miniReportDisplay, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);
        // Add panels to main panel
        // miniReportDisplayDialog.add(miniReportDisplay, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        frame.getContentPane().add(mainPanel);
        frame.pack();
//        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (getClient().getGame().getPhase() == GamePhase.VICTORY) {
                    die();
                } else {
                    int closePrompt = JOptionPane.showConfirmDialog(null,
                        "Would you like to exit the game?",
                        Messages.getString("ClientGUI.gameSaveFirst"),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    if (closePrompt == JOptionPane.YES_OPTION) {
                        getClient().die();
                        die();
                    }
                }
            }
        });

        // Update entity list on phase change
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePlayerChat(GamePlayerChatEvent e) {
                chatOverlay.addChatMessage(e.getPlayer(), e.getMessage());
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                super.gamePhaseChange(e);
                var game = getClient().getGame();
                var round = game.getCurrentRound();
                if (e.getOldPhase() == GamePhase.LOUNGE) {
                    endGame.setEnabled(false);
                    endGame.setText("Finish Scenario");
                    var listeners = endGame.getActionListeners();
                    for (var listener : listeners) {
                        endGame.removeActionListener(listener);
                    }
                    endGame.addActionListener(evt -> {
                        client.sendChat("/victory");
                    });
                }
                if (e.getNewPhase() == GamePhase.LOUNGE) {
                    endGame.setText("Ready");
                    endGame.setEnabled(true);
                    endGame.addActionListener(evt -> {
                        client.sendDone(true);
                        endGame.setEnabled(false);
                        endGame.setText("Finish Scenario");
                    });
                } else if (e.getNewPhase() == GamePhase.VICTORY) {
                    audioService.playSound(SoundType.BING_MY_TURN);
                    endGame.setEnabled(true);
                    endGame.setText("Scenario Completed, click here to close");
                    endGame.requestFocus();
                    var listeners = endGame.getActionListeners();
                    for (var listener : listeners) {
                        endGame.removeActionListener(listener);
                    }
                    endGame.addActionListener(evt -> {
                        client.sendDone(true);
                        die();
                    });
                    progressBar.setString("Game Over");
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                } else {
                    if (round < 1) {
                        progressBar.setString("Preparing...");
                    } else {
                        if (round == 5) {
                            endGame.setEnabled(true);
                        }
                        if (isLoading) {
                            isLoading = false;
                            centerPanel.remove(0);
                            centerPanel.add(minimap, BorderLayout.CENTER, 0);
                            audioService.playSound(SoundType.BING_MY_TURN);
                        }
                        progressBar.setString("Round #" + game.getCurrentRound() + ": " + e.getNewPhase().localizedName());
                    }
                }
                entityListEntries.removeAll();
                entityListEntries.add(entitiesHeader);
                game.getInGameObjects().stream().filter(entity -> entity instanceof Entity).forEach(ent -> {
                    var entity = (Entity) ent;
                    var isCrippled = entity.isCrippled(true);
                    var entityLabelText = entity.getBlipID() + ":" + entity.getId() + " - " + entity.getDisplayName() + (isCrippled ? " (Crippled)" : "");
                    JLabel entityLabel = new JLabel(entityLabelText);
                    entityLabel.setForeground(entity.getOwner().getColour().getColour());
                    entityListEntries.add(entityLabel);
                });
                entityListEntries.revalidate();
                entityListEntries.repaint();
            }
        });
        frame.getRootPane().registerKeyboardAction(e -> pauseUnpause(pauseContinue),
            KeyStroke.getKeyStroke(VK_SPACE, InputEvent.SHIFT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void pauseUnpause(JButton pauseContinue) {
        audioService.playSound(SoundType.BING_OTHERS_TURN);
        if (pauseContinue.getText().equals("Pause Game")) {
            client.sendPause();
            pauseContinue.setText("Continue Game");
        } else {
            client.sendUnpause();
            pauseContinue.setText("Pause Game");
        }
    }

    private JPopupMenu createManeuverPopup() {
        JPopupMenu popup = new JPopupMenu("Maneuver Menu");
        var aiName = getClient().getLocalPlayer().getName() + "@AI";
        var princess = (Princess) localBots.get(aiName);

        JMenuItem noPrisoners = new JMenuItem("No Prisoners");
        noPrisoners.setToolTipText("Will not spare retreating units or ejected crew");
        noPrisoners.addActionListener(evt -> {
            princess.getBehaviorSettings().clearIgnoredUnitTargets();
            for (var player : client.getGame().getPlayersList()) {
                if (player.isEnemyOf(princess.getLocalPlayer())) {
                    princess.getHonorUtil().setEnemyDishonored(player.getId());
                }
            }
        });
        popup.add(noPrisoners);

        JMenuItem forGlory = new JMenuItem("For Glory!");
        forGlory.addActionListener(evt -> {
            princess.getBehaviorSettings().setBraveryIndex(10);
            princess.getBehaviorSettings().setHyperAggressionIndex(10);
            princess.getBehaviorSettings().setSelfPreservationIndex(0);
        });
        popup.add(forGlory);
        JMenuItem stayAtRange = new JMenuItem("Stay at Range!");
        stayAtRange.addActionListener(evt -> {
            princess.getBehaviorSettings().setBraveryIndex(4);
            princess.getBehaviorSettings().setHyperAggressionIndex(10);
            princess.getBehaviorSettings().setSelfPreservationIndex(7);
        });
        popup.add(stayAtRange);
        JMenuItem disperse = new JMenuItem("Disperse");
        disperse.addActionListener(evt -> {
            princess.getBehaviorSettings().setHerdMentalityIndex(0);
        });
        popup.add(disperse);
        JMenuItem closeFormation = new JMenuItem("Close Formation");
        closeFormation.addActionListener(evt -> {
            princess.getBehaviorSettings().setHerdMentalityIndex(8);
        });
        popup.add(closeFormation);
        JMenuItem freeFire = new JMenuItem("Free Fire");
        closeFormation.addActionListener(evt -> {
            princess.getBehaviorSettings().setSelfPreservationIndex(8);
            princess.getBehaviorSettings().setBraveryIndex(7);
            princess.getBehaviorSettings().setHyperAggressionIndex(8);
            princess.getBehaviorSettings().setFallShameIndex(5);
        });
        popup.add(freeFire);
        JMenuItem moveFast = new JMenuItem("Move Fast");
        moveFast.addActionListener(evt -> {
            princess.getBehaviorSettings().setFallShameIndex(0);
        });
        popup.add(moveFast);
        JMenuItem watchYourStep = new JMenuItem("Watch Your Step!");
        watchYourStep.addActionListener(evt -> {
            princess.getBehaviorSettings().setFallShameIndex(10);
        });
        popup.add(watchYourStep);
        popup.add(moveFast);
        JMenuItem moveAtEase = new JMenuItem("Move at Ease");
        watchYourStep.addActionListener(evt -> {
            princess.getBehaviorSettings().setFallShameIndex(5);
        });
        popup.add(watchYourStep);

        return popup;
    }

    private JPopupMenu createPriorityTargetPopup() {
        JPopupMenu popup = new JPopupMenu("Priority Target Menu");
        var aiName = getClient().getLocalPlayer().getName() + "@AI";
        for (var entity : client.getEntitiesVector().stream().filter(e -> e.getOwner().isEnemyOf(client.getLocalPlayer())).toList()) {
            JMenuItem targetItem = new JMenuItem(entity.getBlipID() + ":" + entity.getId() + " - " + entity.getDisplayName());
            targetItem.addActionListener(evt -> {
                client.sendChat(aiName + ": pr : " + entity.getId());
            });
            popup.add(targetItem);
        }
        return popup;
    }
    private JPopupMenu createIgnoreTargetPopup() {
        JPopupMenu popup = new JPopupMenu("Ignore Target Menu");
        var aiName = getClient().getLocalPlayer().getName() + "@AI";
        for (var entity : client.getEntitiesVector().stream().filter(e -> e.getOwner().isEnemyOf(client.getLocalPlayer())).toList()) {
            JMenuItem targetItem = new JMenuItem(entity.getBlipID() + ":" + entity.getId() + " - " + entity.getDisplayName());
            targetItem.addActionListener(evt -> {
                client.sendChat(aiName + ": ig : " + entity.getId());
            });
            popup.add(targetItem);
        }
        return popup;
    }

    private JPopupMenu createRetreatPopup() {
        JPopupMenu popup = new JPopupMenu("Retreat Menu");
        var aiName = getClient().getLocalPlayer().getName() + "@AI";
        JMenuItem retreatNow = new JMenuItem("Retreat Nearest Edge");
        retreatNow.addActionListener(evt -> {
            client.sendChat(aiName + ": fl : 4");
        });
        popup.add(retreatNow);
        JMenuItem retreatNorth = new JMenuItem("Retreat North");
        retreatNorth.addActionListener(evt -> {
            client.sendChat(aiName + ": fl : 0");
        });
        popup.add(retreatNorth);
        JMenuItem retreatEast = new JMenuItem("Retreat East");
        retreatEast.addActionListener(evt -> {
            client.sendChat(aiName + ": fl : 3");
        });
        popup.add(retreatEast);
        JMenuItem retreatSouth = new JMenuItem("Retreat South");
        retreatSouth.addActionListener(evt -> {
            client.sendChat(aiName + ": fl : 0");
        });
        popup.add(retreatSouth);
        JMenuItem retreatWest = new JMenuItem("Retreat West");
        retreatWest.addActionListener(evt -> {
            client.sendChat(aiName + ": fl : 2");
        });
        popup.add(retreatWest);

        return popup;
    }

    private JPopupMenu createPauseContinuePopup() {
        JPopupMenu popup = new JPopupMenu("Pause/Continue Menu");

        JMenuItem pauseItem = new JMenuItem("Pause Game");
        pauseItem.addActionListener(evt -> client.sendPause());
        popup.add(pauseItem);

        JMenuItem continueItem = new JMenuItem("Continue Game");
        continueItem.addActionListener(evt -> {
            // Your continue logic
            System.out.println("Game continued.");
        });
        popup.add(continueItem);

        return popup;
    }


    @Override
    public JFrame getFrame() {
        return frame;
    }

    @Override
    public boolean shouldIgnoreHotKeys() {
        return false;
    }

    @Override
    public void die() {
        frame.dispose();
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public JComponent turnTimerComponent() {
        return null;
    }

    @Override
    public void setChatBoxActive(boolean active) {

    }

    @Override
    public void clearChatBox() {

    }

    @Override
    public Map<String, AbstractClient> getLocalBots() {
        return localBots;
    }
}
