package com.example.minijobhunt.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.minijobhunt.R;
import com.example.minijobhunt.fragments.TaskDetailsBottomSheet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView txtSelectedAddress, txtRadiusLabel;
    private Slider sliderRadius;
    private MaterialButton btnConfirmLocation;
    private String selectedAddress = "";
    private double selectedLat = 0, selectedLng = 0;
    private float selectedRadiusKm = 10f;
    private Circle mCircle;
    private boolean showRadius = false;
    private List<JSONObject> jobs = new ArrayList<>();
    private List<Marker> jobMarkers = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        txtSelectedAddress = findViewById(R.id.txtSelectedAddress);
        txtRadiusLabel = findViewById(R.id.txtRadiusLabel);
        sliderRadius = findViewById(R.id.sliderRadius);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);

        showRadius = getIntent().getBooleanExtra("show_radius", false);
        if (!showRadius) {
            txtRadiusLabel.setVisibility(android.view.View.GONE);
            sliderRadius.setVisibility(android.view.View.GONE);
            btnConfirmLocation.setText("Confirm Location");
        } else {
            String jobsJson = getIntent().getStringExtra("jobs_data");
            if (jobsJson != null) {
                try {
                    JSONArray array = new JSONArray(jobsJson);
                    for (int i = 0; i < array.length(); i++) {
                        jobs.add(array.getJSONObject(i));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            selectedRadiusKm = value;
            txtRadiusLabel.setText("Radius: " + (int) value + " km");
            updateCircle();
        });

        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedAddress.isEmpty() || selectedAddress.equals("Move map to select location")) {
                Toast.makeText(this, "Please select a valid location", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent();
            intent.putExtra("address", selectedAddress);
            intent.putExtra("latitude", selectedLat);
            intent.putExtra("longitude", selectedLng);
            intent.putExtra("radius", selectedRadiusKm);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Custom styling for markers
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 14));
                    // Initialize coordinates at current location immediately
                    selectedLat = current.latitude;
                    selectedLng = current.longitude;
                    getAddressFromLatLng(current);
                    updateCircle();
                } else {
                    useDefaultLocation();
                }
            });
        } else {
            useDefaultLocation();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        mMap.setOnCameraIdleListener(() -> {
            if (!showRadius) {
                LatLng center = mMap.getCameraPosition().target;
                selectedLat = center.latitude;
                selectedLng = center.longitude;
                getAddressFromLatLng(center);
            }
        });

        mMap.setOnMapClickListener(latLng -> {
            if (showRadius) {
                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;
                getAddressFromLatLng(latLng);
                updateCircle();
            }
        });

        mMap.setOnMyLocationButtonClickListener(() -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 14));
                    }
                });
            }
            return false;
        });

        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() instanceof JSONObject) {
                try {
                    JSONObject job = (JSONObject) marker.getTag();
                    TaskDetailsBottomSheet sheet = TaskDetailsBottomSheet.newInstance(job.toString());
                    sheet.show(getSupportFragmentManager(), "TaskDetails");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        });

        showJobMarkers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 12));
                        }
                    });
                }
            }
        }
    }

    private void useDefaultLocation() {
        // Default to Kathmandu
        LatLng defaultLoc = new LatLng(27.7172, 85.3240);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12));
    }

    private void updateCircle() {
        if (mMap == null || !showRadius) return;

        // Ensure we have a valid center
        if (selectedLat == 0 && selectedLng == 0) {
            LatLng center = mMap.getCameraPosition().target;
            selectedLat = center.latitude;
            selectedLng = center.longitude;
        }

        LatLng center = new LatLng(selectedLat, selectedLng);
        if (mCircle == null) {
            mCircle = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(selectedRadiusKm * 1000)
                    .strokeWidth(4)
                    .strokeColor(Color.parseColor("#E64A19"))
                    .fillColor(Color.parseColor("#33FF9800")));
        } else {
            mCircle.setCenter(center);
            mCircle.setRadius(selectedRadiusKm * 1000);
        }
        
        filterMarkers();
    }

    private void showJobMarkers() {
        if (mMap == null || jobs.isEmpty()) return;

        for (JSONObject job : jobs) {
            try {
                // Handle coordinates even if they come as strings from API
                double lat = job.optDouble("latitude", 0);
                double lng = job.optDouble("longitude", 0);
                
                if (lat == 0 && job.has("latitude")) {
                    lat = Double.parseDouble(job.optString("latitude", "0"));
                }
                if (lng == 0 && job.has("longitude")) {
                    lng = Double.parseDouble(job.optString("longitude", "0"));
                }

                if (lat != 0 && lng != 0) {
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(job.optString("title", "Job")));
                    if (marker != null) {
                        marker.setTag(job);
                        jobMarkers.add(marker);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        filterMarkers();
    }

    private void filterMarkers() {
        if (!showRadius) return;
        
        for (Marker marker : jobMarkers) {
            float[] results = new float[1];
            LatLng pos = marker.getPosition();
            
            // selectedLat and selectedLng are the center of the radius
            android.location.Location.distanceBetween(selectedLat, selectedLng, pos.latitude, pos.longitude, results);
            
            // Visibility is based on if job is within radius (in meters)
            marker.setVisible(results[0] <= selectedRadiusKm * 1000);
        }
    }

    private void getAddressFromLatLng(LatLng latLng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                selectedAddress = address.getAddressLine(0);
                txtSelectedAddress.setText(selectedAddress);
            }
        } catch (Exception e) {
            selectedAddress = "Unknown Location";
            txtSelectedAddress.setText(selectedAddress);
        }
    }
}

