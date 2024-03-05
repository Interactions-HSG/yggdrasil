package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IntegerSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ValueSpecification;


public class Adder extends HypermediaArtifact {
  @OPERATION
  public void add(final int x, final int y, final OpFeedbackParam<Integer> sumParam) {
    this.log("adder performs add");
    sumParam.set(x + y);
    this.log("result in adder: " + sumParam.get());
  }

  @Override
  protected void registerInteractionAffordances() {
    this.registerSignifier(
        "http://example.org/Add",
        "add",
        "/add",
      new QualifiedValueSpecification.Builder()
        .setIRIAsString("http://example.org/addends")
        .addRequiredSemanticType("https://www.w3.org/1999/02/22-rdf-syntax-ns#List")
        .setRequired(true)
        .addPropertySpecification("https://www.w3.org/1999/02/22-rdf-syntax-ns#first",
          new IntegerSpecification.Builder()
            .setName("1st Parameter")
            .setRequired(true)
            .build())
        .addPropertySpecification("https://www.w3.org/1999/02/22-rdf-syntax-ns#rest",
          new QualifiedValueSpecification.Builder()
            .setIRIAsString("http://example.org/addendsRest")
            .setRequired(true)
            .addRequiredSemanticType("https://www.w3.org/1999/02/22-rdf-syntax-ns#List")
            .addPropertySpecification(
              "https://www.w3.org/1999/02/22-rdf-syntax-ns#first",
              new IntegerSpecification.Builder()
                .setName("2nd Parameter")
                .setRequired(true)
                .build()
            )
            .addPropertySpecification(
              "https://www.w3.org/1999/02/22-rdf-syntax-ns#rest",
              new ValueSpecification.Builder()
                .addRequiredSemanticType("https://www.w3.org/1999/02/22-rdf-syntax-ns#List")
                .setValueAsString("https://www.w3.org/1999/02/22-rdf-syntax-ns#nil")
                .setRequired(true)
                .build()
            )
            .build())
        .build()
    );
    this.registerFeedbackParameter("add");
  }
}
