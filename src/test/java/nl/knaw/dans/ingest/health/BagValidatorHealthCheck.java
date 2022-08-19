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
package nl.knaw.dans.ingest.health;

import com.codahale.metrics.health.HealthCheck.Result;
import nl.knaw.dans.easy.dd2d.dansbag.DansBagValidator;
import nl.knaw.dans.ingest.core.legacy.DepositIngestTaskFactoryWrapper;
import nl.knaw.dans.ingest.health.DansBagValidatorHealthCheck;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class BagValidatorHealthCheck {

    @Test
    void checkDansBagValidatorAvailable() {
        DepositIngestTaskFactoryWrapper depositIngestTaskFactoryWrapper = Mockito.mock(DepositIngestTaskFactoryWrapper.class);
        DansBagValidator dansBagValidator = Mockito.mock(DansBagValidator.class, RETURNS_DEEP_STUBS);
        Mockito.when(dansBagValidator.checkConnection().isSuccess()).thenReturn(true);
        Mockito.when(depositIngestTaskFactoryWrapper.getDansBagValidatorInstance()).thenReturn(dansBagValidator);
        Result result = new DansBagValidatorHealthCheck(depositIngestTaskFactoryWrapper.getDansBagValidatorInstance()).check();
        assertTrue(result.isHealthy());
    }

    @Test
    void checkDansBagValidatorNotAvailable() {
        DepositIngestTaskFactoryWrapper depositIngestTaskFactoryWrapper = Mockito.mock(DepositIngestTaskFactoryWrapper.class);
        DansBagValidator dansBagValidator = Mockito.mock(DansBagValidator.class, RETURNS_DEEP_STUBS);
        Mockito.when(dansBagValidator.checkConnection().isSuccess()).thenReturn(false);
        Mockito.when(depositIngestTaskFactoryWrapper.getDansBagValidatorInstance()).thenReturn(dansBagValidator);
        Result result = new DansBagValidatorHealthCheck(depositIngestTaskFactoryWrapper.getDansBagValidatorInstance()).check();
        assertFalse(result.isHealthy());
    }
}
