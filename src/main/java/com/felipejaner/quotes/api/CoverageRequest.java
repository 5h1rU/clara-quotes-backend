package com.felipejaner.quotes.api;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.felipejaner.quotes.domain.*;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/** Setters distinguish an omitted property from an explicitly supplied null. */
public class CoverageRequest {
    @NotNull
    private CoverageType coverageType;
    private Boolean hasPreexistingConditions;
    private Set<@NotNull Condition> conditions;
    private Boolean takesPrescriptionMedication;
    private Boolean usesTobacco;
    private Boolean needsSpouseCoverage;
    public CoverageRequest() {}
    public CoverageRequest(CoverageType coverageType, Boolean hasPreexistingConditions, Set<@NotNull Condition> conditions, Boolean takesPrescriptionMedication, Boolean usesTobacco, Boolean needsSpouseCoverage) {
        this.coverageType = coverageType;
        this.hasPreexistingConditions = hasPreexistingConditions;
        this.conditions = conditions;
        this.takesPrescriptionMedication = takesPrescriptionMedication;
        this.usesTobacco = usesTobacco;
        this.needsSpouseCoverage = needsSpouseCoverage;
    }
    public CoverageType coverageType() { return coverageType; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setCoverageType(CoverageType value) { this.coverageType = value; }
    public Boolean hasPreexistingConditions() { return hasPreexistingConditions; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setHasPreexistingConditions(Boolean value) { this.hasPreexistingConditions = value; }
    public Set<@NotNull Condition> conditions() { return conditions; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setConditions(Set<@NotNull Condition> value) { this.conditions = value; }
    public Boolean takesPrescriptionMedication() { return takesPrescriptionMedication; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setTakesPrescriptionMedication(Boolean value) { this.takesPrescriptionMedication = value; }
    public Boolean usesTobacco() { return usesTobacco; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setUsesTobacco(Boolean value) { this.usesTobacco = value; }
    public Boolean needsSpouseCoverage() { return needsSpouseCoverage; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setNeedsSpouseCoverage(Boolean value) { this.needsSpouseCoverage = value; }
}
