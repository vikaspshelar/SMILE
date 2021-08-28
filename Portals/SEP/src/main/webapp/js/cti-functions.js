var connected = false;
var websocket;

function bindEvents() {
	websocket.onopen = function() {
		// Web Socket is connected. You can send data by send() method
		var requestString = extensionNumber
			+ '|' + sessionStorage.lastCCIncomingCall;
		console.log('Sending request to CTI websocket: ' + requestString);
		websocket.send(requestString);
		keepWSAlive(websocket, 20000);
	};
	websocket.onmessage = function(evt) {
		console.log("received websocket data: " + evt.data);
		var eventDetails = evt.data.split(":");
		var lastCCIncomingCall = sessionStorage.lastCCIncomingCall;
		if (lastCCIncomingCall != eventDetails[0]) {//we have a fresh call coming in....
			var x = eventDetails[2];
			sessionStorage.setItem("lastCCIncomingCall", eventDetails[0]);   //update the new last call we acknowledged
			console.log("Received incoming call event with epoch: " + eventDetails[0]);
			Modalbox.show("<div class='warning'><p>Would you like to load the details for caller (" + x + ")?</p> <input type='button' value='Yes, please!' onclick=\"Modalbox.hide({afterHide: showCustomer('" + x + "')})\" /> or <input type='button' value='No thanks!' onclick='Modalbox.hide()' /></div>", {
				title: 'Incoming Call',
				width: 300
			});
		}
	};

	websocket.onclose = function() {
		console.log("websocket connection closed.... going to reconnect...");
		websocket = new WebSocket(CTIUrl);
		bindEvents();
	};
}

$j(document).ready(function() {
	console.log("connecting to CTI websocket");
	console.log(CTIUrl);
	websocket = new WebSocket(CTIUrl);
	bindEvents();
});


