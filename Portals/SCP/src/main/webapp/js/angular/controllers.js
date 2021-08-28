'use strict';

/* Controllers */

scpApp.controller('BasicController', function ($scope, $timeout, scpFactory) {
    $scope.editorEnabled = false;

    $scope.getTargetServiceInstanceUserName = function () {
        //send request to server if the account is 10 or more characters
        if ($scope.targetAccountId.toString().length < 10) {
            $scope.targetCustomerWithProduct = null;
            return;
        }
        scpFactory.getTargetServiceInstanceUserName($scope.targetAccountId).then(function (data) {
            $scope.targetCustomerWithProduct = data;
        });
    };

    $scope.disableEditor = function () {
        $scope.editorEnabled = false;
    };

    $scope.enableEditor = function (placeHolder) {
        $scope.editorEnabled = true;
        $scope.editableName = placeHolder;
    };


    $scope.modifyProductInstanceFriendlyName = function (customerId, productInstanceId) {

        scpFactory.modifyProductInstanceFriendlyName(customerId, productInstanceId, $scope.editableName).then(function (data) {

            var productInstance = new Object();
            productInstance = data;
            $scope.disableEditor();
            if (angular.isDefined(productInstance.done)) {
                if ("FALSE" === productInstance.done.toUpperCase()) {
                    $scope.serverError = "error";
                    $scope.productName = productInstance.friendlyName;
                }
            } else {
                if (angular.isDefined(productInstance.friendlyName)) {
                    $scope.serverSuccess = "succesful";
                    $scope.productName = productInstance.friendlyName;
                } else {
                    $scope.productName = $scope.editableName;
                }
            }
        }, function (error) {
            $scope.serverError = "error";
        });

    };


    $scope.tcstype = [];
    $scope.termsAndConditionsEnabled = false;
    $scope.showGatewayTermsAndConditions = function () {
        $scope.termsAndConditionsEnabled = true;
    };


    $scope.amountInMajorUnits = '';
    $scope.airtimeAmountInMajorUnits = 0;
    $scope.dynamicUnitCreditSpecificationId = 0;
    $scope.dynamicProductInstanceId = 0;
    $scope.productInstanceId = 0;

    $scope.accountSIMsToDisplay = {};//Helps to decide which particular account's SIMs should be shown to the user
    $scope.simProductInstanceId = {};//Multiple services on one account
    $scope.singleServiceProductInstanceId = {};//The is only one service on the account


    $scope.airtimeAmountChanged = function () {
        $scope.amountInMajorUnits = '';
        if (!isNaN(Number($scope.airtimeAmountInMajorUnits)) && isFinite(Number($scope.airtimeAmountInMajorUnits))) {
            $scope.amountInMajorUnits += Math.floor(Number($scope.airtimeAmountInMajorUnits));//round down to the nearest integer; no kobos
        }
    };

    $scope.payingAccountChanged = function (index, accountId) {
        $scope.accountSIMsToDisplay = {}
        $scope.accountSIMsToDisplay[accountId] = accountId;
        $scope.dynamicUnitCreditSpecificationId = 0;
    };

    $scope.simCardSelectedChanged = function (accountId) {
        $scope.dynamicProductInstanceId = 0;
        if (!isNaN($scope.simProductInstanceId[accountId])) {
            $scope.dynamicProductInstanceId = Number($scope.simProductInstanceId[accountId]);
        }
    };

    $scope.singleAccountSimCardSelectedChanged = function (productInstanceId) {
        $scope.dynamicProductInstanceId = 0;
        if (!isNaN(productInstanceId)) {
            $scope.dynamicProductInstanceId = Number(productInstanceId);
        }
    };

    $scope.selectedGatewayCode = 'Unknown';
    $scope.selectedItemPaymentGatewayType = {};

    $scope.selectedCardIntegrationChanged = function (index) {
        $scope.selectedGatewayCode = $scope.selectedItemPaymentGatewayType[index];
    };

    $scope.paymentMethodChanged = function (index) {
        $scope.selectedGatewayCode = $scope.selectedItemPaymentGatewayType[index];
    };

    $scope.gatewayCodeValue = 'Unknown';
    $scope.paidByAccountId = 0;
    $scope.gatewayCodeChanged = function (gatewayCode) {
        $scope.gatewayCodeValue = gatewayCode;
    };

    $scope.displayProceedButton = false;
    $scope.paidByAccountIdChanged = function (wallet, accountId) {
        $scope.paidByAccountId = 0;
        $scope.displayProceedButton = true;
        if (wallet == 'Wallet') {
            $scope.paidByAccountId = accountId;
        }
    };

    $scope.enableProceedButton = function () {
        $scope.displayProceedButton = true;
    };

    $scope.urlLinkSelected = false;
    $scope.otherTypesSelected = true;
    $scope.paymentUrlLink = '';

    $scope.enableProceedButtonByType = function (paymentType, link) {
        $scope.displayProceedButton = true;
        $scope.paymentUrlLink = '';

        if (paymentType == 'LINK') {
            $scope.urlLinkSelected = true;
            $scope.otherTypesSelected = false;
            $scope.paymentUrlLink = link;
        } else {
            $scope.otherTypesSelected = true;
            $scope.urlLinkSelected = false;
        }
    };

    $scope.me2uAllInActive = true;
    $scope.showAllMe2UAccounts = false;
    $scope.sourceAccountId = 0;
    $scope.enableMe2UToAll = function (value) {

        if (value == 'YES') {
            $scope.me2uAllInActive = false;
            $scope.targetCustomerWithProduct = null;
            $scope.targetAccountId = 0;
            $scope.showAllMe2UAccounts = true;
            $scope.getMe2UApplicableAccounts($scope.sourceAccountId);
        } else {
            $scope.me2uAllInActive = true;
            $scope.showAllMe2UAccounts = false;
        }

    };


    $scope.getMe2UApplicableAccounts = function (accountId) {
        if (($scope.showAllMe2UAccounts && !isNaN(Number(accountId))) || (!isNaN(Number(accountId)) && Number(accountId) == 0)) {
            scpFactory.getMe2UCompliantAccountList(accountId).then(function (data) {
                $scope.Me2UAccountList = data;
            }, function (error) {
            });
        }
    };


    $scope.dataLoading = true;
    $scope.dataLoadingCounter = 0;
    $scope.transactionStatusJS = 'unsuccessful';

    var poll = function (orderID, transactionReference) {
        $scope.dataLoading = true;
        var timer = $timeout(function () {
            scpFactory.getPaymentGatewayTransactionStatus(orderID, transactionReference).then(function (data) {

                var paymentGatewaySaleStatus = new Object();
                paymentGatewaySaleStatus = data;
                if (!angular.isDefined(paymentGatewaySaleStatus.saleId)) {
                    $scope.paymentGatewaySale = paymentGatewaySaleStatus;
                } else {
                    $scope.paymentGatewaySale = paymentGatewaySaleStatus;
                    if ($scope.paymentGatewaySale.status == 'PD') {
                        $scope.transactionStatusJS = 'successful';
                    }

                    var arrayGatewaResponse = $scope.paymentGatewaySale.paymentGatewayResponse.split("\n");

                    for (var x = 0; x < arrayGatewaResponse.length; x++) {
                        var avpArray = arrayGatewaResponse[x].split("=");
                        var attrib = avpArray[0];
                        var attribValue = avpArray[1];
                        if (attrib == "paymentRef") {
                            $scope.paymentRef = attribValue;
                        }
                        if (attrib == "issueId") {
                            $scope.issueId = attribValue;
                        }
                        if (attrib == "responseDescription") {
                            $scope.responseDescription = attribValue;
                        }
                        if (attrib == "titleMessage") {
                            $scope.titleMessage = attribValue;
                        }
                    }
                }
            }, function (error) {
            }).finally(function () {
                $scope.dataLoading = false;
                $scope.dataLoadingCounter++;
            });


        }, 10000);

        timer.then(function () {

            if (!angular.isDefined($scope.paymentGatewaySale)) {
                $timeout(function () {
                    poll(orderID, transactionReference);
                }, 5000);
            } else {
                if ($scope.paymentGatewaySale.status != 'PD' && $scope.paymentGatewaySale.status == 'PP' && $scope.dataLoadingCounter < 15) {
                    $timeout(function () {
                        poll(orderID, transactionReference);
                    }, 5000);
                }
            }
        });

        $scope.$on("$destroy", function (event) {
            $timeout.cancel(timer);
        });

    };

    $scope.pollData = function (orderID, transactionReference) {
        poll(orderID, transactionReference);
    };


    $scope.initBeforePollData = function (orderID, transactionReference) {
        $scope.dataLoading = true;
        scpFactory.getPaymentGatewayTransactionStatus(orderID, transactionReference).then(function (data) {
            $scope.paymentGatewaySale = data;
            if (angular.isDefined($scope.paymentGatewaySale)) {
                if ($scope.paymentGatewaySale.status == 'PD') {
                    $scope.transactionStatusJS = 'successful';
                }
                var arrayGatewaResponse = $scope.paymentGatewaySale.paymentGatewayResponse.split("\n");
                for (var x = 0; x < arrayGatewaResponse.length; x++) {
                    var avpArray = arrayGatewaResponse[x].split("=");
                    var attrib = avpArray[0];
                    var attribValue = avpArray[1];
                    if (attrib == "paymentRef") {
                        $scope.paymentRef = attribValue;
                    }
                    if (attrib == "issueId") {
                        $scope.issueId = attribValue;
                    }
                    if (attrib == "responseDescription") {
                        $scope.responseDescription = attribValue;
                    }
                    if (attrib == "titleMessage") {
                        $scope.titleMessage = attribValue;
                    }
                }
            }
        }, function (error) {
        }).finally(function () {
            $scope.dataLoading = false;
        });
    };

    $scope.airtimeVisible = false;
    $scope.unitCreditVisible = false;

    $scope.toggleAirtime = function () {
        $scope.airtimeVisible = !$scope.airtimeVisible;
        $scope.unitCreditVisible = false;
    };
    $scope.toggleUnitCredit = function () {
        $scope.unitCreditVisible = !$scope.unitCreditVisible;
        $scope.airtimeVisible = false;
    };

    $scope.getAccountsSIMs = function (accountId) {
        //console.log("Account id is called: ", accountId)
        if (angular.isUndefined(accountId) || accountId == null) {
            $scope.productInstanceID = 0;
            return;
        }
        scpFactory.getAccountsSIMs(accountId).then(function (data) {
            $scope.SIMs = data;
            $scope.productInstanceID = $scope.SIMs.accountSIMs.products[0].productInstanceId;
        }, function (error) {
        });
    };

    $scope.toggleProductInstanceID = function (piId) {
        $scope.productInstanceID = piId;
    };




    /* START OF PAGINATION LOGIC AND CONTROL */


    $scope.accountsPerPage = 25; // actionBean.customerQuery.productInstanceResultLimit ::: this should match however many results your API puts on one page
    $scope.pendingPagesCount = 0;
    $scope.splitUnitCreditDataTargetAccount = 0;

    $scope.pageChangeHandler = function (newPage) {
        $scope.getResultsPage(newPage);
    };


    $scope.getResultsPage = function (pageNumber) {
       //console.log("Current page: ", pageNumber)
        scpFactory.getCustomersAccountsPage(pageNumber).then(function (data) {
            $scope.accountsPerPage = data.userAccounts.numberOfAccounts;
            $scope.pendingPagesCount = data.userAccounts.pendingPagesCount;
            $scope.usersAccounts = data.userAccounts.accounts;
            $scope.payingAccountID= data.userAccounts.accounts[0].accountId;
            //console.log('Back-end : ', data);
        }, function (error) {
            //console.log('ERROR Back-end : ', error);
        });
    }

    /* END OF PAGINATION LOGIC AND CONTROL */




});
