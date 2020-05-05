package com.transport.vehicleOwner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.transport.R;
import com.transport.adapter.DrawerAdapter;
import com.transport.adapter.OwnerVehicleDialogAdapter;
import com.transport.adapter.VehicleDialogAdapter;
import com.transport.api.ApiClient;
import com.transport.api.model.AddUpdateOrder;
import com.transport.api.model.OwnerVehicle;
import com.transport.api.model.RowMaterials;
import com.transport.api.model.Vehicles;
import com.transport.app.GlideApp;
import com.transport.databinding.ActivityOwnerHomeBinding;
import com.transport.databinding.DialogRowMaterialBinding;
import com.transport.home.HomeActivity;
import com.transport.login.LoginActivity;
import com.transport.myOrder.MyOrderActivity;
import com.transport.utils.AppUtils;
import com.transport.utils.Constants;
import com.transport.utils.DialogUtil;
import com.transport.utils.EndlessRecyclerViewScrollListener;
import com.transport.utils.SharedPrefs;
import com.transport.vehicleOwner.receiveOrder.ReceiveOrderActivity;
import com.transport.widget.NestedScrollableViewHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.reactivex.disposables.CompositeDisposable;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.transport.api.ApiClient.applySchedulers;
import static com.transport.api.ApiClient.getApiService;
import static com.transport.utils.Constants.AUTOCOMPLETE_REQUEST_CODE;

@RuntimePermissions
public class OwnerHomeActivity extends AppCompatActivity {
    private final int MAX_LIMIT = 20;
    private AppCompatActivity activity = this;
    private ActivityOwnerHomeBinding binding;
    private CompositeDisposable disposable = new CompositeDisposable();

    //    private Vehicles.VehicleType selectedVehicle;
//    private Vehicles.VehicleType humanSelectedVehicle;
    private long orderDate = 0;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH);
    private DrawerAdapter drawerAdapter;
    private final String[] weights = new String[]{"gm", "kg", "tone"};
    private List<Vehicles.VehicleType> vehicleList;

    /**
     * For Location Update
     */
//    private FusedLocationProviderClient mFusedLocationClient;
//    private SettingsClient mSettingsClient;
//    private LocationRequest mLocationRequest;
//    private LocationSettingsRequest mLocationSettingsRequest;
//    private LocationCallback mLocationCallback;
//    private GoogleMap map;
//    private Geocoder mGeocoder;
//    private Marker mMarker;

    public LatLng mCurrentLatLng;
    public LatLng fromLatLng;
    public LatLng toLatLng;

    private OwnerVehicleDialogAdapter ownerVehicleDialogAdapter;

    private OwnerVehicle.Vehicle selectedOwnerVehicleGoods;
    private OwnerVehicle.Vehicle selectedOwnerVehicleHumans;
    private boolean moreOwnerVehicleLoad = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(activity, R.layout.activity_owner_home);
        binding.mapView.onCreate(savedInstanceState);
        //   TODO: 8/4/20  uncomment method stuff
        initLocationApi();
        // TODO: 8/4/20 remove both
        fromLatLng = new LatLng(24.55, 72.55);
        toLatLng = new LatLng(24.65, 72.85);
        init();
        initListener();
        initDrawer();

    }

    @Override
    protected void onResume() {
        super.onResume();
//        binding.mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
        //        binding.mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
//        binding.mapView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    private void initLocationApi() {
     /*   binding.mapView.onResume();
        mGeocoder = new Geocoder(activity, Locale.getDefault());


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                if (locationResult.getLastLocation() != null) {
                    Timber.d(locationResult.getLastLocation().toString());

                    boolean needToLoadAgain = mCurrentLatLng == null;

                    mCurrentLatLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                    if (needToLoadAgain)
                        setCurrentLocationMarker(false);
//                    SharedPrefs.setString(activity, SharedPrefs.PREF_LAST_LAT_LNG, mCurrentLatLng.latitude + "," + mCurrentLatLng.longitude);

                    //stopLocationUpdates();

//                    if (needToLoadAgain) {
//                        bnv.setSelectedItemId(R.id.menuHome);
//                    }
                }
            }
        };

        binding.mapView.getMapAsync(googleMap -> {
            map = googleMap;
            map.setMinZoomPreference(2);
            map.setIndoorEnabled(true);

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);

                UiSettings uiSettings = map.getUiSettings();
                uiSettings.setIndoorLevelPickerEnabled(true);
                uiSettings.setMyLocationButtonEnabled(true);
                uiSettings.setMapToolbarEnabled(true);
                uiSettings.setCompassEnabled(true);
                uiSettings.setZoomControlsEnabled(true);


            }
//                getDeviceLocation();


//            if (address == null) return;
//            LatLng latLng = new LatLng(address.getLat(), address.getLong());


//            mMarker = map.addMarker(new MarkerOptions().position(latLng));
//            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(18).build();
//            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//             setCurrentLocationMarker();
        });

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();*/
    }


    private void init() {
        binding.slidingLayout.setScrollableViewHelper(new NestedScrollableViewHelper());
        OwnerHomeActivityPermissionsDispatcher.getCurrentLocationWithPermissionCheck(OwnerHomeActivity.this);
        binding.tvGoods.setSelected(true);
    }

    private void initDrawer() {
        GlideApp.with(activity).load(R.drawable.drawer_bg).into(binding.ivDrawerBg);
        binding.cvHome.setOnClickListener(v -> {
            if (!binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        binding.tvUserName.setText(String.format("Mr. %s", SharedPrefs.getString(activity, SharedPrefs.NAME)));
        binding.tvUserNumber.setText(String.format("Mo. %s", SharedPrefs.getString(activity, SharedPrefs.PHONE)));


        drawerAdapter = new DrawerAdapter((position, drawerItem) -> {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
//            if (drawerAdapter.isSelected(position)) return;

            switch (position) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    Intent intent = new Intent(activity, MyOrderActivity.class);
                    startActivity(intent);
                    break;
                case 3:
                    Intent receiveIntent = new Intent(activity, ReceiveOrderActivity.class);
                    startActivity(receiveIntent);
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
                case 7:
                    break;
                case 8:
                    logout();
                    break;
            }
//            Toast.makeText(activity, drawerItem.getTitle(), Toast.LENGTH_SHORT).show();

        });
        binding.rvList.setAdapter(drawerAdapter);
    }

    private void logout() {
        if (!AppUtils.isNetworkAvailableWithDialog(activity)) return;
        DialogUtil.showProgressDialog(activity, getSupportFragmentManager());

        disposable.add(getApiService().logout(SharedPrefs.getAuthToken(activity), SharedPrefs.getUserId(activity)).compose(applySchedulers())
                .subscribe(statusObjectHolder -> {
                    DialogUtil.dismissProgressDialog();
                    if (statusObjectHolder.getStatus().getCode() == 0) {
                        SharedPrefs.remove(activity, SharedPrefs.AUTH_TOKEN);
                        SharedPrefs.remove(activity, SharedPrefs.USER_ID);
                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                    Toast.makeText(activity, statusObjectHolder.getStatus().getReturnMessage(), Toast.LENGTH_SHORT).show();
                }, throwable -> {
                    DialogUtil.dismissProgressDialog();
                }));
    }

    private void initListener() {
//        binding.tvSelectMaterial.setOnClickListener(v -> openSelectMaterialDialog());
        binding.etTo.setOnClickListener(v -> {
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), getString(R.string.map_key));
            }
            // Create a new Places client instance.
            PlacesClient placesClient = Places.createClient(activity);

//            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
            List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

            // Start the autocomplete intent.
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .setCountry("IN") //NIGERIA
//                    .setTypeFilter(TypeFilter.ADDRESS)
                    .build(activity);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);

          /*  Dialog dialog = new Dialog(activity,android.R.style.Theme_Translucent_NoTitleBar);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            dialog.setContentView(R.layout.fragment_google_place);


            Window window = dialog.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();

            wlp.gravity = Gravity.CENTER;
            wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(wlp);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);




//            AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
//                    getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
            AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                  getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NotNull Place place) {
                    Timber.i("Place: %s", place.toString());
                    toLatLng = place.getLatLng();
                    binding.etTo.setText(place.getAddress());

                }

                @Override
                public void onError(@NotNull Status status) {
                    Timber.i("An error occurred: %s", status);
                    if (!status.isSuccess()) {
                        Toast.makeText(activity, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.show();*/

        });

        binding.tvGoods.setOnClickListener(v -> {
            if (binding.tvGoods.isSelected()) return;
            binding.tvGoods.setSelected(true);
            binding.tvHumans.setSelected(false);
            binding.cvFromData.setVisibility(VISIBLE);
            binding.cvFormData2.setVisibility(GONE);
        });

        binding.tvHumans.setOnClickListener(v -> {
            if (binding.tvHumans.isSelected()) return;
            binding.tvHumans.setSelected(true);
            binding.tvGoods.setSelected(false);
            binding.cvFromData.setVisibility(GONE);
            binding.cvFormData2.setVisibility(VISIBLE);
        });

        binding.fabCurrentLocation.setOnClickListener(v -> setCurrentLocationMarker(true));

        binding.tvSendOrder.setOnClickListener(v -> sendOrder());
        binding.tvSendOrder2.setOnClickListener(v -> sendOrder());

        binding.cvDate.setOnClickListener(v -> selectDate());

        binding.tvSelectWeight.setOnClickListener(v -> {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.select_weight_unit)
                    .setItems(weights, (dialog, which) -> binding.tvSelectWeight.setText(weights[which]))
                    .setPositiveButton(android.R.string.cancel, null)
                    .show();
        });
//        binding.tvSelectSize.setOnClickListener(v -> {
//            new AlertDialog.Builder(activity)
//                    .setTitle(R.string.select_size_unit)
//                    .setItems(sizes, (dialog, which) -> binding.tvSelectSize.setText(sizes[which]))
//                    .setPositiveButton(android.R.string.cancel, null)
//                    .show();
//        });

//        binding.llVehicleSelection.setOnClickListener(v -> getVehicles(false));
//        binding.llVehicleSelection2.setOnClickListener(v -> getVehicles(true));
        binding.llVehicleSelection.setOnClickListener(v -> getYourVehicles(1));
        binding.llVehicleSelection2.setOnClickListener(v -> getYourVehicles(1));

//        binding.tvVehicleSelection.setOnClickListener(v -> getVehicles());
//        binding.tvVehicleSelection2.setOnClickListener(v -> getVehicles());
    }

  /*  private void getVehicles(boolean isHuman) {
        if (vehicleList != null) {
            openVehicleSelectDialog(isHuman);
            return;
        }
        if (!AppUtils.isNetworkAvailableWithDialog(activity)) return;
        DialogUtil.showProgressDialog(activity, getSupportFragmentManager());

      *//*  Map<String, Object> header = new HashMap<>();
        header.put("AuthToken", SharedPrefs.getAuthToken(activity));

        disposable.add(getApiService().getVehicleType(header).compose(applySchedulers())
                .subscribe(vehicles -> {
                    DialogUtil.dismissProgressDialog();
                    if (vehicles.getStatus().getCode() == 0) {
                        vehicleList = vehicles.getVehicleType();
                        openVehicleSelectDialog(isHuman);
                    } else
                        Toast.makeText(activity, vehicles.getStatus().getReturnMessage(), Toast.LENGTH_SHORT).show();
                }, throwable -> {
                    DialogUtil.dismissProgressDialog();
                    throwable.printStackTrace();
                    Toast.makeText(activity, R.string.fail, Toast.LENGTH_SHORT).show();
                }));
*//*
        Map<String, Object> data = new HashMap<>();
        data.put("id", SharedPrefs.getUserId(activity));
        data.put("pageNo", 1);
        data.put("pageSize", 20);
        disposable.add(getApiService().getVehicleByUserId(SharedPrefs.getAuthToken(activity), data)
                .compose(applySchedulers())
                .subscribe(s -> {
                    DialogUtil.dismissProgressDialog();
                    Timber.d("getVehicles: %s", s);
                }, throwable -> {
                }));

    }

    private void openVehicleSelectDialog(boolean isHuman) {
        Dialog dialog = new Dialog(activity);
        DialogRowMaterialBinding dialogRowMaterialBinding = DialogRowMaterialBinding.inflate(getLayoutInflater(), null, false);
        dialog.setContentView(dialogRowMaterialBinding.getRoot());
        Objects.requireNonNull(dialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        dialog.setOnShowListener(dialog1 -> Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)));

        dialog.setCancelable(true);

        for (Vehicles.VehicleType vehicleType : vehicleList) {
            vehicleType.setSelected(isHuman ? humanSelectedVehicle != null && humanSelectedVehicle.getVehicleTypeId().equals(vehicleType.getVehicleTypeId())
                    : selectedVehicle != null && selectedVehicle.getVehicleTypeId().equals(vehicleType.getVehicleTypeId()));
        }


        VehicleDialogAdapter vehicleDialogAdapter = new VehicleDialogAdapter((position, vehicleType) -> {
            if (!isHuman) {
                selectedVehicle = vehicleType;
                binding.tvVehicleSelection.setText(selectedVehicle.getVehicleType());
                GlideApp.with(activity).load(ApiClient.PREFIX + vehicleType.getImage()).into(binding.ivVehicleSelection);
            } else {
                humanSelectedVehicle = vehicleType;
                binding.tvVehicleSelection2.setText(humanSelectedVehicle.getVehicleType());
                GlideApp.with(activity).load(ApiClient.PREFIX + vehicleType.getImage()).into(binding.ivVehicleSelectionHuman);
            }
            dialog.dismiss();
        });

        dialogRowMaterialBinding.tvTitle.setText(R.string.select_vehicle);
        dialogRowMaterialBinding.rvList.setAdapter(vehicleDialogAdapter);

        vehicleDialogAdapter.addData(vehicleList);
        dialog.show();

    }*/

    private void getYourVehicles(int page) {
        if (!AppUtils.isNetworkAvailableWithDialog(activity)) return;
        DialogUtil.showProgressDialog(activity, getSupportFragmentManager());

        Map<String, Object> data = new HashMap<>();
        data.put("id", SharedPrefs.getUserId(activity));
        data.put("pageNo", page);
        data.put("pageSize", MAX_LIMIT);

        if (page == 1) moreOwnerVehicleLoad = true;

        disposable.add(getApiService().getVehicleByUserId(SharedPrefs.getAuthToken(activity), data)
                .compose(applySchedulers())
                .subscribe(ownerVehicle -> {
                    DialogUtil.dismissProgressDialog();
                    if (ownerVehicle.getStatus().getCode() == 0) {
                        for (OwnerVehicle.Vehicle vehicle : ownerVehicle.getVehicle()) {
                            vehicle.setSelected(binding.tvHumans.isSelected() ?
                                    selectedOwnerVehicleHumans != null && selectedOwnerVehicleHumans.getVehicleId().equals(vehicle.getVehicleId())
                                    : selectedOwnerVehicleGoods != null && selectedOwnerVehicleGoods.getVehicleId().equals(vehicle.getVehicleId()));
                        }
                        if (page == 1)
                            openOwnerVehicleSelectDialog(page, ownerVehicle.getVehicle());
                        else ownerVehicleDialogAdapter.addData(page, ownerVehicle.getVehicle());
                    } else
                        Toast.makeText(activity, ownerVehicle.getStatus().getReturnMessage(), Toast.LENGTH_SHORT).show();
                }, throwable -> {
                    DialogUtil.dismissProgressDialog();
                    throwable.printStackTrace();
                    Toast.makeText(activity, R.string.fail, Toast.LENGTH_SHORT).show();
                }));
    }


    private void openOwnerVehicleSelectDialog(int page, List<OwnerVehicle.Vehicle> vehicleList) {
        Dialog dialog = new Dialog(activity);
        DialogRowMaterialBinding dialogRowMaterialBinding = DialogRowMaterialBinding.
                inflate(getLayoutInflater(), null, false);
        dialog.setContentView(dialogRowMaterialBinding.getRoot());
        Objects.requireNonNull(dialog.getWindow()).setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);

        dialog.setOnShowListener(dialog1 ->
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)));

        dialog.setCancelable(true);

        ownerVehicleDialogAdapter = new OwnerVehicleDialogAdapter((position, mVehicle) -> {
            if (binding.tvGoods.isSelected()) {
                selectedOwnerVehicleGoods = mVehicle;
                binding.tvVehicleSelection.setText(mVehicle.getVehicleType());
                GlideApp.with(activity).load(ApiClient.PREFIX + mVehicle.getImage()).into(binding.ivVehicleSelection);
            } else {
                selectedOwnerVehicleHumans = mVehicle;
                binding.tvVehicleSelection2.setText(mVehicle.getVehicleType());
                GlideApp.with(activity).load(ApiClient.PREFIX + mVehicle.getImage()).into(binding.ivVehicleSelection);
            }
            dialog.dismiss();
        });


        GridLayoutManager manager = new GridLayoutManager(activity, 3);
        EndlessRecyclerViewScrollListener scrollListener = new EndlessRecyclerViewScrollListener(manager, 5) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (!moreOwnerVehicleLoad) return;
                getYourVehicles(page);
            }
        };
        dialogRowMaterialBinding.rvList.setLayoutManager(manager);
        dialogRowMaterialBinding.rvList.addOnScrollListener(scrollListener);


        dialogRowMaterialBinding.tvTitle.setText(R.string.select_your_vehicle);
        dialogRowMaterialBinding.rvList.setAdapter(ownerVehicleDialogAdapter);


        ownerVehicleDialogAdapter.addData(page, vehicleList);
        dialog.show();
    }

    private void selectDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        new DatePickerDialog(activity, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            new TimePickerDialog(activity, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                orderDate = calendar.getTimeInMillis();
                binding.tvDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void sendOrder() {
        if (!AppUtils.isNetworkAvailableWithDialog(activity)) return;
        if (!isValid()) return;
        DialogUtil.showProgressDialog(activity, getSupportFragmentManager());

/*
       List<AddUpdateOrder.ProductList> productList = new ArrayList<>();
        productList.add(new AddUpdateOrder.ProductList(binding.tvGoods.isSelected() ?
                (binding.rbSolid.isChecked() ? Constants.SOLID
                        : binding.rbLiquid.isChecked() ? Constants.LIQUID : Constants.GAS)
                : Constants.HUMAN,
                "",
                0,
                binding.tvGoods.isSelected() ? binding.etProductWeight.getText().toString() : binding.etPeoples.getText().toString(),
                binding.tvGoods.isSelected() ? binding.tvSelectWeight.getText().toString() : "",
                "",
                "",
                "",
                binding.tvGoods.isSelected() ? binding.etBudget.getText().toString() : binding.etPrice.getText().toString(),
                "0",
                binding.tvGoods.isSelected() ? "" : binding.etLuggageWeight.getText().toString()));

        AddUpdateOrder addUpdateOrder =
                new AddUpdateOrder(binding.etTo.getText().toString(),
                        0,
                        toLatLng.longitude,
                        "",
                        fromLatLng.latitude,
                        SharedPrefs.getUserId(activity),
                        fromLatLng.longitude,
                        binding.etTo.getText().toString(),
                        binding.tvGoods.isSelected() ? binding.rbByCustomer.isChecked() : binding.rbWithLuggage.isChecked(),
                        toLatLng.latitude,
                        binding.tvGoods.isSelected() ? binding.etBudget.getText().toString() : binding.etPrice.getText().toString(),
                        0,
                        "",
                        binding.etFrom.getText().toString(),
                        binding.etFrom.getText().toString(),
                        binding.tvGoods.isSelected() ? selectedVehicle.getVehicleTypeId() : humanSelectedVehicle.getVehicleTypeId(),
                        AppUtils.getDateCurrentTimeZone(orderDate),
                        binding.tvGoods.isSelected() ? binding.rbSharing.isChecked() : binding.rbSharingHuman.isChecked(),
                        productList,
                        Constants.ORDER_OPEN,
                        SharedPrefs.getString(activity, SharedPrefs.CITY_ID),
                        SharedPrefs.getString(activity, SharedPrefs.STATE_ID));

        disposable.add(getApiService().addUpdateOrder(SharedPrefs.getAuthToken(activity), addUpdateOrder)
                .compose(applySchedulers())
                .subscribe(addUpdateOrderResponse -> {
                    DialogUtil.dismissProgressDialog();
                    if (addUpdateOrderResponse.getStatus() != null) {
                        if (addUpdateOrderResponse.getStatus().getCode() == 0) {
                            clean();
                            Intent intent = new Intent(activity, MyOrderActivity.class);

                            startActivity(intent);
                        } else
                            Toast.makeText(activity, addUpdateOrderResponse.getStatus().getReturnMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, R.string.fail, Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    DialogUtil.dismissProgressDialog();
                    throwable.printStackTrace();
                    Toast.makeText(activity, R.string.fail, Toast.LENGTH_SHORT).show();
                }));

*/

        Map<String, Object> mapData = new HashMap<>();
        mapData.put("vehicleOrderSummaryId", "0");
        mapData.put("vehicleOrderNo", "0");
        mapData.put("vehicleOrderTitle", "");
        mapData.put("vehicleOrderDescription", "");
        mapData.put("vehicleOrderDate", AppUtils.getDateCurrentTimeZone(orderDate));
        mapData.put("fromAddress", binding.etFrom.getText().toString());
        mapData.put("fromLandmark", binding.etFrom.getText().toString());
        mapData.put("fromArea", "0");
        mapData.put("fromCityId", SharedPrefs.getString(activity, SharedPrefs.CITY_ID));
        mapData.put("fromStateId", SharedPrefs.getString(activity, SharedPrefs.STATE_ID));
        mapData.put("fromLAT", fromLatLng.latitude);
        mapData.put("fromLONG", fromLatLng.longitude);
        mapData.put("toAddress", binding.etTo.getText().toString());
        mapData.put("toLandmark", binding.etTo.getText().toString());
        mapData.put("toArea", "0");
        mapData.put("toCityId", "0");
        mapData.put("toStateId", "0");
        mapData.put("toLAT", toLatLng.latitude);
        mapData.put("toLONG", toLatLng.longitude);
        mapData.put("vehicleId", binding.tvGoods.isSelected() ? selectedOwnerVehicleGoods.getVehicleTypeId() : selectedOwnerVehicleHumans.getVehicleTypeId());
        mapData.put("isShared", binding.tvGoods.isSelected() ? binding.rbSharing.isChecked() : binding.rbSharingHuman.isChecked());
        mapData.put("isLoaded", binding.tvGoods.isSelected() ? binding.rbByCustomer.isChecked() : binding.rbWithLuggage.isChecked());
        mapData.put("contactName", "");
        mapData.put("contactNumber", "");
        mapData.put("userId", SharedPrefs.getUserId(activity));
        mapData.put("price", binding.tvGoods.isSelected() ? binding.etBudget.getText().toString() : binding.etPrice.getText().toString());
        mapData.put("size",  binding.tvGoods.isSelected() ? "" : binding.etLuggageWeight.getText().toString());
        mapData.put("qty",  binding.tvGoods.isSelected() ? binding.etProductWeight.getText().toString() : binding.etPeoples.getText().toString());
        mapData.put("uoM",  binding.tvGoods.isSelected() ? binding.tvSelectWeight.getText().toString() : "");
        mapData.put("status",Constants.PROPOSAL_PENDING);
        mapData.put("productState", binding.tvGoods.isSelected() ?
                (binding.rbSolid.isChecked() ? Constants.SOLID
                        : binding.rbLiquid.isChecked() ? Constants.LIQUID : Constants.GAS)
                : Constants.HUMAN);
        mapData.put("createdById", SharedPrefs.getUserId(activity));


        disposable.add(getApiService().addUpdateVehicleOrder(SharedPrefs.getAuthToken(activity), mapData).compose(applySchedulers())
                .subscribe(statusObjectHolder -> {
                    DialogUtil.dismissProgressDialog();
                    if (statusObjectHolder.getStatus() != null) {
                        if (statusObjectHolder.getStatus().getCode() == 0) {
                            clean();
                            Intent intent = new Intent(activity, MyOrderActivity.class);
                            startActivity(intent);
                        } else
                            Toast.makeText(activity, statusObjectHolder.getStatus().getReturnMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, R.string.fail, Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    DialogUtil.dismissProgressDialog();
                    throwable.printStackTrace();
                    Toast.makeText(activity, R.string.fail, Toast.LENGTH_SHORT).show();
                }));


    }

    private boolean isValid() {
        if (fromLatLng == null || TextUtils.isEmpty(binding.etFrom.getText())) {
            Toast.makeText(activity, R.string.select_your_location, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (toLatLng == null || TextUtils.isEmpty(binding.etTo.getText())) {
            Toast.makeText(activity, R.string.select_target_location, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (orderDate == 0) {
            Toast.makeText(activity, R.string.select_date, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.tvGoods.isSelected()) {
            if (TextUtils.isEmpty(binding.etProductWeight.getText())) {
                Toast.makeText(activity, R.string.enter_weight, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (TextUtils.isEmpty(binding.etBudget.getText())) {
                Toast.makeText(activity, R.string.enter_your_budget, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (binding.rbgVehicleTyp.getCheckedRadioButtonId() == -1) {
                Toast.makeText(activity, R.string.select_vehicle_type, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (selectedOwnerVehicleGoods == null) {
                Toast.makeText(activity, R.string.select_vehicle, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (binding.rbgVehicleTyp.getCheckedRadioButtonId() == -1) {
                Toast.makeText(activity, R.string.select_vehicle_type, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (binding.rbgLoading.getCheckedRadioButtonId() == -1) {
                Toast.makeText(activity, R.string.select_loading_unloading, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            if (TextUtils.isEmpty(binding.etPeoples.getText())) {
                Toast.makeText(activity, R.string.enter_peoples_number, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (TextUtils.isEmpty(binding.etPrice.getText())) {
                Toast.makeText(activity, R.string.enter_price, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (selectedOwnerVehicleHumans == null) {
                Toast.makeText(activity, R.string.select_vehicle, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (binding.rbgVehicleTypHuman.getCheckedRadioButtonId() == -1) {
                Toast.makeText(activity, R.string.select_luggage_option, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (binding.rbgLuggage.getCheckedRadioButtonId() == -1) {
                Toast.makeText(activity, R.string.select_luggage_option, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (TextUtils.isEmpty(binding.etLuggageWeight.getText())) {
                Toast.makeText(activity, R.string.enter_luggage_approx_weight, Toast.LENGTH_SHORT).show();
                return false;
            }


        }
        return true;

    }

    private void clean() {
        selectedOwnerVehicleGoods = null;
        selectedOwnerVehicleHumans = null;

        binding.etFrom.setText("");
        binding.etTo.setText("");
        binding.tvDate.setText(R.string.date);

        binding.etProductWeight.setText("");
        binding.etBudget.setText("");


        binding.rbSolid.setChecked(false);
        binding.rbLiquid.setChecked(false);
        binding.rbGas.setChecked(false);

        binding.tvVehicleSelection.setText("");

        binding.rbPrivate.setChecked(false);
        binding.rbSharing.setChecked(false);

        binding.rbByCustomer.setChecked(false);
        binding.rbByVehicleOwner.setChecked(false);

        /*SECOND PART*/

        binding.etPrice.setText("");
        binding.etPeoples.setText("");

        binding.rbWithLuggage.setChecked(false);
        binding.rbWithoutLuggage.setChecked(false);

        binding.etLuggageWeight.setText("");
        binding.tvVehicleSelection2.setText("");

        binding.rbSharingHuman.setChecked(false);
        binding.rbPrivateHuman.setChecked(false);

        GlideApp.with(activity).load(R.drawable.rectangle_bg_gray).into(binding.ivVehicleSelection);
        GlideApp.with(activity).load(R.drawable.rectangle_bg_gray).into(binding.ivVehicleSelectionHuman);

    }

    // TODO: 8/4/20  UNCOMMENT THIS
    private void setCurrentLocationMarker(boolean setLocation) {
     /*   if (mCurrentLatLng == null) return;

        LatLng latLng = mCurrentLatLng;

        if (mMarker != null) {
            mMarker.remove();
        }
        mMarker = map.addMarker(new MarkerOptions().position(latLng));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(18).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (!setLocation) return;
        List<Address> addresses = new ArrayList<>();
        try {
            addresses = mGeocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        android.location.Address address = addresses.get(0);

        if (address != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                sb.append(address.getAddressLine(i));
            }
            try {
                fromLatLng = latLng;
                binding.etFrom.setText(sb.toString().trim());
//                mAddressLatLng.setAddress(sb.toString().trim());
//                mAddressLatLng.setLatitude(latLng.latitude);
//                mAddressLatLng.setLongitude(latLng.longitude);
//                setAddress();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    }

    // TODO: 8/4/20  unComment this
    private void startLocationUpdates() {
      /*  mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(locationSettingsResponse -> {
                    Timber.i("All location settings are satisfied. - Started location updates!");
                    //noinspection MissingPermission
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                })
                .addOnFailureListener(e -> {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                            Timber.i("Location settings are not satisfied. Attempting to upgrade location settings");
                            try {
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(activity, Constants.REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sie) {
                                Timber.i("PendingIntent unable to execute request.");
                            }
                            break;
                        }
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Timber.e("Location settings are inadequate, and cannot be fixed here. Fix in Settings.");
                    }
                });*/
    }


    //region result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == Constants.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                startLocationUpdates();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(activity, getString(R.string.please_turn_on_gps), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Place place = Autocomplete.getPlaceFromIntent(data);
                    Timber.i("Place: " + place.getName() + ", " + place.getId() + ", " + place.getAddress());
//                    Toast.makeText(activity, "ID: " + place.getId() + "address:" + place.getAddress()
//                            + "Name:" + place.getName() + " latlong: " + place.getLatLng(), Toast.LENGTH_LONG).show();
                    String address = place.getAddress();

                    toLatLng = place.getLatLng();
                    binding.etTo.setText(place.getAddress());
                }
                // do query with address

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                assert data != null;
                Status status = Autocomplete.getStatusFromIntent(data);
                Toast.makeText(activity, "Error: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                Timber.i(status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
                Timber.d("onActivityResult: CANCEL");
            }
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }
    //endregion

    //region location permission
    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void getCurrentLocation() {
        startLocationUpdates();
//        mLocationPermissionGranted = true;
//        getDeviceLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        OwnerHomeActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnPermissionDenied({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void deniedLocation() {
    }

    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    void neverAskLocation() {
    }
    //endregion


}
