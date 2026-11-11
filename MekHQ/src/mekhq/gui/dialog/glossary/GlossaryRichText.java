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
package mekhq.gui.dialog.glossary;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;

/**
 * Reads a glossary definition's HTML into blocks of styled text a page layout can set: headings, paragraphs, list
 * items, quotations and table rows, each made of runs that are bold, italic, or a link.
 *
 * <p>Only what glossary text uses is understood. Anything else contributes its text and nothing more.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GlossaryRichText {
    private static final MMLogger LOGGER = MMLogger.create(GlossaryRichText.class);

    private GlossaryRichText() {
    }

    /** What a block is. */
    enum Kind {
        HEADING,
        PARAGRAPH,
        LIST_ITEM,
        TABLE_ROW
    }

    /**
     * A stretch of text in one style. A run whose text is a line break forces a new line.
     *
     * @param text   the text
     * @param bold   {@code true} for bold
     * @param italic {@code true} for italic
     * @param link   the link's target, such as {@code GLOSSARY:KEY}, or {@code null}
     */
    record Run(String text, boolean bold, boolean italic, @Nullable String link) {
        static final String LINE_BREAK = "\n";

        boolean isLineBreak() {
            return LINE_BREAK.equals(text);
        }
    }

    /**
     * One block of text.
     *
     * @param kind   what the block is
     * @param level  a heading's level, from 1, or a list item's depth, from 1; otherwise 0
     * @param marker a list item's bullet or number, otherwise empty
     * @param quoted {@code true} if the block sits in a quotation
     * @param centered {@code true} if the block is centred
     * @param runs   the block's text, for every kind but a table row
     * @param cells  a table row's cells, each a list of runs; empty for other kinds
     * @param header {@code true} for a table row of header cells
     */
    record Block(Kind kind, int level, String marker, boolean quoted, boolean centered, List<Run> runs,
          List<List<Run>> cells, boolean header) {
        /** @return the block's text with styles dropped and white space collapsed */
        String plainText() {
            StringBuilder text = new StringBuilder();
            List<List<Run>> parts = kind == Kind.TABLE_ROW ? cells : List.of(runs);
            for (List<Run> part : parts) {
                if (!text.isEmpty()) {
                    text.append(' ');
                }
                part.forEach(run -> text.append(run.text()));
            }
            return text.toString().replaceAll("\\s+", " ").strip();
        }
    }

    /**
     * Reads HTML into blocks. Blocks with no visible text are dropped.
     *
     * @param html the HTML
     *
     * @return the blocks, in reading order
     */
    static List<Block> parse(String html) {
        Reader reader = new Reader();
        try {
            new ParserDelegator().parse(new StringReader(html), reader, true);
        } catch (IOException ex) {
            LOGGER.warn(ex, "Unable to read glossary text");
        }
        reader.endBlock();
        return reader.blocks;
    }

    private static final class Reader extends HTMLEditorKit.ParserCallback {
        private final List<Block> blocks = new ArrayList<>();
        private final Deque<int[]> lists = new ArrayDeque<>();
        private final Deque<Boolean> orderedLists = new ArrayDeque<>();

        private @Nullable List<Run> runs;
        private Kind kind = Kind.PARAGRAPH;
        private int level;
        private String marker = "";
        private boolean centered;
        private int bold;
        private int italic;
        private int quoted;
        private @Nullable String link;

        private @Nullable List<List<Run>> row;
        private @Nullable List<Run> cell;
        private boolean headerRow;

        @Override
        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.CENTER) {
                startBlock(Kind.PARAGRAPH, 0, "", isCentered(tag, attributes));
            } else if (tag == HTML.Tag.H1 || tag == HTML.Tag.H2 || tag == HTML.Tag.H3 || tag == HTML.Tag.H4
                             || tag == HTML.Tag.H5 || tag == HTML.Tag.H6) {
                startBlock(Kind.HEADING, headingLevel(tag), "", isCentered(tag, attributes));
            } else if (tag == HTML.Tag.UL || tag == HTML.Tag.OL) {
                endBlock();
                lists.push(new int[] { 0 });
                orderedLists.push(tag == HTML.Tag.OL);
            } else if (tag == HTML.Tag.LI) {
                int depth = Math.max(1, lists.size());
                String itemMarker = "\u2022";
                if (!lists.isEmpty() && Boolean.TRUE.equals(orderedLists.peek())) {
                    itemMarker = (++lists.peek()[0]) + ".";
                }
                startBlock(Kind.LIST_ITEM, depth, itemMarker, false);
            } else if (tag == HTML.Tag.BLOCKQUOTE) {
                endBlock();
                quoted++;
            } else if (tag == HTML.Tag.B || tag == HTML.Tag.STRONG) {
                bold++;
            } else if (tag == HTML.Tag.I || tag == HTML.Tag.EM) {
                italic++;
            } else if (tag == HTML.Tag.A) {
                Object href = attributes.getAttribute(HTML.Attribute.HREF);
                link = href == null ? null : href.toString();
            } else if (tag == HTML.Tag.TR) {
                endBlock();
                row = new ArrayList<>();
                headerRow = false;
            } else if (tag == HTML.Tag.TD || tag == HTML.Tag.TH) {
                if (row == null) {
                    row = new ArrayList<>();
                }
                cell = new ArrayList<>();
                if (tag == HTML.Tag.TH) {
                    headerRow = true;
                }
            }
        }

        @Override
        public void handleEndTag(HTML.Tag tag, int position) {
            if (tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.CENTER || tag == HTML.Tag.LI
                      || tag == HTML.Tag.H1 || tag == HTML.Tag.H2 || tag == HTML.Tag.H3 || tag == HTML.Tag.H4
                      || tag == HTML.Tag.H5 || tag == HTML.Tag.H6) {
                endBlock();
            } else if (tag == HTML.Tag.UL || tag == HTML.Tag.OL) {
                endBlock();
                if (!lists.isEmpty()) {
                    lists.pop();
                    orderedLists.pop();
                }
            } else if (tag == HTML.Tag.BLOCKQUOTE) {
                endBlock();
                quoted = Math.max(0, quoted - 1);
            } else if (tag == HTML.Tag.B || tag == HTML.Tag.STRONG) {
                bold = Math.max(0, bold - 1);
            } else if (tag == HTML.Tag.I || tag == HTML.Tag.EM) {
                italic = Math.max(0, italic - 1);
            } else if (tag == HTML.Tag.A) {
                link = null;
            } else if (tag == HTML.Tag.TD || tag == HTML.Tag.TH) {
                endCell();
            } else if (tag == HTML.Tag.TR) {
                endRow();
            } else if (tag == HTML.Tag.TABLE) {
                endRow();
            }
        }

        @Override
        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.BR) {
                List<Run> target = cell != null ? cell : runs;
                if (target != null && !target.isEmpty()) {
                    target.add(new Run(Run.LINE_BREAK, false, false, null));
                }
            } else if (tag == HTML.Tag.HR) {
                endBlock();
            }
        }

        @Override
        public void handleText(char[] data, int position) {
            String text = new String(data);
            Run run = new Run(text, bold > 0, italic > 0, link);
            if (cell != null) {
                cell.add(run);
                return;
            }
            if (runs == null) {
                startBlock(Kind.PARAGRAPH, 0, "", false);
            }
            runs.add(run);
        }

        private void startBlock(Kind newKind, int newLevel, String newMarker, boolean isCentered) {
            endBlock();
            runs = new ArrayList<>();
            kind = newKind;
            level = newLevel;
            marker = newMarker;
            centered = isCentered;
        }

        private void endBlock() {
            if (runs == null) {
                return;
            }
            // A trailing line break adds nothing at the end of a block.
            while (!runs.isEmpty() && runs.get(runs.size() - 1).isLineBreak()) {
                runs.remove(runs.size() - 1);
            }
            Block block = new Block(kind, level, marker, quoted > 0, centered, List.copyOf(runs), List.of(), false);
            if (!block.plainText().isEmpty()) {
                blocks.add(block);
            }
            runs = null;
        }

        private void endCell() {
            if (row != null && cell != null) {
                row.add(List.copyOf(cell));
            }
            cell = null;
        }

        private void endRow() {
            endCell();
            if (row != null && !row.isEmpty()) {
                Block block = new Block(Kind.TABLE_ROW, 0, "", quoted > 0, false, List.of(), List.copyOf(row),
                      headerRow);
                if (!block.plainText().isEmpty()) {
                    blocks.add(block);
                }
            }
            row = null;
        }

        private static int headingLevel(HTML.Tag tag) {
            if (tag == HTML.Tag.H1) {
                return 1;
            } else if (tag == HTML.Tag.H2) {
                return 2;
            }
            return 3;
        }

        private static boolean isCentered(HTML.Tag tag, MutableAttributeSet attributes) {
            if (tag == HTML.Tag.CENTER) {
                return true;
            }
            Object style = attributes.getAttribute(HTML.Attribute.STYLE);
            Object align = attributes.getAttribute(HTML.Attribute.ALIGN);
            boolean styledCenter = style != null
                                         && style.toString().replace(" ", "").toLowerCase(Locale.ROOT)
                                                  .contains("text-align:center");
            return styledCenter || (align != null && "center".equalsIgnoreCase(align.toString()));
        }
    }
}
