package detroit.com.safeandsound;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.CompositeSymbol;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class DisplayMap extends AppCompatActivity {

    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
    private static MapView mMapView;
    private int requestCode = 2;
    private LocationDisplay mLocationDisplay;
    private LocationChangeListener locationChangeListener;
    private RenderLayers renderLayers;
    private RouteSolver routeSolver;
    private RouteTask routeTask;
    private ArcGISMap map;
    private static FeatureLayer usersLayer;
    private static ArcGISFeature userPos;
    private static ServiceFeatureTable usersFeaTable;
    private static GraphicsOverlay stopsOverlay;
    private static GraphicsOverlay routeOverlay;
    private static GraphicsOverlay trafficIncOverlay;
    private static SharedPreferences sharedPrefs;
    private static SimpleMarkerSymbol stopSymbolPoint;
    private static boolean addPointStop = false;
    private static boolean addPolyBarrier = false;
    private static SimpleMarkerSymbol incSymbolPoint;
    private static TextSymbol incSymbolText = new TextSymbol(10.0f, "B", Color.BLACK, TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
    private static SimpleFillSymbol incSymbolPoly = new SimpleFillSymbol(
            SimpleFillSymbol.Style.SOLID, Color.RED, null);

    static {
        incSymbolPoint = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 14.0f);
        incSymbolPoint.setOutline(
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f));

        stopSymbolPoint = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.CIRCLE, Color.WHITE, 12.0f);
        getStopSymbolPoint().setOutline(
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 2.0f));
    }

    public static SimpleMarkerSymbol getStopSymbolPoint() {
        return stopSymbolPoint;
    }

    public static ArcGISFeature getUserPos() {
        return userPos;
    }

    public static ServiceFeatureTable getUsersFeaTable() {
        return usersFeaTable;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hide toolbar
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_display_map);


        map = new ArcGISMap(Basemap.Type.STREETS_VECTOR, 42.361759, -83.064882, 10);
        mMapView = (MapView) findViewById(R.id.mapView);
        getMapView().setMap(map);
        getMapView().setAttributionTextVisible(false);

        map.addLoadStatusChangedListener(new LoadStatusChangedListener() {
            @Override
            public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
                if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.LOADING) {
                    Toast.makeText(DisplayMap.this, "LOADING MAP...", Toast.LENGTH_LONG).show();
                } else if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.LOADED) {
                    //Toast.makeText(DisplayMap.this, "MAP LOADED...", Toast.LENGTH_LONG).show();
                    //getMapView().setViewpointCenterAsync(new Point(-9251271, 5220746, SpatialReference.create(102100)), 90000);
                }

            }
        });

        //check if data exist
        checkDataExist();

        //definesharedPrefernce
        sharedPrefs = getSharedPreferences(Settings.SHARED_PREFERENCES, MODE_PRIVATE);

        //add usersLocation
        addFeautureLayer();

        //add graphics
        addGraphicsLayers();

        Log.e("NAME", sharedPrefs.getString(Settings.USER_NAME, ""));
        Log.e("NAME ID", sharedPrefs.getString(Settings.USER_NAME_ID, ""));
        Log.e("Omit First panel", String.valueOf(sharedPrefs.getBoolean(Settings.PASS_LOGIN_ACTIVITY, false)));

        sharedPrefs.edit().putBoolean(Settings.PASS_LOGIN_ACTIVITY, true).commit();
        sharedPrefs.edit().putString(Settings.ROUTE_MODE, Settings.ROUTE_MODE_SAFEST).commit();
        sharedPrefs.edit().putString(Settings.ROUTE_TRANSIT, Settings.ROUTE_TRANSIT_DRIVING).commit();
        sharedPrefs.edit().putBoolean(Settings.ROUTE_STOP_BEST_SEQ, true).commit();
        sharedPrefs.edit().putBoolean(Settings.ROUTE_STOP_FIRST, true).commit();
        sharedPrefs.edit().putBoolean(Settings.ROUTE_STOP_LAST, true).commit();

        //set Layers flags
        sharedPrefs.edit().putBoolean(Settings.SHOW_INC_LAYER, false).commit();
        sharedPrefs.edit().putBoolean(Settings.SHOW_TRA_LAYER, false).commit();
        sharedPrefs.edit().putBoolean(Settings.ROUTE_CUR_TRA_INC, false).commit();

        Log.e("Omit First panel", String.valueOf(sharedPrefs.getBoolean(Settings.PASS_LOGIN_ACTIVITY, false)));

        //setDisplayLocation
        mLocationDisplay = getMapView().getLocationDisplay();
        mLocationDisplay.startAsync();
        locationChangeListener = new LocationChangeListener(getUsersFeaTable());

        mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
            @Override
            public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                if (locationChangedEvent != null && locationChangedEvent.getLocation().getPosition() != null) {
                    Point toCenter = locationChangeListener.checkForUpdates(locationChangedEvent.getLocation().getPosition());
//                    if (renderLayers != null && !GeometryEngine.intersects(renderLayers.getBbox(), toCenter)) {
//                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.outside_detroit), Toast.LENGTH_LONG);
//                    }
                }
            }
        });

        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                if (dataSourceStatusChangedEvent.getError() == null)
                    return;

                boolean permissionCheck1 = ContextCompat.checkSelfPermission(DisplayMap.this, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(DisplayMap.this, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;

                if (!(permissionCheck1 && permissionCheck2)) {
                    ActivityCompat.requestPermissions(DisplayMap.this, reqPermissions, requestCode);
                } else {
                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(DisplayMap.this, message, Toast.LENGTH_LONG).show();

                }
            }
        });

        FloatingActionButton fabAddIncident = (FloatingActionButton) findViewById(R.id.gps);
        fabAddIncident.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMapView.setViewpoint(new Viewpoint(locationChangeListener.getLastPosition(), 2000));
            }
        });

        FloatingActionButton fabRouting = (FloatingActionButton) findViewById(R.id.route);
        fabRouting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    removeRouteGraphics();
                    Route mRoute = routeSolver.solveRoute();
                    if (mRoute != null) {
                        showRouteSummary(mRoute.getTotalTime(), mRoute.getTotalLength(), mRoute.getDirectionManeuvers());
                    } else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_route_found), Toast.LENGTH_LONG).show();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.login_error), Toast.LENGTH_LONG).show();
                }
            }
        });

        FloatingActionButton fabMenuButton = (FloatingActionButton) findViewById(R.id.menu);
        fabMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MenuActivity.class));
            }
        });

        FloatingActionButton fabAddButton = (FloatingActionButton) findViewById(R.id.add);
        fabAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTypeToAdd();

            }
        });

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motion) {
                Log.e("MAP", "TOUCHED");
                android.graphics.Point mClickPoint = new android.graphics.Point((int) motion.getX(), (int) motion.getY());
                Point stop = mMapView.screenToLocation(mClickPoint);
                if (addPointStop) {
                    makeReverseGeocoding(stop);
                    addPointStop = false;
                } else if (addPolyBarrier) {
                    //addBarrier
                    getTrafficIncOverlay().getGraphics().add(new Graphic(GeometryEngine.buffer(stop, 10), incSymbolPoly));
                    getTrafficIncOverlay().getGraphics().add(new Graphic(stop, incSymbolPoint));
                    getTrafficIncOverlay().getGraphics().add(new Graphic(stop, incSymbolText));
                    sharedPrefs.edit().putBoolean(Settings.ROUTE_CUR_TRA_INC, true).commit();
                    addPolyBarrier = false;
                } else {
                    //show Callout
                    ListenableFuture<IdentifyGraphicsOverlayResult> results = mMapView.identifyGraphicsOverlayAsync(getStopsOverlay(), mClickPoint, 10, false);
                    try {
                        IdentifyGraphicsOverlayResult res = results.get();
                        if (!res.getGraphics().isEmpty()) {
                            Graphic gr = res.getGraphics().get(0);
                            LayoutInflater inflater = getLayoutInflater();
                            View callout_view = inflater.inflate(R.layout.callout_stop_layout, null);
                            TextView textView = (TextView) callout_view.findViewById(R.id.text_value);
                            textView.setText(gr.getAttributes().get("Address").toString());
                            Callout mCallout = mMapView.getCallout();
                            Callout.ShowOptions s = new Callout.ShowOptions();
                            s.setRecenterMap(true);
                            s.setAnimateCallout(true);
                            mCallout.setShowOptions(s);
                            mCallout.setLocation((Point) gr.getGeometry());
                            mCallout.setContent(callout_view);
                            mCallout.show();
                        }

                    } catch (InterruptedException | ExecutionException e) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.login_error), Toast.LENGTH_LONG).show();
                    }
                }
                return super.onSingleTapConfirmed(motion);
            }

            @Override
            public void onLongPress(MotionEvent motion) {
                Log.e("MAP", "LONG PRESSED");
                getStopsOverlay().getGraphics().clear();
                getRouteOverlay().getGraphics().clear();
                getTrafficIncOverlay().getGraphics().clear();
            }
        });

    }

    private void addFeautureLayer() {
        usersFeaTable = new ServiceFeatureTable(getResources().getString(R.string.login_url));
        usersLayer = new FeatureLayer(getUsersFeaTable());
        usersLayer.setId("usersLayer");
        usersLayer.setDefinitionExpression("1=2");
        usersLayer.setVisible(false);
        TextSymbol symbolText = new TextSymbol(10.0f, "U",
                Color.BLACK, TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
        SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.CIRCLE, Color.MAGENTA, 14.0f);
        symbol.setOutline(
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f));
        List<Symbol> list = new ArrayList<>();
        list.add(symbol);
        list.add(symbolText);
        usersLayer.setRenderer(new SimpleRenderer(new CompositeSymbol(list)));
        getMapView().getMap().getOperationalLayers().add(usersLayer);

        QueryParameters query = new QueryParameters();
        query.setReturnGeometry(true);
        query.setWhereClause("Name = '" + sharedPrefs.getString(Settings.USER_NAME, "") + "'");  ///lub ocjectID
        query.setMaxFeatures(1);
        ListenableFuture<FeatureQueryResult> future = getUsersFeaTable().queryFeaturesAsync(query);
        try {
            FeatureQueryResult result = future.get();
            if (result.iterator().hasNext()) {
                userPos = (ArcGISFeature) result.iterator().next();
                userPos.loadAsync();
            }
        } catch (InterruptedException | ExecutionException e) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.login_error), Toast.LENGTH_LONG).show();
        }
    }

    private void selectTypeToAdd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle(getResources().getString(R.string.select_type_add))
                .setItems(R.array.type_to_add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                //my location
                                routeSolver.setAddMyCurrentLocation(true);
                                break;
                            case 1:
                                //address from text
                                showAddressDialog();
                                break;
                            case 2:
                                //point stop
                                addPointStop = true;
                                break;
                            case 3:
                                //favourites
                                showSelectFavouriteDialog();
                                break;
                            case 4:
                                //barrier
                                addPolyBarrier = true;
                                break;
                            case 5:
                                //add Tracking User
                                addTrackingUser();
                                break;
                        }
                    }
                });
        builder.create();
        builder.show();

    }

    private void addTrackingUser() {
        new SearchNearbyUsers(DisplayMap.this, LocationChangeListener.getLastPosition(), usersFeaTable).execute();
    }

    private void showSelectFavouriteDialog() {
        Set<String> hs = sharedPrefs.getStringSet(Settings.MY_FAVOURITES, new HashSet<String>());
        if (hs.isEmpty()) {
            return;
        }
        String[] items = new String[hs.size()];
        final String[] myFavs = new String[hs.size()];
        Iterator iterator = hs.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String val = (String) iterator.next();
            myFavs[i] = val;
            items[i] = val.split(",")[0];
            i++;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.select_favourite));
        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Double pktX = Double.valueOf(myFavs[i].split(",")[1]);
                Double pktY = Double.valueOf(myFavs[i].split(",")[2]);
                HashMap<String, Object> atts = new HashMap<>();
                atts.put("Address", myFavs[i].split(",")[0]);
                getStopsOverlay().getGraphics().add(new Graphic(new Point(pktX, pktY, SpatialReference.create(102100)),
                        atts, getStopSymbolPoint()));

            }
        });
        builder.create();
        builder.show();
    }


    private void showAddressDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialView = inflater.inflate(R.layout.add_address, null);
        builder.setView(dialView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText editText = (EditText) dialView.findViewById(R.id.addressInput);
                        geocodeAddress(editText.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create();
        builder.show();
    }

    private void geocodeAddress(String query) {
        GeocodeParameters geocodeParameters = new GeocodeParameters();
        geocodeParameters.getResultAttributeNames().add("*");
        geocodeParameters.setMaxResults(1);
        geocodeParameters.setSearchArea(renderLayers.getBbox());
        geocodeParameters.setOutputSpatialReference(getMapView().getSpatialReference());
        new MakeGeocoding(this.getApplicationContext(), query, geocodeParameters).execute();

    }

    private void makeReverseGeocoding(Point stop) {
        ReverseGeocodeParameters reverseGeocodeParameters = new ReverseGeocodeParameters();
        reverseGeocodeParameters.setOutputSpatialReference(mMapView.getSpatialReference());
        new MakeReverseGeocoding(this.getApplicationContext(), stop, reverseGeocodeParameters).execute();
    }

    private void showRouteSummary(double totalTime, double totalLength, final List<DirectionManeuver> directionManeuvers) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.route_summary, (ViewGroup) findViewById(R.id.route_summary_id));

        ((TextView) layout.findViewById(R.id.total_length)).setText(
                String.valueOf(Math.round((totalLength / 1609.344) * 100.0) / 100.0) + " " + getResources().getString(R.string.miles));
        ((TextView) layout.findViewById(R.id.total_time)).setText(
                String.valueOf(Math.round((totalTime) * 100) / 100) + " " + getResources().getString(R.string.minutes));

        if (getSharedPreferences().getString(Settings.ROUTE_TRANSIT, Settings.ROUTE_TRANSIT_BICYCLING).equalsIgnoreCase(Settings.ROUTE_TRANSIT_BICYCLING)
                || getSharedPreferences().getString(Settings.ROUTE_TRANSIT, Settings.ROUTE_TRANSIT_BICYCLING).equalsIgnoreCase(Settings.ROUTE_TRANSIT_WALKING)) {
            ((TextView) layout.findViewById(R.id.total_calories)).setText(
                    String.valueOf(Math.round((totalTime * 8) * 100) / 100) + " " + getResources().getString(R.string.kCal));
            ((TextView) layout.findViewById(R.id.total_carbon)).setText(
                    String.valueOf("0" + " " + Html.fromHtml(getResources().getString(R.string.co2))));
        } else {
            ((TextView) layout.findViewById(R.id.total_calories)).setText(
                    String.valueOf("0 kCal"));
            ((TextView) layout.findViewById(R.id.total_carbon)).setText(
                    String.valueOf(Math.round((totalLength * 0.00286) * 100) / 100) + " " + Html.fromHtml(getResources().getString(R.string.co2)));
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        startNavigation(directionManeuvers);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create();
        builder.show();
    }

    private void removeRouteGraphics() {
        for (Graphic gr : getRouteOverlay().getGraphics()) {
            if (gr.getGeometry().getGeometryType() == GeometryType.POLYLINE) {
                getRouteOverlay().getGraphics().remove(gr);
            }
        }
    }

    private void addGraphicsLayers() {
        stopsOverlay = new GraphicsOverlay();
        routeOverlay = new GraphicsOverlay();
        trafficIncOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().addAll(Arrays.asList(getRouteOverlay(), getStopsOverlay(), getTrafficIncOverlay()));
    }

    /**
     * Checks if data exists
     */
    private void checkDataExist() {
        try {
            File directory = new File(Environment.getExternalStorageDirectory().toString()
                    + "/SafeAndSound/dataforanalytics.geodatabase");
            renderLayers = RenderLayers.getInstance(getResources().getString(R.string.incident_data)
                    , getResources().getString(R.string.reference_data), getResources().getString(R.string.traffic_incidents));
            List<FeatureLayer> fList = renderLayers.addReferenceData();
            for (FeatureLayer fl : fList) {
                map.getOperationalLayers().add(fl);
            }

            //zoomToDetroit
            getMapView().setViewpointGeometryAsync(renderLayers.getBbox());

            routeTask = new RouteTask(getApplicationContext(), directory.getAbsolutePath(), "Network_ND");
            routeTask.loadAsync();
            routeSolver = RouteSolver.getInstance(routeTask);

        } catch (RuntimeException ex) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.login_error), Toast.LENGTH_LONG).show();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(DisplayMap.this, "NO PERMISSION",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        getMapView().pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getMapView().resume();
    }

    public static MapView getMapView() {
        return mMapView;
    }

    public static GraphicsOverlay getStopsOverlay() {
        return stopsOverlay;
    }

    public static GraphicsOverlay getRouteOverlay() {
        return routeOverlay;
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPrefs;
    }

    public static GraphicsOverlay getTrafficIncOverlay() {
        return trafficIncOverlay;
    }


    public void startNavigation(List<DirectionManeuver> directionManeuvers) {

        String uri = "https://maps.google.com/maps?saddr=";

        Geometry geom = directionManeuvers.get(0).getGeometry();
        if (geom.getGeometryType() == GeometryType.POLYLINE) {
            Point pkt = ((Polyline) directionManeuvers.get(0).getGeometry()).getParts().get(0).getPoint(0);
            Point pkt4326 = (Point) GeometryEngine.project(pkt, SpatialReference.create(4326));
            uri = uri + String.valueOf(pkt4326.getY())
                    + "," + String.valueOf(pkt4326.getX());

        } else {
            Point pkt = (Point) directionManeuvers.get(0).getGeometry();
            Point pkt4326 = (Point) GeometryEngine.project(pkt, SpatialReference.create(4326));

            uri = uri + String.valueOf(pkt4326.getY())
                    + "," + String.valueOf(pkt4326.getX());

        }

        Geometry geom1 = directionManeuvers.get(1).getGeometry();
        if (geom1.getGeometryType() == GeometryType.POLYLINE) {

            Point pkt = ((Polyline) directionManeuvers.get(1).getGeometry()).getParts().get(0).getPoint(0);
            Point pkt4326 = (Point) GeometryEngine.project(pkt, SpatialReference.create(4326));
            uri = uri + "&daddr=" + String.valueOf(pkt4326.getY())
                    + "," + String.valueOf(pkt4326.getX());

        } else {
            Point pkt = (Point) directionManeuvers.get(1).getGeometry();
            Point pkt4326 = (Point) GeometryEngine.project(pkt, SpatialReference.create(4326));

            uri = uri + "&daddr=" + String.valueOf(pkt4326.getY())
                    + "," + String.valueOf(pkt4326.getX());

        }

        for (int i=2; i< directionManeuvers.size(); i++) {
            Geometry geom2 = directionManeuvers.get(i).getGeometry();
            if (geom2.getGeometryType() == GeometryType.POLYLINE) {
                Point pkt = ((Polyline) directionManeuvers.get(i).getGeometry()).getParts().get(0).getPoint(0);
                Point pkt4326 = (Point) GeometryEngine.project(pkt, SpatialReference.create(4326));
                uri = uri + "+to:" + String.valueOf(pkt4326.getY())
                        + "," + String.valueOf(pkt4326.getX());

            } else {
                Point pkt = (Point) directionManeuvers.get(i).getGeometry();
                Point pkt4326 = (Point) GeometryEngine.project(pkt, SpatialReference.create(4326));

                uri = uri + "+to:" + String.valueOf(pkt4326.getY())
                        + "," + String.valueOf(pkt4326.getX());
            }
        }

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        startActivity(intent);
    }

    public void getStopCalloutStar(View view) {
        Point favLoc = getMapView().getCallout().getLocation();
        showAddFavouriteDialog(favLoc);
        getMapView().getCallout().dismiss();
    }

    public void getStopCalloutChart(View view) {
        Point favLoc = getMapView().getCallout().getLocation();
        getMapView().getCallout().dismiss();
        generateIncidentSummary(favLoc);
    }

    private void generateIncidentSummary(Point favLoc) {
        QueryParameters query = new QueryParameters();
        query.setReturnGeometry(false);
        query.setMaxFeatures(20000);
        query.setSpatialRelationship(QueryParameters.SpatialRelationship.INTERSECTS);
        query.setGeometry(GeometryEngine.buffer(favLoc, 500));
        new QueryIncidents(DisplayMap.this, query, getResources().getString(R.string.point_incidents)).execute();

    }

    public void showAddFavouriteDialog(final Point pkt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View favouriteView = inflater.inflate(R.layout.add_favourite, null);
        builder.setTitle(getResources().getString(R.string.add_favourite));
        builder.setView(favouriteView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText editText = (EditText) favouriteView.findViewById(R.id.favourite_name);
                        String favourValue = editText.getText().toString() + "," + String.valueOf(pkt.getX())
                                + "," + String.valueOf(pkt.getY());
                        Set<String> hs = sharedPrefs.getStringSet(Settings.MY_FAVOURITES, new HashSet<String>());
                        hs.add(favourValue);
                        sharedPrefs.edit().clear().putStringSet(Settings.MY_FAVOURITES, hs).commit();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create();
        builder.show();
    }

    public static FeatureLayer getUsersLayer() {
        return usersLayer;
    }
}
