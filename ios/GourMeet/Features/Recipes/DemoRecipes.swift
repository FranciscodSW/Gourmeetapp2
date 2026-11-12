import Foundation

struct DemoRecipes: RecipeSource {
    private let entries = [
        RecipeDetail(
            recipe: Recipe(id: 1, name: "Tostadas de aguacate", minutes: 15,
                           difficulty: "Fácil", category: "Desayuno",
                           description: "Pan crujiente con aguacate, limón y semillas.", portions: 2),
            ingredients: [
                Ingredient(name: "Pan integral", amount: "2 rebanadas"),
                Ingredient(name: "Aguacate", amount: "1 pieza"),
                Ingredient(name: "Limón", amount: "½ pieza")
            ],
            steps: [
                RecipeStep(number: 1, instruction: "Tuesta el pan hasta que esté dorado."),
                RecipeStep(number: 2, instruction: "Machaca el aguacate con jugo de limón y extiéndelo sobre las tostadas.")
            ]
        ),
        RecipeDetail(
            recipe: Recipe(id: 2, name: "Arroz con verduras", minutes: 30,
                           difficulty: "Fácil", category: "Comida",
                           description: "Arroz con zanahoria y calabacita salteadas.", portions: 4),
            ingredients: [
                Ingredient(name: "Arroz", amount: "1 taza"),
                Ingredient(name: "Zanahoria", amount: "1 pieza"),
                Ingredient(name: "Calabacita", amount: "1 pieza")
            ],
            steps: [
                RecipeStep(number: 1, instruction: "Cuece el arroz y déjalo reposar tapado."),
                RecipeStep(number: 2, instruction: "Saltea las verduras picadas y mézclalas con el arroz.")
            ]
        )
    ]

    func recipes(matching query: String) async throws -> [Recipe] {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let recipes = entries.map(\.recipe)
        guard !term.isEmpty else { return recipes }
        return recipes.filter { $0.name.localizedStandardContains(term) }
    }

    func detail(for id: Int) async throws -> RecipeDetail {
        guard let entry = entries.first(where: { $0.recipe.id == id }) else {
            throw RecipeAPIError.missingRecipe
        }
        return entry
    }

    func imageURL(for recipe: Recipe) -> URL? { nil }
}
