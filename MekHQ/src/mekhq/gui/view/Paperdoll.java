/*
 * Copyright (C) 2016 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.view;

import java.awt.AWTEvent;
import java.awt.AWTEventMulticaster;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import mekhq.MekHQ;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.gui.utilities.MultiplyComposite;

/**
 * A component allowing to display a "paper doll" image, with overlays
 * for body locations.
 */
public class Paperdoll extends Component {
    private static final long serialVersionUID = 2427542264332728643L;
    
    public static final int DEFAULT_WIDTH = 256;
    public static final int DEFAULT_HEIGHT = 768;
    
    private transient ActionListener listener;
    
    private Image base;
    private Map<BodyLocation, Path2D> locOverlays;
    private Map<BodyLocation, Color> locColors;

    private transient double scale;
    
    public Paperdoll(InputStream is) {
        locOverlays = new EnumMap<>(BodyLocation.class);
        locColors = new EnumMap<>(BodyLocation.class);
        
        try {
            loadShapeData(is);
        } catch(Exception ex) {
            MekHQ.logError(ex);
        }
        
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }
    
    public void loadShapeData(InputStream is) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(OverlayLocDataList.class, OverlayLocData.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        OverlayLocDataList dataList = (OverlayLocDataList) unmarshaller.unmarshal(is);
        if(null != dataList.locs) {
            dataList.locs.forEach(data -> {
                locOverlays.put(data.loc, data.genPath());
            });
        }
        if((null != dataList.base) && !dataList.base.isEmpty()) {
            base = Toolkit.getDefaultToolkit().createImage(dataList.base);
            MediaTracker mt = new MediaTracker(this);
            mt.addImage(base, 0);
            try {
                mt.waitForAll();
            } catch(InterruptedException iex) {
                MekHQ.logError(iex);
            }
        } else {
            base = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }
        setSize(base.getWidth(null), base.getHeight(null));
    }
    
    public void setLocShape(BodyLocation loc, Path2D path) {
        Objects.requireNonNull(loc);
        if(null != path) {
            locOverlays.put(loc, (Path2D) path.clone());
        } else {
            locOverlays.remove(loc);
        }
        invalidate();
    }
    
    public void setLocColor(BodyLocation loc, Color color) {
        Objects.requireNonNull(loc);
        Color oldColor = locColors.get(loc);
        locColors.put(loc, color);
        if(!Objects.equals(color, oldColor)) {
            invalidate();
        }
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
        g2.drawImage(base, 0, 0, (int) Math.round(imgWidth * scale), (int) Math.round(imgHeight * scale), this);
        g2.scale(scale, scale);
        locColors.entrySet().stream().filter(Objects::nonNull)
            .filter(entry -> ((null != entry.getValue()) && locOverlays.containsKey(entry.getKey())))
            .forEach(entry -> {
                final Path2D overlay = locOverlays.get(entry.getKey());
                g2.setPaint(entry.getValue());
                g2.setComposite(MultiplyComposite.INSTANCE);
                g2.fill(overlay);
            });
        g2.setComposite(AlphaComposite.SrcOver);
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
        if(e instanceof MouseEvent) {
            final MouseEvent event = (MouseEvent) e;
            if(event.getButton() == MouseEvent.BUTTON1) {
                if(event.getID() == MouseEvent.MOUSE_CLICKED) {
                    if(null != listener) {
                        final int x = (int) Math.round(event.getX() / scale);
                        final int y = (int) Math.round(event.getY() / scale);
                        BodyLocation loc = locOverlays.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(x, y)).findAny()
                            .map(entry -> entry.getKey()).orElse(BodyLocation.GENERIC);
                        ActionEvent myEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, loc.toString());
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
        @XmlAttribute(name="missing")
        public String missingImageOverlay;
        
        public Path2D genPath() {
            Path2D result = new Path2D.Float();
            if(!path.isEmpty()) {
                result.moveTo(path.get(0).getX(), path.get(0).getY());
                IntStream.range(1, path.size()).mapToObj(i -> path.get(i))
                    .forEachOrdered(p -> result.lineTo(p.getX(), p.getY()));
                result.closePath();
            }
            return result;
        }
    }
    
    private static class XMLPoint2DAdapter extends XmlAdapter<String, Point2D> {
        @Override
        public Point2D unmarshal(String v) throws Exception {
            if((null == v) || v.isEmpty()) {
                return null;
            }
            String data[] = v.split(",", 2);
            if(data.length < 2) {
                return null;
            }
            try {
                return new Point2D.Float(Float.parseFloat(data[0]), Float.parseFloat(data[1]));
            } catch(NumberFormatException nfex) {
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
