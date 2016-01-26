/***********************************************************************
 * Copyright (c) 2015:
 * Istituto Nazionale di Fisica Nucleare (INFN), Italy
 * Consorzio COMETA (COMETA), Italy
 *
 * See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 * the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package it.infn.ct.futuregateway.apiserver.inframanager;

import it.infn.ct.futuregateway.apiserver.resources.Task;
import org.ogf.saga.job.JobService;

/**
 * Utility class to build a JobService.
 *
 * @author Marco Fargetta <marco.fargetta@ct.infn.it>
 */
public final class JobServiceFactory {
    /**
     * Avoid the class be instantiable.
     */
    private JobServiceFactory() { }
    /**
     * Create the JobService for the infrastructure.
     *
     * @param aTask The task requesting the JobService
     * @return The JobService
     * @throws InfrastructureException If the infrastructure cannot be used for
     * some problem in the configuration or in the infrastructure
     */
    public static JobService createJobService(final Task aTask)
            throws InfrastructureException {
        return null;
    }
}
