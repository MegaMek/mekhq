/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import java.util.Arrays;
import java.util.HashSet;

import org.commonmark.node.BlockQuote;
import org.commonmark.node.Heading;
import org.commonmark.node.ListBlock;
import org.commonmark.node.Node;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/***
 * This is a class with a single static instance that will take markdown flavored text and parse it
 * back out as HTML, using the commonmark-java library. It is intended for allowing users to mix
 * markdown and html elements in various descriptions of units, people, etc.
 * @author aarong
 */
public class MarkdownRenderer {
    private static MarkdownRenderer renderer;

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    private MarkdownRenderer() {
        // only enable certain block types
        parser = Parser.builder().enabledBlockTypes(new HashSet<>(
              Arrays.asList(Heading.class, ListBlock.class, ThematicBreak.class, BlockQuote.class))).build();
        htmlRenderer = HtmlRenderer.builder().build();
    }

    public static MarkdownRenderer getInstance() {
        if (null == renderer) {
            renderer = new MarkdownRenderer();
        }
        return renderer;
    }

    /**
     * This method renders markdown-flavored text as HTML
     *
     * @param input - a String possible containing Markdown markup (and html) to be rendered
     *
     * @return a string rendered to html
     */
    public static String getRenderedHtml(String input) {
        if (null == input) {
            return "";
        }
        Node document = getInstance().parser.parse(input);
        return getInstance().htmlRenderer.render(document);

    }
}
