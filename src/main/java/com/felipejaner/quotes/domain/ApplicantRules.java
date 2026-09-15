package com.felipejaner.quotes.domain;

public final class ApplicantRules {
  private ApplicantRules() {}

  public static boolean isSenior(int age) {
    return age > 65;
  }
}
