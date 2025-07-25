package app.grapheneos.geocoder

import android.location.Address
import java.util.Locale

interface Geocoder {
    fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        maxResults: Int,
        preferredLocale: Locale,
    ): List<Address>
}
