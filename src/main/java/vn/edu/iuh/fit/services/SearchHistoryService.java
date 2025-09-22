package vn.edu.iuh.fit.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface SearchHistoryService {
    void saveHistory(String userId, String keyword, ObjectNode filtersJson);
}
