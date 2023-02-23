package org.hyperagents.yggdrasil.jason;

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

  @Test
  public void testInvokeAction() {
    // This is a sample request
    // make sure that the TD got created previously!
    yAgentArch2.invokeAction("http://localhost:8080/workspaces/online-disinfo/artifacts/img-annotator", "annotate image", new String[]{"https://i.imgur.com/5bGzZi7.jpg"}, HEADERS, new HashMap<>());
  }
}
