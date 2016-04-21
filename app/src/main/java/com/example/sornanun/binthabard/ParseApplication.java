package com.example.sornanun.binthabard;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseUser;
import android.app.Application;

public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "xwh8EUo1w8ZmezPsxYfOwlU7z5yghX9aJj0yGyw2", "HMIS2zYNT6F12TCqe0FmMJKgTgnntVGDwXr5cOHs");
        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
    }

}
