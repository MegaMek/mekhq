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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import mekhq.campaign.utilities.glossary.DocumentationEntry;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Turns one of MekHQ's Markdown guides into HTML for the glossary console's reader.
 *
 * <p>The guides are written in GitHub-flavored Markdown, so they read well on GitHub too: tables are supported, and
 * a link to {@code #a-heading} jumps to that heading. The reader is a Swing HTML pane, which knows neither heading
 * ids nor table sections, so each heading gets a named anchor of its GitHub id, and {@code thead} and {@code tbody}
 * are left out. A guide's opening level-one heading is dropped too, as the console shows the guide's title above
 * the text.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class GuideMarkdown {
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();
    /** Table section tags, which Swing's HTML pane does not understand. */
    private static final Pattern TABLE_SECTIONS = Pattern.compile("</?t(?:head|body)>\\n?");
    /** What GitHub strips from a heading to make its id: everything but letters, digits, spaces, - and _. */
    private static final Pattern NOT_ID_CHARACTERS = Pattern.compile("[^\\p{L}\\p{N} _-]");

    private GuideMarkdown() {
    }

    /**
     * @param entry a guide
     *
     * @return the guide's file
     */
    static File fileFor(DocumentationEntry entry) {
        return new File(entry.getFileAddress());
    }

    /**
     * Reads a guide and turns it into HTML.
     *
     * @param entry the guide
     *
     * @return the guide as HTML, without an {@code html} or {@code body} element
     *
     * @throws IOException if the guide's file cannot be read
     */
    static String read(DocumentationEntry entry) throws IOException {
        return toHtml(Files.readString(fileFor(entry).toPath(), StandardCharsets.UTF_8));
    }

    /**
     * Turns Markdown into HTML the reader can show.
     *
     * @param markdown the Markdown
     *
     * @return the HTML, without an {@code html} or {@code body} element
     */
    static String toHtml(String markdown) {
        Node document = PARSER.parse(markdown);
        Node first = document.getFirstChild();
        if (first instanceof Heading heading && heading.getLevel() == 1) {
            heading.unlink();
        }
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                HtmlInline anchor = new HtmlInline();
                anchor.setLiteral("<a name='" + headingId(heading) + "'></a>");
                heading.prependChild(anchor);
            }
        });
        return TABLE_SECTIONS.matcher(RENDERER.render(document)).replaceAll("");
    }

    /**
     * The id GitHub gives a heading, so that a guide's {@code #links} work in the reader as they do on GitHub: the
     * heading's text in lower case, with punctuation dropped and each space turned into a hyphen.
     *
     * @param heading a heading
     *
     * @return its id
     */
    static String headingId(Heading heading) {
        StringBuilder text = new StringBuilder();
        heading.accept(new AbstractVisitor() {
            @Override
            public void visit(Text node) {
                text.append(node.getLiteral());
            }

            @Override
            public void visit(Code node) {
                text.append(node.getLiteral());
            }
        });
        return NOT_ID_CHARACTERS.matcher(text.toString().strip().toLowerCase(Locale.ROOT)).replaceAll("")
                     .replace(' ', '-');
    }
}
