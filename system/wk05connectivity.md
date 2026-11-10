# Hardware Connectivity Status Update

## Recent and Current Progress

- Successfully connected all Raspberry Pis to the campus Wi-Fi network.
- Designated Student D's Raspberry Pi to send sensor readings to Cloud Firestore. The connection between the Raspberry Pi and Firebase has been successfully established.
- Successfully merged the hardware sensor code. Further work will focus on integrating the sensor data with the Android application and refining the code to support all required functions.
- Resolved last week's daisy-chaining issue: the sensors could not be successfully daisy-chained while retaining the Sense HAT. The Sense HAT was removed and replaced with a BME688 environmental sensor, and the sensors were successfully daisy-chained on the PCB.

## Problems and Hyperlinks/URLs to Potential Solutions

**Color sensor issue:**
- The color sensor has difficulty accurately detecting and distinguishing colors.
- Further troubleshooting will be attempted. If the issue persists, the sensor may be removed because the project's updated requirements may no longer require color detection.

**Useful resources:**
- [Adafruit CircuitPython OPT4048 library](https://github.com/adafruit/Adafruit_CircuitPython_OPT4048) — reference for troubleshooting the color sensor.
- [Cloud Firestore documentation](https://firebase.google.com/docs/firestore) — reference for integrating and transmitting sensor data.

## Financial

### Expenditures Since Previous Report

- Bolts and nuts for hardware assembly: $10 CAD

### Planned Future Expenses

- No additional expenses currently planned.
