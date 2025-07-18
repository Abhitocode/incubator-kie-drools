/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.api.concurrent;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;

import org.kie.api.internal.utils.KieService;

public interface KieExecutors extends KieService {

    ExecutorService getExecutor();

    ExecutorService newSingleThreadExecutor();

    ExecutorService newFixedThreadPool();

    ExecutorService newFixedThreadPool(int nThreads);

    <T> CompletionService<T> getCompletionService();

    public static class Pool {
        public static int SIZE = Runtime.getRuntime().availableProcessors();
    }
}
