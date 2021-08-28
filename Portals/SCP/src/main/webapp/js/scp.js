/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


function submitForm(){
    document.getElementById("loginform").submit();
}
function previousPreviousPage(){
    window.history.go(-2);
}
function previousPage(){
    window.history.back();
}

function unhide(divId){
    var item = document.getElementById(divId);
    if(item){
        item.className = (item.className == 'hidden')?'visible': 'hidden';
        if (item.className == 'hidden'){
            document.getElementById('btnMore').value = 'Details';
            document.getElementById('entity').style.height = "220px";
            
        }else if(item.className == 'visible'){
            document.getElementById('btnMore').value = 'Summary';
            document.getElementById('entity').style.height = "auto";
        }
    }
}

function validate(textbox, regex, otherrules) {
    var textBoxValue = textbox.value;
    if (otherrules.indexOf("emptyok") != -1 && textBoxValue.length == 0) {
        textbox.className="validationOk";
        return;
    }
    var oREGEXP = new RegExp(regex);
    
    if (oREGEXP.test(textBoxValue)) {
        textbox.className="validationOk";
    } else {
        textbox.className="validationNotOk";
        return;
    }
    if (otherrules.indexOf("luhn") != -1) {
        if (validateLuhn(textBoxValue)) {
            textbox.className="validationOk";
        } else {
            textbox.className="validationNotOk";
            return;
        }
    }
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

function getJSessionId() {
    var jSessionId = "";
    var vFrom = document.URL.indexOf("jsessionid");
    
    if(vFrom != -1) {
        jSessionId = document.URL.substring(vFrom);
        var vTo = jSessionId.indexOf("?");
        if(vTo != -1) { 
            jSessionId = jSessionId.substring(0,vTo);
        }
    }
    return jSessionId;
}

function getXMLHttprequest(){
    try { 
        return new XMLHttpRequest(); 
    }
    catch (ex) { 
        try {  
            return new ActiveXObject('Msxml2.XMLHTTP'); 
        }
        catch (ex1) { 
            try { 
                return new ActiveXObject('Microsoft.XMLHTTP'); 
            }
            catch(ex1) {       
                return new ActiveXObject('Msxml2.XMLHTTP.4.0'); 
            }
        }
    }
}

function sendRequestToServer(url, callbackFunction){
    var xmlhttp = getXMLHttprequest();
    xmlhttp.onreadystatechange=function(){
        if (xmlhttp.readyState==4 && callbackFunction != null)
        {
            callbackFunction(xmlhttp);
        }
    }
    xmlhttp.open('GET',url,true,"","");
    xmlhttp.send("");
    return xmlhttp;
}