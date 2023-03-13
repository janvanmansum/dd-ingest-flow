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
package nl.knaw.dans.ingest.core.service.mapper;

import nl.knaw.dans.lib.dataverse.model.dataset.CompoundSingleValueField;
import org.junit.jupiter.api.Test;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.CONTRIBUTOR_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.NOTES_TEXT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SERIES;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SERIES_INFORMATION;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.dcmi;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getCompoundMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getCompoundSingleValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getPrimitiveSingleValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.mapDdmToDataset;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.minimalDdmProfile;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.readDocumentFromString;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.rootAttributes;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.toPrettyJsonString;
import static org.assertj.core.api.Assertions.assertThat;

public class CitationMetadataFromDcmiTest {

    @Test
    public void CIT002_CIT010_first_title_alternatives_and_the_rest () throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "        <dct:title>title 1</dct:title>\n"
            + "        <dct:title>title 2</dct:title>\n"
            + "        <dct:alternative>alt title 1</dct:alternative>\n"
            + "        <dct:alternative>alt title 2</dct:alternative>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);

        // CIT002 first of dcmi title/alternative
        assertThat(getPrimitiveSingleValueField("citation", "alternativeTitle", result))
            .isEqualTo("title 1");

        // CIT010 rest of dcmi title/alternative
        assertThat(getCompoundMultiValueField("citation", "dsDescription", result))
            .extracting("dsDescriptionValue").extracting("value")
            .containsExactlyInAnyOrder("<p>title 2</p>", "<p>alt title 1</p>", "<p>alt title 2</p>");
    }

    @Test
    public void CIT002A () throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi("")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        var field = getCompoundMultiValueField("citation", "otherId", result);
        assertThat(field).extracting("otherIdAgency").extracting("value")
            .containsExactlyInAnyOrder("otherId");
        assertThat(field).extracting("otherIdValue").extracting("value")
            .containsExactlyInAnyOrder("something");
    }

    @Test
    void CIT012_DctAccesRights_maps_to_description_DD_1216() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">\n"
            + "    <ddm:profile>\n"
            + "        <dc:title>Title of the dataset</dc:title>\n"
            + "        <dc:description>Lorem ipsum.</dc:description>\n"
            + "        <ddm:audience>D24000</ddm:audience>"
            + "    </ddm:profile>\n"
            + dcmi("<dct:accessRights>Some story</dct:accessRights>\n")
            + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false);
        var str = toPrettyJsonString(result);

        assertThat(str).containsOnlyOnce("<p>Some story</p>");

        var field = getCompoundMultiValueField("citation", DESCRIPTION, result);
        assertThat(field).extracting(DESCRIPTION_VALUE).extracting("value")
            .containsOnly("<p>Some story</p>", "<p>Lorem ipsum.</p>");
        assertThat(result.getDatasetVersion().getTermsOfAccess()).isEqualTo("");
    }

    @Test
    void CIT017A_provenance_maps_to_notes_DD_1216() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">\n"
            + minimalDdmProfile()
            + dcmi("<dct:provenance>copied xml to csv</dct:provenance>\n")
            + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false);
        var str = toPrettyJsonString(result);

        assertThat(str).containsOnlyOnce("copied xml to csv");
        assertThat(str).doesNotContain("<p>copied xml to csv</p>");

        assertThat(getPrimitiveSingleValueField("citation", NOTES_TEXT, result))
            .isEqualTo("copied xml to csv");
    }

    @Test
    void CIT021A_description_type_other_maps_only_to_author_name_DD_1216() throws Exception { // TODO no rule number
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">\n"
            + minimalDdmProfile()
            + dcmi("<ddm:description descriptionType='Other'>Author from description other</ddm:description>\n")
            + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false);
        var field = getCompoundMultiValueField("citation", "contributor", result);
        var expected = "Author from description other";
        assertThat(field).extracting(CONTRIBUTOR_NAME).extracting("value")
            .containsOnly(expected);
        // not as description and author
        assertThat(toPrettyJsonString(result)).containsOnlyOnce(expected);
    }

    @Test
    public void CIT027_without_series_info_in_dcmi () throws Exception {  // TODO should not produce <p></p>
        // see also DD_1292_multiple_series_informations_to_single_compound_field in MappingIntegrationMap
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi("")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        assertThat(getCompoundSingleValueField("citation", "series", result))
            .isNull();
    }

    @Test
    void CIT027_multiple_series_informations_to_single_compound_field_DD_1292() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
                + minimalDdmProfile()
                + "    <ddm:dcmiMetadata>\n"
                + "        <dct:rightsHolder>Mr. Rights</dct:rightsHolder>\n"
                + "        <ddm:description descriptionType='SeriesInformation'>series\n123</ddm:description>\n"
                + "        <ddm:description descriptionType='SeriesInformation'>another\nseries\n456</ddm:description>\n"
                + "    </ddm:dcmiMetadata>\n"
                + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false);

        var field = (CompoundSingleValueField) result.getDatasetVersion().getMetadataBlocks()
            .get("citation").getFields().stream()
            .filter(f -> f.getTypeName().equals(SERIES)).findFirst().orElseThrow();
        assertThat(field.getValue())
            .extracting(SERIES_INFORMATION )
            .extracting("value")
            .isEqualTo("<p>series<br>123</p><p>another<br>series<br>456</p>");
    }
}
