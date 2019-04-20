/**
 *  X-10 Node Red Bridge Device Handler
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
 
metadata {
        definition(name: "X-10 Mochad Device", namespace: "enishoca", author: "Enis Hoca") {
        capability "Notification" 
        capability "Telnet"         
        attribute "Telnet", ""
        attribute "MochadEvent", ""
    }
}

preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}
 
def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    if (logEnable) log.debug "installed"
    initialize()
}

def updated() {   
    initialize() 
    if (logEnable) runIn(1800,logsOff)
}

def initialize() {
    if (logEnable) log.debug "initialize"
	try {
	  runIn(2,mochad_connect)
	}
	catch (ex){
	  runIn(60, initialize)
	}
}

def deviceNotification(String text)
{
	sendMsg(text)
}

def sendMsg(String msg) {
  if (logEnable) log.debug "Sending msg = [${msg}]"
  return new hubitat.device.HubAction(msg, hubitat.device.Protocol.TELNET)
}   

def telnetStatus(String status){
	if (logEnable) log.debug "telnetStatus- error: ${status}"
	if (status == "receive error: Stream is closed"){
		
		log.error "Telnet connection dropped..."
        sendEvent(name: "Telnet", value: "Disconnected")
		runIn(60, initialize)
	} else {
		sendEvent(name: "Telnet", value: "Connected")
	}
}

def parse(String msg) {
  if (logEnable) log.debug "Telnet Response = ${msg}"
	
	sendEvent(name: "MochadEvent", value: msg);	 
	sendLocationEvent(name: "MochadEvent", value: "MochadEventStatus", data: msg, source: "DEVICE", isStateChange: true)
}

def mochad_connect(){
  def address = device.deviceNetworkId.tokenize(":")
  def ip =   address[0]
  def port = address[1]

  if (logEnable) log.debug "Connecting to telnet - IP = ${ip}, Port = ${port}"
  telnetConnect(null, ip, port.toInteger(), null, null)
  sendEvent(name: "Telnet", value: "Connected") 
}