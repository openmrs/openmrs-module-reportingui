package org.openmrs.module.reportingui.fragment.controller;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.definition.RowPerObjectDataSetDefinition;
import org.openmrs.module.reporting.definition.library.AllDefinitionLibraries;
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
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private boolean notNull(JsonNode node) {
        return node != null && !node.isMissingNode() && !node.isNull();
    }

    private Object parseParameterValue(JsonNode param) throws Exception {
        // param looks like
        // {
        //   "name":"effectiveDate",
        //   "type":"java.util.Date",
        //   "collectionType":null,
        //   "value":"2013-04-03T04:00:00.000Z"
        // }
        if (notNull(param.get("collectionType"))) {
            throw new IllegalStateException("collection parameters are not yet implemented");
        }
        Class<?> clazz = Context.loadClass(param.get("type").getTextValue());
        if (Date.class.equals(clazz)) {
            return new SimpleDateFormat("yyyy-MM-dd").parse(param.get("value").getValueAsText());
        }
        else {
            throw new IllegalStateException("type " + clazz.getName() + " is not yet implemented");
        }
    }

    public SimpleObject runAdHocExport(@RequestParam("dataset") List<String> dsdUuids,
                                       @RequestParam("outputFormat") String outputFormat,
                                       //@MethodParam("getParamValues") Map<String, Object> paramValues, // UIFR-137
                                       HttpServletRequest req,
                                       @SpringBean AdHocExportManager adHocExportManager,
                                       @SpringBean ReportService reportService,
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

        private List<String> columnNames;

        private List<List<String>> data;

        public Result() { }

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
