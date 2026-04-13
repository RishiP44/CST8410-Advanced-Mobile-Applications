---

SENSOR SYNC

This project started as a lab requirement. I built it to understand how Android sensors, Bluetooth communication, and a simple backend work together in one flow. I tested it across two devices and adjusted things as they broke.

---

OVERVIEW

Sensor Sync records real-time sensor data and shares it between devices.

The app reads ambient light and proximity values. When I moved the phone or changed lighting, I saw values update instantly. Each reading gets stored with a timestamp and an optional photo.

I added Bluetooth connection to link two devices. Once connected, both devices exchange data. I tested this on two phones on the same network. Remote values updated after connection, which confirmed the flow was working.

The app also sends readings to a Node.js server. I exposed the server using ngrok and opened it in a browser. Images and data appeared there in a simple layout.

---

FEATURES

* Real-time ambient light and proximity tracking
* Photo capture linked to sensor readings
* Device-to-device connection using Bluetooth
* Local storage of readings
* Server sync with image display
* Clear history (local and server)

---

TECH STACK

Frontend

* Kotlin
* Jetpack Compose

Backend

* Node.js
* Express

Other

* Bluetooth (device connection)
* Android Sensors (light, proximity)
* ngrok (server exposure)

---

PROJECT STRUCTURE

final_project/

* MainActivity.kt
* navigation/
* viewmodel/
* data/

  * local/
  * remote/
* sensors/
* network/
* ui/

I kept the structure modular so each part stays isolated. ViewModel handles state, repository handles data, and UI reacts to state.

---

HOW IT WORKS

1. App starts and sensors begin listening
2. Sensor values update ViewModel
3. User captures a photo
4. Reading is created with:

   * timestamp
   * sensor values
   * image (Base64)
5. Reading is saved locally
6. Reading is sent to server
7. Server stores and displays it
8. Bluetooth connection allows syncing between devices

I tested each step separately before combining them.

---

SETUP

1. Clone the repository
2. Open in Android Studio
3. Run the Node server:

```id="r1"
node server.js
```

4. Start ngrok:

```id="r2"
ngrok http 3000
```

5. Copy ngrok URL and paste into:

* ServerApiHelper (remote)
* network ServerApiHelper

6. Run the app on device

---

TESTING

I tested using two phones:

* Both on same WiFi
* Bluetooth enabled

Steps I followed:

* Install app on both devices
* Connect devices
* Capture photo
* Check local history
* Open ngrok link
* Verify image appears
* Clear history
* Confirm data removed

At one point, old images kept showing. Restarting ngrok and updating the URL fixed it.

---

PRIVACY

The app processes:

* sensor data
* device identifiers
* user-captured images

No external services or ads are used.

Privacy policy:
[Insert your GitHub privacy policy link here]

---

KNOWN LIMITATIONS

* Bluetooth permission handling varies by device
* Older Android devices do not always prompt correctly
* Server data resets on restart
* No authentication or encryption

---

FUTURE IMPROVEMENTS

* Stable Bluetooth connection handling
* Persistent backend database
* Better UI for connection status
* Error handling for permissions
* Optional user accounts

---

AUTHOR

Rishi Patel

---
