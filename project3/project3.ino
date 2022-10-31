#include "DFRobotDFPlayerMini.h"
#include <SoftwareSerial.h>
#include <Adafruit_NeoPixel.h>
#include <MsTimer2.h>


#define PIN        6
#define NUMPIXELS 45
Adafruit_NeoPixel pixels(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);
#define DELAYVAL 20 // Time (in milliseconds) to pause between pixels


DFRobotDFPlayerMini player;
SoftwareSerial playerSerial(11, 10);
SoftwareSerial bluetoothSerial(3, 2);


int gPlayVolume = 20;
//bool gIsStarted = false;
bool gIsStarted = true;
byte gPrevData = '\n';

void playLighting()
{
  // GROUP 1 : 0 ~ 6 7개
  // GROUP 2 : 7 ~ 22 15개
  // GROUP 3 : 23 ~ 44 23개

  for(int j=0;j<25;j++)
  {
    for(int i=0; i<7; i++) 
    {
      pixels.setPixelColor(i, pixels.Color(70-j, 70-j, 70-j));
      pixels.show();
    }
    delay(35);
  }

  for(int j=0;j<25;j++)
  {
    for(int i=0; i<23; i++) 
    {
      pixels.setPixelColor(i, pixels.Color(45-j, 45-j, 45-j));
      pixels.show();
    }
    delay(15);
  }

  for(int j=0;j<20;j++)
  {
    for(int i=0; i<45; i++) 
    {
      pixels.setPixelColor(i, pixels.Color(20-j, 20-j, 20-j));
      pixels.show();
    }
    delay(35);
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
  delay(200);
  player.volume(0);  //Set volume value. From 0 to 30
  player.play(1);  //Play the first mp3
  player.enableLoop();
  Serial.println("MP3 Module Initialized!");
}

void playSound()
{
  player.volume(gPlayVolume);
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

  pinMode(5, INPUT); // 자석센서
  MsTimer2::set(50, bluetoothInterruptFunction);
  MsTimer2::start();
}

void loop() {
  if(gIsStarted)
  {
    int mag = digitalRead(5);
    if(!mag)
    {
      // 자석감지
      playSound();
      playLighting();
      stopLighting();
      delay(1000);
    }
    else
    {
      stopSound();
      stopLighting();
    }
    delay(100);
    yield;
  }
  else
  {
    stopSound();
    stopLighting();
    delay(100);
    yield;
  }
  delay(100);
  yield;
}
