# SmartThingsX
![Image of Logo](https://github.com/enishoca/SmartThingsX/raw/master/x1oredb.png)

SmartThing SmartApps and Device Handlers for X-10 devices using Node Red and Mochad

This solution allows 2-way communication between SmartThings and X-10 devices using Node Red and Mochad.  You can control your X-10 devices from within ST and use various automation solutions within ST.  You can also use X-10 remotes and motion sensors to control other ST devices.   X-10 equipment is fairly inexpensive and  readily available on E-Bay etc.

In this solution all X-10 settings and device mappings are done using the SmartThings app. Only a couple of one-time settings need to be made on the Node Red flow.   

See this page for details 
https://community.smartthings.com/t/release-node-red-bridge-for-x-10-devices-switches-modules-remotes-motion-sensors/116909

Supported Devices and Functionality
* X-10 Switches and Modules -  on/off is supported
* X-10 Motion Senors and Remote buttions -  on/off funtions supported from devices sending housecode/unitcode on/off commands

Please note: Dimmer functions and security devices are not supported

**Requirements**

Raspberry Pi 

These instructions are for Raspberry Pi, but they can be easily adapted to any Linux variant

Mochad and CM15A or CM19A USB Devices

Mochad exposes a TCP interface to X-10 commands, and can run on any linux system including Raspberry PI. It uses CM15A or CM19A USB devices to communicate with X-10 over RF or powerline. Using these devices you are not limited to a single house code, you can use any of the 15 house code with any x-10 device.
https://bfocht.github.io/mochad/
https://bfocht.github.io/mochad/mochad_reference.html

Node Red

Node-RED is a programming tool for wiring together hardware devices, APIs and online services using a browser-based editor.  Node Red is very simple to deploy and is very light-weight. Requires very little effort to setup and run
https://nodered.org/ 
https://nodered.org/docs/hardware/raspberrypi 


Setup
-------------------------------------
**Step 1: Install Mochad on Raspberry Pi**

Follow any number of tutorials to setup your Raspberry Pi using your favorite distro, make sure you install the development tools. On Raspbian you can do this using the following command

> sudo apt-get install build-essential

***Build mochad***

* Install libusb-1.0.0 development files. mochad will not build without this.

  > $ sudo apt-get install libusb-1.0-0-dev

* Get mochad source code and unpack it.

  > $ wget -O mochad.tgz http://sourceforge.net/projects/mochad/files/latest/download 
  > $ tar xf mochad.tgz
  > Now build mochad.
  > $ cd mochad*
  > $ ./configure
  > $ make
  > $ sudo make install

* Edit /etc/udev/rules.d/91-usb-x10-controllers.rules change the following line to : 

  > RUN+="/usr/local/bin/mochad &"  

  This ensures that mochad will load whenever the usb device is detected, include when Raspberry pi reboots

 ***Blink a lamp***

  * Plug in a CM15A, CM19A, or CM15Pro into a Raspi USB port.  Connect a lamp to X10 module set for address A1.
* If using a CM15A or CM15Pro try the following power line (pl) commands.
    > $ echo "pl a1 on" | nc localhost 1099
    > $ echo "pl a1 off" | nc locahost 1099

* If using a CM19A try the following radio frequency (rf) commands.
  > $ echo "rf a1 on" | nc localhost 1099
  > $ echo "rf a1 off" | nc localhost 1099

  At this point you should make a list of commands that you need to turn your X-10 devices on and off, you will need this later.


  ***credit - http://x10linux.blogspot.com/2012/08/installing-mochad-on-raspberry-pi.html*** 

**Step 2: Install & Setup Node Red**

 ***Install Node Red***
Follow the instructions on this page to install Node Red on your Pi https://nodered.org/docs/hardware/raspberrypi 
Play around a little bit to get the hangs of the basics

 ***Setup Node Red***
* Copy the contents of smartthings-x10.flow.json to the clipboard from https://raw.githubusercontent.com/enishoca/SmartThingsX/master/node-red-flows/smartthings-x10.flow.json

* Browse to the Node Red page on your browser

* Click on the menu to right and select Import->Clipboard


* Copy the contents of the clipboard in the edit box, select 'new flow' and import it


* Edit the flow and put the hostname for the mochad server, and the file path for the settings file.  If you are running node red as user pi and mochad is running on the same machine, you wouldn't need to make any changes.  

* Next Deploy the flow to activate it.
 
**Step 3: Install the SmartApps and Device Handler in Smertthings**
You will need to install the following SmartApps and DTH - make sure you install all 4
* x-10-node-red-bridge.groovy
* x-10-node-red-button-child.groovy
* x-10-node-red-switch-child.groovy
* x-10-node-red-device.groovy

The easiest way to get these is to use the github repository by clicking "Settings" when in the Device Handlers or SmartApps segment of the API, and adding a github repository:

Owner: enishoca
Repository Name: SmartThingsX
Branch: master

Otherwise, you can download them from here:
https://github.com/enishoca/SmartThingsX/blob/master/smartapps/enishoca/x-10-node-red-bridge.src/x-10-node-red-bridge.groovy

https://github.com/enishoca/SmartThingsX/blob/master/smartapps/enishoca/x-10-node-red-button-child.src/x-10-node-red-button-child.groovy

https://github.com/enishoca/SmartThingsX/blob/master/smartapps/enishoca/x-10-node-red-switch-child.src/x-10-node-red-switch-child.groovy

https://github.com/enishoca/SmartThingsX/blob/master/devicetypes/enishoca/x-10-node-red-device.src/x-10-node-red-device.groovy


**Step 4: SmartApp Setup and Adding Switches, Modules, Remotes and Motion Sensors**
* Install the SmartApp from your phone 
You will need to select the hub and configure IP address and port for the Node Red server - this should be the same IP address and port you use to connect to Node Red on the browser 

* You can then add X-10 devices on smartthings and set their X-10 addresses.  
Once you add a switch you should be able to turn it on and off.  You can map remote buttons and motion sensor address to any ST Thing that supports on/off function and control it.

