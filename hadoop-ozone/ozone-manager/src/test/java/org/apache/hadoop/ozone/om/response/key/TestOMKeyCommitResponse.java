/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ozone.om.response.key;

import org.apache.hadoop.ozone.om.helpers.OmVolumeArgs;
import org.apache.hadoop.util.Time;
import org.junit.Assert;
import org.junit.Test;

import org.apache.hadoop.ozone.om.helpers.OmKeyInfo;
import org.apache.hadoop.ozone.om.request.TestOMRequestUtils;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos;

/**
 * Tests OMKeyCommitResponse.
 */
public class TestOMKeyCommitResponse extends TestOMKeyResponse {

  @Test
  public void testAddToDBBatch() throws Exception {

    OmKeyInfo omKeyInfo = TestOMRequestUtils.createOmKeyInfo(volumeName,
        bucketName, keyName, replicationType, replicationFactor);
    OmVolumeArgs omVolumeArgs = OmVolumeArgs.newBuilder()
        .setOwnerName(keyName).setAdminName(keyName)
        .setVolume(volumeName).setCreationTime(Time.now()).build();

    OzoneManagerProtocolProtos.OMResponse omResponse =
        OzoneManagerProtocolProtos.OMResponse.newBuilder().setCommitKeyResponse(
            OzoneManagerProtocolProtos.CommitKeyResponse.getDefaultInstance())
            .setStatus(OzoneManagerProtocolProtos.Status.OK)
            .setCmdType(OzoneManagerProtocolProtos.Type.CommitKey)
            .build();

    // As during commit Key, entry will be already there in openKeyTable.
    // Adding it here.
    TestOMRequestUtils.addKeyToTable(true, volumeName, bucketName, keyName,
        clientID, replicationType, replicationFactor, omMetadataManager);

    String openKey = omMetadataManager.getOpenKey(volumeName, bucketName,
        keyName, clientID);
    Assert.assertTrue(omMetadataManager.getOpenKeyTable().isExist(openKey));

    String ozoneKey = omMetadataManager.getOzoneKey(volumeName, bucketName,
        keyName);
    OMKeyCommitResponse omKeyCommitResponse = new OMKeyCommitResponse(
        omResponse, omKeyInfo, ozoneKey, openKey, omVolumeArgs);

    omKeyCommitResponse.addToDBBatch(omMetadataManager, batchOperation);

    // Do manual commit and see whether addToBatch is successful or not.
    omMetadataManager.getStore().commitBatchOperation(batchOperation);

    // When key commit key is deleted from openKey table and added to keyTable.
    Assert.assertFalse(omMetadataManager.getOpenKeyTable().isExist(openKey));
    Assert.assertTrue(omMetadataManager.getKeyTable().isExist(
        omMetadataManager.getOzoneKey(volumeName, bucketName, keyName)));
  }

  @Test
  public void testAddToDBBatchNoOp() throws Exception {

    OmKeyInfo omKeyInfo = TestOMRequestUtils.createOmKeyInfo(volumeName,
        bucketName, keyName, replicationType, replicationFactor);
    OmVolumeArgs omVolumeArgs = OmVolumeArgs.newBuilder()
        .setOwnerName(keyName).setAdminName(keyName)
        .setVolume(volumeName).setCreationTime(Time.now()).build();

    OzoneManagerProtocolProtos.OMResponse omResponse =
        OzoneManagerProtocolProtos.OMResponse.newBuilder().setCommitKeyResponse(
            OzoneManagerProtocolProtos.CommitKeyResponse.getDefaultInstance())
            .setStatus(OzoneManagerProtocolProtos.Status.KEY_NOT_FOUND)
            .setCmdType(OzoneManagerProtocolProtos.Type.CommitKey)
            .build();

    String openKey = omMetadataManager.getOpenKey(volumeName, bucketName,
        keyName, clientID);
    String ozoneKey = omMetadataManager.getOzoneKey(volumeName, bucketName,
        keyName);

    OMKeyCommitResponse omKeyCommitResponse = new OMKeyCommitResponse(
        omResponse, omKeyInfo, ozoneKey, openKey, omVolumeArgs);

    // As during commit Key, entry will be already there in openKeyTable.
    // Adding it here.
    TestOMRequestUtils.addKeyToTable(true, volumeName, bucketName, keyName,
        clientID, replicationType, replicationFactor, omMetadataManager);

    Assert.assertTrue(omMetadataManager.getOpenKeyTable().isExist(openKey));

    omKeyCommitResponse.checkAndUpdateDB(omMetadataManager, batchOperation);

    // Do manual commit and see whether addToBatch is successful or not.
    omMetadataManager.getStore().commitBatchOperation(batchOperation);


    // As omResponse is error it is a no-op. So, entry should still be in
    // openKey table.
    Assert.assertTrue(omMetadataManager.getOpenKeyTable().isExist(openKey));
    Assert.assertFalse(omMetadataManager.getKeyTable().isExist(
        omMetadataManager.getOzoneKey(volumeName, bucketName, keyName)));
  }
}
