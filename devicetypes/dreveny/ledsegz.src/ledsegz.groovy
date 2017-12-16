/**
 *  LED Segz - a custom made LED segment project.
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
	definition (name: "ledsegz", namespace: "dreveny", author: "Chad Dreveny") {
		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
        capability "Configuration"
		capability "Execute"
		capability "Light"
		capability "Media Controller"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
	}


	simulator {
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"http://hosted.lifx.co/smartthings/v1/196xOn.png", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"http://hosted.lifx.co/smartthings/v1/196xOff.png", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'Turning on', action:"switch.off", icon:"http://hosted.lifx.co/smartthings/v1/196xOn.png", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'Turning off', action:"switch.on", icon:"http://hosted.lifx.co/smartthings/v1/196xOff.png", backgroundColor:"#ffffff", nextState:"turningOn"
			}

			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}

			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}

			tileAttribute ("device.model", key: "SECONDARY_CONTROL") {
				attributeState "model", label: '${currentValue}'
			}
		}

		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		valueTile("null", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:''
		}

		controlTile("colorTempSliderControl", "device.colorTemperature", "slider", height: 2, width: 4, inactiveLabel: true, range:"(2700..9000)") {
			state "colorTemp", action:"color temperature.setColorTemperature"
		}

		valueTile("colorTemp", "device.colorTemperature", inactiveLabel: false, decoration: "flat", height: 2, width: 2) {
			state "colorTemp", label: '${currentValue}K'
		}

		main "switch"
		details(["switch", "colorTempSliderControl", "colorTemp", "refresh"])
	}

    preferences {
        input "ip_address", "text", title: "IP Address", description: "Address of the light strip controller", required: true, displayDuringSetup: true
        input "port", "number", title: "Port", description: "Port number of the light strip controller", required: false, displayDuringSetup: true
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'hue' attribute
	// TODO: handle 'saturation' attribute
	// TODO: handle 'color' attribute
	// TODO: handle 'colorTemperature' attribute
	// TODO: handle 'data' attribute
	// TODO: handle 'activities' attribute
	// TODO: handle 'currentActivity' attribute
	// TODO: handle 'switch' attribute
	// TODO: handle 'level' attribute

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
}

// handle commands
def configure() {
    log.trace "Executing 'configure'"
    // this would be for a physical device when it gets a handler assigned to it
    initialize()
}

def installed() {
    log.trace "Executing 'installed'"
    initialize()
}

def updated() {
    log.trace "Executing 'updated'"
    initialize()
}

def setHue(percentage) {
	log.debug "Executing 'setHue(${percentage})'"
	
    Integer newHue = percentage
    Integer currentSaturation = device.currentValue("saturation")

	def hex = colorUtil.hsvToHex(newHue, currentSaturation)
    sendEvent(name: 'colorTemperature', value: null)
	sendEvent(name: 'color', value: hex)
	sendEvent(name: 'hue', value: newHue)
    setStripRgb(colorUtil.hexToRgb(hex))
}

def setSaturation(percentage) {
	log.debug "Executing 'setSaturation(${percentage})'"
	
    Integer newSaturation = percentage
    Integer currentHue = device.currentValue("hue")

	def hex = colorUtil.hsvToHex(currentHue, newSaturation)
    sendEvent(name: 'colorTemperature', value: null)
	sendEvent(name: 'color', value: hex)
	sendEvent(name: 'saturation', value: newSaturation)
    setStripRgb(colorUtil.hexToRgb(hex))
}

def setColor(Map color) {
	log.debug "Executing 'setColor(${color})'"
    
    Integer newHue = color ?.hue ?: 0
    Integer newSaturation = color ?.saturation ?: 0

	log.debug "new HS: ${newHue}, ${newSaturation}"
	def hex = colorUtil.hsvToHex(newHue, newSaturation)
    log.debug "new hex: ${hex}"
    
    sendEvent(name: 'colorTemperature', value: null)
	sendEvent(name: 'color', value: hex)
	sendEvent(name: 'hue', value: newHue)
	sendEvent(name: 'saturation', value: newSaturation)
    setStripRgb(colorUtil.hexToRgb(hex))
}

def setColorTemperature(kelvin) {
	log.debug "Executing 'setColorTemperature(${kelvin})'"
    def hsv = kelvinToHsv(kelvin)    
	def hex = colorUtil.hsvToHex(hsv[0], hsv[1])
    sendEvent(name: 'colorTemperature', value: kelvin, unit: "Kelvin")
	sendEvent(name: 'color', value: hex)
	sendEvent(name: 'hue', value: hsv[0])
	sendEvent(name: 'saturation', value: hsv[1])
    setStripRgb(colorUtil.hexToRgb(hex))
}

def setLevel(percentage, rate=null) {
	log.debug "Executing 'setLevel(${percentage}, ${rate})'"    
    Integer currentHue = device.currentValue("hue")
    Integer currentSaturation = device.currentValue("saturation")
    Integer newLevel = Math.round(percentage * 255.0 / 100.0)
    sendEvent(name: 'level', value: percentage)    
    sendEvent(name: 'switch', value: "on")
    delayBetween(
    	[hubGet("/cmd/brightness([${newLevel}])"), 
         setStripRgb(colorUtil.hexToRgb(colorUtil.hsvToHex(currentHue, currentSaturation)))],
        250)
}

def execute(String command, args) {
	log.debug "Executing 'execute(${command}, ${args})'"
	// TODO: handle 'execute' command
}

def startActivity(String activityId) {
	log.debug "Executing 'startActivity(${activityId})'"
	// TODO: handle 'startActivity' command
}

def refresh() {
	log.debug "Executing 'refresh'"
    def m = colorUtil.getDeclaredMethods()
    for (i in m) {
    	log.debug "${i}"
    }
	// TODO: handle 'refresh' command
}

private List<String> splitEqually(String text, int size) {
    def List ret = new ArrayList()

    for (int start = 0; start < text.length(); start += size) {
        ret.add(text.substring(start, Math.min(text.length(), start + size)));
    }
    return ret;
}

def on() {
	log.debug "Executing 'on'"
    Integer currentHue = device.currentValue("hue")
    Integer currentSaturation = device.currentValue("saturation")
    setStripRgb(colorUtil.hexToRgb(colorUtil.hsvToHex(currentHue, currentSaturation)))
}

def off() {
	log.debug "Executing 'off'"
    sendEvent(name: 'switch', value: "off")
    hubGet("/cmd/off")
}

// Private Methods
private initialize() {
    log.trace "Executing 'initialize'"
    sendEvent(name: 'colorTemperature', value: null)
	sendEvent(name: "hue", value: 0)
    sendEvent(name: "saturation", value: 0)
    sendEvent(name: "color", value: "#FFFFFF")
    sendEvent(name: "level", value: 100)
    sendEvent(name: "switch", value: "off")
}

private setStripRgb(rgb) {
	log.debug "setStripRgb(${rgb})"
    sendEvent(name: 'switch', value: "on")
    hubGet("/cmd/color([${rgb[0]},${rgb[1]},${rgb[2]}])")
}

private rgbToHex(rgb) {
	colorUtil.rgbToHex(rgb[0], rgb[1], rgb[2])
}

private List kelvinToHsv(Double kelvin) {
    def hsv = colorUtil.hexToHsv(rgbToHex(kelvinToRgb(kelvin)))
    [(Integer)hsv[0], (Integer)hsv[1], (Integer)hsv[2]]
}

private List kelvinToRgb(Double kelvin) {
  // http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code
  def tmpKelvin = kelvin / 100.0
  [kelvinRed(tmpKelvin), kelvinGreen(tmpKelvin), kelvinBlue(tmpKelvin)]
}

private Integer kelvinRed(Double tmpKelvin) {
	// tmpKelvin is the kelvin temperature / 100.0
    if (tmpKelvin <= 66) {
    	return 255
    }
    return saturateColor(329.698727446f * ((tmpKelvin - 60) ** -0.1332047592f))
}

private Integer kelvinGreen(Double tmpKelvin) {
	// tmpKelvin is the kelvin temperature / 100.0
    def green
    if (tmpKelvin <= 66) {
        green = 99.4708025861f * Math.log(tmpKelvin) - 161.1195681661f
    } else {
    	green = 288.1221695283f * ((tmpKelvin - 60) ** -0.0755148492f)
    }
    return saturateColor(green)
}

private Integer kelvinBlue(Double tmpKelvin) {
	// tmpKelvin is the kelvin temperature / 100.0
    if (tmpKelvin >= 66) {
        return 255
    }
    return saturateColor(138.5177312231f * Math.log(tmpKelvin - 10) - 305.0447927307f)
}

private Integer saturateColor(value) {
	return Math.min(Math.max(value, 0), 255)
}

private List OBSOLETE_rgbToHsv(r, g, b) {
	return rgbToHsv([r, g, b])
}

private List OBSOLETE_rgbToHsv(rgb) {
	// Based on: https://www.rapidtables.com/convert/color/rgb-to-hsv.html
	def rp = rgb[0] / 255.0
    def gp = rgb[1] / 255.0
    def bp = rgb[2] / 255.0
    def cmax = [rp, gp, bp].max()
    def cmin = [rp, gp, bp].min()
    def delta = cmax - cmin
    
    // Hue calculation.
    def hue
    if (delta == 0) {
    	hue = 0	
    } else if (cmax == rp) {
    	hue = ((gp - bp) / delta).remainder(6)
    } else if (cmax == gp) {
    	hue = ((bp - rp) / delta) + 2
    } else if (cmax == bp) {
    	hue = ((rp - gp) / delta) + 4
	} else {
    	log.error "Unknown hue value."
        hue = 0
    }
    // Above hues are in 60 degree increments.  Normalize to 0..1.
    hue = hue / 6.0
    
    // Saturation calculation.
    def sat
    if (cmax == 0) {
    	sat = 0
    } else {
    	sat = delta / cmax
    }
    
    // Value calculation.
    def val = cmax
    
    return [hue, sat, val]
}

private hubPost(command) {
    hubGet(command, "POST")
}

private hubGet(command, method="GET") {
    log.debug "hubGet -> command = ${command} (size = ${command.size()})"

    def portNum = port ?: 80
    log.debug "hubGet -> Using IP '${ip_address}:${portNum}'"

    // Set the Network Device Id
    def hosthex = convertIPtoHex(ip_address).toUpperCase()
    def porthex = convertPortToHex(portNum).toUpperCase()
    device.deviceNetworkId = "$hosthex:$porthex"
    log.info "hubGet -> Network Device Id = $device.deviceNetworkId"

    // Set our Headers
    def headers = [:]
    headers.put("HOST", "${ip_address}:${portNum}")
    log.debug "hubGet -> headers = ${headers.inspect()}"

    // Do the deed
    try {
        def hubAction = new physicalgraph.device.HubAction(
            method: method,
            path: command,
            headers: headers
        )
        log.debug "hubGet -> hubAction = $hubAction"
        hubAction
    }
    catch (Exception e) {
        log.warn "hubGet -> 'HubAction' Exception -> $e ($hubAction)"
    }
}

private String convertIPtoHex(ipAddress) {
    try {
        String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
        log.info "convertIPtoHex -> IP address passed in is $ipAddress and the converted hex code is $hex"
        return hex
    }
    catch ( Exception e ) {
        log.warn "IP Address is invalid ($ipAddress), Error: $e"
        return null  // Nothing to return
    }
}

private String convertPortToHex(port) {
    try {
        String hexport = port.toString().format( '%04x', port.toInteger() )
        log.info "convertPortToHex -> Port passed in is $port and the converted hex code is $hexport"
        return hexport
    }
    catch ( Exception e ) {
        log.warn "Port is invalid ($ipAddress), Error: $e"
        return null  // Nothing to return
    }
}
