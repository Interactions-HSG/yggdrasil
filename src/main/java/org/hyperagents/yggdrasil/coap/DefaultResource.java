package org.hyperagents.yggdrasil.coap;

import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.utils.CoapResource;

public class DefaultResource extends CoapResource {
  @Override
  public void get(CoapExchange coapExchange) throws CoapCodeException {
    coapExchange.setResponseCode(Code.C203_VALID);
    coapExchange.setResponseBody("Yggdrasil v0.0");
    coapExchange.sendResponse();
  }
}
