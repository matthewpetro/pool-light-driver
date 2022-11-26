import groovy.json.JsonSlurper
import groovy.transform.Field

@Field static Map lightEffects = [
    1:'SAm',
    2:'Party',
    3:'Romance',
    4:'Caribbean',
    5:'American',
    6:'California Sunset',
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
        capability 'Light'
        capability 'LightEffects'
        capability 'Refresh'
        command 'setEffectByName', [[name: 'effectName', type: 'STRING', description: 'The name of the effect to set']]
    }

    preferences {
        input name: 'txtEnable', type: 'bool', title: 'Enable descriptionText logging', defaultValue: true
        input name: 'controllerAddress', type: 'text', title: 'Controller IP address'
        input name: 'controllerPort', type: 'number', title: 'Controller port', defaultValue: 80, range: '1..65535'
    }
}

def installed() {
    def le = new groovy.json.JsonBuilder(lightEffects)
    sendEvent(name:'lightEffects', value:le)
}

def refresh() {
    if (txtEnable) log.info 'refreshing'
    try {
        httpGet(getRefreshUrl()) { resp ->
            if (txtEnable) log.debug "response: ${resp.data}"
            def descriptionText = "${device.displayName}, colorMode is EFFECTS"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name:'colorMode', value:'EFFECTS', descriptionText:descriptionText)

            def controllerPower = resp.data.variables.power
            if (controllerPower && !controllerPower.equalsIgnoreCase(device.currentValue('switch'))) {
                descriptionText = "${device.displayName}, setting switch to ${controllerPower}"
                if (txtEnable) log.info "${descriptionText}"
                sendEvent(name:'switch', value:controllerPower, descriptionText:descriptionText)
            }

            def controllerScene = resp.data.variables.scene
            if (controllerScene && !controllerScene.equalsIgnoreCase(device.currentValue('effectName'))) {
                def selectedEffect = lightEffects.find { it.value.equalsIgnoreCase(controllerScene) }
                descriptionText = "${device.displayName}, effect is ${selectedEffect.value}"
                if (txtEnable) log.info "${descriptionText}"
                sendEvent(name:'effectName', value:selectedEffect.value, descriptionText:descriptionText)
            }
        }
    } catch (Exception e) {
        log.error "Error while refreshing: ${e.message}"
    }
}

def on() {
    if (txtEnable) log.info "${device.displayName}, turning on"
    try {
        httpGet(getPowerUrl(POWER_STATE_ON)) { resp ->
            def descriptionText = "${device.displayName}, switch was set to on"
            if (txtEnable) log.info "${descriptionText}"
            if (resp.success) sendEvent(name:'switch', value:'on', descriptionText:descriptionText)
        }
    } catch (Exception e) {
        log.error "Error turning on: ${e.message}"
    }
}

def off() {
    if (txtEnable) log.info "${device.displayName}, turning off"
    try {
        httpGet(getPowerUrl(POWER_STATE_OFF)) { resp ->
            def descriptionText = "${device.displayName}, switch was set to off"
            if (txtEnable) log.info "${descriptionText}"
            if (resp.success) sendEvent(name:'switch', value:'off', descriptionText:descriptionText)
        }
    } catch (Exception e) {
        log.error "Error turning off: ${e.message}"
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
    try {
        httpGet(getEffectUrl(effect.value)) { resp ->
            def descriptionText = "${device.displayName}, effect was set to ${effect.value}"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name:'effectName', value:effect.value, descriptionText:descriptionText)
            descriptionText = "${device.displayName}, colorMode is EFFECTS"
            if (txtEnable) log.info "${descriptionText}"
            sendEvent(name:'colorMode', value:'EFFECTS', descriptionText:descriptionText)
        }
    } catch (Exception e) {
        log.error "Error setting effect: ${e.message}"
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
