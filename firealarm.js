

const Vonage = require('@vonage/server-sdk')
const vonage = new Vonage({
	apiKey: "944bf506",
	apiSecret: "DmOLlSJeo53d3MhI"
})
const from = "18554224118"
const to = "14087682335"
const text = 'Fire Detected'
const bleno = require("bleno");
const sirenGpio = require('onoff').Gpio;
const sirenOutput = new sirenGpio(17, 'out');
const fireAlarmServiceUUID = "00010000-89BD-43C8-9231-40F6E305F9BB";
const sirenUUID = "00010001-2345-2312-9231-40F6E305F9EE";
const sensorUUID = "00010010-2345-1122-9231-40F6E305F96D";
const descriptorUUID = "00002902-0000-1000-8000-00805F9B34FB"
const sensorGpio = require('onoff').Gpio;
const sensorOutput = new sensorGpio(27, 'in', 'rising',{debounceTimeout: 10});

sensorOutput.watch((berr, value) => {
	sensorValueChanged = true;
	sirenOutput.writeSync(1);
	vonage.message.sendSms(from, to, text);
});

vonage.message.sendSms(from, to, text, (err, responseData) => {
	if(err) {
		
	}
})


sensorValueChanged = false;

class sirenCharacteristic extends bleno.Characteristic {
    constructor(uuid, name) {
        super({
            uuid: uuid,
            properties: ["write"],
            value: null,
            descriptors: [
                new bleno.Descriptor({
                    uuid: "2901",
                    value: name
                  })
            ]
        });

        this.sirenData = 0;
        this.name = name;
    }

    onWriteRequest(data, offset, withoutResponse, callback) {
        try {
            this.sirenData = data.readUInt8();
            if(this.sirenData == 1){
				console.log(`Siren commend sent to fire alarm ${this.name}`);
				console.log(`Value written into characteristic that controls the ${this.name} is 0x01`);
            callback(this.RESULT_SUCCESS);
            callback(this.RESULT_SUCCESS);
				sirenOutput.writeSync(1);
			}
			else{
				console.log(`Silence commend sent to fire alarm ${this.name}`);
				console.log(`Value written into characteristic that controls the ${this.name} is 0x00`);
            callback(this.RESULT_SUCCESS);
				sensorValueChanged = false;
				console.log(`Sensor value is reset`);
				sirenOutput.writeSync(0);
			}
			
            callback(this.RESULT_SUCCESS);

        } catch (err) {
            console.error(err);
            callback(this.RESULT_UNLIKELY_ERROR);
        }
    }
    
}

class sensorCharacteristic extends bleno.Characteristic {
    constructor(uuid) {
        super({
            uuid: uuid,
            properties: ["read", "write", "notify"],
            value: null
        });
        
        
        this.sensorData = 0;
    }


    onReadRequest(offset, callback) {
        try {
			console.log(`${this.name} is now reading`);
			const data = Buffer.alloc(1);
			data.writeUInt8(this.sensorData,0);
            callback(this.RESULT_SUCCESS, data)
        } catch (err) {
            console.error(err);
            callback(this.RESULT_UNLIKELY_ERROR);
        }
    }
  
    onWriteRequest(data, offset, withoutResponse, callback) {
        try {
			
            console.log(`${this.name} is now ${this.argument}`);
            callback(this.RESULT_SUCCESS);

        } catch (err) {
            console.error(err);
            callback(this.RESULT_UNLIKELY_ERROR);
        }
    }
    
    onSubscribe(maxValueSize, updateValueCallback) {
		console.log(`Characteristic for the sensor is now notifying to the user`);
		this.updateValueCallback = updateValueCallback;
	}
	
	notify() {
		if(this.updateValueCallback) {
			const notifyData = Buffer.alloc(1);
			notifyData.writeInt8(10, 0);
			this.updateValueCallback(notifyData);
		}
	}
	
	start() {
		this.handle = setInterval(() => {
			if(sensorValueChanged){
				this.notify();
			}
		}, 1000);
	}
}
	let sensor = new sensorCharacteristic(sensorUUID);
	sensor.start();
	
console.log("Starting bleno...");

bleno.on("stateChange", state => {

    if (state === "poweredOn") {
        
        bleno.startAdvertising("fireAlarm1", [fireAlarmServiceUUID], err => {
            if (err) console.log(err);
        });

    } else {
        console.log("Stopping...");
        bleno.stopAdvertising();
    }        
});

bleno.on("advertisingStart", err => {

    console.log("Configuring services...");
    
    if(err) {
        console.error(err);
        return;
    }

    let siren = new sirenCharacteristic(sirenUUID, "Siren");
	
    let alarm = new bleno.PrimaryService({
        uuid: fireAlarmServiceUUID,
        characteristics: [
            siren,
            sensor
        ]
    });

    bleno.setServices([alarm], err => {
        if(err)
            console.log(err);
        else
            console.log("Services configured");
    });
});


// some diagnostics 
bleno.on("stateChange", state => console.log(`Bleno: Adapter changed state to ${state}`));

bleno.on("advertisingStart", err => console.log("Bleno: advertisingStart"));
bleno.on("advertisingStartError", err => console.log("Bleno: advertisingStartError"));
bleno.on("advertisingStop", err => console.log("Bleno: advertisingStop"));

bleno.on("servicesSet", err => console.log("Bleno: servicesSet"));
bleno.on("servicesSetError", err => console.log("Bleno: servicesSetError"));

bleno.on("accept", clientAddress => console.log(`Bleno: accept ${clientAddress}`));
bleno.on("disconnect", clientAddress => console.log(`Bleno: disconnect ${clientAddress}`));	
