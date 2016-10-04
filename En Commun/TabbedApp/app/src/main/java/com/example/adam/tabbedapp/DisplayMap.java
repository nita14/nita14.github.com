package com.example.adam.tabbedapp;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.AttachmentManager;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.AttachmentInfo;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.tasks.na.Route;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DisplayMap extends AppCompatActivity {

    public static ArcGISFeatureLayer driverLyr;
    public static ArcGISFeatureLayer paxLyr;
    public static ArcGISFeatureLayer incLyr;
    public static GraphicsLayer grLyr;
    public static MapView map;
    public static LocationDisplayManager locManager;
    public static Point wgsPos;
    public static int driverId = -1;
    public static int paxId = -1;
    public static RouteSolver routeSolver;
    public View curView;
    public static boolean addStartMap = false;
    public static boolean addEndtMap = false;
    public static Point startingPoint = null;
    public static Point endingPoint = null;
    public static int numberPlaces;
    public static int maxBuffer = 1000;
    public static String closestDriverOID = null;
    public File imageFileDriver;
    public File imageFilePax;
    public static int fCount;
    public static int paxAccept;
    public static int driverAccept;
    public static int noGraphics=0;
    public static int prevNoGraphics=0;
    public static int paxGraphicId;
    public static int paxGraphicOid;
    public static Locator locator = Locator.createOnlineLocator(
            "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");

    public static String driverStatus = "free";
    public static String paxStatus = "free";
    public static ArrayList<Integer> listOfPaxs= new ArrayList<>();
    public static Route routeDriver;
    public static ArrayList<Integer> incDetArray = new ArrayList<>();
    public static int grIdDriver = -1;

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int REQ_CODE_SPEECH_INPUT_END = 101;

    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_map);

        map = (MapView) findViewById(R.id.map);
        String fsDriverUrl = this.getResources().getString(R.string.fsDriver);
        driverLyr = new ArcGISFeatureLayer(fsDriverUrl, ArcGISFeatureLayer.MODE.SNAPSHOT);
        driverLyr.setAutoRefreshOnExpiration(true);
        driverLyr.setExpirationInterval(10);
        String fsPaxUrl = this.getResources().getString(R.string.fsPax);
        paxLyr = new ArcGISFeatureLayer(fsPaxUrl, ArcGISFeatureLayer.MODE.SNAPSHOT);
        paxLyr.setAutoRefreshOnExpiration(true);
        paxLyr.setExpirationInterval(10);
        String fsIncUrl = this.getResources().getString(R.string.fsInc);
        incLyr = new ArcGISFeatureLayer(fsIncUrl, ArcGISFeatureLayer.MODE.SNAPSHOT);
        incLyr.setAutoRefreshOnExpiration(true);
        incLyr.setExpirationInterval(10);
        map.addLayer(driverLyr);
        map.addLayer(paxLyr);
        map.addLayer(incLyr);
        grLyr = new GraphicsLayer();
        map.addLayer(grLyr);
        final boolean[] zoomFirst = {true};

        curView = findViewById(R.id.add);


        paxLyr.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object o, STATUS status) {
                if (status == STATUS.INITIALIZED){
                    if(MainActivity.isDriver) {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkChangesDriver();
                            }
                        }, 8000);
                    } else {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkChangesPax();
                            }
                        }, 8000);

                    }
                }
            }
        });

        incLyr.setOnStatusChangedListener(new OnStatusChangedListener(){
            @Override
                public void onStatusChanged(Object o, STATUS status) {
                if (status == STATUS.INITIALIZED){
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                incidentCheckDetector();
                            }
                        }, 5000);
                }

                }
            }
        );

        //change fab add if driver
        FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                curView = view;

                if(MainActivity.isDriver){
                    Snackbar.make(view, R.string.CalculatingRoute, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    findPaxs(view, false);
                } else {
                    //show fragment
                    AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMap.this);
                    builder.setMessage(R.string.AddingStops).setTitle(R.string.AddingStartingTitle);
                    builder.setPositiveButton(R.string.AddingStopsMap, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            addStartMap = true;
                        }
                    });
                    builder.setNegativeButton(R.string.AddingStopsVoice, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            addStartMap = false;
                            createInputForGeocoding(true);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

            }
        });

        if (MainActivity.isDriver) {
            Drawable myFabSrc = getResources().getDrawable(R.drawable.ic_directions);
            fabAdd.setImageDrawable(myFabSrc);

        } else {
            Drawable myFabSrc = getResources().getDrawable(R.drawable.ic_add_circle);
            fabAdd.setImageDrawable(myFabSrc);
        }

        //initialize RouteSolver
        routeSolver = new RouteSolver();
        //set touch listener
        map.setOnTouchListener(new TouchListener(DisplayMap.this, map));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.gps);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomToCurrentLocation();
            }
        });

        map.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object o, STATUS status) {
                if (o == map && status == STATUS.INITIALIZED) {
                    map.getLayers()[0].setOpacity(0.91f);
                    locManager = map.getLocationDisplayManager();
                    locManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.NAVIGATION);
                    locManager.setLocationListener(new LocationListener() {
                        @Override
                        public void onLocationChanged(Location loc) {
                            double locy = loc.getLatitude();
                            double locx = loc.getLongitude();
                            double distance = 0;


                            Point lastPos = wgsPos;
                            wgsPos = new Point(locx, locy);

                            if (lastPos != null && distance == GeometryEngine.geodesicDistance(wgsPos, lastPos, SpatialReference.create(4326), (LinearUnit) Unit.create(LinearUnit.Code.METER))){
                                //Log.e("Distance 1 ", "Return");
                                return;
                            }


                            if (lastPos != null && GeometryEngine.geodesicDistance(wgsPos, lastPos, SpatialReference.create(4326), (LinearUnit) Unit.create(LinearUnit.Code.METER)) > 30) {
//                                Log.e("Distance ", String.valueOf(GeometryEngine.geodesicDistance(wgsPos, lastPos, SpatialReference.create(4326), (LinearUnit) Unit.create(LinearUnit.Code.METER))));
//                                Log.e("Point WGS ", String.valueOf(wgsPos.getX()) + String.valueOf(wgsPos.getY())  );
//                                Log.e("Point lastPos ", String.valueOf(lastPos.getX()) + String.valueOf(lastPos.getY())  );

                                updatePosition();
                            }

                            if (zoomFirst[0]) {
                                zoomToCurrentLocation();
                                zoomFirst[0] = false;
                                savePosition();
                            }

                        }

                        @Override
                        public void onStatusChanged(String s, int i, Bundle bundle) {

                        }

                        @Override
                        public void onProviderEnabled(String s) {

                        }

                        @Override
                        public void onProviderDisabled(String s) {

                        }
                    });
                    locManager.start();
                    locManager.setOpacity(0.4f);

                }
            }
        });

        //fab add incident
        FloatingActionButton fabAddIncident = (FloatingActionButton) findViewById(R.id.add_incident);
        fabAddIncident.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveIncidentData();

            }
        });

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void saveIncidentData() {

        Graphic newFeatureGraphic = new Graphic((Point) GeometryEngine
                .project(wgsPos,
                        SpatialReference.create(4326),
                        map.getSpatialReference()), null, null);

        Graphic[] adds = {newFeatureGraphic};
        incLyr.applyEdits(adds, null, null, new CallbackListener<FeatureEditResult[][]>() {
            @Override
            public void onCallback(FeatureEditResult[][] featureEditResults) {
                if (featureEditResults[0] != null && featureEditResults[0][0] != null && featureEditResults[0][0].isSuccess()) {
                    //Log.e("INCIDENT ADDED", String.valueOf(featureEditResults[0][0].getObjectId()));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("ERROR!!!", "INCIDENT not added");

            }
        });

    }

    private void incidentCheckDetector() {
        final long period = 10000;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                for (int idOnc : incLyr.getGraphicIDs()) {
                    Graphic testInc = incLyr.getGraphic(idOnc);
                    Polygon incBuffer = GeometryEngine.buffer(testInc.getGeometry(), map.getSpatialReference(), 2000, map.getSpatialReference().getUnit());
                    Point curPos = (Point) GeometryEngine.project(wgsPos, SpatialReference.create(4326), map.getSpatialReference());
                    if (GeometryEngine.contains(incBuffer, curPos, map.getSpatialReference()) && !incDetArray.contains(idOnc)) {
                        map.centerAt(((Point) testInc.getGeometry()), true);
                        Snackbar snackbar = Snackbar
                                .make(curView, "Reported request for help", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("DISMISS", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        });

                        snackbar.show();
                        incDetArray.add(idOnc);
                    }
                }
            }
        }, 0, period);
    }

    public void checkChangesDriver() {
        final long period = 10000;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                prevNoGraphics = noGraphics;
                for(int id : paxLyr.getGraphicIDs()) {
                    Graphic gtTest = paxLyr.getGraphic(id);
                        if (gtTest.getAttributeValue("status").equals("free") && driverId != -1
                            && gtTest.getAttributeValue("driver") != null
                            && gtTest.getAttributeValue("paxAccept") != null
                            && driverId == Integer.valueOf((String) gtTest.getAttributeValue("driver"))
                            && Integer.valueOf((int) gtTest.getAttributeValue("paxAccept")) == 1) {

                            boolean isId= false;
                            for(int idpax: listOfPaxs){
                                int curId = (int) gtTest.getId();
                                if (idpax == curId){
                                    isId = true;
                                }
                            }
                            if(!isId){
                                //Log.e("checkChangesDriver", "add id");
                                listOfPaxs.add((int) gtTest.getId());
                                paxGraphicId=id;
                                //Log.e("checkChangesDriver ID", String.valueOf(paxGraphicId));
                                noGraphics++;
                            }
                    }
                }

                if (prevNoGraphics != noGraphics) {
                    int grID = getGraphicDiriverId(driverId);
                    Graphic driverGraphic = driverLyr.getGraphic(grID);
                    paxGraphicOid = (Integer) paxLyr.getGraphic(paxGraphicId).getAttributeValue("OBJECTID");
                    int paxNoPlaces = Integer.valueOf((String) paxLyr.getGraphic(paxGraphicId).getAttributeValue("notes"));
                    if (driverGraphic.getAttributeValue("status").equals("free") && (MainActivity.maxCap - MainActivity.curCap >= paxNoPlaces)) {
                        //Log.e("checkChangesDriver", "Driver found free");
                        //make a routing to only this graphic
                        findPaxs(curView, true);
                        showSnackbarClosestPaxInfo();
                    } else if (driverGraphic.getAttributeValue("status").equals("search") && (MainActivity.maxCap - MainActivity.curCap >= paxNoPlaces)) {
                        findPaxs(curView, false);
                        showSnackbarClosestPaxInfo();


                    } else {
                        Log.e("checkChangesDriver", "driver status undefined");
                    }
                }
            }
        }, 0, period);
    }

    public void checkChangesPax() {
        int griD = -1;
        final long period = 10000;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // do your task her
                prevNoGraphics = noGraphics;
                for(int id : paxLyr.getGraphicIDs()) {
                    Graphic gtTest = paxLyr.getGraphic(id);
                    if (gtTest.getAttributeValue("status").equals("full") && paxId != -1
                            && gtTest.getAttributeValue("driver") != null
                            && gtTest.getAttributeValue("paxAccept") != null
                            && gtTest.getAttributeValue("driverAccept") != null
                            && paxId == (Integer) gtTest.getAttributeValue("OBJECTID")
                            && Integer.valueOf((int) gtTest.getAttributeValue("paxAccept")) == 1) {
                        //is your driver clode to you?
                        Polygon bufferPointStartPax = GeometryEngine.buffer(startingPoint, map.getSpatialReference(), 400, map.getSpatialReference().getUnit());
                        grIdDriver = driverLyr.getGraphicIDWithOID(Integer.valueOf((String) gtTest.getAttributeValue("driver")));
                        if(GeometryEngine.contains(bufferPointStartPax, driverLyr.getGraphic(grIdDriver).getGeometry(), map.getSpatialReference())){
                            //Log.e("PAX SEARCH!!!", "Graphic is inside buffer");
                            Snackbar snackbar = Snackbar
                                    .make(curView, "Driver is close to you", Snackbar.LENGTH_INDEFINITE);
                            snackbar.setAction("DISMISS", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            });

                            snackbar.show();

                        } else {
                            Log.e("PAX SEARCH!!!", "Graphic is NOT inside buffer");
                        }

                        boolean isId= false;
                        for(int idpax: listOfPaxs){
                            int curId = (int) gtTest.getId();
                            if (idpax == curId){
                                //Log.e("checkChangesPax", "Found id");
                                isId = true;
                            }
                        }

                        if(!isId){
                            //Log.e("checkChangesPax", "add id");
                            listOfPaxs.add((int) gtTest.getId());
                            noGraphics++;
                        }
                    }

                    //check other updates
                    if (gtTest.getAttributeValue("status").equals("full") && paxId != -1
                            && gtTest.getAttributeValue("driver") != null
                            && gtTest.getAttributeValue("paxAccept") != null
                            && gtTest.getAttributeValue("driverAccept") != null
                            && grIdDriver == Integer.valueOf((String) gtTest.getAttributeValue("driver"))
                            && Integer.valueOf((int) gtTest.getAttributeValue("paxAccept")) == 1
                            && Integer.valueOf((int) gtTest.getAttributeValue("driverAccept")) == 1){
                            Log.e("OTHER PAX FOUND", "show profile");

                            AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMap.this);

                            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }});
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            int age = Integer.valueOf((String) gtTest.getAttributeValue("age"));
                            String ageJenk = null;
                            if (age==0){
                                ageJenk= "to 25";
                            } else if(age==1) {
                                ageJenk= "form 25 to 65";
                            } else  {
                                ageJenk= "over 65";
                            }

                            builder.setTitle(String.valueOf(gtTest.getAttributeValue("name")));
                            String nameAndRank = getString(R.string.phone) + " " + String.valueOf(gtTest.getAttributeValue("phone") + "\n");

                            String ageAndSex =  getString(R.string.sex) + " " + String.valueOf(gtTest.getAttributeValue("sex")) + " "
                                    + getString(R.string.age) + " " + ageJenk + "\n" ;

                            builder.setMessage(nameAndRank +"\n"+ ageAndSex);

                            AlertDialog dialog = builder.show();
                            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
                            messageText.setGravity(Gravity.CENTER);

                    }
                }

                if (prevNoGraphics != noGraphics) {
                    int grID = getGraphicPaxId(paxId);
                    Graphic paxGraphic = paxLyr.getGraphic(grID);
                    if ((Integer) paxGraphic.getAttributeValue("driverAccept") == 1) {
                        //Log.e("checkChangesPax", "Driver accepted");
                        Snackbar snackbar = Snackbar
                                .make(curView, "Driver accepted your ride", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("DISMISS", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        });

                        snackbar.show();
                    } else if ((Integer) paxGraphic.getAttributeValue("driverAccept") == 1) {
                        //make a arouting once again
                        Snackbar snackbar = Snackbar
                                .make(curView, "Driver did not accept your ride", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("DISMISS", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                routeTransfer(v);
                            }
                        });

                        snackbar.show();

                    } else {
                        Log.e("checkChangesPax", "No valid status");
                    }
                }
            }
        }, 0, period);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            setMaxbuffer();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public int getGraphicDiriverId(int oid){
        int grId=-1;
        //Log.e("DRIVER", "Searching...");
        for (int id: driverLyr.getGraphicIDs()){
            Graphic curGr = driverLyr.getGraphic(id);
            //Log.e("DRIVER", String.valueOf(curGr.getAttributeValue("OBJECTID")));
            if (curGr.getAttributeValue(driverLyr.getObjectIdField()).equals(oid)){
                //Log.e("DRIVER", String.valueOf(id));
                grId = id;
            }
        }
        return grId;
    }

    public int getGraphicPaxId(int oid){
        int grId=-1;
        //Log.e("PAAX", "Searching...");
        for (int id: paxLyr.getGraphicIDs()){
            Graphic curGr = paxLyr.getGraphic(id);
            //Log.e("PAX", String.valueOf(curGr.getAttributeValue("OBJECTID")));
            if (curGr.getAttributeValue(paxLyr.getObjectIdField()).equals(oid)){
                //Log.e("DRIVER", String.valueOf(id));
                grId = id;
            }

        }

        return grId;
    }

    public void updatePosition() {

        Log.e("DRIVER ID!!!", String.valueOf(driverId));
        if (MainActivity.isDriver && driverId != -1) {
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(driverLyr.getObjectIdField(), driverId);
            attributes.put("name", MainActivity.userName);
            attributes.put("curCap", MainActivity.curCap);
            attributes.put("maxCap", MainActivity.maxCap);
            attributes.put("model", MainActivity.carModel);
            attributes.put("sex", MainActivity.sex);
            attributes.put("age", MainActivity.age);
            attributes.put("status", driverStatus);
            attributes.put("rating", 4);
            attributes.put("regNumber", MainActivity.carReg);
            attributes.put("phone", MainActivity.userPhone);

            Graphic updateGraphic = new Graphic((Point) GeometryEngine
                    .project(wgsPos,
                            SpatialReference.create(4326),
                            map.getSpatialReference()), null, attributes);

            Graphic[] ups = {updateGraphic};
            driverLyr.applyEdits(null, null, ups, new CallbackListener<FeatureEditResult[][]>() {
                @Override
                public void onCallback(FeatureEditResult[][] featureEditResults) {
                    if (featureEditResults[2] != null && featureEditResults[2][0] != null && featureEditResults[2][0].isSuccess()) {
                        // Implement any required success logic here
                        Log.e("DRIVER Update", String.valueOf(featureEditResults[2][0].isSuccess()));
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("DRIVER Update", throwable.getLocalizedMessage());
                }
            });
        }

    }

    public void savePosition() {
        //save current data
        if (MainActivity.isDriver) {

            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put("name", MainActivity.userName);
            attributes.put("curCap", MainActivity.curCap);
            attributes.put("maxCap", MainActivity.maxCap);
            attributes.put("model", MainActivity.carModel);
            attributes.put("sex", MainActivity.sex);
            attributes.put("age", MainActivity.age);
            attributes.put("status", "free");
            attributes.put("rating", 4);
            attributes.put("regNumber", MainActivity.carReg);
            attributes.put("phone", MainActivity.userPhone);

            Graphic newFeatureGraphic = new Graphic((Point) GeometryEngine
                    .project(wgsPos,
                            SpatialReference.create(4326),
                            map.getSpatialReference()), null, attributes);

            Graphic[] adds = {newFeatureGraphic};
            driverLyr.applyEdits(adds, null, null, new CallbackListener<FeatureEditResult[][]>() {
                @Override
                public void onCallback(FeatureEditResult[][] featureEditResults) {
                    if (featureEditResults[0] != null && featureEditResults[0][0] != null && featureEditResults[0][0].isSuccess()) {

                        driverId = (int) featureEditResults[0][0].getObjectId();
                        Log.e("DRIVER", String.valueOf(featureEditResults[0][0].getObjectId()));


                        //try to add attachment
                        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                        File imageFile = new File(extStorageDirectory, "Profile.png");

                        OutputStream os;
                        try {
                            os = new FileOutputStream(imageFile);
                            MainActivity.photo.compress(Bitmap.CompressFormat.PNG, 100, os);
                            os.flush();
                            os.close();
                        } catch (Exception e) {
                            Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
                        }

                        driverLyr.addAttachment((int) featureEditResults[0][0].getObjectId(), imageFile, new CallbackListener<FeatureEditResult>() {
                            @Override
                            public void onCallback(FeatureEditResult featureEditResult) {
                                Log.e("DRIVER", String.valueOf(featureEditResult.isSuccess()));
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                Log.e("DRIVER", throwable.getMessage());

                            }
                        });
                    }

                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("ERROR", "error");

                }
            });

        }

    }


    public void updatePax(boolean isEmptyDriver) {

        //Log.e("PAX ID!!!", String.valueOf(paxLyr.getGraphic(paxGraphicId).getAttributeValue("OBJECTID")));
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(paxLyr.getObjectIdField(), paxLyr.getGraphic(paxGraphicId).getAttributeValue("OBJECTID"));
            attributes.put("driverAccept", driverAccept);
            attributes.put("status", paxStatus);
            if(isEmptyDriver){
                attributes.put("driver", -1);
            }



            Graphic updateGraphic = new Graphic(paxLyr.getGraphic(paxGraphicId).getGeometry(), null, attributes);

            Graphic[] ups = {updateGraphic};
            paxLyr.applyEdits(null, null, ups, new CallbackListener<FeatureEditResult[][]>() {
                @Override
                public void onCallback(FeatureEditResult[][] featureEditResults) {
                    if (featureEditResults[2] != null && featureEditResults[2][0] != null && featureEditResults[2][0].isSuccess()) {
                        Log.e("PAX Update", String.valueOf(featureEditResults[2][0].isSuccess()));
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("Pax Update", throwable.getLocalizedMessage());
                }
            });


    }

    public void zoomToCurrentLocation() {
        Point mapPoint = (Point) GeometryEngine
                .project(wgsPos,
                        SpatialReference.create(4326),
                        map.getSpatialReference());
        map.zoomToScale(mapPoint, 5000);
    }

    public void findPaxs(View view, boolean isFree) {
        //get all paxes with status free
        routeSolver.clear();

        //add driver location
        routeSolver.idGraphicDriver = routeSolver.stopsGraphics
                .addGraphic(new Graphic(GeometryEngine.project(wgsPos, SpatialReference.create(4326), SpatialReference.create(102100)), null));

        Polygon bufferRouteDriver = null;
        //get buffer of driver route
        if (!isFree && routeDriver!=null){
           bufferRouteDriver = GeometryEngine.buffer(routeDriver.getRouteGraphic().getGeometry(), map.getSpatialReference(), maxBuffer, map.getSpatialReference().getUnit());
        }

        for (int i : paxLyr.getGraphicIDs()) {

            if ((paxLyr.getGraphic(i).getAttributeValue("status").equals("free") && driverId != -1
                    && paxLyr.getGraphic(i).getAttributeValue("driver") != null
                    && paxLyr.getGraphic(i).getAttributeValue("paxAccept") != null
                    && driverId == Integer.valueOf((String) paxLyr.getGraphic(i).getAttributeValue("driver"))
                    && Integer.valueOf((int) paxLyr.getGraphic(i).getAttributeValue("paxAccept")) == 1) ||
                    //or already found as valid stop
                    (paxLyr.getGraphic(i).getAttributeValue("status").equals("full") && driverId != -1
                           && paxLyr.getGraphic(i).getAttributeValue("driver") != null )
                            && paxLyr.getGraphic(i).getAttributeValue("paxAccept") != null
                            && paxLyr.getGraphic(i).getAttributeValue("driverAccept") != null
                            && driverId == Integer.valueOf((String) paxLyr.getGraphic(i).getAttributeValue("driver"))
                            && Integer.valueOf((int) paxLyr.getGraphic(i).getAttributeValue("paxAccept")) == 1
                            && Integer.valueOf((int) paxLyr.getGraphic(i).getAttributeValue("driverAccept")) == 1){

                //check if input graphic inside buffer
                if (!isFree && bufferRouteDriver!=null){
                    //Log.e("PAX SEARCH!!!", "Start");
                    if(!GeometryEngine.contains(bufferRouteDriver, paxLyr.getGraphic(i).getGeometry(), map.getSpatialReference())){
                        Log.e("PAX SEARCH!!!", "Graphic is not inside buffer");
                        continue;

                    }

                }

                //Log.e("PAX Found OID!!!", String.valueOf(paxLyr.getGraphic(i).getAttributeValue("OBJECTID")));
                routeSolver.numberGr++;
                int startId = routeSolver.stopsGraphics.addGraphic(new Graphic(paxLyr.getGraphic(i).getGeometry(), null));
                routeSolver.listStopsOdd.add(startId);
                routeSolver.listStopsIds.add(startId);

                //Log.e("Start ", String.valueOf(startId));

                //add stop graphic
                Point pt = GeometryEngine.project((Double) paxLyr.getGraphic(i).getAttributeValue("lon"),
                        (Double) paxLyr.getGraphic(i).getAttributeValue("lat"), SpatialReference.create(map.getSpatialReference().getID()));
                int stopId = routeSolver.stopsGraphics.addGraphic(new Graphic(pt, null));
                routeSolver.listStopsEven.add(stopId);
                routeSolver.listStopsIds.add(stopId);

                //Log.e("Stop ", String.valueOf(stopId));
            }
        }


        //solve route
        if (routeSolver.listStopsIds.size() > 0) {

            Thread thread = new Thread() {
                @Override
                public void run() {
                    String routeSummary = routeSolver.solveRouting();
                    showSnackbar(routeSummary);
                }
            };

            thread.start();


        }

    }

    public void showSnackbar(String str) {
        String totalMileage = str.split(";")[0];
        String totalMinutes = str.split(";")[1];
        String strSnack = getString(R.string.RouteMileage) + " " + totalMileage.substring(0, 4) +
                " km " + getString(R.string.RouteTime) + " " + totalMinutes + " minutes.";
        Snackbar snackbar = Snackbar
                .make(curView, strSnack, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("DISMISS", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        snackbar.show();
    }

    public void showSnackbarClosestDriver(String str) {
        String totalMileage = str.split(";")[0];
        String totalCost = str.split(";")[1];
        String strSnack = getString(R.string.RouteMileage) + " " + totalMileage.substring(0, 4) +
                " km " + getString(R.string.RouteCost) + " " + totalCost + " MAD. ";
        Snackbar snackbar = Snackbar
                .make(curView, strSnack, Snackbar.LENGTH_LONG);
        snackbar.setAction("DISMISS", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        snackbar.show();
    }


    public void showSnackbarClosestDriverInfo() {

        //get driver data - attachement
        driverLyr.queryAttachmentInfos(Integer.valueOf(closestDriverOID), new CallbackListener<AttachmentInfo[]>() {
            @Override
            public void onCallback(AttachmentInfo[] attachmentInfos) {
                if (attachmentInfos.length > 0){
                    try {

                        //Log.e("SNACK Name" , attachmentInfos[0].getName());
                        //String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                        imageFileDriver = new File(getApplicationContext().getFilesDir().getPath() + File.separator + "image" + String.valueOf(fCount++), attachmentInfos[0].getName());
                        Log.e("SNACK path" , getApplicationContext().getFilesDir().getPath());
                        AttachmentManager man = new AttachmentManager(DisplayMap.this,driverLyr.getUrl(),null, Environment.getExternalStorageDirectory());

                        man.downloadAttachment(Long.valueOf(closestDriverOID), (int) attachmentInfos[0].getId(), attachmentInfos[0].getName(), new AttachmentManager.AttachmentDownloadListener() {
                            @Override
                            public void onAttachmentDownloading() {
                                //Log.e("SNACK 1" , "SNACK Downloafing");
                            }

                            @Override
                            public void onAttachmentDownloaded(File file) {
                                imageFileDriver = file;
                                showWindowDriver();

                            }

                            @Override
                            public void onAttachmentDownloadFailed(Exception e) {
                                Log.e("Error" , "ERROR");
                            }
                        });


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
    }


    private void showWindowDriver() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imgView = new ImageView(DisplayMap.this);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                imgView.setImageBitmap(BitmapFactory.decodeFile(imageFileDriver.getAbsolutePath(),bmOptions));


                AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMap.this);

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        paxAccept= 0;
                                        dialog.dismiss();
                                    }});
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        paxAccept= 1;
                                        dialog.dismiss();
                                        boolean del = imageFileDriver.delete();
                                        savePaxData();
                                        Log.e("SNACK Deleted???" , String.valueOf(del));
                                    }
                                });

                builder.setView(imgView);

                Graphic driverGraphic = driverLyr.getGraphic(driverLyr.getGraphicIDWithOID(Integer.valueOf(closestDriverOID)));
                int age = Integer.valueOf((String) driverGraphic.getAttributeValue("age"));
                String ageJenk = null;
                if (age==0){
                    ageJenk= "to 25";
                } else if(age==1) {
                    ageJenk= "form 25 to 65";
                } else  {
                    ageJenk= "over 65";
                }

                builder.setTitle(String.valueOf(driverGraphic.getAttributeValue("name")));
                String nameAndRank = getString(R.string.rank) + " " + String.valueOf(driverGraphic.getAttributeValue("rating")
                        + "/5 " + getString(R.string.phone) + " " + String.valueOf(driverGraphic.getAttributeValue("phone") + "\n"));

                String ageAndSex =  getString(R.string.sex) + " " + String.valueOf(driverGraphic.getAttributeValue("sex")) + " "
                        + getString(R.string.age) + " " + ageJenk + "\n" ;

                String modelAndReg = String.valueOf(driverGraphic.getAttributeValue("model")) + " "
                        + String.valueOf(driverGraphic.getAttributeValue("regNumber")) ;
                builder.setMessage(nameAndRank +"\n"+ ageAndSex +"\n"+ modelAndReg);

                AlertDialog dialog = builder.show();
                TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
                messageText.setGravity(Gravity.CENTER);

            }
        });
    }


    public void showSnackbarClosestPaxInfo () {

        paxLyr.queryAttachmentInfos(paxGraphicOid, new CallbackListener<AttachmentInfo[]>() {
            @Override
            public void onCallback(AttachmentInfo[] attachmentInfos) {
                if (attachmentInfos.length > 0){
                    try {

                        imageFilePax = new File(getApplicationContext().getFilesDir().getPath(), attachmentInfos[0].getName());
                        AttachmentManager man = new AttachmentManager(DisplayMap.this,paxLyr.getUrl(),null, Environment.getExternalStorageDirectory());

                        man.downloadAttachment(Long.valueOf(paxGraphicOid), (int) attachmentInfos[0].getId(), attachmentInfos[0].getName(), new AttachmentManager.AttachmentDownloadListener() {
                            @Override
                            public void onAttachmentDownloading() {
                            }

                            @Override
                            public void onAttachmentDownloaded(File file) {
                                imageFilePax = file;
                                showWindowPax();

                            }

                            @Override
                            public void onAttachmentDownloadFailed(Exception e) {
                                Log.e("ERROR" , "ERROR 1");
                            }
                        });


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ;
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });

    }

    private void showWindowPax() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imgView = new ImageView(DisplayMap.this);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                imgView.setImageBitmap(BitmapFactory.decodeFile(imageFilePax.getAbsolutePath(),bmOptions));


                AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMap.this);

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        driverAccept= 0;
                        paxStatus = "full";
                        boolean del = imageFilePax.delete();
                        updatePax(true);

                        dialog.dismiss();
                    }});
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        driverAccept= 1;
                        dialog.dismiss();
                        boolean del = imageFilePax.delete();
                        MainActivity.curCap =  MainActivity.curCap + Integer.valueOf((String) paxLyr.getGraphic(paxGraphicId).getAttributeValue("notes"));
                        driverStatus = "search";
                        updatePosition();

                        int paxBuffer = (Integer) paxLyr.getGraphic(paxGraphicId).getAttributeValue("buffer");
                        //Log.e("SNACK PAX BUFFER???" , String.valueOf(paxBuffer));
                        if (paxBuffer < maxBuffer){
                            maxBuffer = paxBuffer;
                        }

                        paxStatus = "full";
                        updatePax(false);

                    }
                });

                builder.setView(imgView);

                int age = Integer.valueOf((String) paxLyr.getGraphic(paxGraphicId).getAttributeValue("age"));
                String ageJenk = null;
                if (age==0){
                    ageJenk= "to 25";
                } else if(age==1) {
                    ageJenk= "form 25 to 65";
                }else  {
                    ageJenk= "over 65";
                }

                builder.setTitle(String.valueOf(paxLyr.getGraphic(paxGraphicId).getAttributeValue("name")));
                String nameAndRank = getString(R.string.phone) + " " + String.valueOf(paxLyr.getGraphic(paxGraphicId).getAttributeValue("phone") + "\n");

                String ageAndSex =  getString(R.string.sex) + " " + String.valueOf(paxLyr.getGraphic(paxGraphicId).getAttributeValue("sex")) + " "
                        + getString(R.string.age) + " " + ageJenk + "\n" ;

                builder.setMessage(nameAndRank +"\n"+ ageAndSex);

                AlertDialog dialog = builder.show();
                TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
                messageText.setGravity(Gravity.CENTER);

            }
        });
    }




    @Override
    public void onStart() {
        super.onStart();

        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "DisplayMap Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.adam.tabbedapp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "DisplayMap Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.adam.tabbedapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();

    }


    /////
    /////
    /////////////////Class for map interaction
    ////
    ////

    class TouchListener extends MapOnTouchListener {

        private int routeHandle = -1;

        @Override
        public void onLongPress(MotionEvent point) {
            routeSolver.clear();
            map.getCallout().animatedHide();
            routeSolver.clear();
        }

        @Override
        public boolean onSingleTap(MotionEvent point) {


            // Add a graphic to the screen for the touch event
            Point mapPoint = map.toMapPoint(point.getX(), point.getY());
            if(addStartMap){
                startingPoint = mapPoint;
                SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(Color.GREEN, 12, SimpleMarkerSymbol.STYLE.CIRCLE);
                Graphic graphic = new Graphic(mapPoint, symbol);
                grLyr.addGraphic(graphic);
                showEndPointDialog();
                addStartMap = false;
            }
            if(addEndtMap){
                endingPoint = mapPoint;
                SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(Color.RED, 12, SimpleMarkerSymbol.STYLE.CIRCLE);
                Graphic graphic = new Graphic(mapPoint, symbol);
                grLyr.addGraphic(graphic);
                setNumberOfPlaces();
                addEndtMap= false;
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent point) {
            return true;
        }

        public TouchListener(Context context, MapView view) {
            super(context, view);
        }
    }

    public void createInputForGeocoding(boolean isStart){
        //get voice input
        if (isStart){
            promptSpeechInput(true);
            showEndPointDialog();
        } else {
            promptSpeechInput(false);
            setNumberOfPlaces();
        }
    }

    public  void setNumberOfPlaces() {

        AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMap.this);
        builder.setTitle(R.string.SeetingNoPlaces).setItems(R.array.placesArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                numberPlaces = Integer.parseInt(getResources().getStringArray(R.array.placesArray)[which]);
                //routing for input and output
                routeTransfer(curView);
            }

        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void setMaxbuffer() {

        AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMap.this);
        builder.setTitle(R.string.SettingMaxBuffer).setItems(R.array.bufferArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                maxBuffer = Integer.parseInt(getResources().getStringArray(R.array.bufferArray)[which]);
            }

        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void promptSpeechInput(boolean isStart) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            if(isStart){
                //Log.e("address ", "isStart");
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            } else {
                //Log.e("address NO ", "isStart");
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT_END);
            }

        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT : {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    geocodeAddress(result.get(0));

                }
                break;
            }

            case REQ_CODE_SPEECH_INPUT_END: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    geocodeAddress(result.get(0));

                }
                break;
            }

        }
    }

    private void geocodeAddress(String s) {

        LocatorFindParameters parameters = new LocatorFindParameters(s);
        parameters.setMaxLocations(1);
        parameters.setOutSR(map.getSpatialReference());

        locator.find(parameters, new CallbackListener<List<LocatorGeocodeResult>>() {
            @Override
            public void onCallback(List<LocatorGeocodeResult> locatorGeocodeResults) {
                LocatorGeocodeResult result = locatorGeocodeResults.get(0);
                if (result !=null){
                    SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(Color.GREEN, 12, SimpleMarkerSymbol.STYLE.CIRCLE);
                    if (!addStartMap) {
                        startingPoint = result.getLocation();
                        addStartMap=true;
                    }

                    if (!addEndtMap) {
                        endingPoint = result.getLocation();
                        addEndtMap=true;
                        symbol = new SimpleMarkerSymbol(Color.RED, 12, SimpleMarkerSymbol.STYLE.CIRCLE);
                    }
                    map.centerAt(result.getLocation(), true);
                    Graphic graphic = new Graphic(result.getLocation(), symbol);
                    grLyr.addGraphic(graphic);
                    String message =  getString(R.string.foundAddress) + " " + result.getAddress();
                    Snackbar.make(curView, message, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                }
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("Geocoding Error ", String.valueOf("Error while geociding address"));

            }
        });

    }

    public void showEndPointDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMap.this);
        builder.setMessage(R.string.AddingStops).setTitle(R.string.AddingEndingTitle);
        builder.setPositiveButton(R.string.AddingStopsMap, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                addEndtMap = true;
            }
        });
        builder.setNegativeButton(R.string.AddingStopsVoice, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                addEndtMap = false;
                createInputForGeocoding(false);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();


    }

    public void routeTransfer (View view) {
        //get all paxes with status free
        routeSolver.clear();

        //add pax location
        routeSolver.idGraphicDriver = routeSolver.stopsGraphics
                .addGraphic(new Graphic(startingPoint, null));

        for (int i : driverLyr.getGraphicIDs()) {
            if (driverLyr.getGraphic(i).getAttributeValue("status").equals("free")
                    || driverLyr.getGraphic(i).getAttributeValue("status").equals("search")) {

                //check number of places available
                int maxCapDriver = ((Integer) driverLyr.getGraphic(i).getAttributeValue("maxCap"));
                int curCapDriver = ((Integer) driverLyr.getGraphic(i).getAttributeValue("curCap"));
                if (maxCapDriver - curCapDriver >= numberPlaces){
                    routeSolver.numberGr++;
                    int startId = routeSolver.stopsGraphics.addGraphic(new Graphic(driverLyr.getGraphic(i).getGeometry(), null));
                    routeSolver.listStopsOdd.add(startId);
                }
            }
        }


        //solve route
        if (routeSolver.numberGr > 0) {

            Thread thread = new Thread() {
                @Override
                public void run() {
                    String distanceAndId = routeSolver.findClosestDriver(routeSolver.idGraphicDriver,  routeSolver.listStopsOdd);

                    double minDist = Double.valueOf(distanceAndId.split(";")[0]);
                    closestDriverOID = distanceAndId.split(";")[1];
                    showSnackbarClosestDriver(String.valueOf(minDist) + ";" +
                            String.valueOf(Math.ceil((minDist*6)+7)));

                    showSnackbarClosestDriverInfo();
                }
            };

            thread.start();

        }

    }

    public void savePaxData() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("name", MainActivity.userName);
        attributes.put("sex", MainActivity.sex);
        attributes.put("age", MainActivity.age);
        attributes.put("status", paxStatus);
        attributes.put("phone", MainActivity.userPhone);
        attributes.put("buffer", maxBuffer);
        attributes.put("driver", String.valueOf(closestDriverOID));
        attributes.put("paxAccept", paxAccept);
        attributes.put("notes", String.valueOf(numberPlaces));


        Point endPoint4326 = (Point) GeometryEngine.project(endingPoint, map.getSpatialReference(), SpatialReference.create(4326));
        attributes.put("lat", endPoint4326.getY());
        attributes.put("lon", endPoint4326.getX());


        Graphic newFeatureGraphic = new Graphic(startingPoint,
                        null, attributes);

        Graphic[] adds = {newFeatureGraphic};
        paxLyr.applyEdits(adds, null, null, new CallbackListener<FeatureEditResult[][]>() {
            @Override
            public void onCallback(FeatureEditResult[][] featureEditResults) {
                if (featureEditResults[0] != null && featureEditResults[0][0] != null && featureEditResults[0][0].isSuccess()) {

                    paxId = (int) featureEditResults[0][0].getObjectId();
                    //Log.e("PAX!!!", String.valueOf(featureEditResults[0][0].getObjectId()));
                    //try to add attachment
                    String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
                    File imageFile = new File(extStorageDirectory, "Profile.png");

                    OutputStream os;
                    try {
                        os = new FileOutputStream(imageFile);
                        MainActivity.photo.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                        os.close();
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
                    }

                    paxLyr.addAttachment((int) featureEditResults[0][0].getObjectId(), imageFile, new CallbackListener<FeatureEditResult>() {
                        @Override
                        public void onCallback(FeatureEditResult featureEditResult) {
                            Log.e("PAX", String.valueOf(featureEditResult.isSuccess()));
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.e("PAX", throwable.getMessage());

                        }
                    });
                }

            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("ERROR!!!", "errr");

            }
        });

    }
}
