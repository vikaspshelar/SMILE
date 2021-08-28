/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.chartpostprocessors;

import de.laures.cewolf.ChartPostProcessor;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

/**
 *
 * @author PCB
 */
public class LineEnhancer implements ChartPostProcessor, Serializable {

    private static final Logger log = LoggerFactory.getLogger(LineEnhancer.class.getName());

    private static final List<Color> colours = new ArrayList();

    static {
        colours.add(Color.red);
        colours.add(Color.BLUE);
        colours.add(Color.green);
        colours.add(Color.orange);
        colours.add(Color.black);
        colours.add(Color.GRAY);
    }

    @Override
    public void processChart(JFreeChart chart, Map arg1) {
        XYPlot xyPlot = chart.getXYPlot();
        XYItemRenderer xyir = xyPlot.getRenderer();
        Stroke s = new BasicStroke(5);
        int cnt = xyPlot.getSeriesCount();
        for (int i=0;i<cnt;i++) {
          xyir.setSeriesStroke(i, s);
          setSeriesColor(chart,i,colours.get(i));
        }
        ValueAxis valueAxis = xyPlot.getDomainAxis();

        valueAxis.setVerticalTickLabels(true);
    }

    private void setSeriesColor(JFreeChart chart, int seriesIndex, Color color) {
        if (chart != null) {
            Plot plot = chart.getPlot();
            try {
                if (plot instanceof CategoryPlot) {
                    CategoryPlot categoryPlot = chart.getCategoryPlot();
                    CategoryItemRenderer cir = categoryPlot.getRenderer();
                    cir.setSeriesPaint(seriesIndex, color);
                } else if (plot instanceof XYPlot) {
                    XYPlot xyPlot = chart.getXYPlot();
                    XYItemRenderer xyir = xyPlot.getRenderer();
                    xyir.setSeriesPaint(seriesIndex, color);
                } else {
                    System.out.println("setSeriesColor() unsupported plot: "+plot);
                }
            } catch (Exception e) { //e.g. invalid seriesIndex
                log.warn("Error setting color '"+color+"' for series '"+seriesIndex+"' of chart '"+chart+"': "+e);
            }
        }//else: input unavailable
    }//setSeriesColor()
}
