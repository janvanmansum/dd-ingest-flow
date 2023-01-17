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
package nl.knaw.dans.ingest.core.service.mapper.builder;

import org.w3c.dom.Node;

import java.util.stream.Stream;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ABR_ARTIFACT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ABR_COMPLEX;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ABR_PERIOD;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ABR_RAPPORT_NUMMER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ABR_RAPPORT_TYPE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ABR_VERWERVINGSWIJZE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ARCHIS_NUMBER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ARCHIS_ZAAK_ID;

public class ArchaeologySpecificBlock extends MetadataBlock {

    public void addArchisZaakId(Stream<String> nodes) {
        addMultiplePrimitivesString(ARCHIS_ZAAK_ID, nodes);
    }

    public void addArchisNumber(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addRepeatableCompoundValue(ARCHIS_NUMBER, stream, generator);
    }

    public void addRapportType(Stream<String> nodes) {
        addMultiplePrimitivesString(ABR_RAPPORT_TYPE, nodes);
    }

    public void addRapportNummer(Stream<String> nodes) {
        addMultiplePrimitivesString(ABR_RAPPORT_NUMMER, nodes);
    }

    public void addVerwervingswijze(Stream<String> nodes) {
        addMultiplePrimitivesString(ABR_VERWERVINGSWIJZE, nodes);
    }

    public void addComplex(Stream<String> nodes) {
        addMultiplePrimitivesString(ABR_COMPLEX, nodes);
    }

    public void addArtifact(Stream<String> nodes) {
        addMultiplePrimitivesString(ABR_ARTIFACT, nodes);
    }

    public void addPeriod(Stream<String> nodes) {
        addMultiplePrimitivesString(ABR_PERIOD, nodes);
    }

}
