import groovy.transform.Field

@Field static Map lightEffects = [
    1:'SAm',
    2:'Party',
    3:'Romance',
    4:'Caribbean',
    5:'American',
    6:'California',
    7:'Royal',
    8:'Blue',
    9:'Green',
    10:'Red',
    11:'White',
    12:'Magenta'
]

@Field static String POWER_STATE_ON = 'on'
@Field static String POWER_STATE_OFF = 'off'

metadata {
    definition(name: 'Pool Light', namespace: 'Petro', author: 'Matthew Petro') {
        capability 'ColorMode'
        capability 'Light'
        capability 'LightEffects'
        capability 'Refresh'
        capability 'Switch'
        command 'setEffectByName', [[name: 'effectName', type: 'STRING', description: 'The name of the effect to set']]
    }

    preferences {
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', defaultValue: true
        input name: 'refreshInterval', type: 'number', title: 'Number of minutes between automatic refreshes of device state. 0 means no automatic refresh.', defaultValue: 60, range: '0..1440'
        input name: 'controllerAddress', type: 'text', title: 'Controller IP address'
        input name: 'controllerPort', type: 'number', title: 'Controller port', defaultValue: 80, range: '1..65535'
    }
}

def installed() {
    if (txtEnable) log.debug "${device.displayName} installed"
    def le = new groovy.json.JsonBuilder(lightEffects)
    sendEvent(name:'lightEffects', value:le)
    initialize()
}

def uninstalled() {
    if (txtEnable) log.debug "${device.displayName} uninstalled"
    unschedule('refresh')
}

def updated() {
    if (txtEnable) log.debug "${device.displayName} updated"
    initialize()
}

def initialize() {
    pingController()
}

def pingController() {
    refresh()
    if (refreshInterval > 0) {
        runIn(refreshInterval * 60, 'pingController', [overwrite: true])
    }
}

def refresh() {
    if (txtEnable) log.info "${device.displayName}, refreshing"
    asynchttpGet('handleRefresh', [uri: getRefreshUrl(), timeout: 5])
}

def handleRefresh(response, data) {
    if (!response.error) {
        if (txtEnable) log.info "${device.displayName}, received refreshed data from controller"
        if (!device.currentValue('colorMode') || !device.currentValue('colorMode').equalsIgnoreCase('EFFECTS')) {
            def descriptionText = "${device.displayName}, setting colorMode to EFFECTS"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name:'colorMode', value:'EFFECTS', descriptionText:descriptionText)
        }

        def controllerPower = response.json.variables.power
        if (controllerPower && !controllerPower.equalsIgnoreCase(device.currentValue('switch'))) {
            descriptionText = "${device.displayName}, setting switch to ${controllerPower}"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name:'switch', value:controllerPower, descriptionText:descriptionText)
        }

        def controllerScene = response.json.variables.scene
        if (controllerScene && !controllerScene.equalsIgnoreCase(device.currentValue('effectName'))) {
            def selectedEffect = lightEffects.find { it.value.equalsIgnoreCase(controllerScene) }
            if (null != selectedEffect) {
                descriptionText = "${device.displayName}, effect is ${selectedEffect.value}"
                if (txtEnable) log.info "${descriptionText}"
                sendEvent(name:'effectName', value:selectedEffect.value, descriptionText:descriptionText)
            }
        }
    } else {
        log.error "Error refreshing: ${response.status} ${response.errorMessage}"
    }
}

def on() {
    if (txtEnable) log.info "${device.displayName}, turning on"
    asynchttpGet('handlePowerChange', [uri: getPowerUrl(POWER_STATE_ON), timeout: 5])
}

def off() {
    if (txtEnable) log.info "${device.displayName}, turning off"
    asynchttpGet('handlePowerChange', [uri: getPowerUrl(POWER_STATE_OFF), timeout: 5])
}

def handlePowerChange(response, data) {
    if (!response.error) {
        String powerState = response.json.return_value == 1 ? POWER_STATE_ON : POWER_STATE_OFF
        def descriptionText = "${device.displayName}, switch was set to ${powerState}"
        if (txtEnable) log.info "${descriptionText}"
        sendEvent(name:'switch', value:"${powerState}", descriptionText:descriptionText)
    } else {
        log.error "Error setting power state: ${response.status} ${response.errorMessage}"
    }
}

def setEffectByName(String effect) {
    def selectedEffect = lightEffects.find { it.value.equalsIgnoreCase(effect) }
    if (selectedEffect) setSelectedEffect(selectedEffect)
}

def setEffect(id) {
    def selectedEffect = lightEffects.find { it.key == id }
    if (selectedEffect) setSelectedEffect(selectedEffect)
}

def setSelectedEffect(Map.Entry effect) {
    if (txtEnable) log.info "${device.displayName}, setting effect to ${effect.value}"
    asynchttpGet('handleEffectChange', [uri: getEffectUrl(effect.value), timeout: 20])
}

def handleEffectChange(response, data) {
    if (!response.error) {
        String effectName = lightEffects.find { it.key == response.json.return_value }.value
        if (!effectName) {
            log.error "Error setting effect! ID ${response.json.return_value} not found in lightEffects map."
            return
        }
        def descriptionText = "${device.displayName}, effect was set to ${effectName}"
        if (txtEnable) log.info "${descriptionText}"
        sendEvent(name:'effectName', value:effectName, descriptionText:descriptionText)
        if (!device.currentValue('colorMode') || !device.currentValue('colorMode').equalsIgnoreCase('EFFECTS')) {
            descriptionText = "${device.displayName}, setting colorMode to EFFECTS"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name:'colorMode', value:'EFFECTS', descriptionText:descriptionText)
        }
        if (!device.currentValue('switch').equalsIgnoreCase(POWER_STATE_ON)) {
            descriptionText = "${device.displayName}, setting switch to ${POWER_STATE_ON}"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name:'switch', value:POWER_STATE_ON, descriptionText:descriptionText)
        }
    } else {
        log.error "Error setting effect: ${response.status} ${response.errorMessage}"
    }
}

def setNextEffect() {
    def currentEffectId = lightEffects.find { it.value.equalsIgnoreCase(device.currentValue('effectName')) }.key
    def nextEffectId
    if (null == currentEffectId || currentEffectId >= lightEffects.size()) {
        nextEffectId = 1
    } else {
        nextEffectId = currentEffectId + 1
    }
    setEffect(nextEffectId)
}

def setPreviousEffect() {
    def currentEffectId = lightEffects.find { it.value.equalsIgnoreCase(device.currentValue('effectName')) }.key
    def previousEffectId
    if (null == currentEffectId || currentEffectId <= 1) {
        previousEffectId = lightEffects.size()
    } else {
        previousEffectId = currentEffectId - 1
    }
    setEffect(previousEffectId)
}

def getPowerUrl(String state) {
    return "http://${controllerAddress}:${controllerPort}/power?params=${state}"
}

def getRefreshUrl() {
    return "http://${controllerAddress}:${controllerPort}/"
}

def getEffectUrl(String effectName) {
    return "http://${controllerAddress}:${controllerPort}/scene?params=${effectName}"
}
