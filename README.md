<img src="openhab-conf/icons/landroid.png">

# WorxLandroid Binding
This binding connects openHAB with your WorxLandroid Mower using the API and MQTT.

# Table of contents

 1. [Disclaimer](https://github.com/nibi79/worxlandroid/tree/master#disclaimer)
 2. [Installation and upgrade](https://github.com/nibi79/worxlandroid/tree/master#installation-and-upgrade)
 3. [Supported Things](https://github.com/nibi79/worxlandroid/tree/master#supported-things)
 4. [Discovery](https://github.com/nibi79/worxlandroid/tree/master#discovery)
 5. [Binding Configuration](https://github.com/nibi79/worxlandroid/tree/master#binding-configuration)
 6. [Properties](https://github.com/nibi79/worxlandroid/tree/master#properties)
 7. [Channels](https://github.com/nibi79/worxlandroid/tree/master#channels)
 8. [File based configuration](https://github.com/nibi79/worxlandroid/tree/master#file-based-configuration)
 9. [Iconset](https://github.com/nibi79/worxlandroid/tree/master#iconset)
10. [Support](https://github.com/nibi79/worxlandroid/tree/master#support)

***

## Disclaimer

This binding is currently under development. Your help and testing would be greatly appreciated but there is no stability or functionality warranty.

## Installation and upgrade

For the first installation of the binding you need to copy the [latest release](https://github.com/nibi79/worxlandroid/releases)  into the /addons folder of your openHAB installation. In case you want to upgrade the binding to a newer version, please check the release notes first.

## Supported Things

Currently following Things are supported:

- **WorxLandroid Bridge** Thing representing the handler for Worx API
- One or many Things for supported **WorxLandroid Mower**

## Discovery

Bridge Configuration can be achieved via PaperUI - Configuration - Things - Add Thing “+” Button - WorxLandroid Binding - Add manually - **Bridge Worx Landroid API**. Here you can provide your credentials for your WorxLandroid account. Once the bridge has been added **Worx Landroid Mower**s will be discovered automatically and appear in your PaperUI inbox. You just need to add them as new things.

For textual configuration go to [File based configuration](https://github.com/nibi79/worxlandroid/tree/master#file-based-configuration)

## Binding Configuration

Following options can be set for the **Bridge Worx Landroid API**:

| Property  | Description |
|-----------|-----------|
| webapiUsername | Username to access the WorxLandroid API. |
| webapiPassword | Password to access the WorxLandroid API. |


Following options can be set for the **WorxLandroid Mower**:

| Property  | Description |
|-----------|-----------|
| refreshStatusInterval | Interval for refreshing mower status (ONLINE/OFFLINE) and channel 'common#online' in seconds (min="30")|
| pollingInterval | Interval for polling in seconds (min="30" max="599", after 10 minutes / 600 seconds of inactivity, the connection is closed). |

## Properties

The binding retrieves properties from the API. To view the properties open the thing in PaperUI and click on 'SHOW PROPERTIES':

Bridge:
<br>
<img src="images/SC_PaperUI_Bridge.png" width="40%">

Mower:
<br>
<img src="images/SC_PaperUI_Mower.png" width="40%">

## Channels

Currently following **Channels** are supported on the **Worx Landroid Mower**:

##### common

| Channel   | Type | ChannelName | Values |
|------------|-----------|-----------|-----------|
| online      | `Switch` | common#online | |
| lastUpdateOnlineStatus | `DateTime` | common#lastUpdateOnlineStatus | |
| enable | `Switch` | common#enable | |
| poll | `Switch` | common#poll | |
| action | `String` | common#action | START, STOP, HOME |
| lock | `Switch` | common#lock | |

##### cfgCommon

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| id      | `Switch` | cfgCommon#id |
| serialNumber | `String` | cfgCommon#serialNumber |
| language | `String` | cfgCommon#language |
| lastUpdate | `DateTime` | cfgCommon#lastUpdate |
| command | `Number` | cfgCommon#command |
| rainDelay | `Number` | cfgCommon#rainDelay |

##### datCommon

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| macAdress | `String` | datCommon#macAdress |
| firmware | `Number` | datCommon#firmware |
| wifiQuality | `Number` | datCommon#wifiQuality |
| statusCode | `Number` | datCommon#statusCode |
| statusDescription | `String` | datCommon#statusDescription |
| errorCode | `Number` | datCommon#errorCode |
| errorDescription | `String` | datCommon#errorDescription |
| lastZone | `Number` | datCommon#lastZone |

##### datBattery

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| batteryTemperature | `Number` | datBattery#batteryTemperature | |
| batteryVoltage | `Number` | datBattery#batteryVoltage | |
| batteryLevel | `Number` | datBattery#batteryLevel | |
| batteryChargeCycle | `Number` | datBattery#batteryChargeCycle | |
| batteryChargeCycleCurent | `Number` | datBattery#batteryChargeCycleCurrent | cycles since last reset |
| batteryCharging | `Switch` | datBattery#batteryCharging | |

##### datDmp

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| pitch | `Number` | datDmp#pitch |
| roll | `Number` | datDmp#roll |
| yaw | `Number` | datDmp#yaw |

##### datSt

| Channel   | Type | ChannelName ||
|------------|-----------|-----------|-----------------|
| totalBladeTime | `Number` | datSt#totalBladeTime | |
| currentBladeTime | `Number` | datSt#currentBladeTime | time since last reset |
| totalDistance | `Number` | datSt#totalDistance | |
| totalTime | `Number` | datSt#totalTime | |

##### datRain

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------------|
| state | `Switch` | datRain#state | ONLY IF SUPPORTED - property 'rain_delay_start' |
| counter | `Number` | datRain#counter | ONLY IF SUPPORTED - property 'rain_delay_start'|

##### cfgSc

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| scheduleTimeExtension | `Number` | cfgSc#scheduleTimeExtension | |
| scheduleMode | `Number` | cfgSc#scheduleMode | ONLY IF SUPPORTED - property 'one_time_scheduler'|

##### cfgScSunday

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgScSunday#enable |
| scheduleStartHour | `Number` | cfgScSunday#scheduleStartHour |
| scheduleStartMinutes | `Number` | cfgScSunday#scheduleStartMinutes |
| scheduleDuration | `Number` | cfgScSunday#scheduleDuration |
| scheduleEdgecut | `Number` | cfgScSunday#scheduleEdgecut |

##### cfgScSunday Slot 2

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| enable | `Switch` | cfgScSunday2#enable | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartHour | `Number` | cfgScSunday2#scheduleStartHour | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartMinutes | `Number` | cfgScSunday2#scheduleStartMinutes | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleDuration | `Number` | cfgScSunday2#scheduleDuration | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleEdgecut | `Number` | cfgScSunday2#scheduleEdgecut | ONLY IF SUPPORTED - property 'scheduler_two_slots'|

##### cfgScMonday

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgScMonday#enable |
| scheduleStartHour | `Number` | cfgScMonday#scheduleStartHour |
| scheduleStartMinutes | `Number` | cfgScMonday#scheduleStartMinutes |
| scheduleDuration | `Number` | cfgScMonday#scheduleDuration |
| scheduleEdgecut | `Number` | cfgScMonday#scheduleEdgecut |

##### cfgScMonday Slot 2

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| enable | `Switch` | cfgScMonday2#enable | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartHour | `Number` | cfgScMonday2#scheduleStartHour | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartMinutes | `Number` | cfgScMonday2#scheduleStartMinutes | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleDuration | `Number` | cfgScMonday2#scheduleDuration | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleEdgecut | `Number` | cfgScMonday2#scheduleEdgecut | ONLY IF SUPPORTED - property 'scheduler_two_slots'|

##### cfgScTuesday

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgScTuesday#enable |
| scheduleStartHour | `Number` | cfgScTuesday#scheduleStartHour |
| scheduleStartMinutes | `Number` | cfgScTuesday#scheduleStartMinutes |
| scheduleDuration | `Number` | cfgScTuesday#scheduleDuration |
| scheduleEdgecut | `Number` | cfgScTuesday#scheduleEdgecut |

##### cfgScTuesday Slot 2

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| enable | `Switch` | cfgScTuesday2#enable | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartHour | `Number` | cfgScTuesday2#scheduleStartHour | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartMinutes | `Number` | cfgScTuesday2#scheduleStartMinutes | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleDuration | `Number` | cfgScTuesday2#scheduleDuration | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleEdgecut | `Number` | cfgScTuesday2#scheduleEdgecut | ONLY IF SUPPORTED - property 'scheduler_two_slots'|

##### cfgScWednesday

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgScWednesday#enable |
| scheduleStartHour | `Number` | cfgScWednesday#scheduleStartHour |
| scheduleStartMinutes | `Number` | cfgScWednesday#scheduleStartMinutes |
| scheduleDuration | `Number` | cfgScWednesday#scheduleDuration |
| scheduleEdgecut | `Number` | cfgScWednesday#scheduleEdgecut |

##### cfgScWednesday Slot 2

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| enable | `Switch` | cfgScWednesday2#enable | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartHour | `Number` | cfgScWednesday2#scheduleStartHour | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartMinutes | `Number` | cfgScWednesday2#scheduleStartMinutes | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleDuration | `Number` | cfgScWednesday2#scheduleDuration | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleEdgecut | `Number` | cfgScWednesday2#scheduleEdgecut | ONLY IF SUPPORTED - property 'scheduler_two_slots'|

##### cfgScThursday

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgScThursday#enable |
| scheduleStartHour | `Number` | cfgScThursday#scheduleStartHour |
| scheduleStartMinutes | `Number` | cfgScThursday#scheduleStartMinutes |
| scheduleDuration | `Number` | cfgScThursday#scheduleDuration |
| scheduleEdgecut | `Number` | cfgScThursday#scheduleEdgecut |

##### cfgScThursday Slot 2

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| enable | `Switch` | cfgScThursday2#enable | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartHour | `Number` | cfgScThursday2#scheduleStartHour | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartMinutes | `Number` | cfgScThursday2#scheduleStartMinutes | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleDuration | `Number` | cfgScThursday2#scheduleDuration | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleEdgecut | `Number` | cfgScThursday2#scheduleEdgecut | ONLY IF SUPPORTED - property 'scheduler_two_slots'|

##### cfgScFriday

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgScFriday#enable |
| scheduleStartHour | `Number` | cfgScFriday#scheduleStartHour |
| scheduleStartMinutes | `Number` | cfgScFriday#scheduleStartMinutes |
| scheduleDuration | `Number` | cfgScFriday#scheduleDuration |
| scheduleEdgecut | `Number` | cfgScFriday#scheduleEdgecut |

##### cfgScFriday Slot 2

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| enable | `Switch` | cfgScFriday2#enable | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartHour | `Number` | cfgScFriday2#scheduleStartHour | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartMinutes | `Number` | cfgScFriday2#scheduleStartMinutes | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleDuration | `Number` | cfgScFriday2#scheduleDuration | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleEdgecut | `Number` | cfgScFriday2#scheduleEdgecut | ONLY IF SUPPORTED - property 'scheduler_two_slots'|

##### cfgScSaturday

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgScSaturday#enable |
| scheduleStartHour | `Number` | cfgScSaturday#scheduleStartHour |
| scheduleStartMinutes | `Number` | cfgScSaturday#scheduleStartMinutes |
| scheduleDuration | `Number` | cfgScSaturday#scheduleDuration |
| scheduleEdgecut | `Number` | cfgScSaturday#scheduleEdgecut |

##### cfgScSaturday Slot 2

| Channel   | Type | ChannelName | |
|------------|-----------|-----------|-----------|
| enable | `Switch` | cfgScSaturday2#enable | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartHour | `Number` | cfgScSaturday2#scheduleStartHour | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleStartMinutes | `Number` | cfgScSaturday2#scheduleStartMinutes | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleDuration | `Number` | cfgScSaturday2#scheduleDuration | ONLY IF SUPPORTED - property 'scheduler_two_slots'|
| scheduleEdgecut | `Number` | cfgScSaturday2#scheduleEdgecut | ONLY IF SUPPORTED - property 'scheduler_two_slots'|

##### cfgMultiZones
If Multi Zones are supported, you are able to define 4 separate Zones and split working times by 10 to those.

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| enable | `Switch` | cfgMultiZones#enable |

To ease Zone Configuration, you are able to set distance in meters where a specific Zone starts. Bearing in mind that you roughly shall know how many meters of cable have been used (without buffer).

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| zone1Meter | `Number` | cfgMultiZones#zone1Meter |
| zone2Meter | `Number` | cfgMultiZones#zone2Meter |
| zone3Meter | `Number` | cfgMultiZones#zone3Meter |
| zone4Meter | `Number` | cfgMultiZones#zone4Meter |

As second step you are able to set time in percent and split in parts of 10 between zones,

| Channel   | Type | ChannelName |
|------------|-----------|-----------|
| allocation0 | `Number` | cfgMultiZones#allocation0 |
| allocation1 | `Number` | cfgMultiZones#allocation1 |
| allocation2 | `Number` | cfgMultiZones#allocation2 |
| allocation3 | `Number` | cfgMultiZones#allocation3 |
| allocation4 | `Number` | cfgMultiZones#allocation4 |
| allocation5 | `Number` | cfgMultiZones#allocation5 |
| allocation6 | `Number` | cfgMultiZones#allocation6 |
| allocation7 | `Number` | cfgMultiZones#allocation7 |
| allocation8 | `Number` | cfgMultiZones#allocation8 |
| allocation9 | `Number` | cfgMultiZones#allocation9 |

## File based configuration

You have to have Persistence Service configured in order to use statistic graphs
It's recommended to use rr4dj

### .persist
```
MowerBat_Chart*, MowerBatTemp_Chart*, MowerBatStatus_Chart* : strategy = everyMinute
```

### .things
```
Bridge worxlandroid:worxlandroidBridge:MyWorxBridge "MyWorx Bridge" [ webapiUsername="my username", webapiPassword="my password" ] {
    Thing mower MySerialNumber "MyLandroid Shaun" [ refreshStatusInterval=60, pollingInterval=300 ]
}
```
'MySerialNumber' is the serial number of the mower.

### .items
```
Group All
    Group Mower                    (All)
        Group MowerBat             (Mower)
        Group MowerStatus          (Mower)
        Group MowerSchedule        (Mower)
        Group MowerBat_Chart       (Mower)
        Group MowerBatTemp_Chart   (Mower)
        Group MowerBatStatus_Chart (Mower)


/* Chart Parameters */
Number Mower_Chart_Period         "Chart Period"

/* Landroid */
String Shaun                      "Shaun [%s]"
Switch LandroidEnable             "Mowing enabled"                          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:common#enable"}

String LandroidAction             "Action []"             <movecontrol>     {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:common#action"}
String LandroidLastUpdate         "Last Update [%1$td.%1$tm.%1$ty / %1$tH:%1$tM:%1$tS]"    <calendar>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgCommon#lastUpdate"}
Switch LandroidPoll               "Poll []"               <refresh>         {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:common#poll"}
Switch LandroidLock               "Lock"                  <mlock>           {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:common#lock"}
Number LandroidScheduleMode       "Schedule Mode []"      <party>           {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgSc#scheduleMode"}

String LandroidMacAdress          "MAC [%s]"              <text>            {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#macAdress"}
String LandroidSerialNumber       "Serial Number [%s]"    <text>            {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgCommon#serialNumber"}
Number LandroidFirmware           "Firmware [v%s]"        <text>            {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#firmware"}
Switch LandroidOnline             "Onlinestatus [%s]"     <network>         {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:common#online"}
Number LandroidId                 "Id []"                                   {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgCommon#id"}

String LandroidLastUpdateOnlineStatus    "Last Update Online Status [%1$td.%1$tm.%1$ty / %1$tH:%1$tM:%1$tS]"    <calendar>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:common#lastUpdateOnlineStatus"}

// Multizone
Switch LandroidMultizoneEnable    "Multizone enable []"                            {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#enable"}
Number LandroidLastZone           "Start Zone []"    <zones>    {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#lastZone"}

// Zone Meters
Number LandroidMeterZone1         "Meters Zone 1 [%d]"    <distance>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#zone1Meter"}
Number LandroidMeterZone2         "Meters Zone 2 [%d]"    <distance>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#zone2Meter"}
Number LandroidMeterZone3         "Meters Zone 3 [%d]"    <distance>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#zone3Meter"}
Number LandroidMeterZone4         "Meters Zone 4 [%d]"    <distance>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#zone4Meter"}

// Allocation Zones
Number LandroidAllocation0        "Alloction 0 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation0"}
Number LandroidAllocation1        "Alloction 1 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation1"}
Number LandroidAllocation2        "Alloction 2 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation2"}
Number LandroidAllocation3        "Alloction 3 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation3"}
Number LandroidAllocation4        "Alloction 4 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation4"}
Number LandroidAllocation5        "Alloction 5 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation5"}
Number LandroidAllocation6        "Alloction 6 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation6"}
Number LandroidAllocation7        "Alloction 7 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation7"}
Number LandroidAllocation8        "Alloction 8 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation8"}
Number LandroidAllocation9        "Alloction 9 []"        <zones>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgMultiZones#allocation9"}

// Status
Number LandroidWifiQuality        "Wifi Quality [%d]"     <network>          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#wifiQuality"}
Switch LandroidBatteryCharging    "Battery charging [%s]" <lowbattery>       {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datBattery#batteryCharging"}
Number LandroidStatusCode         "Status Code [%d]"      <lawnmower>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#statusCode"}
String LandroidStatusDescription  "[%s]"                  <lawnmower>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#statusDescription"}
Number LandroidErrorCode          "Error Code [%d]"       <error>            {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#errorCode"}
String LandroidErrorDescription   "Error: [%s]"           <error>            {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datCommon#errorDescription"}

// Rain
Switch          LandroidRainState                       "Rain State []"                   <rain>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datRain#state"}
Number          LandroidRainCounter                     "Rain Counter [%d]"                 <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datRain#counter"}

// Move
Number LandroidPitch              "Pitch [%s]"            <incline>          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datDmp#pitch"}
Number LandroidRoll               "Roll [%s]"             <incline>          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datDmp#roll"}
Number LandroidYaw                "Yaw [%s]"              <incline>          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datDmp#yaw"}

// Battery
Number LandroidBatteryLevel       "Battery Level [%d %%]"     <battery>            (MowerBat, MowerBatStatus_Chart)    {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datBattery#batteryLevel"}
Number LandroidBatteryVoltage     "Battery Voltage [%.2f V]"  <battery>            (MowerBat, MowerBat_Chart)          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datBattery#batteryVoltage"}
Number LandroidBatteryTemperature "Battery Temperature [%.1f °C]" <temperature>    (MowerBat, MowerBatTemp_Chart)      {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datBattery#batteryTemperature"}
Number LandroidBatteryChargeCycle "Battery ChargeCycle [%d]"  <battery>                                                {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datBattery#batteryChargeCycle"}
Number LandroidBatteryChargeCycleCurrent "Battery ChargeCycle Current [%d]"  <battery>                                 {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datBattery#batteryChargeCycleCurrent"}

// Settings
Number LandroidRainDelay             "Rain Delay [%d min]"              <rain>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgCommon#rainDelay"}
Number LandroidScheduleTimeExtension "Schedule Time Extension [%d %%]"  <time>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgSc#scheduleTimeExtension"}

// Statistics
Number LandroidTotalTime          "Total Time [JS(minstohours.js):%s]"           <time>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datSt#totalTime"}
Number:Length LandroidTotalDistance    "Total Distance [%s m]"                   <chart>       {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datSt#totalDistance"}
Number LandroidTotalBladeTime     "Total Bladetime [JS(minstohours.js):%s]"      <time>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datSt#totalBladeTime"}
Number LandroidCurrentBladeTime   "Current Bladetime [JS(minstohours.js):%s]"    <time>        {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:datSt#currentBladeTime"}

//Schedule
// OneTime Schedule
Switch          LandroidOneTimeScheduleEdgecut          "OneTime Edgecut"           <flow>                          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgOneTimeSc#scheduleEdgecut"}
Number          LandroidOneTimeScheduleDuration         "OneTime Duration"          <time>                          {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgOneTimeSc#scheduleDuration"}

// Monday
Switch          LandroidScheduleMondayEnable            "Monday: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday#enable"}
Number          LandroidScheduleMondayStartHour         "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday#scheduleStartHour"}
Number          LandroidScheduleMondayStartMinutes      "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday#scheduleStartMinutes"}
Number          LandroidScheduleMondayDuration          "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday#scheduleDuration"}
Switch          LandroidScheduleMondayEdgecut           "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday#scheduleEdgecut"}
// Monday 2
Switch          LandroidScheduleMonday2Enable            "Monday Slot2: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday2#enable"}
Number          LandroidScheduleMonday2StartHour         "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday2#scheduleStartHour"}
Number          LandroidScheduleMonday2StartMinutes      "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday2#scheduleStartMinutes"}
Number          LandroidScheduleMonday2Duration          "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday2#scheduleDuration"}
Switch          LandroidScheduleMonday2Edgecut           "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScMonday2#scheduleEdgecut"}

// Tuesday
Switch          LandroidScheduleTuesdayEnable           "Tuesday: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday#enable"}
Number          LandroidScheduleTuesdayStartHour        "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday#scheduleStartHour"}
Number          LandroidScheduleTuesdayStartMinutes     "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday#scheduleStartMinutes"}
Number          LandroidScheduleTuesdayDuration         "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday#scheduleDuration"}
Switch          LandroidScheduleTuesdayEdgecut          "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday#scheduleEdgecut"}
// Tuesday 2
Switch          LandroidScheduleTuesday2Enable           "Tuesday Slot2: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday2#enable"}
Number          LandroidScheduleTuesday2StartHour        "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday2#scheduleStartHour"}
Number          LandroidScheduleTuesday2StartMinutes     "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday2#scheduleStartMinutes"}
Number          LandroidScheduleTuesday2Duration         "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday2#scheduleDuration"}
Switch          LandroidScheduleTuesday2Edgecut          "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScTuesday2#scheduleEdgecut"}

// Wednesday
Switch          LandroidScheduleWednesdayEnable         "Wednesday: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday#enable"}
Number          LandroidScheduleWednesdayStartHour      "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday#scheduleStartHour"}
Number          LandroidScheduleWednesdayStartMinutes   "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday#scheduleStartMinutes"}
Number          LandroidScheduleWednesdayDuration       "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday#scheduleDuration"}
Switch          LandroidScheduleWednesdayEdgecut        "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday#scheduleEdgecut"}
// Wednesday 2
Switch          LandroidScheduleWednesday2Enable         "Wednesday Slot2: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday2#enable"}
Number          LandroidScheduleWednesday2StartHour      "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday2#scheduleStartHour"}
Number          LandroidScheduleWednesday2StartMinutes   "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday2#scheduleStartMinutes"}
Number          LandroidScheduleWednesday2Duration       "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday2#scheduleDuration"}
Switch          LandroidScheduleWednesday2Edgecut        "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScWednesday2#scheduleEdgecut"}

// Thursday
Switch          LandroidScheduleThursdayEnable          "Thursday: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday#enable"}
Number          LandroidScheduleThursdayStartHour       "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday#scheduleStartHour"}
Number          LandroidScheduleThursdayStartMinutes    "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday#scheduleStartMinutes"}
Number          LandroidScheduleThursdayDuration        "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday#scheduleDuration"}
Switch          LandroidScheduleThursdayEdgecut         "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday#scheduleEdgecut"}
// Thursday 2
Switch          LandroidScheduleThursday2Enable          "Thursday Slot2: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday2#enable"}
Number          LandroidScheduleThursday2StartHour       "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday2#scheduleStartHour"}
Number          LandroidScheduleThursday2StartMinutes    "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday2#scheduleStartMinutes"}
Number          LandroidScheduleThursday2Duration        "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday2#scheduleDuration"}
Switch          LandroidScheduleThursday2Edgecut         "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScThursday2#scheduleEdgecut"}

// Friday
Switch          LandroidScheduleFridayEnable            "Friday: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday#enable"}
Number          LandroidScheduleFridayStartHour         "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday#scheduleStartHour"}
Number          LandroidScheduleFridayStartMinutes      "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday#scheduleStartMinutes"}
Number          LandroidScheduleFridayDuration          "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday#scheduleDuration"}
Switch          LandroidScheduleFridayEdgecut           "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday#scheduleEdgecut"}
// Friday 2
Switch          LandroidScheduleFriday2Enable            "Friday Slot2: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday2#enable"}
Number          LandroidScheduleFriday2StartHour         "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday2#scheduleStartHour"}
Number          LandroidScheduleFriday2StartMinutes      "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday2#scheduleStartMinutes"}
Number          LandroidScheduleFriday2Duration          "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday2#scheduleDuration"}
Switch          LandroidScheduleFriday2Edgecut           "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScFriday2#scheduleEdgecut"}

// Saturday
Switch          LandroidScheduleSaturdayEnable          "Saturday: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday#enable"}
Number          LandroidScheduleSaturdayStartHour       "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday#scheduleStartHour"}
Number          LandroidScheduleSaturdayStartMinutes    "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday#scheduleStartMinutes"}
Number          LandroidScheduleSaturdayDuration        "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday#scheduleDuration"}
Switch          LandroidScheduleSaturdayEdgecut         "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday#scheduleEdgecut"}
// Saturday 2
Switch          LandroidScheduleSaturday2Enable          "Saturday Slot2: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday2#enable"}
Number          LandroidScheduleSaturday2StartHour       "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday2#scheduleStartHour"}
Number          LandroidScheduleSaturday2StartMinutes    "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday2#scheduleStartMinutes"}
Number          LandroidScheduleSaturday2Duration        "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday2#scheduleDuration"}
Switch          LandroidScheduleSaturday2Edgecut         "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSaturday2#scheduleEdgecut"}

// Sunday
Switch          LandroidScheduleSundayEnable            "Sunday: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday#enable"}
Number          LandroidScheduleSundayStartHour         "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday#scheduleStartHour"}
Number          LandroidScheduleSundayStartMinutes      "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday#scheduleStartMinutes"}
Number          LandroidScheduleSundayDuration          "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday#scheduleDuration"}
Switch          LandroidScheduleSundayEdgecut           "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday#scheduleEdgecut"}
// Sunday 2
Switch          LandroidScheduleSunday2Enable            "Sunday Slot2: [MAP(landroid_schedule_enable.map):%s]" <time>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday2#enable"}
Number          LandroidScheduleSunday2StartHour         "Start Hour [%d]"                   <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday2#scheduleStartHour"}
Number          LandroidScheduleSunday2StartMinutes      "Start Minutes [%d]"                <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday2#scheduleStartMinutes"}
Number          LandroidScheduleSunday2Duration          "Duration [%d]"                     <time>                  {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday2#scheduleDuration"}
Switch          LandroidScheduleSunday2Edgecut           "Edgecut"                           <lawnmower>             {channel="worxlandroid:mower:MyWorxBridge:MySerialNumber:cfgScSunday2#scheduleEdgecut"}
```

### .sitemap
```
sitemap landroid label="Landroid"
{
    Group item=Shaun icon="landroid" {
        Frame item=LandroidStatusDescription {
            Switch item=LandroidAction label="Action" mappings=[START="Start"] visibility=[LandroidStatusCode==0, LandroidStatusCode==1]
            Switch item=LandroidAction label="Action" mappings=[START="Start",HOME="Home"] visibility=[LandroidStatusCode==34]
            Switch item=LandroidAction label="Action" mappings=[STOP="Stop",HOME="Home"] visibility=[LandroidStatusCode==6, LandroidStatusCode==7, LandroidStatusCode==33, LandroidStatusCode==32]
            Switch item=LandroidAction label="Action" mappings=[STOP="Stop"] visibility=[LandroidStatusCode==5,LandroidStatusCode==30]
            Switch item=LandroidLastZone mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"] visibility=[LandroidStatusCode==1]
            Switch item=LandroidOneTimeScheduleEdgecut mappings=[ON="Start Edgecut"] visibility=[LandroidStatusCode==0, LandroidStatusCode==1]
            Slider item=LandroidOneTimeScheduleDuration minValue=0 maxValue=300 step=30 visibility=[LandroidStatusCode==0, LandroidStatusCode==1]
            Switch item=LandroidEnable
            Switch item=LandroidScheduleMode mappings=[1="Normal", 2="Party"]
            Text label="Orientation" icon="compass" visibility=[LandroidStatusCode!="1"] {
                Text item=LandroidPitch
                Text item=LandroidRoll
                Text item=LandroidYaw
            }
            Text label="Settings" icon="settings" {
                Slider item=LandroidScheduleTimeExtension minValue=-100 maxValue=100 step=10
                Slider item=LandroidRainDelay minValue=0 maxValue=750 step=30
               Text label="Schedule" icon="time"{
                    Frame label="Schedule Monday" {
                        Switch item=LandroidScheduleMondayEnable
                        Switch item=LandroidScheduleMondayEdgecut visibility=[LandroidScheduleMondayEnable==ON]
                        Slider item=LandroidScheduleMondayStartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleMondayEnable==ON]
                        Slider item=LandroidScheduleMondayStartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleMondayEnable==ON]
                        Slider item=LandroidScheduleMondayDuration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleMondayEnable==ON]
                    }
                    Frame label="Schedule Monday Slot 2" {
                        Switch item=LandroidScheduleMonday2Enable
                        Switch item=LandroidScheduleMonday2Edgecut visibility=[LandroidScheduleMonday2Enable==ON]
                        Slider item=LandroidScheduleMonday2StartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleMonday2Enable==ON]
                        Slider item=LandroidScheduleMonday2StartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleMonday2Enable==ON]
                        Slider item=LandroidScheduleMonday2Duration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleMonday2Enable==ON]
                    }
                    Frame label="Schedule Tuesday" {
                        Switch item=LandroidScheduleTuesdayEnable
                        Switch item=LandroidScheduleTuesdayEdgecut visibility=[LandroidScheduleTuesdayEnable==ON]
                        Slider item=LandroidScheduleTuesdayStartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleTuesdayEnable==ON]
                        Slider item=LandroidScheduleTuesdayStartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleTuesdayEnable==ON]
                        Slider item=LandroidScheduleTuesdayDuration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleTuesdayEnable==ON]
                    }
                    Frame label="Schedule Tuesday Slot 2" {
                        Switch item=LandroidScheduleTuesday2Enable
                        Switch item=LandroidScheduleTuesday2Edgecut visibility=[LandroidScheduleTuesday2Enable==ON]
                        Slider item=LandroidScheduleTuesday2StartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleTuesday2Enable==ON]
                        Slider item=LandroidScheduleTuesday2StartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleTuesday2Enable==ON]
                        Slider item=LandroidScheduleTuesday2Duration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleTuesday2Enable==ON]
                    }
                    Frame label="Schedule Wednesday" {
                        Switch item=LandroidScheduleWednesdayEnable
                        Switch item=LandroidScheduleWednesdayEdgecut visibility=[LandroidScheduleWednesdayEnable==ON]
                        Slider item=LandroidScheduleWednesdayStartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleWednesdayEnable==ON]
                        Slider item=LandroidScheduleWednesdayStartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleWednesdayEnable==ON]
                        Slider item=LandroidScheduleWednesdayDuration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleWednesdayEnable==ON]
                    }
                    Frame label="Schedule Wednesday Slot 2" {
                        Switch item=LandroidScheduleWednesday2Enable
                        Switch item=LandroidScheduleWednesday2Edgecut visibility=[LandroidScheduleWednesday2Enable==ON]
                        Slider item=LandroidScheduleWednesday2StartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleWednesday2Enable==ON]
                        Slider item=LandroidScheduleWednesday2StartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleWednesday2Enable==ON]
                        Slider item=LandroidScheduleWednesday2Duration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleWednesday2Enable==ON]
                    }
                    Frame label="Schedule Thursday" {
                        Switch item=LandroidScheduleThursdayEnable
                        Switch item=LandroidScheduleThursdayEdgecut visibility=[LandroidScheduleThursdayEnable==ON]
                        Slider item=LandroidScheduleThursdayStartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleThursdayEnable==ON]
                        Slider item=LandroidScheduleThursdayStartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleThursdayEnable==ON]
                        Slider item=LandroidScheduleThursdayDuration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleThursdayEnable==ON]
                    }
                    Frame label="Schedule Thursday Slot 2" {
                        Switch item=LandroidScheduleThursday2Enable
                        Switch item=LandroidScheduleThursday2Edgecut visibility=[LandroidScheduleThursday2Enable==ON]
                        Slider item=LandroidScheduleThursday2StartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleThursday2Enable==ON]
                        Slider item=LandroidScheduleThursday2StartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleThursday2Enable==ON]
                        Slider item=LandroidScheduleThursday2Duration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleThursday2Enable==ON]
                    }
                    Frame label="Schedule Friday" {
                        Switch item=LandroidScheduleFridayEnable
                        Switch item=LandroidScheduleFridayEdgecut visibility=[LandroidScheduleFridayEnable==ON]
                        Slider item=LandroidScheduleFridayStartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleFridayEnable==ON]
                        Slider item=LandroidScheduleFridayStartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleFridayEnable==ON]
                        Slider item=LandroidScheduleFridayDuration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleFridayEnable==ON]
                    }
                    Frame label="Schedule Friday Slot 2" {
                        Switch item=LandroidScheduleFriday2Enable
                        Switch item=LandroidScheduleFriday2Edgecut visibility=[LandroidScheduleFriday2Enable==ON]
                        Slider item=LandroidScheduleFriday2StartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleFriday2Enable==ON]
                        Slider item=LandroidScheduleFriday2StartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleFriday2Enable==ON]
                        Slider item=LandroidScheduleFriday2Duration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleFriday2Enable==ON]
                    }
                    Frame label="Schedule Saturday" {
                        Switch item=LandroidScheduleSaturdayEnable
                        Switch item=LandroidScheduleSaturdayEdgecut visibility=[LandroidScheduleSaturdayEnable==ON]
                        Slider item=LandroidScheduleSaturdayStartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleSaturdayEnable==ON]
                        Slider item=LandroidScheduleSaturdayStartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleSaturdayEnable==ON]
                        Slider item=LandroidScheduleSaturdayDuration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleSaturdayEnable==ON]
                    }
                    Frame label="Schedule Saturday Slot 2" {
                        Switch item=LandroidScheduleSaturday2Enable
                        Switch item=LandroidScheduleSaturday2Edgecut visibility=[LandroidScheduleSaturday2Enable==ON]
                        Slider item=LandroidScheduleSaturday2StartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleSaturday2Enable==ON]
                        Slider item=LandroidScheduleSaturday2StartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleSaturday2Enable==ON]
                        Slider item=LandroidScheduleSaturday2Duration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleSaturday2Enable==ON]
                    }
                    Frame label="Schedule Sunday" {
                        Switch item=LandroidScheduleSundayEnable
                        Switch item=LandroidScheduleSundayEdgecut visibility=[LandroidScheduleSundayEnable==ON]
                        Slider item=LandroidScheduleSundayStartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleSundayEnable==ON]
                        Slider item=LandroidScheduleSundayStartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleSundayEnable==ON]
                        Slider item=LandroidScheduleSundayDuration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleSundayEnable==ON]
                    }
                    Frame label="Schedule Sunday Slot 2" {
                        Switch item=LandroidScheduleSunday2Enable
                        Switch item=LandroidScheduleSunday2Edgecut visibility=[LandroidScheduleSunday2Enable==ON]
                        Slider item=LandroidScheduleSunday2StartHour minValue=0 maxValue=23 step=1 visibility=[LandroidScheduleSunday2Enable==ON]
                        Slider item=LandroidScheduleSunday2StartMinutes minValue=0 maxValue=45 step=15 visibility=[LandroidScheduleSunday2Enable==ON]
                        Slider item=LandroidScheduleSunday2Duration minValue=0 maxValue=1425 step=15 visibility=[LandroidScheduleSunday2Enable==ON]
                    }
                }
                Text label="MultiZone" icon="zones" {
                    Frame label="Zone Meters" {
                        Switch item=LandroidMultizoneEnable
                        Slider item=LandroidMeterZone1 minValue=0 maxValue=120 visibility=[LandroidMultizoneEnable==ON]
                        Slider item=LandroidMeterZone2 minValue=0 maxValue=120 visibility=[LandroidMultizoneEnable==ON]
                        Slider item=LandroidMeterZone3 minValue=0 maxValue=120 visibility=[LandroidMultizoneEnable==ON]
                        Slider item=LandroidMeterZone4 minValue=0 maxValue=120 visibility=[LandroidMultizoneEnable==ON]
                    }
                    Frame label="Allocation Zones" visibility=[LandroidMultizoneEnable==ON] {
                        Switch item=LandroidAllocation0 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation1 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation2 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation3 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation4 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation5 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation6 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation7 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation8 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                        Switch item=LandroidAllocation9 mappings=[0="Zone 1", 1="Zone 2", 2="Zone 3", 3="Zone 4"]
                    }
                }
            }
        }
        Frame label="Status" {
            Text item=LandroidLastUpdate
            Switch item=LandroidPoll label="Refresh" mappings=[ON="Poll"]
            Text item=LandroidOnline
            Text item=LandroidWifiQuality
            Text item=LandroidStatusDescription
            Text item=LandroidErrorDescription visibility=[LandroidErrorCode!=0]
            Switch item=LandroidRainState visibility=[LandroidRainCounter!=0]
            Text item=LandroidRainCounter visibility=[LandroidRainCounter!=0]
            Text label="Device Information" icon="landroid" {
                Switch item=LandroidLock label="Lock" mappings=[ON="LOCK",OFF="UNLOCK"]
                Text item=LandroidLastUpdateOnlineStatus
                Text item=LandroidSerialNumber
                Text item=LandroidMacAdress
                Text item=LandroidFirmware
            }
            Text item=LandroidBatteryLevel {
                Text item=LandroidBatteryCharging
                Text item=LandroidBatteryVoltage
                Text item=LandroidBatteryTemperature
            }
            Text label="Statistics" icon="line"{
                Frame label="General" {
                    Text item=LandroidTotalTime
                    Text item=LandroidTotalDistance label="Total Distance [%.2f km]"
                    Text item=LandroidTotalBladeTime
                    Text item=LandroidCurrentBladeTime
                }
                Frame label="Battery" {
                    Text item=LandroidBatteryChargeCycle
                    Text item=LandroidBatteryChargeCycleCurrent
                    Switch item=Mower_Chart_Period label="Skalierung" mappings=[0="1H", 1="4H", 2="8H", 3="12H", 4="1D", 5="1W", 6="1M", 7="1Y"]
                    Chart item=MowerBatStatus_Chart period=h refresh=6000 legend=true visibility=[Mower_Chart_Period==0]
                    Chart item=MowerBatStatus_Chart period=4h refresh=30000 legend=true visibility=[Mower_Chart_Period==1]
                    Chart item=MowerBatStatus_Chart period=8h refresh=30000 legend=true visibility=[Mower_Chart_Period==2]
                    Chart item=MowerBatStatus_Chart period=12h refresh=30000 legend=true visibility=[Mower_Chart_Period==3, Mower_Chart_Period=="NULL", Mower_Chart_Period=="Uninitialized"]
                    Chart item=MowerBatStatus_Chart period=D refresh=30000 legend=true visibility=[Mower_Chart_Period==4]
                    Chart item=MowerBatStatus_Chart period=W refresh=30000 legend=true visibility=[Mower_Chart_Period==5]
                    Chart item=MowerBatStatus_Chart period=M refresh=30000 legend=true visibility=[Mower_Chart_Period==6]
                    Chart item=MowerBatStatus_Chart period=Y refresh=30000 legend=true visibility=[Mower_Chart_Period==7]

                    Chart item=MowerBat_Chart period=h refresh=6000 legend=true visibility=[Mower_Chart_Period==0]
                    Chart item=MowerBat_Chart period=4h refresh=30000 legend=true visibility=[Mower_Chart_Period==1]
                    Chart item=MowerBat_Chart period=8h refresh=30000 legend=true visibility=[Mower_Chart_Period==2]
                    Chart item=MowerBat_Chart period=12h refresh=30000 legend=true visibility=[Mower_Chart_Period==3, Mower_Chart_Period=="NULL", Mower_Chart_Period=="Uninitialized"]
                    Chart item=MowerBat_Chart period=D refresh=30000 legend=true visibility=[Mower_Chart_Period==4]
                    Chart item=MowerBat_Chart period=W refresh=30000 legend=true visibility=[Mower_Chart_Period==5]
                    Chart item=MowerBat_Chart period=M refresh=30000 legend=true visibility=[Mower_Chart_Period==6]
                    Chart item=MowerBat_Chart period=Y refresh=30000 legend=true visibility=[Mower_Chart_Period==7]

                    Chart item=MowerBatTemp_Chart period=h refresh=6000 legend=true visibility=[Mower_Chart_Period==0]
                    Chart item=MowerBatTemp_Chart period=4h refresh=30000 legend=true visibility=[Mower_Chart_Period==1]
                    Chart item=MowerBatTemp_Chart period=8h refresh=30000 legend=true visibility=[Mower_Chart_Period==2]
                    Chart item=MowerBatTemp_Chart period=12h refresh=30000 legend=true visibility=[Mower_Chart_Period==3, Mower_Chart_Period=="NULL", Mower_Chart_Period=="Uninitialized"]
                    Chart item=MowerBatTemp_Chart period=D refresh=30000 legend=true visibility=[Mower_Chart_Period==4]
                    Chart item=MowerBatTemp_Chart period=W refresh=30000 legend=true visibility=[Mower_Chart_Period==5]
                    Chart item=MowerBatTemp_Chart period=M refresh=30000 legend=true visibility=[Mower_Chart_Period==6]
                    Chart item=MowerBatTemp_Chart period=Y refresh=30000 legend=true visibility=[Mower_Chart_Period==7]
                }
            }
        }
    }
}
```

### Preview of above Sitemap

<img src="images/SC_BasicUI_Main_Status.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_Home.png" width="50%">

#### "Start Zone" will change to "Orientation" once Mower is underway
<br>
<img src="images/SC_BasicUI_Main_Action.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_Orientation.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_Settings.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_Settings_Schedule.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_Settings_MultiZone.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_DeviceInfo.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_Battery.png" width="50%">
<br>
<img src="images/SC_BasicUI_Main_Statistic.png" width="50%">

### .rules

Update Landroid Status to reflect in main menu

```
rule MowerStatus
when
  Item LandroidErrorCode changed or
  Item LandroidStatusCode changed
then
  if (LandroidErrorCode.state != 0) {
    Shaun.postUpdate(transform("MAP", "landroid_error_de.map", LandroidErrorCode.state.toString))
  } else {
    Shaun.postUpdate(transform("MAP", "landroid_status_de.map", LandroidStatusCode.state.toString))
  }
end
```

Rule "MowerStatusMonitor" requires configured pushNotification or adopt this to Mail Action

```
rule MowerStatusMonitor
when
    Item Mower_StatusCode changed
then
    if (LandroidStatusCode.state == 5 && previousState == 0) {
        pushNotification("Shaun: " + now, "Regenverzögerung ausgelöst")
    } else if (LandroidStatusCode.state == 0 && previousState == 5) {
        pushNotification("Shaun: " + now, "Regenverzögerung beendet")
    } else {
        pushNotification("Shaun: " + now, previousState + " changed to " + LandroidStatusCode.state)
    }
end
```

### .transform

Place the following *.map to your ..\conf\transform

1. [landroid_error_de.map](/openhab-conf/transform/landroid_error_de.map)
2. [landroid_status_de.map](/openhab-conf/transform/landroid_status_de.map)
3. [landroid_schedule_enable](/openhab-conf/transform/landroid_schedule_enable.map)
4. [minstohours.js](/openhab-conf/transform/minstohours.js)

## Iconset

Several Icons have been created in order to suit requirements for robo-mower. Please feel free to download from this repository and place/extract them into ..\conf\icons\classic

1. [Mower](/openhab-conf/icons/landroid.png)
2. [Distance](/openhab-conf/icons/distance.zip)
3. [Zones](/openhab-conf/icons/zones.zip)
4. [Refresh](/openhab-conf/icons/refresh.png)
5. [Lock](/openhab-conf/icons/mlock.zip)

## Support

If you encounter critical issues with this binding, please consider to:

- create an [issue](https://github.com/nibi79/worxlandroid/issues) on GitHub
- search [community forum](https://community.openhab.org/) for answers already given
- or make a new post there, if nothing was found

In any case please provide some information about your problem:

- openHAB and binding version
- error description and steps to retrace if applicable
- any related `[WARN]`/`[ERROR]` from openhab.log (`log:set DEBUG org.openhab.binding.worxlandroid`)
- whether it's the binding, bridge, device or channel related issue

For the sake of documentation please use English language.
