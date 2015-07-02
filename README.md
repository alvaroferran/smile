# Smile
The tablet that turns to follow your face as you move. 

---

It uses OpenCV's haarcascade to find front-facing faces and communicates over bluetooth with an arduino to move itself around.

[This wheel](https://github.com/bq/mechatronics/tree/master/wheels/clippableWheel) is used to as the stand to move the tablet.

Note: This will NOT work unless you change the variable "address" in the Android MainActivity.java file to match your own bluetooth module's MAC address.

Attributions
--------------------------------------------------------------
 - [Alvaro Ferrán Cifuentes](https://github.com/alvaroferran)
  - Android app, PCB design

 - [Pighixxx](http://www.pighixxx.com/test/)
  - Orginal idea, PCB design
