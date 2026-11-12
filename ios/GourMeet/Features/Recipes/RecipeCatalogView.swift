import SwiftUI

struct RecipeCatalogView: View {
    let source: any RecipeSource
    let isDemo: Bool

    @AppStorage private var savedRecipes: Data

    init(source: any RecipeSource, isDemo: Bool, favoritesScope: String) {
        self.source = source
        self.isDemo = isDemo
        _savedRecipes = AppStorage(wrappedValue: Data(), "favorites.v1." + favoritesScope)
    }

    @State private var query = ""
    @State private var result: LoadState<[Recipe]> = .loading
    @State private var completedRequest: Request?
    @State private var revision = 0

    private struct Request: Equatable {
        let query: String
        let revision: Int
    }

    private var request: Request {
        Request(query: query.trimmingCharacters(in: .whitespacesAndNewlines), revision: revision)
    }

    var body: some View {
        let current = request
        NavigationStack {
            Group {
                if completedRequest == current {
                    switch result {
                    case .loading:
                        ProgressView("Cargando recetas")
                    case .failure:
                        LoadErrorView(message: "No se pudieron cargar las recetas.") { revision += 1 }
                    case .loaded(let recipes):
                        if recipes.isEmpty {
                            emptyState(for: current.query)
                        } else {
                            catalog(recipes)
                        }
                    }
                } else {
                    ProgressView("Cargando recetas")
                }
            }
            .navigationTitle("Qué cocinar")
            .searchable(text: $query, prompt: "Busca una receta")
            .toolbar {
                NavigationLink {
                    FavoriteRecipesView(source: source, savedRecipes: $savedRecipes)
                } label: {
                    Label("Favoritos", systemImage: "heart")
                }
            }
            .safeAreaInset(edge: .top, spacing: 0) {
                if isDemo {
                    Label("Modo demostración", systemImage: "info.circle")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(.regularMaterial)
                }
            }
            .task(id: current) {
                await load(current, debounce: !current.query.isEmpty)
            }
        }
    }

    private func catalog(_ recipes: [Recipe]) -> some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 280), spacing: 16)], spacing: 16) {
                ForEach(recipes) { recipe in
                    NavigationLink {
                        RecipeDetailView(summary: recipe, source: source, savedRecipes: $savedRecipes)
                    } label: {
                        RecipeCard(recipe: recipe, imageURL: source.imageURL(for: recipe))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
        .refreshable { await load(request, debounce: false) }
    }

    private func emptyState(for term: String) -> some View {
        ContentUnavailableView {
            Label(term.isEmpty ? "Todavía no hay recetas" : "Sin resultados",
                  systemImage: term.isEmpty ? "fork.knife" : "magnifyingglass")
        } description: {
            Text(term.isEmpty ? "Intenta de nuevo más tarde." : "Prueba con otro nombre.")
        } actions: {
            if term.isEmpty {
                Button("Actualizar") { revision += 1 }
                    .buttonStyle(.bordered)
            } else {
                Button("Borrar búsqueda") { query = "" }
                    .buttonStyle(.bordered)
            }
        }
    }

    private func load(_ current: Request, debounce: Bool) async {
        do {
            if debounce { try await Task.sleep(for: .milliseconds(280)) }
            let recipes = try await source.recipes(matching: current.query)
            guard !Task.isCancelled, request == current else { return }
            result = .loaded(recipes)
        } catch {
            guard !Task.isCancelled, request == current else { return }
            result = .failure
        }
        completedRequest = current
    }
}

enum LoadState<Value> {
    case loading
    case loaded(Value)
    case failure
}

struct LoadErrorView: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(message, systemImage: "exclamationmark.circle")
        } actions: {
            Button("Reintentar", action: retry).buttonStyle(.bordered)
        }
    }
}

private struct FavoriteRecipesView: View {
    let source: any RecipeSource
    @Binding var savedRecipes: Data

    var body: some View {
        let entries = FavoriteRecipes(data: savedRecipes).entries
        Group {
            if entries.isEmpty {
                ContentUnavailableView(
                    "Aún no tienes favoritos",
                    systemImage: "heart",
                    description: Text("Abre una receta y toca el corazón para guardarla aquí.")
                )
            } else {
                List(entries) { detail in
                    NavigationLink {
                        RecipeDetailContent(
                            detail: detail,
                            imageURL: source.imageURL(for: detail.recipe),
                            savedRecipes: $savedRecipes
                        )
                    } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(detail.recipe.name).font(.headline)
                            RecipeMetadata(recipe: detail.recipe)
                        }
                        .padding(.vertical, 6)
                    }
                }
            }
        }
        .navigationTitle("Favoritos")
    }
}

private struct RecipeCard: View {
    let recipe: Recipe
    let imageURL: URL?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let imageURL {
                RecipePhoto(url: imageURL, height: 148)
            }
            VStack(alignment: .leading, spacing: 8) {
                if let category = recipe.category, !category.isEmpty {
                    Text(category).font(.subheadline).foregroundStyle(Color.accentColor)
                }
                Text(recipe.name).font(.title3.weight(.semibold))
                RecipeMetadata(recipe: recipe)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
        }
        .background(.background, in: RoundedRectangle(cornerRadius: 18))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay {
            RoundedRectangle(cornerRadius: 18).strokeBorder(.quaternary)
        }
    }
}
