#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Wire.h>
#include "DHT.h"

#define TEMP_SENSOR_ID "DS18B20"  // id датчика - отправится в апи
#define PIR_SENSOR_ID "PIR"       // id датчика - отправится в апи
#define ONE_WIRE_BUS 4            // пин для подключения датчика температуры

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);

int pirState = LOW;
char ssid[] = "WiFi-DOM.ru-7102";
char pass[] = "kFyuKfxuA9";
const char* api_host = "http://192.168.0.11:9999";
int calibrationTime = 20;       // Время калибровки датчика (10-60 сек. по даташиту)
long unsigned int lowIn;        // Время, в которое был принят сигнал отсутствия движения(LOW)
long unsigned int pause = 5000; // Пауза, после которой движение считается оконченным
boolean lockLow = true;         // false = значит движение уже обнаружено, true - уже известно, что движения нет
boolean takeLowTime;            // запомнить время начала отсутствия движения
int pirPin = D2;                // пин для PIR датчика движения

void setup()
{
  sensors.begin();
  Serial.begin(115200);
  Serial.println("Setting up. Calibrating sensors");
  pinMode(pirPin, INPUT);

  // ждем калибровки
  for(int i = 0; i < calibrationTime; i++) {
      delay(1000);
  }
  // подключаем сеть
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, pass);
  int i = 0;  

  // Если в течение 2 минуты нет коннекта - все плохо
  while (WiFi.status() != WL_CONNECTED && i++ < 120) {
    Serial.println("Waiting for wifi connection.");
    delay(1000);
  }  
  Serial.println("Setup done");  
}

/**
 * Отправляет значение на апи
 */
void setValueToServer(String value, String sensor_id) {
  HTTPClient http;
  http.begin(api_host);
  http.addHeader("Content-Type", "application/x-www-form-urlencoded");
  String postData = "value="+ value + "&sensor_id=" + sensor_id;
  http.POST(postData);
  http.end();        
}

void loop() {  
  delay(2000);
  sensors.requestTemperatures();
  setValueToServer(String(sensors.getTempCByIndex(0),DEC), TEMP_SENSOR_ID);
  Serial.println(String(sensors.getTempCByIndex(0),DEC));

  //Если обнаружено движение
  if(digitalRead(pirPin) == HIGH) { 
    // Если еще не вывели информацию об обнаружении
    if(lockLow) { 
      lockLow = false;
      delay(50);
    }        
    takeLowTime = true;
    setValueToServer("1", PIR_SENSOR_ID);
  } 

  //Если движения нет       
  if(digitalRead(pirPin) == LOW) {
    if(takeLowTime) {             // Если время окончания движения еще не записано
      lowIn = millis();           // Сохраним время окончания движения
      takeLowTime = false;        // Изменим значения флага, чтобы больше не брать время, пока не будет нового движения
    }    
    if(!lockLow && millis() - lowIn > pause) {  // Если время без движения превышает паузу => движение окончено
      lockLow = true;                           // Изменяем значение флага, чтобы эта часть кода исполнилась лишь раз, до нового движения
      delay(100);
    }
  }
}
