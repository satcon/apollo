/*
 * Copyright 2024 Apollo Authors
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
 *
 */
package com.ctrip.framework.apollo.configservice.util;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Instance;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;
import com.ctrip.framework.apollo.biz.service.InstanceService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class InstanceConfigAuditUtilTest {
  private InstanceConfigAuditUtil instanceConfigAuditUtil;

  @Mock
  private InstanceService instanceService;
  @Mock
  private BizConfig bizConfig;
  @Mock
  private MeterRegistry meterRegistry;
  private BlockingQueue<InstanceConfigAuditUtil.InstanceConfigAuditModel> audits;

  private String someAppId;
  private String someConfigClusterName;
  private String someClusterName;
  private String someDataCenter;
  private String someIp;
  private String someConfigAppId;
  private String someConfigNamespace;
  private String someReleaseKey;

  private InstanceConfigAuditUtil.InstanceConfigAuditModel someAuditModel;

  @Before
  public void setUp() throws Exception {
    when(bizConfig.getInstanceConfigAuditMaxSize()).thenReturn(100);
    when(bizConfig.getInstanceCacheMaxSize()).thenReturn(100);
    when(bizConfig.getInstanceConfigCacheMaxSize()).thenReturn(100);

    instanceConfigAuditUtil = new InstanceConfigAuditUtil(instanceService, bizConfig, meterRegistry);

    audits = (BlockingQueue<InstanceConfigAuditUtil.InstanceConfigAuditModel>)
        ReflectionTestUtils.getField(instanceConfigAuditUtil, "audits");

    someAppId = "someAppId";
    someClusterName = "someClusterName";
    someDataCenter = "someDataCenter";
    someIp = "someIp";
    someConfigAppId = "someConfigAppId";
    someConfigClusterName = "someConfigClusterName";
    someConfigNamespace = "someConfigNamespace";
    someReleaseKey = "someReleaseKey";

    someAuditModel = new InstanceConfigAuditUtil.InstanceConfigAuditModel(someAppId,
        someClusterName, someDataCenter, someIp, someConfigAppId, someConfigClusterName,
        someConfigNamespace, someReleaseKey);
  }

  @Test
  public void testAudit() throws Exception {
    boolean result = instanceConfigAuditUtil.audit(someAppId, someClusterName, someDataCenter,
        someIp, someConfigAppId, someConfigClusterName, someConfigNamespace, someReleaseKey);

    InstanceConfigAuditUtil.InstanceConfigAuditModel audit = audits.poll();

    assertTrue(result);
    assertTrue(Objects.equals(someAuditModel, audit));
  }

  @Test
  public void testDoAudit() throws Exception {
    long someInstanceId = 1;
    Instance someInstance = mock(Instance.class);

    when(someInstance.getId()).thenReturn(someInstanceId);
    when(instanceService.createInstance(any(Instance.class))).thenReturn(someInstance);

    instanceConfigAuditUtil.doAudit(someAuditModel);

    verify(instanceService, times(1)).findInstance(someAppId, someClusterName, someDataCenter,
        someIp);
    verify(instanceService, times(1)).createInstance(any(Instance.class));
    verify(instanceService, times(1)).findInstanceConfig(someInstanceId, someConfigAppId,
        someConfigNamespace);
    verify(instanceService, times(1)).createInstanceConfig(any(InstanceConfig.class));
  }


}
