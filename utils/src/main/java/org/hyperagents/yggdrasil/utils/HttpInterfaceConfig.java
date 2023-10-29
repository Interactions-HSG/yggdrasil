package org.hyperagents.yggdrasil.utils;

import java.util.Optional;

public interface HttpInterfaceConfig {
  String getHost();

  int getPort();

  String getBaseUri();

  Optional<String> getWebSubHubUri();
}
