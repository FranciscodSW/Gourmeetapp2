import Foundation

protocol RecipeSource: Sendable {
    func recipes(matching query: String) async throws -> [Recipe]
    func detail(for id: Int) async throws -> RecipeDetail
    func imageURL(for recipe: Recipe) -> URL?
}

enum RecipeAPIError: LocalizedError {
    case invalidURL
    case invalidResponse
    case missingRecipe

    var errorDescription: String? {
        switch self {
        case .invalidURL: "La dirección de la API no es válida."
        case .invalidResponse: "El servidor no pudo completar la solicitud."
        case .missingRecipe: "La receta ya no está disponible."
        }
    }
}

struct RecipeAPI: RecipeSource {
    let baseURL: URL

    init(address: String) throws {
        guard let url = URL(string: address),
              let host = url.host, !host.isEmpty,
              url.user == nil, url.password == nil, url.query == nil, url.fragment == nil,
              url.scheme == "https" || (url.scheme == "http" && host == "127.0.0.1") else {
            throw RecipeAPIError.invalidURL
        }
        baseURL = url
    }

    func recipes(matching query: String) async throws -> [Recipe] {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let response: CatalogResponse
        if term.isEmpty {
            response = try await post("api_recetas_inicio.php", body: ["categoriaId": 0])
        } else {
            response = try await post("api_filtros_recetas_nombre.php", body: ["busqueda": term])
        }
        guard response.success else { throw RecipeAPIError.invalidResponse }
        var seen = Set<Int>()
        return response.recipes.filter { seen.insert($0.id).inserted }
    }

    func detail(for id: Int) async throws -> RecipeDetail {
        let response: DetailResponse = try await post("api_detalle_receta.php", body: ["REC_ID": id])
        guard response.success, let recipe = response.recipe, recipe.id == id else {
            throw RecipeAPIError.missingRecipe
        }
        return RecipeDetail(recipe: recipe, ingredients: response.ingredients ?? [],
                            steps: (response.steps ?? []).sorted { $0.number < $1.number })
    }

    func imageURL(for recipe: Recipe) -> URL? {
        guard let image = recipe.imageURL else { return nil }
        if baseURL.host == "127.0.0.1", image.path.hasPrefix("/admin/adjuntos/"),
           var local = URLComponents(url: baseURL, resolvingAgainstBaseURL: false) {
            local.percentEncodedPath = URLComponents(url: image, resolvingAgainstBaseURL: false)?.percentEncodedPath ?? image.path
            return local.url
        }
        guard image.host != nil, image.scheme == "https" else { return nil }
        return image
    }

    private func post<Body: Encodable, Response: Decodable>(_ route: String, body: Body) async throws -> Response {
        let url = baseURL.appendingPathComponent("recetas").appendingPathComponent(route)
        var request = URLRequest(url: url, timeoutInterval: 15)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw RecipeAPIError.invalidResponse
        }
        return try JSONDecoder().decode(Response.self, from: data)
    }
}

private struct CatalogResponse: Decodable {
    let success: Bool
    let recipes: [Recipe]

    enum CodingKeys: String, CodingKey {
        case success, coincidencia, calorias, tiempo, gasto, sin_lacteos, sin_azucar, dificultad
    }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        success = try values.decode(Bool.self, forKey: .success)
        let groups: [CodingKeys] = [.coincidencia, .calorias, .tiempo, .gasto, .sin_lacteos, .sin_azucar, .dificultad]
        recipes = try groups.flatMap { try values.decodeIfPresent([Recipe].self, forKey: $0) ?? [] }
    }
}

private struct DetailResponse: Decodable {
    let success: Bool
    let recipe: Recipe?
    let ingredients: [Ingredient]?
    let steps: [RecipeStep]?

    enum CodingKeys: String, CodingKey {
        case success
        case recipe = "receta"
        case ingredients = "ingredientes"
        case steps = "preparacion"
    }
}
