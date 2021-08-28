/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tnf;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.tnf.TNFData;
import com.smilecoms.xml.schema.tnf.TNFQuery;
import com.smilecoms.xml.tnf.TNFError;
import com.smilecoms.xml.tnf.TNFSoap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;

/**
 *
 * @author paul
 */
@WebService(serviceName = "TNF", portName = "TNFSoap", endpointInterface = "com.smilecoms.xml.tnf.TNFSoap", targetNamespace = "http://xml.smilecoms.com/TNF", wsdlLocation = "TNFServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class TNFManager extends SmileWebService implements TNFSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;

    @Override
    public com.smilecoms.xml.schema.tnf.Done isUp(java.lang.String isUpRequest) throws TNFError {

        if (!BaseUtils.isPropsAvailable()) {
            throw createError(TNFError.class, "Properties are not available so this platform will be reported as down");
        }
        if (BaseUtils.getBooleanProperty("env.development.mode", false)) {
            // Only mark as down in prod environments. Hate the constant stack traces in dev
            return makeDone();
        }

        if (BaseUtils.getBooleanProperty("env.tnf.markasup.ondeploy", false)) {
            // TNF web-service is not avaialable.... doesn't seem this will change in the next few months... since monitoring currently has no value, going to mark it as up
            return makeDone();
        }
        
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();
        String server = BaseUtils.getSubProperty("env.tnf.config", "CCE_WsdlLocation");//e.g. CCE_WsdlLocation=http://10.0.1.92:8201/customer-care-endpoint/service.wsdl
        try {

            URL tnfURL = new URL(server);
            URLConnection openTNFConnection = tnfURL.openConnection();
            openTNFConnection.setConnectTimeout(2000);
            openTNFConnection.setReadTimeout(3000);
            openTNFConnection.connect();//The actual connection to the remote object is made using the connect method.

            if (log.isDebugEnabled()) {
                in = new BufferedReader(new InputStreamReader(openTNFConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    sb.append(inputLine);
                }
                log.debug("###############Beginning of TNF IsUp Webservice response#####################");
                String line = sb.toString();
                if (line.length() > 2000) {
                    line = line.substring(0, 2000) + "...LINE IS TOO LONG TO DISPLAY FULLY";
                }
                log.debug(line);
                log.debug("###############End of TNF IsUp Webservice response###########################");
            }
            return makeDone();

        } catch (Exception e) {
            if (BaseUtils.getBooleanProperty("env.tnf.show.isup.stacktrace", false)) {
                log.warn("Server at [{}] is not listening: ", server, e);
            }

            if (BaseUtils.getBooleanProperty("env.tnf.markasup.ondeploy", false)) {
                // This is for testing speed of deploy... Only mark as down if not deploying.
                return makeDone();
            }

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    log.warn("Error closing reader: ", e);
                }
            }
        }

        log.warn("TNF has failed its availability test!!!");
        com.smilecoms.xml.schema.tnf.Done done = new com.smilecoms.xml.schema.tnf.Done();
        return done;
    }

    private com.smilecoms.xml.schema.tnf.Done makeDone() {
        com.smilecoms.xml.schema.tnf.Done done = new com.smilecoms.xml.schema.tnf.Done();
        done.setDone(com.smilecoms.xml.schema.tnf.StDone.TRUE);
        return done;
    }

    @Override
    public TNFData getTNFData(TNFQuery tnfQuery) throws TNFError {
        setContext(tnfQuery, wsctx);
        TNFData data;

        if (log.isDebugEnabled()) {
            logStart();
        }

        try {

            if (tnfQuery.getTNFMethod().equalsIgnoreCase("Attribution")) {

                data = TNFHelper.getAttributionDataAsXmlString(tnfQuery);

            } else if (tnfQuery.getTNFMethod().equalsIgnoreCase("Complex")) {

                if (tnfQuery.getReportIds() != null) {
                    //REPORT_CONNECTIVITY_SCORE=*connectivity_score*
                    String confReportIds = BaseUtils.getProperty("env.tnf.cem.report.ids");
                    List<String> reportIds = tnfQuery.getReportIds().getReportId();
                    ArrayList<String> tnfRepIds = new ArrayList<>();

                    for (String reportIdFriendlyName : reportIds) {
                        String reportIdTechnicalName = Utils.getValueFromCRDelimitedAVPString(confReportIds, reportIdFriendlyName);
                        tnfRepIds.add(reportIdTechnicalName);
                        log.debug("ComplexData: Report ID to add to list is [{}] and its technical name is [{}]", reportIdFriendlyName, reportIdTechnicalName);
                    }
                    tnfQuery.getReportIds().getReportId().clear();
                    tnfQuery.getReportIds().getReportId().addAll(tnfRepIds);
                }

                //GRANULARITY_FIFTEEN_MINUTES=_MIN_15
                String granularities = BaseUtils.getProperty("env.tnf.cem.report.granularities");
                String granularityFriendlyName = tnfQuery.getTimeRange().getGranularity();
                String granularityTechnicalName = Utils.getValueFromCRDelimitedAVPString(granularities, granularityFriendlyName);
                tnfQuery.getTimeRange().setGranularity(granularityTechnicalName);
                log.debug("ComplexData: Granularity used for this request is [{}] and its technical name is [{}]", granularityFriendlyName, granularityTechnicalName);

                data = TNFHelper.getComplexDataAsXmlString(tnfQuery);

            } else if (tnfQuery.getTNFMethod().equalsIgnoreCase("Metric")) {

                String confReportIds = BaseUtils.getProperty("env.tnf.cem.report.ids");
                String reportIdFriendlyName = tnfQuery.getReportIds().getReportId().get(0);//The should be only one report-id for a metric call
                String reportIdTechnicalName = Utils.getValueFromCRDelimitedAVPString(confReportIds, reportIdFriendlyName);
                tnfQuery.getReportIds().getReportId().clear();
                tnfQuery.getReportIds().getReportId().add(reportIdTechnicalName);
                log.debug("MetricData: Report ID used for this request is [{}] and its technical name is [{}]", reportIdFriendlyName, reportIdTechnicalName);

                //GRANULARITY_FIFTEEN_MINUTES=_MIN_15
                String granularities = BaseUtils.getProperty("env.tnf.cem.report.granularities");
                String granularityFriendlyName = tnfQuery.getTimeRange().getGranularity();
                String granularityTechnicalName = Utils.getValueFromCRDelimitedAVPString(granularities, granularityFriendlyName);
                tnfQuery.getTimeRange().setGranularity(granularityTechnicalName);
                log.debug("MetricData: Granularity used for this request is [{}] and its technical name is [{}]", granularityFriendlyName, granularityTechnicalName);
                data = TNFHelper.getMetricDataAsXmlString(tnfQuery);

            } else {
                throw new Exception("The TNFMethod provided does not exist");
            }

        } catch (Exception ex) {
            throw processError(TNFError.class, ex);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd();
            }
        }
        return data;
    }
}
