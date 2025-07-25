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

private const val TAG = "GeocodeProviderImpl"

class GeocodeProviderImpl(private val context: Context) : GeocodeProviderBase(context, TAG) {
    override fun onForwardGeocode(
        request: ForwardGeocodeRequest,
        callback: OutcomeReceiver<MutableList<Address>, Throwable>
    ) {
        TODO("Not yet implemented")
    }

    override fun onReverseGeocode(
        request: ReverseGeocodeRequest,
        callback: OutcomeReceiver<MutableList<Address>, Throwable>
    ) {
        TODO("Not yet implemented")
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
