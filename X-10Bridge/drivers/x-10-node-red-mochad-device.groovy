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

def installed() {
    initialize()
}

def updated() {
    initialize() 
}

def initialize() {
	try {
	  mochad_connect()
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
    log.debug "Sending msg = [${msg}]"
	log.debug "current telnet connection value: ${device.currentValue('Telnet')}"
	 
    return new hubitat.device.HubAction(msg, hubitat.device.Protocol.TELNET)
}   

def telnetStatus(String status){
	log.info "telnetStatus- error: ${status}"
	if (status == "receive error: Stream is closed"){
		
		log.error "Telnet connection dropped..."
        sendEvent(name: "Telnet", value: "Disconnected")
		runIn(60, initialize)
	} else {
		sendEvent(name: "Telnet", value: "Connected")
	}
}


def parse(String msg) {
    log.debug "Telnet Response = ${msg}"
	
	sendEvent(name: "MochadEvent", value: msg);	 
	sendLocationEvent(name: "MochadEvent", value: "MochadEventStatus", data: msg, source: "DEVICE", isStateChange: true)
}

def mochad_connect(){
  def ip = "192.168.0.131"
  def port = "1025"
	
  log.debug "Connecting to telnet - IP = ${ip}, Port = ${port.toInteger()}"
  telnetConnect(null, ip, port.toInteger(), null, null)
  sendEvent(name: "Telnet", value: "Connected") 
}