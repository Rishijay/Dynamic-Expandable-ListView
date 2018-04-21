package com.rjay.dynamicexpandablelistview;

/**
 * Here, we are going to create expandable dynamic list view using SQLite.
 * We will get the data from JSON (using Volley).
 * Consider, we have a menu card and we will divide the data category wise.
 */


import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public List<String> category_name = new ArrayList<>();
    public List<String> name_of_dish = new ArrayList<>();
    public List<String> item_code = new ArrayList<>();
    public List<String> rate_of_half = new ArrayList<>();
    public List<String> rate_of_full = new ArrayList<>();
    public List<String> item_status = new ArrayList<>();
    public List<String> category_id = new ArrayList<>();
    public List<String> full = new ArrayList<>();
    public List<String> half = new ArrayList<>();
    //String url = "http://192.168.1.109/pages/food_available_menu_json.php"; // Replace with your own url
    String url = "http://miscos.in/dqm/dqm_json/food_available_menu_json.php";
    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    List<String> listitems;
    HashMap<String, List<String>> listDataChild;
    SwipeRefreshLayout mSwipeRefreshLayout;
    public SQLiteDatabase database;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        //Creating database to store menu items
        database = openOrCreateDatabase("Menu.db", MODE_PRIVATE, null);
        final String q = "Create Table if not exists List (dishName varchar(50), categoryName varchar(50), categoryID varchar(20), itemCode varchar(20), rateOfHalf varchar(10), rateOfFull varchar(10), itemStatus varchar(20), half varchar(20), full varchar(20))";
        database.execSQL(q);


        // Get data from JSON
        getMenu();

        // Swipe down to refresh list 
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeToRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getMenu();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        expListView = (ExpandableListView) findViewById(R.id.lvExp);


        final int[] prevExpandPosition = {-1};
        //Lisview on group expand listner... to close other expanded headers...
        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int i) {
                if (prevExpandPosition[0] >= 0) {
                    expListView.collapseGroup(prevExpandPosition[0]);
                }
                prevExpandPosition[0] = i;
            }
        });


        // Listview on child click listener
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                String itemName = listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
                Toast.makeText(MainActivity.this, "You selected : " + itemName, Toast.LENGTH_SHORT).show();
                Log.e("Child", listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition));
                return false;
            }
        });
    }

    /**
     * Get menu data from JSON
     */
    private void getMenu() {
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                progressDialog.dismiss();
                try {
                    JSONObject obj = new JSONObject(s);
                    Log.e("Json response", " " + obj.toString());
                    int error = obj.getInt("error_code");
                    Log.e("error code", " " + error);
                    if (error == 100) {
                        JSONArray arr = obj.getJSONArray("category_name");
                        for (int i = 0; i < arr.length(); i++) {
                            //retrieving each divisional value
                            String categoryName = arr.getJSONObject(i).getString("category_name");
                            String name = arr.getJSONObject(i).getString("name_of_dish");
                            String itemCode = arr.getJSONObject(i).getString("item_code");
                            String rateOfHalf = arr.getJSONObject(i).getString("rate_of_half");
                            String rateOfFull = arr.getJSONObject(i).getString("rate_of_full");
                            String status = arr.getJSONObject(i).getString("item_status");
                            String categoryID = arr.getJSONObject(i).getString("category_id");
                            String fullStatus = arr.getJSONObject(i).getString("full");
                            String halfStatus = arr.getJSONObject(i).getString("half");

                            //adding values division-wise
                            category_name.add(categoryName);
                            name_of_dish.add(name);
                            item_code.add(itemCode);
                            rate_of_half.add(rateOfHalf);
                            rate_of_full.add(rateOfFull);
                            item_status.add(status);
                            category_id.add(categoryID);
                            full.add(fullStatus);
                            half.add(halfStatus);

                            Log.e("List Showing ", name + " " + status);
                        }
                        addrows();
                        // preparing list data
                        try {
                            makeHeaderChildData();
                        } catch (Exception e) {
                            Log.e("makeHeaderChildData", "Exception " + e.toString());
                        }

                        listAdapter = new ExpandableListAdapter(MainActivity.this, listDataHeader, listDataChild);
                        // setting list adapter
                        expListView.setAdapter(listAdapter);
                    } else {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    Log.e("Check", "JSONEXCEPTION " + e.toString());
                    e.printStackTrace();
                }
                Log.e("response", s);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("error response", "Some error occurred!!" + volleyError);
                progressDialog.dismiss();
                final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Error");
                alert.setMessage("Connection error.! Unable to connect to server.");
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        return;
                    }
                });
                alert.show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("rest_id", "001");
                parameters.put("category_name", "");
                Log.d("Params", parameters.toString());
                return parameters;
            }
        };

        RequestQueue rQueue = Volley.newRequestQueue(MainActivity.this);
        rQueue.add(request);
    }

    /**
     * Adding rows to the table
     */
    private void addrows() {
        //adding values in row manner
        for (int i = 0; i < name_of_dish.size(); i++) {
            Log.e("Checking rows", "");
            ContentValues cv = new ContentValues();
            cv.put("dishName", name_of_dish.get(i));
            cv.put("categoryName", category_name.get(i));
            cv.put("categoryID", category_id.get(i));
            cv.put("itemCode", item_code.get(i));
            cv.put("rateOfHalf", rate_of_half.get(i));
            cv.put("rateOfFull", rate_of_full.get(i));
            cv.put("itemStatus", item_status.get(i));
            cv.put("half", half.get(i));
            cv.put("full", full.get(i));
            database.insert("List", null, cv);
        }
    }


    /**
     * Differentiate between header and child and add respectively
     */
    private void makeHeaderChildData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        String itemCategoryID, itemName, categoryName;
        String query = "SELECT DISTINCT categoryName FROM List";
        Cursor c = database.rawQuery(query, null);
        if (c != null) {
            c.moveToFirst();
            do {
                itemCategoryID = c.getString(c.getColumnIndex("categoryName"));
                Log.e("Check id", itemCategoryID);
                listDataHeader.add(itemCategoryID);

                String q = "SELECT DISTINCT dishName FROM List\n" +
                        "WHERE categoryName=" + "'" + itemCategoryID + "'";

                Cursor cursor = database.rawQuery(q, null);
                listitems = new ArrayList<String>();
                if (cursor != null) {
                    cursor.moveToFirst();
                    do {
                        itemName = cursor.getString(cursor.getColumnIndex("dishName"));
                        listitems.add(itemName);
                        Log.e("Check name", itemName);
                        for (int i = 0; i <= listitems.size(); i++) {
                            listDataChild.put(itemCategoryID, listitems); // Header, Child data
                        }
                    } while (cursor.moveToNext());
                }
                if (cursor.getCount() == 0) {
                    Log.e("Check", "cursor.getcount=0");
                }
            } while (c.moveToNext());
        }
        if (c.getCount() == 0) {
            Log.e("Check", "c.getcount=0");
        }
    }
}
