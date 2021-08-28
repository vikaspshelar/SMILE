'use strict';

/* Directives */

/* D SLD */
scpApp.directive('basicDirective', function() {
    return {
      template : 'Name: {{customer.name}}<br /> Street: {{customer.street}}'
    }
});
    
scpApp.directive('basicDirectiveTwo', function() {
    return {
      restrict : 'E',
      template : '<span> Directive Two</span>',
    }
});