package app.grapheneos.geocoder

import android.app.AppGlobals
import android.content.Context
import android.ext.settings.GeocoderSettings.GEOCODER_DISABLED
import android.ext.settings.GeocoderSettings.GEOCODER_SERVER_GRAPHENEOS_PROXY
import android.ext.settings.GeocoderSettings.GEOCODER_SERVER_NOMINATIM
import android.ext.settings.GeocoderSettings.GEOCODER_SETTING
import android.location.Address
import android.os.Bundle
import android.util.Log
import androidx.core.os.toPersistableBundle
import java.io.IOException
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlinx.serialization.json.Json
import org.grapheneos.tls.ModernTLSSocketFactory

private const val TAG = "NominatimGeocoder"
private const val EXTRA_VERBOSE_TAG = "NominatimGeocoderVV"

class NominatimGeocoder : Geocoder {
    private val tlsSocketFactory = ModernTLSSocketFactory()

    @Throws(IOException::class)
    override fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        maxResults: Int,
        preferredLocale: Locale,
    ): List<Address> {
        verboseLog(TAG) {
            "reverse geocode parameters: latitude: $latitude, longitude: $longitude, " +
                    "maxResults: $maxResults, preferredLocale: $preferredLocale"
        }
        val (baseUrl, enforceModernTls) = getServerBaseUrl()

        val url =
            // Nominatim doesn't support returning more than 1 reverse geocoding result
            URL("$baseUrl/reverse?format=geocodejson&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1&extratags=1")
        val connection = url.openConnection() as HttpsURLConnection
        try {
            if (enforceModernTls) {
                connection.sslSocketFactory = tlsSocketFactory
            }
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept-Language", preferredLocale.toLanguageTag())
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException("non-200 response code: $responseCode")
            }
            val responseBytes = connection.inputStream.use { inputStream ->
                inputStream.readAllBytes()
            }
            val responseString = responseBytes.decodeToString()

            // occurs when coordinates are in the middle of the ocean, presumably because there were
            // no results
            if (responseString == "{\"error\":\"Unable to geocode\"}") {
                return emptyList()
            }

            val response = run {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<GeocodeJson>(responseString)
            }
            verboseLog(TAG) {
                "byte size: ${responseBytes.size}"
            }
            if (Log.isLoggable(EXTRA_VERBOSE_TAG, Log.VERBOSE)) {
                Log.v(EXTRA_VERBOSE_TAG, "response headers: " + connection.headerFields)
            }
            verboseLog(TAG) {
                "response features list size: ${response.features?.size}"
            }

            val result = mutableListOf<Address>()
            response.features?.forEach { feature ->
                val extra = feature.properties.geocoding.extra?.toMutableMap()
                // we don't know which locale was actually used, so we just assume it was the one
                // we requested
                val address = Address(preferredLocale)
                address.latitude = feature.geometry.coordinates[1]
                address.longitude = feature.geometry.coordinates[0]
                address.postalCode = feature.properties.geocoding.postcode
                address.featureName = feature.properties.geocoding.name
                address.countryName = feature.properties.geocoding.country
                address.countryCode = feature.properties.geocoding.countryCode
                address.adminArea = feature.properties.geocoding.state
                address.subAdminArea = feature.properties.geocoding.county
                address.locality =
                    feature.properties.geocoding.city ?: feature.properties.geocoding.locality
                address.subLocality = feature.properties.geocoding.district
                address.thoroughfare = feature.properties.geocoding.street
                address.subThoroughfare = feature.properties.geocoding.houseNumber
                address.url = extra?.remove("website")
                address.phone = extra?.remove("phone")
                address.extras = extra?.let { Bundle(it.toPersistableBundle()) }

                result.add(address)
            }
            if (Log.isLoggable(EXTRA_VERBOSE_TAG, Log.VERBOSE)) {
                result.forEachIndexed { i, address ->
                    Log.v(
                        EXTRA_VERBOSE_TAG, "address[$i]: $address"
                    )
                }
            }
            return result
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun getServerBaseUrl(): Pair<URL, Boolean> {
        val context: Context = AppGlobals.getInitialApplication()
        val setting = GEOCODER_SETTING.get(context)
        return when (setting) {
            GEOCODER_SERVER_GRAPHENEOS_PROXY -> Pair(URL("https://nominatim.grapheneos.org"), true)

            GEOCODER_SERVER_NOMINATIM -> Pair(URL("https://nominatim.openstreetmap.org"), true)

            GEOCODER_DISABLED ->
                // geocoder can be disabled by the user at any point
                throw IOException("geocoder setting became disabled")

            else -> throw IllegalStateException("unexpected URL setting: $setting")
        }
    }
}
