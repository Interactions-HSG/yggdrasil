package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.hmas.core.vocabularies.CORE;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IntegerSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ValueSpecification;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;

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
        "http://example.org/add",
        "add",
        "/add",
      new QualifiedValueSpecification.Builder()
        .addRequiredSemanticType("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
        .setRequired(true)
        .addPropertySpecification("http://www.w3.org/1999/02/22-rdf-syntax-ns#First",
          new IntegerSpecification.Builder()
            .setName("1st Parameter")
            .setRequired(true)
            .build())
        .addPropertySpecification("http://www.w3.org/1999/02/22-rdf-syntax-ns#Rest",
          new QualifiedValueSpecification.Builder()
            .setRequired(true)
            .addRequiredSemanticType("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
            .addPropertySpecification(
              "http://www.w3.org/1999/02/22-rdf-syntax-ns#First",
              new IntegerSpecification.Builder()
                .setName("2nd Parameter")
                .setRequired(true)
                .build()
            )
            .addPropertySpecification(
              "http://www.w3.org/1999/02/22-rdf-syntax-ns#Rest",
              new ValueSpecification.Builder()
                .setValueAsString("http://www.w3.org/1999/02/22-rdf-syntax-ns#Nil")
                .setRequired(true)
                .build()
            )
            .build())
        .build()
    );
    this.registerFeedbackParameter("add");
  }
}
