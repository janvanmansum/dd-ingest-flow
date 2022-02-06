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
package nl.knaw.dans.ingest.resources;

import nl.knaw.dans.ingest.core.ImportInbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Paths;

@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {
    private static final Logger log = LoggerFactory.getLogger(ImportResource.class);

    private final ImportInbox inbox;

    public ImportResource(ImportInbox inbox) {
        this.inbox = inbox;
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startBatch(String path) throws IOException {
        log.trace("startBatch with path = {}", path);
        try {
            inbox.startBatch(Paths.get(path));
        }
        catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return Response.accepted().build();
    }

}
