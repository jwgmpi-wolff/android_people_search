package com.jerrywolff.androidpeoplesearch.search

import org.json.JSONObject

class CountyCatalog(private val client: PublicApiClient = HttpPublicApiClient()) {
    suspend fun citiesForState(state: String): List<String> {
        val stateCode = US_STATE_FIPS[state] ?: return emptyList()
        val url = "https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/Places_CouSub_ConCity_SubMCD/MapServer/0/query" +
            "?where=STATE%3D%27$stateCode%27&outFields=BASENAME&returnGeometry=false&orderByFields=BASENAME&f=json"
        return featureNames(client.get(url), "BASENAME")
    }

    suspend fun countiesForState(state: String): List<String> {
        val stateCode = US_STATE_FIPS[state] ?: return emptyList()
        val url = "https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/State_County/MapServer/1/query" +
            "?where=STATE%3D%27$stateCode%27&outFields=NAME&returnGeometry=false&orderByFields=NAME&f=json"
        return featureNames(client.get(url), "NAME")
    }

    private fun featureNames(response: String, field: String): List<String> {
        val features = JSONObject(response).getJSONArray("features")
        return (0 until features.length()).mapNotNull { index ->
            features.getJSONObject(index).getJSONObject("attributes").optString(field)
                .trim().takeIf(String::isNotBlank)
        }.distinct().sorted()
    }
}

fun usStateNames(): List<String> = US_STATE_FIPS.keys.toList()

private val US_STATE_FIPS = linkedMapOf(
    "Alabama" to "01", "Alaska" to "02", "Arizona" to "04", "Arkansas" to "05",
    "California" to "06", "Colorado" to "08", "Connecticut" to "09", "Delaware" to "10",
    "District of Columbia" to "11", "Florida" to "12", "Georgia" to "13", "Hawaii" to "15",
    "Idaho" to "16", "Illinois" to "17", "Indiana" to "18", "Iowa" to "19", "Kansas" to "20",
    "Kentucky" to "21", "Louisiana" to "22", "Maine" to "23", "Maryland" to "24",
    "Massachusetts" to "25", "Michigan" to "26", "Minnesota" to "27", "Mississippi" to "28",
    "Missouri" to "29", "Montana" to "30", "Nebraska" to "31", "Nevada" to "32",
    "New Hampshire" to "33", "New Jersey" to "34", "New Mexico" to "35", "New York" to "36",
    "North Carolina" to "37", "North Dakota" to "38", "Ohio" to "39", "Oklahoma" to "40",
    "Oregon" to "41", "Pennsylvania" to "42", "Rhode Island" to "44", "South Carolina" to "45",
    "South Dakota" to "46", "Tennessee" to "47", "Texas" to "48", "Utah" to "49",
    "Vermont" to "50", "Virginia" to "51", "Washington" to "53", "West Virginia" to "54",
    "Wisconsin" to "55", "Wyoming" to "56",
)
