<%@ include file="/include/scp_include.jsp" %>

<c:set var="title">
    <fmt:message key="scp.service.instance.configuration"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="head">
        <script type="text/javascript">


            var serviceStartValue = Number('${actionBean.downAndUplinkInternetSpeed}');
            var availableUserDefinedDPIRulesList = [];

            <c:forEach items="${s:getServiceSpecificationAvailableUserDefinedDPIRules(actionBean.serviceSpecification.serviceSpecificationId)}" var="rulename" varStatus="loop">
            availableUserDefinedDPIRulesList[${loop.index}] = "${rulename}";
            </c:forEach>

            $(document).ready(function () {

                var subUrl = 'images/tmpFiles/';

                /*Initialise default speed*/
                if (serviceStartValue == 1) {
                    $('input:radio[name="downAndUplinkInternetSpeed"]').filter('[value="1"]').attr('checked', true);
                    $("a#category_speed1").css('background-image', 'url(' + subUrl + '1-button.png)');

                } else if (serviceStartValue == 2) {
                    $('input:radio[name="downAndUplinkInternetSpeed"]').filter('[value="2"]').attr('checked', true);
                    $("a#category_speed2").css('background-image', 'url(' + subUrl + '2-button.png)');

                } else if (serviceStartValue == 5) {
                    $('input:radio[name="downAndUplinkInternetSpeed"]').filter('[value="5"]').attr('checked', true);
                    $("a#category_speed5").css('background-image', 'url(' + subUrl + '5-button.png)');

                } else if (serviceStartValue == 21) {
                    $('input:radio[name="downAndUplinkInternetSpeed"]').filter('[value="21"]').attr('checked', true);
                    $("a#category_speed21").css('background-image', 'url(' + subUrl + 'speed-button.png)');
                } else {
                    $('input:radio[name="downAndUplinkInternetSpeed"]').filter('[value="21"]').attr('checked', true);
                    $("a#category_speed21").css('background-image', 'url(' + subUrl + 'speed-button.png)');
                }

                /*Initialise default rules for this service*/
                var siDefaultDPIRules = '${actionBean.userDefinedDPIRules}';
                var siDefaultDPIRulesArray = siDefaultDPIRules.split(",");

                for (var i = 0; i < availableUserDefinedDPIRulesList.length; i++) {
                    var configuredDPIRuleName = availableUserDefinedDPIRulesList[i];
                    var imgOn = 'a#category_' + configuredDPIRuleName + '_on';
                    var imgOff = 'a#category_' + configuredDPIRuleName + '_off';

                    $(imgOn).css({'background-image': 'url(' + subUrl + 'onIsOffRadio.png)', 'background-repeat': 'no-repeat'});
                    $(imgOff).css({'background-image': 'url(' + subUrl + 'offIsOnRadio.png)', 'background-repeat': 'no-repeat'});
                }

                for (var m = 0; m < siDefaultDPIRulesArray.length; m++) {//If the is a previous preference for this rule, initialise preference
                    var siDPIRuleName = siDefaultDPIRulesArray[m];
                    var imgOn = 'a#category_' + siDPIRuleName + '_on';
                    var imgOff = 'a#category_' + siDPIRuleName + '_off';

                    //$(imgOn).css('background-image', 'url(' + subUrl + 'onIsOnRadio.png)');//$(imgOn).css('background-image', 'url(' + subUrl + 'onIsOnRadio.png) no-repeat scroll 0 0 #EBECED');
                    $(imgOn).css({'background-image': 'url(' + subUrl + 'onIsOnRadio.png)', 'background-repeat': 'no-repeat'});
                    $(imgOff).css({'background-image': 'url(' + subUrl + 'offIsOffRadio.png)', 'background-repeat': 'no-repeat'});
                }


                $("a.radio-picture").click(function () {
                    var id = $(this).attr('id');

                    if (id == "category_speed1") {
                        $("a#" + id).css('background-image', 'url(' + subUrl + '1-button.png)');
                        $("#speed_1").prop("checked", true);

                        $("#category_speed2").css('background-image', 'url(' + subUrl + '2-greyout.png)');
                        $("#category_speed5").css('background-image', 'url(' + subUrl + '5-greyout.png)');
                        $("#category_speed21").css('background-image', 'url(' + subUrl + 'speed-greyout.png)');

                    } else if (id == "category_speed2") {
                        $("a#" + id).css('background-image', 'url(' + subUrl + '2-button.png)');
                        $("#speed_2").prop("checked", true);

                        $("#category_speed1").css('background-image', 'url(' + subUrl + '1-greyout.png)');
                        $("#category_speed5").css('background-image', 'url(' + subUrl + '5-greyout.png)');
                        $("#category_speed21").css('background-image', 'url(' + subUrl + 'speed-greyout.png)');

                    } else if (id == "category_speed5") {
                        $("a#" + id).css('background-image', 'url(' + subUrl + '5-button.png)');
                        $("#speed_5").prop("checked", true);

                        $("#category_speed1").css('background-image', 'url(' + subUrl + '1-greyout.png)');
                        $("#category_speed2").css('background-image', 'url(' + subUrl + '2-greyout.png)');
                        $("#category_speed21").css('background-image', 'url(' + subUrl + 'speed-greyout.png)');

                    } else if (id == "category_speed21") {
                        $("a#" + id).css('background-image', 'url(' + subUrl + 'speed-button.png)');
                        $("#speed_21").prop("checked", true);

                        $("#category_speed1").css('background-image', 'url(' + subUrl + '1-greyout.png)');
                        $("#category_speed2").css('background-image', 'url(' + subUrl + '2-greyout.png)');
                        $("#category_speed5").css('background-image', 'url(' + subUrl + '5-greyout.png)');
                    } else {
                        $("a#" + id).css('background-image', 'url(' + subUrl + 'speed-button.png)');
                        $("#speed_21").prop("checked", true);

                        $("#category_speed1").css('background-image', 'url(' + subUrl + '1-greyout.png)');
                        $("#category_speed2").css('background-image', 'url(' + subUrl + '2-greyout.png)');
                        $("#category_speed5").css('background-image', 'url(' + subUrl + '5-greyout.png)');
                    }
                });


                $("a.radio-dpirules").click(function () {
                    var id = $(this).attr('id');//category_dpiRuleName_on
                    if (id.lastIndexOf("_on") !== -1) {
                        var dpiRuleName = id.substring(9, id.lastIndexOf("_on"));//9="category_".length

                        var imgOff = '#category_' + dpiRuleName + '_off';
                        var ruleNameRadio = '#' + dpiRuleName + 'on';
                        $('#' + id).css({'background-image': 'url(' + subUrl + 'onIsOnRadio.png)', 'background-repeat': 'no-repeat'});
                        $(imgOff).css({'background-image': 'url(' + subUrl + 'offIsOffRadio.png)', 'background-repeat': 'no-repeat'});
                        $(ruleNameRadio).prop("checked", true);
                    }
                    if (id.lastIndexOf("_off") !== -1) {
                        var dpiRuleName = id.substring(9, id.lastIndexOf("_off"));//9="category_".length
                        var imgOn = '#category_' + dpiRuleName + '_on';
                        var ruleNameRadio = '#' + dpiRuleName + 'on';
                        $(imgOn).css({'background-image': 'url(' + subUrl + 'onIsOffRadio.png)', 'background-repeat': 'no-repeat'});
                        $('#' + id).css({'background-image': 'url(' + subUrl + 'offIsOnRadio.png)', 'background-repeat': 'no-repeat'});
                        $(ruleNameRadio).prop("checked", true);
                    }

                });


                var serviceDialog = "";
                var serviceLinkId = "";

                for (i = 0; i < availableUserDefinedDPIRulesList.length; i++) {

                    var availableDPIRuleName = availableUserDefinedDPIRulesList[i];
                    var dialog = "#dialog_" + availableDPIRuleName;
                    var infoLink = "#info_link_" + availableDPIRuleName;

                    serviceDialog = serviceDialog + dialog + ",";
                    serviceLinkId = serviceLinkId + infoLink + ",";
                }

                var serviceDialogList = serviceDialog.substring(0, serviceDialog.lastIndexOf(","));
                var serviceLinkIdList = serviceLinkId.substring(0, serviceLinkId.lastIndexOf(","));

                $(serviceDialogList).dialog({
                    resizable: false,
                    modal: true,
                    width: 935,
                    buttons: {
                        Ok: function () {
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

                $(serviceLinkIdList).click(function () {
                    var id = $(this).attr('id');
                    var linkNamePrefix = "info_link_";
                    var linkNamePrefixLen = linkNamePrefix.length;
                    var linkName = id.substring(linkNamePrefixLen);

                    var dialogArray = serviceDialogList.split(",");
                    var dialogToShow = "";

                    for (var i = 0; i < dialogArray.length; i++) {
                        var dialogName = dialogArray[i];
                        var dialogNamePrefix = "#dialog_";
                        var dialogNamePrefixLen = dialogNamePrefix.length;
                        var name = dialogName.substring(dialogNamePrefixLen);

                        if (name == linkName) {
                            dialogToShow = dialogName;
                            break;
                        }
                    }

                    $(dialogToShow).dialog("option", "position", 'center');
                    $(dialogToShow).dialog("open");
                });


            });

            function set_radio($inputid) {
                $("input#" + $inputid).click();
            }
        </script>

        <style type="text/css"> 
            p {margin:20px 0;}

            label {font-weight:bold;}

            a.radio-picture {
                border: 1px solid transparent;
                display: inline-block;
                height: 46px;
                margin-right: 10px;
                text-decoration: none;
                width: 46px;
            }
            a.radio-dpirules {
                /*border: 1px solid transparent;*/
                display: inline-block;
                height: 46px;
                margin-right: 10px;
                text-decoration: none;
                width: 46px;
            }
            a.radio-picture:hover {
                border:1px dashed green;
            }
            /*a.radio-dpirules:hover {
                border:1px dashed green;
            }*/
            a.green-border {
                border:1px solid green;
            }
            a#category_speed1 {
                background: url("images/tmpFiles/1-greyout.png") no-repeat scroll 0 0 #EBECED;
            }
            a#category_speed2 {
                background: url("images/tmpFiles/2-greyout.png") no-repeat scroll 0 0 #EBECED;
            }
            a#category_speed5 {
                background: url("images/tmpFiles/5-greyout.png") no-repeat scroll 0 0 #EBECED;
            }
            a#category_speed21 {
                background: url("images/tmpFiles/speed-greyout.png") no-repeat scroll 0 0 #EBECED;
            }

            a#category_alldpirules {
                background: scroll 0 0 #EBECED;
            }

            .hidden {
                left: -10000px;
                position: absolute;
                top: -1000px;
            }
            input[type="submit"] {cursor:pointer;}

        </style>


    </stripes:layout-component>
    <stripes:layout-component name="contents">    
        <div style="margin-top: 10px;">
            <div class="configservicecustomer columns">
                <img src="images/customer2.png"><br>
                <font class="light" style="font-size:30px;"><fmt:message key="scp.basic.data"/></font><br>
                <font style="font-size:12px; color:#75B343;">${actionBean.customer.firstName} ${actionBean.customer.lastName}<br>
                <strong> <fmt:message key="customer.since"/>: </strong><br>
                <strong>${s:formatDateShort(actionBean.customer.createdDateTime)}</strong><br>
                </font>
            </div>

            <div class="twelve columns">

                <div class="manage_my_data_usage_top_container">
                    <div style="margin-left: 100px;">
                        <div class="manage_my_data_title_top">
                            <strong><fmt:message key="scp.service.manage.myspeed.title"/></strong>
                            <stripes:link href="/Product.action" event="showSliderInfoPage">
                                <img src="images/tmpFiles/information.png"/>
                            </stripes:link>
                        </div>

                        <stripes:form action="/Product.action">
                            <c:set var="canChangeAVPs" value="false"/>
                            <c:if test="${actionBean.allowedToChangeSpeed == true}">
                                <c:set var="canChangeAVPs" value="true"/>
                                <div>
                                    <div style="margin-bottom:5px;">
                                        <input type="radio" value="1" name="downAndUplinkInternetSpeed" id="speed_1" class="hidden" />
                                        <a id="category_speed1" href="javascript:set_radio('speed_1');" class="radio-picture">&nbsp;</a><font style="font-size:20px; margin-left: 30px;">up to 1 <fmt:message key="scp.slider.speed.units"/></font>
                                    </div>

                                    <div style="margin-bottom:5px;">
                                        <input type="radio" value="2" name="downAndUplinkInternetSpeed" id="speed_2" class="hidden" />
                                        <a id="category_speed2" href="javascript:set_radio('speed_2');" class="radio-picture">&nbsp;</a><font style="font-size:20px; margin-left: 30px;">up to 2 <fmt:message key="scp.slider.speed.units"/></font>
                                    </div>

                                    <div style="margin-bottom:5px;">
                                        <input type="radio" value="5" name="downAndUplinkInternetSpeed" id="speed_5" class="hidden" />
                                        <a id="category_speed5" checked='checked' href="javascript:set_radio('speed_5');" class="radio-picture">&nbsp;</a><font style="font-size:20px; margin-left: 30px;">up to 5 <fmt:message key="scp.slider.speed.units"/></font>
                                    </div>

                                    <div style="margin-bottom:5px;">
                                        <input type="radio" value="21" name="downAndUplinkInternetSpeed" id="speed_21" class="hidden" />
                                        <a id="category_speed21" href="javascript:set_radio('speed_21');" class="radio-picture">&nbsp;</a><font style="font-size:20px; margin-left: 30px;">Smile Speed up to 21 <fmt:message key="scp.slider.speed.units"/></font>
                                    </div>

                                </div>
                                <hr style="height:2px; background: green">
                            </c:if>

                            <c:forEach items="${s:getServiceSpecificationAvailableUserDefinedDPIRules(actionBean.serviceSpecification.serviceSpecificationId)}" var="rulename" varStatus="loop">
                                <c:set var="canChangeAVPs" value="true"/>
                                <c:set var="OddEvenDiv" value="${loop.count mod 2 == 0 ? 'even' : 'odd'}"/>
                                <c:choose>
                                    <c:when test="${OddEvenDiv eq 'odd'}">
                                        <div id="radio_${rulename}" style="margin-left:2px;float: left; margin-bottom:5px; width: 280px;">
                                            <strong><font style="font-size: 20px; font-weight:900;"><fmt:message key="scp.dpi.description.${rulename}"/></strong> <a id="info_link_${rulename}"> <img src="images/tmpFiles/information.png"/></a></font><br/>
                                            <input type="radio" value="off" name="${rulename}" id="${rulename}_off" class="hidden" />
                                            <a id="category_${rulename}_off" href="javascript:set_radio('${rulename}_off');" class="radio-dpirules category_alldpirules">&nbsp;</a>

                                            <input type="radio" value="on" name="${rulename}" id="${rulename}_on" class="hidden" />
                                            <a id="category_${rulename}_on" href="javascript:set_radio('${rulename}_on');" class="radio-dpirules category_alldpirules">&nbsp;</a>

                                            <div id="dialog_${rulename}" title="<fmt:message key="scp.dpi.rule.name.title.${rulename}"/>">
                                                <fmt:message key="scp.dpi.rule.name.info.msg.${rulename}"/>
                                            </div>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <div id="radio_${rulename}" style="margin-left:6px; float: left; margin-bottom:5px;">
                                            <strong><font style="font-size: 20px; font-weight:900;"><fmt:message key="scp.dpi.description.${rulename}"/></strong> <a id="info_link_${rulename}"> <img src="images/tmpFiles/information.png"/></a></font><br/>
                                            <input type="radio" value="off" name="${rulename}" id="${rulename}_off" class="hidden" />
                                            <a id="category_${rulename}_off" href="javascript:set_radio('${rulename}_off');" class="radio-dpirules category_alldpirules">&nbsp;</a>

                                            <input type="radio" value="on" name="${rulename}" id="${rulename}_on" class="hidden" />
                                            <a id="category_${rulename}_on" href="javascript:set_radio('${rulename}_on');" class="radio-dpirules category_alldpirules">&nbsp;</a>

                                            <div id="dialog_${rulename}" title="<fmt:message key="scp.dpi.rule.name.title.${rulename}"/>">
                                                <fmt:message key="scp.dpi.rule.name.info.msg.${rulename}"/>
                                            </div>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>

                            <hr style="height:2px; background: green">

                            <div>
                                <c:if test="${canChangeAVPs eq 'true'}">
                                    <div style="margin-bottom:5px;">
                                        <stripes:hidden name="productOrder.serviceInstanceOrders[0].serviceInstance.serviceSpecificationId" value="${actionBean.serviceInstance.serviceSpecificationId}"/>                
                                        <stripes:hidden name="productOrder.serviceInstanceOrders[0].serviceInstance.serviceInstanceId" value="${actionBean.serviceInstance.serviceInstanceId}"/>
                                        <stripes:hidden name="productOrder.productInstanceId" value="${actionBean.serviceInstance.productInstanceId}"/> 
                                        <stripes:hidden name="accountQuery.accountId" value="${actionBean.serviceInstance.accountId}"/>                
                                        <stripes:hidden name="userDefinedDPIRules" value="${actionBean.userDefinedDPIRules}"/>                
                                        <input style="margin-left:200px; margin-bottom: 10px;" class="manage_my_data_btn_confirm" type="submit" name="changeServiceInstanceConfiguration" value=""/>
                                    </div>
                                </c:if>
                                <c:if test="${canChangeAVPs eq 'false'}">
                                    <div style="margin-bottom:5px;">
                                        <fmt:message key="scp.avp.config.notsupported"/>
                                    </div>
                                </c:if>

                            </div>
                        </stripes:form>

                    </div>
                </div>

            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>
