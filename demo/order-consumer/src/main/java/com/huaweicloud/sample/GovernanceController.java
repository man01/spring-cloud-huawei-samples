/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huaweicloud.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping(path = "govern")
public class GovernanceController {
  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private FeignService feignService;

  private int count = 0;

  @RequestMapping("/hello")
  public String hello() {
    return restTemplate.getForObject("http://provider/hello", String.class);
  }

  @RequestMapping("/retry")
  public String retry(@RequestParam(name = "invocationID") String invocationID) {
    return restTemplate.getForObject("http://provider/retry?invocationID={1}", String.class, invocationID);
  }

  @RequestMapping("/retryFeign")
  public String retryFeign(@RequestParam(name = "invocationID") String invocationID) {
    return feignService.retry(invocationID);
  }

  @RequestMapping("/circuitBreaker")
  public String circuitBreaker() throws Exception {
    count++;
    if (count % 3 == 0) {
      return "ok";
    }
    throw new RuntimeException("test error");
  }

  @RequestMapping("/bulkhead")
  public String bulkhead(@RequestParam(name = "num", required = false, defaultValue = "1") int num) {
    CountDownLatch latch = new CountDownLatch(1);
    Map<String, Object> temp = new HashMap<>();
    for (int i = 0; i < num; i++) {
      for (int j = 0; j < 10; j++) {
        String name = "t-" + i + "-" + j;
        new Thread(name) {
          public void run() {
            String result = restTemplate.getForObject("http://provider/bulkhead", String.class);
            latch.countDown();
          }
        }.start();
      }
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "test bulkhead is end!";
  }
}
