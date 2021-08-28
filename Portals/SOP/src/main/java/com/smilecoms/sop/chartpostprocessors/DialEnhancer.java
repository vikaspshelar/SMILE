package com.smilecoms.sop.chartpostprocessors;

import com.smilecoms.commons.base.BaseUtils;
import java.awt.Color;
import java.awt.Font;
import java.io.Serializable;
import java.util.Map;

import de.laures.cewolf.ChartPostProcessor;

import java.awt.GradientPaint;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.slf4j.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.dial.*;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.StandardGradientPaintTransformer;

/**
 * A postprocessor for changing details of a Dial plot: <BR><b>pointerType</b>
 * pin/pointer; default pointer <BR><b>dialText</b> text to display on the dial;
 * optional <BR><b>lowerBound</b> optional; default 0; starting value for the
 * scale <BR><b>upperBound</b> optional; default 100; end value for the scale
 * <BR><b>majorTickIncrement</b> optional; default 10; value increment between
 * major tick marks <BR><b>minorTickCount</b> optional; default 4; minor tick
 * marks to put between major tick marks
 * <P>
 * Usage:
 * <P>
 * &lt;chart:chartpostprocessor id="dialEnhancer"&gt;<BR>
 * &nbsp;&nbsp;&lt;chart:param name="pointerType" value='&lt;%= "pin"
 * %&gt;'/&gt;<BR> &nbsp;&nbsp;&lt;chart:param name="dialText" value='&lt;%=
 * "(km/h)" %&gt;'/&gt;<BR> &nbsp;&nbsp;&lt;chart:param name="lowerBound"
 * value='&lt;%= "0.1" %&gt;'/&gt;<BR> &nbsp;&nbsp;&lt;chart:param
 * name="upperBound" value='&lt;%= "0.1" %&gt;'/&gt;<BR>
 * &nbsp;&nbsp;&lt;chart:param name="majorTickIncrement" value='&lt;%= "20"
 * %&gt;'/&gt;<BR> &nbsp;&nbsp;&lt;chart:param name="minorTickCount"
 * value='&lt;%= "9" %&gt;'/&gt;<BR> &lt;/chart:chartpostprocessor&gt;
 */// TODO: capFill, capRadius and capOutline don't work yet
/*
 * <BR><b>capFill</b> optional; default #000000 (i.e., black)
 * <BR><b>capOutline</b> optional; default #FFFFFF (i.e., white)
 * <BR><b>capRadius</b> optional; 0.0 &lt; radius &lt; 1.0; default 0.05

 * &nbsp;&nbsp;&lt;chart:param name="capFill" value='&lt;%= "#FF8800" %&gt;'/&gt;<BR>
 * &nbsp;&nbsp;&lt;chart:param name="capOutline" value='&lt;%= "#0088FF" %&gt;'/&gt;<BR>
 * &nbsp;&nbsp;&lt;chart:param name="capRadius" value='&lt;%= "0.1" %&gt;'/&gt;<BR>
 */
public class DialEnhancer implements ChartPostProcessor, Serializable {

    static final long serialVersionUID = 6708371054518325470L;
    private Map params = null;
    private static final Logger log = LoggerFactory.getLogger(DialEnhancer.class.getName());
    private static final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();

    static {
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
    }

    public String getParamAsString(String param) {
        String str = (String) params.get(param);
        if (str != null && str.trim().length() > 0) {
            str = str.trim();
        }
        return str;
    }

    public String getParamAsString(String param, String defaultVal) {
        String str = getParamAsString(param);
        if (str == null) {
            str = defaultVal;
        }
        return str;
    }

    public double getParamAsDouble(String param) {
        return Double.parseDouble(getParamAsString(param, "0"));
    }

    public int getParamAsInt(String param) {
        return Integer.parseInt(getParamAsString(param, "0"));
    }

    @Override
    public void processChart(JFreeChart chart, Map params) {

        this.params = params;
        Plot plot = chart.getPlot();
        DialPlot dialplot = (DialPlot) plot;

        DialFrame frame = new StandardDialFrame();
        dialplot.setDialFrame(frame);
        JFreeChart chartJ = (JFreeChart) chart;
        chartJ.setBackgroundPaint(null);

        // Dial value in box
        DialValueIndicator dialvalueindicator = new DialValueIndicator(0);
        dialvalueindicator.setFont(new Font("Dialog", 1, 20));
        dialvalueindicator.setNumberFormat(new DecimalFormat("###,###.0", formatSymbols));
        dialvalueindicator.setBackgroundPaint(new Color(0, 0, 0, 0));
        dialvalueindicator.setOutlinePaint(new Color(0, 0, 0, 0));
        dialplot.addLayer(dialvalueindicator);

        // Dial scale around edge
        StandardDialScale standarddialscale = new StandardDialScale(getParamAsDouble("lowerBound"), getParamAsDouble("upperBound"), -120D, -300D, getParamAsDouble("majorTickIncrement"), getParamAsInt("minorTickCount"));
        standarddialscale.setTickRadius(0.88D);
        standarddialscale.setTickLabelOffset(0.14999999999999999D);
        standarddialscale.setTickLabelFont(new Font("Dialog", 0, 14));
        standarddialscale.setTickLabelPaint(Color.BLACK);
        standarddialscale.setTickLabelFormatter(new DecimalFormat("###,###", formatSymbols));
        dialplot.addScale(0, standarddialscale);

        // Text
        DialTextAnnotation dialtextannotation = new DialTextAnnotation(getParamAsString("dialText"));
        dialtextannotation.setFont(new Font("Dialog", 1, 14));
        dialtextannotation.setRadius(0.69999999999999996D);
        dialplot.addLayer(dialtextannotation);

        // Red on warning
        double highWarnLevel = getParamAsDouble("highwarn");
        double lowWarnLevel = getParamAsDouble("lowwarn");
        double dialValue = dialplot.getDataset().getValue().doubleValue();
        if (dialValue >= highWarnLevel) {
            // Make background red
            // Dial background
            GradientPaint gradientpaint = new GradientPaint(new Point(), new Color(255, 255, 255), new Point(), new Color(255, 0, 0));
            DialBackground dialbackground = new DialBackground(gradientpaint);
            dialbackground.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.VERTICAL));
            dialplot.setBackground(dialbackground);
            // Send Trap to Operations management indicating the warning
            if (BaseUtils.getBooleanProperty("env.sop.process.stats", true)) {
                BaseUtils.sendTrapToOpsManagement(
                        BaseUtils.MAJOR,
                        getParamAsString("title"),
                        "A SOP Dashboard has exceeded its warning threshold: " + getParamAsString("title") + " breaks high threshold " + highWarnLevel + " " + getParamAsString("dialText"));
            }
        } else if (dialValue <= lowWarnLevel) {
            // Make background red
            // Dial background
            GradientPaint gradientpaint = new GradientPaint(new Point(), new Color(255, 255, 255), new Point(), new Color(255, 0, 0));
            DialBackground dialbackground = new DialBackground(gradientpaint);
            dialbackground.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.VERTICAL));
            dialplot.setBackground(dialbackground);
            // Send Trap to Operations management indicating the warning
            if (BaseUtils.getBooleanProperty("env.sop.process.stats", true)) {
                BaseUtils.sendTrapToOpsManagement(
                        BaseUtils.MAJOR,
                        getParamAsString("title"),
                        "A SOP Dashboard is below its warning threshold: " + getParamAsString("title") + " breaks low threshold " + lowWarnLevel + " " + getParamAsString("dialText"));
            }
        } else {
            // Dial background
            GradientPaint gradientpaint = new GradientPaint(new Point(), new Color(255, 255, 255), new Point(), new Color(185, 207, 98));
            DialBackground dialbackground = new DialBackground(gradientpaint);
            dialbackground.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.VERTICAL));
            dialplot.setBackground(dialbackground);
        }

        // Range areas on the dial
        /*
         StandardDialRange standarddialrange = new StandardDialRange(40D, 60D, Color.red);
         standarddialrange.setInnerRadius(0.52000000000000002D);
         standarddialrange.setOuterRadius(0.55000000000000004D);
         dialplot.addLayer(standarddialrange);
         StandardDialRange standarddialrange1 = new StandardDialRange(10D, 40D, Color.orange);
         standarddialrange1.setInnerRadius(0.52000000000000002D);
         standarddialrange1.setOuterRadius(0.55000000000000004D);
         dialplot.addLayer(standarddialrange1);
         StandardDialRange standarddialrange2 = new StandardDialRange(-40D, 10D, Color.green);
         standarddialrange2.setInnerRadius(0.52000000000000002D);
         standarddialrange2.setOuterRadius(0.55000000000000004D);
         dialplot.addLayer(standarddialrange2);
         */
        // Dial pointer
        org.jfree.chart.plot.dial.DialPointer.Pointer pointer = new org.jfree.chart.plot.dial.DialPointer.Pointer();
        pointer.setFillPaint(new Color(89, 148, 38));
        dialplot.addPointer(pointer);

        // Dial Cap
        DialCap dialcap = new DialCap();
        dialcap.setFillPaint(Color.BLACK); //was 99
        dialplot.setCap(dialcap);

    }
}
