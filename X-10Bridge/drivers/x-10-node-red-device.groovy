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
        definition(name: "X-10 Node Red Device", namespace: "enishoca", author: "Enis Hoca") {
        capability "Switch"
        capability "Switch Level"
    }
}

// parse events into attributes
def parse(String description) {}

def on() {
  log.debug "Executing 'on'"
  sendEvent(name : "switch", value : "off");
  sendEvent(name : "switch", value : "on");
}

def off() {
  log.debug "Executing 'off'"
  sendEvent(name : "switch", value : "on");
  sendEvent(name : "switch", value : "off");
}

def setLevel(val){
    def prev = device.currentValue("level")
    log.info "setLevel $val : prev ${prev}"
    log.debug "current switch value: ${device.currentValue('level')}"
    log.debug "latest switch value: ${device.latestValue('level')}"
   
    // make sure we don't drive switches past allowed values (command will hang device waiting for it to
    // execute. Never commes back)
    if (val < 0){
    	val = 0
    }
    
    if( val > 100){
    	val = 100
    }
    
    if (val == 0){ 
    	sendEvent(name:"level",value:val)
        off()
    	 
    }
    else
    { 
     	on()
    	sendEvent(name:"level",value:val)
    	sendEvent(name:"switch.setLevel",value:val)
    }
}
