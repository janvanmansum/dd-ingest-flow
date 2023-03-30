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
package nl.knaw.dans.ingest.core.service;

import gov.loc.repository.bagit.reader.BagReader;
import nl.knaw.dans.ingest.core.dataverse.DataverseServiceImpl;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.exception.RejectedDepositException;
import nl.knaw.dans.ingest.core.service.mapper.mapping.BaseTest;
import nl.knaw.dans.ingest.core.service.mapper.mapping.FileElement;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatasetEditorTest extends BaseTest {
    private final Path testDir = new File("target/test/" + getClass().getSimpleName()).toPath();

    @BeforeEach
    void clear() {
        FileUtils.deleteQuietly(testDir.toFile());
    }

    private DatasetEditor createDatasetEditor(Deposit deposit, final Pattern fileExclusionPattern, final List<URI> supportedLicenses) {
        return new DatasetEditor(false, null, deposit, supportedLicenses, fileExclusionPattern, null, null, Mockito.mock(DataverseServiceImpl.class)) {

            @Override
            public String performEdit() {
                return null;
            }
        };
    }

    private Deposit createDeposit(String manifest) throws Exception {
        var bagDir = testDir.resolve("bag");
        FileUtils.write(bagDir.resolve("bagit.txt").toFile(), (""
            + "BagIt-Version: 0.97\n"
            + "Tag-File-Character-Encoding: UTF-8\n"), StandardCharsets.UTF_8);
        FileUtils.write(bagDir.resolve("manifest-sha1.txt").toFile(), manifest, StandardCharsets.UTF_8);
        var deposit = new Deposit();
        deposit.setBagDir(bagDir);
        deposit.setBag(new BagReader().read(bagDir));
        return deposit;
    }

    @Test
    void getFileInfo_trims_and_filters_paths() throws Exception {
        Deposit deposit = createDeposit("");
        deposit.setDdm(readDocumentFromString("<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'/>"));
        deposit.setFilesXml(readDocumentFromString(""
            + "<files xmlns='http://easy.dans.knaw.nl/schemas/bag/metadata/files/'>"
            + "    <file filepath='data/file1.txt'/>"
            + "    <file filepath='data/subdir/file2.txt'/>"
            + "    <file filepath='data/subdir2/file3.txt'/>"
            + "</files>"));
        var fileInfoMap = FileElement.pathToFileInfo(deposit);
        var filteredFileInfoMap = createDatasetEditor(deposit, Pattern.compile(".*file2.*"), null)
            .getFileInfo();

        assertThat(fileInfoMap.keySet()).containsExactlyInAnyOrder(
            Paths.get("data/file1.txt"),
            Paths.get("data/subdir/file2.txt"),
            Paths.get("data/subdir2/file3.txt"));
        assertThat(filteredFileInfoMap.keySet()).containsExactlyInAnyOrder(
            Paths.get("file1.txt"),
            Paths.get("subdir2/file3.txt"));
    }

    @Test
    void FIL001_FIL002_FIL003() throws Exception {
        Deposit deposit = createDeposit("");
        deposit.setDdm(readDocumentFromString("<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'/>"));
        deposit.setFilesXml(readDocumentFromString(""
            + "<files xmlns='http://easy.dans.knaw.nl/schemas/bag/metadata/files/' xmlns:dcterms='http://purl.org/dc/terms/'>"
            + "    <file filepath='data/subdir_υποφάκελο/c:a*q?d&quot;l&lt;g&gt;p|s;h#.txt'/>"
            + "</files>"));
        var fileInfo = createDatasetEditor(deposit, null, null)
            .getFileInfo().values().iterator().next().getMetadata();
        assertThat(fileInfo)
            // FIL001
            .hasFieldOrPropertyWithValue("label", "c_a_q_d_l_g_p_s_h_.txt")
            // FIL002
            .hasFieldOrPropertyWithValue("directoryLabel", "subdir__________")
            // FIL003
            .hasFieldOrPropertyWithValue("description", "original_filepath: \"subdir_υποφάκελο/c:a*q?d\"l<g>p|s;h#.txt\"");
    }

    @Test
    void FIL002A_FIL003() throws Exception {
        Deposit deposit = createDeposit("");
        deposit.setDdm(readDocumentFromString("<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'/>"));
        deposit.setFilesXml(readDocumentFromString(""
            + "<files xmlns='http://easy.dans.knaw.nl/schemas/bag/metadata/files/' xmlns:afm='http://easy.dans.knaw.nl/schemas/bag/metadata/afm/'>"
            + "    <file filepath='data/subdir/#.txt'>"
            + "        <afm:keyvaluepair>"
            + "            <afm:key>FOTONR</afm:key>"
            + "            <afm:value>3</afm:value>"
            + "        </afm:keyvaluepair>"
            + "        <afm:keyvaluepair>"
            + "            <afm:key>some</afm:key>"
            + "            <afm:value>thing</afm:value>"
            + "        </afm:keyvaluepair>"
            + "    </file>"
            + "</files>"));
        var descriptions = createDatasetEditor(deposit, null, null)
            .getFileInfo().values().iterator().next().getMetadata().getDescription().split("; ");
        assertThat(descriptions).containsExactlyInAnyOrder(
            "some: \"thing\"",
            "original_filepath: \"subdir/#.txt\"",
            "FOTONR: \"3\"");
    }

    @Test
    void FIL002B_FIL003() throws Exception {
        Deposit deposit = createDeposit("");
        deposit.setDdm(readDocumentFromString("<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'/>"));
        deposit.setFilesXml(readDocumentFromString(""
            + "<files xmlns='http://easy.dans.knaw.nl/schemas/bag/metadata/files/' xmlns:afm='http://easy.dans.knaw.nl/schemas/bag/metadata/afm/'>"
            + "    <file filepath='data/subdir/#.txt'>"
            + "        <afm:othmat_codebook>FOTOBEST.csv; FOTOLST.csv</afm:othmat_codebook>"
            + "    </file>"
            + "</files>"));
        var description = createDatasetEditor(deposit, null, null)
            .getFileInfo().values().iterator().next().getMetadata().getDescription();
        assertThat(description)
            .contains("othmat_codebook: \"FOTOBEST.csv; FOTOLST.csv\"");
        assertThat(description.split("; "))
            .contains("original_filepath: \"subdir/#.txt\"");
    }

    @Test
    void FIL004_FIL003() throws Exception {
        Deposit deposit = createDeposit("");
        deposit.setDdm(readDocumentFromString("<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'/>"));
        deposit.setFilesXml(readDocumentFromString(""
            + "<files xmlns='http://easy.dans.knaw.nl/schemas/bag/metadata/files/' xmlns:dcterms='http://purl.org/dc/terms/'>"
            + "    <file filepath='data/subdir/#.txt'>"
            + "        <dcterms:description>A file with a problematic name</dcterms:description>"
            + "    </file>"
            + "</files>"));
        var fileInfo = createDatasetEditor(deposit, null, null)
            .getFileInfo().values().iterator().next();
        assertThat(fileInfo)
            .hasFieldOrPropertyWithValue("metadata.label", "_.txt")
            .hasFieldOrPropertyWithValue("metadata.description", "original_filepath: \"subdir/#.txt\"; description: \"A file with a problematic name\"");
    }

    @Test
    void FIL005_FIL009_TODO() throws Exception { // TODO break down
        Deposit deposit = createDeposit(""
            + "a5c5c4051724b655863c517a15c56e45753c3e5a  data/file1.txt\n"
            + "0d57a5bc9f5af7e8edcc90d64fd3c24dfc23e727  data/subdir/file2.txt\n"
            + "fa4cdb6b45c8a393aaca564ded8a52d62ee7a944  data/subdir_υποφάκελο/c:a*q?d\"l<g>p|s;h#.txt\n");
        deposit.setDdm(readDocumentFromString(""
            + "<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'>"
            + "    <ddm:profile>"
            + "        <ddm:accessRights>NO_ACCESS</ddm:accessRights>"
            + "        <ddm:available>2222-01-01</ddm:available>"
            + "    </ddm:profile>"
            + "</ddm:DDM>"));
        deposit.setFilesXml(readDocumentFromString(""
            + "<files xmlns='http://easy.dans.knaw.nl/schemas/bag/metadata/files/' xmlns:dcterms='http://purl.org/dc/terms/'>"
            + "    <file filepath='data/file1.txt'>"
            + "        <dcterms:description>A file with a simple description</dcterms:description>"
            + "    </file>"
            + "    <file filepath='data/subdir/file2.txt'>"
            + "        <accessibleToRights>RESTRICTED_REQUEST</accessibleToRights>"
            + "    </file>"
            + "    <file filepath='data/subdir_υποφάκελο/c:a*q?d&quot;l&lt;g&gt;p|s;h#.txt'>"
            + "        <dcterms:description>A file with a problematic name</dcterms:description>"
            + "    </file>"
            + "</files>"));
        var fileInfoMap = createDatasetEditor(deposit, null, null)
            .getFileInfo();

        var path1 = Paths.get("file1.txt");
        var path2 = Paths.get("subdir/file2.txt");
        var path3 = Paths.get("subdir_υποφάκελο/c:a*q?d\"l<g>p|s;h#.txt");
        assertThat(fileInfoMap.get(path1))
            .hasFieldOrPropertyWithValue("metadata.description", "A file with a simple description"); // FIL002B
        assertThat(fileInfoMap.get(path2))
            .hasFieldOrPropertyWithValue("checksum", "0d57a5bc9f5af7e8edcc90d64fd3c24dfc23e727")
            .hasFieldOrPropertyWithValue("path", Paths.get("target/test/DatasetEditorTest/bag/data/subdir/file2.txt"))
            .hasFieldOrPropertyWithValue("metadata.label", "file2.txt") // FIL001
            .hasFieldOrPropertyWithValue("metadata.directoryLabel", "subdir") // FIL002
            .hasFieldOrPropertyWithValue("metadata.restricted", true); // FIL006
        assertThat(fileInfoMap.get(path3))
            .hasFieldOrPropertyWithValue("checksum", "fa4cdb6b45c8a393aaca564ded8a52d62ee7a944")
            .hasFieldOrPropertyWithValue("path", Paths.get("target/test/DatasetEditorTest/bag/data/" + path3))
            .hasFieldOrPropertyWithValue("metadata.label", "c_a_q_d_l_g_p_s_h_.txt") // FIL001
            // FIL003 + FIL002A
            .hasFieldOrPropertyWithValue("metadata.description", "original_filepath: \"subdir_υποφάκελο/c:a*q?d\"l<g>p|s;h#.txt\"; description: \"A file with a problematic name\"")
            .hasFieldOrPropertyWithValue("metadata.directoryLabel", "subdir__________") // FIL002
            .hasFieldOrPropertyWithValue("metadata.restricted", true); // FIL006
    }

    @Test
    void getDateAvailable() throws Exception {
        var datasetXml = readDocumentFromString(""
            + "<ddm:DDM xmlns:ddm='http://schemas.dans.knaw.nl/dataset/ddm-v2/'>"
            + "    <ddm:profile>"
            + "        <ddm:available>2018-04-09</ddm:available>"
            + "    </ddm:profile>"
            + "</ddm:DDM>");
        var deposit = new Deposit();
        deposit.setDdm(datasetXml);
        var editor = createDatasetEditor(deposit, null, null);
        String actual = editor.getDateAvailable(deposit).toString()
            .replaceAll("T.*", "");
        // TODO date changes without time or with time around midnight, used to determine embargo files
        assertThat(actual).isEqualTo("2018-04-08");
    }

    @Test
    void getDateAvailable_at_midnight() throws Exception {
        var datasetXml = readDocumentFromString(""
            + "<ddm:DDM xmlns:ddm='http://schemas.dans.knaw.nl/dataset/ddm-v2/'>"
            + "    <ddm:profile>"
            + "        <ddm:available>2018-04-09T12:00</ddm:available>"
            + "    </ddm:profile>"
            + "</ddm:DDM>");
        var deposit = new Deposit();
        deposit.setDdm(datasetXml);
        var editor = createDatasetEditor(deposit, null, null);
        String actual = editor.getDateAvailable(deposit).toString()
            .replaceAll("T.*", "");
        assertThat(actual).isEqualTo("2018-04-09");
    }

    @Test
    void getDateAvailable_not_found() throws Exception {
        var datasetXml = readDocumentFromString(""
            + "<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'>"
            + "</ddm:DDM>");
        var deposit = new Deposit();
        deposit.setDdm(datasetXml);
        var editor = createDatasetEditor(deposit, null, null);
        assertThatThrownBy(() -> editor.getDateAvailable(deposit))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Deposit without a ddm:available element");
    }

    @Test
    void getLicense() throws Exception {
        String license = "http://opensource.org/licenses/MIT";
        var datasetXml = readDocumentFromString(""
            + "<ddm:DDM xmlns:ddm='http://schemas.dans.knaw.nl/dataset/ddm-v2/'"
            + "         xmlns:dcterms='http://purl.org/dc/terms/'"
            + "         xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'"
            + ">"
            + "    <ddm:dcmiMetadata>"
            + "        <dcterms:license xsi:type='dcterms:URI'>" + license + "</dcterms:license>"
            + "    </ddm:dcmiMetadata>"
            + "</ddm:DDM>");
        List<URI> supportedLicenses = List.of(new URI(license));
        var editor = createDatasetEditor(new Deposit(), null, supportedLicenses);
        assertThat(editor.getLicense(datasetXml)).isEqualTo(license);
    }

    @Test
    void getLicense_not_supported() throws Exception {
        var datasetXml = readDocumentFromString(""
            + "<ddm:DDM xmlns:ddm='http://schemas.dans.knaw.nl/dataset/ddm-v2/'"
            + "         xmlns:dcterms='http://purl.org/dc/terms/'"
            + "         xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'"
            + ">"
            + "    <ddm:dcmiMetadata>"
            + "        <dcterms:license xsi:type='dcterms:URI'>http://opensource.org/licenses/MIT</dcterms:license>"
            + "    </ddm:dcmiMetadata>"
            + "</ddm:DDM>");
        var editor = createDatasetEditor(new Deposit(), null, List.of());
        assertThatThrownBy(() -> editor.getLicense(datasetXml))
            .isInstanceOf(IllegalArgumentException.class) // TODO shouldn't this be RejectedDepositException?
            .hasMessage("Unsupported license: http://opensource.org/licenses/MIT");
    }

    @Test
    void getLicense_not_found() throws Exception {
        var datasetXml = readDocumentFromString(""
            + "<ddm:DDM xmlns:ddm='http://easy.dans.knaw.nl/schemas/md/ddm/'>"
            + "</ddm:DDM>");
        var editor = createDatasetEditor(new Deposit(), null, null);
        assertThatThrownBy(() -> editor.getLicense(datasetXml))
            .isInstanceOf(RejectedDepositException.class)
            .hasMessage("Rejected null: no license specified");
    }
}
