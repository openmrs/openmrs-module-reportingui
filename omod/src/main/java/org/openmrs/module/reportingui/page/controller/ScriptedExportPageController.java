/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.reportingui.page.controller;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.dataset.definition.RowPerObjectDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.SqlDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reportingrest.adhoc.AdHocDataSet;
import org.openmrs.module.reportingrest.adhoc.AdHocExportManager;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.WebConstants;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ScriptedExportPageController {

    public void get(PageModel model) throws Exception {

    }

	public String post(@SpringBean ReportDefinitionService reportDefinitionService,
					   @SpringBean ReportService reportService,
					   UiUtils ui,
					   HttpServletRequest request,
					   @RequestParam("script") String script,
					   @RequestParam("scriptType") String scriptType) {

		ReportDefinition rd = new ReportDefinition();

		// TODO:  Handle parameters, handle more script types, save and categorize exports

		if ("sql".equals(scriptType)) {
			SqlDataSetDefinition dsd = new SqlDataSetDefinition();
			dsd.setSqlQuery(script);
			rd.addDataSetDefinition("dsd", Mapped.mapStraightThrough(dsd));
		}
		else {
			throw new IllegalArgumentException("Unsupported script type passed in");
		}

		ReportRequest rr = new ReportRequest();
		rr.setReportDefinition(Mapped.mapStraightThrough(rd));

		// TODO: We might want to check here if this exact same report request is already queued and just re-direct if so

		rr = reportService.queueReport(rr);
		reportService.processNextQueuedReports();

		return "redirect:" + ui.pageLink("reportingui", "runReport", SimpleObject.create("reportDefinition", rd.getUuid()));
	}
}
