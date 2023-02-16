package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public class DLTInterface extends HypermediaArtifact {

  String dltClientUrl;
  HttpClient client;

  public void init(String dltClientUrl){
    this.dltClientUrl = dltClientUrl;
    this.client = HttpClient.newBuilder().build();
  }

  @OPERATION
  public void sendTransaction(String transaction){
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .POST(new HttpRequest.BodyPublisher() {
          @Override
          public long contentLength() {
            return 0;
          }

          @Override
          public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {

          }
        })
        .uri(new URI(dltClientUrl))
        .build();
      this.client.send(request, new HttpResponse.BodyHandler<Object>() {
        @Override
        public HttpResponse.BodySubscriber<Object> apply(HttpResponse.ResponseInfo responseInfo) {
          return null;
        }
      });
    } catch (Exception e){
      e.printStackTrace();
    }

  }
  @Override
  protected void registerInteractionAffordances() {
    registerActionAffordance("http://example.org/sendTransaction", "sendTransaction", "/sendTransaction",
      new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .build());

  }
}
