<%@ include file="/include/sep_include.jsp" %>

<script type='text/javascript'>
	var tickerMessagesText = "${s:getStringFromRemoteCache('env.sep.ticker.messages')}";
</script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/ticker-functions.js"></script>        
<div class="tickermessages">       
        <ul id="js-news" class="js-hidden">
	</ul>
</div>
