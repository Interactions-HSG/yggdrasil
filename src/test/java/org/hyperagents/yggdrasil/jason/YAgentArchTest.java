package org.hyperagents.yggdrasil.jason;

import cartago.Op;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import jason.asSemantics.*;
import jason.asSyntax.*;
import org.hyperagents.yggdrasil.http.HttpServerVerticle;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class YAgentArchTest {
  private YAgentArch arch;

  @Before
  public void setUp(TestContext tc) {
    arch = new YAgentArch();
  }

  @Test
  public void testInvokeAction(TestContext tc){
    //Literal l = new LiteralImpl("invokeAction(\"http://example.org\")");
    Structure structure = new Structure("invokeAction");
    structure.addTerm(new StringTermImpl("http://example.org"));
    Intention i = new Intention();
    Trigger t = new Trigger(Trigger.TEOperator.add, Trigger.TEType.belief, new LiteralImpl("f"));
    i.push(new IntendedMeans(new Option(new Plan(), new Unifier()), t));
    System.out.println(i.peek());
    ActionExec action = new ActionExec(structure,i);
    Structure s = action.getActionTerm();
    System.out.println(s.getFunctor());
    System.out.println(s.getTerms());
    arch.act(action);
    tc.assertTrue(true);
  }
}
