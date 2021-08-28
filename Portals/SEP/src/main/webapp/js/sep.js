var myMenu;
window.onload = function () {
    myMenu = new SDMenu("my_menu");
    myMenu.init();
}
window.onunload = function () {
    if (ajaxRequest != null) {
        unloading = true;
        ajaxRequest.transport.abort();
    }
}


/*
 -------------------------------------------------------------------------------
 GPS Utils
 -------------------------------------------------------------------------------
 */

function convertToDMS(latDeg, longDeg) {

    var longEW = 'E';
    if (longDeg < 0) {
        longEW = 'W';
    }
    longDeg = Math.abs(longDeg);

    var latNS = 'N';
    if (latDeg < 0) {
        latNS = 'S';
    }
    latDeg = Math.abs(latDeg);

    var gpsLongdeg = parseInt(longDeg);
    var remainderLong = longDeg - (gpsLongdeg * 1.0);
    var gpsLongmin = remainderLong * 60.0;
    var remainderLong2 = gpsLongmin - (parseInt(gpsLongmin) * 1.0);
    var gpsLongsec = (Math.round(remainderLong2 * 60.0 * 10.0)) / 10;
    if (gpsLongdeg < 10) {
        gpsLongdeg = '0' + gpsLongdeg;
    }
    gpsLongmin = parseInt(gpsLongmin);
    if (gpsLongmin < 10) {
        gpsLongmin = '0' + gpsLongmin;
    }
    if (gpsLongsec < 10) {
        gpsLongsec = '0' + gpsLongsec;
    }
    if (gpsLongsec == parseInt(gpsLongsec)) {
        gpsLongsec = gpsLongsec + '.0';
    }
    var gpsLong = gpsLongdeg + ' ' + gpsLongmin + ' ' + gpsLongsec + ' ' + longEW;

    var gpsLatdeg = parseInt(latDeg);
    var remainderLat = latDeg - (gpsLatdeg * 1.0);
    var gpsLatmin = remainderLat * 60.0;
    var remainderLat2 = gpsLatmin - (parseInt(gpsLatmin) * 1.0);
    var gpsLatsec = (Math.round(remainderLat2 * 60.0 * 10.0)) / 10;
    if (gpsLatdeg < 10.0) {
        gpsLatdeg = '0' + gpsLatdeg;
    }
    gpsLatmin = parseInt(gpsLatmin);
    if (gpsLatmin < 10.0) {
        gpsLatmin = '0' + gpsLatmin;
    }
    if (gpsLatsec < 10.0) {
        gpsLatsec = '0' + gpsLatsec;
    }
    if (gpsLatsec == parseInt(gpsLatsec)) {
        gpsLatsec = gpsLatsec + '.0';
    }
    var gpsLat = gpsLatdeg + ' ' + gpsLatmin + ' ' + gpsLatsec + ' ' + latNS;

    return gpsLat + ' ' + gpsLong;

}

function convertDMSToDegree(deg, min, sec) {
    dec_min = (min * 1.0 + (sec / 60.0));
    return deg * 1.0 + (dec_min / 60.0);
}

function getLatInDegrees(gps) {

    isSouth = 1;
    if (gps.substring(11, 12) == "S") {
        isSouth = -1;
    }
    deg1 = 0;
    min1 = 0;
    sec1 = 0;
    deg1 = gps.substring(0, 2);
    min1 = gps.substring(3, 5);
    sec1 = gps.substring(6, 10);
    return isSouth * convertDMSToDegree(deg1, min1, sec1);

}

function getLngInDegrees(gps) {

    isWest = 1;
    if (gps.substring(24) == "W") {
        isWest = -1;
    }
    deg1 = 0;
    min1 = 0;
    sec1 = 0;
    deg1 = gps.substring(13, 15);
    min1 = gps.substring(16, 18);
    sec1 = gps.substring(19, 23);
    return isWest * convertDMSToDegree(deg1, min1, sec1);

}

/*
 -------------------------------------------------------------------------------
 Left Menu
 -------------------------------------------------------------------------------
 */
function SDMenu(id) {
    if (!document.getElementById || !document.getElementsByTagName)
        return false;
    this.menu = document.getElementById(id);
    if (this.menu == null) {
        return false;
    }
    this.submenus = this.menu.getElementsByTagName("div");
    this.remember = true;
    this.speed = 3;
    this.markCurrent = true;
    this.oneSmOnly = false;
    return true;
}
SDMenu.prototype.init = function () {
    var mainInstance = this;
    if (this.menu == null) {
        return;
    }
    for (var i = 0; i < this.submenus.length; i++)
        this.submenus[i].getElementsByTagName("span")[0].onclick = function () {
            mainInstance.toggleMenu(this.parentNode);
        };
    if (this.markCurrent) {
        var links = this.menu.getElementsByTagName("a");
        for (var i = 0; i < links.length; i++)
            if (links[i].href == document.location.href) {
                links[i].className = "current";
                break;
            }
    }
    if (this.remember) {
        var regex = new RegExp("sdmenu_" + encodeURIComponent(this.menu.id) + "=([01]+)");
        var match = regex.exec(document.cookie);
        if (match) {
            var states = match[1].split("");
            for (var i = 0; i < states.length; i++)
                this.submenus[i].className = (states[i] == 0 ? "collapsed" : "");
        }
    }
};
SDMenu.prototype.toggleMenu = function (submenu) {
    if (submenu.className == "collapsed")
        this.expandMenu(submenu);
    else
        this.collapseMenu(submenu);
};
SDMenu.prototype.expandMenu = function (submenu) {
    var fullHeight = submenu.getElementsByTagName("span")[0].offsetHeight;
    var links = submenu.getElementsByTagName("a");
    for (var i = 0; i < links.length; i++)
        fullHeight += links[i].offsetHeight;
    var moveBy = Math.round(this.speed * links.length);

    var mainInstance = this;
    var intId = setInterval(function () {
        var curHeight = submenu.offsetHeight;
        var newHeight = curHeight + moveBy;
        if (newHeight < fullHeight)
            submenu.style.height = newHeight + "px";
        else {
            clearInterval(intId);
            submenu.style.height = "";
            submenu.className = "";
            mainInstance.memorize();
        }
    }, 30);
    this.collapseOthers(submenu);
};
SDMenu.prototype.collapseMenu = function (submenu) {
    var minHeight = submenu.getElementsByTagName("span")[0].offsetHeight;
    var moveBy = Math.round(this.speed * submenu.getElementsByTagName("a").length);
    var mainInstance = this;
    var intId = setInterval(function () {
        var curHeight = submenu.offsetHeight;
        var newHeight = curHeight - moveBy;
        if (newHeight > minHeight)
            submenu.style.height = newHeight + "px";
        else {
            clearInterval(intId);
            submenu.style.height = "";
            submenu.className = "collapsed";
            mainInstance.memorize();
        }
    }, 30);
};
SDMenu.prototype.collapseOthers = function (submenu) {
    if (this.oneSmOnly) {
        for (var i = 0; i < this.submenus.length; i++)
            if (this.submenus[i] != submenu && this.submenus[i].className != "collapsed")
                this.collapseMenu(this.submenus[i]);
    }
};
SDMenu.prototype.expandAll = function () {
    var oldOneSmOnly = this.oneSmOnly;
    this.oneSmOnly = false;
    for (var i = 0; i < this.submenus.length; i++)
        if (this.submenus[i].className == "collapsed")
            this.expandMenu(this.submenus[i]);
    this.oneSmOnly = oldOneSmOnly;
};
SDMenu.prototype.collapseAll = function () {
    for (var i = 0; i < this.submenus.length; i++)
        if (this.submenus[i].className != "collapsed")
            this.collapseMenu(this.submenus[i]);
};
SDMenu.prototype.memorize = function () {
    if (this.remember) {
        var states = new Array();
        for (var i = 0; i < this.submenus.length; i++)
            states.push(this.submenus[i].className == "collapsed" ? 0 : 1);
        var d = new Date();
        d.setTime(d.getTime() + (30 * 24 * 60 * 60 * 1000));
        document.cookie = "sdmenu_" + encodeURIComponent(this.menu.id) + "=" + states.join("") + "; expires=" + d.toGMTString() + "; path=/";
    }
};

/*
 -------------------------------------------------------------------------------
 Paragraph Expand/Collapse
 -------------------------------------------------------------------------------
 */


function toggleMe(a) {
    var e = document.getElementById(a);
    if (!e)
        return true;
    if (e.style.display == "none") {
        e.style.display = "block"
    } else {
        e.style.display = "none"
    }
    return true;
}


/* Functions for Ajax of Callcentre CTI integration */
var unloading = false;
var ajaxRequest = null;

function checkForCalls(pars) {
    if (ajaxRequest != null) {
        ajaxRequest.transport.abort();
    }
    var params = pars;
    new Ajax.Request('/sep/Callcentre.action;' + getJSessionId(), {
        method: 'post',
        parameters: params,
        onSuccess: function (transport) {
            if (!transport.responseText.match('nothing')) {
                var z = transport.responseText + 'empty';
                if (z != 'empty') {
                    var x = transport.responseText;
                    Modalbox.show("<div class='warning'><p>Would you like to load the details for caller (" + x + ")?</p> <input type='button' value='Yes, please!' onclick=\"Modalbox.hide({afterHide: showCustomer('" + x + "')})\" /> or <input type='button' value='No thanks!' onclick='Modalbox.hide()' /></div>", {
                        title: 'Incoming Call',
                        width: 300
                    });
                }
            }
            if (!unloading) {
                checkForCalls(pars);
            }



        },
        onCreate: function (request) {
            ajaxRequest = request;
        }
    });
}

function showCustomer(phoneNumber) {
//    var num = phoneNumber.toString();
    window.location = "/sep/Customer.action;" + getJSessionId() + "?retrieveCustomer=&customerQuery.resultLimit=1&customerQuery.alternativeContact=" + phoneNumber;
}


function checkAll(field) {

    for (i = 0; i < field.length; i++)
        field[i].checked = true;
}

function uncheckAll(field)
{
    for (i = 0; i < field.length; i++)
        field[i].checked = false;
}

function checkInvert(field)
{
    for (i = 0; i < field.length; i++)
        if (field[i].checked == false) {
            field[i].checked = true;
        } else {
            field[i].checked = false;
        }
}

/* END OF CTI JavaScript Functions */


/* FORM CONFIRMATION */

function confirmSubmit(msg) {
    var agree = confirm(msg);
    if (agree) {
        return true;
    } else {
        return false;
    }
}

function previousPage() {
    window.history.back()
}

function previousPreviousPage() {
    window.history.go(-2);
}

function calculateLuhn(Luhn) {
    var sum = 0;
    for (i = 0; i < Luhn.length; i++)
    {
        sum += parseInt(Luhn.substring(i, i + 1));
    }
    var delta = new Array(0, 1, 2, 3, 4, -4, -3, -2, -1, 0);
    for (i = Luhn.length - 1; i >= 0; i -= 2)
    {
        var deltaIndex = parseInt(Luhn.substring(i, i + 1));
        var deltaValue = delta[deltaIndex];
        sum += deltaValue;
    }
    var mod10 = sum % 10;
    mod10 = 10 - mod10;
    if (mod10 == 10)
    {
        mod10 = 0;
    }
    return mod10;
}

function validateLuhn(Luhn) {
    if (!(!isNaN(parseFloat(Luhn)) && isFinite(Luhn))) {
        return false;
    }
    var LuhnDigit = parseInt(Luhn.substring(Luhn.length - 1, Luhn.length));
    var LuhnLess = Luhn.substring(0, Luhn.length - 1);
    if (calculateLuhn(LuhnLess) == LuhnDigit)
    {
        return true;
    }
    return false;
}

function validateVisaDate(input) {
    if (input.id === "datePicker3") {
        var date = Date.parse(input.value);
        if (date === 'For-Life') {
            input.className = 'validationOk';
        } else if (date < Date.now()) {
            input.className = 'validationNotOk';
        }
    }
}

function isNumberKey(evt)
    {
       var charCode = (evt.which) ? evt.which : evt.keyCode;
       if (charCode != 46 && charCode > 31 
         && (charCode < 48 || charCode > 57))
          return false;

       return true;
    }

function validate(textbox, regex, otherrules) {
    var textBoxValue = textbox.value;
    if (otherrules.indexOf("emptyok") != -1 && textBoxValue.length == 0) {
        textbox.className = "validationOk";
        return;
    }
    var oREGEXP = new RegExp(regex);

    if (oREGEXP.test(textBoxValue)) {
        textbox.className = "validationOk";
        validateVisaDate(textbox);
    } else {
        if (textBoxValue === 'For-Life') {
            textbox.className = "validationOk";
        } else {
            textbox.className = "validationNotOk";
        }
        return;
    }
    if (otherrules.indexOf("luhn") != -1) {
        if (validateLuhn(textBoxValue)) {
            textbox.className = "validationOk";
        } else {
            textbox.className = "validationNotOk";
            return;
        }
    }
}

function alertValidationErrorsForElement(form) {
    if (typeof skipValidate != 'undefined' && skipValidate == true) {
        console.log('Skipping validation');
        return true;
    }
    if (!doAllTextBoxesValidate(form) || !doAllSelectionBoxesValidate(form) || !doAllTextAreasValidate(form)) {
        document.getElementById("validationpicture").innerHTML = "<img src='/sep/images/validation.png'/>";
        setTimeout(
                function () {
                    try {
                        document.getElementById("validationpicture").innerHTML = "";
                    } catch (e) {
                        console.log(e.toString());
                    }
                }, 1000);
        return false;
    }
    return true;
}

function alertValidationErrors() {
    return alertValidationErrorsForElement(document);
}

function doAllTextBoxesValidate(form) {
    var boxes = form.getElementsByTagName("INPUT");
    for (var i = 0; i < boxes.length; i++) {
        console.log('In doAllTextBoxesValidate for type ' + boxes[i].type + ' with id ' + boxes[i].id);
        if (boxes[i].type == 'text' || boxes[i].type == 'file') {
            try {
                boxes[i].onkeyup();
            } catch (e) {
                console.log("Error at element - boxes[i].name = " + boxes[i].name + " boxes[i].className = " + boxes[i].className);
                console.log('Error in doAllTextBoxesValidate ' + e);
            }
        }
    }

    for (i = 0; i < boxes.length; i++) {
        if ((boxes[i].type == 'text' || boxes[i].type == 'file') && boxes[i].className == 'validationNotOk') {
            console.log("In returning FALSE " + boxes[i].name + " - className = " + boxes[i].className);
            return false;
        }
    }

    return true;
}

function doAllTextAreasValidate(form) {
    var boxes = form.getElementsByTagName("TEXTAREA");
    for (var i = 0; i < boxes.length; i++) {
        console.log('In doAllTextAreasValidate for type ' + boxes[i].type + ' with id ' + boxes[i].id);
        if (boxes[i].type == 'textarea') {
            try {
                boxes[i].onkeyup();
            } catch (e) {
                console.log("Error at element - boxes[i].name = " + boxes[i].name + " boxes[i].className = " + boxes[i].className);
                console.log('Error in doAllTextAreasValidate ' + e);
            }
        }
    }

    for (i = 0; i < boxes.length; i++) {
        if ((boxes[i].type == 'textarea') && boxes[i].className == 'validationNotOk') {
            console.log("In returning FALSE " + boxes[i].name + " - className = " + boxes[i].className);
            return false;
        }
    }

    return true;
}

function doAllSelectionBoxesValidate(form) {
    var boxes = form.getElementsByTagName("SELECT");
    for (var i = 0; i < boxes.length; i++) {
        try {
            boxes[i].onchange();
        } catch (e) {
        }
    }
    for (i = 0; i < boxes.length; i++) {

        if (boxes[i].className == 'validationNotOk') {
            return false;
        }
    }
    return true;
}



setTimeout(
        function () {
            try {
                doAllTextBoxesValidate();
                doAllTextAreasValidate();
                doAllSelectionBoxesValidate();
            } catch (e) {
            }
        }, 1);

// Called as: document.getElementsByRegex("pattern", "tag").
// Returns an array of all elements of type "tag" matching a given regular expression on id.
// 'pattern' argument is a regular expression string.
document['getElementsByRegex'] = function (pattern, tag) {
    var arrElements = [];   // to accumulate matching elements
    var re = new RegExp(pattern);   // the regex to match with

    var tagElements = document.getElementsByTagName(tag);
    if (tagElements) {
        for (var idx = 0; idx < tagElements.length; idx++) {
            if (tagElements[idx] !== undefined)
                if (tagElements[idx].id !== undefined && tagElements[idx].id.search(re) != -1)
                    arrElements.push(tagElements[idx]);  // FOUND ONE!
        }
    }

    return arrElements; // return matching elements
}

/* Code to manage the image table entries */
/* Code to manage the image table entries */
function addRow(id, arrayName) {

    var tbody = document.getElementById(id).getElementsByTagName("TBODY")[0];
    var rowCount = document.getElementById(id).rows.length;

    var row = document.createElement("TR");
    var rowId = "row" + rowCount;

    row.setAttribute("id", rowId);
    row.setAttribute("name", rowId);

    var td1 = document.createElement("TD")

    var selectDocTypes = document.createElement("SELECT");

    selectDocTypes.setAttribute("id", "photoType" + rowCount);

    selectDocTypes.setAttribute("name", arrayName + "[" + rowCount + "].photoType");
    selectDocTypes.setAttribute("onchange", "validate(this,'^[a-zA-Z]{1,50}$','emptynotok')");

    var myOptions = document.getElementById('customer.document.types').options;

    var newOption = null;

    for (var i = 0; i < myOptions.length; i++) {
        newOption = document.createElement('option');
        newOption.value = myOptions[i].value;
        newOption.text = myOptions[i].text;
        selectDocTypes.appendChild(newOption);
    }

    td1.appendChild(selectDocTypes);

    td1.setAttribute('valign', 'top');
    td1.setAttribute('align', 'left');

    var td2 = document.createElement("TD")
    td2.innerHTML = "<div id='imgDiv" + +rowCount + "'><img class='thumb' id='imgfile" + rowCount + "' src='images/upload.png' /></div>";
    td2.setAttribute('valign', 'top');
    td2.setAttribute('align', 'left');

    var td3 = document.createElement("TD")
    td3.innerHTML = "<input type='button' class='file' value = 'Browse ...' onclick =\"javascript:document.getElementById('file" + rowCount + "').click();\" />";
    if (typeof useWebCam == "undefined") { // Use WebCam by default 
        td3.innerHTML += "<input type='button' value='WebCam' class='file' id='snap' onclick=\"snapAndUpload('file" + rowCount + "')\"/>";
    } else
    if (useWebCam) {
        td3.innerHTML += "<input type='button' value='WebCam' class='file' id='snap' onclick=\"snapAndUpload('file" + rowCount + "')\"/>";
    }
    td3.innerHTML += "<input type='button' value='Remove' class='file' onclick=\"removeDocument(this,'" + arrayName + "');\"/>";
    td3.innerHTML += "<input type='hidden' id='photoGuid" + rowCount + "' name='" + arrayName + "[" + rowCount + "].photoGuid' value='' />";
    td3.innerHTML += "<input type='file'  id='file" + rowCount + "' name='file" + rowCount + "' style='visibility: hidden;'/>";

    td3.setAttribute('valign', 'top');
    td3.setAttribute('align', 'left');

    row.appendChild(td1);
    row.appendChild(td2);
    row.appendChild(td3);

    tbody.appendChild(row);

    var file = document.getElementById('file' + rowCount);

    if (file.addEventListener) {
        file.addEventListener('change', uploadDocument, false);
    } else if (file.attachEvent) {
        file.attachEvent('onchange', uploadDocument);
    }

}


function removeFingerprints() {

    var url = 'http://127.0.0.1:8888/?';
    var oXHR = getXMLHttprequest();

    try {

        oXHR.onreadystatechange = function () {
            if (oXHR.readyState == 4)
            {
                if (oXHR.status == 200 && oXHR.responseText == 'Done') {
                    // alert("Successfully cleared fingerprints. RESPONSE: "+  oXHR.responseText);
                    // All g
                } else {
                    alert("Error while trying to clear fingerprints (Error Message: " + oXHR.responseText + ")(Error Code: " + oXHR.status + "). Please try again, if this error persists, contact IT support.");
                }
            }
        }


        oXHR.open('GET', url, true, "", "");
        oXHR.send("");

    } catch (e) {
        alert("Error trying to clear fingerprints." + e);
    }
}

/*
 function removeFingerprints() {
 // Also delete the image from the staging folder on the server
 try {
 
 var url = 'http://127.0.0.1:8888/?';
 var async = false;
 
 var oXHR = new XMLHttpRequest();
 if ('withCredentials' in xhr) {
 oXHR.open('GET', url, async);
 } else if (typeof XDomainRequest != "undefined") {
 oXHR = new XDomainRequest(); //for IE
 oXHR.open('GET', url);
 } else {
 oXHR.open('GET', url, async);
 }
 
 console.log("Opened XMLHttpRequest");
 oXHR.withCredentials = "true";
 oXHR.setRequestHeader('Access-Control-Allow-Origin', '*');
 oXHR.setRequestHeader('Access-Control-Allow-Methods', 'GET');
 
 oXHR.send();
 
 if (oXHR.status==200) {
 alert("Successfully cleared fingerprints. RESPONSE: "+  oXHR.responseText);
 } else {
 alert("Error while trying to clear fingerprints (Error Message: " + oXHR.responseText + ")(Error Code: " + oXHR.status + "). Please try again, if this error persists, contact IT support.");  
 }
 
 /*
 oXHR.onreadystatechange=function() {
 if (oXHR.readyState==4 && oXHR.status==200) {
 alert("Successfully cleared fingerprints. RESPONSE: "+  oXHR.responseText);
 } else {
 alert("Error while trying to clear fingerprints (Error Message: " + oXHR.responseText + ")(Error Code: " + oXHR.status + "). Please try again, if this error persists, contact IT support.");  
 }
 } 
 // return ((oXHR.status == 200) ? true : false);                    
 } catch (e) {
 alert("Error trying to clear fingerprints." + e);
 }
 } */

function removeDocument(src, arrayName) {
    var oRow = src.parentNode.parentNode;
    var fileId = oRow.rowIndex;

    var photoGuid = document.getElementById('photoGuid' + fileId).value;

    // Also delete the image from the staging folder on the server
    try {
        var vFormData = new FormData();
        var oXHR = new XMLHttpRequest();
        oXHR.open('POST', '/sep/FileUploadServlet;' + getJSessionId() + '?deleteFile=YES&currentGuid=' + photoGuid, false);
        oXHR.send(vFormData);
    } catch (e) {
    }

    document.getElementById("tblDocuments").deleteRow(oRow.rowIndex);
    // Rename all the form elements on each row of the table so that they are consistent with Stripes mapping ...
    renameRowControls(arrayName);

}

function renameRowControls(arrayName) {
    // Get all the file controls and rename them accordingly
    var files = document.getElementsByRegex('^file.*', "input");
    // Get all the hidden input controls which are storing photoType
    var photoTypes = document.getElementsByRegex('^photoType.*', "select");
    // Get all the hidden controls that are storing photoGuids
    var photoGuids = document.getElementsByRegex('^photoGuid.*', "input");


    var rows = document.getElementsByRegex('^row.*', "tr");

    for (var i = 0; i < document.getElementById('tblDocuments').rows.length; i++) {
        files[i].id = "file" + i;
        photoTypes[i].id = "photoType" + i;
        photoTypes[i].name = arrayName + "[" + i + "].photoType";
        photoGuids[i].id = "photoGuid" + i;
        photoGuids[i].name = arrayName + "[" + i + "].photoGuid";

        rows[i].id = "row" + i;
        rows[i].name = "row" + i;
    }
}

function uploadFinish(e) { // Upload successfully finished
// Do nothing so far ...
}

function removeOptions(selectbox) {
    var i;
    for (i = selectbox.options.length - 1; i >= 0; i--)
    {
        selectbox.remove(i);
    }
}

function removeRow(trId) {
    var roleName = document.getElementsByRegex('^roleName.*', 'input');
    var organisationName = document.getElementsByRegex('^organisationName.*', 'input');
    var rows = document.getElementsByRegex('^row.*', 'tr');

    $j(document.getElementById(trId)).remove();

    /*for(var i = 0; i < document.getElementById('roleTable').rows.length; i++){         
     roleName[i].name = "customer.customerRoles["+ i +"].roleName";
     organisationName[i].name = "customer.customerRoles["+ i +"].organisationName";
     rows[i].id = "row" + i;
     }*/
    $j(document.getElementById("deleteCustomerRole")).click();
}

function addRoleToTable(tableId, items) {
    var rowCount = document.getElementById(tableId).rows.length - 1;
    var customerId = document.getElementById("customerId");
    var options = document.getElementById("organisation.list").options;

    $j(document.getElementById(tableId)).find('tbody')
            .append($j("<tr>")
                    .attr('id', 'row' + rowCount)
                    .append($j('<td>')
                            .append($j('<select>')
                                    .attr('id', 'organisation.list')
                                    .attr('name', items + '[' + rowCount + '].organisationId')
                                    .append(
                                            $j.each(options, function (index) {
                                                $j(document.createElement('option')).attr('value', options[index].value).text(options[index].text);
                                            })
                                            )
                                    )
                            .append($j('<input>')
                                    .attr('type', 'hidden')
                                    .attr('name', items + '[' + rowCount + '].customerId')
                                    .attr('value', customerId.value)
                                    )
                            )
                    .append($j('<td>')
                            .append($j('<input>')
                                    .attr('name', items + '[' + rowCount + '].roleName')
                                    .attr('placeholder', 'Role Name')
                                    )
                            )
                    .append($j('<td>')
                            .append($j('<input>')
                                    .attr('type', 'button')
                                    .attr('value', 'Remove')
                                    .attr('onclick', 'removeRow(\'row' + rowCount + '\')')
                                    )
                            )
                    );
}

function uploadError(e) { // upload error
    alert(e.target.responseText);
}

function checkRequiredDocuments() {

    if (checkRequiredDocumentsRegEx == ".*") {
        return true;
    }

    var oREGEXP = new RegExp(checkRequiredDocumentsRegEx);


    // Scanned documents
    var photoTypes = document.getElementsByRegex('^photoType.*', "select");
    var photoGuids = document.getElementsByRegex('^photoGuid.*', "input");
    var errorLabel = document.getElementById('lblErrorMessages');
    var emptyDocsFound = 0;
    var checkForVisa = false;
    var visaAttachmentFound = false;

    try {
        if (visaExpiryDate.trim().length > 0) {
            checkForVisa = true;
        }
    } catch (e) {
        checkForVisa = false;
    }

    errorLabel.innerHTML = "";
    errorLabel.setAttribute("style", "color:red");

    // - Build a string containing a list of uploaded documents.
    var scannedDocumentTypes = "";
    var methodOfIdentificationFound = false;

    // First check if selected photo types are unique.
    // Extract photoTypes selected values
    var photoTypesValuesArray = new Array();
    for (var j = 0; j < photoTypes.length; j++) {
        photoTypesValuesArray[j] = photoTypes[j].value;
    }
    if (!checkIfArrayIsUnique(photoTypesValuesArray)) {
        errorLabel.innerHTML = "<b>Error:</b><br/><ul><li>Duplicate document types detected, please ensure document types are <b>unique</b> and then try again!</li></ul>";
        return false;
    } else {


        for (var j = 0; j < photoTypes.length; j++) {
            scannedDocumentTypes += photoTypes[j].value + " ";
            // Check for method of identification document.
            if (methodOfIdentification == photoTypes[j].value) {
                methodOfIdentificationFound = true;
            }
            // Check that the actual documents have been uploaded as well ...
            if (photoGuids[j].value == undefined || photoGuids[j].value == "") {
                emptyDocsFound++;
            }

            if (checkForVisa) {
                //Check if VisaDocument is supplied
                if (photoTypes[j].value == 'visa') {
                    visaAttachmentFound = true;
                }
            }
        }

        if (methodOfIdentification.trim().length <= 0) // No need to check for method of identification, in case of a minor
            methodOfIdentificationFound = true;

        var allDocumentsFound = true;

        allDocumentsFound = oREGEXP.test(scannedDocumentTypes);

        if (!allDocumentsFound || (emptyDocsFound > 0)) {
            errorLabel.innerHTML = "<b>Error:</b><br/><ul><li>Please ensure that all the required documents are uploaded as stated by the regulatory requirements and try again.</li></ul>";
            return false;
        }

        // Check if the method of identification document is uploaded as well?
        if (methodOfIdentificationFound != true) {
            errorLabel.innerHTML = "<b>Error:</b><br/><ul><li>In the previous screen, you have selected <b>" + methodOfIdentificationDescr + "</b> as a method of identification, please capture this document and then try again.</li></ul>";
            return false;
        }

        if (checkForVisa && !visaAttachmentFound && visaExpiryDate !== 'For-Life') {
            errorLabel.innerHTML = "<b>Error:</b><br/><ul><li>A Visa Expiry Date of <b>" + visaExpiryDate + "</b> has been specified for this customer, but no Visa document is attached, please capture this document and then try again.</li></ul>";
            return false;
        }
    }

    return allDocumentsFound;
}

function checkConsentValid() {

    var errorLabel = document.getElementById('lblErrorMessages');
    var rsm = document.getElementById('rsm');
    var emptyDocsFound = 0;

    errorLabel.innerHTML = "";
    errorLabel.setAttribute("style", "color:red");

    if (document.getElementById('file0')) {
        var photoGuid = document.getElementById('file0');
        if (photoGuid.value == undefined || photoGuid.value == "") {
            emptyDocsFound++;
        }
        if (emptyDocsFound > 0) {
            errorLabel.innerHTML = "<b>Please attach the customer's signed consent form.</b>";
            return false;
        }
    }

    if (rsm.value == '-1') {
        errorLabel.innerHTML = "<b>Please select consent approver.</b>";
        return false;
    }


    return true;
}

function clearErrorMessages() {
    document.getElementById('lblErrorMessages').innerHTML = "";
}



function showCustomerDiv(src) {
    var txtOrgName = document.getElementById('customer.organisationName');

    if (src.value == 'organisation') {
        document.getElementById('divOrganisation').style.display = '';
        document.getElementById('divNormalCustomer').style.display = 'none';
        txtOrgName.setAttribute('onkeyup', 'javascript:validate(this,\"^.{3,50}$\",\"emptynotok\");');

    } else
    if (src.value == 'customer' || src.value == 'staff') {
        document.getElementById('divOrganisation').style.display = 'none';
        document.getElementById('divNormalCustomer').style.display = '';
        txtOrgName.value = '';
        txtOrgName.setAttribute('onkeyup', '');
    } else {
        document.getElementById('divOrganisation').style.display = 'none';
        document.getElementById('divNormalCustomer').style.display = 'none';
    }
}

function getJSessionId() {
    var jSessionId = "";
    var vFrom = document.URL.indexOf("jsessionid");

    if (vFrom != -1) {
        jSessionId = document.URL.substring(vFrom);
        var vTo = jSessionId.indexOf("?");
        if (vTo != -1) {
            jSessionId = jSessionId.substring(0, vTo);
        }
    }
    return jSessionId;
}


function checkFileExtension(fileName) {

    if (fileName.indexOf('.') == -1)
        return false;

    var validExtensions = new Array();
    var ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

    validExtensions[0] = 'jpg';
    validExtensions[1] = 'jpeg';
    validExtensions[2] = 'bmp';
    validExtensions[3] = 'data';
    validExtensions[4] = 'pdf'; // Added to cater for contract document attachments.
    validExtensions[5] = 'asc'; // To support PGP public keys for distributing Voucher PINs.
    validExtensions[6] = 'xls';
    validExtensions[7] = 'xlsx';
    validExtensions[8] = 'ppt';
    validExtensions[8] = 'pptx';
    validExtensions[9] = 'txt';

    for (var i = 0; i < validExtensions.length; i++) {
        if (ext == validExtensions[i])
            return true;
    }
    return false;
}
/*START of background-worker functions */
function sendRequestToServer(url, cfunc) {
    var xmlhttp = getXMLHttprequest();
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4 && cfunc != null)
        {
            cfunc(xmlhttp);
        }

    }

    xmlhttp.open('GET', url, true, "", "");
    xmlhttp.send("");
    return xmlhttp;
}

function getXMLHttprequest() {

    try {
        return new XMLHttpRequest();
    } catch (ex) {
        try {
            return new ActiveXObject('Msxml2.XMLHTTP');
        } catch (ex1) {
            try {
                return new ActiveXObject('Microsoft.XMLHTTP');
            } catch (ex1) {
                return new ActiveXObject('Msxml2.XMLHTTP.4.0');
            }
        }
    }
}

function invokeActionBeanMethod(composeUrl, cfunc) {
    return sendRequestToServer(composeUrl, cfunc);
}

//This method to be called when a background task needs to be implemented

function loadDivAsynchronously(url, divId) { //,divId TODO: to be put in later
    var fullURL = window.location.protocol + "//" + document.location.host + url;
    $j("#page-load").show();
    $j.ajax({
        url: fullURL,
        dataType: "html",
        success: function (data) {
            $j("#page-load").fadeOut();
            $j("#" + divId).html(data);
        }
    });
}




function uploadDocument(evt) {
    var file = evt.target;
    var fileId = file.id.substring(4);
    var fileName = file.files[0].name;

    var photoGuid = document.getElementById("photoGuid" + fileId);
    var photoType = document.getElementById("photoType" + fileId);
    var imageCell = document.getElementById("imgDiv" + fileId);

    photoGuid.value = "";

    var ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

    // Filter for image files
    if (!checkFileExtension(fileName)) {
        imageCell.innerHTML = "<div id='file_error'><blink><b>Only .jpeg, .jpg, .bmp or .wsq image files and .xlsx, .xls files are allowed, please try again!</b></blink></div>";
        return;
    }

    // If the document type is a finger print, makesure a correct .wsq file is selected.
    if (photoType != null && photoType.value == "fingerprints") {
        if (fileName.match(/^FINGERPRINTS.*.DATA/i) == null) {
            imageCell.innerHTML = "<div id='file_error'><blink><b>Only filename FINGERPRINTS*.DATA is accepted, please try again!</b></blink></div>";
            return;
        }
    }

    imageCell.innerHTML = "<div id='progress_info' class='thumb'><blink><b>... busy uploading image file, please wait ...</b></blink></div>";
    imageCell.style.display = "block";

    setTimeout(
            function () {
                try {
                    var vFormData = new FormData();

                    vFormData.append(file.id, file.files[0]);

                    var oXHR = new XMLHttpRequest();
                    oXHR.addEventListener('load', uploadFinish, false);
                    oXHR.addEventListener('error', uploadError, false);
                    oXHR.open('POST', '/sep/FileUploadServlet;' + getJSessionId() + '?nationality=' + nationality, false);
                    oXHR.send(vFormData);

                    if (oXHR.status == 200) {
                        photoGuid.value = oXHR.responseText.split("\r\n")[0];

                        var innerHTML = "";

                        //if (photoType.value == "publickey") {
                        if (ext == "asc") { // Public Key
                            innerHTML += "<a href='../sep/images/public_key.png' target='_blank' >";
                            innerHTML += "<img id='imgfile" + fileId + "' class='thumb' src='../sep/images/public_key.png' />";
                        } else
                        if (ext == "pdf") {
                            innerHTML += "<a href='../sep/images/dummy-fingerprint.jpg' target='_blank' >";
                            innerHTML += "<img id='imgfile" + fileId + "' class='thumb' src='../sep/images/pdf-icon.jpg' />";
                        } else
                        if (ext == "data") {
                            innerHTML += "<a href='../sep/images/dummy-fingerprint.jpg' target='_blank' >";
                            innerHTML += "<img id='imgfile" + fileId + "' class='thumb' src='../sep/images/dummy-fingerprint.jpg' />";
                        } else if (ext == "xlsx" || ext == "xls") {
                            innerHTML += "<a href='../sep/images/excel_file_image.png' target='_blank' >";
                            innerHTML += "<img id='imgfile" + fileId + "' class='thumb' src='../sep/images/excel_file_image.png' />";
                        } else {
                            innerHTML += "<a href='/sep/GetImageDataServlet;" + getJSessionId() + '?photoGuid=' + photoGuid.value + "' target='_blank' >"
                            innerHTML += "<img id='imgfile" + fileId + "' class='thumb' src='/sep/GetImageDataServlet;" + getJSessionId() + '?photoGuid=' + photoGuid.value + "' />";
                        }
                        innerHTML += "</a>";

                        imageCell.innerHTML = innerHTML;
                        imageCell.style.display = "block";


                    } else {
                        alert("Error while trying to load picture (Error Message: " + oXHR.responseText + ")(Error Code: " + oXHR.status + "). Please try again, if this error persists, contact IT support.");
                    }

                    return ((oXHR.status == 200) ? true : false);

                } catch (e) {
                    console.log("uploadDocument: Error - " + e);
                }

            }, 500);
}


/**************************************************************
 * Customer and Organisation wizard address utility functions  
 **************************************************************/
function checkRequiredAddresses() {
    if (!isAddressMandatory)
        return true;

    // - Scanned documents
    var addressTypes = document.getElementsByRegex('^addressType.*', "select");
    var errorLabel = document.getElementById('lblErrorMessages');
    var docTypeFound = false;
    var numAddrTypesFound = 0;
    var innerHTML = "";

    errorLabel.innerHTML = "";
    errorLabel.setAttribute("style", "color:red");

    for (var i = 0; i < requiredAddresses.length; i++) {
        docTypeFound = false;
        // For each required address type, loop across the addressess to see if it was captured
        for (var j = 0; j < addressTypes.length; j++) {
            if (requiredAddresses[i] == addressTypes[j].value) {
                numAddrTypesFound++;
                docTypeFound = true;
            }
        }

        if (docTypeFound == false) {
            // Write error message to the lblErrorMessages element ...
            innerHTML += "<li>A required address of type <b>'" + requiredAddresses[i] + "'</b> was not captured, please capture and try again!</li>";
        }
    }

    if (innerHTML.length > 0) {
        errorLabel.innerHTML = "Error/s:<br/ ><ul>" + innerHTML + "</ul>";
        return false;
    }


    return (numAddrTypesFound == requiredAddresses.length);

}

function addAddress(entityName) {

    var tbody = document.getElementById('tblAddresses').getElementsByTagName("TBODY")[0];
    var rowCount = document.getElementById('tblAddresses').rows.length;

    var row = document.createElement("TR");
    var rowId = "row" + rowCount;

    var td1 = document.createElement("TD");
    td1.setAttribute('valign', 'top');
    td1.setAttribute('align', 'left');

    var td2 = document.createElement("TD");
    td2.setAttribute('valign', 'top');
    td2.setAttribute('align', 'left');

    var td3 = document.createElement("TD");
    td3.setAttribute('valign', 'top');
    td3.setAttribute('align', 'left');

    var selectDocTypes = document.createElement("SELECT");
    selectDocTypes.setAttribute("id", "addressType" + rowCount);
    selectDocTypes.setAttribute("name", entityName + ".addresses[" + rowCount + "].type");
    selectDocTypes.setAttribute("onchange", addressTypeValidationRule);

    selectDocTypes.innerHTML = document.getElementById('address.types').innerHTML;

    td1.appendChild(selectDocTypes);

    var vInnerHTML = "<table class='clear' style='vertical-align: top; margin-top:auto' >";
    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + strAddressLine1 + ":</td>";
    vInnerHTML += "<td> <input type='text' id='addressLine1" + rowCount + "' name='" + entityName + ".addresses[" + rowCount + "].line1' maxlength='100' size='40' onkeyup=\"" + addressLine1ValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + strAddressLine2 + ":</td>";
    vInnerHTML += "<td><input type='text' id='addressLine2" + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].line2' maxlength='100' size='40' onkeyup=\"" + addressLine2ValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    if (entityName == 'organisation') {
        statesSelectName = "addressStateSel";
        zonesSelectName = "addressZoneSel";
    } else {
        statesSelectName = "addressState";
        zonesSelectName = "addressZone";
    }

    vInnerHTML += "<tr id='addressRowState" + rowCount + "'>";
    vInnerHTML += "<td>" + strState + ":</td>";
    vInnerHTML += "<td><select id='" + statesSelectName + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].state' onchange=\"populateZones(" + rowCount + "); return " + addressStateValidationRule + ";\">" +
            document.getElementById('address.states').innerHTML + "</select>" +
            "<input type='text' id='addressStateTxt" + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].state.notused' style=\"display:none;\" maxlength='50' size='20'/>" +
            "</td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<tr id='addressRowZone" + rowCount + "'>";
    vInnerHTML += "<td>" + strZone + ":</td>";
    vInnerHTML += "<td><select id='" + zonesSelectName + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].zone' onchange=\"" + addressZoneValidationRule + "\"></select>" +
            "<input type='text' id='addressZoneTxt" + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].zone.notused' style=\"display:none;\" maxlength='50' size='20'/>" +
            "</td>";
    vInnerHTML += "</tr>";
    /* document.getElementById('address.zones').innerHTML */
    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + strTown + ":</td>";
    if (isTownMandatory) {
        vInnerHTML += "<td><input type='text' id='addressTown" + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].town' maxlength='50' size='20' onkeyup=\"" + addressTownValidationRule + "\"/></td>";
    } else {
        vInnerHTML += "<td><input type='text' id='addressTown" + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].town' maxlength='50' size='20' onkeyup=\"validate(this,'^.{0,50}$','emptyok')\"/></td>";
    }
    vInnerHTML += "</tr>";

    if (entityName == 'organisation') {
        vInnerHTML += "<tr>";
        vInnerHTML += "<td>" + strCountry + ":</td>";
        vInnerHTML += "<td><select id='addressCountrySel" + rowCount + "'  name='" + entityName + ".addresses[" + rowCount + "].country' onchange=\"toggleStatesAndZones(" + rowCount + "); return " + addressCountryValidationRule + ";\">" +
                document.getElementById('address.countries').innerHTML + "</select>"
        vInnerHTML += "</td></tr>";
    } else {
        vInnerHTML += "<tr>";
        vInnerHTML += "<td>" + strCountry + ":</td>";
        vInnerHTML += "<td><input type='text' size='20' value=\"" + countryName + "\" disabled='disabled' />     ";
        vInnerHTML += "    <input type='hidden' id='addressCountry" + rowCount + "' name='" + entityName + ".addresses[" + rowCount + "].country' value=\"" + countryName + "\"/></td>";
        vInnerHTML += "</tr>";
    }

    if (entityName == 'organisation') { // Postal code is optional for organisations.
        vInnerHTML += "<tr>";
        vInnerHTML += "<td>" + strCode + ":</td>";
        vInnerHTML += "<td><input type='text' id='addressCode" + rowCount + "' name='" + entityName + ".addresses[" + rowCount + "].code' maxlength='20' size='20'";
        vInnerHTML += "</tr>";
    }

    if (displayPostalCode == 'true' && entityName != 'organisation') {
        vInnerHTML += "<tr>";
        vInnerHTML += "<td>" + strCode + ":</td>";
        vInnerHTML += "<td><input type='text' id='addressCode" + rowCount + "' name='" + entityName + ".addresses[" + rowCount + "].code' maxlength='20' size='20' onkeyup=\"" + addressPostalCodeValidationRule + "\"/></td>";
        vInnerHTML += "</tr>";
    } else {
        vInnerHTML += "<input type='hidden' id='addressCode" + rowCount + "' name='" + entityName + ".addresses[" + rowCount + "].code' value=''/>"
    }


    vInnerHTML += "</table>";

    td2.innerHTML += vInnerHTML;

    td3.innerHTML += "<input type='button' value='Remove' class='file' onclick=\"removeAddress(this, '" + entityName + "');\"/>";

    row.appendChild(td1);
    row.appendChild(td2);
    row.appendChild(td3);

    tbody.appendChild(row);

    if (entityName == 'organisation') {
        document.getElementById("addressStateSel" + rowCount).onchange();
    } else {
        document.getElementById("addressState" + rowCount).onchange();
    }
}

function removeAddress(src, entityName) {
    var oRow = src.parentNode.parentNode;
    document.getElementById("tblAddresses").deleteRow(oRow.rowIndex);
    //Rename all the form elements on each row of the table so that they are consistent with Stripes mapping ...
    renameAddressRowControls(entityName);
}

function renameAddressRowControls(entityName) {

    var addressType = document.getElementsByRegex('^addressType.*', "select");
    var addressLine1 = document.getElementsByRegex('^addressLine1.*', "input");
    var addressLine2 = document.getElementsByRegex('^addressLine2.*', "input");
    var addressTown = document.getElementsByRegex('^addressTown.*', "input");
    var addressCountry = document.getElementsByRegex('^addressCountry.*', "input");
    var addressCode = document.getElementsByRegex('^addressCode.*', "input");
    var addressZone = document.getElementsByRegex('^addressZone.*', "zone");

    for (var i = 0; i < document.getElementById('tblAddresses').rows.length; i++) {
        addressType[i].id = "addressType" + i;
        addressType[i].name = entityName + ".addresses[" + i + "].type";
        addressLine1[i].id = "addressLine1" + i;
        addressLine1[i].name = entityName + ".addresses[" + i + "].line1";
        addressLine2[i].id = "addressLine2" + i;
        addressLine2[i].name = entityName + ".addresses[" + i + "].line2";
        addressTown[i].id = "addressTown" + i;
        addressTown[i].name = entityName + ".addresses[" + i + "].town";
        addressCountry[i].id = "addressCountry" + i;
        addressCountry[i].name = entityName + ".addresses[" + i + "].country";
        addressCode[i].id = "addressCode" + i;
        addressCode[i].name = entityName + ".addresses[" + i + "].code";
        addressZone[i].id = "addressZone" + i;
        addressZone[i].name = entityName + ".addressZone[" + i + "].zone";
    }
}

/*END of background-worker functions*/


/*WEBSOCKET keepalive functionality*/

function keepAlive(websocket) {
    websocket.send("keepalive");
}

/**
 * function to send some arb data on a weboscket to make sure it does not timeout
 * @param {String} websocket
 * @param {int} frequency - frequency of keepalive in ms
 * @returns {undefined}
 */

function keepWSAlive(websocket, frequency)
{
    window.setInterval(function () {
        keepAlive(websocket);
    }, frequency);
}



/* 
 /**
 * add a user-level ticker to the ticker tape - this will last for the duration of the browser session
 * message: the message to add
 * link: an optional href link to include - use # for dead link 
 */
function addUserLevelTicker(message, link) {
    delete Array.prototype.toJSON;  //we need this because of library conflicts....

    userEntry = new Object();

    //first check we dont already have this message in our session
    currentUserEntries = sessionStorage.getItem("userTickers");
    if (currentUserEntries == null || currentUserEntries == '') {
        //if there are no messages we can go ahead and add this new one
        console.log("current user ticket entries is empty....adding a new one");
        var userEntryArray = new Array();
        userEntry = new Object();
        userEntry.text = message;
        userEntry.link = link;
        userEntry.visible = 1;       //0=hidden, 1=show
        userEntry.type = "user";
        userEntryArray.push(userEntry);
        var jsonText = JSON.stringify(userEntryArray);
        sessionStorage.setItem("userTickers", jsonText);
    } else {
        //let's make sure we dont already have this message - our messages are effectively key'ed by the message itself
        var userEntryArray = JSON.parse(sessionStorage.getItem("userTickers"));

        for (var i = 0; i < userEntryArray.length; i++) {
            var userEntry = userEntryArray[i]
            if (userEntry.text == message) {
                console.log("we already have this message.....ignoring it");
                return;
            }
        }
        //if we get here we can add the new message
        userEntry = new Object();
        userEntry.text = message;
        userEntry.link = link;
        userEntry.visible = 1;       //0=hidden, 1=show
        userEntry.type = "user";
        userEntryArray.push(userEntry);
        var jsonText = JSON.stringify(userEntryArray);
        console.log('updated json text going into session object is ' + jsonText);
        sessionStorage.setItem("userTickers", jsonText);
        console.log('plain json is ' + sessionStorage.getItem("userTickers"));
    }
}

function checkIfArrayIsUnique(myArray) {
    for (var i = 0; i < myArray.length; i++) {
        if (myArray.indexOf(myArray[i]) !== myArray.lastIndexOf(myArray[i])) {
            return false; // means not unique
        }
    }
    return true; // - Array is unique
}

function removeUserLevelTicker(message) {
    var userEntryArray = JSON.parse(sessionStorage.getItem("userTickers"));

    if (userEntryArray != null && userEntryArray.length > 0) {
        var i = 0;
        for (i = 0; i < userEntryArray.length; i++) {
            var userEntry = userEntryArray[i]
            if (userEntry.text == message) {
                console.log("found user ticker to remove... removing");
                //we don't really delete - just make is invisible..... it may be used again sometime....
                userEntry.visible = 0;
                break;
            }
        }
    }
}

function initWebCam() {
    // Grab elements, create settings, etc.
    var video = document.getElementById("video");
    var videoObj = {"video": true};
    var errBack = function (error) {
        console.log("Video capture error: " + error, error.code);
    };

    // Put video listeners into place
    if (navigator.getUserMedia) { // Standard
        navigator.getUserMedia(videoObj, function (stream) {
            video.src = window.URL.createObjectURL(stream);
            video.play();
        }, errBack);
    } else if (navigator.webkitGetUserMedia) { // WebKit-prefixed
        navigator.webkitGetUserMedia(videoObj, function (stream) {
            video.src = window.URL.createObjectURL(stream);
            video.play();
        }, errBack);
    } else if (navigator.mozGetUserMedia) { // Firefox-prefixed
        navigator.mozGetUserMedia(videoObj, function (stream) {
            video.src = window.URL.createObjectURL(stream);
            video.play();
        }, errBack);
    }
}

function snapAndUpload(file) {
    var fileId = file.substring(4);
    var photoType = document.getElementById("photoType" + fileId);
    if (photoType != null && photoType.value == "fingerprints") {
        var imageCell = document.getElementById("imgDiv" + fileId);
        imageCell.innerHTML = "<div id='file_error'><blink><b>Cant upload fingerprints via WebCam!</b></blink></div>";
        return;
    }
    var canvas = document.getElementById("canvas");
    var context = canvas.getContext("2d");
    var video = document.getElementById("video");
    context.drawImage(video, 0, 0, 1024, 768);
    var image = new Image();
    image.onload = function () {
        sendSnap(image, fileId);
    };
    image.src = canvas.toDataURL("image/jpeg");
}

function sendSnap(image, fileId) {
    var imageCell = document.getElementById("imgDiv" + fileId);
    imageCell.innerHTML = "<div id='progress_info' class='thumb'><blink><b>... busy uploading image file, please wait ...</b></blink></div>";
    imageCell.style.display = "block";
    var photoGuid = document.getElementById("photoGuid" + fileId);
    setTimeout(
            function () {
                try {
                    var vFormData = new FormData();

                    vFormData.append("snap.jpg", image.src);

                    var oXHR = new XMLHttpRequest();
                    oXHR.addEventListener('load', uploadFinish, false);
                    oXHR.addEventListener('error', uploadError, false);
                    console.log("Sending file to SEP");
                    oXHR.open('POST', '/sep/FileUploadServlet;' + getJSessionId(), false);
                    oXHR.send(vFormData);
                    if (oXHR.status == 200) {
                        console.log("Sent file and got 200 OK");
                        photoGuid.value = oXHR.responseText.split("\r\n")[0];

                        var innerHTML = "<a href='/sep/GetImageDataServlet;" + getJSessionId() + '?photoGuid=' + photoGuid.value + "' target='_blank' >"
                        innerHTML += "<img id='imgfile" + fileId + "' class='thumb' src='/sep/GetImageDataServlet;" + getJSessionId() + '?photoGuid=' + photoGuid.value + "' />";
                        innerHTML += "</a>";

                        imageCell.innerHTML = innerHTML;
                        imageCell.style.display = "block";
                        console.log("Set innerHtml to " + innerHTML);

                    } else {
                        alert("Error while trying to load picture (Error Message: " + oXHR.responseText + ")(Error Code: " + oXHR.status + "). Please try again, if this error persists, contact IT support.");
                    }

                    return ((oXHR.status == 200) ? true : false);

                } catch (e) {
                    console.log("Error in file upload: " + e);
                }

            }, 500);
}


function addLegalContacts(entityName) {

    var tbody = document.getElementById('tblLegalContacts').getElementsByTagName("TBODY")[0];
    var rowCount = document.getElementById('tblLegalContacts').rows.length;

    var row = document.createElement("TR");
    var rowId = "row" + rowCount;

    var td1 = document.createElement("TD");
    td1.setAttribute('valign', 'top');
    td1.setAttribute('align', 'left');

    var td2 = document.createElement("TD");
    td2.setAttribute('valign', 'top');
    td2.setAttribute('align', 'left');

    var td3 = document.createElement("TD");
    td3.setAttribute('valign', 'top');
    td3.setAttribute('align', 'left');

    var selectDocTypes = document.createElement("SELECT");
    selectDocTypes.setAttribute("id", "userType" + rowCount);
    selectDocTypes.setAttribute("name", entityName + "[" + rowCount + "].legalContactType");
    selectDocTypes.setAttribute("onchange", legalContactTypeValidationRule);

    selectDocTypes.innerHTML = document.getElementById('userType').innerHTML;

    td1.appendChild(selectDocTypes);

    var vInnerHTML = "<table class='clear' style='vertical-align: top; margin-top:auto' >";

//    vInnerHTML += "<tr>";
//    vInnerHTML += "<td>" + legalContactId + ":</td>";
//    vInnerHTML += "<td><input type='text' id='legalContactId" + rowCount + "'  name='" + entityName + "[" + rowCount + "].legalContactId' maxlength='100' size='40' onkeyup=\"" + legalContactIdValidationRule + "\"/></td>";
//    vInnerHTML += "</tr>";

    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + organisationId + ":</td>";
    vInnerHTML += "<td><input type='text' id='organisationId" + rowCount + "'  name='" + entityName + "[" + rowCount + "].organisationId' maxlength='50' size='30' onkeyup=\"" + organisationIdValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + nin + ":</td>";
    vInnerHTML += "<td><input type='text' id='nin" + rowCount + "'  name='" + entityName + "[" + rowCount + "].nin' maxlength='50' size='30' onkeyup=\"" + ninValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + firstName + ":</td>";
    vInnerHTML += "<td><input type='text' id='firstName" + rowCount + "'  name='" + entityName + "[" + rowCount + "].firstName' maxlength='50' size='30' onkeyup=\"" + firstNameValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + lastName + ":</td>";
    vInnerHTML += "<td><input type='text' id='lastName" + rowCount + "'  name='" + entityName + "[" + rowCount + "].lastName' maxlength='50' size='30' onkeyup=\"" + lastNameValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + email + ":</td>";
    vInnerHTML += "<td><input type='text' id='email" + rowCount + "'  name='" + entityName + "[" + rowCount + "].email' maxlength='50' size='30' onkeyup=\"" + emailValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<tr>";
    vInnerHTML += "<td>" + telNumber + ":</td>";
    vInnerHTML += "<td><input type='text' id='telNumber" + rowCount + "'  name='" + entityName + "[" + rowCount + "].telNumber' maxlength='50' size='30' onkeyup=\"" + telNumberValidationRule + "\"/></td>";
    vInnerHTML += "</tr>";

    vInnerHTML += "<table class='clear' id='tblDocuments" + rowCount + "' align='left'>";
    vInnerHTML += "<tbody>";
    vInnerHTML += "</tbody>";
    vInnerHTML += "</table>";
    vInnerHTML += "<input onclick=\"addDocumetnsRow('tblDocuments" + rowCount + "', '" + entityName + "[" + rowCount + "].customerPhotographs','" + rowCount + "');\" type='button' value='Add Document'/>";

    vInnerHTML += "</table>";
    vInnerHTML += "<br>";


    td2.innerHTML += vInnerHTML;

//    var table = document.createElement("TABLE");
//    table.innerHTML = document.getElementById('tblDocuments').innerHTML;
//    td2.appendChild(table);



    td3.innerHTML += "<input type='button' value='Remove' class='file' onclick=\"removeLegalContacts(this, '" + entityName + "');\"/>";

    row.appendChild(td1);
    row.appendChild(td2);
    row.appendChild(td3);

    tbody.appendChild(row);
}

function removeLegalContacts(src, entityName) {
    var oRow = src.parentNode.parentNode;
    document.getElementById("tblLegalContacts").deleteRow(oRow.rowIndex);
    //Rename all the form elements on each row of the table so that they are consistent with Stripes mapping ...
//    renameAddressRowControls(entityName);
}

function addDocumetnsRow(id, arrayName, tableCount) {

    var tbody = document.getElementById(id).getElementsByTagName("TBODY")[0];
    var rowCount = document.getElementById(id).rows.length;

    var row = document.createElement("TR");
    var rowId = "row" + rowCount;

    row.setAttribute("id", rowId);
    row.setAttribute("name", rowId);

    var td1 = document.createElement("TD")

    var selectDocTypes = document.createElement("SELECT");

    selectDocTypes.setAttribute("id", "photoType" + rowCount + "table" + tableCount);

    selectDocTypes.setAttribute("name", arrayName + "[" + rowCount + "].photoType");
    selectDocTypes.setAttribute("onchange", "validate(this,'^[a-zA-Z]{1,50}$','emptynotok')");

    var myOptions = document.getElementById('customer.document.types').options;

    var newOption = null;

    for (var i = 0; i < myOptions.length; i++) {
        newOption = document.createElement('option');
        newOption.value = myOptions[i].value;
        newOption.text = myOptions[i].text;
        selectDocTypes.appendChild(newOption);
    }

    td1.appendChild(selectDocTypes);

    td1.setAttribute('valign', 'top');
    td1.setAttribute('align', 'left');

    var td2 = document.createElement("TD")
    td2.innerHTML = "<div id='imgDiv" + rowCount + "table" + tableCount + "'><img class='thumb' id='imgfile" + rowCount + "table" + tableCount + "' src='images/upload.png' /></div>";
    td2.setAttribute('valign', 'top');
    td2.setAttribute('align', 'left');

    var td3 = document.createElement("TD")
    td3.innerHTML = "<input type='button' class='file' value = 'Browse ...' onclick =\"javascript:document.getElementById('file" + rowCount + "table" + tableCount + "').click();\" />";
    if (typeof useWebCam == "undefined") { // Use WebCam by default 
        td3.innerHTML += "<input type='button' value='WebCam' class='file' id='snap' onclick=\"snapAndUpload('file" + rowCount + "table" + tableCount + "')\"/>";
    } else
    if (useWebCam) {
        td3.innerHTML += "<input type='button' value='WebCam' class='file' id='snap' onclick=\"snapAndUpload('file" + rowCount + "table" + tableCount + "')\"/>";
    }
    td3.innerHTML += "<input type='button' value='Remove' class='file' onclick=\"removeDocumentRow(this,'" + arrayName + "','" + tableCount + "');\"/>";
    td3.innerHTML += "<input type='hidden' id='photoGuid" + rowCount + "table" + tableCount + "' name='" + arrayName + "[" + rowCount + "].photoGuid' value='' />";
    td3.innerHTML += "<input type='file'  id='file" + rowCount + "table" + tableCount + "' name='file" + rowCount + "table" + tableCount + "' style='visibility: hidden;'/>";

    td3.setAttribute('valign', 'top');
    td3.setAttribute('align', 'left');

    row.appendChild(td1);
    row.appendChild(td2);
    row.appendChild(td3);

    tbody.appendChild(row);

    var file = document.getElementById('file' + rowCount + "table" + tableCount);

    if (file.addEventListener) {
        file.addEventListener('change', uploadDocument, false);
    } else if (file.attachEvent) {
        file.attachEvent('onchange', uploadDocument);
    }

}


function removeDocumentRow(src, arrayName, tableRwoNumber) {
    var oRow = src.parentNode.parentNode;
    var fileId = oRow.rowIndex;

    var photoGuid = document.getElementById('photoGuid' + fileId + "table" + tableRwoNumber).value;

    // Also delete the image from the staging folder on the server
    try {
        var vFormData = new FormData();
        var oXHR = new XMLHttpRequest();
        oXHR.open('POST', '/sep/FileUploadServlet;' + getJSessionId() + '?deleteFile=YES&currentGuid=' + photoGuid, false);
        oXHR.send(vFormData);
    } catch (e) {
    }

    document.getElementById("tblDocuments" + tableRwoNumber).deleteRow(oRow.rowIndex);
    // Rename all the form elements on each row of the table so that they are consistent with Stripes mapping ...
    //renameDocumentsRowControls(arrayName,tableRwoNumber);

}

function renameDocumentsRowControls(arrayName, tableRwoNumber) {
    // Get all the file controls and rename them accordingly
    var files = document.getElementsByRegex('^file.*', "input");
    // Get all the hidden input controls which are storing photoType
    var photoTypes = document.getElementsByRegex('^photoType.*', "select");
    // Get all the hidden controls that are storing photoGuids
    var photoGuids = document.getElementsByRegex('^photoGuid.*', "input");


    var rows = document.getElementsByRegex('^row.*', "tr");

    for (var i = 0; i < document.getElementById('tblDocuments' + tableRwoNumber).rows.length; i++) {
        table = document.getElementById('tblDocuments' + tableRwoNumber);
        table.files[i].id = "file" + i + "table" + tableRwoNumber;
        table.photoTypes[i].id = "photoType" + i + "table" + tableRwoNumber;
        table.photoTypes[i].name = arrayName + "[" + i + "].photoType";
        table.photoGuids[i].id = "photoGuid" + i + "table" + tableRwoNumber;
        table.photoGuids[i].name = arrayName + "[" + i + "].photoGuid";

        rows[i].id = "row" + i;
        rows[i].name = "row" + i;
    }
}