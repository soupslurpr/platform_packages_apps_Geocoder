package app.grapheneos.geocoder

import android.content.Context
import android.ext.settings.GeocoderSettings.GEOCODER_DISABLED
import android.ext.settings.GeocoderSettings.GEOCODER_SERVER_GRAPHENEOS_PROXY
import android.ext.settings.GeocoderSettings.GEOCODER_SERVER_NOMINATIM
import android.ext.settings.GeocoderSettings.GEOCODER_SETTING
import android.location.Address
import android.location.provider.ForwardGeocodeRequest
import android.location.provider.GeocodeProviderBase
import android.location.provider.ReverseGeocodeRequest
import android.os.OutcomeReceiver
import android.util.Log
import java.io.IOException

private const val TAG = "GeocodeProviderImpl"

class GeocodeProviderImpl(private val context: Context) : GeocodeProviderBase(context, TAG) {
    override fun onForwardGeocode(
        request: ForwardGeocodeRequest,
        callback: OutcomeReceiver<List<Address>, Throwable>
    ) {
        try {
            val geocoder = getGeocoder()
            return callback.onResult(
                geocoder.forwardGeocode(
                    request.locationName,
                    request.lowerLeftLatitude,
                    request.lowerLeftLongitude,
                    request.upperRightLatitude,
                    request.upperRightLongitude,
                    request.maxResults,
                    request.locale,
                )
            )
        } catch (e: IOException) {
            Log.d(TAG, "unable to forward geocode: $e")
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "", e)
            }
            return callback.onError(e)
        }
    }

    override fun onReverseGeocode(
        request: ReverseGeocodeRequest,
        callback: OutcomeReceiver<List<Address>, Throwable>
    ) {
        try {
            val geocoder = getGeocoder()
            return callback.onResult(
                geocoder.reverseGeocode(
                    request.latitude,
                    request.longitude,
                    request.maxResults,
                    request.locale
                )
            )
        } catch (e: IOException) {
            Log.d(TAG, "unable to reverse geocode: $e")
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "", e)
            }
            return callback.onError(e)
        }
    }

    @Throws(IOException::class)
    private fun getGeocoder(): Geocoder {
        val setting = GEOCODER_SETTING.get(context)
        return when (setting) {
            GEOCODER_SERVER_GRAPHENEOS_PROXY, GEOCODER_SERVER_NOMINATIM -> NominatimGeocoder()

            GEOCODER_DISABLED -> throw IOException("geocoder setting is disabled")

            else -> throw IllegalStateException("unexpected URL setting: $setting")
        }
    }
}
