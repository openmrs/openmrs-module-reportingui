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

package org.openmrs.module.reportingui.converter;

import org.openmrs.Cohort;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class StringArrayToCohortConverter implements Converter<String[], Cohort> {

    /**
     * Accepts elemants like "135" and "135,203,415"
     * @param source each element must be parseable to an Integer, or else a comma-separated list of Integers
     * @return
     */
    @Override
    public Cohort convert(String[] source) {
        Cohort cohort = new Cohort();
        for (String maybeCommaSeparated : source) {
            String[] patientIds = maybeCommaSeparated.split(",");
            for (String patientId : patientIds) {
                cohort.getMemberIds().add(Integer.valueOf(patientId));
            }
        }
        return cohort;
    }

}
