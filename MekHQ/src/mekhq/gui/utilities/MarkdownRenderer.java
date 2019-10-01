/* Copyright (C) 2019 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.utilities;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/***
 * This is a class with a single static instance that will take markdown flavored text and parse it 
 * back out as HTML, using the commonmark-java library. It is intended for allowing users to mix markdown
 * and html elements in various descriptions of units, people, etc.
 * @author aarong
 *
 */
public class MarkdownRenderer {
    private static MarkdownRenderer renderer;
    
    private Parser parser;
    private HtmlRenderer htmlRenderer; 
    
    private MarkdownRenderer() {
        parser = Parser.builder().build();
        htmlRenderer = HtmlRenderer.builder().build();
    }
    
    public static MarkdownRenderer getInstance() {
        if(null == renderer) {
            renderer = new MarkdownRenderer();
        }
        return renderer;
    }
    
    /**
     * This method renders markdown-flavored text as HTML
     * @param input - a String possible containing markdown markup (and html) to be rendered
     * @return a string rendered to html
     */
    public static String getRenderedHtml(String input) {
        if(null == input) {
            return "";
        }
        Node document = getInstance().parser.parse(input);
        return getInstance().htmlRenderer.render(document);
        
    } 
}