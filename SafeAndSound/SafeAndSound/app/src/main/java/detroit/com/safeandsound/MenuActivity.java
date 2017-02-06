package detroit.com.safeandsound;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.LayerList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class MenuActivity extends PreferenceActivity  {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            final LayerList lyrList = DisplayMap.getMapView().getMap().getOperationalLayers();

            final SwitchPreference incSwitch = (SwitchPreference) this.findPreference("incident_layer_sp");
            incSwitch.setChecked(DisplayMap.getSharedPreferences().getBoolean(Settings.SHOW_INC_LAYER, false));
            incSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    boolean isVisible = (Boolean) o;
                    Log.e("LAYR", String.valueOf(isVisible));
                    if (isVisible){
                        DisplayMap.getMapView().getMap().getOperationalLayers().add(RenderLayers.visualizeSafety());
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.SHOW_INC_LAYER, true).commit();
                    } else {
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.SHOW_INC_LAYER, false).commit();
                        for (Layer lyr: lyrList){
                            if (lyr.getId().equalsIgnoreCase("IncidentDensity")){
                                lyrList.remove(lyr);

                            }
                        }
                    }
                    incSwitch.setChecked(isVisible);
                    return  isVisible;
                }
            });


            //traffic layer

            final SwitchPreference trafficSwitch = (SwitchPreference) this.findPreference("traffic_layer_sp");
            trafficSwitch.setChecked(DisplayMap.getSharedPreferences().getBoolean(Settings.SHOW_TRA_LAYER, false));
            trafficSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    boolean isVisible = (Boolean) o;
                    Log.e("LAYR", String.valueOf(isVisible));
                    if (isVisible){
                        DisplayMap.getMapView().getMap().getOperationalLayers().add(
                                RenderLayers.visualizeTraffic(getResources().getString(R.string.traffic_data)));
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.SHOW_TRA_LAYER, true).commit();
                    } else {
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.SHOW_TRA_LAYER, false).commit();
                        for (Layer lyr: lyrList){
                            if (lyr.getId().equalsIgnoreCase("TrafficLayer")){
                                lyrList.remove(lyr);

                            }
                        }
                    }
                    trafficSwitch.setChecked(isVisible);
                    return  isVisible;
                }
            });


            //add_users_sp

            final SwitchPreference usersSwitch = (SwitchPreference) this.findPreference("add_users_sp");
            usersSwitch.setChecked(DisplayMap.getSharedPreferences().getBoolean(Settings.SHOW_USR_LAYER, false));
            usersSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    boolean isVisible = (Boolean) o;
                    Log.e("LAYR", String.valueOf(isVisible));
                    if (isVisible){
                        DisplayMap.getUsersLayer().setVisible(true);
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.SHOW_USR_LAYER, true).commit();

                        Set<String> hs = DisplayMap.getSharedPreferences().getStringSet(Settings.MY_FRIENDS, new HashSet<String>());
                        if (hs.isEmpty()) {
                            return isVisible;
                        }
                        Iterator iterator = hs.iterator();
                        int i = 0;
                        String defQuery = "OBJECTID = ";
                        while (iterator.hasNext()) {
                            if (i >= 1){
                                defQuery = defQuery + " OR OBJECTID = ";
                            }
                            String val = (String) iterator.next();
                            Log.e("FRIENDS ID", val);
                            defQuery = defQuery + val;
                            i++;
                        }
                        Log.e("defQuery ", defQuery);
                        DisplayMap.getUsersLayer().setDefinitionExpression(defQuery);
                        DisplayMap.getUsersLayer().resetFeaturesVisible();


                    } else {
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.SHOW_USR_LAYER, false).commit();
                        DisplayMap.getUsersLayer().setVisible(false);
                    }
                    usersSwitch.setChecked(isVisible);
                    return  isVisible;
                }
            });

            final ListPreference modeTravel = (ListPreference) this.findPreference("route_mode_lp");
            String selValuePre =  DisplayMap.getSharedPreferences().getString(Settings.ROUTE_MODE, "");
            switch (selValuePre){
                case Settings.ROUTE_MODE_SAFEST:
                    modeTravel.setValueIndex(0);
                    modeTravel.setSummary("Safest");
                    break;
                case Settings.ROUTE_MODE_FASTEST:
                    modeTravel.setValueIndex(1);
                    modeTravel.setSummary("Fastest");
                    break;
                case Settings.ROUTE_MODE_SHORTEST:
                    modeTravel.setValueIndex(2);
                    modeTravel.setSummary("Shortest");
                    break;
            }


            modeTravel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Log.e("LIST", String.valueOf(o));
                    String selValue =  String.valueOf(o);
                    switch (selValue){
                        case "Safest":
                            DisplayMap.getSharedPreferences().edit().putString(Settings.ROUTE_MODE, Settings.ROUTE_MODE_SAFEST).commit();
                            break;
                        case "Fastest":
                            modeTravel.setValueIndex(1);
                            DisplayMap.getSharedPreferences().edit().putString(Settings.ROUTE_MODE, Settings.ROUTE_MODE_FASTEST).commit();
                            break;
                        case "Shortest":
                            modeTravel.setValueIndex(2);
                            DisplayMap.getSharedPreferences().edit().putString(Settings.ROUTE_MODE, Settings.ROUTE_MODE_SHORTEST).commit();
                            break;
                    }
                    modeTravel.setSummary(selValue);
                    return true;
                }
            });

            final ListPreference transitTravel = (ListPreference) this.findPreference("route_transit_lp");
            String selValuePreTr =  DisplayMap.getSharedPreferences().getString(Settings.ROUTE_TRANSIT, "");
            switch (selValuePreTr){
                case Settings.ROUTE_TRANSIT_DRIVING:
                    transitTravel.setValueIndex(0);
                    transitTravel.setSummary("Driving");
                    break;
                case Settings.ROUTE_TRANSIT_WALKING:
                    transitTravel.setValueIndex(1);
                    transitTravel.setSummary("Walking");
                    break;
                case Settings.ROUTE_TRANSIT_BICYCLING:
                    transitTravel.setValueIndex(2);
                    transitTravel.setSummary("Bicycling");
                    break;
            }


            //Add traffic incidents
            final SwitchPreference curTrafficIncSwitch = (SwitchPreference) this.findPreference("route_cur_incident_sw");
            curTrafficIncSwitch.setChecked(DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_CUR_TRA_INC, false));
            curTrafficIncSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    boolean findBest = (Boolean) o;
                    if (findBest){
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_CUR_TRA_INC, true).commit();
                        RenderLayers.addIncindentsGraphics();
                    } else {
                        DisplayMap.getTrafficIncOverlay().getGraphics().clear();
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_CUR_TRA_INC, false).commit();
                    }
                    curTrafficIncSwitch.setChecked(findBest);
                    return  findBest;
                }
            });


            transitTravel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Log.e("LIST", String.valueOf(o));
                    String selValue =  String.valueOf(o);
                    switch (selValue){
                        case "Driving":
                            transitTravel.setValueIndex(0);
                            curTrafficIncSwitch.setEnabled(true);
                            DisplayMap.getSharedPreferences().edit().putString(Settings.ROUTE_TRANSIT, Settings.ROUTE_TRANSIT_DRIVING).commit();
                            break;
                        case "Walking":
                            transitTravel.setValueIndex(1);
                            curTrafficIncSwitch.setEnabled(false);
                            curTrafficIncSwitch.setChecked(false);
                            DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_CUR_TRA_INC, false).commit();
                            DisplayMap.getSharedPreferences().edit().putString(Settings.ROUTE_TRANSIT, Settings.ROUTE_TRANSIT_WALKING).commit();
                            break;
                        case "Bicycling":
                            transitTravel.setValueIndex(2);
                            curTrafficIncSwitch.setEnabled(false);
                            curTrafficIncSwitch.setChecked(false);
                            DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_CUR_TRA_INC, false).commit();
                            DisplayMap.getSharedPreferences().edit().putString(Settings.ROUTE_TRANSIT, Settings.ROUTE_TRANSIT_BICYCLING).commit();
                            break;
                    }
                    transitTravel.setSummary(selValue);
                    return true;
                }
            });

            //First Stop
            final SwitchPreference firstStopSwitch = (SwitchPreference) this.findPreference("route_stop_first_sw");
            firstStopSwitch.setChecked(DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_STOP_FIRST, true));
            firstStopSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    boolean findBest = (Boolean) o;
                    if (findBest){
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_STOP_FIRST, true).commit();
                    } else {
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_STOP_FIRST, false).commit();
                    }
                    firstStopSwitch.setChecked(findBest);
                    return  findBest;
                }
            });

            final SwitchPreference lastStopSwitch = (SwitchPreference) this.findPreference("route_stop_last_sw");
            lastStopSwitch.setChecked(DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_STOP_LAST, true));
            lastStopSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    boolean findBest = (Boolean) o;
                    if (findBest){
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_STOP_LAST, true).commit();
                    } else {
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_STOP_LAST, false).commit();
                    }
                    lastStopSwitch.setChecked(findBest);
                    return  findBest;
                }
            });

            final SwitchPreference stopSwitch = (SwitchPreference) this.findPreference("route_stop_sw");
            stopSwitch.setChecked(DisplayMap.getSharedPreferences().getBoolean(Settings.ROUTE_STOP_BEST_SEQ, true));
            stopSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    boolean findBest = (Boolean) o;
                    Log.e("findBest", String.valueOf(findBest));
                    if (findBest){
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_STOP_BEST_SEQ, true).commit();
                        lastStopSwitch.setEnabled(true);
                        firstStopSwitch.setEnabled(true);
                    } else {
                        DisplayMap.getSharedPreferences().edit().putBoolean(Settings.ROUTE_STOP_BEST_SEQ, false).commit();
                        lastStopSwitch.setEnabled(false);
                        firstStopSwitch.setEnabled(false);
                    }
                    stopSwitch.setChecked(findBest);
                    return  findBest;
                }
            });
        }
    }

}
