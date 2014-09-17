# background tracker

An android app to track you in the background with minimal battery impact and show your locations them to you on a map when you open the app.

## How do I build this?

1.  Download sbt from your favorite package manager
2.  Connect your device with ADB
3.  sbt android:run
4.  Done!

## FAQ

-   Why?
    -   I felt like seeing how well an app could track you with `NO_POWER`, it turns out that when walking around you get rather good data (i.e. you can easily identify the route you took assuming that there are wifi APs on your route), for other modes of transport you only get cell tower accuracy (which is filtered out with this app).
-   What libraries does this use?
    -   Scala 2.11, [macroid](https://macroid.github.io/), spray-json, google play services
-   What is the compatibility of this?
    -   probably anything that google play services works on, but I have have only tested it on 4.4.4 devices.
-   Why does this app use the `INTERNET` permission?
    -   For google to download map tiles of course.
-   How do I get the data out?
    -   `adb shell su -c '\'cp /data/data/me.venuatu.tracker/files/locations.txt /sdcard/\''`
    -   `adb pull /sdcard/locations.txt`
    -   Is there a better way? nope, you can add one though.

## LICENSE

UNLICENSE or public domain, whichever is better for you.
