"# ZenRoomBackend" 

## Property embedding generator

`HeuristicPropertyEmbeddingGenerator` extracts the key attributes of a property (such as area, price, deposit, capacity, number of rooms, number of floors, parking slots, title, description, list of services/furnishings, address, and whether it is a BUILDING or ROOM). Each value is then normalized to the [0, 1] range using fixed scaling functions. The final result is a 15-dimensional vector. If all elements are zero (i.e., there is no meaningful signal), the generator returns `Optional.empty()` o avoid storing empty vectors.

The `embedding` column in the `Property` entity is declared as a JSON string, which is used to store the serialized version of this vector.