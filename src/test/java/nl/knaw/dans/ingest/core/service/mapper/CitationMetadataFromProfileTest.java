package nl.knaw.dans.ingest.core.service.mapper;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.AUTHOR;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.AUTHOR_NAME;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.DESCRIPTION_VALUE;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.dcmi;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.ddmProfileWithAudiences;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.ddmWithCustomProfileContent;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getCompoundMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getControlledMultiValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.getPrimitiveSingleValueField;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.mapDdmToDataset;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.readDocumentFromString;
import static nl.knaw.dans.ingest.core.service.mapper.MappingTestHelper.rootAttributes;
import static org.assertj.core.api.Assertions.assertThat;

public class CitationMetadataFromProfileTest {

    @Test
    void CIT001_should_map_title() throws ParserConfigurationException, IOException, SAXException {
        var doc = ddmWithCustomProfileContent("");

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getPrimitiveSingleValueField("citation", "title", result))
            .isEqualTo("Title of the dataset");
    }

    @Test
    void CIT005_should_map_dc_creators() throws ParserConfigurationException, IOException, SAXException {
        var doc = ddmWithCustomProfileContent(""
            + "<dc:creator>J. Bond</dc:creator>\n"
            + "<dc:creator>D. O'Seven</dc:creator>\n");

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getCompoundMultiValueField("citation", AUTHOR, result))
            .extracting(AUTHOR_NAME).extracting("value")
            .isEqualTo(List.of("J. Bond", "D. O'Seven"));
    }

    @Test
    void CIT006_should_map_authors() throws ParserConfigurationException, IOException, SAXException {
        var doc = ddmWithCustomProfileContent(""
            + "<dcx-dai:creatorDetails>\n"
            + "    <dcx-dai:author>\n"
            + "        <dcx-dai:initials>I</dcx-dai:initials>\n"
            + "        <dcx-dai:surname>Lastname</dcx-dai:surname>\n"
            + "    </dcx-dai:author>\n"
            + "</dcx-dai:creatorDetails>\n");
        // TODO affiliation, ORCID, ISNI, DAI in AuthorTest
        var result = mapDdmToDataset(doc, false, false);
        assertThat(getCompoundMultiValueField("citation", AUTHOR, result))
            .extracting(AUTHOR_NAME).extracting("value")
            .isEqualTo(List.of("I Lastname"));
    }

    @Test
    void CIT007_should_map_organizations() throws ParserConfigurationException, IOException, SAXException {
        var doc = ddmWithCustomProfileContent(""
            + "<dcx-dai:creatorDetails>\n"
            + "    <dcx-dai:author>\n"
            + "        <dcx-dai:initials>I</dcx-dai:initials>\n"
            + "        <dcx-dai:surname>Lastname</dcx-dai:surname>\n"
            + "        <dcx-dai:organization>\n"
            + "            <dcx-dai:name xml:lang='en'>Example Org</dcx-dai:name>\n"
            + "        </dcx-dai:organization>\n"
            + "    </dcx-dai:author>\n"
            + "</dcx-dai:creatorDetails>\n");
        // TODO affiliation, ISNI, VIAF in AuthorTest
        var result = mapDdmToDataset(doc, false, false);
        assertThat(getCompoundMultiValueField("citation", AUTHOR, result))
            .extracting(AUTHOR_NAME).extracting("value")
            .isEqualTo(List.of("I Lastname"));
    }

    @Test
    void CIT009_should_map_description() throws ParserConfigurationException, IOException, SAXException {
        var doc = ddmWithCustomProfileContent(""
            + "<dc:description xml:lang='nl'>Lorem ipsum.</dc:description>\n"
            + "<dc:description xml:lang='dut'>dolor</dc:description>\n"
            + "<dc:description xml:lang='de'>sit amet</dc:description>\n"
        );

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getCompoundMultiValueField("citation", DESCRIPTION, result))
            .extracting(DESCRIPTION_VALUE).extracting("value")
            .isEqualTo(List.of("<p>Lorem ipsum.</p>", "<p>dolor</p>", "<p>sit amet</p>"));
    }

    @Test
    void CIT013_should_map_audience() throws ParserConfigurationException, IOException, SAXException {
        var doc = readDocumentFromString(
            "<ddm:DDM " + rootAttributes + ">\n"
                + ddmProfileWithAudiences("D19200", "D11200", "D88200", "D40200", "D17200")
                + dcmi("")
                + "</ddm:DDM>\n");

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getControlledMultiValueField("citation", "subject", result))
            .hasSameElementsAs(List.of("Astronomy and Astrophysics", "Law", "Mathematical Sciences"));
    }

    @Test
    void CIT019_should_map_creation_date() throws ParserConfigurationException, IOException, SAXException {
        var doc = ddmWithCustomProfileContent("<ddm:created>2012-12</ddm:created>");

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getPrimitiveSingleValueField("citation", "productionDate", result))
            .isEqualTo("2012-12-01");
    }

    @Test
    void CIT025_map_date_available() throws ParserConfigurationException, IOException, SAXException {
        var doc = ddmWithCustomProfileContent("<ddm:available>2014-12</ddm:available>");

        var result = mapDdmToDataset(doc, false, false);
        assertThat(getPrimitiveSingleValueField("citation", "distributionDate", result))
            .isEqualTo("2014-12-01");
    }
}
