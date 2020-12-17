/**
 *  OpenSprinkler Valve
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
metadata {
	definition (name: "OpenSprinkler Station", namespace: "dreveny", author: "Chad Dreveny") {
		capability "Actuator"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Switch"
	}

    attribute "runTime", "number"
    
	command "setRunTime"
	command "triggerRefresh"
	
    simulator {
	}

	tiles(scale: 2) {
		standardTile("state", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${name}', action: "switch.on", icon: "st.valves.water.closed", backgroundColor: "#ffffff", nextState:"open"
            state "on", label: '${name}', action: "switch.off", icon: "st.valves.water.open", backgroundColor: "#00A0DC", nextState:"closed"
		}
        controlTile("duration", "device.runTime", "slider", width: 2, height: 2, inactiveLabel: false, range:"(1..3600)") {
            state "settime", action: "setRunTime"
        }
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "state"
		details(["state", "duration", "refresh"])
	}

	preferences {
		input name: "ip_address_port", type: "text", 
        	title: "IP Address:Port", 
            description: "Enter OpenSprinkler IP Address and optional port", 
            defaultValue: "foo:bar", 
            required: true, 
            displayDuringSetup: true
		input name: "station", type: "number", 
        	title: "Station Number", 
            description: "Enter zero-based station number", 
            defaultValue: 0, 
            required: true, 
            displayDuringSetup: true
	}
}

// parse events into attributes
def parse(String description) {
    log.debug "parse: '${description}'"
    def msg = parseLanMessage(description)

    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)

    log.debug "headerMap: '${headerMap}'"
    log.debug "status: '${status}'" 
    log.debug "json: '${json}'"
    
    if (json != null) {
    	if (json['sn'] != null) {
        	// Status message.
            def station_status = json['sn'][station]
            log.debug "STATION ${station}: ${station_status}"
            if (station_status) {
	            return createEvent(name: 'switch', value: 'on')
            } else {
	            return createEvent(name: 'switch', value: 'off')
            }
        }
    }
}

def installed() {
	doDebug("Executing 'installed'")
    sendEvent(name: "runTime", value: 300)
}

// handle commands
def poll() {
	doDebug("Executing 'poll'")
    hubGetStatus()
}

def refresh() {
	doDebug("Executing 'refresh'")
    hubGetStatus()
}

def setRunTime(int seconds) {
	sendEvent(name: 'runTime', value: seconds, isStateChange: true, displayed: false)
}

def on() {
	doDebug("Executing 'on'")
    def runtime = device.currentValue("runTime")
    sendEvent(name: "switch", value: "on")
    runIn(runtime + 5, triggerRefresh)
    delayBetween([hubGet("/cm?sid=${station}&en=1&t=${runtime}"), hubGetStatus()], delayMs())
}

def off() {
	doDebug("Executing 'off'")
    sendEvent(name: "switch", value: "off") 
    delayBetween([hubGet("/cm?sid=${station}&en=0"), hubGetStatus()], delayMs())
}

def triggerRefresh() {
	sendHubCommand(hubGetStatus())
}

///////////// Private methods
private Integer delayMs() {
	return 1000
}

private hubGetStatus() {
	return hubGet("/js")
}

private hubGet(def apiCommand) {
    doDebug("hubGet -> BEGIN")
    doDebug("hubGet -> apiCommand = $apiCommand (size = ${apiCommand.size()})")

    def port = 80
	if (ip_address_port) {
		def address_port = ip_address_port.tokenize(':')
		if (address_port.size() == 1) {
			state.Host = address_port[0]
            port = 80
		} else {
            state.Host = address_port[0]
            port = address_port[1]
        }
    }
        
    doDebug("hubGet -> Using IP '$state.Host'")

    // Set the Network Device Id
    def hosthex = convertIPtoHex(state.Host).toUpperCase()
    def porthex = convertPortToHex(port).toUpperCase()
    device.deviceNetworkId = "$hosthex:$porthex"
    doDebug("hubGet -> Network Device Id = $device.deviceNetworkId", "info")

    // Set our Headers
    def headers = [:]
    headers.put("HOST", "${state.Host}:${port}")
    doDebug("hubGet -> headers = ${headers.inspect()}")

    // Do the deed
    try {
        def hubAction = new physicalgraph.device.HubAction(
            method: "GET",
            path: apiCommand,
            headers: headers
        )
        doDebug("hubGet -> hubAction = $hubAction")
        hubAction
    }
    catch (Exception e) {
        log.warn "hubGet -> 'HubAction' Exception -> $e ($hubAction)"
    }
}

private String convertIPtoHex(ipAddress) {
    try {
        String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
        doDebug("convertIPtoHex -> IP address passed in is $ipAddress and the converted hex code is $hex", "info")
        return hex
    }
    catch ( Exception e ) {
        doDebug("IP Address is invalid ($ipAddress), Error: $e", "warn")
        return null //Nothing to return
    }
}

private String convertPortToHex(port) {
    try {
        String hexport = port.toString().format( '%04x', port.toInteger() )
        doDebug("convertPortToHex -> Port passed in is $port and the converted hex code is $hexport", "info")
        return hexport
    }
    catch ( Exception e ) {
        doDebug("Port is invalid ($ipAddress), Error: $e", "warn")
        return null //Nothing to return
    }
}

private doDebug(s, dbgType = null) {
    log.debug s
}
