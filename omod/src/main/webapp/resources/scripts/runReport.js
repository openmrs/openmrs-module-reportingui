var runReportApp = angular.module("runReportApp", [ ]).
    filter('translate', function() {
        return function(input, prefix) {
            var code = prefix ? prefix + input : input;
            return emr.message(code, input);
        }
    });

runReportApp.controller('RunReportController', ['$scope', '$http', '$window', '$timeout', function($scope, $http, $window, $timeout) {

    $scope.loading = true;

    $scope.queue = [];

    $scope.completed = [];

    $scope.submitting = false;

    $scope.refreshHistory = function() {
        $http.get("reportStatus/getQueuedRequests.action?reportDefinition=" + $window.reportDefinition.uuid).
            success(function(data, status, headers, config) {
                $scope.queue = data;
                $scope.loading = false;
                if ($scope.queue.length > 0) {
                    $timeout($scope.refreshHistory, 10000);
                }
            }).
            error(function(data, status, headers, config) {
                console.log("Error getting queue: " + status);
                $scope.queue = [];
            });

        $http.get("reportStatus/getCompletedRequests.action?reportDefinition=" + $window.reportDefinition.uuid).
            success(function(data, status, headers, config) {
                $scope.completed = data;
            }).
            error(function(data, status, headers, config) {
                console.log("Error getting completed: " + status);
                $scope.completed = [];
            });

        jQuery("#returnButton").hide();
        if (fetchReturnURL()) {
            jQuery("#returnButton").show();
        }
    }

    $scope.hasResults = function() {
        return !$scope.loading && $scope.completed.length > 0;
    }

    $scope.hasNoResults = function() {
        return !$scope.loading && $scope.completed.length == 0;
    }

    var defaultSuccessAction = function(data, status, headers, config) {
        emr.successMessage(data.message);
        $scope.refreshHistory();
    }

    var defaultErrorAction = function(data, status, headers, config) {
        emr.errorMessage(data.message);
        $scope.refreshHistory();
    }

    $scope.cancelRequest = function(request) {
        $http.post("reportStatus/cancelRequest.action?reportRequest=" + request.uuid).
            success(defaultSuccessAction).
            error(defaultErrorAction);
    }

    $scope.canSave = function(request) {
        return request.status == 'COMPLETED';
    }

    $scope.saveRequest = function(request) {
        $http.post("reportStatus/saveRequest.action?reportRequest=" + request.uuid).
            success(defaultSuccessAction).
            error(defaultErrorAction);
    }

    $scope.runReport = function() {
        // this is a plain old form, not really using angular. need to rewrite the datepickers to be angular-friendly
        var form = angular.element('#run-report');
        var missingParams = [ ];
        var submission = [], obj;

        form.each(function(){
            $(this.elements).each(function(){
                if (this.value == "" && this.className.includes("required")) {
                    missingParams.push(this.name);
                }else{
                    obj = {}
                    obj["name"] = this.name;
                    obj["value"] = this.value;
                    obj["class"] = this.className;
                    submission.push(obj)
                }
            })
        });

        if (missingParams.length > 0) {
            emr.errorMessage(emr.message("reportingui.runReport.missingParameter", "Missing parameter values"));
        }
        else {
            $scope.submitting = true;
            $.post("runReport.page?reportDefinition=" + $window.reportDefinition.uuid, submission, function() {
                location.href = location.href;
            });
        }
    }

    $scope.returnToURL = function() {
        window.location.href = fetchReturnURL()
    }

    var fetchReturnURL = function() {
        return new URLSearchParams(window.location.search).get("returnUrl")
    }

    $scope.reRunRequest = function(request) {
        request.reportDefinition.mappings.forEach(function(mapping) {
            let $inputField = jQuery('#run-report').find("input[name*='" + mapping.name + "']");
            if ($inputField) {
                let value = mapping.dateValue ?? mapping.value;
                jQuery($inputField).val(value);
                if (mapping.dateValue) {
                    jQuery($inputField).parent().find("input[type='text']").val(mapping.value);
                }
            }
        });
    }

}]);
