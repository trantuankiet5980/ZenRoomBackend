package vn.edu.iuh.fit.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank
    private String query;

    private String suggestionId;

    public enum SearchInteractionType {
        QUERY,
        CLICK
    }
}
