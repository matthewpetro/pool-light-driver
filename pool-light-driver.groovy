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

metadata {
    definition (name: "Pool Light", namespace: "Petro", author: "Matthew Petro") {
        capability "Light"
        capability "LightEffects"
        command "setEffectByName", [[name: "effectName", type: "STRING", description: "The name of the effect to set"]]
    }

     preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "controllerAddress", type: "text", title: "Controller IP address"
        input name: "controllerPort", type: "number", title: "Controller port", defaultValue: 80, range: "1..65535"
    }
}

def installed() {
    def le = new groovy.json.JsonBuilder(lightEffects)
    sendEvent(name:'lightEffects', value:le)
}

def on() {
    sendEvent(name:'switch', value:'on')
    // send device event here
}

def off() {
    sendEvent(name:'switch', value:'off')
    // send device event here
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
    def descriptionText = "${device.displayName}, effect was set to ${effect}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:'effectName', value:effect, descriptionText:descriptionText)
    descriptionText = "${device.displayName}, colorMode is EFFECTS"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name:'colorMode', value:'EFFECTS', descriptionText:descriptionText)
    state.currentEffectId = effect.key
    // send device event here
}

def setNextEffect() {
    def nextEffectId
    if (null == state.currentEffectId || state.currentEffectId >= lightEffects.size()) {
       nextEffectId = 1
    } else {
       nextEffectId = state.currentEffectId + 1
    }
    setEffect(nextEffectId)
}

def setPreviousEffect() {
    def previousEffectId
    if (null == state.currentEffectId || state.currentEffectId <= 1) {
       previousEffectId = lightEffects.size()
    } else {
       previousEffectId = state.currentEffectId - 1
    }
    setEffect(previousEffectId)
}
