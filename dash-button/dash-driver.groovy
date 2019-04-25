/**
 *  Dash Button Device Driver for Hubitat
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
        definition(name: "Dash Button Device", namespace: "enishoca", author: "Enis Hoca") {
 
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
    //initialize()
}

def updated() {   
    //initialize() 
    if (logEnable) runIn(1800,logsOff)
}
/*
def initialize() {
    if (logEnable) log.debug "initialize"
	try {
	  runIn(2,mochad_connect)
	}
	catch (ex){
	  runIn(60, initialize)
	}
}
*/
 
def parse(description) {
 	if (logEnable) log.debug 'Dash Button Device": ' + description
	// send parent app any LAN communications sent to the Partition. 
	parent.lanResponseHandler(description)

}

 