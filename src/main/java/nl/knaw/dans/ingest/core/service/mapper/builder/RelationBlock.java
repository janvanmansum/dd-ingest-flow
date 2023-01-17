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

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.AUDIENCE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.COLLECTION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.RELATION;

public class RelationBlock extends MetadataBlock {

    public void addAudiences(Stream<String> nodes) {
        addMultiplePrimitivesString(AUDIENCE, nodes);
    }

    public void addCollections(Stream<String> values) {
        addMultiplePrimitivesString(COLLECTION, values);
    }

    public void addRelations(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addRepeatableCompoundValue(RELATION, stream, generator);
    }
}
