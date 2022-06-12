dd-ingest-flow
==============
Ingests DANS deposit directories into Dataverse.

SYNOPSIS
--------

    # Start server
    dd-ingest-flow { server | check }

    # Client
    ingest-flow start-import <path-to-batch>
    ingest-flow start-migration <path-to-batch>

DESCRIPTION
-----------

### Summary

The `dd-ingest-flow` service imports [DANS deposit directories](deposit-directory.md) into Dataverse. If successful, this will result in a new dataset in
Dataverse or a new version of a an existing dataset. The input deposit directories must be located in a directory on local disk storage known as
an [ingest area](#ingest-areas).

### Ingest areas

An ingest area is a directory on local disk storage that is used by the service to receive deposits. It contains the following subdirectories:

* `inbox` - the directory under which all input deposits must be located
* `outbox` - a directory where the processed deposit are moved to (if successful to a subdirectory `processed`, otherwise to one of `rejected` or `failed`)

The service supports three ingest areas:

* import - for bulk import of deposits, triggered by a data manager;
* migration - for bulk import of datasets migrated from [EASY]{:target=_blank};
* auto-ingest - for continuous import of deposits offered through deposit service, such as [dd-sword2]{:target=_blank};

### Processing of a deposit

#### Order of deposit processing

A deposit directory represents one dataset version. The version history of a datasets is represented by a sequence of deposit directories. When enqueueing
deposits the program will first order them by the timestamp in the `Created` element in the contained bag's `bag-info.txt` file.

#### Processing steps

The processing of a deposit consists of the following steps:

1. Check that the deposit is a valid [deposit directory](deposit-directory.md)
2. Check that the bag in the deposit is a valid DANS bag.
3. Map the dataset level metadata to the metadata fields expected in the target Dataverse.
4. If:
    * deposit represents first version of a dataset: create a new dataset draft.
    * deposit represents an update to an existing dataset: [draft a new version](#update-deposit)
5. Publish the new dataset-version.

#### Update-deposit

When receiving a deposit that specifies a new version for an existing dataset (an update-deposit) the assumption is that the bag contains the metadata and file
data that must be in the new version. This means:

* The metadata specified completely overwrites the metadata in the latest version. So, even if the client needs to change only one word, it must send all the
  existing metadata with only that particular word changed. Any metadata left out will be deleted.
* The files will replace the files in the latest version. So the files that are in the deposit are the ones that will be in the new version. If a file is to be
  deleted from the new version, it should simply be left out in the deposit. If a file is to remain unchanged in the new version, an exact copy of the current
  file must be sent.

!!! note "File path is key"

    The local file path (in Dataverse terms: directoryLabel + name) is used as the key to determine what file in the latest published version, if any, is targetting. 
    For example, to replace a published file with label `foo.txt` and directoryLabel `my/special/folder`, the bag must contain the new version at 
    `data/my/special/folder/foo.txt`. (Note that the directoryLabel is the path relative to the bag's `data/` folder.)

### Mapping to Dataverse dataset

!!! note "Target Dataverse variations in mapping"

    In the current version of the tool there is only one target Dataverse, and therefore only one set of
    mapping rules. This will change in the future, as the target Dataverses will be different data stations with different requirements.

#### Dataset level metadata

Per metadata block a mapping is defined from information in the deposit directory to the fields in the block. The details of these mappings are defined in
internal document, but can be found in the [DepositToDvDatasetMetadataMapper.scala]{:target=_blank} class as well.

If a block is found to be active in the targeted Dataverse instance, the mapping is executed, if the block is inactive, it is skipped.

Mappings for the following blocks have been defined:

* Citation Metadata (`citation`)
* Rights Metadata (`dansRights`)
* Relation Metadata (`dansRelationMetadata`)
* Archaeology-Specific Metadata (`dansArchaeologyMetadata`)
* Temporal and Spatial Coverage (`dansTemporalSpatial`)
* Data Vault Metadata (`dansDataVaultMetadata`)

#### Embargoes

The element `ddm:available` in `dataset.xml` contains the date when the files in the dataset should become available. If it contains a future date, all the
files in the deposit are placed under embargo, excluding the ones that have been released in a previous version. The files that have been released in a previous
version may have been placed under embargo when that version was published. However, it is not possible to change that embargo by creating a new version.

Although Dataverse allows you to set embargo at the file level, it is currently not possible to specify an embargo for specific files through the DANS deposit
APIs.

#### File level metadata

The file level metadata is derived from the deposit as follows:

From `<bag>/metadata/files.xml` the corresponding `<file>` element is looked up:

* The directory part of the `filepath` attribute is used for `directoryLabel`
* The filename part is used as the file name (i.e. `label`).
* The child elements of `<file>` are used to [determine the description attribute in Dataverse](#description-attribute)
* If an `<accessibleToRights>` element is found then the dataset's accessibility is based on the value in it:

  accessibleToRights  | Restrict?
                            --------------------|---------------------------------
  `KNOWN`             |  Yes
  `NONE`              |  Yes
  `RESTRICTED_REQUEST`|  Yes
  `ANONYMOUS`         |  No

  Otherwise the dataset's accessibility is based on the `<ddm:accessRights>` value found in
  `<bag>/metadata/dataset.xml`:

  accessRights        | Restrict?
                            --------------------|---------------------------------
  `OPEN_ACCESS_FOR_REGISTERED_USERS`|  Yes
  `NO_ACCESS`              |  Yes
  `REQUEST_PERMISSION`     |  Yes
  `OPEN_ACCESS`            |  No

##### Description attribute

In Dataverse a file has a description attribute. The value of this attribute is used to include child elements of the source `<file>` element
in `<bag>/metadata/files.xml`. These elements are displayed in the Dataverse description attribute as follows:

```
key1: "value1"; key2: "value2"; ... 
```

Multiple values for one key are separated by commas:

```
key1: "value1a", "value1b"; key2: "value2a", "value2b"; ... 
```

The source element is either

```xml

<keyvaluepair>
    <key>key</key>
    <value>value</key>
</keyvaluepair>
```

or

```xml

<key>value</key>
```

if `key` is one of the following:

* `description`
* `title`
* `hardware`
* `original_OS`
* `software`
* `notes`
* `case_quantity`
* `file_category`
* `othmat_codebook`
* `data_collector`
* `collection_date`
* `time_period`
* `geog_cover`
* `geog_unit`
* `local_georef`
* `mapprojection`
* `analytic_units`

!!! note

    * If there is only one key-value pair and its key is 'description', the value is used a the value for the decsription attribute without the aformentioned 
      formatting;
    * Any title key-value-pair that thas a value equal to the filename (case insensitively) is omitted.

#### Permission requests

In Dataverse permission requests can be enabled only at the dataset level. It is not possible to allow permission requests for one file and disallow them for
another file in the same dataset (even if the two files are in different versions). The following rule will be applied to enabled or disable permission
requests:

*If one or more files in the dataset (in any of its version) have an (effective) accessibility of `NONE`, permission requests will be disabled; otherwise they
will be enabled.*

Note that it is therefore possible for an update deposit to disable permission requests on a dataset, but not to enable them.

### Importing pre-staged files

#### Overview

Pre-staged files are files that are uploaded to the final location in storage prior to running the `import` command. For this to work the location must conform
to the storage scheme that Dataverse uses. Unfortunately this scheme does not seem to be documented.

For the migration from EASY the following procedure is envisioned:

1. Import a batch of deposits the regular way, so without prestaging files.
2. Test metadata mapping + fix problems in software.
3. Remove Dataverse, including database, but excluding files in file storage (object store or disk).
4. Reinstall Dataverse and other components.
5. Import batch again, but now with pre-staged files.

#### Mechanics

To be able to import pre-staged files `dd-dans-deposit-to-dataverse` needs some metadata about those files. This metadata can be loaded into a database
by [`dd-migration-info`](https://dans-knaw.github.io/dd-migration-info/){:target=_blank} after step 2 and then accessed in step 5
by `dd-dans-deposit-to-dataverse`.

ARGUMENTS
---------

### Server

    positional arguments:
    {server,check}         available commands
    
    named arguments:
    -h, --help             show this help message and exit
    -v, --version          show the application version and exit

### Client

TO DO

INSTALLATION AND CONFIGURATION
------------------------------
Currently this project is built as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/dd-ingest-flow` and the configuration files to `/etc/opt/dans.knaw.nl/dd-ingest-flow`. The configuration options are documented by comments
in the default configuration file `config.yml`.

To install the module on systems that do not support RPM, you can copy and unarchive the tarball to the target host. You will have to take care of placing the
files in the correct locations for your system yourself. For instructions on building the tarball, see next section.

BUILDING FROM SOURCE
--------------------
Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
* RPM

Steps:

```shell 
git clone https://github.com/DANS-KNAW/dd-ingest-flow.git
cd dd-ingest-flow
mvn clean install
```

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM packaging will be activated. If `rpm` is available, but at a
different path, then activate it by using Maven's `-P` switch: `mvn -Pprm install`.

Alternatively, to build the tarball execute:

```bash
mvn clean install assembly:single
```

[DANS BagIt Profile v1]: https://dans-knaw.github.io/dans-bagit-profile/versions/1.0.0/

[dd-sword2]: https://dans-knaw.github.io/dd-sword2/

[EASY]: https://easy.dans.knaw.nl

[DepositToDvDatasetMetadataMapper.scala]: https://github.com/DANS-KNAW/dd-ingest-flow/blob/master/src/main/scala/nl.knaw.dans.easy.dd2d/DepositToDvDatasetMetadataMapper.scala