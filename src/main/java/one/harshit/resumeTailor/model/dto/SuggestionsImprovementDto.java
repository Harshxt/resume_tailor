package one.harshit.resumeTailor.model.dto;

import java.util.List;

public record SuggestionsImprovementDto(
        List<Change> changes

) {
    public record Change(
            ChangeType type,
            ChangeAction action,
            String text) {
    }

    public enum ChangeType {
        MUST_DO, RECOMMENDED, GOOD_TO_HAVE
    }

    public enum ChangeAction {
        ADD, REMOVE, IMPROVE

    }

}
