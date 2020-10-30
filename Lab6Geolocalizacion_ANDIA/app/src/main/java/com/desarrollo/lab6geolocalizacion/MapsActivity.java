package com.desarrollo.lab6geolocalizacion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.icu.text.DecimalFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager UbicacionManager;
    private Location Ubicacion;
    private static final String TAG = "Estilo del mapa";
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    //UBICACIONES DE DESTINO
    private LatLng mMapJapon = new LatLng(35.680513, 139.769051);
    private LatLng mMapAlemania = new LatLng(52.516934, 13.403190);
    private LatLng mMapItalia = new LatLng(41.902609, 12.494847);
    private LatLng mMapFrancia = new LatLng(48.843489, 2.355331);

    //CREAR LA VARIABLE UbiDestino QUE GUARDARA LOS PUNTOS FINALES
    Location UbiDestino = new Location("Destino");

    //ORIGEN
    private double UbiActuLat = 0;
    private double UbiActuLng = 0;
    private Marker marcador;
    private LatLng coordenadas;

    //DESTINO
    private double UbiFinLat = 0;
    private double UbiFinLng = 0;

    //VERIFICADOR DE VARIABLE KILOMETROS
    private int VerKm = 1000;

    //FORMATO DE LA DISTANCIA
    DecimalFormat formatoKm = new DecimalFormat("#.00");
    DecimalFormat formatom = new DecimalFormat("#");

    //MI UBICACION
    Location UbiOrigen = new Location("Origen");

    //BOTON AUXILIAR
    private Button btn_verdatos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, mapFragment).commit();
        mapFragment.getMapAsync(this);



        btn_verdatos = (Button) findViewById(R.id.btn_calcular);
        btn_verdatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ASIGNAR NUESTRA UBICACION A LA VARIABLE UbiOrigen
                UbiOrigen.setLatitude(UbiActuLat);
                UbiOrigen.setLongitude(UbiActuLng);

                UbiDestino.setLatitude(UbiFinLat);
                UbiDestino.setLongitude(UbiFinLng);

                //HALLAR DISTANCIA Y MOSTRARLA 2

                if (UbiDestino != null && UbiOrigen != null) {
                    CalcularDistanciaAlPulsar(UbiDestino, UbiOrigen);
                }
            }
        });

        btn_verdatos.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_options, menu);
        //inflater.inflate(R.menu.map_distancia, menu);
        return true;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        MiUbicacion();
        setOnMapClick(mMap);
        setMapLongClick(mMap);
        setPoiClick(mMap);
        setInfoWindowClickToPanorama(mMap);
        enableMyLocation();
    }

    //LIMPIAR EL MAPA
    private void setOnMapClick(final GoogleMap map){
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.clear();
                MiUbicacion();
            }
        });
    }

    //AGREGA MARCADOR A MI UBICACION ACTUAL
    private void AgregarMarcadorActual(double UbiActuLat, double UbiActuLng) {
        coordenadas = new LatLng(UbiActuLat, UbiActuLng);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 16);
        if (marcador != null) marcador.remove();
        mMap.addMarker(new MarkerOptions().position(coordenadas).title("Su ubicacion actual").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
        mMap.animateCamera(miUbicacion);
    }

    //OBTIENE LONGITUD LATITUD DE MI UBICACION
    private void ActualizarMiUbicacion(Location location) {
        if (location != null) {
            UbiActuLat = location.getLatitude();
            UbiActuLng = location.getLongitude();
            AgregarMarcadorActual(UbiActuLat, UbiActuLng);
        }
    }

    //METODOS DE MI UBICACION
    LocationListener locListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            ActualizarMiUbicacion(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    //OBTENER MI UBICACION
    public void MiUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,500,0, locListener);
        ActualizarMiUbicacion(location);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LimpiarDatos();
        //ASIGNAR NUESTRA UBICACION A LA VARIABLE UbiOrigen
        UbiOrigen.setLatitude(UbiActuLat);
        UbiOrigen.setLongitude(UbiActuLng);

        switch (item.getItemId()) {

            //CASO JAPON

            case R.id.normal_map:
                LimpiarDatos();
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                //COLOCAR UN MARCADOR EN JAPON
                mMap.addMarker(new MarkerOptions().position(mMapJapon).title("Japon"));

                //ASIGNAR LONGITUDES Y LATITUDES DE DESTINO
                UbiDestino.setLatitude(mMapJapon.latitude);
                UbiDestino.setLongitude(mMapJapon.longitude);

                //HALLAR DISTANCIA Y MOSTRARLA
                Toast.makeText(getApplicationContext(), "De "+  ObtenerCiudadPais() + " a Japon son: " + String.valueOf(formatoKm.format(CalcularDistancia(UbiDestino, UbiOrigen)))+" Km",Toast.LENGTH_LONG).show();
                return true;

            //CASO ITALIA

            case R.id.hybrid_map:
                LimpiarDatos();
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                //COLOCAR UN MARCADOR EN ITALIA
                mMap.addMarker(new MarkerOptions().position(mMapItalia).title("Italia"));

                //ASIGNAR LONGITUDES Y LATITUDES DE DESTINO
                UbiDestino.setLatitude(mMapItalia.latitude);
                UbiDestino.setLongitude(mMapItalia.longitude);

                //HALLAR DISTANCIA Y MOSTRARLA
                Toast.makeText(getApplicationContext(), "De "+  ObtenerCiudadPais() + " a Italia son: " + String.valueOf(formatoKm.format(CalcularDistancia(UbiDestino, UbiOrigen)))+" Km",Toast.LENGTH_LONG).show();
                return true;

            //CASO ALEMANIA

            case R.id.satellite_map:
                LimpiarDatos();
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                //COLOCAR UN MARCADOR EN ALEMANIA
                mMap.addMarker(new MarkerOptions().position(mMapAlemania).title("Alemania"));

                //ASIGNAR LONGITUDES Y LATITUDES DE DESTINO
                UbiDestino.setLatitude(mMapAlemania.latitude);
                UbiDestino.setLongitude(mMapAlemania.longitude);

                //HALLAR DISTANCIA Y MOSTRARLA
                Toast.makeText(getApplicationContext(), "De " + ObtenerCiudadPais() + " a Alemania son: " + String.valueOf(formatoKm.format(CalcularDistancia(UbiDestino, UbiOrigen)))+" Km",Toast.LENGTH_LONG).show();
                return true;

            //CASO FRANCIA

            case R.id.terrain_map:
                LimpiarDatos();
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

                //COLOCAR UN MARCADOR EN FRANCIA
                mMap.addMarker(new MarkerOptions().position(mMapFrancia).title("Francia"));

                //ASIGNAR LONGITUDES Y LATITUDES DE DESTINO
                UbiDestino.setLatitude(mMapFrancia.latitude);
                UbiDestino.setLongitude(mMapFrancia.longitude);

                //HALLAR DISTANCIA Y MOSTRARLA
                Toast.makeText(getApplicationContext(), "De "+ ObtenerCiudadPais() + " a Francia son: " + String.valueOf(formatoKm.format(CalcularDistancia(UbiDestino, UbiOrigen))) +" Km",Toast.LENGTH_LONG).show();
                return true;

            //CASO UBICACION CON POI SELECCION

            case R.id.calcular:
                LimpiarDatos();
                UbiDestino.setLatitude(UbiFinLat);  //latitud
                UbiDestino.setLongitude(UbiFinLng); //longitud

                //HALLAR DISTANCIA Y MOSTRARLA 2
                CalcularDistanciaAlPulsar(UbiDestino, UbiOrigen);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //OBTENER LADO SEGUN EL METODO DE SELECCION POI
    private String ObtenerCiudadPais2() {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(UbiFinLat, UbiFinLng, 1);
            String Direccion = addresses.get(0).getAddressLine(0);
            return Direccion;

        } catch (IOException e) {
            return e.getMessage();
        }
    }

    //OBTENER EL NOMBRE DE LA CIUDAD Y PAIS DONDE SE UBICA
    private String ObtenerCiudadPais() {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(UbiActuLat, UbiActuLng, 1);
            String Direccion = addresses.get(0).getAddressLine(0);
            return Direccion;

        } catch (IOException e) {
            return e.getMessage();
        }
    }

    //OBTENER LA DISTANCIA EXACTA
    private double CalcularDistancia(Location ubiDestino, Location ubiOrigen) {
        double DistanciaOriFin = 0;
        DistanciaOriFin = ubiOrigen.distanceTo(ubiDestino) / VerKm;
        return DistanciaOriFin;
    }

    private void setInfoWindowClickToPanorama(GoogleMap mMap) {
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (marker.getTag() == "poi") {
                    StreetViewPanoramaOptions options = new StreetViewPanoramaOptions().position(marker.getPosition());
                    SupportStreetViewPanoramaFragment streetViewFragment = SupportStreetViewPanoramaFragment.newInstance(options);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,streetViewFragment).addToBackStack(null).commit();
                }
            }
        });
    }

    //DA EL PERMISO / SI LO REQUIERE
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation();
                    break;
                }
        }
    }

    //METODO PARA PEDIR PERMISO
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    //PONE UN MARCADOR EN PLAZAS QUE GOOGLEMAP RECONOZCA
    private void setPoiClick(final GoogleMap mMap) {
        mMap.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
            @Override
            public void onPoiClick(PointOfInterest poi) {
                Marker poiMarker = mMap.addMarker(new MarkerOptions().position(poi.latLng).title(poi.name));
                poiMarker.showInfoWindow();
                poiMarker.setTag("poi");
            }
        });
    }

    //AL APRETAR POR UN CORTO TIEMPO COLOCA UN MARCADOR CON SU LATITUD Y LONGITUD
    private void setMapLongClick(final GoogleMap mMap) {

        //ASIGNAR NUESTRA UBICACION A LA VARIABLE UbiOrigen
        UbiOrigen.setLatitude(UbiActuLat);
        UbiOrigen.setLongitude(UbiActuLng);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                String snippet = String.format(Locale.getDefault(),"Lat: %1$.5f, Long: %2$.5f",latLng.latitude,latLng.longitude);
                //LE ASIGNAMOS COMO UBICACIONES FINALES
                LimpiarDatos();
                UbiFinLat = latLng.latitude;
                UbiFinLng = latLng.longitude;
                mMap.addMarker(new MarkerOptions().position(latLng).title(ObtenerCiudadPais2()).snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                DibujarTrazo();
                btn_verdatos.setEnabled(true);
            }
        });
    }

    private void DibujarTrazo() {
        Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(UbiOrigen.getLatitude(), UbiOrigen.getLongitude()), new LatLng(UbiFinLat, UbiFinLng))
                .width(5)
                .color(Color.RED)
                .geodesic(true));
    }

    private void LimpiarDatos() {
        btn_verdatos.setEnabled(false);
        mMap.clear();
        MiUbicacion();
        AgregarMarcadorActual(UbiActuLat, UbiActuLng);
    }

    //OBTENER LA DISTANCIA EXACTA CON LONG
    private void CalcularDistanciaAlPulsar(Location ubiDestino, Location ubiOrigen) {
        double DistanciaOriFin = 0;
        DistanciaOriFin = ubiOrigen.distanceTo(ubiDestino);

        if (DistanciaOriFin < 1000) {
            Toast.makeText(getApplicationContext(), "De " + ObtenerCiudadPais() + " a " + ObtenerCiudadPais2() + " son: " + formatom.format(DistanciaOriFin) + " metros", Toast.LENGTH_LONG).show();
        } else {
            DistanciaOriFin = (DistanciaOriFin / VerKm);
            Toast.makeText(getApplicationContext(), "De " + ObtenerCiudadPais() + " a " + ObtenerCiudadPais2() + " son: " + formatoKm.format(DistanciaOriFin) + " Km", Toast.LENGTH_LONG).show();
        }
    }
}
