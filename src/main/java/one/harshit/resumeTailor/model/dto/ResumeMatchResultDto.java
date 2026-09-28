package one.harshit.resumeTailor.model.dto;

import java.util.List;

public record ResumeMatchResultDto(
    int jdMatchScore,
    int targetRoleScore,
    int overallCompositeScore,
    JdEvaluationDetails jdDetails,
    RoleEvaluationDetails roleDetails
    

) {

    public record JdEvaluationDetails(
        int baseScore,                    // Starts at 100 (or calculated baseline)
        int totalPenalties,               // Total deducted points (negative sum)
        int totalBonuses,                 // Total earned bonus points
        List<ScoredCriterion> criteria,   // Item-by-item breakdown
        List<String> matchedMustHaves,
        List<String> missingMustHaves,
        List<String> matchedCoreSkills,
        List<String> missingCoreSkills,
        String summaryFeedback
    ) {}


   
    public record ScoredCriterion(
        String name,                      // e.g. "Bachelor's Degree in CS"
        CriterionTier tier,               // MUST_HAVE, CORE, NICE_TO_HAVE
        CriterionStatus status,           // SATISFIED, PARTIALLY_SATISFIED, MISSING
        int pointImpact,                  // e.g., -35 or +10
        String evidenceOrReasoning        // e.g., "Resume lists No Degree / High School only"
    ) {}

     public enum CriterionTier {
        MUST_HAVE,     // Asymmetric: +10 if met, -35 if missing
        CORE,          // Symmetrical: +15 if met, -15 if missing
        NICE_TO_HAVE   // One-way bonus: +5 if met, 0 if missing
    }

    public enum CriterionStatus {
        SATISFIED,
        PARTIALLY_SATISFIED,
        MISSING
    }


      public record RoleEvaluationDetails(
        int seniorityFitScore,            // 0 - 40 (e.g., scope, leadership, years)
        int archetypeFitScore,            // 0 - 35 (e.g., core competencies for this job title)
        int careerTrajectoryScore,        // 0 - 25 (e.g., coherence of past roles towards target role)
        String identifiedSeniorityLevel,  // e.g., "Mid-Level (~3 years)"
        String expectedSeniorityLevel,    // e.g., "Senior (5+ years)"
        List<String> roleStrengths,
        List<String> roleGaps,
        String roleAlignmentSummary
    ) {}
}

 