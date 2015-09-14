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

import org.openmrs.api.context.Context;
import org.openmrs.module.reportingui.adhoc.AdHocExportManager;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class AdHocRunPageController {

    public void get(@SpringBean AdHocExportManager adHocExportManager,
                    @RequestParam("dataset") List<String> dsdUuids,
                    PageModel model) {
        List<AdHocExportManager.AdHocDataSet> exports = adHocExportManager.getAdHocDataSets(Context.getAuthenticatedUser());
        model.addAttribute("exports", exports);
        model.addAttribute("selected", dsdUuids);
    }

}
