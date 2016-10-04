package com.example.adam.tabbedapp;

import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import com.esri.android.map.GraphicsLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;
import com.esri.core.symbol.SimpleMarkerSymbol.STYLE;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Adam on 2016-09-13.
 */


public class RouteSolver {


    public GraphicsLayer stopsGraphics, routeGraphics;
    public static SimpleFillSymbol fillSymbol = new SimpleFillSymbol(Color.GREEN);

    public static NAFeaturesAsFeature stopsNA;
    public static RouteTask network;
    public static ArrayList<Integer> listStopsIds;
    public static ArrayList<Integer> listStopsOdd;
    public static ArrayList<Integer> listStopsEven;
    public static ArrayList<Integer> listStopsVisited;
    public static int idGraphicDriver;
    public static double distanceBuf = 200;
    public static int noRoutes= 0;
    public static int numberGr;


    public RouteSolver(){

        fillSymbol.setAlpha(150);
        listStopsIds = new ArrayList<>();
        listStopsOdd = new ArrayList<>();
        listStopsEven = new ArrayList<>();
        listStopsVisited = new ArrayList<>();

        stopsGraphics = new GraphicsLayer();
        DisplayMap.map.addLayer(stopsGraphics);

        routeGraphics = new GraphicsLayer();
        DisplayMap.map.addLayer(routeGraphics);

        stopsNA = new NAFeaturesAsFeature();
        stopsNA.setSpatialReference( DisplayMap.map.getSpatialReference());

        try {
            network = RouteTask.createLocalRouteTask(
                    Environment.getExternalStorageDirectory().toString() +"/EnCommun/gisdatabase.geodatabase", "Car_ND");
        } catch (Exception e) {
            Log.d("NoNetworkData", e.getLocalizedMessage());
        }

    }

    public Route solveTask() throws Exception {

        RouteParameters routeParameters;
        routeParameters = network.retrieveDefaultRouteTaskParameters();
        routeParameters.setStops(stopsNA);
        routeParameters.setOutSpatialReference( DisplayMap.map.getSpatialReference());
        routeParameters.setReturnDirections(false);
        routeParameters.setFindBestSequence(false);
        routeParameters.setPreserveFirstStop(true);
        routeParameters.setReturnStops(true);
        Calendar c = Calendar.getInstance();
        routeParameters.setStartTime(c.getTime());
        RouteResult result = network.solve(routeParameters);
        Route topRoute = result.getRoutes().get(0);
        return topRoute;

    }

    public String solveRouting() {

        String routeSummary = null;
        HashMap<String, Object> attributes = new HashMap<>();
        stopsNA.clearFeatures();
        if (listStopsIds.size() == 2) {

            // make a simple route since there is only one stop
            attributes.put("Name", "Start");
            stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                    idGraphicDriver).getGeometry(), attributes));
            attributes.clear();
            // add stops
            for (int i = 0; i < listStopsIds.size(); i++) {

                Graphic symbolBack = new Graphic(stopsGraphics.getGraphic(
                        listStopsIds.get(i)).getGeometry(),  new SimpleMarkerSymbol(Color.BLUE, 16, STYLE.CIRCLE));
                int symbolBackId = routeGraphics.addGraphic(symbolBack);
                routeGraphics.updateGraphic(symbolBackId, 100);

                Graphic symbolFront = new Graphic(stopsGraphics.getGraphic(
                        listStopsIds.get(i)).getGeometry(), new TextSymbol(10, String.valueOf(i+1), Color.WHITE, TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE));
                int symbolFrontId = routeGraphics.addGraphic(symbolFront);
                routeGraphics.updateGraphic(symbolFrontId, 101);


                attributes.put("Name", "Stop" + listStopsIds.get(i));
                stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                        listStopsIds.get(i)).getGeometry(), attributes));
                attributes.clear();

            }

            // find route and add result to map
            try{
                Route routeFinal = solveTask();
                DisplayMap.routeDriver = routeFinal;
                addRouteToMap(routeFinal);
                DisplayMap.map.setExtent(GeometryEngine.buffer(routeFinal.getEnvelope(), DisplayMap.map.getSpatialReference(), 400,  DisplayMap.map.getSpatialReference().getUnit()));


                routeSummary = String.valueOf(routeFinal.getTotalKilometers()) + ";" +
                        String.valueOf(Math.ceil(routeFinal.getTotalMinutes()));

            } catch (Exception e){
                Log.d("ErrorAddRoute", e.getLocalizedMessage());
            }

        } else {
            // routing 4 points and more
            // None of stops were visited
            ///TEST
            /////
            //listStopsVisited.add(11);
            /////
            /////
            //if (listStopsVisited.isEmpty()) {

            Log.e("hrer", "before 3");
            ArrayList<Integer> finalOrderedListOfStops = new ArrayList<>();
            finalOrderedListOfStops.add(idGraphicDriver);

            ArrayList<Integer> tempStopsOdd = listStopsOdd;
            ArrayList<Integer> tempStopsEven = listStopsEven;
            Log.e("tempStopsOdd", String.valueOf(tempStopsOdd.size()));
            Log.e("tempStopsEven", String.valueOf(tempStopsEven.size()));

            if (!listStopsVisited.isEmpty()) {
                for (int id: listStopsVisited){
                    if(tempStopsOdd.contains(id)){
                        tempStopsOdd.set(tempStopsOdd.indexOf(id), tempStopsEven.get(tempStopsOdd.indexOf(id)));
                    } else {
                        tempStopsEven.remove((Object) id);
                    }
                }

            }

            int tempStopsSize = tempStopsOdd.size() + 2 - listStopsVisited.size();
            int firstStop = idGraphicDriver;

            while (tempStopsSize > 0){
                int closestId = findClosestStopsList(firstStop, tempStopsOdd);
                firstStop = closestId;
                finalOrderedListOfStops.add(closestId);
//                if (listStopsEven.size() ==1){
//                    //finalOrderedListOfStops.add(listStopsEven.get(0));
//                    //findClosestStopsList(closestId, listStopsEven);
//                }

                if (!tempStopsEven.contains(closestId)){
                    tempStopsOdd.set(tempStopsOdd.indexOf(closestId), tempStopsEven.get(tempStopsOdd.indexOf(closestId)));

                } else {
                    tempStopsOdd.remove((Object) closestId);
                    tempStopsEven.remove((Object) closestId);
                }
                tempStopsSize--;

            }

            stopsNA.clearFeatures();
            for (int id: finalOrderedListOfStops){
                stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                        id).getGeometry()));
            }
            try{

                Route routeFinal = solveTask();
                DisplayMap.routeDriver = routeFinal;
                DisplayMap.map.setExtent(GeometryEngine.buffer(routeFinal.getEnvelope(), DisplayMap.map.getSpatialReference(), 400,  DisplayMap.map.getSpatialReference().getUnit()));
                routeSummary = String.valueOf(routeFinal.getTotalKilometers()) + ";" +
                        String.valueOf(Math.ceil(routeFinal.getTotalMinutes()));
                //Log.e("Final Result ", String.valueOf(routeFinal.getTotalKilometers()));


            } catch (Exception e){
                Log.d("ErrorAddRoute", e.getLocalizedMessage());
            }

        }
        return routeSummary;

    }

    public int findClosestStopsList(int idGraphicDriver2,
                                     ArrayList<Integer> tempStops) {
        HashMap<String, Object> attributes = new HashMap<>();
        int closestId = -99999;
        double minDist = Double.MAX_VALUE;
        Graphic interStop = null;
        for (int i = 0; i < tempStops.size(); i++) {
            stopsNA.clearFeatures();
            // add start point
            attributes.put("Name", "Start");
            stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                    idGraphicDriver2).getGeometry(), attributes));
            attributes.clear();

            // add end point
            attributes.put("Name", "Stop" + tempStops.get(i));
            stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                    tempStops.get(i)).getGeometry(), attributes));
            attributes.clear();

            // find route and add result to map
            try {
                Route route = solveTask();
                double curDist = route.getTotalKilometers();
                if(curDist < minDist){
                    minDist = curDist;
                    closestId = tempStops.get(i);
                    interStop = route.getRouteGraphic();

                }

            } catch (Exception e) {
                Log.d("ErrorRouting", e.getLocalizedMessage());
            }

        }



        if (interStop != null){

            //add intermediate stops graphics
            SimpleLineSymbol symbolLine = null;
            switch (noRoutes){
                case 0:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#ffaaef"), 8);
                    break;
                case 1:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#fd9cea"), 7);
                    break;
                case 2:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#ff2cd6"), 6);
                    break;
                case 3:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#fd12cf"), 5);
                    break;
                case 4:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#e734c5"), 4);
                    break;
                case 5:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#dd0eb5"), 3);
                    break;
                case 6:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#9a007c"), 2);
                    break;
                default:
                    symbolLine = new SimpleLineSymbol(Color.parseColor("#ffaaef"), 2);
                    break;

            }
            routeGraphics.addGraphic(new Graphic(interStop.getGeometry(), symbolLine));
            noRoutes = noRoutes + 1;
            Point pointLabel = ((Polyline) interStop.getGeometry()).getPoint(((Polyline) interStop.getGeometry()).getPointCount()-1);
            Graphic symbolBack = new Graphic(pointLabel,  new SimpleMarkerSymbol(Color.BLUE, 16, STYLE.CIRCLE));
            int symbolBackId = routeGraphics.addGraphic(symbolBack);
            routeGraphics.updateGraphic(symbolBackId, 100);

            Graphic symbolFront = new Graphic(pointLabel, new TextSymbol(10, String.valueOf(noRoutes), Color.WHITE, TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE));
            int symbolFrontId = routeGraphics.addGraphic(symbolFront);
            routeGraphics.updateGraphic(symbolFrontId, 101);

            //zoom to route graphic
            DisplayMap.map.setExtent(GeometryEngine.buffer(interStop.getGeometry(),
                    DisplayMap.map.getSpatialReference(), 100,  DisplayMap.map.getSpatialReference().getUnit()));


        }

        return closestId;
    }

    public void addRouteToMap(Route rout) {
        Graphic routeGraphic = new Graphic(
                rout.getRouteGraphic().getGeometry(), new SimpleLineSymbol(Color.parseColor("#fd9cea"), 4));

        routeGraphics.addGraphic(routeGraphic);

    }

    //create buffer around input route
    public void createBufferArea(Geometry poly) {
        //project
        Polyline p = (Polyline) poly;
        p.getPoint(p.getPointCount()-1);
        Polygon inputBuffer = (Polygon) GeometryEngine.project(GeometryEngine.buffer(GeometryEngine.project(poly,  DisplayMap.map.getSpatialReference(), SpatialReference.create(54032)),
                SpatialReference.create(54032), distanceBuf, SpatialReference.create(54032).getUnit()),SpatialReference.create(54032),  DisplayMap.map.getSpatialReference());
        routeGraphics.addGraphic(new Graphic(inputBuffer, fillSymbol));

    }
    //clear data
    public void clear(){

        for(int kk : listStopsIds){
            if(kk != idGraphicDriver)
                stopsGraphics.removeGraphic(kk);
        }
        routeGraphics.removeAll();
        stopsNA.clearFeatures();
        listStopsIds.clear();
        listStopsOdd.clear();
        listStopsEven.clear();
        listStopsVisited.clear();
        noRoutes = 0;


    }


    public String findClosestDriver (int idGraphicDriver2,
                                    ArrayList<Integer> tempStops) {
        HashMap<String, Object> attributes = new HashMap<>();
        int closestId = -99999;
        double minDist = Double.MAX_VALUE;
        String totalDistanceAndClosest = null;
        String oidClosest= null;
        Graphic interStop = null;
        for (int i = 0; i < tempStops.size(); i++) {
            stopsNA.clearFeatures();
            // add start point
            attributes.put("Name", "Start");
            stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                    idGraphicDriver2).getGeometry(), attributes));
            attributes.clear();

            // add end point
            attributes.put("Name", "Stop" + tempStops.get(i));
            stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                    tempStops.get(i)).getGeometry(), attributes));
            attributes.clear();

            // find route and add result to map
            try {
                Route route = solveTask();
                double curDist = route.getTotalKilometers();
                if(curDist < minDist){
                    minDist = curDist;
                    closestId = tempStops.get(i);
                    interStop = route.getRouteGraphic();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (interStop!=null){
            SimpleLineSymbol symbolLine = new SimpleLineSymbol(Color.parseColor("#2B2626"), 3);
            symbolLine.setStyle(SimpleLineSymbol.STYLE.DOT);
            Graphic gr = new Graphic(interStop.getGeometry(), symbolLine);
            int idInter = routeGraphics.addGraphic(gr);
            routeGraphics.sendToBack(idInter);

            //get id of closest driver
            int pointCount = ((Polyline) interStop.getGeometry()).getPointCount();
            Geometry geomToTest = GeometryEngine.buffer(((Polyline) interStop.getGeometry()).getPoint(pointCount-1), DisplayMap.map.getSpatialReference(), 200, SpatialReference.create(102100).getUnit()) ;
            for (int ii: DisplayMap.driverLyr.getGraphicIDs()) {
                if(GeometryEngine.contains(geomToTest, DisplayMap.driverLyr.getGraphic(ii).getGeometry(), DisplayMap.map.getSpatialReference())){
                    oidClosest = String.valueOf(DisplayMap.driverLyr.getGraphic(ii).getAttributeValue("OBJECTID"));
                }
            }
        }

        if (closestId != -99999){
            stopsNA.clearFeatures();

            stopsNA.addFeature(new StopGraphic(stopsGraphics.getGraphic(
                    idGraphicDriver2).getGeometry(), null));

            stopsNA.addFeature(new StopGraphic(DisplayMap.startingPoint, null));
            stopsNA.addFeature(new StopGraphic(DisplayMap.endingPoint, null));

            try{
                Route route = solveTask();
                DisplayMap.routeDriver = route;
                Graphic routeGraphic = route.getRouteGraphic();
                totalDistanceAndClosest = String.valueOf(route.getTotalKilometers()) + ";" + oidClosest;

                Graphic gr = new Graphic(routeGraphic.getGeometry(), new SimpleLineSymbol(Color.parseColor("#FF0000"), 3, SimpleLineSymbol.STYLE.DOT));
                int idRoute = routeGraphics.addGraphic(gr);
                routeGraphics.sendToBack(idRoute);

                DisplayMap.map.setExtent(GeometryEngine.buffer(routeGraphic.getGeometry(),
                        DisplayMap.map.getSpatialReference(), 100,  DisplayMap.map.getSpatialReference().getUnit()));

            } catch (Exception e) {
                Log.d("ErrorRouting", e.getLocalizedMessage());
            }
        }

        return totalDistanceAndClosest;

    }

}
