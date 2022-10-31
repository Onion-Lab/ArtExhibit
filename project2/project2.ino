#include "DFRobotDFPlayerMini.h"
#include <SoftwareSerial.h>
#include <Adafruit_NeoPixel.h>
#include <MsTimer2.h>


#define PIN        6
#define NUMPIXELS 24
Adafruit_NeoPixel pixels(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);
#define DELAYVAL 20 // Time (in milliseconds) to pause between pixels


DFRobotDFPlayerMini player;
SoftwareSerial playerSerial(11, 10);
SoftwareSerial bluetoothSerial(3, 2);


int gPlayVolume = 20;
bool gIsStarted = false;
byte gPrevData = '\n';

int MAGNATIC_DETECT_COUNT = 0;
int RIGHT_PWR = 1;
int RIGHT_DIR = 1;

void playLighting()
{
  for(int i=0; i<NUMPIXELS; i++) 
  {
    pixels.setPixelColor(i, pixels.Color(RIGHT_PWR, RIGHT_PWR, RIGHT_PWR));
    pixels.show();
  }

  RIGHT_PWR += RIGHT_DIR;
  if(RIGHT_PWR > 30 || RIGHT_PWR <= 0)
  {
    RIGHT_DIR = RIGHT_DIR*-1;
  }
  Serial.print("Light PWR:");
  Serial.println(RIGHT_PWR);
}

void stopLighting()
{
  for(int i=0; i<NUMPIXELS; i++) 
  {
    pixels.setPixelColor(i, pixels.Color(0, 0, 0));
    pixels.show();
  }
  RIGHT_PWR = 1;
  RIGHT_DIR = 1;
}

void initSound()
{
  playerSerial.begin(9600);
  delay(200);
  player.begin(playerSerial, false); //Use softwareSerial to communicate with mp3.
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
  // pixels.begin();
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
      if(MAGNATIC_DETECT_COUNT <= 35)
      {
        MAGNATIC_DETECT_COUNT += 1;
      }
    }
    else
    {
      if(MAGNATIC_DETECT_COUNT > 0)
      {
        MAGNATIC_DETECT_COUNT -= 1;
      }
    }
    Serial.println(MAGNATIC_DETECT_COUNT);
    if(MAGNATIC_DETECT_COUNT >= 30)
    {
      Serial.println("play!!!");
      playSound();
      // playLighting();
    }
    else
    {
      Serial.println("stop!!!");
      stopSound();
      // stopLighting();
    }
    delay(100);
  }
  else
  {
    stopSound();
    delay(100);
  }
}
