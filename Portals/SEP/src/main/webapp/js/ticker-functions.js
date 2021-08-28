$j(function() {
	if (window.Prototype) {
		delete Object.prototype.toJSON;
		delete Array.prototype.toJSON;
		delete Hash.prototype.toJSON;
		delete String.prototype.toJSON;
	}
	if (typeof(Storage) !== "undefined") //does our browser support local storage
	{
		//get the global ticker messages (from cache)
		var tickerEntriesText = tickerMessagesText;

		var oldTickerEntriesText = sessionStorage.getItem("tickerEntriesText");

		if (tickerEntriesText != oldTickerEntriesText) {
			console.log("ticker messages have changed - updating global ticker entries");
			var globalEntry;

			if (tickerEntriesText != "") {
				var globalEntryArray = [];
				var entries = tickerEntriesText.split("|");
				for (var i = 0; i < entries.length; i++) {
					//now we will have each entry as 'text<link>'
					var entryData = entries[i].split("<");
					if (entryData.length != 2) {
						console.log("parse error in ticker text entry: " + entries[i]);
						return;
					}
					globalEntry = new Object();
					globalEntry.text = entryData[0];
					globalEntry.link = entryData[1].replace(/[<>]/g, "");
					globalEntry.visible = 1;
					globalEntryArray.push(globalEntry);
				}
				sessionStorage.setItem("globalTickers", JSON.stringify(globalEntryArray));
			} else {
				//clear our ticker of global entries
				sessionStorage.removeItem("globalTickers");
			}
			//set flag to say we are up to date
			sessionStorage.tickerEntriesText = tickerEntriesText;

		}

		if (sessionStorage.globalTickers || sessionStorage.userTickers) {
			$j('#js-news').ticker({
				titleText: 'PLEASE NOTE'
			});
		} else {
			console.log("no entries for ticker");
		}
	} else {
		alert("Please use an HTML5-compatible browser");
	}
});                   