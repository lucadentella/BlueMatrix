// ----------------------------------------
//  Serial communication protocol
// ----------------------------------------
//
// QUERY COMMANDS (first char = ?)
// ?B => get battery status
// ?T => get the text displayed
// ?S => get display status (ON|OFF)
//
// EXEC COMMANDS (first char = !) 
// !T<text> => set the text to be displayed
// !S => toggle display status (ON|OFF)


void parseCommand(char* command) {

  Serial.print("Parsing command -> ");
  Serial.println(command);  
  
  // QUERY COMMANDS
  if(command[0] == '?') {
    
    // get battery status
    if(command[1] == 'B') sendBatteryStatus();

    // get the text displayed
    else if(command[1] == 'T') sendText();
    
    // get display status
    else if(command[1] == 'S') sendDisplayStatus();
  }
  
  // EXEC COMMANDS
  else if(command[0] == '!') {
    
    // set the text to be displayed
    if(command[1] == 'T') setDisplayText(command);
    
    // toggle display status
    else if(command[1] == 'S') toggleDisplayStatus();    
  }
  
  // clear buffer to start receiving a new command
  text_position = 0;
}

