package com.example.sornanun.binthabard;

import android.util.Log;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

/**
 * Created by mital on 20/04/59.
 */
public class ParseController {

    static String objectId;
    ParseObject monk_location;

    public void saveLocation(String _lat, String _long, String _address) {

        monk_location = new ParseObject("Monk_Location");
        monk_location.put("lat", _lat);
        monk_location.put("long", _long);
        if(_address == null) _address = "ไม่พบสถานที่";
        monk_location.put("address", _address);

        monk_location.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    //saved successfully
                    objectId = monk_location.getObjectId();
                    Log.d("ParseController","Save Location Finished ObjectId "+objectId);
                }
                else{
                    Log.e("ParseController","Error : "+e.toString());
                    Log.d("ParseController","Cannot get objectId");
                }
            }
        });
    }

    public void updateLocation(final String _lat, final String _long, final String _address) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Monk_Location");
        // Retrieve the object by id
        query.getInBackground(objectId, new GetCallback<ParseObject>() {
            public void done(ParseObject _monk_location, ParseException e) {
                if (e == null) {
                    _monk_location.put("lat", _lat);
                    _monk_location.put("long", _long);
                    _monk_location.put("address", _address);
                    _monk_location.saveInBackground();

                    Log.d("ParseController","Update Location Finished");
                }
            }
        });
    }

    public void removeLocation() {
        if(monk_location != null) {
            monk_location.deleteInBackground();
            objectId = "";
            Log.d("ParseController", "Remove Location Finished");
        }
        else{
            Log.e("ParseController","monk_location is null");
        }
    }
}
