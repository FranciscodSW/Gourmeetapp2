import Foundation

struct Recipe: Codable, Identifiable, Sendable {
    let id: Int
    let name: String
    let minutes: Int?
    let difficulty: String?
    let category: String?
    let imageURL: URL?
    let description: String?
    let portions: Int?
    let tip: String?

    enum CodingKeys: String, CodingKey {
        case id = "REC_ID"
        case name = "REC_NOMBRE"
        case minutes = "REC_TIEMPO_PREPARACION"
        case difficulty = "Dificultad"
        case category = "Categoria"
        case imageURL = "FotoReceta"
        case description = "REC_DESCRIPCION"
        case portions = "REC_PORCIONES"
        case tip = "REC_DATOGOUMEET"
    }

    init(id: Int, name: String, minutes: Int?, difficulty: String?, category: String?,
         imageURL: URL? = nil, description: String? = nil, portions: Int? = nil, tip: String? = nil) {
        self.id = id
        self.name = name
        self.minutes = minutes
        self.difficulty = difficulty
        self.category = category
        self.imageURL = imageURL
        self.description = description
        self.portions = portions
        self.tip = tip
    }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        guard let id = values.integer(.id), id > 0 else {
            throw DecodingError.dataCorruptedError(forKey: .id, in: values, debugDescription: "REC_ID inválido")
        }
        self.init(
            id: id,
            name: try values.decode(String.self, forKey: .name),
            minutes: values.integer(.minutes),
            difficulty: try values.decodeIfPresent(String.self, forKey: .difficulty),
            category: try values.decodeIfPresent(String.self, forKey: .category),
            imageURL: (try? values.decode(String.self, forKey: .imageURL)).flatMap { URL(string: $0) },
            description: try values.decodeIfPresent(String.self, forKey: .description),
            portions: values.integer(.portions),
            tip: try values.decodeIfPresent(String.self, forKey: .tip)
        )
    }
}

struct Ingredient: Codable, Sendable {
    let name: String
    let amount: String?

    enum CodingKeys: String, CodingKey {
        case name = "ING_DESCRIPCION"
        case amount = "cantidad"
    }
}

struct RecipeStep: Codable, Sendable {
    let number: Int
    let instruction: String

    enum CodingKeys: String, CodingKey {
        case number = "paso"
        case instruction = "descripcion"
    }
}

struct RecipeDetail: Codable, Identifiable, Sendable {
    let recipe: Recipe
    let ingredients: [Ingredient]
    let steps: [RecipeStep]

    var id: Int { recipe.id }
}

struct FavoriteRecipes {
    let entries: [RecipeDetail]

    init(data: Data) {
        entries = (try? JSONDecoder().decode([RecipeDetail].self, from: data)) ?? []
    }

    func contains(_ id: Int) -> Bool {
        entries.contains { $0.id == id }
    }

    func toggling(_ detail: RecipeDetail) throws -> Data {
        var updated = entries
        if let index = updated.firstIndex(where: { $0.id == detail.id }) {
            updated.remove(at: index)
        } else {
            updated.insert(detail, at: 0)
        }
        return try JSONEncoder().encode(updated)
    }
}

private extension KeyedDecodingContainer {
    func integer(_ key: Key) -> Int? {
        if let value = try? decode(Int.self, forKey: key) {
            return value
        }
        return (try? decode(String.self, forKey: key)).flatMap { Int($0) }
    }
}
