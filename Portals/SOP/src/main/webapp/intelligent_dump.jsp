<%@page import="java.text.*,org.jfree.data.general.*,com.smilecoms.sop.dp.*, com.smilecoms.commons.base.BaseUtils, java.util.*, java.util.Collections.*, com.smilecoms.sop.helpers.*,com.smilecoms.sop.util.*" %>
<%
            //2010-02-08-12-16-56
            SimpleDateFormat sdfLong = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String date = sdfLong.format(new Date());
            String locationFilter = request.getParameter("loc");
            String nameFilter = request.getParameter("name");
            String typeFilter = request.getParameter("type");
            String sumOrAvg = request.getParameter("agg");
            Map params = new HashMap();
            params.put("location_filter", locationFilter);
            params.put("name_filter", nameFilter);
            params.put("type_filter", typeFilter);
            params.put("sum_avg", sumOrAvg);
            SyslogStatsValueDataProducer data = new SyslogStatsValueDataProducer();
            DefaultValueDataset res = null;
            res = (DefaultValueDataset)(data.produceDataset(params));
            String value = String.valueOf(res.getValue().doubleValue());
%>
<%=date%>|<%=value%>|<%=request.getParameter("desc")%>
