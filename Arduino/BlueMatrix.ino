#include <EEPROM.h>
#include <MAX17043.h>
#include <SoftwareSerial.h>
#include <Wire.h>
#include "aipointe.h"

//----- Defines and variables for softserial -----
SoftwareSerial sSerial(10, 11);

//----- Defines and variables for battery monitor -----
MAX17043 batteryMonitor;

//----- Defines and variables for matrix display -----
#define DISPLAY_CS        3
#define DISPLAY_WR        4
#define DISPLAY_DATA      5
#define HT1632C_READ      B00000110
#define HT1632C_WRITE     B00000101
#define HT1632C_CMD       B00000100
#define HT1632_CMD_SYSON  0x01
#define HT1632_CMD_LEDON  0x03
#define SPACING_SIZE      1          // space between letters
#define SPACE_SIZE        3          // "space" character length
#define BLANK_SIZE        7          // blank columns before restarting the string
#define SCROLLING_SPEED   150        // scrolling speed in ms
#define TEXT_BUFFER_SIZE  100        // max characters in string
#define S_OFF             0
#define S_LETTER          1
#define S_SPACING         2
#define S_SPACE           3
#define S_BLANK           4
byte display_buffer[32];
char text_buffer[TEXT_BUFFER_SIZE];
char display_string[TEXT_BUFFER_SIZE];
byte actual_state;
byte letter_info;
byte letter_length;
unsigned int letter_offset;
byte letter_position;
byte column_position;
byte text_position;
byte buffer_position;
byte space_count;
long previous_millis;


void setup() {

  // serial communication 
  // softserial -> bluetooth
  // serial -> debug
  Serial.begin(9600);
  sSerial.begin(9600);
  Serial.println("BlueMatrixDemo2");
  Serial.println();
  Serial.println("SETUP:");

  // setup battery monitor
  Wire.begin();
  batteryMonitor.reset();
  batteryMonitor.quickStart();
  delay(2000);
  Serial.println("- Battery monitor initialized");  

  // setup FSM and buffers
  for(int i = 0; i < 32; i++) display_buffer[i] = 0x00;
  actual_state = S_OFF;
  previous_millis = 0;
  Serial.println("- Variables initialized"); 
  
  // setup display
  pinMode(DISPLAY_CS, OUTPUT);
  pinMode(DISPLAY_WR, OUTPUT);
  pinMode(DISPLAY_DATA, OUTPUT);
  ht1632c_send_command(HT1632_CMD_SYSON);
  ht1632c_send_command(HT1632_CMD_LEDON);
  ht1632c_clear_display();
  Serial.println("- Display initialized and cleared"); 
  
  // read previous text from EEPROM
  byte eeprom_value;
  int eeprom_address = 0;
  int display_string_position = 0;
  do {
    eeprom_value = EEPROM.read(eeprom_address);
    display_string[display_string_position] = eeprom_value;
    eeprom_address++;
    display_string_position++;
  } while(eeprom_value != '\0');
  Serial.print("- Got text from EEPROM: "); 
  Serial.println(display_string);
  
  Serial.println();
  Serial.println("READY!");
  Serial.println();
}

void loop() {
  
  long current_millis = millis();
  
  // Time to scroll?
  if(current_millis - previous_millis > SCROLLING_SPEED) {
    previous_millis = current_millis;
    scroll();
  }
  
  // New character received from Serial?
  if (sSerial.available() > 0) {
    
    // Read incoming character
    char incoming_char = sSerial.read();
    
    // End of line?
    if(incoming_char == '\n') {
      
      // Add string terminator to rx buffer
      text_buffer[text_position] = '\0';
      
      // parse the command
      parseCommand(text_buffer);
    }
    
    // Normal character? Save it in rx buffer
    else if(text_position < TEXT_BUFFER_SIZE - 2) {
      text_buffer[text_position] = incoming_char;
      text_position++;
    }
    
    // End of buffer reached? Restart from 0
    else {
      text_buffer[0] = incoming_char;
      text_position = 1;
    }
  }  
}

