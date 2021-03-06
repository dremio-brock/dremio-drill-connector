/*
 * Copyright (C) 2017-2018 Dremio Corporation
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
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.server.SabotContext;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcStoragePlugin;
import com.dremio.exec.store.jdbc.JdbcStoragePlugin.Config;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.google.common.annotations.VisibleForTesting;

import io.protostuff.Tag;

/**
 * Configuration for Drill sources.
 */
@SourceType(value = "DRILL", label = "Drill")
public class DrillConf extends AbstractArpConf<DrillConf> {
  private static final String ARP_FILENAME = "arp/implementation/drill-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
  private static final String DRIVER = "org.apache.drill.jdbc.Driver";

  @Tag(1)
  @DisplayMetadata(label = "Drillbit")
  public boolean drillbit;

  @NotBlank
  @Tag(2)
  @DisplayMetadata(label = "Host")
  public String host;

  @NotBlank
  @Tag(3)
  @DisplayMetadata(label = "Port")
  public String port;

  @Tag(4)
  @DisplayMetadata(label = "Directory")
  public String directory = "/drill";

  @Tag(5)
  @DisplayMetadata(label = "Cluster ID")
  public String clusterId = "drillbits1";

  @Tag(6)
  @DisplayMetadata(label = "schema")
  public String schema;

  @Tag(7)
  @DisplayMetadata(label = "Record fetch size")
  @NotMetadataImpacting
  public int fetchSize = 200;

  @VisibleForTesting
  public String toJdbcConnectionString() {
    final String database = checkNotNull(this.host, "Missing host.");
    final String port = checkNotNull(this.port, "Missing port.");
    final StringBuilder builder = new StringBuilder("jdbc:drill");

    if (drillbit) {
      builder.append(String.format(":drillbit=%s:%s", host, port));
    } else {
      builder.append(String.format(":zk=%s:%s%s/%s", host, port, directory, clusterId));
    }

    if (schema != null && schema.length() != 0) {
      builder.append(String.format(";schema=%s", schema));
    }

    return builder.toString();
  }

  @Override
  @VisibleForTesting
  public Config toPluginConfig(SabotContext context) {
    return JdbcStoragePlugin.Config.newBuilder()
        .withDialect(getDialect())
        .withFetchSize(fetchSize)
        .withDatasourceFactory(this::newDataSource)
        .clearHiddenSchemas()
        .addHiddenSchema("SYSTEM")
        .build();
  }

  private CloseableDataSource newDataSource() {
    return DataSources.newGenericConnectionPoolDataSource(DRIVER,
      toJdbcConnectionString(), null, null, null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE);
  }

  @Override
  public ArpDialect getDialect() {
    return ARP_DIALECT;
  }

  @VisibleForTesting
  public static ArpDialect getDialectSingleton() {
    return ARP_DIALECT;
  }
}
