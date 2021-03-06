package com.sondsara.v_scan;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashMap;

//This is our class for animal products. It implements parcelable so we can send our instances of it to Analyze.
class animalIngredient implements Parcelable {
    String name = "name";
    String derivation = "derivation";
    String status = "status";

    public animalIngredient (Parcel source){
        name = source.readString();
        derivation = source.readString();
        status = source.readString();
    }

    public animalIngredient (String name, String derivation, String status){
        this.name = name;
        this.derivation = derivation;
        this.status = status;
    }

    public int describeContents(){
        return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int flags){
        dest.writeString(name);
        dest.writeString(derivation);
        dest.writeString(status);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){
        public animalIngredient createFromParcel (Parcel in){
            return new animalIngredient(in);
        }
        public animalIngredient[] newArray(int size){
            return new animalIngredient[size];
        }
    };
}

//Same thing as for animalIngredient.
class veganProduct implements Parcelable {
    String name = "name";
    String company = "company";

    public veganProduct (Parcel source){
        name = source.readString();
        company = source.readString();
    }

    public veganProduct (String name, String company){
        this.name = name;
        this.company = company;
    }

    public int describeContents(){
        return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int flags){
        dest.writeString(name);
        dest.writeString(company);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){
        public veganProduct createFromParcel (Parcel in){
            return new veganProduct(in);
        }
        public veganProduct[] newArray(int size){
            return new veganProduct[size];
        }
    };
}


//This activity class is responsible for setting everything up: It parses the databases, stores them into array lists, and then launches the camera.
public class MainActivity extends ActionBarActivity {

    public String upc = "";

    private static final int ZBAR_SCANNER_REQUEST = 0;
    private Cursor c;
    private Cursor vegC;
    private Database db;
    public static HashMap<String, animalIngredient> animalProducts = new HashMap<String, animalIngredient>();
    public static HashMap<String, animalIngredient> spacedAnimalProducts = new HashMap<String, animalIngredient>();
    public static HashMap<String, veganProduct> veganProducts = new HashMap<String, veganProduct>();
    //public ArrayList<animalIngredient> animalProducts = new ArrayList<>();
    //public ArrayList<veganProduct> veganProducts = new ArrayList<>();
    public final static String EXTRA_MESSAGE = "com.Veganizer.main.MESSAGE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);
        db= new Database(this);

        try {
            c = db.getIngredients();

            //Add each animal product from the database into the array list one at a time.
            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndexOrThrow("NAME")).toLowerCase();
                String derivation = c.getString(c.getColumnIndexOrThrow("DERIVATION"));
                String status = c.getString(c.getColumnIndexOrThrow("STATUS"));
                animalIngredient temp = new animalIngredient(name, derivation, status);

                if (name.contains(" "))
                    spacedAnimalProducts.put(temp.name, temp);
                else
                    animalProducts.put(temp.name, temp);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        try {
            vegC = db.getVeggies();

            //Adding the vegan products now.
            while (vegC.moveToNext()) {
                String name = vegC.getString(vegC.getColumnIndexOrThrow("NAME"));
                String company = vegC.getString(vegC.getColumnIndexOrThrow("COMPANY"));
                veganProduct temp = new veganProduct(name, company);
                veganProducts.put(temp.company, temp);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        //Now launch the camera, which will kick off the scanning activity.
        //LaunchCamera();

        //Start up our Analyze class, which checks the product ingredients against our list of animal products and displays the result.
        Intent intent = new Intent(getApplicationContext(), Analyze.class);
        //Give that class access to the upc, as well as our two custom databases.
        //intent.putExtra(EXTRA_MESSAGE, upc);
        //Go to Analyze.
        this.startActivity(intent);

        //Intent intent = new Intent(MainActivity.this, Lookup.class);
        //startActivity(intent);
    }

    void LaunchCamera(){
        //Check to see if we have a working camera.
        if (isCameraAvailable()){
            Intent intent = new Intent (this, ZBarScannerActivity.class);
            //When this finishes, onActivityResult (below) will get called.
            startActivityForResult (intent, ZBAR_SCANNER_REQUEST);
        }else{
            Toast.makeText(this, "Sorry, it looks like your camera isn't working.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isCameraAvailable(){
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK){
            //Get the upc code from the scanner. Sometimes it has 13 digits, but our database uses 12 digits only.
            //Removing the first one will usually find the right result.
            upc = data.getStringExtra(ZBarConstants.SCAN_RESULT);
            if (upc.length() == 13) {
                upc = upc.substring(1, 13);
            }

            //Start up our Analyze class, which checks the product ingredients against our list of animal products and displays the result.
            Intent intent = new Intent(getApplicationContext(), Analyze.class);
            //Give that class access to the upc, as well as our two custom databases.
            intent.putExtra(EXTRA_MESSAGE, upc);
            //Go to Analyze.
            this.startActivity(intent);

        }else if (resultCode == RESULT_CANCELED){
            Toast.makeText(this, "Camera Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy (){
        super.onDestroy();
        c.close();
        db.close();
    }

}