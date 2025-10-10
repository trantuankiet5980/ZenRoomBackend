package vn.edu.iuh.fit.dtos.requests;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchInteractionRequest {

    @NotNull
    private SearchInteractionType type;

    @Size(max = 255)
    private String query;

    private String suggestionId;

    @AssertTrue(message = "query must be provided for QUERY interactions")
    public boolean isQueryPresentWhenRequired() {
        if (type == null) {
            return false;
        }
        if (type == SearchInteractionType.QUERY) {
            return query != null && !query.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "suggestionId must be provided for CLICK interactions")
    public boolean isSuggestionPresentForClick() {
        if (type == null) {
            return false;
        }
        if (type == SearchInteractionType.CLICK) {
            return suggestionId != null && !suggestionId.isBlank();
        }
        return true;
    }

    public enum SearchInteractionType {
        QUERY,
        CLICK
    }
}
