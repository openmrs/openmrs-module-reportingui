package org.openmrs.module.reportingui.fragment.controller;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.AllPatientsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.DefinitionLibraryCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.data.patient.definition.DefinitionLibraryPatientDataDefinition;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.RowPerObjectDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.service.DataSetDefinitionService;
import org.openmrs.module.reporting.definition.library.AllDefinitionLibraries;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reportingui.adhoc.AdHocDataSet;
import org.openmrs.module.reportingui.adhoc.AdHocExportManager;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.action.FailureResult;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class AdHocAnalysisFragmentController {

    private DateTimeFormatter iso8601= ISODateTimeFormat.dateTime();

    public SimpleObject saveDataExport(@RequestParam("dataSet") String dataSetJson,
                                       @SpringBean ReportDefinitionService reportDefinitionService,
                                       @SpringBean AdHocExportManager adHocExportManager,
                                       @SpringBean AllDefinitionLibraries definitionLibraries,
                                       UiUtils ui) throws Exception {
        ObjectMapper jackson = new ObjectMapper();
        AdHocDataSet dataSet = jackson.readValue(dataSetJson, AdHocDataSet.class);

        RowPerObjectDataSetDefinition dsd = dataSet.toDataSetDefinition(adHocExportManager, definitionLibraries);

        dsd = adHocExportManager.saveAdHocDataSet(dsd);

        SimpleObject ret = SimpleObject.fromObject(dsd, ui, "uuid", "name", "description");
        ret.put("name", ((String) ret.get("name")).substring(AdHocExportManager.NAME_PREFIX.length()));
        return ret;
    }

    public Object preview(@RequestParam("rowQueries") String rowQueriesJson,
                          @RequestParam("columns") String columnsJson,
                          @RequestParam("parameterValues") String parameterValuesJson,
                          @RequestParam(value = "customCombination", required = false) String customCombination,
                          UiUtils ui,
                          @SpringBean AllDefinitionLibraries definitionLibraries,
                          @SpringBean CohortDefinitionService cohortDefinitionService,
                          @SpringBean DataSetDefinitionService dataSetDefinitionService) throws Exception {

        ObjectMapper jackson = new ObjectMapper();
        ArrayNode rowQueries = jackson.readValue(rowQueriesJson, ArrayNode.class);
        ArrayNode columns = jackson.readValue(columnsJson, ArrayNode.class);

        Map<String, Object> paramValues = parseParameterValues(jackson, parameterValuesJson);

        Result result = new Result();

        CohortDefinition composedRowQuery = null;
        if (rowQueries.size() > 0) {
            CompositionCohortDefinition composition = new CompositionCohortDefinition();
            int i = 0;
            for (JsonNode rowQuery : rowQueries) {
                // {
                //   "type":"org.openmrs.module.reporting.cohort.definition.GenderCohortDefinition",
                //   "key":"reporting.library.cohortDefinition.builtIn.males",
                //   "name":"Male patients",
                //   "description":"Patients whose gender is M",
                //   "parameters":[]
                // }
                i += 1;
                DefinitionLibraryCohortDefinition cohortDefinition = new DefinitionLibraryCohortDefinition(rowQuery.get("key").getTextValue());
                cohortDefinition.loadParameters(definitionLibraries);

                Map<String, Object> mappings = Mapped.straightThroughMappings(cohortDefinition);
                composition.addSearch("" + i, cohortDefinition, mappings);
            }

            if (StringUtils.isNotBlank(customCombination)) {
                composition.setCompositionString(customCombination);
            } else {
                composition.setCompositionString(OpenmrsUtil.join(composition.getSearches().keySet(), " AND "));
            }

            composedRowQuery = composition;
        }
        else {
            composedRowQuery = new AllPatientsCohortDefinition();
        }

        EvaluatedCohort cohort;
        try {
            cohort = cohortDefinitionService.evaluate(composedRowQuery, new EvaluationContext());
        }
        catch (Exception ex) {
            return new FailureResult(ex.getMessage());
        }
        result.setAllRows(cohort.getMemberIds());

        // for preview purposes, just get a small number of rows
        Cohort previewCohort = new Cohort();
        int j = 0;
        for (Integer member : cohort.getMemberIds()) {
            j += 1;
            previewCohort.addMember(member);
            if (j >= 10) {
                break;
            }
        }

        PatientDataSetDefinition dsd = new PatientDataSetDefinition();
        for (JsonNode column : columns) {
            // {
            //   "type":"org.openmrs.module.reporting.data.patient.definition.PatientIdDataDefinition",
            //   "key":"reporting.patientDataCalculation.patientId",
            //   "name":"reporting.patientDataCalculation.patientId.name",
            //   "description":"reporting.patientDataCalculation.patientId.description",
            //   "parameters":[]
            // }

            DefinitionLibraryPatientDataDefinition definition = new DefinitionLibraryPatientDataDefinition(column.get("key").getTextValue());
            definition.loadParameters(definitionLibraries);
            dsd.addColumn(column.get("name").getTextValue(), definition, Mapped.straightThroughMappings(definition));
        }

        EvaluationContext previewEvaluationContext = new EvaluationContext();
        previewEvaluationContext.setBaseCohort(previewCohort);
        previewEvaluationContext.setParameterValues(paramValues);

        try {
            DataSet data = dataSetDefinitionService.evaluate(dsd, previewEvaluationContext);
            result.setColumnNames(getColumnNames(data));
            result.setData(transform(data, ui));

            return result;
        }
        catch (Exception ex) {
            return new FailureResult(ex.getMessage());
        }
    }


    public Result evaluateRowQuery(@RequestParam("rowQueries") String rowQueriesJson,
                          @RequestParam("parameterValues") String parameterValuesJson,
                          @RequestParam(value = "customCombination", required = false) String customCombination,
                          @RequestParam("rowIndex") int rowIndex,
                          UiUtils ui,
                          @SpringBean AllDefinitionLibraries definitionLibraries,
                          @SpringBean CohortDefinitionService cohortDefinitionService,
                          @SpringBean DataSetDefinitionService dataSetDefinitionService) throws Exception {

        ObjectMapper jackson = new ObjectMapper();
        ArrayNode rowQueries = jackson.readValue(rowQueriesJson, ArrayNode.class);

        Map<String, Object> paramValues = parseParameterValues(jackson, parameterValuesJson);

        Result result = new Result();

        CohortDefinition composedRowQuery;
        CohortDefinition singleComposedRowQuery;

        if (rowQueries.size() > rowIndex) {
            CompositionCohortDefinition composition = new CompositionCohortDefinition();
            JsonNode rowQuery=rowQueries.get(rowIndex); // Evaluating of rowIndex element of rowQuery
            DefinitionLibraryCohortDefinition cohortDefinition = new DefinitionLibraryCohortDefinition(rowQuery.get("key").getTextValue());
            cohortDefinition.loadParameters(definitionLibraries);

            Map<String, Object> mappings = Mapped.straightThroughMappings(cohortDefinition);
            composition.addSearch("" + 1, cohortDefinition, mappings);
            composition.setCompositionString(OpenmrsUtil.join(composition.getSearches().keySet(), " AND "));
            singleComposedRowQuery=composition;

            EvaluatedCohort cohort = cohortDefinitionService.evaluate(singleComposedRowQuery, new EvaluationContext());
            result.setNumberOfResults(cohort.getMemberIds().size());
        }

        return result;
    }

    public Result evaluateWholeComposition(@RequestParam("rowQueries") String rowQueriesJson,
                                   @RequestParam("parameterValues") String parameterValuesJson,
                                   @RequestParam(value = "customCombination", required = false) String customCombination,
                                   UiUtils ui,
                                   @SpringBean AllDefinitionLibraries definitionLibraries,
                                   @SpringBean CohortDefinitionService cohortDefinitionService,
                                   @SpringBean DataSetDefinitionService dataSetDefinitionService) throws Exception {

        ObjectMapper jackson = new ObjectMapper();
        ArrayNode rowQueries = jackson.readValue(rowQueriesJson, ArrayNode.class);

        Result result = new Result();
        CohortDefinition composedRowQuery;

        if (rowQueries.size() > 0) {
            CompositionCohortDefinition composition = new CompositionCohortDefinition();
            int i = 0;
            for (JsonNode rowQuery : rowQueries) {
                i += 1;
                DefinitionLibraryCohortDefinition cohortDefinition = new DefinitionLibraryCohortDefinition(rowQuery.get("key").getTextValue());
                cohortDefinition.loadParameters(definitionLibraries);

                Map<String, Object> mappings = Mapped.straightThroughMappings(cohortDefinition);
                composition.addSearch("" + i, cohortDefinition, mappings);
            }

            if (StringUtils.isNotBlank(customCombination)) {
                composition.setCompositionString(customCombination);
            } else {
                composition.setCompositionString(OpenmrsUtil.join(composition.getSearches().keySet(), " AND "));
            }

            composedRowQuery = composition;
        }
        else {
            composedRowQuery = new AllPatientsCohortDefinition();
        }

        EvaluatedCohort cohort = cohortDefinitionService.evaluate(composedRowQuery, new EvaluationContext());
        result.setAllRows(cohort.getMemberIds());

        return result;
    }

    public SimpleObject runAdHocExport(@RequestParam("dataset") List<String> dsdUuids,
                                       @RequestParam("outputFormat") String outputFormat,
                                       //@MethodParam("getParamValues") Map<String, Object> paramValues, // UIFR-137
                                       HttpServletRequest req,
                                       @SpringBean AdHocExportManager adHocExportManager,
                                       @SpringBean
                                       ReportService reportService,
                                       UiUtils ui) throws Exception {
        if (dsdUuids.size() == 0) {
            return SimpleObject.create("error", ui.message("reportingui.adHocRun.error.noDatasets"));
        }

        RenderingMode mode = new RenderingMode((ReportRenderer) Context.loadClass(outputFormat).newInstance(), outputFormat, null, 0);

        Map<String, Object> paramValues = getParamValues(req);
        ReportRequest reportRequest = adHocExportManager.buildExportRequest(dsdUuids, paramValues, mode);
        reportRequest.setDescription("[Ad Hoc Export]");
        reportRequest = reportService.queueReport(reportRequest);
        reportService.processNextQueuedReports();

        return SimpleObject.create("uuid", reportRequest.getUuid());
    }

    /**
     * Used by runAdHocDataExport. TODO: refactor so that page can also use #parseParameterValues
     * @param request
     * @return
     */
    private Map<String, Object> getParamValues(HttpServletRequest request) {
        Map<String, Object> paramValues = new HashMap<String, Object>();
        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {
            String name = e.nextElement();
            if (name.startsWith("param[")) {
                Object value = request.getParameter(name);
                name = name.substring(name.indexOf("[") + 1, name.lastIndexOf("]"));
                try {
                    value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse((String) value);
                } catch (Exception e1) {
                    // pass
                }
                paramValues.put(name, value);
            }
        }
        return paramValues;
    }

    private Map<String, Object> parseParameterValues(ObjectMapper jackson, String json) throws IOException {
        // Expected json: { "startDate": "2013-12-01", "endDate": "2013-12-07" }
        Map<String, Object> map = jackson.readValue(json, new TypeReference<Map<String, Object>>() { });
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            try {
                entry.setValue(iso8601.parseDateTime((String) entry.getValue()).toDate());
            } catch (Exception e1) {
                // pass
            }
        }
        return map;
    }

    private List<String> getColumnNames(DataSet data) {
        List<String> list = new ArrayList<String>();
        for (DataSetColumn dataSetColumn : data.getMetaData().getColumns()) {
            list.add(dataSetColumn.getLabel());
        }
        return list;
    }

    private List transform(DataSet data, UiUtils ui) {
        List<DataSetColumn> columns = data.getMetaData().getColumns();

        List<List<String>> list = new ArrayList<List<String>>();
        for (DataSetRow row : data) {
            List<String> simpleRow = new ArrayList<String>();
            Map<DataSetColumn, Object> columnValues = row.getColumnValues();
            for (DataSetColumn column : columns) {
                simpleRow.add(ui.format(columnValues.get(column)));
            }
            list.add(simpleRow);
        }

        return list;
    }

    public class Result {

        private Set<Integer> allRows;

        public int numberOfResults;

        private List<String> columnNames;

        private List<List<String>> data;

        public Result() { }

        public void setNumberOfResults(int singleRowSize) {
            this.numberOfResults = singleRowSize;
        }

        public int getNumberOfResults() {
            return this.numberOfResults;
        }

        public Set<Integer> getAllRows() {
            return allRows;
        }

        public void setAllRows(Set<Integer> allRows) {
            this.allRows = allRows;
        }

        public List<String> getColumnNames() {
            return columnNames;
        }

        public void setColumnNames(List<String> columnNames) {
            this.columnNames = columnNames;
        }

        public List<List<String>> getData() {
            return data;
        }

        public void setData(List<List<String>> data) {
            this.data = data;
        }

    }

}
