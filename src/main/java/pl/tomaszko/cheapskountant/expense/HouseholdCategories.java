package pl.tomaszko.cheapskountant.expense;

import java.util.List;
import java.util.Set;

public final class HouseholdCategories {

    public static final String UNKNOWN = "Unknown";

    public static final List<String> NAMES = List.of(
            "Jedzenie",
            "Jedzenie na mieście",
            "Browar",
            "Kwiatki",
            "Bilet ZTM",
            "Wyjazdy",
            "Telefon",
            "Kino",
            "Słodycze",
            "Lekarstwa/suplementy",
            "Przybory toal.",
            "Alkohol inny",
            "Lekarze",
            "Łachy",
            "Multimedia",
            "Inne wydatki",
            "Materiały biurowe",
            "Pieniądze, po prostu\u2026",
            "Książki i gazety",
            "Rachunki / podatki",
            "Przybory czyszczące",
            "Naczynia,kuchnia",
            "Narzędzia/mat. Eksploatacyjne",
            "Numizmatyka",
            "Elektronika",
            "Dzieciaki",
            "Samochód",
            "Strzelectwo",
            "Ofiary/darowizny",
            "Przesyłki pocztowe/kurier",
            "Oszczędności",
            "Dom/remonty",
            "Opakowania (torby, butelki)",
            "Fermentacja alkoholowa");

    public static final Set<String> RECEIPT_NAMES = Set.copyOf(concatUnknown());

    private HouseholdCategories() {
    }

    public static boolean household(String name) {
        return NAMES.contains(name);
    }

    public static boolean receipt(String name) {
        return RECEIPT_NAMES.contains(name);
    }

    private static List<String> concatUnknown() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>(NAMES);
        names.add(UNKNOWN);
        return names;
    }
}
