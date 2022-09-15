package nl.knaw.dans.ingest.core.service;

import nl.knaw.dans.ingest.core.legacy.DepositIngestTaskFactoryWrapper;

import java.nio.file.Path;

public class SingleDepositImportTaskIterator extends AbstractDepositsImportTaskIterator {
    public SingleDepositImportTaskIterator(Path deposit, Path outBox, DepositIngestTaskFactoryWrapper taskFactory,
        EventWriter eventWriter) {
        super(null, outBox, taskFactory, eventWriter);
        addTaskForDeposit(deposit);
    }
}
