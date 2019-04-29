/**
 *  Hubitat App: Amazon Dash Button
 *
 *  Author: redloro@gmail.com
 *  Adapted to Hubitat by enishoca@outlook.com
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
  name: "Dash Button",
  namespace: "enishoca",
  author: "enishoca@outlook.com",
  description: "Amazon Dash Button Hubitat App",
  category: "My Apps",
  iconUrl: "https://raw.githubusercontent.com/redloro/smartthings/master/images/dash-button.png",
  iconX2Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/dash-button.png",
  iconX3Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/dash-button.png",
  singleInstance: true
)

preferences {
  page(name: "pageMain")
  page(name: "buttonSettings")
  page(name: "discoveryPage")
  page(name: "discoveryWaitPage")
  
}

def pageMain() {
  
  dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {

    section("Node Proxy") {
      input "proxyAddress", "text", title: "Proxy Address", description: "(ie. 192.168.1.10)", required: true
      input "proxyPort", "text", title: "Proxy Port", description: "(ie. 8080)", required: true, defaultValue: "8080"
      input "authCode", "password", title: "Auth Code", description: "", required: true, defaultValue: "secret-key"
      input "macAddr", "text", title: "MacAddr of Proxy Server", description: "", required: true, defaultValue: "ABCA1234ABA"

    }

    if (state.buttonCount && !(this."buttonMAC${state.buttonCount-1}")) {
      ifDebug("Delete misconfigured button")
      state.buttonCount = state.buttonCount - 1
    }

    state.buttonCount = (!state.buttonCount) ? 0 : state.buttonCount;
    ifDebug("Button count: ${state.buttonCount}")

    section(title:"Buttons") {
      for (def i = 0; i < state.buttonCount; i++) {
        href "buttonSettings", title: this."buttonName${i}"+ "  " +this."buttonMAC${i}", description: "Controls "+ this."buttonSwitch${i}", required: false, page: "buttonSettings", params: [num: i]
      }
      href "discoveryPage", title: "Add New Button", description: "Tap here to add a new button", image: "http://cdn.device-icons.smartthings.com/thermostat/thermostat-up-icn.png", 
           required: false, page: "discoveryPage", params: [num: state.buttonCount, new: true]
    }

    section("") {
       input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }

  }
}


def buttonSettings(params) {
  params.num = (int)params.num;
  
  if (params.new) {
    ifDebug("Adding button ${params.num}")
    state.buttonCount = params.num + 1;
  }

  dynamicPage(name: "buttonSettings", title: "Button Settings", install: false, uninstall: false) {
    if (params.new) {
        section() {
            paragraph "Found New Button MAC= " + state.lastMAC
        }
    } else {
           section() {
           paragraph "Editing Existing Button"
    	}
    }
    section() {
	    input "buttonName${params.num}", "text", title: "Button Name", description: "Hallway", required: false
      input "buttonMAC${params.num}", "text", title: "Button MAC Address", description: "(ie. aa:bb:cc:dd:ee:f1)", required: false
      input "buttonSwitch${params.num}", "capability.switch", title: "Control Switch", required: false, multiple: true
    }
  }
	//log.debug "Button - " + this."buttonName${params.num}"+" - MAC " + this."buttonMAC${params.num}" + " - controls "+ this."buttonSwitch${params.num}"

}

def discoveryPage(params) {
  state.inDiscovery = true
  return discoveryWaitPage(params) 
}

def discoveryWaitPage(params) { 
  params.num = (int)params.num;
   
  
  if (!state.inDiscovery) {
    def oldButton =-1;
     
    for (def i = 0; i < state.buttonCount; i++) {
      if (this."buttonMAC${i}".toUpperCase() == state.lastMAC) {
	       oldButton = i;
         break;
      }
      log.debug "i = $i"
    }
    if (oldButton >= 0) { 
        def newparams 
        params =  [num: oldButton] //= [num: oldButton]newparams 
    }
    else {
      app.updateSetting("buttonMAC${params.num}",[value:state.lastMAC,type:"String"])
    }
    
    return buttonSettings(params) 
  } 
  else { 
    return dynamicPage(name: "discoveryWaitPage", title: "", refreshInterval: 5, install: true, uninstall: true) {
      section(title: "Press a Dash button now to add or edit it....\n") {
         paragraph title: "Device Discovery Mode", required: true,"Looking for Dash button...\n "               
      }  
    }
  }
}

def installed() {
  	updated()
}

def subscribeToEvents() {
  subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def uninstalled() {
 	removeChildDevices()
}

def updated() {
  unsubscribe()
  removeChildDevices()
  subscribeToEvents()
  updateButtons()
	if (logEnable) runIn(1800,logsOff)
}

def logsOff(){
    log.warn "Dashbutton:debug logging disabled..."
    app.updateSetting("logEnable",[value:"false",type:"bool"])
}

def updateButtons() {
  def buttons = [:]
  for (def i = 0; i < state.buttonCount; i++) {
    if (this."buttonMAC${i}" && this."buttonSwitch${i}") {
	  this."buttonMAC${i}" = this."buttonMAC${i}".toUpperCase()
      buttons.put(this."buttonMAC${i}", "${i}")
      //log.debug " ${i} MAC " + this."buttonMAC${i}" + " - controls "+ this."buttonSwitch${i}"
    }
  }

  state.buttons = buttons
  ifDebug(" Set: ${buttons}")

  // Create child device to handle events
  def DNI=macAddr.replace(":","").toUpperCase()
  ifDebug("Creating Dash Child device")
  addChildDevice("enishoca", "Dash Button Device", DNI)
  state.installed = true

  // subscribe to callback/notifications from STNP
  sendCommand('/subscribe/'+getNotifyAddress())

  // run discover
  runIn(5, discoverButtons)
}

def discoverButtons() {
  sendCommand('/plugins/dash/discover/30')
}


def lanResponseHandler(fromChildDev) {
 
	ifDebug("lanResponseHandler recieved from Child $fromChildDev")
	try {
    	def parsedEvent = parseLanMessage(fromChildDev).json
		def description = parsedEvent?.description
		def map = parseLanMessage(fromChildDev)
        ifDebug(" map ${map}")
  		if (map.headers.'stnp-plugin' != 'dash') {
      		return
  		}
  		processEvent(parsedEvent)
	} catch(MissingMethodException) {
		// these are events with description: null and data: null, so we'll just pass.
		pass
	}
}
 
private sendCommand(path) {
  ifDebug("Send command: ${path}")

  if (settings.proxyAddress.length() == 0 ||
    settings.proxyPort.length() == 0) {
    log.error "Node Proxy configuration not set!"
    return
  }

  def host = getProxyAddress()
  def headers = [:]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/json")
  headers.put("stnp-auth", settings.authCode)

  def hubAction = new hubitat.device.HubAction(
      method: "GET",
      path: path,
      headers: headers
  )
  sendHubCommand(hubAction)
}

private processEvent(evt) {
  ifDebug("Running Process Event ${evt}")
  if (evt.status == "active") {
    updateDevice(evt)
  }
}

private updateDevice(evt) {
  ifDebug("updateDevice: ${evt}")
  //send("A button [${evt.address}] has been pressed")
  state.lastMAC = evt.address.toUpperCase()
  def devices = getDevices(state.lastMAC)
  if (state.inDiscovery)
  {
    log.info "Discovered $state.lastMAC"
    state.inDiscovery = false
    return
  }
  def deviceOn = false
  for (device in devices) {
    if (device.currentSwitch == "on") {
      deviceOn = true
    }
  }
  ifDebug("deviceOn flag  - ${deviceOn}")
  
  if (deviceOn) {
    log.info ("Turning off Devices ${devices}")    
    devices.off()
  } else {
    log.info ("Turning on Devices ${devices}")  
    devices.on()
  }
}

private getDevices(address) {
  ifDebug("state.buttons[ ${state.buttons}")
  return this."buttonSwitch${state.buttons[address]}"
}

private removeChildDevices() {
  getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

private getProxyAddress() {
	return settings.proxyAddress + ":" + settings.proxyPort
}

private getNotifyAddress() {
  // only support single hub.
  def hub = location.hubs[0] 
  ifDebug("Hubitat IP: ${hub.getDataValue("localIP")}")
  ifDebug("Hubitat LAN Port: ${hub.getDataValue("localSrvPortTCP")}")
  return hub.getDataValue("localIP") + ":" + hub.getDataValue("localSrvPortTCP")
}

private String convertIPtoHex(ipAddress) {
  if (!ipAddress) return;
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() ).toUpperCase()
  return hexport
}

private ifDebug(msg) {  
    if (logEnable)  log.debug 'Dash Button: ' + msg  
}