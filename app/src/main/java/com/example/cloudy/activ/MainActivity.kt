package com.example.cloudy.activ

import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.cloudy.Model.OpenWeatherMap
import com.example.cloudy.R
import com.example.cloudy.Common.location
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {
    val PERMISSION_REQUEST_CODE = 1001
    val PLAY_SERVICE_RESOLUTION_REQUEST = 1000
    var mGoogleApiClient: GoogleApiClient? = null
    var mLocationRequest: LocationRequest? = null
    internal var openWeatherMap = OpenWeatherMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions();
        if(checkplayService())
            buildGoogleApiClient()

    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), PERMISSION_REQUEST_CODE
            )

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkplayService()) {
                        buildGoogleApiClient()
                    }

                }
            }
        }
    }

    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API).build()
    }

    private fun checkplayService(): Boolean {
        val resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

                GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    PLAY_SERVICE_RESOLUTION_REQUEST
                ).show()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Application is not supported",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return false
        }
        return true

    }

    override fun onConnected(p0: Bundle?) {
        createLocationRequest();
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 10000
        mLocationRequest!!.fastestInterval = 5000
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
            mGoogleApiClient,
            mLocationRequest,
            this
        )

    }

    override fun onConnectionSuspended(p0: Int) {
        mGoogleApiClient!!.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i("ERROR", "operation failed:" + p0.errorCode)
    }

    override fun onLocationChanged(location: Location?) {
        GetWeather().execute(com.example.cloudy.Common.location.apiRequest(location!!.latitude.toString(),location.longitude!!.toString()))
    }

    override fun onStart() {
        super.onStart()
        if (mGoogleApiClient != null)
            mGoogleApiClient!!.connect()
    }

    override fun onDestroy() {
        mGoogleApiClient!!.disconnect()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        checkplayService()
    }

    private inner class GetWeather: AsyncTask<String, Void, String>()
    {
        internal var pd= ProgressDialog(this@MainActivity)
        override fun onPreExecute() {
            super.onPreExecute()
            pd.setTitle("Please wait...")
            pd.show()
        }

        override fun doInBackground(vararg params: String?): String {
            var stream: String? = null
            var urlString = params[0]

            var http = com.example.cloudy.Common.Helper()
            stream=http.getHTTPData(urlString)
            return stream
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
                if(result!!.contains("Error:Not found city")) {
                    pd.dismiss()
                    return
                }
           val gson=Gson()
           val mType = object:TypeToken<OpenWeatherMap>(){}.type

            openWeatherMap = gson.fromJson<OpenWeatherMap>(result,mType)
            pd.dismiss()

            txtcity.text="${openWeatherMap.name},${openWeatherMap.sys!!.country}"
            txtLastUpdate.text="Last Updated:${location.dateNow}"
            txtDescription.text="${openWeatherMap.weather!![0].description}"
            txtTime.text="${location.unixTimeStampToDateTime(openWeatherMap.sys!!.sunrise)}/${location.unixTimeStampToDateTime(openWeatherMap.sys!!.sunset)}"
            txtHumidity.text="${openWeatherMap.main!!.humidity}"
            txtCelsius.text="${openWeatherMap.main!!.temp} °C"
            Picasso.with(this@MainActivity)
                .load(location.getImage(openWeatherMap.weather!![0].icon!!))
                .into(imageView)

         }

    }

}