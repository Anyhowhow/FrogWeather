package com.wlhlearning.frogweather;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;

import com.thinkpage.lib.api.TPWeatherManager;
import com.thinkpage.lib.api.TPCity;
import com.thinkpage.lib.api.TPListeners;
import com.thinkpage.lib.api.TPWeatherNow;
import com.thinkpage.lib.api.TPWeatherDaily;

public class MainActivity extends CheckPermissionsActivity {
    private SensorManager mSensorManager;//定义传感器管理器
    private Sensor temperature;          //温度传感器（摄氏度 ℃）
    private Sensor humidity;             //相对湿度传感器（百分比 %）
    private Sensor pressure;             //压力传感器（百帕斯卡 hPa）

    //定义各个事件监听器
    private SensorEventListener temperatureListener;
    private SensorEventListener humidityListener;
    private SensorEventListener pressureListener;

    double  pressurepast=0;//定义全局变量，用来记录上次的气压值，防止测量值反复波动

    //定义exitTime变量，用于实现再按一次退出程序
    private long exitTime = 0;

    //声明高德地图定位所需要的对象
    private AMapLocationClientOption mLocationOption = null;
    private AMapLocationClient mlocationClient=null;

    public String weathercity=null;//定义全局变量，用来记录高德地图的定位城市，传给心知天气获取天气信息
    Date date=new Date();//定义日期变量，用来获取天气预报
    EditText editText1 =null;
    public String str1="请输入城市名";

    //启动时的初始化
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // 实现全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initLocation();   //初始化定位
        delay();
        editText1=(EditText)findViewById(R.id.editText1);
        Button weather_search = (Button) findViewById(R.id.searchweather);
        weather_search.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                str1=editText1.getText().toString();
                initLocation();   //初始化定位
                delay();

            }
        });

    }

    private void delay(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(weathercity!=null)
                    initweather();    //初始化天气
                Toast toast = Toast.makeText(getApplicationContext(), "已为您更新天气", Toast.LENGTH_SHORT);
                toast.show();
            }
        },2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);         //获取Sensor类型对象
        temperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);//注册温度传感器服务
        humidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);      //注册相对湿度传感器服务
        pressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);                //注册压力传感器服务

        //定义监听器
        this.temperatureListener = new TemperatureListener();
        this.humidityListener = new HumidityListener();
        this.pressureListener = new PressureListener();

        mSensorManager.registerListener(temperatureListener, temperature, SensorManager.SENSOR_DELAY_NORMAL);//注册温度监听器
        mSensorManager.registerListener(humidityListener, humidity, SensorManager.SENSOR_DELAY_NORMAL);       //注册相对湿度监听器
        mSensorManager.registerListener(pressureListener, pressure, SensorManager.SENSOR_DELAY_NORMAL);       //注册压力监听器


    }


    //再按一次退出程序的实现
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
            StartActivity.close_it.finish();
            if((System.currentTimeMillis()-exitTime) > 2000){
                Toast toast = Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT);
                toast.show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    //注销监听器
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this.temperatureListener);
        mSensorManager.unregisterListener(this.humidityListener);
        mSensorManager.unregisterListener(this.pressureListener);
    }

    //定义温度监听器的类
    private class TemperatureListener implements SensorEventListener {
        @Override
        public final void onSensorChanged(SensorEvent event) {
            float temperatureValue = event.values[0];
            BigDecimal bd = new BigDecimal(temperatureValue);
            double temperature = bd.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();//对数据进行处理，取两位小数并四舍五入
            TextView tem = (TextView)MainActivity.this.findViewById(R.id.temperature);//在id为temperature的按钮上显示处理后的数据
            tem.setText("体感"+temperature + "℃");                                          //显示温度的数值及单位
        }

        //检测传感器新的值，与传感器发生交互
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    //定义相对湿度监听器的类
    private class HumidityListener implements SensorEventListener {
        @Override
        public final void onSensorChanged(SensorEvent event) {
            float humidityValue = event.values[0];
            double humidity_real;
            BigDecimal bd = new BigDecimal(humidityValue);
            double humidity = bd.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();//对数据进行处理，取两位小数并四舍五入
            humidity_real=100-humidity;
            BigDecimal newvalue = new BigDecimal(humidity_real);
            double showhumidity = newvalue.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
            TextView tem = (TextView)MainActivity.this.findViewById(R.id.humidity);//在id为humidity的按钮上显示处理后的数据
            tem.setText("湿度"+showhumidity + " %");                                           //显示相对湿度的数值及单位
        }

        //检测传感器新的值，与传感器发生交互
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    //定义压力监听器的类
    private class PressureListener implements SensorEventListener {
        @Override
        public final void onSensorChanged(SensorEvent event)
        {
            float pressureValue = event.values[0];
            BigDecimal bd = new BigDecimal(pressureValue);
            double pressure = bd.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();//对数据进行处理，取两位小数并四舍五入
            //判断是否变动超过0.2,以决定是否更新值
            if(Math.abs(pressure-pressurepast)>0.2)
            {
                TextView tem = (TextView) MainActivity.this.findViewById(R.id.pressure);//在id为pressure的按钮上显示处理后的数据
                tem.setText("气压"+pressure + "hPa");                                 //显示压力的数值及单位
                pressurepast=pressure;                                                 //更新上一次的压力值
            }
        }

        //检测传感器新的值，与传感器发生交互
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }



    public void initLocation(){
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置返回地址信息，默认为true
        mLocationOption.setNeedAddress(true);
        //设置定位模式,Hight_Accuracy为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationMode.Battery_Saving);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //声明Client对象
        mlocationClient = new AMapLocationClient(this.getApplicationContext());
        //设置定位监听
        mlocationClient.setLocationListener(locationListener);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
        //启动定位
        mlocationClient.startLocation();
    }

    //初始化天气管理器
    public void initweather()
    {
        //声明一个天气管理器对象
        TPWeatherManager weatherManager = TPWeatherManager.sharedWeatherManager();
        //设置用户密钥，帮助服务器验证用户身份是否合法
        weatherManager.initWithKeyAndUserId("cxda1roqhwdsp1jj","U4B70BB7CC");
        //声明获取实时天气的方法
        weatherManager.getWeatherNow(new TPCity(weathercity),TPWeatherManager.TPWeatherReportLanguage.kSimplifiedChinese,
                TPWeatherManager.TPTemperatureUnit.kCelsius,weatherNowListener);
        //声明获取天气预报的方法
        weatherManager.getWeatherDailyArray(new TPCity(weathercity),TPWeatherManager.TPWeatherReportLanguage.kSimplifiedChinese,
                TPWeatherManager.TPTemperatureUnit.kCelsius,date,3,dailyListener);

    }
    //获取定位
    AMapLocationListener locationListener = new AMapLocationListener(){
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            String location,address;
            if (amapLocation != null) {
                {
                    //定位成功回调信息，设置相关消息
                    amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                    amapLocation.getLatitude();//获取纬度
                    amapLocation.getLongitude();//获取经度
                    amapLocation.getAccuracy();//获取精度信息
                    amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                    amapLocation.getCountry();//国家信息
                    amapLocation.getProvince();//省信息
                    amapLocation.getCity();//城市信息
                    amapLocation.getDistrict();//城区信息
                    amapLocation.getStreet();//街道信息
                    amapLocation.getStreetNum();//街道门牌号信息
                    amapLocation.getCityCode();//城市编码
                    amapLocation.getAdCode();//地区编码

                    //显示经纬度,并进行数据处理
                    BigDecimal longitude = new BigDecimal(amapLocation.getLongitude());
                    double longitude_after_effect = longitude.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    BigDecimal latitude = new BigDecimal(amapLocation.getLatitude());
                    double latitude_after_effect = latitude.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    //显示经纬度
                    TextView lonlat = (TextView)MainActivity.this.findViewById(R.id.location);
                    location="北纬:"+latitude_after_effect+"°"+"\n东经:"+longitude_after_effect+"°";
                    lonlat.setText(location);

                    //显示地址
                    address=amapLocation.getProvince()+amapLocation.getCity()+amapLocation.getDistrict()+
                            "\n"+amapLocation.getStreet()+amapLocation.getStreetNum();
                    TextView where_you_are = (TextView)MainActivity.this.findViewById(R.id.address);
                    where_you_are.setText(address);
                    if(str1.equals("请输入城市名")||str1.isEmpty())
                        weathercity=amapLocation.getCity();
                    else
                        weathercity=str1;
                    TextView city=(TextView)MainActivity.this.findViewById(R.id.city);
                    city.setText(weathercity);

                }

            }
        }
    };

    //获取实时天气
    TPListeners.TPWeatherNowListener weatherNowListener=new  TPListeners.TPWeatherNowListener()
    { @Override
    public void onTPWeatherNowAvailable(TPWeatherNow weatherNow, String errorInfo) {
        String theweather,thetemperature,theupdatetime,date_string;
        ImageButton imageButton1=(ImageButton)findViewById(R.id.imageButton1);
        if (weatherNow != null) {
            String tq = weatherNow.text;
            Date update_date = weatherNow.lastUpdateDate;
            SimpleDateFormat dateFormat =new SimpleDateFormat("HH:mm");
            int wd = weatherNow.temperature;
            View main_activity=findViewById(R.id.activity_main);
            //显示实时天气和气温
            TextView weathernow = (TextView) MainActivity.this.findViewById(R.id.weather);
            theweather = tq;
            weathernow.setText(theweather);
            TextView temperaturenow = (TextView) MainActivity.this.findViewById(R.id.the_temperature);
            thetemperature = wd+"℃";
            temperaturenow.setText(thetemperature);
            if ("晴".equals(tq)){
                imageButton1.setBackgroundResource(R.drawable.sunny);
                main_activity.setBackgroundResource(R.drawable.background_sunny);}
            else if ("阴".equals(tq)){
                imageButton1.setBackgroundResource(R.drawable.overcast);
                main_activity.setBackgroundResource(R.drawable.background_overcast);}
            else if ("多云".equals(tq)){
                imageButton1.setBackgroundResource(R.drawable.cloudy);
                main_activity.setBackgroundResource(R.drawable.background_cloudy);}
            else if ("雨".equals(tq)||"阵雨".equals(tq)||"小雨".equals(tq)||"中雨".equals(tq)||"大雨".equals(tq)||"暴雨".equals(tq)){
                imageButton1.setBackgroundResource(R.drawable.rainy);
                main_activity.setBackgroundResource(R.drawable.background_rainy);}
            else if (("雪".equals(tq)||"小雪".equals(tq)||"中雪".equals(tq)||"大雪".equals(tq)||"暴雪".equals(tq))){
                imageButton1.setBackgroundResource(R.drawable.snowy);
                main_activity.setBackgroundResource(R.drawable.background_snowy);}
            //显示更新时间
            date_string = dateFormat.format(update_date);
            TextView update_time = (TextView) MainActivity.this.findViewById(R.id.update_time);
            theupdatetime=date_string+"更新";
            update_time.setText(theupdatetime);
        }
    }
    };
    //获取天气预报
    TPListeners.TPWeatherDailyListener dailyListener=new TPListeners.TPWeatherDailyListener()
    {
        @Override
        public void onTPWeatherDailyAvailable(TPWeatherDaily[] tpWeatherDailies, String s) {
            String day_weather,day_2_weather,day_3_weather,day_temperature,day_2_temperature,day_3_temperature,
                    dayweather1_4bit,dayweather2_4bit,dayweather3_4bit, ht1_space,lt1_space,ht2_space,lt2_space
                    ,ht3_space,lt3_space,space0="",space1=" ";
            ImageButton imageButton2=(ImageButton)findViewById(R.id.imageButton2);
            ImageButton imageButton3=(ImageButton)findViewById(R.id.imageButton3);
            ImageButton imageButton4=(ImageButton)findViewById(R.id.imageButton4);
            //提取预报数组的三个元素
            TPWeatherDaily day1=tpWeatherDailies[0];
            TPWeatherDaily day2=tpWeatherDailies[1];
            TPWeatherDaily day3=tpWeatherDailies[2];

            //处理第一天预报的数据
            String dayweather1=day1.textDay;
            if (dayweather1.length()<2)
                dayweather1_4bit=String.format("%-4s",dayweather1);
            else dayweather1_4bit=dayweather1;
            int ht1=day1.highTemperature;
            int lt1=day1.lowTemperature;
            //处理第二天预报的数据
            String dayweather2=day2.textDay;
            if (dayweather2.length()<2)
                dayweather2_4bit=String.format("%-4s",dayweather2);
            else dayweather2_4bit=dayweather2;
            int ht2=day2.highTemperature;
            int lt2=day2.lowTemperature;
            //处理第三天预报的数据
            String dayweather3=day3.textDay;
            if (dayweather3.length()<2)
                dayweather3_4bit=String.format("%-4s",dayweather3);
            else dayweather3_4bit=dayweather3;
            int ht3=day3.highTemperature;
            int lt3=day3.lowTemperature;

            //判断是否温度的位数，从而对齐
            if (ht1<10)
                ht1_space=space1;
            else ht1_space=space0;
            if (lt1<10)
                lt1_space=space1;
            else lt1_space=space0;
            if (ht2<10)
                ht2_space=space1;
            else ht2_space=space0;
            if (lt2<10)
                lt2_space=space1;
            else lt2_space=space0;
            if (ht3<10)
                ht3_space=space1;
            else ht3_space=space0;
            if (lt3<10)
                lt3_space=space1;
            else lt3_space=space0;

            //显示第一天的天气预报
            TextView day_1= (TextView) MainActivity.this.findViewById(R.id.day_1);
            day_weather=dayweather1_4bit;                      ;
            day_1.setText(day_weather);
            if ("晴".equals(dayweather1))
                imageButton2.setBackgroundResource(R.drawable.sunny_s);
            else if ("阴".equals(dayweather1))
                imageButton2.setBackgroundResource(R.drawable.overcast_s);
            else if ("多云".equals(dayweather1))
                imageButton2.setBackgroundResource(R.drawable.cloudy_s);
            else if ("雨".equals(dayweather1)||"阵雨".equals(dayweather1)||"小雨".equals(dayweather1)||"中雨".equals(dayweather1)||"大雨".equals(dayweather1)||"暴雨".equals(dayweather1))
                imageButton2.setBackgroundResource(R.drawable.rainy_s);
            else if ("雪".equals(dayweather1)||"小雪".equals(dayweather1)||"中雪".equals(dayweather1)||"大雪".equals(dayweather1)||"暴雪".equals(dayweather1))
                imageButton2.setBackgroundResource(R.drawable.snowy_s);
            TextView day_1_t= (TextView) MainActivity.this.findViewById(R.id.day_1_temperature);
            day_temperature=lt1_space+lt1+"~"+ht1_space+ht1+"℃";
            day_1_t.setText(day_temperature);

            //显示第二天的天气预报
            TextView day_2= (TextView) MainActivity.this.findViewById(R.id.day_2);
            day_2_weather=dayweather2_4bit;
            day_2.setText(day_2_weather);
            if ("晴".equals(dayweather2))
                imageButton3.setBackgroundResource(R.drawable.sunny_s);
            else if ("阴".equals(dayweather2))
                imageButton3.setBackgroundResource(R.drawable.overcast_s);
            else if ("多云".equals(dayweather2))
                imageButton3.setBackgroundResource(R.drawable.cloudy_s);
            else if ("雨".equals(dayweather2)||"阵雨".equals(dayweather2)||"小雨".equals(dayweather2)||"中雨".equals(dayweather2)||"大雨".equals(dayweather2)||"暴雨".equals(dayweather2))
                imageButton3.setBackgroundResource(R.drawable.rainy_s);
            else if ("雪".equals(dayweather2)||"小雪".equals(dayweather2)||"中雪".equals(dayweather2)||"大雪".equals(dayweather2)||"暴雪".equals(dayweather2))
                imageButton3.setBackgroundResource(R.drawable.snowy_s);
            TextView day_2_t= (TextView) MainActivity.this.findViewById(R.id.day_2_temperature);
            day_2_temperature=lt2_space+lt2+"~"+ht2_space+ht2+"℃";
            day_2_t.setText(day_2_temperature);

            //显示第三天的天气预报
            TextView day_3= (TextView) MainActivity.this.findViewById(R.id.day_3);
            day_3_weather=dayweather3_4bit;
            day_3.setText(day_3_weather);
            if ("晴".equals(dayweather3))
                imageButton4.setBackgroundResource(R.drawable.sunny_s);
            else if ("阴".equals(dayweather3))
                imageButton4.setBackgroundResource(R.drawable.overcast_s);
            else if ("多云".equals(dayweather3))
                imageButton4.setBackgroundResource(R.drawable.cloudy_s);
            else if ("雨".equals(dayweather3)||"阵雨".equals(dayweather3)||"小雨".equals(dayweather3)||"中雨".equals(dayweather3)||"大雨".equals(dayweather3)||"暴雨".equals(dayweather3))
                imageButton4.setBackgroundResource(R.drawable.rainy_s);
            else if ("雪".equals(dayweather3)||"小雪".equals(dayweather3)||"中雪".equals(dayweather3)||"大雪".equals(dayweather3)||"暴雪".equals(dayweather3))
                imageButton4.setBackgroundResource(R.drawable.snowy_s);
            TextView day_3_t= (TextView) MainActivity.this.findViewById(R.id.day_3_temperature);
            day_3_temperature=lt3_space+lt3+"~"+ht3_space+ht3+"℃";
            day_3_t.setText(day_3_temperature);
        }
    };
}
