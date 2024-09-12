package org.hyperagents.yggdrasil.utils.impl;

import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * Factory to give the correct RepresentationFactory depending on ontology.
 */
public final class RepresentationFactoryFactory {

  private RepresentationFactoryFactory() {}

  /**
   * Returns the correct representationFactory for the given ontology.
   *
   * @param representationFactory String representing the ontology
   * @param notificationConfig notifConfig
   * @param httpConfig httpConfig
   * @return RepresentationFactory
   */
  public static RepresentationFactory getRepresentationFactory(
      final String representationFactory,
      final WebSubConfig notificationConfig,
      final HttpInterfaceConfig httpConfig) {
    if (representationFactory == null) {
      return new RepresentationFactoryTDImplt(httpConfig, notificationConfig);
    }
    return switch (representationFactory) {
      case "hmas" -> new RepresentationFactoryHMASImpl(httpConfig, notificationConfig);
      case "td" -> new RepresentationFactoryTDImplt(httpConfig, notificationConfig);
      default -> throw new IllegalArgumentException(
          "Unknown representation factory: " + representationFactory);
    };
  }
}
