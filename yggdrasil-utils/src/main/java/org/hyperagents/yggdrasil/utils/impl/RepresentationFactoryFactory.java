package org.hyperagents.yggdrasil.utils.impl;

import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;

public final class RepresentationFactoryFactory {

  private RepresentationFactoryFactory(){}

  public static RepresentationFactory getRepresentationFactory(final String representationFactory,final HttpInterfaceConfig httpConfig) {
    return switch (representationFactory) {
      case "hmas" -> new RepresentationFactoryHMASImpl(httpConfig);
      case "td" -> new RepresentationFactoryTDImplt(httpConfig);
      default -> throw new IllegalArgumentException("Unknown representation factory: " + representationFactory);
    };
  }
}
