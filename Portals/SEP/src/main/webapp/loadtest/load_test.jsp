<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    Load Test
</c:set>


<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="html_head">            
        <script type="text/javascript">
            var lotsofdata = ""; //10 000 000 Bytes


            function upload20MB() {

                var form = document.createElement("form");
                var node = document.createElement("input");

                form.action = "https://${s:getProperty('env.portal.url')}/sep/LoadServlet";
                form.method = "post";
                form.enctype = "multipart/form-data";


                node.name = "data1";
                node.value = lotsofdata;
                form.appendChild(node.cloneNode());
                node.name = "data2";
                node.value = lotsofdata;
                form.appendChild(node.cloneNode());
                // To be sent, the form needs to be attached to the main document.
                form.style.display = "none";
                document.body.appendChild(form);
                console.log("Submitting loadtest form");
                form.submit();
                console.log("Finished submitting loadtest form");
            }
        </script>


    </stripes:layout-component>

    <stripes:layout-component name="contents">
        Load test is running...
        <img width="1px" height="1px" hidden="true" src="https://${s:getProperty('env.portal.url')}/sep/LoadServlet?a=1">
        <img width="1px" height="1px" hidden="true" src="https://${s:getProperty('env.portal.url')}/sep/LoadServlet?a=2">
        <img width="1px" height="1px" hidden="true" src="https://${s:getProperty('env.portal.url')}/sep/LoadServlet?a=3">

        <script type="text/javascript">
            console.log("In onload");
            var i;
            for (i = 0; i < 100000; i++) {
                lotsofdata = lotsofdata + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
            }
            upload20MB();
            upload20MB();
            upload20MB();
        </script>

    </stripes:layout-component>

</stripes:layout-render>