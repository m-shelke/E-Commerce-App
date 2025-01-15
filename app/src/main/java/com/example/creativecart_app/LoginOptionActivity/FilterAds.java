package com.example.creativecart_app.LoginOptionActivity;

import android.widget.Filter;

import com.example.creativecart_app.adapters.AdapterAds;
import com.example.creativecart_app.models.ModelAds;

import java.util.ArrayList;

public class FilterAds extends Filter {

    //Declaring AdapterAds and ArrayList<ModelAds> instance that will be initialized in constructor of this class
    private AdapterAds adapterAds;
    private ArrayList<ModelAds> filterList;

    /* Filter Ads Constructor
    *param adapterAds, AdapterAds instance to pass this when this constructor is created
    * param filterList ads ArrayList to be passed, when this constructor is created
     */
    public FilterAds(AdapterAds adapterAds, ArrayList<ModelAds> filterList) {
        this.adapterAds = adapterAds;
        this.filterList =filterList;
    }


    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        //perform filter base on what user type

        FilterResults results=new FilterResults();

        if (constraint != null && constraint.length() > 0){
            //The search query is not null and not empty, we can perform filter
            //convert the type query to Upper Case to make search not case sensitive e.g Samsung S23 Ultra --> SAMSUNG S23 ULTRA
            constraint=constraint.toString().toUpperCase();

            //Hold the filtered list of Ads bases on User Search Query
            ArrayList<ModelAds> filteredModels =new ArrayList<>();
            for (int i=0; i<filterList.size(); i++){

                //Ads filter based on Brand,CategoryFragment,Condition,Title. If any of this matches add it to the filteredModels list
                if (filterList.get(i).getBrand().toUpperCase().contains(constraint) ||
                    filterList.get(i).getCategory().toUpperCase().contains(constraint) ||
                     filterList.get(i).getCondition().toUpperCase().contains(constraint) ||
                      filterList.get(i).getTitle().toUpperCase().contains(constraint))
                {
                    //Filter matched add to filteredModels list
                    filteredModels.add(filterList.get(i));
                }
            }

            results.count=filteredModels.size();
            results.values=filteredModels;

        }else {
            //the search query is either or empty, we can't perform filter. Return null/original list
            results.count=filterList.size();
            results.values=filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
        //published the filter result

          adapterAds.adsArrayList=(ArrayList<ModelAds>) filterResults.values;


        adapterAds.notifyDataSetChanged();
    }
}
