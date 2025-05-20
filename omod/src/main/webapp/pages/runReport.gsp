<%
    ui.decorateWith("appui", "standardEmrPage", [includeBootstrap: false])
    ui.includeCss("reportingui", "runReport.css")

    ui.includeJavascript("uicommons", "angular.min.js")
    ui.includeJavascript("reportingui", "runReport.js")

    def interactiveClass = context.loadClass("org.openmrs.module.reporting.report.renderer.InteractiveReportRenderer")
    def isInteractive = {
        interactiveClass.isInstance(it.renderer)
    }

    def quartersOptions = []
    quartersOptions << [label: 'Q1', value: 'Q1', selected: false]
    quartersOptions << [label: 'Q2', value: 'Q2', selected: false]
    quartersOptions << [label: 'Q3', value: 'Q3', selected: false]
    quartersOptions << [label: 'Q4', value: 'Q4', selected: false]

    def ALLOWED_LOCALE_LIST = org.openmrs.api.context.Context.getAdministrationService().getGlobalProperty('locale.allowed.list')

    def localeList = ALLOWED_LOCALE_LIST.split(',')*.trim();

    def localeOptions = []
    localeList.each { locale ->
        localeOptions << [label: locale.toUpperCase(), value: locale, selected: false]
    }
%>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        <% if (breadcrumb) { %>
            ${ breadcrumb },
        <% } else { %>
            { label: "${ ui.escapeJs(ui.message("reportingui.reportsapp.home.title")) }", link: emr.pageLink("reportingui", "reportsapp/home") },
        <% } %>
        { label: "${ ui.message(ui.format(reportDefinition)) }", link: "${ ui.escapeJs(ui.thisUrl()) }" }
    ];

    window.reportDefinition = {
        uuid: '${ reportDefinition.uuid}'
    };
</script>

${ ui.includeFragment("appui", "messages", [ codes: [
        "reportingui.reportRequest.Status.REQUESTED", "reportingui.reportRequest.Status.SCHEDULED", "reportingui.reportRequest.Status.PROCESSING",
        "reportingui.reportRequest.Status.FAILED", "reportingui.reportRequest.Status.COMPLETED", "reportingui.reportRequest.Status.SCHEDULE_COMPLETED",
        "reportingui.reportRequest.Status.SAVED",
        "reportingui.reportRequest.Priority.HIGHEST", "reportingui.reportRequest.Priority.HIGH", "reportingui.reportRequest.Priority.NORMAL",
        "reportingui.reportRequest.Priority.LOW", "reportingui.reportRequest.Priority.LOWEST",
        "reportingui.runReport.missingParameter"]
])}

<div ng-app="runReportApp" ng-controller="RunReportController" ng-init="refreshHistory()">

    <h1>${ ui.message(reportDefinition.name) }</h1>
    <h3>${ ui.message(reportDefinition.description) }</h3>

    <div class="past-reports">
        <fieldset ng-show="queue" class="report-list">
            <legend>${ ui.message("reportingui.runReport.queue.legend") }</legend>
            <table>
                <thead>
                    <tr>
                        <th>${ ui.message("reportingui.reportRequest.status") }</th>
                        <th>${ ui.message("reportingui.reportRequest.parameters") }</th>
                        <th>${ ui.message("reportingui.reportRequest.requested") }</th>
                        <th>${ ui.message("reportingui.reportRequest.actions") }</th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="request in queue">
                        <td>
                            <img class="right small" ng-show="request.status=='PROCESSING'" src="${ ui.resourceLink("uicommons", "images/spinner.gif") }"/>
                            {{request.status | translate:'reportingui.reportRequest.Status.'}}
                            <span ng-show="request.status=='REQUESTED'">
                                <br/>
                                ${ ui.message("reportingui.reportRequest.priority") }: {{request.priority | translate:'reportingui.ReportRequest.Priority.'}} <br/>
                                ${ ui.message("reportingui.reportRequest.position") }: {{request.positionInQueue}}
                            </span>
                            <span ng-hide="request.status=='REQUESTED'">
                                <br/>
                                {{request.evaluateCompleteDatetime}}
                            </span>
                        </td>
                        <td>
                        <% reportDefinition.parameters.each { %>
                            ${ ui.message(it.labelOrName) }:
                            <span ng-repeat="param in request.reportDefinition.mappings" ng-show="param.name == '${it.name}'">
                                {{ param.value }} <br/>
                            </span>
                        <% } %>
                        </td>
                        <td>
                            {{request.requestedBy}} <br/>
                            {{request.requestDate}}
                        </td>
                        <td>
                            <a ng-show="request.status=='REQUESTED'" ng-click="cancelRequest(request)">
                                <i class="small icon-remove"></i>
                                ${ ui.message("emr.cancel") }
                            </a>
                        </td>
                    </tr>
                </tbody>
            </table>
        </fieldset>

        <fieldset class="report-list">
            <legend>${ ui.message("reportingui.runReport.completed.legend") }</legend>

            <span ng-show="loading">
                ${ ui.message("uicommons.loading.placeholder") }
                <img src="${ ui.resourceLink("uicommons", "images/spinner.gif") }"/>
            </span>

            <span ng-show="hasNoResults()">
                ${ ui.message("emr.none") }
            </span>

            <table ng-show="hasResults()">
                <thead>
                    <tr>
                        <th>${ ui.message("reportingui.reportRequest.status") }</th>
                        <th>${ ui.message("reportingui.reportRequest.parameters") }</th>
                        <th>${ ui.message("reportingui.reportRequest.requested") }</th>
                        <th>${ ui.message("reportingui.reportRequest.actions") }</th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="request in completed">
                        <td>
                            {{request.status | translate:'reportingui.reportRequest.Status.'}} <br/>
                            {{request.evaluateCompleteDatetime}}
                        </td>
                        <td>
                            <% reportDefinition.parameters.each { %>
                                ${ ui.message(it.labelOrName) }:
                                <span ng-repeat="param in request.reportDefinition.mappings" ng-show="param.name == '${it.name}'">
                                    {{ param.value }} <br/>
                                </span>
                            <% } %>
                        </td>
                        <td>
                            {{request.requestedBy}} <br/>
                            {{request.requestDate}}
                        </td>
                        <td>
                            <span class="download" ng-show="request.status == 'COMPLETED' || request.status == 'SAVED'">
                                <a href="${ ui.pageLink("reportingui", "viewReportRequest") }?request={{ request.uuid }}">
                                    <span ng-show="request.renderingMode.interactive">
                                        <i class="icon-eye-open small"></i>
                                        ${ ui.message("reportingui.reportHistory.open") }
                                    </span>
                                    <span ng-hide="request.renderingMode.interactive">
                                        <i class="icon-download small"></i>
                                        ${ ui.message("uicommons.downloadButtonLabel") }
                                    </span>
                                </a>
                                <br/>
                                <a ng-show="canSave(request)" ng-click="saveRequest(request)">
                                    <i class="icon-save small"></i>
                                    ${ ui.message("reportingui.reportRequest.save.action") }
                                </a>
                                <br/>
                                <a ng-hide="request.renderingMode.interactive" ng-click="reRunRequest(request)">
                                    <i class="icon-play small"></i>
                                    ${ ui.message("reportingui.reportRequest.runAgain.action") }
                                </a>
                            </span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </fieldset>
    </div>

    <div class="running-reports">
        <fieldset>
            <legend>${ ui.message("reportingui.runReport.run.legend") }</legend>

            <form method="post" ng-submit="runReport()" id="run-report">
                <% reportDefinition.parameters.each {
                    def paramLabel = ui.message(it.labelOrName);
                %>
                <p>
                    <% if (it.collectionType) { %>
                        Parameters of type = collection are not yet implemented
                    <% } else if (it?.widgetConfiguration?.uiframeworkFragmentProvider) { %>
                        ${ ui.includeFragment(it.widgetConfiguration.uiframeworkFragmentProvider, it.widgetConfiguration.uiframeworkFragment, [
                                formFieldName: "parameterValues[" + it.name + "]",
                                label: paramLabel,
                                classes: [(it.required == true ? "required" : '')]
                        ])}
                    <% } else if (it.type == java.util.Date) { %>
                        ${ ui.includeFragment("uicommons", "field/datetimepicker", [
                                formFieldName: "parameterValues[" + it.name + "]",
                                label: paramLabel,
                                useTime: false,
                                defaultDate: it.defaultValue,
                                classes: [(it.required == true ? "required" : '')]
                        ])}
                    <% } else if (it.type == org.openmrs.Location) { %>
                        ${ ui.includeFragment("uicommons", "field/location", [
                                formFieldName: "parameterValues[" + it.name + "]",
                                label: paramLabel,
                                initialValue: it.defaultValue ?: sessionContext.sessionLocation,
                                classes: ["drop-down-list " + (it.required == true ? "required" : '')]
                        ])}
                    <% } else if (it.type == org.openmrs.EncounterType) { %>
                        ${ ui.includeFragment("uicommons", "field/encounterType", [
                                formFieldName: "parameterValues[" + it.name + "]",
                                label: paramLabel,
                                initialValue: it.defaultValue,
                                classes: ["drop-down-list " + (it.required == true ? "required" : '')]
                        ])}
                    <% } else if (it.type == java.lang.String && it.name.equalsIgnoreCase("Quarter")) { %>
                        ${ ui.includeFragment("uicommons", "field/dropDown", [
                                formFieldName: "parameterValues[" + it.name + "]",
                                label: paramLabel,
                                options: quartersOptions,
                                hideEmptyLabel: true,
                                initialValue: it.defaultValue,
                                classes: ["drop-down-list " + (it.required == true ? "required" : '')]
                        ])}
                    <% } else if (it.type == java.lang.String && it.name.equalsIgnoreCase("Locale")) { %>
                        ${ ui.includeFragment("uicommons", "field/dropDown", [
                                formFieldName: "parameterValues[" + it.name + "]",
                                label: paramLabel,
                                options: localeOptions,
                                hideEmptyLabel: true,
                                initialValue: it.defaultValue,
                                classes: ["drop-down-list " + (it.required == true ? "required" : '')]
                        ])}
                    <% } else if (it.type == java.lang.String && it?.widgetConfiguration?.optionValues) { %>
                    <%
                            def options = []
                            it.widgetConfiguration.optionValues.split(",").eachWithIndex { optionValue, index ->
                                options[index] = [label: optionValue, value: optionValue, selected: false]
                            }
                            if (it?.widgetConfiguration?.optionLabels) {
                                it.widgetConfiguration.optionLabels.split(",").eachWithIndex { optionLabel, index ->
                                    options[index].label = ui.message(optionLabel)
                                }
                            }
                    %>
                    ${ ui.includeFragment("uicommons", "field/dropDown", [
                            formFieldName: "parameterValues[" + it.name + "]",
                            label: paramLabel,
                            options: options,
                            hideEmptyLabel: true,
                            initialValue: it.defaultValue,
                            classes: ["drop-down-list " + (it.required == true ? "required" : '')]
                    ])}
                    <% } else if (it.type == java.lang.String || it.type == java.lang.Integer) { %>
                        ${ ui.includeFragment("uicommons", "field/text", [
                                formFieldName: "parameterValues[" + it.name + "]",
                                label: paramLabel,
                                initialValue: it.defaultValue,
                                classes: ["drop-down-list " + (it.required == true ? "required" : '')]
                        ])}
                    <% } else { %>
                        Unknown parameter type: ${ it.type }
                    <% } %>
                </p>
                <% } %>
                <%
                    def renderingOptions = renderingModes.findAll {
                        !isInteractive(it)
                    }
                    .collect {
                        [ value: it.descriptor, label: ui.message(it.label) ]
                    }
                %>
                <% if (renderingOptions.size() == 1) { %>
                    <p>
                        <label>
                            ${ ui.message("reportingui.reportRequest.outputFormat") }
                        </label>
                        <input type="hidden" name="renderingMode" value="${ ui.escapeAttribute(renderingOptions[0].value) }"/>
                        ${ renderingOptions[0].label }
                    </p>
                <% } else { %>
                    ${ ui.includeFragment("uicommons", "field/dropDown", [
                            formFieldName: "renderingMode",
                            label: ui.message("reportingui.reportRequest.outputFormat"),
                            hideEmptyLabel: true,
                            options: renderingOptions
                    ]) }
                <% } %>

                <button id="submitButton" type="submit" class="confirm right" ng-class="{disabled: submitting}" >
                    <i class="icon-play"></i>
                    ${ ui.message("reportingui.runButtonLabel") }
                </button>
                <button id="returnButton" type="button" class="confirm right" ng-class="{disabled: submitting}"  ng-click="returnToURL(parameters)" ng-disabled="submitting" >
                    <i class="icon-play"></i>
                    ${ ui.message("reportingui.returnButtonLabel") }
                </button>
            </form>
        </fieldset>
    </div>

</div>
