// Copyright (c) YugaByte, Inc.

package com.yugabyte.yw.commissioner.tasks;

import com.yugabyte.yw.commissioner.Commissioner;
import com.yugabyte.yw.common.ModelFactory;
import com.yugabyte.yw.common.ShellProcessHandler;
import com.yugabyte.yw.forms.BackupTableParams;
import com.yugabyte.yw.models.Backup;
import com.yugabyte.yw.models.CustomerTask;
import com.yugabyte.yw.models.TaskInfo;
import com.yugabyte.yw.models.Universe;
import com.yugabyte.yw.models.helpers.TaskType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static com.yugabyte.yw.models.Backup.BackupState.Completed;
import static com.yugabyte.yw.models.Backup.BackupState.Failed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class CreateBackupTest extends CommissionerBaseTest {

  @InjectMocks
  Commissioner commissioner;

  Universe defaultUniverse;

  @Before
  public void setUp() {
    defaultCustomer = ModelFactory.testCustomer();
    defaultUniverse = ModelFactory.createUniverse();
  }

  private TaskInfo submitTask() {
    BackupTableParams backupTableParams = new BackupTableParams();
    backupTableParams.universeUUID = defaultUniverse.universeUUID;
    backupTableParams.tableName = "bar";
    backupTableParams.keyspace = "foo";
    backupTableParams.tableUUID = UUID.randomUUID();
    backupTableParams.storageConfigUUID = UUID.randomUUID();
    try {
      Backup backup = Backup.create(defaultCustomer.uuid, backupTableParams);
      UUID taskUUID = commissioner.submit(TaskType.BackupTable, backupTableParams);
      CustomerTask.create(defaultCustomer, backup.backupUUID, taskUUID, CustomerTask.TargetType.Backup,
          CustomerTask.TaskType.Create, "Create Backup");
      return waitForTask(taskUUID);
    } catch (InterruptedException e) {
      assertNull(e.getMessage());
    }
    return null;
  }

  @Test
  public void testBackupTableSuccess() {
    ShellProcessHandler.ShellResponse shellResponse =  new ShellProcessHandler.ShellResponse();
    shellResponse.message = "{\"success\": true}";
    shellResponse.code = 0;
    when(mockTableManager.createBackup(any())).thenReturn(shellResponse);

    TaskInfo taskInfo = submitTask();
    verify(mockTableManager, times(1)).createBackup(any());
    Backup backup = Backup.fetchByTaskUUID(taskInfo.getTaskUUID());
    assertNotNull(backup);
    assertEquals(Completed, backup.state);
  }

  @Test
  public void testBackupTableError() {
    ShellProcessHandler.ShellResponse shellResponse =  new ShellProcessHandler.ShellResponse();
    shellResponse.message = "{\"error\": true}";
    shellResponse.code = 0;
    when(mockTableManager.createBackup(any())).thenReturn(shellResponse);

    TaskInfo taskInfo = submitTask();
    verify(mockTableManager, times(1)).createBackup(any());
    Backup backup = Backup.fetchByTaskUUID(taskInfo.getTaskUUID());
    assertNotNull(backup);
    assertEquals(Failed, backup.state);
  }
}