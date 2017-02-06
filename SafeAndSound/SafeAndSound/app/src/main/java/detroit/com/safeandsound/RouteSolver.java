package detroit.com.safeandsound;

import android.graphics.Color;
import android.util.Log;

import com.esri.arcgisruntime.UnitSystem;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.BarrierType;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionsStyle;
import com.esri.arcgisruntime.tasks.networkanalysis.PolygonBarrier;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteShapeType;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;

/**
 * Created by Adam on 2017-01-16.
 */
public class RouteSolver {

    private static RouteSolver instance;
    private static RouteTask routeTask;
    private static RouteParameters routeParameters;
    private static SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.rgb(66, 133, 244), 3);
    private static boolean addMyCurrentLocation = false;

    public static RouteSolver getInstance(RouteTask task) {
        if (instance == null) {
            instance = new RouteSolver();
            routeTask = task;

        }
        return instance;
    }

    public void setAddMyCurrentLocation(boolean setLocation) {
        addMyCurrentLocation = setLocation;
    }

    public Route solveRoute() throws ExecutionException, InterruptedException {

        setRoutingParameters();
        Route mRoute = null;

        try {
            ListenableFuture<RouteResult> res = routeTask.solveRouteAsync(routeParameters);
            RouteResult result = res.get();
            mRoute = result.getRoutes().get(0);
            Graphic routeGraphic = new Graphic(mRoute.getRouteGeometry(), lineSymbol);
            DisplayMap.getRouteOverlay().getGraphics().add(routeGraphic);

        } catch (InterruptedException | ExecutionException e) {
            Log.e("ERROR", e.getCause().getMessage());
        }
        return mRoute;

}

    private void setRoutingParameters() {
        try {
            routeParameters = routeTask.createDefaultParametersAsync().get();

            int travelModeId = getTravelModeId();
            routeParameters.setTravelMode(routeTask.getRouteTaskInfo().getTravelModes().get(travelModeId));

            //set stops order
            routeParameters.setFindBestSequence(DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_STOP_BEST_SEQ, true));
            routeParameters.setPreserveFirstStop(DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_STOP_FIRST, true));
            routeParameters.setPreserveLastStop(DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_STOP_LAST, true));

            //set add barriers
            if (DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_CUR_TRA_INC, false)) {
                addBarriers();
            }


            //set time
            routeParameters.setStartTime(Calendar.getInstance());

            //CONSTANTS
            routeParameters.setReturnRoutes(true);
            routeParameters.setRouteShapeType(RouteShapeType.TRUE_SHAPE_WITH_MEASURES);
            routeParameters.setOutputSpatialReference(SpatialReference.create(3857));
            routeParameters.setReturnDirections(true);
            routeParameters.setDirectionsLanguage("en");
            routeParameters.setDirectionsDistanceUnits(UnitSystem.METRIC);
            routeParameters.setDirectionsStyle(DirectionsStyle.NAVIGATION);
            routeParameters.getStops().clear();

            if(addMyCurrentLocation){
                routeParameters.getStops().add(new Stop(LocationChangeListener.getLastPosition()));;
            }

            for (Graphic gr : DisplayMap.getStopsOverlay().getGraphics()) {
                routeParameters.getStops().add(new Stop((Point) gr.getGeometry()));
            }


        } catch (InterruptedException | ExecutionException e) {
            Log.e("ERROR", e.getCause().getMessage());
        }


    }

    private static void addBarriers() {
        routeParameters.getPolygonBarriers().clear();

        for (Graphic gr : DisplayMap.getTrafficIncOverlay().getGraphics()) {
            if (gr.getGeometry().getGeometryType() == GeometryType.POLYGON) {
                PolygonBarrier br = new PolygonBarrier((Polygon) gr.getGeometry());
                br.setType(BarrierType.RESTRICTION);
                routeParameters.getPolygonBarriers().add(br);
            }
        }
    }

    private int getTravelModeId() {
        int modeId = 0;
        String driveMode = DisplayMap.getSharedPreferences().getString(Settings.ROUTE_MODE, null);
        switch (driveMode) {
            case Settings.ROUTE_MODE_FASTEST:
                if (!DisplayMap.getSharedPreferences().getString(Settings.ROUTE_TRANSIT, null).equalsIgnoreCase(Settings.ROUTE_TRANSIT_DRIVING)) {
                    modeId = getIdFromName("walkTime");
                } else {
                    modeId = getIdFromName("driveTime");
                }
                break;
            case Settings.ROUTE_MODE_SHORTEST:
                if (!DisplayMap.getSharedPreferences().getString(Settings.ROUTE_TRANSIT, null).equalsIgnoreCase(Settings.ROUTE_TRANSIT_DRIVING)) {
                    modeId = getIdFromName("walkLength");
                } else {
                    modeId = getIdFromName("driveLength");
                }
                break;
            case Settings.ROUTE_MODE_SAFEST:
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR);
                if (!DisplayMap.getSharedPreferences().getString(Settings.ROUTE_TRANSIT, null).equalsIgnoreCase(Settings.ROUTE_TRANSIT_DRIVING)) {
                    modeId = getIdFromName("inc" + String.valueOf(hour) + "walk");
                } else {
                    modeId = getIdFromName("inc" + String.valueOf(hour) + "drive");
                }
                break;
        }

        return modeId;

    }

    private int getIdFromName(String modeName) {
        int id = 0;
        switch (modeName) {
            case "inc0drive":
                id = 0;
                break;
            case "inc0walk":
                id = 1;
                break;
            case "inc1walk":
                id = 2;
                break;
            case "inc2walk":
                id = 3;
                break;
            case "inc3walk":
                id = 4;
                break;
            case "inc4walk":
                id = 5;
                break;
            case "inc5walk":
                id = 6;
                break;
            case "inc6walk":
                id = 7;
                break;
            case "inc7walk":
                id = 8;
                break;
            case "inc8walk":
                id = 9;
                break;
            case "inc9walk":
                id = 10;
                break;
            case "inc10walk":
                id = 11;
                break;
            case "inc11walk":
                id = 12;
                break;
            case "inc1drive":
                id = 13;
                break;
            case "inc2drive":
                id = 14;
                break;
            case "inc3drive":
                id = 15;
                break;
            case "inc4drive":
                id = 16;
                break;
            case "inc5drive":
                id = 17;
                break;
            case "inc6drive":
                id = 18;
                break;
            case "inc7drive":
                id = 19;
                break;
            case "inc8drive":
                id = 20;
                break;
            case "inc9drive":
                id = 21;
                break;
            case "inc10drive":
                id = 22;
                break;
            case "inc11drive":
                id = 23;
                break;
            case "driveTime":
                id = 24;
                break;
            case "walkTime":
                id = 25;
                break;
            case "driveLength":
                id = 26;
                break;
            case "walkLength":
                id = 27;
                break;
        }
        return id;
    }

}
