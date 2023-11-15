package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import java.util.regex.Pattern;

public class MatcherArtifact extends HypermediaArtifact {
  private Pattern pattern;

  public void init() {
    this.pattern = Pattern.compile(".*");
  }

  public void init(final String pattern) {
    this.pattern = Pattern.compile(pattern);
  }

  @OPERATION
  public void matches(final String element, final OpFeedbackParam<Boolean> result) {
    result.set(this.pattern.matcher(element).matches());
  }

  @Override
  protected void registerInteractionAffordances() {
    this.registerActionAffordance(
        "http://example.org/matches",
        "matches",
        "/matches",
        new ArraySchema.Builder()
                       .addItem(new StringSchema.Builder().build())
                       .build()
    );
    this.registerFeedbackParameter("matches");
  }
}
