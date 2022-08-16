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
package nl.knaw.dans.easy.dd2d

import better.files.File
import nl.knaw.dans.easy.dd2d.OutboxSubdir.{ FAILED, OutboxSubdir, PROCESSED, REJECTED }
import nl.knaw.dans.easy.dd2d.dansbag.InformationPackageType.InformationPackageType
import nl.knaw.dans.easy.dd2d.dansbag.{ DansBagValidationResult, DansBagValidator, InformationPackageType }
import nl.knaw.dans.easy.dd2d.mapping.FieldMap
import nl.knaw.dans.lib.dataverse.DataverseClient
import nl.knaw.dans.lib.dataverse.model.dataset
import nl.knaw.dans.lib.dataverse.model.dataset.UpdateType.major
import nl.knaw.dans.lib.dataverse.model.dataset.{ Dataset, PrimitiveSingleValueField }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.taskqueue.Task
import org.json4s.native.Serialization
import org.json4s.{ DefaultFormats, Formats }

import java.lang.Thread.sleep
import java.net.URI
import java.util.regex.Pattern
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node }

/**
 * Checks one deposit and then ingests it into Dataverse.
 *
 * @param deposit  the deposit to ingest
 */
case class DepositIngestTask(deposit: Deposit,
                             optFileExclusionPattern: Option[Pattern],
                             zipFileHandler: ZipFileHandler,
                             depositorRole: String,
                             deduplicate: Boolean,
                             activeMetadataBlocks: List[String],
                             optDansBagValidator: Option[DansBagValidator],
                             dataverseClient: DataverseClient,
                             publishAwaitUnlockMaxNumberOfRetries: Int,
                             publishAwaitUnlockMillisecondsBetweenRetries: Int,
                             narcisClassification: Elem,
                             iso1ToDataverseLanguage: Map[String, String],
                             iso2ToDataverseLanguage: Map[String, String],
                             variantToLicense: Map[String, String],
                             supportedLicenses: List[URI],
                             repordIdToTerm: Map[String, String],
                             outboxDir: File) extends Task[Deposit] with DebugEnhancedLogging {
  trace(deposit)
  protected val informationPackageType: InformationPackageType = InformationPackageType.SIP
  protected val bagProfileVersion: Int = 1

  private val datasetMetadataMapper = new DepositToDvDatasetMetadataMapper(deduplicate, activeMetadataBlocks, narcisClassification, iso1ToDataverseLanguage, iso2ToDataverseLanguage, repordIdToTerm)
  private val bagDirPath = File(deposit.bagDir.path)

  override def run(): Try[Unit] = {
    doRun()
      .doIfSuccess(_ => {
        logger.info(s"SUCCESS: $deposit")
        deposit.setState("ARCHIVED", "The deposit was successfully ingested in the Data Station and will be automatically archived")
        moveDepositToOutbox(PROCESSED)
      })
      .doIfFailure {
        case e: RejectedDepositException =>
          logger.info(s"REJECTED: $deposit", e)
          deposit.setState("REJECTED", e.msg)
          moveDepositToOutbox(REJECTED)
        case e =>
          logger.info(s"FAILED: $deposit", e)
          deposit.setState("FAILED", e.getMessage)
          moveDepositToOutbox(FAILED)
      }
  }

  private def moveDepositToOutbox(subDir: OutboxSubdir): Unit = {
    try {
      deposit.dir.moveToDirectory(outboxDir / subDir.toString)
    } catch {
      case NonFatal(e) => logger.info(s"Failed to move deposit: $deposit to ${ outboxDir / subDir.toString }", e)
    }
  }

  private def doRun(): Try[Unit] = {
    trace(())
    logger.info(s"Ingesting $deposit into Dataverse")
    for {
      _ <- checkDepositType()
      _ <- validateDeposit()
      dataverseDataset <- getMetadata
      isUpdate <- deposit.isUpdate
      _ = debug(s"isUpdate? = $isUpdate")
      editor = if (isUpdate) newDatasetUpdater(dataverseDataset)
               else newDatasetCreator(dataverseDataset, depositorRole)
      persistentId <- editor.performEdit()
      _ <- publishDataset(persistentId)
      _ <- postPublication(persistentId)
    } yield ()
  }

  protected def checkDepositType(): Try[Unit] = {
    trace(())
    if (deposit.doi.nonEmpty) Failure(new IllegalArgumentException("Deposits must not have an identifier.doi property unless they are migrated"))
    else Success(())
  }

  private def validateDeposit(): Try[Unit] = {
    trace(())
    optDansBagValidator.map {
      dansBagValidator =>
        for {
          validationResult <- dansBagValidator.validateBag(bagDirPath, informationPackageType, bagProfileVersion)
          _ <- rejectIfInvalid(validationResult)
        } yield ()
    }.getOrElse({
      debug("Skipping validation")
      Success(())
    })
  }

  private def rejectIfInvalid(validationResult: DansBagValidationResult): Try[Unit] = Try {
    if (!validationResult.isCompliant) throw RejectedDepositException(deposit,
      s"""
         |Bag was not valid according to Profile Version ${ validationResult.profileVersion }.
         |Violations:
         |${ validationResult.ruleViolations.map(_.map(formatViolation).mkString("\n")).getOrElse("") }
                      """.stripMargin)
  }

  private def formatViolation(v: (String, String)): String = v match {
    case (nr, msg) => s" - [$nr] $msg"
  }

  private def getMetadata: Try[Dataset] = {
    trace(())
    for {
      optDateOfDeposit <- getDateOfDeposit
      datasetContacts <- getDatasetContacts
      ddm <- deposit.tryDdm
      optAgreements <- deposit.tryOptAgreementsXml
      _ <- checkPersonalDataPresent(optAgreements)
      dataverseDataset <- datasetMetadataMapper.toDataverseDataset(ddm, deposit.getOptOtherDoiId, optAgreements, optDateOfDeposit, datasetContacts, deposit.vaultMetadata)
    } yield dataverseDataset
  }

  /*
   * See DD-901. For non-migration imports we will accept missing agreement.xml for now
   */
  protected def checkPersonalDataPresent(optAgreements: Option[Node]): Try[Unit] = {
    Success(())
  }

  protected def getDateOfDeposit: Try[Option[String]] = Try {
    Option.empty
  }

  private def getDatasetContacts: Try[List[FieldMap]] = {
    logger.trace("DepositIngestTask.getDatasetContacts")
    for {
      response <- Try(dataverseClient.admin().listSingleUser(deposit.depositorUserId))
      user <- Try(response.getData)
      datasetContacts <- createDatasetContacts(user.getDisplayName, user.getEmail, Option(user.getAffiliation))
    } yield datasetContacts
  }

  private def createDatasetContacts(name: String, email: String, optAffiliation: Option[String] = None): Try[List[FieldMap]] = Try {
    val subfields = ListBuffer[PrimitiveSingleValueField]()
    subfields.append(new PrimitiveSingleValueField("datasetContactName", name))
    subfields.append(new PrimitiveSingleValueField("datasetContactEmail", email))
    optAffiliation.foreach(affiliation => subfields.append(new PrimitiveSingleValueField("datasetContactAffiliation", affiliation)))

    List(toFieldMap(subfields: _*))
  }

  protected def newDatasetUpdater(dataverseDataset: Dataset): DatasetUpdater = {
    new DatasetUpdater(deposit, optFileExclusionPattern, zipFileHandler, isMigration = false, dataverseDataset.getDatasetVersion.getMetadataBlocks, variantToLicense, supportedLicenses, dataverseClient)
  }

  protected def newDatasetCreator(dataverseDataset: Dataset, depositorRole: String): DatasetCreator = {
    new DatasetCreator(deposit, optFileExclusionPattern, zipFileHandler, depositorRole, isMigration = false, dataverseDataset, variantToLicense, supportedLicenses, dataverseClient)
  }

  protected def publishDataset(persistentId: String): Try[Unit] = {
    trace(persistentId)
    for {
      _ <- Try(dataverseClient.dataset(persistentId).publish(major, true))
      _ <- Try(dataverseClient.dataset(persistentId).awaitUnlock(
        publishAwaitUnlockMaxNumberOfRetries,
        publishAwaitUnlockMillisecondsBetweenRetries))
    } yield ()
  }

  protected def postPublication(persistentId: String): Try[Unit] = {
    trace(persistentId)
    for {
      _ <- waitForReleasedState(persistentId)
      _ <- savePersistentIdentifiersInDepositProperties(persistentId)
    } yield ()
  }

  private def waitForReleasedState(persistentId: String): Try[Unit] = {
    trace(persistentId)
    var numberOfTimesTried = 0

    def getDatasetState: Try[String] = {
      for {
        response <- Try(dataverseClient.dataset(persistentId).getLatestVersion)
        ds <- Try(response.getData)
        state = ds.getLatestVersion.getVersionState
      } yield state
    }

    def slept(): Boolean = {
      debug(s"Sleeping $publishAwaitUnlockMillisecondsBetweenRetries ms before next try..") // TODO: replace with dedicated settings for waiting for pub.
      sleep(publishAwaitUnlockMillisecondsBetweenRetries)
      true
    }

    var maybeState = getDatasetState
    do {
      maybeState = getDatasetState
      numberOfTimesTried += 1
    } while (maybeState.isSuccess && maybeState.get != "RELEASED" && numberOfTimesTried != publishAwaitUnlockMaxNumberOfRetries && slept())

    if (maybeState.isFailure) maybeState.map(_ => ())
    else if (maybeState.get != "RELEASED") Failure(FailedDepositException(deposit, "Dataset did not become RELEASED within the wait period"))
         else Success(())
  }

  private def savePersistentIdentifiersInDepositProperties(persistentId: String): Try[Unit] = {
    logger.trace("ENTER")
    implicit val jsonFormats: Formats = DefaultFormats
    for {
      _ <- Try(dataverseClient.dataset(persistentId).awaitUnlock())
      _ = debug(s"Dataset $persistentId is not locked")
      _ <- deposit.setDoi(persistentId)
      r <- Try(dataverseClient.dataset(persistentId).getVersion)
      _ = if (logger.underlying.isDebugEnabled) debug(Serialization.writePretty(r.getEnvelopeAsJson))
      d <- Try(r.getData)
      v = d.getMetadataBlocks.get("dansDataVaultMetadata")
      optUrn = v.getFields.find("dansNbn" == _.getTypeName)
        .map(_.asInstanceOf[dataset.PrimitiveSingleValueField].getValue)
      _ = if (optUrn.isEmpty) throw new IllegalStateException(s"Dataset $persistentId did not obtain a URN:NBN")
      _ <- deposit.setUrn(optUrn.get)
    } yield ()
  }

  override def getTarget: Deposit = {
    deposit
  }

  override def toString: DepositName = {
    s"DepositIngestTask for $deposit"
  }
}
