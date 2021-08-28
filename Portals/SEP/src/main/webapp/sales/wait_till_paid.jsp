<%@ include file="/include/sep_include.jsp" %>


<c:set var="title">
    <fmt:message key="view.sale"/>
</c:set>

<stripes:layout-render name="/layout/standard.jsp" title="${title}">

    <stripes:layout-component name="html_head">        

        <script type="text/javascript">
            var isShowing = false;
            function poll() {
                if (!isShowing) {
                    console.log("inside the poll function");
                    var msg = '<h1>Processing. Please wait...</h1><br/><br/> ' +
                            '<font size=\"6\">Please refer to Card Device to complete payment of <b>${s:convertCentsToCurrencyLongRoundHalfEven(actionBean.sale.totalLessWithholdingTaxCents)}.</b><br/>' +
                            'Sale ID is: <b>${actionBean.sale.saleId}.</b><br/>' +
                            'You will be redirected once the payment is successfully processed by the bank. </font>';

                    showDialog(msg);
                    isShowing = true;
                }
                setInterval(function() {
                    new Ajax.Request('/sep/Sales.action;' + getJSessionId() + '?getSaleStatus=&sale.saleId=${actionBean.sale.saleId}', {
                        method: 'get',
                        onSuccess: function(transport) {
                            console.log("Inside Transport onSuccess Method - " + transport.responseText);
                            if (transport.responseText === 'PD') {
                                $j.unblockUI();

                                window.location = "/sep/Sales.action;" + getJSessionId() + "?showSaleAfterPaid=&sale.saleId=${actionBean.sale.saleId}";
                                $j(function() {                                    
                                    showSuccess();
                                    //this is mighty odd.
                                    setTimeOut(function() {
                                    }, 5000);

                                });                                
                                isShowing = false;
                            } else {
                                poll();
                            }
                        },
                    });
                }, 5000);
            }
            window.onload = function() {
                poll();
            };
            function showSuccess() {                
                $j.blockUI({
                    message: $j('div.notificationUI'),
                    fadeIn: 700,
                    fadeOut: 700,
                    timeout: 3000,
                    showOverlay: false,
                    centerY: false,
                    css: {
                        width: '350px',
                        top: '10px',
                        left: '',
                        right: '10px',
                        border: 'none',
                        padding: '5px',
                        backgroundColor: '#000',
                        '-webkit-border-radius': '10px',
                        '-moz-border-radius': '10px',
                        opacity: .6,
                        color: '#fff'
                    }
                });
            }
            function showDialog(msg) {
                $j.blockUI({message: msg, css: {
                        '-webkit-border-radius': '10px',
                        '-moz-border-radius': '10px',
                        border: '5px solid #52962A',
                        padding: '10px'
                    }});
            }
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">        
        <div class="notificationUI" style="display: none">
            <img width="50px" height="50px" style="float: left; margin-left: 5px;margin-top: 5px;" src="${pageContext.request.contextPath}/images/check.png" alt=":-)"/>
            <h1>Smile Notification</h1>
            <h2>Payment successful...</h2>
        </div>
    </stripes:layout-component>
</stripes:layout-render>

