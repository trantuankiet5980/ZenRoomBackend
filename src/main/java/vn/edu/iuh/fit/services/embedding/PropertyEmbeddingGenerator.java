package vn.edu.iuh.fit.services.embedding;

import vn.edu.iuh.fit.entities.Property;

import java.util.Optional;

/**
 * Generates an embedding vector for a {@link Property}.
 */
public interface PropertyEmbeddingGenerator {

    /**
     * Builds an embedding vector for the given property.
     *
     * @param property property to encode
     * @return optional embedding vector; empty if the property does not have enough information
     */
    Optional<double[]> generate(Property property);
}
