package app.grapheneos.geocoder

import android.content.Context
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
}
