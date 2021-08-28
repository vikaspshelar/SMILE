'use strict';

/* Services */
scpApp.factory('scpFactory', function ($http, $q, $log) {

    /*
     *  Would prefer the service to post-process the result and deal with the $http errors in the service layer instead of the controller handling that logic,
     *  so to achieve this we build our own promise by using the $q service
     */
    var scpFactory = {};

    function makeURL(action) {
        return action + ';' + getJSessionId();
    }

    scpFactory.getCustomer = function (customerId) {
        var deferred = $q.defer();
        var config = {
            params: {
                'getCustomerJSON': '',
                'customerQuery.customerId': customerId
            }
        };
        $http.get(makeURL('Customer.action'), config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
            deferred.reject(msg);
        });
        return deferred.promise;
    };

    scpFactory.getCustomerInSession = function () {
        var deferred = $q.defer();
        var config = {
            params: {
                'getCustomerInSessionAsJSON': ''
            }
        };
        $http.get(makeURL('Customer.action'), config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
            deferred.reject(msg);
        });
        return deferred.promise;
    };

    scpFactory.getTargetServiceInstanceUserName = function (targetAccountId) {
        var deferred = $q.defer();
        var config = {
            params: {
                'getCustomerNameViaXMLHTTP': '',
                'account.accountId': targetAccountId
            }
        };

        $http.get(makeURL('Account.action'), config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
            deferred.reject(msg);
        });
        return deferred.promise;
    };

    scpFactory.modifyProductInstanceFriendlyName = function (customerId, productInstanceId, friendlyName) {
        var deferred = $q.defer();
        var config = {
            params: {
                'changeProductInstanceFriendlyName': ''
            },
            headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8'}
        };

        var payload = "productOrder.customerId=" + customerId + "&productOrder.productInstanceId=" + productInstanceId + "&productOrder.friendlyName=" + friendlyName;

        $http.post(makeURL('Product.action'), payload, config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
            deferred.reject(msg);
        });
        return deferred.promise;
    };

    scpFactory.getPaymentGatewayTransactionStatus = function (OrderID, TransactionReference) {
        var deferred = $q.defer();
        var config = {
            params: {
                'getPaymentGatewayTransactionStatusAsJSON': '',
                'sale.saleId': OrderID
                        //'paymentGatewayTransactionQuery.paymentGatewayTransactionId': TransactionReference
            },
            timeout: 5000
        };

        $http.get(makeURL('PaymentGateway.action'), config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
            deferred.reject(msg);
        });
        return deferred.promise;
    };

    scpFactory.getMe2UCompliantAccountList = function (accountId) {
        var deferred = $q.defer();
        var config = {
            params: {
                'getMe2UCompliantAccountListAsJSON': '',
                'account.accountId': accountId
            },
            timeout: 5000
        };

        $http.get(makeURL('Account.action'), config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
            deferred.reject(msg);
        });
        return deferred.promise;
    };
    
    scpFactory.getAccountsSIMs = function (accountId) {
        var deferred = $q.defer();
        var config = {
            params: {
                'getAccountsSIMsAsJSON': '',
                'account.accountId': accountId
            },
            timeout: 20000
        };

        $http.get(makeURL('Account.action'), config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
            deferred.reject(msg);
        });
        return deferred.promise;
    };
    
    scpFactory.getCustomersAccountsPage = function (pageNumber) {
        var deferred = $q.defer();
        var config = {
            params: {
                'getCustomersAccountsAsJson': '',
                'pageNumber': pageNumber
            },
            timeout: 20000
        };

        $http.get(makeURL('Account.action'), config)
                .success(function (data) {
                    deferred.resolve(data);
                }).error(function (msg, code) {
                    //console.log(msg);
            deferred.reject(msg);
        });
        return deferred.promise;
    };

    return scpFactory;

});
