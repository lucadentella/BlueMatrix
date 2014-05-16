// ----------------------------------------
//  Battery functions
// ----------------------------------------


void sendBatteryStatus() {

  float stateOfCharge = batteryMonitor.getSoC();
  sSerial.print(stateOfCharge);
  sSerial.print('\n');  
}



// ----------------------------------------
//  DIsplay functions
// ----------------------------------------


void sendText() {
  
  sSerial.print(display_string);
  sSerial.print('\n');
}

void sendDisplayStatus() {
  
  if(actual_state == S_OFF) sSerial.print("OFF");
  else sSerial.print("ON");
  sSerial.print('\n');
}

void setDisplayText(char* command) {
  
  // copy the received text in the display_string variable
  strcpy(display_string, command + 2);
  
  // store the received text in the EEPROM
  int eeprom_address = 0;
  int display_string_position = 0;
  char display_string_char;
  do {
    display_string_char = display_string[display_string_position];
    EEPROM.write(eeprom_address, display_string_char);
    eeprom_address++;
    display_string_position++;    
  } while(display_string_char != '\0');
  
  // if display is ON, start displaying the text
  if(actual_state != S_OFF) startDisplayString();
}

void toggleDisplayStatus() {
  
  // if the display is on, clear it 
  // and reset the FSM to S_OFF status
  if(actual_state != S_OFF) {
    ht1632c_clear_display();
    actual_state = S_OFF;
  }
  
  else startDisplayString();
}

void startDisplayString() {
  
  text_position = 0;      
  letter_position = 0;
  column_position = 0;
  buffer_position = 0;
  get_letter_variables();
  actual_state = S_LETTER;
  
  for(int i = 0; i < 32; i++) display_buffer[i] = 0x00;       
}
