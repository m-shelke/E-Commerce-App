package com.example.creativecart_app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.creativecart_app.LoginOptionActivity.RvListenerCategory;
import com.example.creativecart_app.databinding.RowCategoryBinding;
import com.example.creativecart_app.models.ModelCategory;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Random;

public class AdapterCategory extends RecyclerView.Adapter<AdapterCategory.HolderCategory> {

    //View Binding
    private RowCategoryBinding binding;

    //Context of activity/fragment from where instance of AdapterCategory class is created
    private Context context;

    //CategoryFragment ArrayList. The list of the categories
    private ArrayList<ModelCategory> categoryArrayList;

    //RvListenerCategory instances to handle the CategoryFragment click event in it's calling class instead of this class
    private RvListenerCategory rvListenerCategory;

    /*Constructor @param: Context the context of the activity/fragment from where instances of AdapterCategory class is created
    * @param:categoryArrayList.CategoryFragment ArrayList. The list of the categories
    * @param: RvListenerCategory: Instance of the RvListenerCategory interface
     */
    public AdapterCategory(Context context, ArrayList<ModelCategory> categoryArrayList, RvListenerCategory rvListenerCategory) {
        this.context = context;
        this.categoryArrayList = categoryArrayList;
        this.rvListenerCategory = rvListenerCategory;
    }

    @NonNull
    @Override
    public HolderCategory onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/bind the row_category.xml
        binding=RowCategoryBinding.inflate(LayoutInflater.from(context),parent,false);
        return new HolderCategory(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderCategory holder, int position) {

        //get the data from particulat position of the list and set to the UI View of the row_category.xml and Handle click
             ModelCategory modelCategory=categoryArrayList.get(position);

             //get data from madeCategory
        String category=modelCategory.getCategory();
        int icon=modelCategory.getIcon();

        //get random color to set as background color of the categoryIconIv
        Random random=new Random();
        int color= Color.argb(random.nextInt(255),random.nextInt(255),random.nextInt(255),random.nextInt(255));

        //set data to UI view of row_category.xml
        holder.categoryIconIv.setImageResource(icon);
        holder.categoryTitleTv.setText(category);
        holder.categoryIconIv.setBackgroundColor(color);

        //Handle item click, call interface (RvListenerCategory) method to perform click in calling activity/fragment class instead of this class
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rvListenerCategory.onCategoryClick(modelCategory);
            }
        });



    }

    @Override
    public int getItemCount() {
        //return the size of the list
        return categoryArrayList.size();
    }

    class HolderCategory extends RecyclerView.ViewHolder{

        //UI view of the row_category.xml
        ShapeableImageView categoryIconIv;
        TextView categoryTitleTv;

        public HolderCategory(@NonNull View itemView) {
            super(itemView);

            //init UI view of the row_category.xml
            categoryIconIv=binding.categoryIconIv;
            categoryTitleTv=binding.categoryTitleTv;
        }
    }
}
