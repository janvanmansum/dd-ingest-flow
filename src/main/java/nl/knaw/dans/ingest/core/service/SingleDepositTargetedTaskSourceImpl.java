package nl.knaw.dans.ingest.core.service;

import nl.knaw.dans.ingest.core.legacy.DepositImportTaskWrapper;
import nl.knaw.dans.ingest.core.legacy.DepositIngestTaskFactoryWrapper;

import java.nio.file.Path;
import java.util.Iterator;

public class SingleDepositTargetedTaskSourceImpl implements TargetedTaskSource<DepositImportTaskWrapper> {
    private final Path deposit;

    private final Path outbox;

    private final DepositIngestTaskFactoryWrapper taskFactory;

    private final EventWriter eventWriter;

    public SingleDepositTargetedTaskSourceImpl(Path deposit, Path outbox, TaskEventService taskEventService, DepositIngestTaskFactoryWrapper taskFactory) {
        this.deposit = deposit;
        this.outbox = outbox;
        this.taskFactory = taskFactory;
        this.eventWriter = new EventWriter(taskEventService, deposit.getFileName().toString());
    }

    @Override
    public Iterator<DepositImportTaskWrapper> iterator() {
        return new SingleDepositImportTaskIterator(deposit, outbox, taskFactory, eventWriter);
    }
}
