#include "DFRobotDFPlayerMini.h"
#include <SoftwareSerial.h>
#include <Adafruit_NeoPixel.h>
#include <MsTimer2.h>


#define PLAYTIME_MS 4900
#define DETECT_THREASHOLD 450

// NeoPixel
#define PIN        6
#define NUMPIXELS 24
Adafruit_NeoPixel pixels(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

DFRobotDFPlayerMini player;
SoftwareSerial playerSerial(11, 10);
SoftwareSerial bluetoothSerial(3, 2);

int gPlayVolume = 20;
bool gIsStarted = false;
byte gPrevData = '\n';

void playLighting()
{
  for(int j=80;j>0;j--)
  {
    for(int i=0; i<NUMPIXELS; i++) 
    {
      pixels.setPixelColor(i, pixels.Color(j, j, j));
      pixels.show();
    }
  }
}

void stopLighting()
{
  for(int i=0; i<NUMPIXELS; i++) 
  {
    pixels.setPixelColor(i, pixels.Color(0, 0, 0));
    pixels.show();
  }
}

void initSound()
{
  playerSerial.begin(9600);
  delay(200);
  player.begin(playerSerial, false); //Use softwareSerial to communicate with mp3.
  player.volume(0);  //Set volume value. From 0 to 30
  Serial.println("MP3 Module Initialized!");
}

void playSound()
{
  player.volume(gPlayVolume);
  player.play(1);
}

void stopSound()
{
  player.volume(0);
}

void bluetoothInterruptFunction()
{
  byte data;
  if (bluetoothSerial.available())
  {
    data = bluetoothSerial.read();

    if(data != '\n')
    {
      gPrevData = data;
      return;
    }

    // Set Volume
    if(gPrevData >= 0x50 && gPrevData <=0x59)
    {
      gPlayVolume = (gPrevData - 0x50) * 3; // 0 ~ 9
      Serial.print("Bluetooth: Get Volume cmd ");
      Serial.println(gPlayVolume);
    }
    // Set Power: ON
    else if(gPrevData == 'a') // 0x61
    {
      gIsStarted = true;
      Serial.println("Bluetooth: Get Power On cmd");
    }
    // Set Power: OFF
    else if(gPrevData == 'b') // // 0x62
    {
      gIsStarted = false;
      Serial.println("Bluetooth: Get Power Off cmd");
    }
    else
    {
      Serial.println("Bluetooth: Unknown cmd");
    }
  }
}

void setup() {
  Serial.begin(9600);
  pixels.begin();
  delay(200);
  initSound();
  delay(200);
  bluetoothSerial.begin(9600); // 블루투스 시리얼
  delay(200);
  pinMode(A3, INPUT);
  delay(200);

  MsTimer2::set(50, bluetoothInterruptFunction);
  MsTimer2::start();
}

void loop() {
  if(gIsStarted)
  {
    int waterDetect = analogRead(A3);
    Serial.print("Value : ");
    Serial.println(waterDetect);
    if(waterDetect < DETECT_THREASHOLD)
    {
      playSound();
      playLighting();
      stopLighting();


      
      delay(2450);
      
    }
  }
  stopSound();
  stopLighting();
  yield();
  delay(100);
}
