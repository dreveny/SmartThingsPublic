/*
* Author: tguerena and surge919 and dreveny
*
* OpenSprinkler Device Handler Hack
* This is a port of the URI switch with some OpenSprinkler specifics thrown in.
* Ideally OpenSprinkler should be a separate device with each station as a
* child device but this works for now.
*/

preferences {
  section("Internal Access") {
    input "internal_ip", "text", title: "Internal IP", required: true
    input "internal_port", "text", title: "Internal Port (if not 80)", required: false
  }
  section("Sprinkler") {
    input "station", "number", title: "Station number", required: true
    input "on_time", "number", title: "On Time (seconds)", required: false
  }
  section("Cooperation") {
    input "delay", "number", title: "Delay (ticks) before turning on", required: false
  }
}

metadata {
  definition (name: "OpenSprinkler Switch", namespace: "dreveny", author: "Chad Dreveny") {
    capability "Actuator"
    capability "Switch"
    capability "Sensor"
  }

  // simulator metadata
  simulator {
  }

  // UI tile definitions
  tiles {
    standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
      state "off", label: 'Off', action: "switch.on", icon: "st.Outdoor.outdoor12", backgroundColor: "#ffffff", nextState: "on"
        state "on", label: 'On', action: "switch.off", icon: "st.Outdoor.outdoor12", backgroundColor: "#79b821", nextState: "off"
    }
    standardTile("offButton", "device.button", width: 1, height: 1, canChangeIcon: true) {
      state "default", label: 'Force Off', action: "switch.off", icon: "st.Outdoor.outdoor12", backgroundColor: "#ffffff"
    }
    standardTile("onButton", "device.switch", width: 1, height: 1, canChangeIcon: true) {
      state "default", label: 'Force On', action: "switch.on", icon: "st.Outdoor.outdoor12", backgroundColor: "#79b821"
    }
    main "button"
      details (["button","onButton","offButton"])
  }
}

def parse(String description) {
  log.debug(description)
}

def on() {
  def station_time
  if (on_time) {
    station_time = on_time
  } else {
    station_time = defaultOnTime()
  }
  
  if (delay) {
    1.upto(delay, {})
  }
  def result = sendSprinklerCommand(startStationPath(station, station_time))
  sendEvent(name: "switch", value: "on")
  log.debug "Executing ON"
  log.debug result
  runIn(station_time, setSwitchOff)
}

def off() {
  def result = sendSprinklerCommand(stopStationPath(station))
  sendEvent(name: "switch", value: "off")
  log.debug "Executing OFF"
  log.debug result
}

def setSwitchOff() {
  log.debug "Changing switch state to OFF"
  sendEvent(name: "switch", value: "off")
}

// Private Methods
private startStationPath(int station, int seconds) {
  return "/cm?sid=${station}&en=1&t=${seconds}"
}

private stopStationPath(int station) {
  return "/cm?sid=${station}&en=0"
}

private int defaultOnTime() {
  return 10
}

private sendSprinklerCommand(String path) {
  def port
  if (internal_port){
    port = internal_port
  } else {
    port = 80
  }

  def result = new physicalgraph.device.HubAction(
    method: "GET",
    path: path,
    headers: [
      HOST: "${internal_ip}:${port}"
    ]
  )
  sendHubCommand(result)
  return result
}
