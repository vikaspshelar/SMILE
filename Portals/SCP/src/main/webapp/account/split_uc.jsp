<%@include file="/include/scp_include.jsp" %>
<c:set var="title">
    <fmt:message key="scp.split.uc"/>
</c:set>
<stripes:layout-render name="/layout/standard.jsp" title="${title}">
    <stripes:layout-component name="contents">
        <div style="margin-top: 10px;" class="sixteen columns">
            <script type="text/javascript" xml:space="preserve">
                var submitClicked = false;
            </script>

            <jsp:include page="/layout/my_details_left_banner.jsp"/>

            <c:set var="ucs" value="${s:getUnitCreditSpecification(actionBean.unitCreditInstance.unitCreditSpecificationId)}"/>

            <div style="margin-top: 6px; float: left; margin-left: 100px;" class="accounts_transfer_table six columns">
                <stripes:form action="/Account.action">
                    <stripes:hidden name="account.accountId" value="${actionBean.unitCreditInstance.accountId}"/>
                    <stripes:hidden name="accountQuery.accountId" value="${actionBean.unitCreditInstance.accountId}"/>
                    <stripes:hidden name="splitUnitCreditData.unitCreditInstanceId" value="${actionBean.unitCreditInstance.unitCreditInstanceId}"/>
                    <font style="color:#75B343; font-weight:bold;">Target Account ID:</font><br/>
                    <stripes:text name="splitUnitCreditData.targetAccountId" style="width:150px" maxlength="10"/>

                    <table>

                        <tr>
                            <td colspan="4">
                                <p ng-show="accountsPerPage > 1">AND/OR - Select account/s from below:</p>
                                <div ng-init="getResultsPage(1)"></div>
                            </td>
                        </tr>

                        <tr ng-if="accountsPerPage > 1" dir-paginate="account in usersAccounts | itemsPerPage: accountsPerPage" total-items="pendingPagesCount" current-page="pagination.current">
                            <td ng-if="account.accountId != ${actionBean.unitCreditInstance.accountId}" style="white-space: nowrap">{{ account.accountId}} -- <input type="checkbox" name="splitDataTargetAccount" ng-value="{{account.accountId}}"></td>
                        </tr>

                        <tr>
                            <td>
                                <div ng-show="pendingPagesCount > accountsPerPage" class="sixteen columns">
                                    <small>Navigate pages</small>
                                    <div class="fifteen columns">
                                        <dir-pagination-controls on-page-change="pageChangeHandler(newPageNumber)"></dir-pagination-controls>
                                    </div>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2"/>
                        </tr>
                        <tr>
                           <td style="vertical-align:top">OR</td>
                        </tr>  
                        <tr>
                            <td style="vertical-align:top">
                                <strong>Paste list of target accounts ID. One account ID per line:</strong><br/>
                            </td>
                        </tr>
                        <tr>
                            <td style="vertical-align:top">
                            <stripes:textarea name="AccountIdList" cols="55" rows="10"></stripes:textarea>
			    </td>
                        </tr> 
                        
                    </table>



                    <br/><br/><font style="color:#75B343; font-weight:bold;">Volume:</font><br/>
                    <stripes:select name="splitUnitCreditData.units">
                        <stripes:option value="209715200">200 MB</stripes:option>
                        <stripes:option value="524288000">500 MB</stripes:option>
                        <stripes:option value="1024000000">1 GB</stripes:option>
                        <stripes:option value="2048000000">2 GB</stripes:option>
                        <stripes:option value="3072000000">3 GB</stripes:option>
                        <stripes:option value="5120000000">5 GB</stripes:option>
                        <stripes:option value="7168000000">7 GB</stripes:option>
                        <stripes:option value="10240000000">10 GB</stripes:option>
                        <stripes:option value="15360000000">15 GB</stripes:option>
                        <stripes:option value="20480000000">20 GB</stripes:option>
                        <stripes:option value="51200000000">50 GB</stripes:option>
                        <stripes:option value="102400000000">100 GB</stripes:option>
                        <stripes:option value="153600000000">150 GB</stripes:option>
                        <stripes:option value="${actionBean.unitCreditInstance.availableUnitsRemaining}">Remaining Balance - ${s:displayVolumeAsString(actionBean.unitCreditInstance.availableUnitsRemaining, ucs.unitType)}</stripes:option>
                    </stripes:select>  
                    <br/>
                    <%--<input  class="transfer_btn" type="submit" name="splitUnitCreditInstance" value=""/>--%>
                    <input class="button_split_uc" type="submit" name="splitUnitCreditInstance" value="<fmt:message key="scp.split.transfer.uc.btn"/>"/>
                </stripes:form>
            </div>
        </div>
    </stripes:layout-component>
</stripes:layout-render>


