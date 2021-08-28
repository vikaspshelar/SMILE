<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.saleslead.referal">
    </fmt:message>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">

        <style type="text/css">

            .ui-dialog-title
            { 
                font-size: 20px;
                padding-left: 350px;
            }
            .ui-state-default{
                background-image: url('images/green_rectangle_btn.png') !important;
            }

            .ui-widget-content #debebanner {
                position: absolute;
                top: 0px;
                right: 0px;
            }



        </style>
        <script type="text/javascript">
            window.onload = function () {
                makeMenuActive('Profile_SalesleadReferalPage');
            }


            $(function () {

                $("#dialog").dialog({
                    resizable: false,
                    modal: true,
                    width: 935,
                    buttons: {
                        Accept: function () {
                            $(this).dialog("close");
                        }
                    },
                    autoOpen: false,
                    show: {
                        effect: "blind",
                        duration: 500
                    },
                    hide: {
                        effect: "explode",
                        duration: 500
                    }
                });
                $("#dialog").dialog("option", "position", 'center');
                var readTsCs = '${actionBean.termsAndConditionsRead}';
                if (readTsCs == 'true') {
                    //Do nothing, already read terms and conditions
                } else {
                    $("#dialog").dialog("open");
                }

            });




        </script>

    </stripes:layout-component>
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns">
            <div class="configservicecustomer columns">
                <img src="images/customer2.png"><br>
                <font class="light" style="font-size:30px;"><fmt:message key="scp.basic.data"/></font><br>
                <font style="font-size:12px; color:#75B343;">${actionBean.customer.firstName} ${actionBean.customer.lastName}<br>
                <strong> <fmt:message key="customer.since"/>: </strong><br>
                <strong>${s:formatDateShort(actionBean.customer.createdDateTime)}</strong><br>
                </font>
            </div>
            <div class="twelve columns">
                <div class="calculator">
                    <div class="calc_left">
                        <div class="saleslead_referal_heading light">
                            <font style="font-size:12px; color:#75B343; font-weight:bold;"> <fmt:message key="scp.saleslead.heading.msg"/></font>

                            <div id="dialog" title="<fmt:message key="scp.saleslead.referal"/>">

                                <div>
                                    <fmt:message key="scp.refer.a.friend.tscs"/>
                                </div>
                            </div>      
                        </div>

                        <div class="referal_form">
                            <stripes:form action="/SCPTroubleTicket.action" focus="">
                                Friend's Name<br/>
                                <input type='text' name="TT_FIXED_FIELD_Customer Name"/><br/><br/>
                                Telephone Contact<br/>
                                <input type='text' name="TT_FIXED_FIELD_Smile Customer Phone" onkeyup="validate(this, '^[+()0-9 -]{10,15}$', 'emptynotok')" /><br/><br/>
                                Email<br/>
                                <input type='text' name="TT_FIXED_FIELD_Customer Email" onkeyup="validate(this, /^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4}$/, 'emptynotok')"/><br/><br/>
                                Location<br/>
                                <input type='text' name="TT_FIXED_FIELD_Add:Location"/><br/><br/>
                                <input class="general_btn" type="submit" name="addReferredSalesLead" value="Submit"/>
                            </stripes:form>
                        </div>
                        <div class="referAfriendLogoFloatRight">
                            <img src="${s:getPropertyWithDefault('env.scp.refer.a.friend.banner.form.img', '')}"/>
                        </div>
                    </div>
                </div>
            </div>
        </stripes:layout-component>   
    </stripes:layout-render>