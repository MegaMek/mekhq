/*
 * Copyright (C) 2016-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.view;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.personnel.enums.BodyLocation;
import mekhq.gui.utilities.MultiplyComposite;
import org.apache.logging.log4j.LogManager;

import javax.xml.transform.Source;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

/**
 * A component allowing to display a "paper doll" image, with overlays
 * for body locations.
 */
public class Paperdoll extends Component {
    public static final int DEFAULT_WIDTH = 256;
    public static final int DEFAULT_HEIGHT = 768;

    private transient ActionListener listener;

    private Image base;
    private Map<BodyLocation, Path2D> locShapes;
    private Map<BodyLocation, Color> locColors;
    private Map<BodyLocation, Map<String, Image>> locOverlays;
    private Map<BodyLocation, String> locTags;
    private Color highlightColor;

    private transient BodyLocation hoverLoc;
    private transient double scale;

    // TODO: Make this work with any enum, not just BodyLocation
    public Paperdoll(InputStream is) {
        locShapes = new EnumMap<>(BodyLocation.class);
        locColors = new EnumMap<>(BodyLocation.class);
        locOverlays = new EnumMap<>(BodyLocation.class);
        locTags = new EnumMap<>(BodyLocation.class);

        try {
            loadShapeData(is);
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
        }

        highlightColor = null;

        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public void loadShapeData(InputStream is) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(OverlayLocDataList.class, OverlayLocData.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Source inputSource = MHQXMLUtility.createSafeXmlSource(is);
        OverlayLocDataList dataList = (OverlayLocDataList) unmarshaller.unmarshal(inputSource);
        if (null != dataList.locs) {
            dataList.locs.forEach(data -> {
                locShapes.put(data.loc, data.genPath());
                if (null != data.overlayImages) {
                    data.overlayImages.forEach(imgSpec -> {
                        Map<String, Image> overlayMap = locOverlays.computeIfAbsent(data.loc, k -> new HashMap<>());
                        Image img = Toolkit.getDefaultToolkit().createImage(imgSpec.image);
                        overlayMap.put(imgSpec.tag, img);
                    });
                }
            });
        }
        if ((null != dataList.base) && !dataList.base.isEmpty()) {
            base = Toolkit.getDefaultToolkit().createImage(dataList.base);
            MediaTracker mt = new MediaTracker(this);
            mt.addImage(base, 0);
            try {
                mt.waitForAll();
            } catch (InterruptedException e) {
                LogManager.getLogger().error("", e);
            }
        } else {
            base = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }
        setSize(base.getWidth(null), base.getHeight(null));
    }

    public void setLocShape(BodyLocation loc, Path2D path) {
        Objects.requireNonNull(loc);
        if (null != path) {
            locShapes.put(loc, (Path2D) path.clone());
        } else {
            locShapes.remove(loc);
        }
        invalidate();
    }

    public void setLocColor(BodyLocation loc, Color color) {
        Objects.requireNonNull(loc);
        Color oldColor = locColors.get(loc);
        locColors.put(loc, color);
        if (!Objects.equals(color, oldColor)) {
            invalidate();
        }
    }

    public void setLocTag(BodyLocation loc, String tag) {
        Objects.requireNonNull(loc);
        String oldTag = locTags.get(loc);
        if (null == tag) {
            locTags.remove(loc);
        } else {
            locTags.put(loc, tag);
        }

        if (!Objects.equals(tag, oldTag)) {
            invalidate();
        }
    }

    public void clearLocColors() {
        locColors.clear();
    }

    public void clearLocTags() {
        locTags.clear();
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        if (!Objects.equals(this.highlightColor, highlightColor)) {
            invalidate();
        }
        this.highlightColor = highlightColor;
    }

    @Override
    public void paint(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        final int imgWidth = base.getWidth(null);
        final int imgHeight = base.getHeight(null);
        scale = Math.min(getWidth() * 1.0 / imgWidth, getHeight() * 1.0 / imgHeight);
        final int scaledWidth = (int) Math.round(imgWidth * scale);
        final int scaledHeight = (int) Math.round(imgHeight * scale);
        g2.drawImage(base, 0, 0, scaledWidth, scaledHeight, this);
        // Check for image overlays first, and record what we have drawn
        Set<BodyLocation> drawnOverlays = EnumSet.noneOf(BodyLocation.class);
        locTags.entrySet().stream().filter(Objects::nonNull)
            .filter(entry -> ((null != entry.getValue()) && locOverlays.containsKey(entry.getKey())
                && locOverlays.get(entry.getKey()).containsKey(entry.getValue())))
            .forEach(entry -> {
                final Image image = locOverlays.get(entry.getKey()).get(entry.getValue());
                g2.drawImage(image, 0, 0, scaledWidth, scaledHeight, this);
                drawnOverlays.add(entry.getKey());
            });
        g2.scale(scale, scale);
        locColors.entrySet().stream().filter(Objects::nonNull)
            .filter(entry -> ((null != entry.getValue()) && locShapes.containsKey(entry.getKey())
                && !drawnOverlays.contains(entry.getKey())))
            .forEach(entry -> {
                final Path2D overlay = locShapes.get(entry.getKey());
                g2.setPaint(entry.getValue());
                g2.setComposite(MultiplyComposite.INSTANCE);

                // The try catch is required because of a Java bug: https://bugs.openjdk.java.net/browse/JDK-6689349
                // It falls back to just overwriting everything below, instead of nicely merging
                try {
                    g2.fill(overlay);
                } catch (InternalError ignored) {
                    g2.setComposite(AlphaComposite.SrcOver);
                    g2.fill(overlay);
                }
            });
        g2.setComposite(AlphaComposite.SrcOver); // Revert to default composite

        if ((null != highlightColor) && (null != hoverLoc) && locShapes.containsKey(hoverLoc)) {
            g2.setPaint(highlightColor);
            g2.setStroke(new BasicStroke(5f));
            g2.draw(locShapes.get(hoverLoc));
        }
    }

    public BodyLocation locationUnderPoint(double x, double y) {
        final double scaledX = x / scale;
        final double scaledY = y / scale;
        return locShapes.entrySet().stream()
            .filter(entry -> entry.getValue().contains(scaledX, scaledY)).findAny()
            .map(Map.Entry::getKey).orElse(BodyLocation.GENERIC);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(base.getWidth(null), base.getHeight(null));
    }

    public void addActionListener(ActionListener al) {
        listener = AWTEventMulticaster.add(listener, al);
    }

    public void removeActionListener(ActionListener al) {
        listener = AWTEventMulticaster.remove(listener, al);
    }

    @Override
    public void processEvent(AWTEvent e) {
        if (e instanceof MouseEvent) {
            final MouseEvent event = (MouseEvent) e;
            if ((event.getID() == MouseEvent.MOUSE_MOVED) || (event.getID() == MouseEvent.MOUSE_ENTERED)) {
                BodyLocation oldHoverLoc = hoverLoc;
                hoverLoc = locationUnderPoint(event.getX(), event.getY());
                if (oldHoverLoc != hoverLoc) {
                    repaint();
                }
            }
            if (event.getID() == MouseEvent.MOUSE_EXITED) {
                hoverLoc = null;
                repaint();
            }
            if (event.getButton() == MouseEvent.BUTTON1) {
                if (event.getID() == MouseEvent.MOUSE_CLICKED) {
                    if ((null != listener) && (null != hoverLoc)) {
                        ActionEvent myEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, hoverLoc.toString());
                        listener.actionPerformed(myEvent);
                    }
                }
            }
        }
    }

    // XML serialization classes
    @XmlRootElement(name="overlays")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class OverlayLocDataList {
        public String base;
        @XmlElement(name="loc")
        public List<OverlayLocData> locs;
    }

    @XmlRootElement(name="loc")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OverlayLocData {
        @XmlAttribute(name="type")
        public BodyLocation loc;
        @XmlElement(name="p")
        @XmlElementWrapper(name="path")
        @XmlJavaTypeAdapter(XMLPoint2DAdapter.class)
        public List<Point2D> path;
        @XmlElement(name="image")
        public List<OverlayLocImage> overlayImages;

        public Path2D genPath() {
            Path2D result = new Path2D.Float();
            if ((null != path) && !path.isEmpty()) {
                result.moveTo(path.get(0).getX(), path.get(0).getY());
                IntStream.range(1, path.size()).mapToObj(i -> path.get(i))
                    .forEachOrdered(p -> result.lineTo(p.getX(), p.getY()));
                result.closePath();
            }
            return result;
        }
    }

    public static class OverlayLocImage {
        @XmlAttribute
        public String tag;
        @XmlValue
        public String image;
    }

    private static class XMLPoint2DAdapter extends XmlAdapter<String, Point2D> {
        @Override
        public Point2D unmarshal(String v) throws Exception {
            if ((null == v) || v.isEmpty()) {
                return null;
            }
            String[] data = v.split(",", 2);
            if (data.length < 2) {
                return null;
            }
            try {
                return new Point2D.Float(Float.parseFloat(data[0]), Float.parseFloat(data[1]));
            } catch (NumberFormatException ignored) {
                // Oh well, we tried
            }
            return null;
        }

        @Override
        public String marshal(Point2D v) throws Exception {
            return (null != v) ? String.format(Locale.ROOT, "%.3f,%.3f", v.getX(), v.getY()) : null;
        }

    }
}
