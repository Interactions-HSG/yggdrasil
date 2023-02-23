package org.hyperagents.yggdrasil.jason;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class YAgentArch2Test {
  private YAgentArch2 yAgentArch2;
  private Map<String, String> HEADERS = Map.of("X-Agent-WebID","http://example.org/agent", "X-Agent-Name", "example");

  @Before
  public void setUp() throws Exception {
    this.yAgentArch2 = new YAgentArch2();
  }

  // NOTE: Make sure that the Thing Description has e.g. StringSchema as Input Schema. Similar like below:
  // td:hasInputSchema [ a js:StringSchema;
  //          js:properties [ a js:StringSchema, dctypes:StillImage;
  //              js:propertyName "imageUrl"
  //            ]
  //        ];
  @Test
  public void testInvokeActionWithJsonObject() {
    // This is a sample request
    // make sure that the TD got created previously!
    JsonObject body = new JsonObject();
    body.addProperty("imageUrl", "https://i.imgur.com/5bGzZi7.jpg");
    yAgentArch2.invokeAction("http://localhost:8080/workspaces/online-disinfo/artifacts/img-annotator", "annotate image", body, HEADERS, new HashMap<>());
  }

  // NOTE: Make sure that the Thing Description has ObjectSchema as Input Schema. Similar like below:
  // td:hasInputSchema [ a js:ObjectSchema;
  //          js:properties [ a js:ObjectSchema, dctypes:StillImage;
  //              js:propertyName "imageUrl"
  //            ]
  //        ];
  @Test
  public void testInvokeActionWithPrimitive() {
    // This is a sample request
    // make sure that the TD got created previously!
    yAgentArch2.invokeAction("http://localhost:8080/workspaces/online-disinfo/artifacts/img-annotator", "annotate image", "https://i.imgur.com/5bGzZi7.jpg", HEADERS, new HashMap<>());
  }

  // NOTE: Make sure that the Thing Description has ArraySchema as Input Schema. Similar like below:
  // td:hasInputSchema [ a js:ArraySchema;
  //          js:properties [ a js:ArraySchema, dctypes:StillImage;
  //              js:propertyName "imageUrl"
  //            ]
  //        ];
  @Test
  public void testInvokeActionWithJsonArray() {
    // This is a sample request
    // make sure that the TD got created previously!
    JsonArray body = new JsonArray();
    body.add("https://i.imgur.com/5bGzZi7.jpg");
    yAgentArch2.invokeAction("http://localhost:8080/workspaces/online-disinfo/artifacts/img-annotator", "annotate image", body, HEADERS, new HashMap<>());
  }
}
