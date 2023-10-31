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
package nl.knaw.dans.ingest.core.validation;

import nl.knaw.dans.ingest.core.dataverse.DatasetService;
import nl.knaw.dans.ingest.core.domain.Deposit;
import nl.knaw.dans.ingest.core.exception.DepositorValidatorException;
import nl.knaw.dans.ingest.core.exception.InvalidDepositorRoleException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DepositorAuthorizationValidatorImplTest {

    @Test
    void isDatasetPublicationAllowed_should_return_true_when_publisher_role_matches_for_new_dataset() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDataverseRoleAssignments(Mockito.eq("user001")))
            .thenReturn(List.of("admin", "publisher"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(false);

        assertThat(validator.isDatasetPublicationAllowed(deposit)).isTrue();
    }


    @Test
    void isDatasetPublicationAllowed_should_return_true_when_publisher_role_matches_for_dataset_update() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDataverseRoleAssignments(Mockito.eq("user001")))
            .thenReturn(List.of("admin", "publisher"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(true);

        assertThat(validator.isDatasetPublicationAllowed(deposit)).isTrue();
    }

    @Test
    void isDatasetPublicationAllowed_should_return_false_when_publisher_role_does_not_match_for_new_dataset() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDataverseRoleAssignments(Mockito.eq("user001")))
            .thenReturn(List.of("admin", "NOT_publisher"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(false);

        assertThat(validator.isDatasetPublicationAllowed(deposit)).isFalse();
    }

    @Test
    void isDatasetPublicationAllowed_should_return_false_when_publisher_role_does_not_match_for_dataset_update() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDataverseRoleAssignments(Mockito.eq("user001")))
            .thenReturn(List.of("admin", "NOT_publisher"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(true);

        assertThat(validator.isDatasetPublicationAllowed(deposit)).isFalse();
    }

    @Test
    void isDatasetPublicationAllowed_should_throw_when_not_able_to_retrieve_roles() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDataverseRoleAssignments(Mockito.eq("user001")))
            .thenThrow(new IOException("test"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(true);

        assertThrows(DepositorValidatorException.class, () -> validator.isDatasetPublicationAllowed(deposit));
    }

    @Test
    void isDatasetUpdateAllowed_should_return_true_when_updater_role_matches_for_dataset_update() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDatasetRoleAssignments(Mockito.eq("user001"), Mockito.eq("doi:123")))
            .thenReturn(List.of("admin", "updater"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(true);
        deposit.setDataverseDoi("doi:123");

        assertThat(validator.isDatasetUpdateAllowed(deposit)).isTrue();
    }

    @Test
    void isDatasetUpdateAllowed_should_return_false_when_updater_role_does_not_match_for_dataset_update() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDatasetRoleAssignments(Mockito.eq("user001"), Mockito.eq("doi:123")))
            .thenReturn(List.of("admin", "NOT_updater"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(true);
        deposit.setDataverseDoi("doi:123");

        assertThat(validator.isDatasetUpdateAllowed(deposit)).isFalse();
    }

    @Test
    void isDatasetUpdateAllowed_should_throw_when_not_able_to_retrieve_roles() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);
        Mockito.when(datasetService.getDatasetRoleAssignments(Mockito.eq("user001"), Mockito.eq("doi:123")))
            .thenThrow(new IOException("test"));

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(true);
        deposit.setDataverseDoi("doi:123");

        assertThrows(DepositorValidatorException.class, () -> validator.isDatasetUpdateAllowed(deposit));
    }

    @Test
    void isDatasetUpdateAllowed_should_throw_when_deposit_is_not_an_update() throws Exception {
        var datasetService = Mockito.mock(DatasetService.class);

        var validator = new DepositorAuthorizationValidatorImpl(datasetService, "publisher", "updater");

        var deposit = new Deposit();
        deposit.setDepositorUserId("user001");
        deposit.setUpdate(false);
        deposit.setDataverseDoi("doi:123");

        assertThrows(DepositorValidatorException.class, () -> validator.isDatasetUpdateAllowed(deposit));
    }


}