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

package mekhq.gui;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.HeadlessClient;
import megamek.client.IClient;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.MiniReportDisplayDialog;
import megamek.client.ui.swing.IClientGUI;
import megamek.client.ui.swing.ILocalBots;
import megamek.client.ui.swing.MiniReportDisplay;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static java.lang.Math.sqrt;

public class AcarGUI implements IClientGUI, ILocalBots {

    private final Client client;
    private final MegaMekController controller;
    private final Map<String, AbstractClient> localBots;
    private final JFrame frame;
    private MiniMap minimap;
    private boolean isLoading;
    private MiniReportDisplay miniReportDisplay;
    private MiniReportDisplayDialog miniReportDisplayDialog;

    private JProgressBar progressBar;

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
        frame = new JFrame(Messages.getString("ClientGUI.mini.title"));
        frame.setMinimumSize(new Dimension(800, 600));
    }

    private enum IFF {
        OWN_FORCES,
        ALLIE,
        ENEMY;

        public static IFF getIFFStatus(Entity entity, Player player) {
            if (entity.getOwner().isEnemyOf(player)) {
                return ENEMY;
            } else if (entity.getOwner().equals(player) || entity.getOwner().getName().contains("@AI")) {
                return OWN_FORCES;
            } else {
                return ALLIE;
            }
        }
    }
    private record Blip(int x, int y, String code, IFF iff , Color color, int round) {};
    private record Line(int x1, int y1, int x2, int y2, Color color, int round) {};

    private static class MiniMap extends JPanel {
        private final IClient client;
        private final IGame game;
        private final List<Blip> blips;
        private final List<Blip> removedUnits;
        private final List<Line> lines;
        private final List<Line> attackLines;

        public MiniMap(IClient client) {
            super(new BorderLayout());
            this.client = client;
            this.game = client.getGame();
            this.blips = new ArrayList<>();
            this.removedUnits = new ArrayList<>();
            this.lines = new ArrayList<>();
            this.attackLines = new ArrayList<>();
            this.game.addGameListener(new GameListenerAdapter() {
                @Override
                public void gamePhaseChange(GamePhaseChangeEvent e) {
                    if (e.getNewPhase() == GamePhase.MOVEMENT_REPORT) {
                        update();
                    } else if (e.getNewPhase() == GamePhase.FIRING_REPORT) {
                        update();
                    } else if (e.getNewPhase() == GamePhase.END) {
                        update();
                    }
                }

                @Override
                public void gameEntityRemove(GameEntityRemoveEvent e) {
                    var coords = e.getEntity().getPosition();
                    if (coords == null) {
                        return;
                    }
                    var code = e.getEntity().getBlipID() + ":" + e.getEntity().getId();
                    removedUnits.add(
                        new Blip(coords.getX(),
                            coords.getY(),
                            "x" + code + "x",
                            IFF.getIFFStatus(e.getEntity(), client.getLocalPlayer()),
                            e.getEntity().getOwner().getColour().getColour(),
                            game.getCurrentRound()));
                }

                @Override
                public void gameTurnChange(GameTurnChangeEvent e) {
                    update();
                }

                @Override
                public void gameEntityChange(GameEntityChangeEvent e) {
                    var movePath = e.getMovePath();
                    if (movePath != null && !movePath.isEmpty()) {
                        addMovePath(new ArrayList<>(movePath), e.getOldEntity());
                    }
                }

                @Override
                public void gameNewAction(GameNewActionEvent e) {
                    EntityAction entityAction = e.getAction();
                    if (entityAction instanceof AttackAction attackAction) {
                        addAttack(attackAction);
                    }
                }
            });
        }


        public void update() {
            blips.clear();
            for (var inGameObject : game.getInGameObjects()) {
                if (inGameObject instanceof Entity entity) {
                    if (!entity.isActive() || entity.getPosition() == null) {
                        continue;
                    }
                    var coord = entity.getPosition();
                    blips.add(
                        new Blip(
                            coord.getX(),
                            coord.getY(),
                            entity.getBlipID() + ":" + entity.getId(),
                            IFF.getIFFStatus(entity, client.getLocalPlayer()),
                            entity.getOwner().getColour().getColour(),
                            game.getCurrentRound()));
                }
            }
            this.updateUI();
        }
        static public void drawArrowHead(Graphics g, int x0, int y0, int x1,
                                     int y1, int headLength, int headAngle) {
            double offs = headAngle * Math.PI / 180.0;
            double angle = Math.atan2(y0 - y1, x0 - x1);
            int[] xs = { x1 + (int) (headLength * Math.cos(angle + offs)), x1,
                x1 + (int) (headLength * Math.cos(angle - offs)) };
            int[] ys = { y1 + (int) (headLength * Math.sin(angle + offs)), y1,
                y1 + (int) (headLength * Math.sin(angle - offs)) };
            g.drawPolyline(xs, ys, 3);
        }

        private static final Color BG_COLOR = new Color(0x191f1a);
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var height = getHeight();
            var width = getWidth();

            var boardHeight = Math.max(game.getBoard().getHeight() - 1, 1);
            var boardWidth = Math.max(game.getBoard().getWidth() - 1, 1);
//            double sizeX = width / (double) boardWidth;
//            double sizeY = height / (double) boardHeight;
            setBackground(BG_COLOR);
            int xPos;
            int yPos;
            for (var blip : removedUnits) {
                switch (blip.iff()) {
                    case ALLIE -> g.setColor(Color.YELLOW);
                    case ENEMY -> g.setColor(Color.RED);
                    case OWN_FORCES -> g.setColor(Color.GREEN);
                }

                xPos = (int) (blip.x / (double) boardWidth * width);
                yPos = (int) (blip.y / (double) boardHeight * height);
//                var p1 = this.projectToView(blip.x, blip.y, sizeX, sizeY);
                g.drawRect(xPos - 6, yPos - 6, 13, 13);

                var delta = game.getCurrentRound() + 1 - blip.round;
                var alpha = delta <= 0 ? 255 : (int) (blip.color.getAlpha() / (double) delta);
                var newColor = new Color(blip.color.getRed() / 2, blip.color.getGreen() / 2, blip.color.getBlue() / 2, alpha);

                g.setColor(newColor);
                g.drawRect(xPos - 4, yPos - 4, 9, 9);
                g.drawString(blip.code, xPos - 4, yPos - 7);
            }

            for (var line : lines) {
                var delta = game.getCurrentRound() - line.round + 1;
                var alpha = delta <= 0 ? 255 : (int) (line.color.getAlpha() / (double) delta);
                var newColor = new Color(line.color.getRed(), line.color.getGreen(), line.color.getBlue(), alpha);
                if (!g.getColor().equals(newColor)) {
                    g.setColor(newColor);
                }
//                var p1 = this.projectToView(line.x1, line.y1, sizeX, sizeY);
//                var p2 = this.projectToView(line.x2, line.y2, sizeX, sizeY);
                xPos = (int) (line.x1 / (double) boardWidth * width);
                yPos = (int) (line.y1 / (double) boardHeight * height);
                var x2 = (int) (line.x2 / (double) boardWidth * width);
                var y2 = (int) (line.y2 / (double) boardHeight * height);
                g.drawLine(xPos, yPos, x2, y2);
            }
            Graphics2D g2d = (Graphics2D) g.create();
            Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{14}, 0);
            g2d.setStroke(dashed);

            for (var line : attackLines) {
                var delta = game.getCurrentRound() - line.round + 1;
                var alpha = delta <= 0 ? 255 : (int) (line.color.getAlpha() / (double) delta);
                var newColor = new Color(line.color.getRed(), line.color.getGreen(), line.color.getBlue(), alpha);
                if (!g2d.getColor().equals(newColor)) {
                    g2d.setColor(newColor);
                    g.setColor(newColor);
                }
//                var p1 = this.projectToView(line.x1, line.y1, sizeX, sizeY);
//                var p2 = this.projectToView(line.x2, line.y2, sizeX, sizeY);
                xPos = (int) (line.x1 / (double) boardWidth * width);
                yPos = (int) (line.y1 / (double) boardHeight * height);
                var x2 = (int) (line.x2 / (double) boardWidth * width);
                var y2 = (int) (line.y2 / (double) boardHeight * height);
                g2d.drawLine(xPos, yPos, x2, y2);
                drawArrowHead(g, xPos, yPos, x2, y2, 15, 30);
            }
            g2d.dispose();
            for (var blip : blips) {
                switch (blip.iff()) {
                    case ALLIE -> g.setColor(Color.YELLOW);
                    case ENEMY -> g.setColor(Color.RED);
                    case OWN_FORCES -> g.setColor(Color.GREEN);
                }
                xPos = (int) (blip.x / (double) boardWidth * width);
                yPos = (int) (blip.y / (double) boardHeight * height);
                g.fillRect(xPos - 6, yPos - 6, 13, 13);
                g.setColor(new Color(blip.color.getRed(), blip.color.getGreen(), blip.color.getBlue(), 255));
                g.fillRect(xPos - 4, yPos-4,  9, 9);
                g.drawString(blip.code, xPos -4 , yPos-7);

            }
        }

        private int[] projectToView(Coords coords, double sizeX, double sizeY) {
            var x = (int) sizeX * 3/2 * coords.getX();
            var y = (int) (sizeY * sqrt(3) * (coords.getY() + 0.5 * (coords.getX() & 1)));
            return new int[]{x, y};
        }

        private int[] projectToView(int x, int y, double sizeX, double sizeY) {
            var nx = (int) sizeX * 3/2 * x;
            var ny = (int) (sizeY * sqrt(3) * (y + 0.5 * (x & 1)));
            return new int[]{nx, ny};
        }

        public void addAttack(AttackAction ea) {
            var attacker = ea.getEntityId();
            var target = ea.getTargetId();
            if (game.getInGameObject(attacker).isPresent() && game.getInGameObject(target).isPresent()) {
                var attackerEntity = (Entity) game.getInGameObject(attacker).get();
                var targetEntity = (Entity) game.getInGameObject(target).get();
                var attackerPos = attackerEntity.getPosition();
                var targetPos = targetEntity.getPosition();
                if (attackerPos != null && targetPos != null) {
                    attackLines.add(
                        new Line(attackerPos.getX(), attackerPos.getY(),
                            targetPos.getX(), targetPos.getY(),
                            attackerEntity.getOwner().getColour().getColour(),
                            game.getCurrentRound()));
                }
            }
        }

        public void addMovePath(List<UnitLocation> unitLocations, Entity entity) {
            Coords previousCoords = entity.getPosition();
            for (var unitLocation : unitLocations) {
                var coords = unitLocation.getCoords();
                lines.add(new Line(previousCoords.getX(), previousCoords.getY(),
                    coords.getX(), coords.getY(),
                    Color.GREEN,
                    game.getCurrentRound()));
                previousCoords = coords;
            }
        }
    }

    @Override
    public void initialize() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Center: Splash image with progress bar
        JPanel centerPanel = new JPanel(new BorderLayout());
        JLabel splashImage = UIUtil.createSplashComponent(splashImages, getFrame());
        miniReportDisplay = new MiniReportDisplay(this);
        miniReportDisplayDialog = new MiniReportDisplayDialog(getFrame(), this);
        miniReportDisplayDialog.setBounds(0, 0, miniReportDisplayDialog.getWidth(),
            miniReportDisplayDialog.getHeight());
        miniReportDisplay.setMinimumSize(new Dimension(600, 600));
        miniReportDisplay.setPreferredSize(new Dimension(600, 600));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setVisible(true);
        minimap = new MiniMap(client);
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
        JButton endGame = new JButton("Request Victory");
        JButton retreat = new JButton("Retreat");
        JButton pauseContinue = new JButton("Pause / Continue");
        JButton maneuver = new JButton("Maneuver");
        JButton priorityTarget = new JButton("Priority Target");
        JButton ignoreTarget = new JButton("Ignore Target");
        endGame.setEnabled(false);
        // Add them to the buttonPanel
        buttonPanel.add(endGame);
        buttonPanel.add(retreat);
        buttonPanel.add(pauseContinue);
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
        endGame.addActionListener(e -> {
            JPopupMenu popup = createEndGamePopup();
            popup.show(endGame, 0, endGame.getHeight());
        });
        retreat.addActionListener(e -> {
            JPopupMenu popup = createRetreatPopup();
            popup.show(retreat, 0, retreat.getHeight());
        });
        pauseContinue.addActionListener(e -> client.sendPause());

        rightPanel.add(new JScrollPane(entityListEntries), BorderLayout.NORTH);
        rightPanel.add(buttonPanel, BorderLayout.CENTER);
        rightPanel.add(miniReportDisplay, BorderLayout.SOUTH);
        // Add panels to main panel
        // miniReportDisplayDialog.add(miniReportDisplay, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setVisible(true);

        // Update entity list on phase change
        client.getGame().addGameListener(new GameListenerAdapter() {

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                super.gamePhaseChange(e);
                var game = getClient().getGame();
                var round = game.getCurrentRound();
                if (e.getNewPhase() == GamePhase.VICTORY) {
                    endGame.setEnabled(true);
                    endGame.setText("Conclude Game");
                    endGame.addActionListener(evt -> {
                        client.die();
                        die();
                    });
                    progressBar.setString("Game Over");
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                } else {
                    if (round < 1) {
                        progressBar.setString("Preparing...");
                    } else {
                        if (round == 10) {
                            endGame.setEnabled(true);
                        }
                        if (isLoading) {
                            isLoading = false;
                            centerPanel.remove(0);
                            centerPanel.add(minimap, BorderLayout.CENTER, 0);
                        }

                        progressBar.setString("Round #" + game.getCurrentRound() + ": " + e.getNewPhase().localizedName());
                    }
                }
                entityListEntries.removeAll();
                entityListEntries.add(entitiesHeader);
                game.getInGameObjects().stream().filter(entity -> entity instanceof Entity).forEach(entity -> {
                    JLabel entityLabel = new JLabel(((Entity) entity).getDisplayName());
                    entityLabel.setForeground(((Entity) entity).getOwner().getColour().getColour());
                    entityListEntries.add(entityLabel);
                });
                entityListEntries.revalidate();
                entityListEntries.repaint();
            }
        });
    }
    /**
     * FLEE("fl", "princessName: flee", "Causes princess-controlled units to start fleeing the board, regardless of " +
     *                                      "damage level or Forced Withdrawal setting."),
     *     VERBOSE("ve", "princessName: verbose : <error/warning/info/debug>", "Sets princess's verbosity level."),
     *     BEHAVIOR("be", "princessName: behavior : behaviorName", "Change's princess's behavior to the named behavior."),
     *     CAUTION("ca", "princessName: caution : <+/->", "Modifies princess's Piloting Caution setting. Each '+' increases " +
     *                                                    "it by 1 and each '-' decreases it by one."),
     *     AVOID("av", "princessName: avoid : <+/->", "Modifies princess's Self Preservation setting. Each '+' increases it " +
     *                                                "by 1 and each '-' decreases it by one."),
     *     AGGRESSION("ag", "princessName: aggression : <+/->", "Modifies princess's Aggression setting. Each '+' increases " +
     *                                                          "it by 1 and each '-' decreases it by one."),
     *     HERDING("he", "princessName: herd : <+/->", "Modifies princess's Herding setting. Each '+' increases it by 1 and " +
     *                                                 "each '-' decreases it by one."),
     *     BRAVERY("br", "princessName: brave : <+/->", "Modifies princess's Bravery setting. Each '+' increases it by 1 " +
     *                                                  "and each '-' decreases it by one."),
     *     TARGET("ta", "princessName: target : hexNumber", "Adds the specified hex to princess's list of Strategic Targets."),
     *     PRIORITIZE("pr", "princessName: prioritize : unitId", "Adds the specified unit to princess's Priority Targets " +
     *                                                           "list."),
     *     SHOW_BEHAVIOR("sh", "princessName: showBehavior", "Princess will state the name of her current behavior."),
     *     LIST__COMMANDS("li", "princessName: listCommands", "Displays this list of commands."),
     *     IGNORE_TARGET("ig", "princessName: ignoreTarget: unitId", "Will not fire on the entity with this ID."),
     *     SHOW_DISHONORED("di", "princessName: dishonored", "Show the players on the dishonored enemies list.");
     *
     *     NORTH(0, Messages.getString("BotConfigDialog.northEdge")),
     *     SOUTH(1, Messages.getString("BotConfigDialog.southEdge")),
     *     WEST(2, Messages.getString("BotConfigDialog.westEdge")),
     *     EAST(3, Messages.getString("BotConfigDialog.eastEdge")),
     *     // this signals that the nearest edge to the currently selected unit should be picked
     *     NEAREST(4, Messages.getString("BotConfigDialog.nearestEdge")),
     *     // no edge
     *     NONE(5, Messages.getString("BotConfigDialog.noEdge"));
     */
    private JPopupMenu createManeuverPopup() {
        JPopupMenu popup = new JPopupMenu("Maneuver Menu");
        var aiName = getClient().getLocalPlayer().getName() + "AI";
        return popup;
    }
    private JPopupMenu createPriorityTargetPopup() {
        JPopupMenu popup = new JPopupMenu("Priority Target Menu");
        var aiName = getClient().getLocalPlayer().getName() + "@AI";
        for (var entity : client.getEntitiesVector().stream().filter(e -> e.getOwner().isEnemyOf(client.getLocalPlayer())).toList()) {
            JMenuItem targetItem = new JMenuItem(entity.getDisplayName());
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
        return popup;
    }
    private JPopupMenu createEndGamePopup() {
        JPopupMenu popup = new JPopupMenu("End Game Menu");
        var aiName = getClient().getLocalPlayer().getName() + "@AI";
        JMenuItem confirmItem = new JMenuItem("Confirm End");
        confirmItem.addActionListener(evt -> {
            client.sendChat("/victory");
        });
        popup.add(confirmItem);
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
        client.die();
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
