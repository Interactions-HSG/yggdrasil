package org.hyperagents.yggdrasil.utils.impl;

import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

public final class RepresentationFactoryFactory {

  private RepresentationFactoryFactory(){}

  public static RepresentationFactory getRepresentationFactory(final String representationFactory,
                                                               final WebSubConfig notificationConfig,
                                                               final HttpInterfaceConfig httpConfig) {
    if (representationFactory == null) return new RepresentationFactoryTDImplt(httpConfig, notificationConfig);
    return switch (representationFactory) {
      case "hmas" -> new RepresentationFactoryHMASImpl(httpConfig, notificationConfig);
      case "td" -> new RepresentationFactoryTDImplt(httpConfig, notificationConfig);
      default -> throw new IllegalArgumentException("Unknown representation factory: " + representationFactory);
    };
  }
}
