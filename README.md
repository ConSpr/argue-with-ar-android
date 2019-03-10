# Testing: argue-with-ar-android 🍭
A minimal test app for live image recognition using ARCore.
<br /><br />
**Note: You can find another version of this where the prototype but not the testing is in the foreground [here](https://github.com/ConSpr/argue-with-ar-android/).**

Also check out the [iOS version](https://github.com/hendriku/argue-with-ar-ios/tree/testing) of this project.

## Usage
The app starts and tries to recognize an image. If the image detection was successful the app restarts itself up to 100 times. If there has been no image detected for 15 seconds the app will restart. Imageset can be defined in a AugmentedImagesDatabase file. You can generate such a file with the [arcoreimg tool](https://developers.google.com/ar/develop/c/augmented-images/arcoreimg) from Goolge

## Test conditions

|Condition|Decription|
|------|----------|
|Images|There are four different imagesets. Some are similar some are distinct in terms of lightning, perspective or appearance.|
|Light|We tested three different types of lightning conditions. Bright (300 lux), normal (120 lux) and dark (20 lux).|
|Background|Tests were executed on restful and noisy backgrounds.|
|Device orientation|The devices werde tested in landscape and portrait orientation|

## Test metrics
|Metric|Decription|
|------|----------|
|Speed|Time the device on a tripod needs from orienting to recognizing an image.|
|Recognition rate|Determines whether the device recognices the imageset at all.|

In the case study the test metrics were measured on a base of 100 test executions for each set of test conditions.

### Credits
This project was started for our case study on DHBW Stuttgart.<br /><br />
[ConSpr](https://github.com/ConSpr),
[hendriku](https://github.com/hendriku)
