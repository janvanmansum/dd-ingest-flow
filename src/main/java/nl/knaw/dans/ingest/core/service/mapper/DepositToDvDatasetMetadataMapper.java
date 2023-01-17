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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.ingest.core.service.VaultMetadata;
import nl.knaw.dans.ingest.core.service.XPathEvaluator;
import nl.knaw.dans.ingest.core.service.mapper.builder.ArchaeologySpecificBlock;
import nl.knaw.dans.ingest.core.service.mapper.builder.CitationBlock;
import nl.knaw.dans.ingest.core.service.mapper.builder.DataVaultBlock;
import nl.knaw.dans.ingest.core.service.mapper.builder.MetadataBlock;
import nl.knaw.dans.ingest.core.service.mapper.builder.RelationBlock;
import nl.knaw.dans.ingest.core.service.mapper.builder.RightsBlock;
import nl.knaw.dans.ingest.core.service.mapper.builder.TemporalSpatialBlock;
import nl.knaw.dans.ingest.core.service.exception.MissingRequiredFieldException;
import nl.knaw.dans.ingest.core.service.mapper.mapping.AbrAcquisitionMethod;
import nl.knaw.dans.ingest.core.service.mapper.mapping.AbrReportType;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Audience;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Author;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Base;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Contact;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Contributor;
import nl.knaw.dans.ingest.core.service.mapper.mapping.DatesOfCollection;
import nl.knaw.dans.ingest.core.service.mapper.mapping.DcxDaiAuthor;
import nl.knaw.dans.ingest.core.service.mapper.mapping.DcxDaiOrganization;
import nl.knaw.dans.ingest.core.service.mapper.mapping.DepositPropertiesOtherDoi;
import nl.knaw.dans.ingest.core.service.mapper.mapping.DepositPropertiesVaultMetadata;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Description;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Identifier;
import nl.knaw.dans.ingest.core.service.mapper.mapping.InCollection;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Language;
import nl.knaw.dans.ingest.core.service.mapper.mapping.PersonalStatement;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Publisher;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Relation;
import nl.knaw.dans.ingest.core.service.mapper.mapping.SpatialBox;
import nl.knaw.dans.ingest.core.service.mapper.mapping.SpatialCoverage;
import nl.knaw.dans.ingest.core.service.mapper.mapping.SpatialPoint;
import nl.knaw.dans.ingest.core.service.mapper.mapping.Subject;
import nl.knaw.dans.ingest.core.service.mapper.mapping.SubjectAbr;
import nl.knaw.dans.ingest.core.service.mapper.mapping.TemporalAbr;
import nl.knaw.dans.lib.dataverse.CompoundFieldBuilder;
import nl.knaw.dans.lib.dataverse.model.dataset.CompoundField;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import nl.knaw.dans.lib.dataverse.model.dataset.SingleValueField;
import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.KEYWORD;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.LANGUAGE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.PUBLICATION;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.RIGHTS_HOLDER;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_ABR_VERWERVINGSWIJZE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SCHEME_URI_ABR_VERWERVINGSWIJZE;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.SUBJECT;
import static nl.knaw.dans.ingest.core.service.DepositDatasetFieldNames.TITLE;

@Slf4j
public class DepositToDvDatasetMetadataMapper {

    private final Set<String> activeMetadataBlocks;

    private final CitationBlock citationFields = new CitationBlock();
    private final RightsBlock rightsFields = new RightsBlock();
    private final RelationBlock relationFields = new RelationBlock();
    private final ArchaeologySpecificBlock archaeologyFields = new ArchaeologySpecificBlock();
    private final TemporalSpatialBlock temporalSpatialFields = new TemporalSpatialBlock();
    private final DataVaultBlock dataVaultBlock = new DataVaultBlock();

    private final Map<String, String> iso1ToDataverseLanguage;
    private final Map<String, String> iso2ToDataverseLanguage;
    private final boolean deduplicate;

    DepositToDvDatasetMetadataMapper(boolean deduplicate, Set<String> activeMetadataBlocks, Map<String, String> iso1ToDataverseLanguage,
        Map<String, String> iso2ToDataverseLanguage) {
        this.deduplicate = deduplicate;
        this.activeMetadataBlocks = activeMetadataBlocks;
        this.iso1ToDataverseLanguage = iso1ToDataverseLanguage;
        this.iso2ToDataverseLanguage = iso2ToDataverseLanguage;
    }

    public Dataset toDataverseDataset(
        @NonNull Document ddm,
        @Nullable String otherDoiId,
        @Nullable Document agreements,
        @Nullable String dateOfDeposit,
        @Nullable AuthenticatedUser contactData,
        @Nullable VaultMetadata vaultMetadata
    ) throws MissingRequiredFieldException {

        if (activeMetadataBlocks.contains("citation")) {
            checkRequiredField(TITLE, getTitles(ddm));
            checkRequiredField(SUBJECT, getAudiences(ddm));

            var alternativeTitles = getAlternativeTitles(ddm).collect(Collectors.toList());
            citationFields.addTitle(getTitles(ddm));
            citationFields.addAlternativeTitle(alternativeTitles.stream().map(Node::getTextContent));

            if (vaultMetadata != null) {
                citationFields.addOtherIdsStrings(Stream.ofNullable(vaultMetadata.getOtherId())
                    .filter(DepositPropertiesVaultMetadata::isValidOtherIdValue), DepositPropertiesVaultMetadata.toOtherIdValue);
            }

            citationFields.addOtherIds(getIdentifiers(ddm).filter(Identifier::canBeMappedToOtherId), Identifier.toOtherIdValue);
            citationFields.addOtherIdsStrings(Stream.ofNullable(otherDoiId), DepositPropertiesOtherDoi.toOtherIdValue);
            citationFields.addAuthors(getCreators(ddm), Author.toAuthorValueObject);
            citationFields.addDatasetContact(Stream.ofNullable(contactData), Contact.toOtherIdValue);
            citationFields.addDescription(getProfileDescriptions(ddm), Description.toDescription);

            if (alternativeTitles.size() > 0) {
                citationFields.addDescription(Stream.of(alternativeTitles.get(alternativeTitles.size() - 1)), Description.toDescription);
            }

            citationFields.addDescription(getDcmiDctermsDescriptions(ddm), Description.toDescription);
            citationFields.addDescription(getDcmiDdmDescriptions(ddm).filter(Description::isNotMapped), Description.toDescription);

            citationFields.addDescription(getOtherDescriptions(ddm).filter(Description::isNotBlank), Description.toPrefixedDescription);

            citationFields.addSubject(getAudiences(ddm), Audience::toCitationBlockSubject);

            var subjects = getSubjects(ddm).collect(Collectors.toList());
            citationFields.addRepeatableCompoundValue(KEYWORD, subjects.stream().filter(Subject::hasNoCvAttributes), Subject.toKeywordValue);
            citationFields.addRepeatableCompoundValue(KEYWORD, subjects.stream().filter(Subject::isPanTerm), Subject.toPanKeywordValue);
            citationFields.addRepeatableCompoundValue(KEYWORD, subjects.stream().filter(Subject::isAatTerm), Subject.toAatKeywordValue);
            citationFields.addRepeatableCompoundValue(KEYWORD, subjects.stream().filter(Language::isNotIsoLanguage), Language.toKeywordValue);

            citationFields.addRepeatableCompoundValue(PUBLICATION, getIdentifiers(ddm).filter(Identifier::isRelatedPublication), Identifier.toRelatedPublicationValue);
            citationFields.addMultipleControlledFields(LANGUAGE, getLanguages(ddm).map(Language.toCitationBlockLanguage(iso1ToDataverseLanguage, iso2ToDataverseLanguage)));
            citationFields.addProductionDate(getCreated(ddm).map(Base::toYearMonthDayFormat));
            citationFields.addContributors(getContributorDetails(ddm).filter(Contributor::isValidContributor), Contributor.toContributorValueObject);
            citationFields.addContributors(getDcmiDdmDescriptions(ddm).filter(Description::hasDescriptionTypeOther), Author.toAuthorValueObject);
            citationFields.addGrantNumbers(getIdentifiers(ddm).filter(Identifier::isNwoGrantNumber), Identifier.toNwoGrantNumber);

            citationFields.addDistributor(getPublishers(ddm).filter(Publisher::isNotDans), Publisher.toDistributorValueObject);
            citationFields.addDistributionDate(getAvailable(ddm).map(Base::toYearMonthDayFormat));

            citationFields.addDateOfDeposit(dateOfDeposit);

            citationFields.addDateOfCollections(getDatesOfCollection(ddm)
                .filter(DatesOfCollection::isValidDistributorDate), DatesOfCollection.toDistributorValueObject);
            citationFields.addDataSources(getDataSources(ddm));
            citationFields.addNotesText(getProvenance(ddm));
            citationFields.addSeries(getDcmiDdmDescriptions(ddm).filter(Description::isSeriesInformation),Description.toSeries);

        }
        else {
            throw new IllegalStateException("Metadatablock citation should always be active");
        }

        if (activeMetadataBlocks.contains("dansRights")) {
            checkRequiredField(RIGHTS_HOLDER, getRightsHolders(ddm));
            rightsFields.addRightsHolders(getRightsHolders(ddm));

            rightsFields.addPersonalDataPresent(getPersonalDataPresent(agreements).map(PersonalStatement::toHasPersonalDataValue));
            rightsFields.addRightsHolders(getContributorDetailsAuthors(ddm).filter(DcxDaiAuthor::isRightsHolder).map(DcxDaiAuthor::toRightsHolder));
            rightsFields.addRightsHolders(getContributorDetailsOrganizations(ddm).filter(DcxDaiOrganization::isRightsHolder).map(DcxDaiOrganization::toRightsHolder));
            rightsFields.addLanguageOfMetadata(getLanguageAttributes(ddm)
                .map(n -> Language.isoToDataverse(n, iso1ToDataverseLanguage, iso2ToDataverseLanguage)));
        }

        if (activeMetadataBlocks.contains("dansRelationMetadata")) {
            relationFields.addAudiences(getAudiences(ddm).map(Audience::toNarcisTerm));
            relationFields.addCollections(getInCollections(ddm).map(InCollection::toCollection));
            relationFields.addRelations(getRelations(ddm)
                .filter(Relation::isRelation), Relation.toRelationObject);
        }

        if (activeMetadataBlocks.contains("dansArchaeologyMetadata")) {
            archaeologyFields.addArchisZaakId(getIdentifiers(ddm).filter(Identifier::isArchisZaakId).map(Identifier::toArchisZaakId));
            archaeologyFields.addArchisNumber(getIdentifiers(ddm).filter(Identifier::isArchisNumber), Identifier.toArchisNumberValue);
            archaeologyFields.addRapportType(getReportNumbers(ddm).filter(AbrReportType::isAbrReportType).map(AbrReportType::toAbrRapportType));
            archaeologyFields.addRapportNummer(getReportNumbers(ddm).map(Base::asText));
            archaeologyFields.addVerwervingswijze(getAcquisitionMethods(ddm).map(AbrAcquisitionMethod::toVerwervingswijze));
            archaeologyFields.addComplex(getSubjects(ddm).filter(SubjectAbr::isAbrComplex).map(SubjectAbr::toAbrComplex));
            archaeologyFields.addArtifact(getSubjects(ddm).filter(SubjectAbr::isOldAbr).map(SubjectAbr::fromAbrOldToAbrArtifact));
            archaeologyFields.addArtifact(getSubjects(ddm).filter(SubjectAbr::isAbrArtifact).map(SubjectAbr::toAbrArtifact));
            archaeologyFields.addPeriod(getSubjects(ddm).filter(TemporalAbr::isAbrPeriod).map(TemporalAbr::toAbrPeriod));
        }

        if (activeMetadataBlocks.contains("dansTemporalSpatial")) {
            temporalSpatialFields.addTemporalCoverage(getTemporal(ddm).filter(TemporalAbr::isNotAbrPeriod).map(TemporalAbr::asText));
            temporalSpatialFields.addSpatialPoint(getSpatial(ddm)
                .filter(node -> SpatialPoint.hasChildNode(node, "/Point")), SpatialPoint.toEasyTsmSpatialPointValueObject);

            temporalSpatialFields.addSpatialBox(getBoundedBy(ddm), SpatialBox.toEasyTsmSpatialBoxValueObject);
            temporalSpatialFields.addSpatialCoverageControlled(getSpatial(ddm)
                .filter(SpatialCoverage::hasNoChildElement).map(SpatialCoverage::toControlledSpatialValue));

            temporalSpatialFields.addSpatialCoverageUncontrolled(getSpatial(ddm)
                .filter(SpatialCoverage::hasNoChildElement).map(SpatialCoverage::toUncontrolledSpatialValue));
        }

        if (activeMetadataBlocks.contains("dansDataVaultMetadata") && vaultMetadata != null) {
            dataVaultBlock.addBagId(vaultMetadata.getBagId());
            dataVaultBlock.addNbn(vaultMetadata.getNbn());
            dataVaultBlock.addDansOtherId(vaultMetadata.getOtherId());
            dataVaultBlock.addDansOtherIdVersion(vaultMetadata.getOtherIdVersion());
            dataVaultBlock.addSwordToken(vaultMetadata.getSwordToken());

        }

        return assembleDataverseDataset();
    }

    private Stream<Node> getPersonalDataPresent(Document agreements) {
        if (agreements == null) {
            return Stream.empty();
        }

        return XPathEvaluator.nodes(agreements, "//agreements:personalDataStatement");
    }

    void processMetadataBlock(boolean deduplicate, Map<String, nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlock> fields, String title, String displayName, MetadataBlock builder) {
        // TODO figure out how to deduplicate compound fields (just on key, or also on value?)
        var compoundFields = builder.getCompoundFields().values()
            .stream()
            .map(this::compoundBuild);

        var primitiveFields = builder.getPrimitiveFields()
            .values()
            .stream()
            .map(b -> b.build(deduplicate));

        if (deduplicate) {
            compoundFields = compoundFields.distinct();
            primitiveFields = primitiveFields.distinct();
        }

        var result = Stream.of(compoundFields, primitiveFields)
            .flatMap(i -> i)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        var block = new nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlock();
        block.setDisplayName(displayName);
        block.setFields(result);

        fields.put(title, block);
    }

    private MetadataField compoundBuild(CompoundFieldBuilder compoundFieldBuilder) {
        // TODO rewrite in the dataverse library
        CompoundField compoundField = compoundFieldBuilder.build();
        if (compoundField.isMultiple()) {
            return compoundField;
        }
        else {
            return new MetadataField(compoundField.getTypeClass(), compoundField.getTypeName(), false) {

                public Map<String, SingleValueField> getValue() {
                    List<Map<String, SingleValueField>> value = compoundField.getValue();
                    if (value != null && !value.isEmpty()) {
                        return value.get(0);
                    }
                    else
                        return null;
                }
            };
        }
    }

    Dataset assembleDataverseDataset() {
        var fields = new HashMap<String, nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlock>();

        processMetadataBlock(deduplicate, fields, "citation", "Citation Metadata", citationFields);
        processMetadataBlock(deduplicate, fields, "dansRights", "Rights Metadata", rightsFields);
        processMetadataBlock(deduplicate, fields, "dansRelationMetadata", "Relation Metadata", relationFields);
        processMetadataBlock(deduplicate, fields, "dansArchaeologyMetadata", "Archaeology-Specific Metadata", archaeologyFields);
        processMetadataBlock(deduplicate, fields, "dansTemporalSpatial", "Temporal and Spatial Coverage", temporalSpatialFields);
        processMetadataBlock(deduplicate, fields, "dansDataVaultMetadata", "Dans Vault Metadata", dataVaultBlock);

        var version = new DatasetVersion();
        version.setTermsOfAccess("N/a"); // TODO: retrieve from input
        version.setMetadataBlocks(fields);
        version.setFiles(new ArrayList<>());

        var dataset = new Dataset();
        dataset.setDatasetVersion(version);

        if (log.isTraceEnabled()) {
            try {
                log.trace("fields: {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(fields));
            }
            catch (JsonProcessingException e) {
                log.trace("error formatting fields as json", e);
            }
        }

        return dataset;
    }

    Stream<Node> getProfileDescriptions(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:profile/dcterms:description | /ddm:DDM/ddm:profile/dc:description");
    }

    Stream<Node> getDcmiDctermsDescriptions(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:description");
    }

    Stream<Node> getDcmiDdmDescriptions(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/ddm:description");
    }

    Stream<Node> getProvenance(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:provenance");
    }

    Stream<Node> getTemporal(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/ddm:temporal");
    }

    Stream<Node> getSpatial(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:spatial");
    }

    Stream<Node> getBoundedBy(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:spatial//gml:boundedBy");
    }

    Stream<Node> getSubjects(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:subject");
    }

    Stream<Node> getLanguages(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:language");
    }

    Stream<Node> getAcquisitionMethods(Document ddm) {
        var expr = String.format(
            "/ddm:DDM/ddm:dcmiMetadata/ddm:acquisitionMethod[@subjectScheme = '%s' and @schemeURI = '%s']",
            SCHEME_ABR_VERWERVINGSWIJZE, SCHEME_URI_ABR_VERWERVINGSWIJZE
        );

        return XPathEvaluator.nodes(ddm, expr);
    }

    Stream<Node> getReportNumbers(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:dcmiMetadata/ddm:reportNumber");
    }

    Stream<Node> getRelations(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:dcmiMetadata//*");
    }

    Stream<Node> getInCollections(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:dcmiMetadata/ddm:inCollection");
    }

    Stream<String> getLanguageAttributes(Document ddm) {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile//@xml:lang | /ddm:DDM/ddm:dcmiMetadata//@xml:lang");
    }

    Stream<Node> getContributorDetailsOrganizations(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:dcmiMetadata/dcx-dai:contributorDetails/dcx-dai:organization");
    }

    Stream<Node> getContributorDetailsAuthors(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:dcmiMetadata/dcx-dai:contributorDetails/dcx-dai:author");
    }

    Stream<Node> getContributorDetails(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcx-dai:contributorDetails[dcx-dai:author] | /ddm:DDM/ddm:dcmiMetadata/dcx-dai:contributorDetails[dcx-dai:organization]");
    }

    Stream<Node> getCreated(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:profile/ddm:created");
    }

    Stream<String> getAudiences(Document ddm) {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile/ddm:audience");
    }

    Stream<Node> getIdentifiers(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier");
    }

    Stream<String> getTitles(Document ddm) {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile/dc:title");
    }

    Stream<Node> getAlternativeTitles(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:title", "/ddm:DDM/ddm:dcmiMetadata/dcterms:alternative");
    }

    Stream<Node> getCreators(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:profile/dcx-dai:creatorDetails", "/ddm:DDM/ddm:profile/dc:creator", "/ddm:DDM/ddm:profile/dcterms:creator");
    }

    Stream<Node> getOtherDescriptions(Document ddm) {
        return XPathEvaluator.nodes(ddm,
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:date",
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateAccepted",
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateCopyrighted",
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:modified",
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:issued",
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:valid",
            "/ddm:DDM/ddm:dcmiMetadata/dcterms:coverage");
    }

    Stream<Node> getPublishers(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:publisher");
    }

    Stream<Node> getAvailable(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:profile/ddm:available");
    }

    Stream<Node> getDatesOfCollection(Document ddm) {
        return XPathEvaluator.nodes(ddm, "/ddm:DDM/ddm:dcmiMetadata/ddm:datesOfCollection");
    }

    Stream<String> getDataSources(Document ddm) {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:dcmiMetadata/dc:source");
    }

    Stream<String> getRightsHolders(Document ddm) {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:dcmiMetadata/dcterms:rightsHolder");
    }

    void checkRequiredField(String fieldName, Stream<String> nodes) {
        var result = nodes
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .findFirst();

        if (result.isEmpty()) {
            throw new MissingRequiredFieldException(fieldName);
        }
    }
}
