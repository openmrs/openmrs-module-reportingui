<script type="text/ng-template" id="modalContent.page">
    <div class="modal-header">${ ui.message('reportingui.adHocReport.modal.header') }</div>
    <div class="modal-body">
        <div class="modal-definition">
            <p class="modal-definition-name"> {{ definition.name }} </p>
            <p> {{ definition.description }} </p>
        </div>
        <div class="modal-params">
            <div ng-repeat="param in definition.parameters | filter:paramFilter">
                <span ng-switch on="param.type">
                    <span class="angular-datepicker" ng-switch-when="class java.util.Date">
                        <div class="form-horizontal">
                            {{ param.name }}:
                            <input type="text" class="datepicker-input" datepicker-popup="dd-MMMM-yyyy" ng-model="param.value" is-open="isOpen" date-disabled="disabled(date, mode)" show-weeks="false" />
                            <i class="icon-calendar btn" ng-click="openDatePicker(isOpen)"></i>
                        </div>
                    </span>
                    <span class="modal-encounter-types" ng-switch-when="class org.openmrs.EncounterType">
                        <p>${ ui.message('reportingui.adHocReport.modal.encounterTypes')}</p>
                        <% def types = context.encounterService.allEncounterTypes %>
                        <% types.each { %>
                            <p><input name="${ it.name }" type="checkbox" value="${ it }" check-list="selection">${ it.name }</p>
                        <% } %>
                    </span>
                    <span ng-switch-default>
                        {{ param.name }}: <input class="modal-input-text" type="text" ng-model="param.value" name="param.name">
                    </span>
                </span>
            </div>
        </div>
    </div>
    <div class="modal-footer">
        <p class="button-left"><button class="cancel-button" ng-click="cancel()">${ ui.message('reportingui.adHocReport.modal.cancel') }</button></p>
        <p class="button-right"><button class="add-button" ng-click="ok()">${ ui.message('reportingui.adHocReport.modal.add') }</button></p>
    </div>
</script>
