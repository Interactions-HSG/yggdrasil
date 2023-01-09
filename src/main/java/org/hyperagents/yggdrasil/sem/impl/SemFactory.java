package org.hyperagents.yggdrasil.sem.impl;

import org.hyperagents.yggdrasil.sem.SignifierExposureMechanism;

public class SemFactory {

  public static SignifierExposureMechanism createSem(String semIdentifier) {
    return new AbilityBasedSem();
  }

}
