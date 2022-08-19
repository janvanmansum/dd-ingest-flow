/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
 */

package nl.knaw.dans.ingest;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import nl.knaw.dans.ingest.core.config.DataverseConfigScala;
import nl.knaw.dans.ingest.core.config.HttpServiceConfig;
import nl.knaw.dans.ingest.core.config.IngestFlowConfig;


public class DdIngestFlowConfiguration extends Configuration {

    private IngestFlowConfig ingestFlow;
    private DataverseConfigScala dataverse;
    private HttpServiceConfig validateDansBag;
    private HttpServiceConfig managePrestaging;
    private DataSourceFactory taskEventDatabase;

    public IngestFlowConfig getIngestFlow() {
        return ingestFlow;
    }

    public void setIngestFlow(IngestFlowConfig ingestFlow) {
        this.ingestFlow = ingestFlow;
    }

    public DataverseConfigScala getDataverse() {
        return dataverse;
    }

    public void setDataverse(DataverseConfigScala dataverse) {
        this.dataverse = dataverse;
    }

    public HttpServiceConfig getValidateDansBag() {
        return validateDansBag;
    }

    public void setValidateDansBag(HttpServiceConfig validateDansBag) {
        this.validateDansBag = validateDansBag;
    }

    public DataSourceFactory getTaskEventDatabase() {
        return taskEventDatabase;
    }

    public void setTaskEventDatabase(DataSourceFactory dataSourceFactory) {
        this.taskEventDatabase = dataSourceFactory;
    }

}
