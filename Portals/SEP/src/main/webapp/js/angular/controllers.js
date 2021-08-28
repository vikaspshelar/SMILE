'use strict';

/* Controllers */

angular.module('SEPApp', ["angucomplete-alt"])
        .controller('BasicController', BasicController)
        .directive('sepDatepicker', CustomDatepicker);

function BasicController($scope, $http) {

    $scope.getCustomer = function (customerId) {
        var config = {
            params: {
                'getCustomerJSON': '',
                'customerQuery.customerId': customerId
            }
        };
        $http.get(makeURL('Customer.action'), config).success(function (data) {
            $scope.customer = data;
        });
    };

    function cloneObject(obj) {
        if (obj === null || typeof obj !== 'object') {
            return obj;
        }
        var temp = obj.constructor(); // give temp the original obj's constructor
        for (var key in obj) {
            temp[key] = cloneObject(obj[key]);
        }
        return temp;
    }

    $scope.addPastLineToSale = function (recipientAccountId, recipientCustomerId, recipientOrganisationId, itemNumber, serialNumber, description, quantity, currency) {
        if ($scope.saleLineItems == null) {
            $scope.saleLineItems = new Array();
        }
        var config = {
            params: {
                'getItemsForSaleJSON': '',
                'inventoryQuery.recipientAccountId': recipientAccountId,
                'inventoryQuery.recipientCustomerId': recipientCustomerId,
                'inventoryQuery.recipientOrganisationId': recipientOrganisationId,
                'inventoryQuery.stringMatch': itemNumber,
                'inventoryQuery.currency': currency
            }
        };
        $http.get(makeURL('Sales.action'), config).success(function (data) {
            for (var i = 0; i < data.inventoryItems.length; i++) {
                var item = data.inventoryItems[i];
                console.log("Got item: SerialNumber:" + item.serialNumber + " ItemNumber:" + item.itemNumber + " Currency:" + item.currency);
                if (item.serialNumber == serialNumber && item.itemNumber == itemNumber && item.currency == currency) {
                    item.quantity = quantity;
                    item.unitPriceInCentsIncl = item.priceInCentsIncl / quantity
                    item.unitPriceInCentsExcl = item.priceInCentsExcl / quantity
                    //console.log("AddPastLineToSale --> PriceInCentsInc: " + item.priceInCentsIncl);
                    if (item.itemNumber.startsWith("BENTMW")) {
                        $scope.isP2PInvoicing = true;
                        if ($scope.isP2PInvoiceOption) {
                            var dtAsString = new Date().toISOString().slice(0, 10);
                            item.unitPriceInCentsIncl = item.priceInCentsIncl / $scope.daysInMonth(dtAsString);
                            item.unitPriceInCentsExcl = item.priceInCentsExcl / $scope.daysInMonth(dtAsString);
                            //console.log("AddPastLineToSale --> UnitPriceInCentsIncl: " + item.unitPriceInCentsIncl);
                        }
                    }
                    $scope.saleLineItems[$scope.saleLineItems.length] = item;
                    break;
                }
            }
        });
    };

    $scope.getSaleTotalTax = function () {
        if ($scope.saleLineItems == null) {
            $scope.saleLineItems = new Array();
        }
        var totalTaxCents = 0;
        for (var i = 0; i < $scope.saleLineItems.length; i++) {
            if ($scope.saleLineItems[i].itemNumber.startsWith("BENTMW") && $scope.isP2PInvoiceOption) {
                totalTaxCents += $scope.saleLineItems[i].quantity * ($scope.saleLineItems[i].unitPriceInCentsIncl - $scope.saleLineItems[i].unitPriceInCentsExcl);
            } else {
                totalTaxCents += $scope.saleLineItems[i].quantity * ($scope.saleLineItems[i].priceInCentsIncl - $scope.saleLineItems[i].priceInCentsExcl);
            }
        }
        return totalTaxCents / 100;
    };

    $scope.getSaleTotalIncl = function () {
        if ($scope.saleLineItems == null) {
            $scope.saleLineItems = new Array();
        }
        var totalCentsIncl = 0;
        for (var i = 0; i < $scope.saleLineItems.length; i++) {
            if ($scope.saleLineItems[i].itemNumber.startsWith("BENTMW") && $scope.isP2PInvoiceOption) {
                totalCentsIncl += $scope.saleLineItems[i].quantity * $scope.saleLineItems[i].unitPriceInCentsIncl;
            } else {
                totalCentsIncl += $scope.saleLineItems[i].quantity * $scope.saleLineItems[i].priceInCentsIncl;
            }
        }
        return totalCentsIncl / 100;
    };

    $scope.canBeCash = function (cash) {
        $scope.cash = cash;
        $scope.accountSelected = true;
    };

    $scope.tenderedAmountChanged = function () {
        if ($scope.sale.amountTenderedCents == null || $scope.sale.amountTenderedCents == '' || $scope.sale.amountTenderedCents < 0) {
            $scope.sale.amountTenderedCents = 0.0;
            return;
        }
        if ($scope.sale.amountTenderedCents.indexOf('0') == 0) {
            $scope.sale.amountTenderedCents = $scope.sale.amountTenderedCents.substring(1);
        }
    };

    $scope.paymentMethodChanged = function () {
        if ($scope.paymentType != 'Cash') {
            $scope.sale.amountTenderedCents = 0.0;
        }
    };

    function makeURL(action) {
        var sessId = getJSessionId();
        if (sessId == null || sessId == '') {
            return action;
        }
        return action + ';' + getJSessionId();
    }

    /*
     *The 'ng-Model' directive has to be different for source and target account elements
     *to avoid updating both elements hence this similar functions
     **/
    $scope.getSourceServiceInstanceUserName = function () {

        //send request to server if the account is 10 or more characters
        if ($scope.sourceAccountId.toString().length < 10) {
            $scope.sourceCustomerWithProduct = null;
            return
        }
        var config = {
            params: {
                'getCustomerNameAsStream': '',
                'account.accountId': $scope.sourceAccountId
            }
        };
        $http.get(makeURL('Account.action'), config).success(function (data) {
            $scope.sourceCustomerWithProduct = data;
        });
    };

    $scope.getTargetServiceInstanceUserName = function () {
        if ($scope.targetAccountId.toString().length < 10) {
            $scope.targetCustomerWithProduct = null;
            return
        }
        var config = {
            params: {
                'getCustomerNameAsStream': '',
                'account.accountId': $scope.targetAccountId
            }
        };
        $http.get(makeURL('Account.action'), config).success(function (data) {
            $scope.targetCustomerWithProduct = data;
        });
    };

    $scope.updateTextBox = function () {
        if ($scope.dropdownAccIdSet == null) {
            $scope.dropdownAccIdSet = -1;
        }
        if ($scope.dropdownAccIdSet == -1) {
            $scope.txtboxAccId = null;
        } else {
            $scope.txtboxAccId = $scope.dropdownAccIdSet;
        }
    }

    $scope.cashInRequiredAmnt = 0;    
    $scope.cashInTotalChange = function (amnt, cb, paymentMethod) {
        if ($scope.cashInRequiredAmnt == null) {
            $scope.cashInRequiredAmnt = 0;
        }
        if (paymentMethod == 'Cash') {
            if (cb) {
                $scope.cashInRequiredAmnt += amnt;
            } else {
                $scope.cashInRequiredAmnt -= amnt;
            }
        }
    };


    $scope.getSalesLeadByWildCard = function (wildCard) {

        if (wildCard === '') {
            $scope.ttSingleIssue = null;
            return;
        }
        //Start searching if there is at least 3 characters, also cant search using both fields
        if (wildCard.length < 3) {
            return;
        }
        $scope.wildCardChar = 'Searching for sales leads, matching \"' + wildCard + "\"...";
        $scope.ttIssues = null;
        $scope.looking = true;

        var config = {
            params: {
                'getSalesLeadByWildCardAsJSON': '',
                'TTIssueQuery.customerName': wildCard,
                'TTIssueQuery.issueID': ''
            },
            timeout: 15000
        };
        $http.get(makeURL('TroubleTicket.action'), config).success(function (data) {
            $scope.wildCardChar = wildCard;

            var issuesFromRequest = new Array();
            issuesFromRequest = data;
            var simplifiedArray = new Array();

            if ($scope.ttIssues === null) {
                $scope.ttIssues = new Object();
            }

            for (var i = 0; i < issuesFromRequest.ttIssueCount; i++) {
                var tempIssue = issuesFromRequest.ttIssueList[i];
                var customisedIssue = new Object();
                var custInfoExtractedFromIssue = new Array();

                custInfoExtractedFromIssue = tempIssue.mindMapFields;
                customisedIssue.issueId = tempIssue.id;
                customisedIssue.assignee = tempIssue.assignee;
                customisedIssue.status = tempIssue.status;

                //tempIssue.created = {"year":2013,"month":12,"day":5,"timezone":120,"hour":23,"minute":49,"second":32,"fractionalSecond":0}
                // now build the yyyy/mm/dd HH:mm format
                var issueDateCreated = tempIssue.created.year + '/';
                var month = tempIssue.created.month;
                issueDateCreated += (month < 10 ? '0' + month : month) + '/';
                var day = tempIssue.created.day;
                issueDateCreated += (day < 10 ? '0' + day : day) + ' ';
                var hours = tempIssue.created.hour;
                issueDateCreated += (hours < 10 ? '0' + hours : hours) + ':';
                var minutes = tempIssue.created.minute;
                issueDateCreated += (minutes < 10 ? '0' + minutes : minutes) + ':';
                var seconds = tempIssue.created.second;
                issueDateCreated += (seconds < 10 ? '0' + seconds : seconds);
                customisedIssue.created = issueDateCreated;

                for (var x = 0; x < custInfoExtractedFromIssue.jiraField.length; x++) {
                    var tempJField = custInfoExtractedFromIssue.jiraField[x];
                    //Field names are configured in JIRA, any changes not inline with JIRA can/may might produce undesired effects
                    if (tempJField.fieldName === 'Customer Name') {
                        customisedIssue.firstName = tempJField.fieldValue;
                    }
                    if (tempJField.fieldName === 'Customer Email') {
                        customisedIssue.emailAddress = tempJField.fieldValue;
                    }
                    if (tempJField.fieldName === 'Smile Customer Phone') {
                        customisedIssue.alternativeContact1 = tempJField.fieldValue;
                    }
                    if (tempJField.fieldName === 'Add:Location') {
                        customisedIssue.addressLine1 = tempJField.fieldValue;
                    }
                    if (tempJField.fieldName === 'Add:Street') {
                        customisedIssue.addressLine2 = tempJField.fieldValue;
                    }
                    if (tempJField.fieldName === 'Add:Town/City') {
                        customisedIssue.addressTown = tempJField.fieldValue;
                    }
                    if (tempJField.fieldName === 'Address Code') {
                        customisedIssue.addressCode = tempJField.fieldValue;
                    }
                }

                simplifiedArray[i] = customisedIssue;
            }

            var simple = "{\"simpleIssue\":" + angular.toJson(simplifiedArray) + "}";
            $scope.ttIssues = JSON.parse(simple);
            $scope.looking = false;
        }).
                error(function (data, status) {

                    if ($scope.ttIssues === null) {
                        $scope.ttIssues = new Object();
                    }
                    var simple = "{\"simpleIssue\":[]}";
                    $scope.ttIssues = JSON.parse(simple);
                    $scope.ttIssues.simpleIssue = null;
                    $scope.looking = false;
                    $scope.serverError = "error";
                    $scope.wildCardChar = wildCard;
                });
    };

    $scope.phoneNumberRangeList = {
        items: []
    };

    $scope.ringfenceNumberRangeList = {
        items: []
    };

    $scope.addPhoneNumberRangeEntryToList = function (arrayName, phoneStart, phoneEnd) {
        if ($scope.$eval(arrayName).items == null) {
            $scope.$eval(arrayName).items = new Array();
        }

        var vStart = phoneStart;
        var vEnd = phoneEnd;

        if (phoneStart == null) {
            vStart = '';
        }

        if (phoneEnd == null) {
            vEnd = '';
        }

        $scope.$eval(arrayName).items.push({
            phoneNumberStart: vStart,
            phoneNumberEnd: vEnd
        });
    };

    $scope.removePhoneNumberRangeEntryFromList = function (arrayName, index) {
        $scope.$eval(arrayName).items.splice(index, 1);
    };

    $scope.phoneNumberRangeEntryListSize = function (arrayName) {
        var total = 0;
        angular.forEach($scope.$eval(arrayName).items, function (item) {
            total += 1;
        })

        if (arrayName == 'ringfenceNumberRangeList') { //Display or hide the document attachment division.
            var divDocuments = document.getElementById("divDocuments");
            if (divDocuments != null) {
                if (total > 0) {
                    divDocuments.style.display = 'block';
                } else {
                    divDocuments.style.display = 'none';
                }
            }
        }
        return total;
    };


    $scope.itemSelected = function (item) {
        if (item) {
            if ($scope.saleLineItems == null) {
                $scope.saleLineItems = new Array();
            }
            var clonedItem = (cloneObject(item.originalObject));
            clonedItem.quantity = 1;
            //console.log("ItemSelected --> PriceInCentsInc: " + clonedItem.priceInCentsIncl);
            clonedItem.unitPriceInCentsIncl = clonedItem.priceInCentsIncl / clonedItem.quantity;
            clonedItem.unitPriceInCentsExcl = clonedItem.priceInCentsExcl / clonedItem.quantity;
            
           if (clonedItem.itemNumber.startsWith("OTT")) {
                $scope.isP2PInvoicing = true;
                //if ($scope.isP2PInvoiceOption) {
                    var dtAsString = new Date().toISOString().slice(0, 10);
                    clonedItem.unitPriceInCentsIncl = $scope.daysInMonth(dtAsString) * clonedItem.priceInCentsIncl;
                    clonedItem.unitPriceInCentsExcl = $scope.daysInMonth(dtAsString) * clonedItem.priceInCentsExcl;
                    //console.log("ItemSelected --> UnitPriceInCentsIncl: " + clonedItem.unitPriceInCentsIncl);
                //}
            }
            
            if (clonedItem.itemNumber.startsWith("BENTMW")) {
                $scope.isP2PInvoicing = true;
                if ($scope.isP2PInvoiceOption) {
                    var dtAsString = new Date().toISOString().slice(0, 10);
                    clonedItem.unitPriceInCentsIncl = clonedItem.priceInCentsIncl / $scope.daysInMonth(dtAsString);
                    clonedItem.unitPriceInCentsExcl = clonedItem.priceInCentsExcl / $scope.daysInMonth(dtAsString);
                    //console.log("ItemSelected --> UnitPriceInCentsIncl: " + clonedItem.unitPriceInCentsIncl);
                }
            }
            
            clonedItem.description = split(clonedItem.description, " [");
            $scope.saleLineItems[$scope.saleLineItems.length] = clonedItem;
            document.getElementById("itemsAutoComplete_value").focus();
            document.getElementById("itemsAutoComplete_value").select();
            
            var  txtRecipientCustomerId =  document.getElementsByName("sale.recipientCustomerId")[0];
            var  txtRecipientAccountId =  document.getElementsByName("sale.recipientAccountId")[0];
            var  txtRecipientOrganisationId =  document.getElementsByName("sale.recipientOrganisationId")[0];
            var  txtTenderedCurrency =  document.getElementsByName("sale.tenderedCurrency")[0];
            
            
            if (clonedItem.itemNumber.startsWith("BUN") || clonedItem.itemNumber.startsWith("BENTMW"))  { // Check to see if the BUN has Upsize item and add them to the selected list. 
                var config = {
                    params: {
                        'getUpSizeBundlesJSON': '',
                        'inventoryQuery.stringMatch': clonedItem.itemNumber,
                        'inventoryQuery.recipientAccountId': txtRecipientAccountId.value,
                        'inventoryQuery.recipientCustomerId': txtRecipientCustomerId.value,
                        'inventoryQuery.recipientOrganisationId': txtRecipientOrganisationId.value,
                        'inventoryQuery.currency': txtTenderedCurrency.value
                    }
                };
                
                console.log("Upsize parameters - stringMatch=" + clonedItem.itemNumber + 
                        " recipientAccountId=" + txtRecipientAccountId.value +  
                        " recipientCustomerId=" + txtRecipientCustomerId.value + 
                        " recipientOrganisationId="+ txtRecipientOrganisationId.value + 
                        " currency:" + txtTenderedCurrency.value);

                $http.get(makeURL('Sales.action'), config).success(function (data) {
                    if ($scope.upsizeLineItems == null) {
                        $scope.upsizeLineItems = new Array();
                    }
                    
                    console.log("Got Upsize items - " + data.inventoryItems.length);
                    
                    for (var i = 0; i < data.inventoryItems.length; i++) {
                        var item = data.inventoryItems[i];
                        item.quantity = item.stockLevel; //0;
                        
                        if (item.itemNumber.startsWith("OTT")) { // OTT tax
                            $scope.isP2PInvoicing = true;
                            var dtAsString = new Date().toISOString().slice(0, 10);
                            item.unitPriceInCentsIncl = $scope.daysInMonth(dtAsString) * item.priceInCentsIncl;
                            item.unitPriceInCentsExcl = $scope.daysInMonth(dtAsString) * item.priceInCentsExcl;
                        } else {
                            item.unitPriceInCentsIncl = item.priceInCentsIncl;
                            item.unitPriceInCentsExcl = item.priceInCentsExcl;
                        }
                        item.isForUpSize = true;
                        console.log("Got Upsize item: SerialNumber:" + item.serialNumber + " ItemNumber:" + 
                                item.itemNumber + " Currency:" + item.currency + 
                                " Calculated unitPriceInCentsIncl: " + item.unitPriceInCentsIncl + " Quantity:" + item.quantity);
                        $scope.saleLineItems[$scope.saleLineItems.length] = item;
                        $scope.upsizeLineItems[$scope.upsizeLineItems.length] = item;
                    }
                });
            }
        }
    };

    function split(str, char) {
        var i = str.indexOf(char);

        if (i > 0)
            return  str.slice(0, i);
        else
            return "";
    }
    
    $scope.checkForUpSize = function (curIndex) { //Make sure only one upsize item can be selected.
        // Limit upsize quantity to 1 only and exclude OTT
        
       if($scope.saleLineItems[curIndex].isForUpSize == true && $scope.saleLineItems[curIndex].quantity > 1 && 
               !$scope.saleLineItems[curIndex].itemNumber.startsWith("OTT")) {
            $scope.saleLineItems[curIndex].quantity =  1;
        } 

        for (var i = 0; i <  $scope.saleLineItems.length; i++) {
            try { 
                if(curIndex != i && $scope.saleLineItems[i].isForUpSize) {
                    $scope.saleLineItems[i].quantity = 0;
                }
            }  catch (e) {
                console.log("Error:" + e.toString());
            }
        }
    };

    $scope.isP2PInvoicing = false;
    $scope.daysInMonth = function (date) {
        var tokens = date.split('-');
        var mydate = new Date(tokens[0], tokens[1] - 1, tokens[2]);
        var numOfDaysInMonth = new Date(mydate.getYear(), mydate.getMonth() + 1, 0).getDate();
        //console.log("Number of days in this month: " + numOfDaysInMonth);
        var quant = (numOfDaysInMonth - mydate.getDate()) + 1;//include current day

        if ($scope.saleLineItems == null) {
            $scope.saleLineItems = new Array();
        }

        for (var i = 0; i < $scope.saleLineItems.length; i++) {

            if ($scope.saleLineItems[i].itemNumber.startsWith("OTT")) {
                $scope.saleLineItems[i].quantity = 1;
                if ($scope.isP2PInvoiceOption) {
                    // $scope.saleLineItems[i].unitPriceInCentsIncl = (numOfDaysInMonth/30) * $scope.saleLineItems[i].priceInCentsIncl;
                    // $scope.saleLineItems[i].unitPriceInCentsExcl = (numOfDaysInMonth/30) * $scope.saleLineItems[i].priceInCentsExcl;
                    
                   $scope.saleLineItems[i].unitPriceInCentsIncl = $scope.saleLineItems[i].priceInCentsIncl; // numOfDaysInMonth;
                   $scope.saleLineItems[i].unitPriceInCentsExcl = $scope.saleLineItems[i].priceInCentsExcl; // numOfDaysInMonth;
                   $scope.saleLineItems[i].quantity = quant;
                    
                   console.log("OTT: unitPriceInCentsIncl = " + $scope.saleLineItems[i].unitPriceInCentsIncl);
                   console.log("OTT: unitPriceInCentsExcl = " + $scope.saleLineItems[i].unitPriceInCentsExcl);
                   console.log("OTT: quantity = " + $scope.saleLineItems[i].quantity);                   
                } else { //P2P Option is off - return to full month.
                   $scope.saleLineItems[i].unitPriceInCentsIncl = $scope.saleLineItems[i].priceInCentsIncl * numOfDaysInMonth;
                   $scope.saleLineItems[i].unitPriceInCentsExcl = $scope.saleLineItems[i].priceInCentsExcl * numOfDaysInMonth;
                   $scope.saleLineItems[i].quantity = 1;
                }
            } 
            
            if ($scope.saleLineItems[i].itemNumber.startsWith("BENTMW")) {
                $scope.saleLineItems[i].quantity = 1;
                if ($scope.isP2PInvoiceOption) {
                    $scope.saleLineItems[i].unitPriceInCentsIncl = $scope.saleLineItems[i].priceInCentsIncl / numOfDaysInMonth;
                    $scope.saleLineItems[i].unitPriceInCentsExcl = $scope.saleLineItems[i].priceInCentsExcl / numOfDaysInMonth;
                    $scope.saleLineItems[i].quantity = quant;
                }
            }
        }

        return numOfDaysInMonth;

    };

    $scope.p2pInvoicingDate = new Date().toISOString().slice(0, 10);
    $scope.isP2PInvoiceOption = false;


}

function CustomDatepicker() {//JQuery datepicker does not bind with angularjs models, hence this custom datepicker directive
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, el, attrs, ngModel) {
            var date = new Date();
            var endDate = date.getFullYear();
            $j(el).datepicker({
                dateFormat: 'yy-mm-dd',
                showOn: 'button',
                buttonText: "...",
                yearRange: '2012:' + endDate,
                //maxDate: '0',
                changeYear: true,
                changeMonth: true,
                onSelect: function (date) {
                    scope.$apply(function () {
                        ngModel.$setViewValue(date);
                    });
                }
            });

        }
    }
}
