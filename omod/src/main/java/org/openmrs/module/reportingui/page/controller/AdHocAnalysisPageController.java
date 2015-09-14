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
import org.openmrs.module.reportingui.adhoc.AdHocDataSet;
import org.openmrs.module.reportingui.adhoc.AdHocExportManager;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

public class AdHocAnalysisPageController {

    public void get(@RequestParam(value = "definition", required = false) String uuid,
                    @RequestParam(value = "definitionClass", required = false) String definitionClass,
                    @SpringBean AdHocExportManager adHocExportManager,
                    PageModel model) throws Exception {

        RowPerObjectDataSetDefinition definition = null;
        String initialStateJson = null;
        if (StringUtils.isNotBlank(uuid)) {
            definition = adHocExportManager.getAdHocDataSetByUuid(uuid);

            AdHocDataSet ds = new AdHocDataSet(definition);

            ObjectMapper jackson = new ObjectMapper();
            initialStateJson = jackson.writeValueAsString(ds);

        }
        else if (StringUtils.isNotBlank(definitionClass)) {
            Class<?> clazz = Context.loadClass(definitionClass);
            definition = (RowPerObjectDataSetDefinition) clazz.newInstance();
            definition.setUuid(null);
        }
        else {
            throw new IllegalArgumentException("definition or definitionClass is required");
        }
        model.addAttribute("definition", definition);
        model.addAttribute("initialStateJson", initialStateJson);
    }

}
