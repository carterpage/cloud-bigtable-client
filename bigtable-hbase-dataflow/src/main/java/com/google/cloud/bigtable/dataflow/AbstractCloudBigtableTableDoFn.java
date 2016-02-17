/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.dataflow;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.dataflow.sdk.transforms.DoFn;

/**
 * This class can be used as a superclass for {@link DoFn}s that require a {@link Connection} in
 * Dataflow.
 * 
 * @param <In> the type of the (main) input elements
 * @param <Out> the type of the (main) output elements
 */
public abstract class AbstractCloudBigtableTableDoFn<In, Out> extends DoFn<In, Out> {
  private static final long serialVersionUID = 1L;

  private static final CloudBigtableConnectionPool pool = new CloudBigtableConnectionPool();

  protected final Logger DOFN_LOG = LoggerFactory.getLogger(getClass());
  protected CloudBigtableConfiguration config;
  protected Connection connection;

  public AbstractCloudBigtableTableDoFn(CloudBigtableConfiguration config) {
    this.config = config;
  }

  protected synchronized Connection getConnection() throws IOException {
    if (connection == null) {
      connection = pool.getConnection(config.toHBaseConfig());
    }
    return connection;
  }

  /**
   * Logs the {@link com.google.cloud.dataflow.sdk.transforms.DoFn.Context} and the exception's
   * {@link RetriesExhaustedWithDetailsException#getExhaustiveDescription()}.
   */
  protected void logExceptions(Context context, RetriesExhaustedWithDetailsException exception) {
    DOFN_LOG.warn("For context {}: exception occured during bulk writing: {}", context,
      exception.getExhaustiveDescription());
  }

  protected static void rethrowException(RetriesExhaustedWithDetailsException exception)
      throws Exception {
    if (exception.getCauses().size() == 1) {
      throw (Exception) exception.getCause(0);
    } else {
      throw exception;
    }
  }
}