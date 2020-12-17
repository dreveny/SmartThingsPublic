/**
 *  Stupid Christmas Tree Timer
 *
 *  Copyright 2017 Chad Dreveny
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Stupid Christmas Tree Plug",
    namespace: "dreveny",
    author: "Chad Dreveny",
    description: "The Christmas tree is stupid since it has a cheap switch that just interrupts the power.  The power plug needs to be cycled 4 times when it turns on, in order to maintain state (3 light patterns + off are the states).",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png")


preferences {
	section("Control this switch") {
		input "theswitch", "capability.switch", required: true
	}
    section("Behaviour") {
    	input "switchCycles", "number", title: "Cycle the switch this many times", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    atomicState.cyclesRemaining = null
    atomicState.lastState = null
    subscribe(theswitch, "switch", switchHandler)
}

def switchHandler(evt) {
	def remaining = atomicState.cyclesRemaining
    
    // Only act on state changes.  This needs to be done because duplicate actions seem to be received.
    if (evt.value == atomicState.lastState) {
    	log.error "switchHandler: duplicate state transition from ${atomicState.lastState} to ${evt.value}"
    	return
    }
	log.debug "switchHandler: ${evt.value} remaining=${remaining} lastState=${atomicState.lastState}"
    atomicState.lastState = evt.value
    
    if (remaining == null) {
    	// The app is dormant.
	    if (evt.value == "on") {
        	// Start up the cycling process.
	        atomicState.cyclesRemaining = switchCycles - 1
            theswitch.off([delay: getDelay()])
        } else {
        	atomicState.lastAction = null        
		}        
	} else if (remaining > 0) {
    	// The app is currently cycling the switch.
        if (evt.value == "off") {
        	atomicState.cyclesRemaining = remaining - 1
            theswitch.on([delay: getDelay()])
        } else {
            theswitch.off([delay: getDelay()])
       	}
    } else {
    	// Cycling is complete.
        atomicState.cyclesRemaining = null
    }
}

def getDelay() {
	1000
}
