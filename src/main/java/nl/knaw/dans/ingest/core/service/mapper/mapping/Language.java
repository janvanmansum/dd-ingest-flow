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
package nl.knaw.dans.ingest.core.service.mapper.mapping;

import nl.knaw.dans.ingest.core.service.mapper.builder.CompoundFieldGenerator;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD_VOCABULARY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD_VOCABULARY_URI;

public class Language extends Base {
    public static CompoundFieldGenerator<Node> toKeywordValue = (builder, value) -> {
        builder.addSubfield(KEYWORD_VALUE, value.getTextContent());
        builder.addSubfield(KEYWORD_VOCABULARY, "");
        builder.addSubfield(KEYWORD_VOCABULARY_URI, "");
    };

    public static boolean isNotIsoLanguage(Node node) {
        return !isIsoLanguage(node);
    }

    public static boolean isIsoLanguage(Node node) {
        var isoLanguages = Set.of("ISO639-1", "ISO639-2");
        var hasTypes = hasXsiType(node, "ISO639-1") || hasXsiType(node, "ISO639-2");

        var attributes = Optional.ofNullable(node.getAttributes());
        var hasEncoding = attributes.map(a -> Optional.ofNullable(a.getNamedItem("encodingScheme")))
            .flatMap(i -> i)
            .map(Node::getTextContent)
            .map(isoLanguages::contains)
            .orElse(false);

        return hasTypes || hasEncoding;
    }

    public static Function<Node, String> toCitationBlockLanguage(Map<String, String> iso1ToDataverseLanguage, Map<String, String> iso2ToDataverseLanguage) {
        return (Node node) -> {
            if (isIsoLanguage(node)) {
                return getAttribute(node, "code")
                    .map(n -> isoToDataverse(n.getTextContent().trim(), iso1ToDataverseLanguage, iso2ToDataverseLanguage))
                    .orElse(null);
            }

            return null;
        };
    }

    public static String isoToDataverse(String code, Map<String, String> iso1ToDataverseLanguage, Map<String, String> iso2ToDataverseLanguage) {
        if (code.length() == 2) {
            return iso1ToDataverseLanguage.get(code);
        }

        return iso2ToDataverseLanguage.get(code);
    }
}
