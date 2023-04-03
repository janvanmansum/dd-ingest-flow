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
import nl.knaw.dans.lib.dataverse.model.dataset.SingleValueField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.ALTERNATIVE_TITLE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.CONTRIBUTOR;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.CONTRIBUTOR_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.CONTRIBUTOR_TYPE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATASET_CONTACT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATASET_CONTACT_AFFILIATION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATASET_CONTACT_EMAIL;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATASET_CONTACT_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATA_SOURCES;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATE_OF_COLLECTION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATE_OF_COLLECTION_END;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DATE_OF_COLLECTION_START;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DISTRIBUTOR;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DISTRIBUTOR_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.GRANT_NUMBER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.GRANT_NUMBER_AGENCY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.GRANT_NUMBER_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD_VOCABULARY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD_VOCABULARY_URI;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.LANGUAGE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.NOTES_TEXT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.OTHER_ID;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.OTHER_ID_AGENCY;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.OTHER_ID_VALUE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_CITATION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_ID_NUMBER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_ID_TYPE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION_URL;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SERIES;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SERIES_INFORMATION;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.dcmi;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getCompoundMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getCompoundSingleValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getControlledMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getPrimitiveMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getPrimitiveSingleValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.mapDdmToDataset;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.minimalDdmProfile;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.readDocumentFromString;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.rootAttributes;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.toPrettyJsonString;
import static org.assertj.core.api.Assertions.assertThat;

public class CitationMetadataFromDcmiTest {

    @Test
    public void CIT002_CIT010_first_title_alternatives_and_the_rest() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "        <dct:title>title 1</dct:title>"
            + "        <dct:title>title 2</dct:title>"
            + "        <dct:alternative>alt title 1</dct:alternative>"
            + "        <dct:alternative>alt title 2</dct:alternative>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);

        // CIT002 first of dcmi title/alternative
        assertThat(getPrimitiveSingleValueField("citation", ALTERNATIVE_TITLE, result))
            .isEqualTo("title 1");

        // CIT010 rest of dcmi title/alternative
        assertThat(getCompoundMultiValueField("citation", DESCRIPTION, result))
            .extracting(DESCRIPTION_VALUE).extracting("value")
            .containsExactlyInAnyOrder("<p>title 2</p>", "<p>alt title 1</p>", "<p>alt title 2</p>");
    }
    @Test
    public void CIT002_alt_title_first() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "        <dct:alternative>alt title 1</dct:alternative>"
            + "        <dct:title>title 1</dct:title>"
            + "        <dct:title>title 2</dct:title>"
            + "        <dct:alternative>alt title 2</dct:alternative>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);

        // CIT002 first of dcmi title/alternative
        assertThat(getPrimitiveSingleValueField("citation", ALTERNATIVE_TITLE, result))
            .isEqualTo("alt title 1");

        // CIT010 rest of dcmi title/alternative
        assertThat(getCompoundMultiValueField("citation", DESCRIPTION, result))
            .extracting(DESCRIPTION_VALUE).extracting("value")
            .containsExactlyInAnyOrder("<p>title 1</p>", "<p>title 2</p>", "<p>title 1</p>", "<p>alt title 2</p>");
    }

    @Test
    public void CIT002A_4_other_id() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "<dct:identifier xsi:type='id-type:EASY2'>easy-dataset:123</dct:identifier>"
            + "<dct:identifier>typeless:123</dct:identifier>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        var field = getCompoundMultiValueField("citation", OTHER_ID, result);
        assertThat(field).hasSize(4);

        // CIT002A from vault metadata
        assertThat(field).extracting(OTHER_ID_AGENCY).extracting("value")
            .contains("otherId");
        assertThat(field).extracting(OTHER_ID_VALUE).extracting("value")
            .contains("something");

        // CIT002B from @type="EASY2"
        assertThat(field).extracting(OTHER_ID_AGENCY).extracting("value")
            .contains("DANS-KNAW");
        assertThat(field).extracting(OTHER_ID_VALUE).extracting("value")
            .contains("easy-dataset:123");

        // CIT003 from bag-info.txt Has-Organizational-Identifier
        assertThat(field).extracting(OTHER_ID_AGENCY).extracting("value")
            .contains("doi");
        assertThat(field).extracting(OTHER_ID_VALUE).extracting("value")
            .contains("typeless:123");

        // CIT004
        assertThat(field).extracting(OTHER_ID_AGENCY).extracting("value")
            .contains("");
        assertThat(field).extracting(OTHER_ID_VALUE).extracting("value")
            .contains("10.12345/678");
    }

    @Test
    public void CIT008_contact() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi("")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        var field = getCompoundMultiValueField("citation", DATASET_CONTACT, result);
        assertThat(field).extracting(DATASET_CONTACT_AFFILIATION).extracting("value")
            .contains("DANS");
        assertThat(field).extracting(DATASET_CONTACT_NAME).extracting("value")
            .contains("D. O'Seven");
        assertThat(field).extracting(DATASET_CONTACT_EMAIL).extracting("value")
            .contains("J.Bond@does.not.exist.dans.knaw.nl");
    }

    @Test
    public void CIT011_dates() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "        <dct:modified>2015-09-08</dct:modified>"
            + "        <dct:date>2015-09-07</dct:date>"
            + "        <dct:dateAccepted>2015-09-06</dct:dateAccepted>"
            + "        <dct:dateCopyrighted>2015-09-05</dct:dateCopyrighted>"
            + "        <dct:issued>2015-09-04</dct:issued>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        List<Map<String, SingleValueField>> field = getCompoundMultiValueField("citation", DESCRIPTION, result);
        assertThat(field).extracting(DESCRIPTION_VALUE).extracting("value")
            .containsExactlyInAnyOrder("Issued: 2015-09-04", "Date Copyrighted: 2015-09-05", "Date Accepted: 2015-09-06", "Date: 2015-09-07", "Modified: 2015-09-08");
    }

    @Test
    public void CIT012_description() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "        <dct:description>blabla rabarbera</dct:description>"
            + "        <dct:description>pietje puck</dct:description>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        List<Map<String, SingleValueField>> field = getCompoundMultiValueField("citation", DESCRIPTION, result);
        assertThat(field).extracting(DESCRIPTION_VALUE).extracting("value")
            .containsExactlyInAnyOrder("<p>blabla rabarbera</p>", "<p>pietje puck</p>");
    }

    @Test
    void CIT012A_DctAccesRights_maps_to_description_DD_1216() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + "    <ddm:profile>"
            + "        <dc:title>Title of the dataset</dc:title>"
            + "        <dc:description>Lorem ipsum.</dc:description>"
            + "        <ddm:audience>D24000</ddm:audience>"
            + "    </ddm:profile>"
            + dcmi("<dct:accessRights>Some story</dct:accessRights>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var str = toPrettyJsonString(result);

        assertThat(str).containsOnlyOnce("<p>Some story</p>");

        var field = getCompoundMultiValueField("citation", DESCRIPTION, result);
        assertThat(field).extracting(DESCRIPTION_VALUE).extracting("value")
            .containsOnly("<p>Some story</p>", "<p>Lorem ipsum.</p>");
        assertThat(result.getDatasetVersion().getTermsOfAccess()).isEqualTo("");
    }

    @Test
    void CIT014_subject() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "  <dc:subject xml:lang='en'>metal</dc:subject>"
            + "  <dct:subject>childcare; voor- en vroegschoolse educatie; schoolloopbanen; school career; sociaal-emotionele ontwikkeling; social-emotional development; primary school; basisschool</dct:subject>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var field = getCompoundMultiValueField("citation", KEYWORD, result);
        assertThat(field).extracting(KEYWORD_VALUE).extracting("value")
            .containsOnly("metal", "childcare; voor- en vroegschoolse educatie; schoolloopbanen; school career; sociaal-emotionele ontwikkeling; social-emotional development; primary school; basisschool");
        assertThat(result.getDatasetVersion().getTermsOfAccess()).isEqualTo("");
    }

    @Test
    void CIT015_pan() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi("<ddm:subject schemeURI='https://data.cultureelerfgoed.nl/term/id/pan/PAN'"
            + "                     subjectScheme='PAN thesaurus ideaaltypes'"
            + "                     valueURI='https://data.cultureelerfgoed.nl/term/id/pan/08-01-08'"
            + "                     xml:lang='en'>non-military uniform button"
            + "        </ddm:subject>"
            + "        <ddm:subject schemeURI='https://data.cultureelerfgoed.nl/term/id/pan/PAN'"
            + "                     subjectScheme='whoops'"
            + "                     valueURI='https://data.cultureelerfgoed.nl/term/id/pan/08-01-08'"
            + "                     xml:lang='en'>non-military uniform button"
            + "        </ddm:subject>"
            + "        <ddm:subject schemeURI='http://vocab.getty.edu/aat/'"
            + "                     subjectScheme='Art and Architecture Thesaurus'"
            + "                     valueURI='http://vocab.getty.edu/aat/300239261'"
            + "                     xml:lang='en'>Broader Match: buttons (fasteners)"
            + "        </ddm:subject>"
            + "        <ddm:subject schemeURI='http://vocab.getty.edu/whoops/'"
            + "                     subjectScheme='Art and Architecture Thesaurus'"
            + "                     valueURI='http://vocab.getty.edu/aat/300239261'"
            + "                     xml:lang='en'>Broader Match: buttons (fasteners)"
            + "        </ddm:subject>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var field = getCompoundMultiValueField("citation", KEYWORD, result);
        assertThat(field).extracting(KEYWORD_VALUE).extracting("value")
            .containsOnly("non-military uniform button", "buttons (fasteners)");
        assertThat(field).extracting(KEYWORD_VOCABULARY).extracting("value")
            .containsOnly("PAN thesaurus ideaaltypes", "Art and Architecture Thesaurus");
        assertThat(field).extracting(KEYWORD_VOCABULARY_URI).extracting("value")
            .containsOnly("https://data.cultureelerfgoed.nl/term/id/pan/PAN",
                "http://vocab.getty.edu/aat/");
        // note that the whoops elements are ignored
        String jsonString = toPrettyJsonString(result);
        assertThat(jsonString).containsOnlyOnce("buttons");
        assertThat(jsonString).containsOnlyOnce("uniform");
    }

    @Test
    void CIT016_language() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi("<dct:language>gibberish</dct:language><dct:language>koeterwaals</dct:language>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var field = getCompoundMultiValueField("citation", KEYWORD, result);
        assertThat(field).extracting(KEYWORD_VALUE).extracting("value")
            .containsOnly("gibberish", "koeterwaals");
        assertThat(field).extracting(KEYWORD_VOCABULARY).extracting("value")
            .containsOnly("", "");
        assertThat(field).extracting(KEYWORD_VOCABULARY_URI).extracting("value")
            .containsOnly("", "");
    }

    @Test
    void CIT017_ISBN_ISSN() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi(""
            + "        <dc:identifier xsi:type='id-type:ISSN'>0925-6229</dc:identifier>"
            + "        <dc:identifier xsi:type='ISSN'>987-654</dc:identifier>"
            + "        <dc:identifier xsi:type='id-type:ISBN'>0-345-24223-8</dc:identifier>"
            + "        <dc:identifier xsi:type='ISBN'>978-3-16-148410-0</dc:identifier>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var field = getCompoundMultiValueField("citation", PUBLICATION, result);
        assertThat(field).extracting(PUBLICATION_ID_TYPE).extracting("value")
            .containsOnly("issn", "issn", "isbn", "isbn");
        assertThat(field).extracting(PUBLICATION_ID_NUMBER).extracting("value")
            .containsOnly("0925-6229", "987-654", "0-345-24223-8", "978-3-16-148410-0");
        assertThat(field).extracting(PUBLICATION_CITATION).extracting("value")
            .containsOnly("", "");
        assertThat(field).extracting(PUBLICATION_URL).extracting("value")
            .containsOnly("", "");
    }

    @Test
    void CIT017A_provenance_maps_to_notes_DD_1216() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi("<dct:provenance>copied xml to csv</dct:provenance>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var str = toPrettyJsonString(result);

        assertThat(str).containsOnlyOnce("copied xml to csv");
        assertThat(str).doesNotContain("<p>copied xml to csv</p>");

        assertThat(getPrimitiveSingleValueField("citation", NOTES_TEXT, result))
            .isEqualTo("copied xml to csv");
    }

    @Test
    void CIT018_language() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi(""
            + "  <ddm:language encodingScheme='ISO639-1' code='fy'>West-Fries</ddm:language>\n"
            + "  <ddm:language encodingScheme='ISO639-2' code='kal'>Groenlands</ddm:language>\n"
            + "  <ddm:language encodingScheme='ISO639-2' code='baq'>Baskisch</ddm:language>\n")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);

        assertThat(getControlledMultiValueField("citation", LANGUAGE, result))
            .containsExactlyInAnyOrder("Western Frisian", "Kalaallisut, Greenlandic", "Basque");
    }

    @Test
    void CIT018_language_is_not_iso() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi(""
            + "  <ddm:language encodingScheme='rabarber' code='baq'>Baskisch</ddm:language>\n")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false).getDatasetVersion().getMetadataBlocks();
        assertThat(result.get("dansArchaeologyMetadata").getFields()).isEmpty();
    }

    @Test
    void CIT020_contributor_author() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + " xmlns:dcx-dai='http://easy.dans.knaw.nl/schemas/dcx/dai/'>"
            + minimalDdmProfile()
            + dcmi(""
            + "    <dcx-dai:contributorDetails>"
            + "      <dcx-dai:author>"
            + "        <dcx-dai:titles>dr.</dcx-dai:titles>"
            + "        <dcx-dai:initials>M. H.</dcx-dai:initials>"
            + "        <dcx-dai:surname>van Binsbergen</dcx-dai:surname>"
            + "        <dcx-dai:role>Distributor</dcx-dai:role>"
            + "        <dcx-dai:organization>"
            + "          <dcx-dai:name>Kohnstamm Instituut</dcx-dai:name>"
            + "        </dcx-dai:organization>"
            + "       </dcx-dai:author>"
            + "     </dcx-dai:contributorDetails>"
            + "     <dcx-dai:contributorDetails>"
            + "         <dcx-dai:author>"
            + "             <dcx-dai:initials>R</dcx-dai:initials>"
            + "             <dcx-dai:surname>Smith</dcx-dai:surname>"
            + "             <dcx-dai:role>RightsHolder</dcx-dai:role>"
            + "         </dcx-dai:author>"
            + "     </dcx-dai:contributorDetails>"
            + "     <dcx-dai:contributorDetails>"
            + "         <dcx-dai:author>"
            + "             <dcx-dai:initials>A</dcx-dai:initials>"
            + "             <dcx-dai:surname>Jones</dcx-dai:surname>"
            + "             <dcx-dai:role>Funder</dcx-dai:role>"
            + "         </dcx-dai:author>"
            + "     </dcx-dai:contributorDetails>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var field = getCompoundMultiValueField("citation", CONTRIBUTOR, result);
        assertThat(field).extracting(CONTRIBUTOR_TYPE).extracting("value")
            .containsOnly("Other", "Funder"); // see CIT024 publisher becomes distributor
        assertThat(field).extracting(CONTRIBUTOR_NAME).extracting("value")
            .containsOnly("M. H. van Binsbergen (Kohnstamm Instituut)", "A Jones");
    }

    @Test
    void CIT021A_description_type_other_maps_only_to_author_name_DD_1216() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi("<ddm:description descriptionType='Other'>Author from description other</ddm:description>")
            + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var field = getCompoundMultiValueField("citation", CONTRIBUTOR, result);
        var expected = "Author from description other";
        assertThat(field).extracting(CONTRIBUTOR_NAME).extracting("value")
            .containsOnly(expected);
        // not as description and author
        assertThat(toPrettyJsonString(result)).containsOnlyOnce(expected);
    }

    @Test
    public void CIT023_nwo_project_nr() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi("<dct:identifier xsi:type='id-type:NWO-PROJECTNR'>380-60-007</dct:identifier>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        List<Map<String, SingleValueField>> field = getCompoundMultiValueField("citation", GRANT_NUMBER, result);
        assertThat(field).extracting(GRANT_NUMBER_AGENCY).extracting("value")
            .containsExactlyInAnyOrder("NWO");
        assertThat(field).extracting(GRANT_NUMBER_VALUE).extracting("value")
            .containsExactlyInAnyOrder("380-60-007");
    }

    @Test
    public void CIT024_publisher() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile()
            + dcmi("<dct:publisher>Synthegra</dct:publisher>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        List<Map<String, SingleValueField>> field = getCompoundMultiValueField("citation", DISTRIBUTOR, result);
        assertThat(field).extracting(DISTRIBUTOR_NAME).extracting("value")
            .containsExactlyInAnyOrder("Synthegra");
    }

    @Test
    public void CIT026_dates_of_collection() throws Exception {
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi(""
            + "        <ddm:datesOfCollection>2022-01-01/2022-02-01</ddm:datesOfCollection>"
            + "        <ddm:datesOfCollection>"
            + "            2021-02-01/2021-03-01"
            + "        </ddm:datesOfCollection>"
            + "        <ddm:datesOfCollection>"
            + "            2020-04-01/"
            + "        </ddm:datesOfCollection>"
            + "        <ddm:datesOfCollection>"
            + "            /2019-05-01"
            + "        </ddm:datesOfCollection>")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        List<Map<String, SingleValueField>> field = getCompoundMultiValueField("citation", DATE_OF_COLLECTION, result);
        assertThat(field).extracting(DATE_OF_COLLECTION_START).extracting("value")
            .containsExactlyInAnyOrder("2022-01-01","2021-02-01","2020-04-01","");
        assertThat(field).extracting(DATE_OF_COLLECTION_END).extracting("value")
            .containsExactlyInAnyOrder("2022-02-01","2021-03-01","2019-05-01","");
    }

    @Test
    public void CIT027_without_series_info_in_dcmi() throws Exception {
        // see also DD_1292_multiple_series_informations_to_single_compound_field in MappingIntegrationMap
        var doc = readDocumentFromString(""
            + "<ddm:DDM " + rootAttributes + ">"
            + minimalDdmProfile() + dcmi("")
            + "</ddm:DDM>");
        var result = mapDdmToDataset(doc, true);
        assertThat(getCompoundSingleValueField("citation", SERIES, result))
            .isNull();
    }

    @Test
    void CIT027_multiple_series_informations_to_single_compound_field_DD_1292() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">"
                + minimalDdmProfile()
                + "    <ddm:dcmiMetadata>"
                + "        <dct:rightsHolder>Mr. Rights</dct:rightsHolder>"
                + "        <ddm:description descriptionType='SeriesInformation'>series\n123</ddm:description>"
                + "        <ddm:description descriptionType='SeriesInformation'>another\nseries\n456</ddm:description>"
                + "    </ddm:dcmiMetadata>"
                + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);

        var field = (CompoundSingleValueField) result.getDatasetVersion().getMetadataBlocks()
            .get("citation").getFields().stream()
            .filter(f -> f.getTypeName().equals(SERIES)).findFirst().orElseThrow();
        assertThat(field.getValue())
            .extracting(SERIES_INFORMATION)
            .extracting("value")
            .isEqualTo("<p>series<br>123</p><p>another<br>series<br>456</p>");
    }

    @Test
    void CIT028_source() throws Exception {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">"
                + minimalDdmProfile()
                + dcmi(""
                + "<dc:source>LISS panel, CentERdata</dc:source>"
                + " <dc:source>General population sample survey, part of the International Social Survey Programme</dc:source>"
                + "<dct:source>HTTP</dct:source>")
                + "</ddm:DDM>");

        var result = mapDdmToDataset(doc, false);
        var field = getPrimitiveMultiValueField("citation", DATA_SOURCES, result);
        assertThat(field).containsExactlyInAnyOrder(
            "LISS panel, CentERdata",
            "General population sample survey, part of the International Social Survey Programme",
            "HTTP");
    }
}
