import SwiftUI

struct RecipeDetailView: View {
    let summary: Recipe
    let source: any RecipeSource
    @Binding var savedRecipes: Data

    @State private var state: LoadState<RecipeDetail> = .loading
    @State private var revision = 0

    var body: some View {
        Group {
            switch state {
            case .loading:
                ProgressView("Abriendo receta")
            case .failure:
                LoadErrorView(message: "No se pudo abrir la receta.") { revision += 1 }
            case .loaded(let detail):
                RecipeDetailContent(
                    detail: detail,
                    imageURL: source.imageURL(for: detail.recipe) ?? source.imageURL(for: summary),
                    savedRecipes: $savedRecipes
                )
            }
        }
        .navigationTitle("Receta")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .task(id: revision) { await load() }
    }

    private func load() async {
        if case .loaded = state { return }
        state = .loading
        do {
            let detail = try await source.detail(for: summary.id)
            guard !Task.isCancelled else { return }
            state = .loaded(detail)
        } catch {
            guard !Task.isCancelled else { return }
            state = .failure
        }
    }
}

struct RecipeDetailContent: View {
    let detail: RecipeDetail
    let imageURL: URL?
    @Binding var savedRecipes: Data
    @State private var checkedIngredients: Set<Int> = []
    @State private var showSaveError = false

    private var favorites: FavoriteRecipes { FavoriteRecipes(data: savedRecipes) }

    var body: some View {
        let recipe = detail.recipe
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if let imageURL {
                    RecipePhoto(url: imageURL, height: 216)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                }
                VStack(alignment: .leading, spacing: 10) {
                    if let category = recipe.category, !category.isEmpty {
                        Text(category).font(.subheadline.weight(.semibold)).foregroundStyle(Color.accentColor)
                    }
                    Text(recipe.name).font(.largeTitle.bold()).accessibilityAddTraits(.isHeader)
                    RecipeMetadata(recipe: recipe)
                    if let description = recipe.description, !description.isEmpty {
                        Text(description).foregroundStyle(.secondary)
                    }
                }
                if !detail.ingredients.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        sectionTitle("Ingredientes")
                        ForEach(detail.ingredients.indices, id: \.self) { index in
                            ingredientRow(at: index)
                            if index < detail.ingredients.count - 1 { Divider() }
                        }
                    }
                }
                if !detail.steps.isEmpty {
                    VStack(alignment: .leading, spacing: 16) {
                        sectionTitle("Preparación")
                        ForEach(detail.steps.indices, id: \.self) { index in
                            let step = detail.steps[index]
                            HStack(alignment: .firstTextBaseline, spacing: 12) {
                                Text("\(step.number).").fontWeight(.semibold).foregroundStyle(Color.accentColor)
                                Text(step.instruction)
                            }
                            .accessibilityElement(children: .combine)
                        }
                    }
                }
                if let tip = recipe.tip, !tip.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        sectionTitle("Consejo GourMeet")
                        Text(tip)
                    }
                }
            }
            .frame(maxWidth: 680, alignment: .leading)
            .padding(16)
            .frame(maxWidth: .infinity)
        }
        .navigationTitle("Receta")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            let isSaved = favorites.contains(detail.id)
            Button {
                do {
                    savedRecipes = try favorites.toggling(detail)
                } catch {
                    showSaveError = true
                }
            } label: {
                Label(isSaved ? "Quitar de favoritos" : "Guardar en favoritos",
                      systemImage: isSaved ? "heart.fill" : "heart")
            }
            ShareLink(item: shareText(detail)) {
                Label("Compartir receta", systemImage: "square.and.arrow.up")
            }
        }
        .alert("No se pudo actualizar Favoritos", isPresented: $showSaveError) {
            Button("Aceptar", role: .cancel) {}
        }
    }

    private func ingredientRow(at index: Int) -> some View {
        let ingredient = detail.ingredients[index]
        let isChecked = checkedIngredients.contains(index)
        return Button {
            if isChecked {
                checkedIngredients.remove(index)
            } else {
                checkedIngredients.insert(index)
            }
        } label: {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: isChecked ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(isChecked ? Color.accentColor : .secondary)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 4) {
                    Text(ingredient.name).strikethrough(isChecked)
                    if let amount = ingredient.amount, !amount.isEmpty {
                        Text(amount).font(.subheadline).foregroundStyle(.secondary)
                    }
                }
            }
            .padding(.vertical, 6)
            .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityValue(isChecked ? "Listo" : "Pendiente")
        .accessibilityHint("Cambia el estado del ingrediente")
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title).font(.title2.bold()).accessibilityAddTraits(.isHeader)
    }

    private func shareText(_ detail: RecipeDetail) -> String {
        let ingredients = detail.ingredients.map { ingredient in
            [ingredient.name, ingredient.amount].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
        }
        let steps = detail.steps.map { "\($0.number). \($0.instruction)" }
        let sections: [String?] = [
            detail.recipe.name,
            ingredients.isEmpty ? nil : (["Ingredientes"] + ingredients).joined(separator: "\n"),
            steps.isEmpty ? nil : (["Preparación"] + steps).joined(separator: "\n")
        ]
        return sections.compactMap { $0 }.joined(separator: "\n\n")
    }
}

struct RecipeMetadata: View {
    let recipe: Recipe

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 12) { items }.fixedSize(horizontal: true, vertical: false)
            VStack(alignment: .leading, spacing: 6) { items }
        }
        .font(.subheadline)
        .foregroundStyle(.secondary)
    }

    @ViewBuilder private var items: some View {
        if let minutes = recipe.minutes, minutes > 0 {
            Label("\(minutes) min", systemImage: "clock")
        }
        if let difficulty = recipe.difficulty, !difficulty.isEmpty {
            Label(difficulty, systemImage: "chart.bar")
        }
        if let portions = recipe.portions, portions > 0 {
            Label("\(portions) porciones", systemImage: "person.2")
        }
    }
}

struct RecipePhoto: View {
    let url: URL?
    let height: CGFloat

    var body: some View {
        Rectangle()
            .fill(Color.accentColor.opacity(0.09))
            .overlay {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                    } else if url != nil, case .empty = phase {
                        ProgressView()
                    } else {
                        Image(systemName: "fork.knife")
                            .font(.title)
                            .foregroundStyle(Color.accentColor)
                    }
                }
            }
            .frame(height: height)
            .clipped()
            .accessibilityHidden(true)
    }
}
