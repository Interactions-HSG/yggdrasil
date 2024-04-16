package org.hyperagents.yggdrasil.utils.impl;

import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;

public class RepresentationFactoryFactory {

  public static RepresentationFactory getRepresentationFactory(final String representationFactory, HttpInterfaceConfig httpConfig) {
    return switch (representationFactory) {
      case "hmas" -> new RepresentationFactoryHMASImpl(httpConfig);
      case "td" -> new RepresentationFactoryTDImplt(httpConfig);
      default -> throw new IllegalArgumentException("Unknown representation factory: " + representationFactory);
    };
  }
}
