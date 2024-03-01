package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import ch.unisg.ics.interactions.hmas.interaction.shapes.StringSpecification;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public class DltInterface extends HypermediaArtifact {
  private String dltClientUrl;
  private HttpClient client;

  public void init(final String dltClientUrl) {
    this.dltClientUrl = dltClientUrl;
    this.client = HttpClient.newBuilder().build();
  }

  @OPERATION
  public void sendTransaction(final String transaction) {
    try {
      final var request = HttpRequest.newBuilder()
                                     .POST(new HttpRequest.BodyPublisher() {
                                       @Override
                                       public long contentLength() {
                                         return 0;
                                       }

                                       @Override
                                       public void subscribe(
                                           final Flow.Subscriber<? super ByteBuffer> subscriber
                                       ) {}
                                     })
                                     .uri(new URI(dltClientUrl))
                                     .build();
      this.client.send(request, responseInfo -> null);
    } catch (final Exception e) {
      this.failed(e.getMessage());
    }
  }
// TODO: set correct Specification
  @Override
  protected void registerInteractionAffordances() {
    this.registerSignifier(
        "http://example.org/sendTransaction",
        "sendTransaction",
        "/sendTransaction",
        new StringSpecification.Builder().build()
    );
  }
}
