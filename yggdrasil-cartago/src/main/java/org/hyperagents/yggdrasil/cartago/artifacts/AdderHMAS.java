package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IntegerSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ValueSpecification;
import org.eclipse.rdf4j.model.vocabulary.RDF;


public class AdderHMAS extends HypermediaHMASArtifact {
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
        "add",
      new QualifiedValueSpecification.Builder()
        .setIRIAsString("http://example.org/addends")
        .addRequiredSemanticType(RDF.LIST.stringValue())
        .setRequired(true)
        .addPropertySpecification(RDF.FIRST.stringValue(),
          new IntegerSpecification.Builder()
            .setName("1st Parameter")
            .setRequired(true)
            .build())
        .addPropertySpecification(RDF.REST.stringValue(),
          new QualifiedValueSpecification.Builder()
            .setIRIAsString("http://example.org/addendsRest")
            .setRequired(true)
            .addRequiredSemanticType(RDF.LIST.stringValue())
            .addPropertySpecification(
              RDF.FIRST.stringValue(),
              new IntegerSpecification.Builder()
                .setName("2nd Parameter")
                .setRequired(true)
                .build()
            )
            .addPropertySpecification(
              RDF.REST.stringValue(),
              new ValueSpecification.Builder()
                .addRequiredSemanticType(RDF.LIST.stringValue())
                .setValueAsString(RDF.NIL.stringValue())
                .setRequired(true)
                .build()
            )
            .build())
        .build()
    );
    this.registerFeedbackParameter("add");
  }
}
