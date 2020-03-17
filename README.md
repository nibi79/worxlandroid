# WorxLandroid Binding

This binding connects openHAB with your WorxLandroid Mower using the API and MQTT.

# Table of contents

1. [Disclaimer](https://github.com/nibi79/worxlandroid/tree/master#disclaimer)
2. [Installation and upgrade](https://github.com/nibi79/worxlandroid/tree/master#installation-and-upgrade)
3. [Supported Things](https://github.com/nibi79/worxlandroid/tree/master#supported-things)
4. [Discovery](https://github.com/nibi79/worxlandroid/tree/master#discovery)
5. [Configuration](https://github.com/nibi79/worxlandroid/tree/master#configuration)
6. [Channels](https://github.com/nibi79/worxlandroid/tree/master#channels)
7. [File based configuration](https://github.com/nibi79/worxlandroid/tree/master#file-based-configuration)
8. [Support](https://github.com/nibi79/worxlandroid/tree/master#support)

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

Manual configuration can be achieved via PaperUI - Configuration - Things - Add Thing “+” Button - WorxLandroid Binding - Add manually - **Bridge Worx Landroid API**. Here you can provide your credentials for WorxLandroid account. Once the server thing has been added **Worx Landroid Mower**s will be discovered automatically and appear in your PaperUI inbox. You just need to add them as new things.

## Binding Configuration

Following options can be set for the **Bridge Worx Landroid API**:

| Property   | Description |
|-----------|-----------|
| username | Username to access the WorxLandroid API. |
| password | Password to access the WorxLandroid API. |

## Channels

### Currently following **Channels** are supported on the **Worx Landroid Mower**:

| Channel   | Type | Values |
|------------|-----------|-----------|
| state      | `Switch` | ON/OFF |
TODO

## File based configuration

### .things
```
TODO
```

### .items
```
TODO
```

### .sitemap
```
TODO
```

### .rules
```
TODO
```

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

