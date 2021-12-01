package com.example.weatherapplication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weatherapplication.db.City;
import com.example.weatherapplication.db.Country;
import com.example.weatherapplication.db.Province;
import com.example.weatherapplication.util.Constant;
import com.example.weatherapplication.util.HttpUtil;
import com.example.weatherapplication.util.Utility;
import com.google.android.material.tabs.TabLayout;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class ChooseAreaFragment extends Fragment {
    private static final String TAG ="ChooseAreaFragment";
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList; //省列表
    private List<City> cityList; //市列表
    private List<Country> countyList;//县列表

    private Province selectedProvince;//选中的省份
    private City selectedCity;//选中的城市
    private int currentLevel; //当前选中的级别

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
              if(currentLevel==LEVEL_PROVINCE) {
                  selectedProvince=provinceList.get(position);
                  queryCities();
              }
            }
        });
        queryProvinces();
    }
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
//            currentLevel = LEVEL_PROVINCE;
        } else {
            queryFromServer(Constant.BASE_URL, "province");
        }
    }

    public void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=LitePal.where("province=?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address=Constant.BASE_URL+"/"+provinceCode;
            queryFromServer(address,"city");
        }
    }

    public void queryCountry(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=LitePal.where("cityid=?",
                String.valueOf(selectedCity.getId())).find(Country.class);
        if(countyList.size()>0){
            dataList.clear();
            for(Country country:countyList){
                dataList.add(country.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address=Constant.BASE_URL+"/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"country");
        }
    }
    private void queryFromServer(String url, final String type) {
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "exception=" + e.toString());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "err", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body().string();
                Log.d(TAG, "responseText=" + responseText);
                boolean result = false;
                if ("province".equals(type)) {
                    result=Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                } /*else if ("county".equals(type)) {
                    result=Utility.handleCountryResponse(responseText,selectedCity.getId());
                }*/

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            }
                        }
                    });
                }
            }
        });
}}