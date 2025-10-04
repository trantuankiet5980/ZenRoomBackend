package vn.edu.iuh.fit.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchSuggestionDto {
    String suggestionId;
    String referenceType;
    String referenceId;
    String title;
    String subtitle;
    BigDecimal price;
    String metadata;
    String keywords;
    double score;
}
