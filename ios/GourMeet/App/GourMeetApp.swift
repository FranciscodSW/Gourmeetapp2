import SwiftUI

@main
struct GourMeetApp: App {
    private let address = ProcessInfo.processInfo.environment["GOURMEET_API_URL"]
    private let accent = Color(red: 20 / 255, green: 92 / 255, blue: 197 / 255)

    var body: some Scene {
        WindowGroup {
            Group {
                if let address, !address.isEmpty {
                    if let api = try? RecipeAPI(address: address) {
                        RecipeCatalogView(source: api, isDemo: false)
                    } else {
                        ContentUnavailableView("Dirección inválida", systemImage: "exclamationmark.circle",
                                               description: Text("Revisa GOURMEET_API_URL en el esquema de Xcode."))
                    }
                } else {
                    RecipeCatalogView(source: DemoRecipes(), isDemo: true)
                }
            }
            .tint(accent)
            .environment(\.locale, Locale(identifier: "es_MX"))
        }
    }
}
