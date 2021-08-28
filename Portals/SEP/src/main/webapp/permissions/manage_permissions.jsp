<%@ include file="/include/sep_include.jsp" %>

<c:set var="title">
    <fmt:message key="permissions"/>
</c:set>

<html>
    <head>
        <meta http-equiv="Content-type" content="text/html; charset=UTF-8" />
        <title>${title}</title>
        <meta http-equiv="imagetoolbar" content="no" />
        <meta name="MSSmartTagsPreventParsing" content="true" />
        <link rel="stylesheet" media="all" type="text/css" href="${pageContext.request.contextPath}/css/style.css"/>    
        <script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js"></script>

        <script type="text/javascript">

            function UpdateTableHeaders() {
                $("div.divTableWithFloatingHeader").each(function () {
                    var originalHeaderRow = $(".tableFloatingHeaderOriginal", this);
                    var floatingHeaderRow = $(".tableFloatingHeader", this);
                    var offset = $(this).offset();

                    var headerHeight = originalHeaderRow.height();
                    if (headerHeight < 1) {
                        headerHeight = $(".tableFloatingHeaderOriginal").height();
                    }

                    var scrollTop = $(window).scrollTop();
                    if (((scrollTop - headerHeight) > offset.top) && ((scrollTop - headerHeight) < offset.top + $(this).height())) {
                        floatingHeaderRow.css("position", "absolute");
                        floatingHeaderRow.css("visibility", "visible");
                        floatingHeaderRow.css("top", Math.min(scrollTop - offset.top, $(this).height() - floatingHeaderRow.height()) + "px");
                        floatingHeaderRow.css("border-width", "0px");
                        floatingHeaderRow.css("border-style", "solid");
                        floatingHeaderRow.css("border-color", "#639B2E");
                        floatingHeaderRow.css("padding", "0px");
                        floatingHeaderRow.css("color", "#fff");
                        floatingHeaderRow.css("left", 0);
                        floatingHeaderRow.css("background-color", " #AEC269");

                        // Copy cell widths from original header
                        $("th", floatingHeaderRow).each(function (index) {
                            var cellWidth = $("th", originalHeaderRow).eq(index).css('width');
                            $(this).css('width', 0 + parseInt(cellWidth));
                        });

                        // Copy row width from whole table
                        //floatingHeaderRow.css("width", $(this).css("width"));
                        floatingHeaderRow.css("width", parseInt($(".tableFloatingHeaderOriginal").css("width")) + 5);
                    }
                    else {
                        floatingHeaderRow.css("visibility", "hidden");
                        floatingHeaderRow.css("top", "0px");
                    }
                });
            }

            $(document).ready(function () {

                $("table.perms").each(function () {
                    $(this).wrap("<div class=\"divTableWithFloatingHeader\" style=\"position:relative\"></div>");

                    var originalHeaderRow = $("tr:first", this);
                    originalHeaderRow.before(originalHeaderRow.clone());
                    var clonedHeaderRow = $("tr:first", this);

                    clonedHeaderRow.addClass("tableFloatingHeader");
                    clonedHeaderRow.css("position", "absolute");
                    clonedHeaderRow.css("top", "0px");
                    clonedHeaderRow.css("visibility", "hidden");
                    originalHeaderRow.addClass("tableFloatingHeaderOriginal");
                });
                UpdateTableHeaders();
                $(window).scroll(UpdateTableHeaders);
                $(window).resize(UpdateTableHeaders);
            });
        </script>
        <%
            response.addHeader("Pragma", "No-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addDateHeader("Expires", 1);
        %>
        <!--[if IE]>
           <style>
              .rotate_text
              {
                 writing-mode: tb-rl;
                 filter: flipH() flipV();
              }
           </style>
        <![endif]-->
        <!--[if !IE]><!-->
        <style>
            .rotate_text
            {
                -moz-transform:rotate(-90deg); 
                -moz-transform-origin: top left;
                -webkit-transform: rotate(-90deg);
                -webkit-transform-origin: top left;
                -o-transform: rotate(-90deg);
                -o-transform-origin:  top left;
                position:relative;
                top:20px;
                width:23px;
            }
        </style>
        <!--<![endif]-->

        <style>  
            table.perms {
                border-width: 0px;
                border-style: solid;
                border-color: #639B2E;
                border-collapse: collapse;
                background-color: white;
                table-layout: fixed;
                padding: 0px;
            }
            table.perms th {
                border-width: 1px;
                padding: 1px;
                border-style: solid;
                border-color: #639B2E;
                color: #fff;
                background-color: #AEC269;
            }
            table.perms tr {
                border-width: 1px;
                padding: 1px;
                border-style: solid;
                border-color: #639B2E;
                background-color: #fff;

            }
            table.perms td {
                border-width: 1px;
                padding: 1px;
                border-style: solid;
                border-color: #639B2E;    
                color: #fff;
            }
            .rotated_cell
            {
                height:300px;
                vertical-align:bottom;
                border-width: 1px;
                padding: 1px;
                border-style: solid;
                border-color: #639B2E;
                color: #fff;
                background-color: #AEC269;
                width:23px;
            }
            .perms_cell{
                background-color: #AEC269; 
                color: #fff; 
                height: 23px;
            }

            td.peven {
                background-color: #E4ECC3;
            }
            td.podd {
                background-color: #fff;
            }
        </style>

    </head>
    <body>
        <stripes:form action="/Property.action">
            <c:set value="${actionBean.allPermissions}" var="AllPermissions"/>
            <c:set value="${actionBean.roles}" var="roles"/>            

            <table class="perms">
                <tr>
                    <th class="perms_cell"></th><th class="perms_cell"></th>
                        <c:forEach items="${actionBean.roles}" var="role">
                        <th class="rotated_cell">
                    <div class="rotate_text">${role.key}</div>
                </th>
            </c:forEach>  
        </tr>

        <c:forEach items="${AllPermissions}" var="permission"  varStatus="loop">
            <tr>
                <td class="perms_cell">${permission.value}</td>
                <td class="perms_cell">${permission.key}</td>
                <c:forEach items="${actionBean.roles}" var="role">
                    <c:set value="${false}" var="ischecked"/>
                    <c:forEach items="${role.value}" var="rolespermission">
                        <c:if test="${permission.key eq rolespermission}">
                            <td align="centre" class="${loop.count mod 2 == 0 ? "peven" : "podd"}">
                                <stripes:checkbox name="roles[${role.key}]" value ="${permission.key}">
                                    ${permission.key}
                                </stripes:checkbox>
                            </td>
                            <c:set var="ischecked" value="${true}"/>
                        </c:if>
                    </c:forEach>
                    <c:if test="${!ischecked}">
                        <td align="centre" class="${loop.count mod 2 == 0 ? "peven" : "podd"}">
                            <stripes:checkbox name="roles[${role.key}]" value ="${permission.key}"></stripes:checkbox>
                            </td>
                    </c:if>
                </c:forEach>
            </tr>
        </c:forEach>
        <tr>
            <td colspan="2">
                <stripes:submit name="updateRolePermissions"/> | <stripes:submit name="goHome" value="goHome"/>
            </td>
        </tr>
    </table>
</stripes:form>
</body>
</html>
