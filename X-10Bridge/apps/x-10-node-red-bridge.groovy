/**
 *  X-10 Node Red Bridge Smart App
 *
 * 	Author: Enis Hoca
 *   - enishoca@outlook.com
 *
 *  Copyright 2018 Enis Hoca
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

import groovy.json.JsonSlurper

definition(
  name: "X-10 Node Red Bridge",
  namespace: "enishoca",
  author: "Enis Hoca",
  description: "A bridge between SmartThings and X-10 via Node Red",
  category: "My Apps",
  iconUrl: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png",
  iconX2Url: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png",
  iconX3Url: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png",
  singleInstance: true
)

preferences {
  page(name: "pageMain")
  page(name: "buttonSettings")
  page(name: "switchSettings")
  page(name: "securitySettings")
}

def pageMain() {
  def installed = app.installationState == "COMPLETE"
  dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {

    section("Node Red Settings") {
      input "nodeRedAddress", "text", title: "Node Red IP Address", description: "(ie. 192.168.1.10)", required: true
      input "nodeRedPort", "text", title: "Node Red  Port", description: "(ie. 1880)", required: true, defaultValue: "1880"
    }

    if (installed) {
      section(title: "X-10 Switches and Modules") {
        href "switchSettings", title: "Switches and Modules", description: "Tap here to add or manage X-10 Switches and Modules", 
              image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", required: false, page: "switchSettings"
      }
      section(title: "X-10 Remotes and Motion Sensors") {
        href "buttonSettings", title: "Remotes and Motion Sensors", description: "Tap here to add or manage X-10 Remotes and Motion Sensors", 
             image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", required: false, page: "buttonSettings"
      }
      section(title: "X-10 Security Sensors") {
        href "securitySettings", title: "Security Remotes and Sensors", description: "Tap here to add or manage X-10 Security Remotes and Sensors", 
             image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", required: false, page: "buttonSettings"
      }

    }
  }
}

def buttonSettings() {
  dynamicPage(name: "buttonSettings", title: "X-10 Remotes and Motion Sensors", install: false, uninstall: false) {
    section() {
      app(name: "childButtons", appName: "X-10 Node Red Button Child", namespace: "enishoca", title: "Add a new remote button or motion sensor...", 
          image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", multiple: true)
    }
  }

}

def switchSettings() {
  dynamicPage(name: "switchSettings", title: "X-10 Switches and Modules", install: false, uninstall: false) {
    section() {
      app(name: "childSwitches", appName: "X-10 Node Red Switch Child", namespace: "enishoca", title: "Add a new switches or module...", 
          image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", multiple: true)
    }
  }

}

def securitySettings() {
  dynamicPage(name: "securitySettings", title: "X-10 Security Sensors", install: false, uninstall: false) {
    section() {
      app(name: "childSecurity", appName: "X-10 Node Red Security Child", namespace: "enishoca", title: "Add a new door/window or other security sensor...", 
          image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", multiple: true)
    }
  }

}

def installed() {
  initialize()
}

def uninstalled() {

}

def initialize() {

  addX10Device()
  setupsubs() 
}

def updated() {
  unsubscribe()
  setupsubs()
}


def removeChildDevices(delete) {
  getChildDevices().find {
    d -> d.deviceNetworkId.startsWith(theDeviceNetworkId)
  }

  delete.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

def addX10Device() {
  //log.debug "Adding Device ${deviceName}"
  //if (!deviceName) return
  def deviceName = "Mochad-client"
  def getHostHubId = location.hubs[0].id //parent.settings.getHostHubId
  def theDeviceNetworkId = getX10DeviceID()
  def theDevice = addChildDevice("enishoca", "X-10 Mochad Device", theDeviceNetworkId, getHostHubId, [label: deviceName, name: deviceName])
  setX10DeviceID(theDevice)
  updateX10Device();
  log.debug "New Device added ${deviceName}"
}

def setupsubs() {
  log.debug "Updating Device ${deviceName}"
  def deviceName = "Mochad-client"
  log.debug "updateX10Device  ${deviceName}"
  def theDeviceNetworkId = getX10DeviceID();
  def theDevice = getDevicebyNetworkId(getX10DeviceID())
  if (theDevice) { // The switch already exists
    setX10DeviceID(theDevice)
    theDevice.label = deviceName
    theDevice.name = deviceName
	log.debug "Subscribe to events  ${deviceName}"
    subscribe(theDevice, "switch", switchChange)
    subscribe(theDevice, "switch.setLevel", switchSetLevelHandler)
	subscribe(location, "MochadEvent", MochadEventHandler)
  } 
  log.debug "Device updated ${deviceName}"
}

 
def MochadEventHandler(evt) {
  log.debug "Mochad event recieved - data: ${evt.data}"	
  def data = parseJson(evt.data)
  processEvent(data)
  return
}

private processEvent(body) {
  log.trace "processEvent Body: ${body}"
  //[protocol:rf, unitcode:6, direction:rx, state:on, housecode:h]
  def deviceString = ""
  def status

  def housecodekey = "housecode"
  if (body.containsKey(housecodekey)) {
    def housecode = body.housecode.toUpperCase()
    def unitcode = body.unitcode
    status = body.state;
    deviceString = "${housecode}-${unitcode}"
    //log.debug "body has housecodekey - status: ${status}"
    updateDevice(deviceString, status)
  }
}

private updateDevice(deviceString, status) {
  //iterate through all child apps and look for state.idX10device
  //compare that with address if it matches return settings.buttonSwitch
  log.debug "updateDevice: Button ${deviceString} ${status} pressed"
  sendLocationEvent(name: "X10RemoteEvent-${deviceString}", value: "updatedX10DeviceStatus", data: ["deviceString": deviceString, 
                    "status": status], source: "DEVICE", isStateChange: true)

}

private sendTelnet(path)
{
  def deviceName = "Mochad-client"
  log.debug "sendTelnet Device ${deviceName}"
  
  def theDeviceNetworkId = getX10DeviceID();
  def theDevice = getDevicebyNetworkId(getX10DeviceID())
  if (theDevice) { // 
	log.debug "The switch already exists ${deviceName}"
    theDevice.deviceNotification(path)
  } 
	
}

private getnodeRedAddress() {
  return settings.nodeRedAddress + ":" + settings.nodeRedPort
}

private String convertIPtoHex(ipAddress) {
  if (!ipAddress) return;
  String hex = ipAddress.tokenize('.').collect {
    String.format('%02x', it.toInteger())
  }.join().toUpperCase()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format('%04x', port.toInteger()).toUpperCase()
  return hexport
}

private getDevicebyNetworkId(deviceNetworkId) {
  return getChildDevices().find {
    d -> d.deviceNetworkId.startsWith((String) deviceNetworkId)
  }
}

def sendStatetoX10(deviceString, state) {
  //deviceString = deviceString.replace("-"," ")
  //def X10code = deviceString.tokenize('-') 
  sendTelnet("${deviceString.replace("-"," ")} ${state}")
}

def getHostHubId() {
  def hub = location.hubs[0]
  return hub.id
}

def getX10DeviceID() {
  if (!state.x10DeviceID) {
    setX10DeviceID()
  }
  return state.x10DeviceID
}

def setX10DeviceID(theDevice) {
  state.x10DeviceID = "Mochad-client"  //"${settings.deviceType}-${settings.deviceHouseCode}${settings.deviceUnitCode}"
  state.deviceString = "Mochad-client" //"${settings.deviceHouseCode}-${settings.deviceUnitCode}"
  if (theDevice) theDevice.deviceNetworkId = state.x10DeviceID
}

private getDevicebyNetworkId(String theDeviceNetworkId) {
  return getChildDevices().find {
    d -> d.deviceNetworkId.startsWith(theDeviceNetworkId)
  }
}