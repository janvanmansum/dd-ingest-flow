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

import java.util.Objects;
import java.util.stream.Stream;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.LANGUAGE_OF_METADATA;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PERSONAL_DATA_PRESENT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.RIGHTS_HOLDER;

public class RightsBlock extends MetadataBlock {

    public void addRightsHolders(Stream<String> nodes) {
        addMultiplePrimitivesString(RIGHTS_HOLDER, nodes);
    }

    public void addPersonalDataPresent(Stream<String> values) {
        var v = values.filter(Objects::nonNull).findFirst().orElse("Unknown");
        addSingleControlledField(PERSONAL_DATA_PRESENT, v);
    }

    public void addLanguageOfMetadata(Stream<String> stream) {
        addMultipleControlledFields(LANGUAGE_OF_METADATA, stream);
    }
}
