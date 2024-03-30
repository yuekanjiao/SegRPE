/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Yuekan Jiao
 */
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.FileInfo;
import ij.measure.Calibration;

import ij.measure.Measurements;
import ij.measure.ResultsTable;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.ParticleAnalyzer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.UIManager;

public class SegRPE_ implements PlugIn, ActionListener, ImageListener, ItemListener {

    ImagePlus imp = null;
    int width, height;
    double pixelWidth;
    double pixelHeight;
    Frame frame;
    Point[] points;
    ImagePlus impPoly;
    FileInfo fInfo = null;
    String imagePath = null;
    String imageName = null;
    String imgName = null;
    int numberChannels = 7;
    int nChannels;
    ImagePlus impNucleus;
    Overlay overlay;
    String[] labels = {"(cell)", "(nucleus)"};
    ArrayList<String> labelList;
    double[][] areaArray;
    ArrayList<Integer> channelList;
    double[][][] resultArray;
    ImageProcessor ipPoints;
    int radius;
    TextField radiusField;
    Choice choice;
    int selectedIndex;
    Checkbox edgeBox;
    Checkbox[] checkboxArray;
    Button checkPolyButton;
    Boolean boolCheckPoly;
    Boolean boolShowLabels;

    public void run(String arg) {

        //Set blackBackground false for binary images, to make sure 
        //objects are black (0)  
        try {
            Field field = Prefs.class.getDeclaredField("blackBackground");
            field.setAccessible(true);
            field.setBoolean(Prefs.blackBackground, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ImagePlus.addImageListener(this);

        radius = 5;
        labelList = new ArrayList<String>();
        channelList = new ArrayList<Integer>();
        boolCheckPoly = false;

        showFrame();
    }

    private void showFrame() {

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame = new Frame("SegRPE");
        frame.setSize(450, 350);
        Dimension dim = frame.getSize();
        frame.setLocation(screen.width - dim.width - 1, screen.height / 5 - 1);
        frame.setLayout(new BorderLayout());

        Color uiColor = UIManager.getColor("Panel.background");

        Panel panel = new Panel(new GridLayout(6, 1));
        panel.setBackground(uiColor);
        Panel buttonPanel = new Panel(new GridLayout(6, 1));
        buttonPanel.setBackground(uiColor);

        panel.add(new Label("1. Multi-point tool to mark vertices: "));
        Button polySegButton = new Button("Poly Seg");
        polySegButton.addActionListener(this);
        buttonPanel.add(polySegButton);

        Panel updatePanel = new Panel(new GridLayout(2, 1));
        updatePanel.setBackground(Color.lightGray);
        Label updateLabel1 = new Label("2. Color picker tool - Black/white, Paintbrush tool to break");
        Label updateLabel2 = new Label("    a line or lines of a vertice to remove the line/vertex");
        updatePanel.add(updateLabel1);
        updatePanel.add(updateLabel2);
        panel.add(updatePanel);
        Button updateButton = new Button("Update Poly");
        updateButton.addActionListener(this);
        buttonPanel.add(updateButton);

        checkPolyButton = new Button("Check Poly");
        checkPolyButton.addActionListener(this);
        panel.add(new Label());
        buttonPanel.add(checkPolyButton);

        Panel nucleusPanel = new Panel(new GridLayout(1, 2));
        nucleusPanel.setBackground(Color.lightGray);
        nucleusPanel.add(new Label("3. Nucleus:"));
        choice = new Choice();
        choice.add("Unselect");
        selectedIndex = 0;
        choice.select(selectedIndex);
        choice.addItemListener(this);
        nucleusPanel.add(choice);
        panel.add(nucleusPanel);
        panel.add(nucleusPanel);
        buttonPanel.add(new Label());

        panel.add(new Label());
        buttonPanel.add(new Label());

        Panel checkBoxPanel = new Panel(new GridLayout(1, numberChannels));
        checkboxArray = new Checkbox[numberChannels];
        checkboxArray[0] = new Checkbox("C" + Integer.toString(1), true);
        checkboxArray[0].setVisible(false);
        checkboxArray[0].setEnabled(false);
        checkBoxPanel.add(checkboxArray[0]);
        for (int index = 1; index < numberChannels; index++) {
            checkboxArray[index] = new Checkbox("C" + Integer.toString(index + 1), false);
            checkboxArray[index].setVisible(false);
            checkboxArray[index].setEnabled(false);
            checkBoxPanel.add(checkboxArray[index]);
        }
        Panel chPanel = new Panel(new BorderLayout());
        chPanel.add(new Label("4. Measurements:"), "West");
        chPanel.add(checkBoxPanel, "Center");
        edgeBox = new Checkbox("Exclude on edges");
        edgeBox.setState(true);
        Panel measurePanel = new Panel(new GridLayout(2, 1));
        measurePanel.setBackground(Color.lightGray);
        measurePanel.add(chPanel);
        measurePanel.add(edgeBox);
        panel.add(measurePanel);
        Button measureButton = new Button("Measure & Save");
        measureButton.addActionListener(this);
        buttonPanel.add(measureButton);

        frame.add(panel, "Center");
        frame.add(buttonPanel, "East");

        Panel okPanel = new Panel(new GridLayout(3, 4));
        okPanel.setBackground(uiColor);
        okPanel.add(new Label());
        okPanel.add(new Label());
        okPanel.add(new Label());
        okPanel.add(new Label());
        okPanel.add(new Label());
        okPanel.add(new Label());
        Button okButton = new Button("OK");
        okButton.addActionListener(this);
        okPanel.add(okButton);
        Button cancelButton = new Button("Cancel");
        cancelButton.addActionListener(this);
        okPanel.add(cancelButton);

        okPanel.add(new Label());
        okPanel.add(new Label());
        okPanel.add(new Label());
        okPanel.add(new Label());

        frame.add(okPanel, "South");

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                frame.dispose();
            }
        });
        frame.setVisible(true);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label.equals("Poly Seg")) {
            roughPoly();
            finePoly();
            setOptions();
        } else if (label.equals("Update Poly")) {
            updatePoly();
        } else if (label.equals("Check Poly")) {
            checkPoly();
        } else if (label.equals("Measure & Save")) {
            if (doMeasure()) {
                doSave();
            }
        } else if (label.equals("OK")) {
            doOk();
        } else if (label.equals("Cancel")) {
            frame.dispose();
        }
    }

    public void roughPoly() {

        imp = IJ.getImage();
        if (imp == null) {
            IJ.showMessage("There are no Images open");
            return;
        }
        width = imp.getWidth();
        height = imp.getHeight();
        nChannels = imp.getNChannels();
        Calibration cal = imp.getCalibration();
        pixelWidth = cal.pixelWidth;
        pixelHeight = pixelWidth;

        PointRoi pointRoi = (PointRoi) imp.getRoi();
        if (pointRoi == null) {
            IJ.showMessage("No Vertices", "There are no vertices marked");
            return;
        }
        boolShowLabels = pointRoi.getShowLabels();

        ImageProcessor ipPoly = new ByteProcessor(width, height);
        if ((impPoly == null) || (!impPoly.isVisible())) {
            impPoly = new ImagePlus("Poly", ipPoly);
        } else {
            impPoly.setProcessor("Poly", ipPoly);
        }

        /* points = pointRoi.getContainedPoints();
        PolygonRoi's getCotainedPoints() gets (-1, -1) for a point drawn in 
        the top left quart of pixel/square(0, 0). The problem was fixed in 
        the ImageJ 1.54g13 daily build of Aug 17, 2023. A workaround is 
        to use getFloatPolygon();
         */
        FloatPolygon fPolygon = (FloatPolygon) pointRoi.getFloatPolygon();
        int length = fPolygon.npoints;
        float[] xPoints = fPolygon.xpoints;
        float[] yPoints = fPolygon.ypoints;

        points = new Point[length];
        Point point;
        for (int index = 0; index < length; index++) {
            point = new Point(Math.round(xPoints[index]), Math.round(yPoints[index]));
            points[index] = point;
            ipPoly.set(point.x, point.y, 255);
        }

        // reset the points to the pixels centers
        setPoints();

        IJ.run(impPoly, "Watershed", "");

        if (impPoly.isVisible()) {
            impPoly.updateAndDraw();
        } else {
            impPoly.show();
        }

        ipPoly = impPoly.getProcessor();
        ipPoly.setThreshold(0, 0);
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        pa.analyze(impPoly);
        overlay = impPoly.getOverlay();
        impPoly.setOverlay(null);

        imp.setOverlay(null);
        boolCheckPoly = false;
    }

    public void setOptions() {
        choice.removeAll();
        choice.addItem("Unselect");
        for (int index = 1; index < (nChannels + 1); index++) {
            choice.addItem("Ch" + index);
        }
        if (selectedIndex > nChannels) {
            selectedIndex = nChannels;
            choice.select(selectedIndex);
        } else {
            choice.select(selectedIndex);
        }
        for (int index = 0; index < nChannels; index++) {
            checkboxArray[index].setVisible(true);
            checkboxArray[index].setEnabled(true);
        }
        for (int index = nChannels; index < numberChannels; index++) {
            checkboxArray[index].setVisible(false);
            checkboxArray[index].setEnabled(false);
        }
    }

    public void updatePoly() {

        if (impPoly == null) {
            IJ.showMessage("No Poly Image", "Poly image does not exists");
            return;
        }
        if (!impPoly.isVisible()) {
            IJ.showMessage("Poly Image Closed", "Poly image is closed");
            return;
        }
        if (imp == null) {
            IJ.showMessage("No Image", "There are no images open");
            return;
        }
        if (!imp.isVisible()) {
            IJ.showMessage("Image Closed", "The images is closed");
            return;
        }

        // get rid of a point if all its connections are broken up 
        // by the brush tool.  
        IJ.run(impPoly, "Fill Holes", "");

        // get the new ipPoints and points   
        ImageProcessor ipPoly = impPoly.getProcessor();
        ipPoints = new ByteProcessor(width, height);
        ArrayList<Point> pointList = new ArrayList<Point>();
        Point point;
        int length = points.length;
        for (int index = 0; index < length; index++) {
            point = points[index];
            if (ipPoly.get(point.x, point.y) > 0) {
                pointList.add(point);
                ipPoints.set(point.x, point.y, 255);
            }
        }
        int listSize = pointList.size();
        points = new Point[listSize];
        for (int index = 0; index < listSize; index++) {
            point = pointList.get(index);
            points[index] = point;
        }

        // update points
        setPoints();

        // get the edited rois
        ipPoly.setThreshold(0, 0);
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        pa.analyze(impPoly);
        Roi[] rois = impPoly.getOverlay().toArray();
        impPoly.setOverlay(null);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                ipPoly.set(i, j, 255);
            }
        }

        Roi roi;
        Rectangle rect;
        ImageProcessor ipRoi;
        ImagePlus impRoi2;
        ImageProcessor ipRoi2;

        // trim the rois
        for (int index = 0; index < rois.length; index++) {
            roi = rois[index];
            rect = roi.getBounds();
            ipRoi = roi.getMask();

            impRoi2 = NewImage.createByteImage("ROI", rect.width + 4, rect.height + 4, 1, NewImage.FILL_BLACK);
            ipRoi2 = impRoi2.getProcessor();

            for (int bY = rect.y; bY < (rect.y + rect.height); bY++) {
                for (int bX = rect.x; bX < (rect.x + rect.width); bX++) {
                    ipRoi2.set(bX - rect.x + 2, bY - rect.y + 2, ipRoi.get(bX - rect.x, bY - rect.y));
                }
            }
            impRoi2.updateImage();
            IJ.run(impRoi2, "Maximum...", "radius=1");
            IJ.run(impRoi2, "Minimum...", "radius=1");
            ipRoi2 = impRoi2.getProcessor();
            for (int bY = rect.y; bY < (rect.y + rect.height); bY++) {
                for (int bX = rect.x; bX < (rect.x + rect.width); bX++) {
                    if (ipRoi2.get(bX - rect.x + 2, bY - rect.y + 2) > 0) {
                        ipPoly.set(bX, bY, 0);
                    }
                }
            }
        }

        // make sure points are included in impPoly
        for (int index = 0; index < length; index++) {
            point = points[index];
            ipPoly.set(point.x, point.y, 255);
        }

        if (impPoly.isVisible()) {
            impPoly.updateAndDraw();
        } else {
            impPoly.show();
        }

        // get overlay
        ipPoly = impPoly.getProcessor();
        ipPoly.setThreshold(0, 0);
        rt = new ResultsTable();
        pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        pa.analyze(impPoly);
        overlay = impPoly.getOverlay();
        impPoly.setOverlay(null);

        imp.setOverlay(null);
        boolCheckPoly = false;

    }

    public void finePoly() {

        if (impPoly == null) {
            IJ.showMessage("No Poly Image", "Poly image does not exists");
            return;
        }
        if (!impPoly.isVisible()) {
            IJ.showMessage("Poly Image Closed", "Poly Image is closed");
            return;
        }
        if (imp == null) {
            IJ.showMessage("No Image", "There are no images open");
            return;
        }
        if (!imp.isVisible()) {
            IJ.showMessage("Image Closed", "The images is closed");
            return;
        }

        // get a fresh ipPoints based on Points
        ipPoints = new ByteProcessor(width, height);
        Point point;
        int length = points.length;
        for (int index = 0; index < length; index++) {
            point = points[index];
            ipPoints.set(point.x, point.y, 255);
        }

        ImagePlus impPoly2 = impPoly.duplicate();
        ImageProcessor ipPoly2 = impPoly2.getProcessor();

        ImageProcessor ipPoly = impPoly.getProcessor();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                ipPoly.set(i, j, 0);
            }
        }

        //radius = Integer.parseInt(radiusField.getText());
        Roi roi;
        ImageProcessor ipRoi;
        Rectangle rect;

        PolygonRoi polyRoi;
        ImageProcessor ipPolyRoi;
        Rectangle rectPolyRoi;

        ImagePlus impPolyRoi2;
        ImageProcessor ipPolyRoi2;
        ImagePlus impPolyRoi3;
        ImageProcessor ipPolyRoi3;

        // get the inner rois in impPoly2
        ipPoly2.setThreshold(0, 0);
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);

        pa.analyze(impPoly2);
        Overlay ol = impPoly2.getOverlay();
        impPoly2.setOverlay(null);

        Roi[] rois = null;
        int roisLength;
        if (ol == null) {
            roisLength = 0;
        } else {
            rois = ol.toArray();
            roisLength = rois.length;
        }

        for (int index = 0; index < roisLength; index++) {
            roi = rois[index];
            rect = roi.getBounds();
            ipRoi = roi.getMask();
            // fill the inner rois in impPoly2
            for (int bY = rect.y; bY < (rect.y + rect.height); bY++) {
                for (int bX = rect.x; bX < (rect.x + rect.width); bX++) {
                    if (ipRoi.get(bX - rect.x, bY - rect.y) > 0) {
                        ipPoly2.set(bX, bY, 255);
                    }
                }
            }

            // construct impPoly using the PolygonRois 
            polyRoi = roi2Polygon(roi);
            rectPolyRoi = polyRoi.getBounds();
            ipPolyRoi = polyRoi.getMask();
            impPolyRoi2 = NewImage.createByteImage("ROI", rectPolyRoi.width + 2, rectPolyRoi.height + 2, 1, NewImage.FILL_BLACK);
            ipPolyRoi2 = impPolyRoi2.getProcessor();
            for (int j = 0; j < rectPolyRoi.height; j++) {
                for (int i = 0; i < rectPolyRoi.width; i++) {
                    if (ipPolyRoi.get(i, j) > 0) {
                        ipPolyRoi2.set(i + 1, j + 1, 255);
                    }
                }
            }
            impPolyRoi3 = impPolyRoi2.duplicate();
            IJ.run(impPolyRoi3, "Maximum...", "radius=1");
            ipPolyRoi3 = impPolyRoi3.getProcessor();
            for (int bY = rectPolyRoi.y - 1; bY < (rectPolyRoi.y - 1 + rectPolyRoi.height + 2); bY++) {
                for (int bX = rectPolyRoi.x - 1; bX < (rectPolyRoi.x - 1 + rectPolyRoi.width + 2); bX++) {
                    if ((bY > -1) && (bY < height)
                            && (bX > -1) && (bX < width)) {
                        if ((ipPolyRoi3.get(bX - (rectPolyRoi.x - 1), bY - (rectPolyRoi.y - 1)) > 0)
                                && (ipPolyRoi2.get(bX - (rectPolyRoi.x - 1), bY - (rectPolyRoi.y - 1)) < 255)) {
                            ipPoly.set(bX, bY, 255);
                        }
                    }
                }
            }
        }

        // set edge points to ipPoints
        ipPoints.set(0, 0, 255);
        ipPoints.set(width - 1, 0, 255);
        ipPoints.set(width - 1, height - 1, 255);
        ipPoints.set(0, height - 1, 255);
        for (int i = 1; i < (width - 1); i++) {
            if (ipPoly2.get(i, 0) > 0) {
                ipPoints.set(i, 0, 255);
            }
            if (ipPoly2.get(i, height - 1) > 0) {
                ipPoints.set(i, height - 1, 255);
            }
        }
        for (int j = 1; j < (height - 1); j++) {
            if (ipPoly2.get(0, j) > 0) {
                ipPoints.set(0, j, 255);
            }
            if (ipPoly2.get(width - 1, j) > 0) {
                ipPoints.set(width - 1, j, 255);
            }

        }

        // get the edge rois in impPoly2        
        ipPoly2.setThreshold(0, 0);
        rt = new ResultsTable();
        pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                //| ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);

        pa.analyze(impPoly2);
        ol = impPoly2.getOverlay();
        impPoly2.setOverlay(null);

        rois = null;
        if (ol == null) {
            roisLength = 0;
        } else {
            rois = ol.toArray();
            roisLength = rois.length;
        }

        for (int index = 0; index < roisLength; index++) {
            roi = rois[index];

            // construct impPoly using the PolygonRois 
            polyRoi = roi2Polygon(roi);
            rectPolyRoi = polyRoi.getBounds();
            ipPolyRoi = polyRoi.getMask();
            impPolyRoi2 = NewImage.createByteImage("ROI", rectPolyRoi.width + 2, rectPolyRoi.height + 2, 1, NewImage.FILL_BLACK);
            ipPolyRoi2 = impPolyRoi2.getProcessor();
            for (int j = 0; j < rectPolyRoi.height; j++) {
                for (int i = 0; i < rectPolyRoi.width; i++) {
                    if (ipPolyRoi.get(i, j) > 0) {
                        ipPolyRoi2.set(i + 1, j + 1, 255);
                    }
                }
            }
            impPolyRoi3 = impPolyRoi2.duplicate();
            IJ.run(impPolyRoi3, "Maximum...", "radius=1");
            ipPolyRoi3 = impPolyRoi3.getProcessor();
            for (int bY = rectPolyRoi.y - 1; bY < (rectPolyRoi.y - 1 + rectPolyRoi.height + 2); bY++) {
                for (int bX = rectPolyRoi.x - 1; bX < (rectPolyRoi.x - 1 + rectPolyRoi.width + 2); bX++) {
                    if ((bY > -1) && (bY < height)
                            && (bX > -1) && (bX < width)) {
                        if ((ipPolyRoi3.get(bX - (rectPolyRoi.x - 1), bY - (rectPolyRoi.y - 1)) > 0)
                                && (ipPolyRoi2.get(bX - (rectPolyRoi.x - 1), bY - (rectPolyRoi.y - 1)) < 255)) {
                            ipPoly.set(bX, bY, 255);
                        }
                    }
                }
            }
        }

        impPoly.setProcessor(ipPoly);
        IJ.run(impPoly, "Maximum...", "radius=1");
        IJ.run("Options...", "iterations=1 count=1 edm=Overwrite");
        IJ.run(impPoly, "Voronoi", "");
        ipPoly = impPoly.getProcessor();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (ipPoly.get(i, j) > 0) {
                    ipPoly.set(i, j, 255);
                }
            }
        }

        // make sure points are included in impPoly
        for (int index = 0; index < length; index++) {
            point = points[index];
            ipPoly.set(point.x, point.y, 255);
        }

        ipPoly = impPoly.getProcessor();
        ipPoly.setThreshold(0, 0);
        rt = new ResultsTable();
        pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 0, Long.MAX_VALUE, 0, 1);
        pa.analyze(impPoly);
        overlay = impPoly.getOverlay();
        impPoly.setOverlay(null);

        imp.setOverlay(null);
        boolCheckPoly = false;

    }

    public PolygonRoi roi2Polygon(Roi roi) {
        // Get the polygon vertices by dilating the roi

        Rectangle rect = roi.getBounds();
        ImageProcessor ipRoi = roi.getMask();

        // dilate the roi
        ImagePlus impRoi2 = NewImage.createByteImage("ROI", rect.width + 2 * radius, rect.height + 2 * radius, 1, NewImage.FILL_BLACK);
        ImageProcessor ipRoi2 = impRoi2.getProcessor();
        for (int bY = rect.y; bY < (rect.y + rect.height); bY++) {
            for (int bX = rect.x; bX < (rect.x + rect.width); bX++) {
                ipRoi2.set(bX - rect.x + radius, bY - rect.y + radius, ipRoi.get(bX - rect.x, bY - rect.y));
            }
        }
        impRoi2.updateImage();
        IJ.run(impRoi2, "Maximum...", "radius=" + radius);

        // get the vertices of the roi
        ipRoi2 = impRoi2.getProcessor();
        ArrayList<Point> polyPoints = new ArrayList<Point>();
        Point point;
        for (int bY = rect.y - radius; bY < (rect.y + rect.height + radius); bY++) {
            for (int bX = rect.x - radius; bX < (rect.x + rect.width + radius); bX++) {
                if ((bY > -1) && (bY < height)
                        && (bX > -1) && (bX < width)) {
                    if ((ipRoi2.get(bX - (rect.x - radius), bY - (rect.y - radius)) > 0)
                            && (ipPoints.get(bX, bY) > 0)) {
                        point = new Point(bX, bY);
                        polyPoints.add(point);
                    }
                }
            }
        }

        // Sort the vertices in rotation
        int poly = polyPoints.size();
        point = polyPoints.get(0);
        int centerX = point.x;
        int centerY = point.y;
        for (int pIndex = 1; pIndex < poly; pIndex++) {
            point = polyPoints.get(pIndex);
            centerX = centerX + point.x;
            centerY = centerY + point.y;
        }
        centerX = (int) ((double) centerX / poly + 0.5);
        centerY = (int) ((double) centerY / poly + 0.5);

        double[] polyAngles = new double[poly];

        for (int pIndex = 0; pIndex < poly; pIndex++) {
            point = polyPoints.get(pIndex);
            polyAngles[pIndex] = Math.atan2(point.y - centerY, point.x - centerX);
        }

        ArrayList<Point> polyPointsSorted = new ArrayList<Point>();
        int[] polySorted = new int[poly];
        for (int pIndex = 0; pIndex < poly; pIndex++) {
            polySorted[pIndex] = -1;
        }
        double minAngle;
        int minIndex;
        for (int j = 0; j < poly; j++) {
            minAngle = Double.POSITIVE_INFINITY;
            minIndex = -1;
            for (int i = 0; i < poly; i++) {
                if (polySorted[i] == -1) {
                    if (polyAngles[i] < minAngle) {
                        minAngle = polyAngles[i];
                        minIndex = i;

                    }
                }
            }
            polySorted[minIndex] = j;
            polyPointsSorted.add(polyPoints.get(minIndex));
        }

        // The vertices forms a polygonRoi
        int[] polyX = new int[poly];
        int[] polyY = new int[poly];
        for (int pIndex = 0; pIndex < poly; pIndex++) {
            point = polyPointsSorted.get(pIndex);
            polyX[pIndex] = point.x;
            polyY[pIndex] = point.y;
        }
        PolygonRoi polyRoi = new PolygonRoi(polyX, polyY, poly, Roi.POLYGON);
        return polyRoi;
    }

    public void checkPoly() {

        if (impPoly == null) {
            IJ.showMessage("No Poly Image", "Poly image does not exists");
            return;
        }
        if (!impPoly.isVisible()) {
            IJ.showMessage("Poly Image Closed", "Poly image is closed");
            return;
        }
        if (imp == null) {
            IJ.showMessage("No Image", "There are no images open");
            return;
        }
        if (!imp.isVisible()) {
            IJ.showMessage("Image Closed", "The images is closed");
            return;
        }
        if (boolCheckPoly) {
            imp.setOverlay(null);
            setPoints(); // in case pointRoi is deselected
            boolCheckPoly = false;
        } else {
            imp.setOverlay(overlay);
            boolCheckPoly = true;
        }
    }

    public void setPoints() {
        int length = points.length;
        Point point;
        int[] xArray = new int[length];
        int[] yArray = new int[length];
        for (int index = 0; index < length; index++) {
            point = points[index];
            xArray[index] = point.x;
            yArray[index] = point.y;

        }
        imp.killRoi();
        PointRoi pointRoi = new PointRoi(xArray, yArray, length);
        pointRoi.setShowLabels(boolShowLabels);
        imp.setRoi(pointRoi);
    }

    public boolean doMeasure() {

        if (imp == null) {
            IJ.showMessage("No Image", "There are no images open");
            return false;
        }
        if (!imp.isVisible()) {
            IJ.showMessage("Image Closed", "The images is closed");
            return false;
        }
        if (impPoly == null) {
            IJ.showMessage("No Poly Image", "Poly does not exists");
            return false;
        }
        if (!impPoly.isVisible()) {
            IJ.showMessage("Poly Image Closed", "Poly image is closed");
            return false;
        }

        nChannels = imp.getNChannels();
        if (nChannels > numberChannels) {
            IJ.showMessage("Number of Channels", "Number of channels can be > " + numberChannels);
        }

        fInfo = imp.getOriginalFileInfo();
        if (fInfo != null) {
            imagePath = fInfo.directory;
            imageName = fInfo.fileName;
            int dotIndex = imageName.lastIndexOf(".");
            imgName = imageName.substring(0, dotIndex);
        } else {
            return false;
        }

        selectedIndex = choice.getSelectedIndex();
        labelList.clear();
        labelList.add(labels[0]);
        if (selectedIndex > 0) {
            labelList.add(labels[1]);
        }

        channelList.clear();
        for (int index = 0; index < numberChannels; index++) {
            if (checkboxArray[index].getState()) {
                channelList.add(index + 1);
            }
        }

        ImageProcessor ipPoly = impPoly.getProcessor();
        ipPoly.setThreshold(0, 0);
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa;
        if (edgeBox.getState()) {
            pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                    | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES
                    | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                    Measurements.AREA,
                    rt, 0, Long.MAX_VALUE, 0, 1);
        } else {
            pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                    | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                    Measurements.AREA,
                    rt, 0, Long.MAX_VALUE, 0, 1);
        }
        pa.analyze(impPoly);
        overlay = impPoly.getOverlay();
        impPoly.setOverlay(null);

        imp.setOverlay(null);
        boolCheckPoly = false;

        if (overlay == null) {
            IJ.showMessage("No Polygons", "There are no polygons");
            return false;
        }

        Roi[] rois = overlay.toArray();
        areaArray = new double[labelList.size()][rois.length];
        resultArray = new double[channelList.size()][labelList.size()][rois.length];
        measureCell();
        if (selectedIndex > 0) {
            measureNucleus();
        }

        showResults();

        return true;
    }

    public void measureCell() {

        ImageStack st = imp.getStack();
        int channel;
        ImageProcessor ipSlice;
        Roi[] rois = overlay.toArray();
        Roi roi;
        Rectangle rect;
        ImageProcessor ipRoi;
        double area;
        double sum;

        for (int listIndex = 0; listIndex < channelList.size(); listIndex++) {
            channel = channelList.get(listIndex);
            ipSlice = st.getProcessor(channel);
            for (int index = 0; index < rois.length; index++) {
                roi = rois[index];
                rect = roi.getBounds();
                ipRoi = roi.getMask();
                area = 0;
                sum = 0;
                for (int bY = rect.y; bY < (rect.y + rect.height); bY++) {
                    for (int bX = rect.x; bX < (rect.x + rect.width); bX++) {
                        if ((ipRoi.get(bX - rect.x, bY - rect.y)) > 0) {
                            area++;
                            sum = sum + ipSlice.get(bX, bY);
                        }
                    }
                }
                areaArray[0][index] = area * pixelWidth * pixelHeight;
                resultArray[listIndex][0][index] = sum;
            }

        }
    }

    public void measureNucleus() {

        ImageStack st = imp.getStack();
        ImageProcessor ipSlice = st.getProcessor(selectedIndex).duplicate();
        int[] hist = ipSlice.getStats().histogram;
        int maxValue = 255;
        while (hist[maxValue] == 0) {
            maxValue--;
        }
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < height; i++) {
                ipSlice.set(i, j, (int) (255.0 / maxValue * ipSlice.get(i, j)));
            }
        }
        ImagePlus impSlice = new ImagePlus("slice", ipSlice);
        IJ.run(impSlice, "Mean...", "radius=2");

        ipSlice.setAutoThreshold("Huang dark 16-bit");
        double thresh = ipSlice.getMinThreshold();
        ImageProcessor ipMask = new ByteProcessor(width, height);
        ImagePlus impMask = new ImagePlus("Mask", ipMask);
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < height; i++) {
                if (ipSlice.get(i, j) > thresh) {
                    ipMask.set(i, j, 0);
                } else {
                    ipMask.set(i, j, 255);
                }
            }
        }
        ipMask.setThreshold(0, 0);
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, 1, Long.MAX_VALUE, 0, 1);
        pa.analyze(impMask);
        double[] roiAreaArray = rt.getColumn("Area");
        Arrays.sort(roiAreaArray);
        double[] dArray = new double[roiAreaArray.length];
        for (int index = 0; index < roiAreaArray.length; index++) {
            dArray[index] = Math.sqrt(roiAreaArray[index]);
        }
        // darray is sort of radius aarray, clustering is better on it than on areaArray. 
        int startNucleus = getStartNucleus(dArray);

        rt.reset();
        pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET
                | ParticleAnalyzer.INCLUDE_HOLES
                | ParticleAnalyzer.SHOW_OVERLAY_OUTLINES,
                Measurements.AREA,
                rt, roiAreaArray[startNucleus], Long.MAX_VALUE, 0, 1);
        pa.analyze(impMask);

        Roi[] rois2 = impMask.getOverlay().toArray();
        Roi roi2;
        Rectangle rect2;
        ImageProcessor ipRoi2;
        impNucleus = NewImage.createByteImage("Nucleus", width, height, 1, NewImage.FILL_WHITE);
        ImageProcessor ipNucleus = impNucleus.getProcessor();
        for (int index = 0; index < rois2.length; index++) {
            roi2 = rois2[index];
            rect2 = roi2.getBounds();
            ipRoi2 = roi2.getMask();
            for (int bY = rect2.y; bY < (rect2.y + rect2.height); bY++) {
                for (int bX = rect2.x; bX < (rect2.x + rect2.width); bX++) {
                    if (ipRoi2.get(bX - rect2.x, bY - rect2.y) > 0) {
                        ipNucleus.set(bX, bY, 0);
                    }
                }
            }
        }

        Roi[] rois = overlay.toArray();

        Roi roi;
        Rectangle rect;
        ImageProcessor ipRoi;
        double area;
        double sum;
        for (int listIndex = 0; listIndex < channelList.size(); listIndex++) {
            ipSlice = st.getProcessor(channelList.get(listIndex));
            for (int index = 0; index < rois.length; index++) {
                roi = rois[index];
                rect = roi.getBounds();
                ipRoi = roi.getMask();
                area = 0;
                sum = 0;
                for (int bY = rect.y; bY < (rect.y + rect.height); bY++) {
                    for (int bX = rect.x; bX < (rect.x + rect.width); bX++) {
                        if ((ipRoi.get(bX - rect.x, bY - rect.y) > 0)
                                && (ipMask.get(bX, bY) < 255)) {
                            area++;
                            sum = sum + ipSlice.get(bX, bY);
                        }
                    }
                }
                areaArray[1][index] = area * pixelWidth * pixelHeight;
                resultArray[listIndex][1][index] = sum;
            }
        }
    }

    public int getStartNucleus(double[] dArray) {
        // k-mean clustring - 2 clusters of nuclei and noise
        int length = dArray.length;
        double lowerMean0;
        double upperMean0;
        double lowerMean = dArray[(int) (length * 0.25)];
        double upperMean = dArray[(int) (length * 0.75)];
        double lowerSum;
        double upperSum;
        double lowerNum;
        double upperNum;
        int upperStart;
        do {
            lowerMean0 = lowerMean;
            upperMean0 = upperMean;
            lowerSum = 0;
            upperSum = 0;
            lowerNum = 0;
            upperNum = 0;
            upperStart = length - 1;
            for (int index = 0; index < length; index++) {
                if (Math.abs(dArray[index] - lowerMean0) < Math.abs(dArray[index] - upperMean0)) {
                    lowerSum = lowerSum + dArray[index];
                    lowerNum++;
                } else {
                    upperSum = upperSum + dArray[index];
                    upperNum++;
                    if (index < upperStart) {
                        upperStart = index;
                    }
                }
            }
            lowerMean = lowerSum / lowerNum;
            upperMean = upperSum / upperNum;
        } while ((lowerMean != lowerMean0) || (upperMean != upperMean0));

        return upperStart;
    }

    public void showResults() {
        String line;

        line = imageName;
        for (int labelIndex = 0; labelIndex < labelList.size(); labelIndex++) {
            line = line + "     \t" + "Area" + labelList.get(labelIndex);
        }
        for (int channelIndex = 0; channelIndex < channelList.size(); channelIndex++) {
            for (int labelIndex = 0; labelIndex < labelList.size(); labelIndex++) {
                line = line + "     \t" + "Sum" + channelList.get(channelIndex) + labelList.get(labelIndex);
            }
        }
        IJ.log(line);

        for (int rowIndex = 0; rowIndex < areaArray[0].length; rowIndex++) {
            line = Integer.toString(rowIndex + 1);
            for (int colIndex = 0; colIndex < labelList.size(); colIndex++) {
                line = line + "     \t" + areaArray[colIndex][rowIndex];
            }
            for (int channelIndex = 0; channelIndex < channelList.size(); channelIndex++) {
                for (int labelIndex = 0; labelIndex < labelList.size(); labelIndex++) {
                    line = line + "     \t" + resultArray[channelIndex][labelIndex][rowIndex];
                }
            }
            IJ.log(line);
        }
    }

    public void doSave() {
        saveResults();
        ImagePlus impCell = impPoly.duplicate();
        impCell.setOverlay(overlay);
        IJ.save(impCell, imagePath + imgName + "_cell.tif");
        if (selectedIndex > 0) {
            IJ.save(impNucleus, imagePath + imgName + "_nucleus.tif");
        }
    }

    public void saveResults() {

        PrintStream out = null;
        String line;
        try {
            FileOutputStream outStream = new FileOutputStream(imagePath + imgName + ".csv");
            out = new PrintStream(outStream);
            line = imageName;
            for (int labelIndex = 0; labelIndex < labelList.size(); labelIndex++) {
                line = line + "," + "Area" + labelList.get(labelIndex);
            }
            for (int channelIndex = 0; channelIndex < channelList.size(); channelIndex++) {
                for (int labelIndex = 0; labelIndex < labelList.size(); labelIndex++) {
                    line = line + "," + "Sum" + channelList.get(channelIndex) + labelList.get(labelIndex);
                }
            }
            out.println(line);

            for (int rowIndex = 0; rowIndex < areaArray[0].length; rowIndex++) {
                line = Integer.toString(rowIndex + 1);
                for (int colIndex = 0; colIndex < labelList.size(); colIndex++) {
                    line = line + "," + areaArray[colIndex][rowIndex];
                }
                for (int channelIndex = 0; channelIndex < channelList.size(); channelIndex++) {
                    for (int labelIndex = 0; labelIndex < labelList.size(); labelIndex++) {
                        line = line + "," + resultArray[channelIndex][labelIndex][rowIndex];
                    }
                }
                out.println(line);
            }

        } catch (IOException e) {
            IJ.log(e.getMessage());
        } finally {
            close(out);
        }
    }

    public void doOk() {

        if ((impPoly != null) && (impPoly.isVisible())) {
            impPoly.changes = false;
            impPoly.close();
        }

        imp.killRoi();
        if ((imp != null) && (imp.isVisible())) {
            imp.changes = false;
            imp.close();
        }

        imp = null;
        impPoly = null;
        boolCheckPoly = false;
        resultArray = null;

    }

    public void close(Closeable stream) {

        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException ioe) {
        }
    }

    @Override
    public void imageOpened(ImagePlus imp) {

    }

    @Override
    public void imageClosed(ImagePlus imp) {
        boolCheckPoly = false;
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        selectedIndex = choice.getSelectedIndex();
    }
}
