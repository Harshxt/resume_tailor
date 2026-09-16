package one.harshit.resumeTailor.model.dto;

import java.util.List;

public record ResumeDataDto(
        String summary,
        PersonalInfo personalInfo,
        List<Experience> experiences,
        List<Project> projects,
        List<SkillCategory> skills,
        List<Education> education,
        List<MiscCategory> miscCategories

) {

}

record PersonalInfo(String fullName, String email, String phone, String location, List<Link> links) {}

record Link(String websiteName, String link) {}

record Experience(String companyName, String position, String startDate, String endDate, String location,
        List<String> bullets) {}

record Project(String title, String startDate, String endDate, List<String> bullets) {}

record SkillCategory(String categoryName, List<String> skills) {}

record MiscCategory(String categoryName, String title, List<String> Bullets) {}

record Education(String institution, String degree, String location, String startDate, String endDate,
        List<String> bullets) {}
