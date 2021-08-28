/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.chartpostprocessors;

import de.laures.cewolf.ChartPostProcessor;
import java.io.Serializable;
import java.util.Map;
import org.slf4j.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;

/**
 *
 * @author PCB
 */
public class BarEnhancer implements ChartPostProcessor, Serializable {

    private static final Logger log = LoggerFactory.getLogger(BarEnhancer.class.getName());

    @Override
    public void processChart(JFreeChart chart, Map arg1) {
        CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
    }
}
