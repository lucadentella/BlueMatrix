// ---------------------------------------
//  Rolling functions
// ----------------------------------------


void get_letter_variables() {
  
  // Get letter information from font descriptor
  letter_info = display_string[letter_position] - 33;
  letter_length = aipoint_info[letter_info].length;
  letter_offset = aipoint_info[letter_info].offset;
}

void scroll() {
  
  byte new_byte;
  
  switch(actual_state) {
    
    case S_OFF:
      return;
    
    case S_LETTER:
      
      // End of string reached?
      if(display_string[letter_position] == '\0') {
        new_byte = 0x00;
        column_position = 1;
        actual_state = S_BLANK;
        break;
      }
      
      // Character to be displayed is space?
      if(display_string[letter_position] == ' ') {
        new_byte = 0x00;
        column_position = 1;
        actual_state = S_SPACE;
        break;
      }
      
      new_byte = pgm_read_byte_near(aipointe_font + letter_offset + column_position);
      column_position++;
      if(column_position == letter_length) {
        column_position = 0;
        actual_state = S_SPACING;
      }
      break;

    // End of character reached? Send space
    case S_SPACING:
      new_byte = 0x00;
      column_position++;
      if(column_position == SPACING_SIZE)  {
        column_position = 0;
        letter_position++;
        get_letter_variables();
        actual_state = S_LETTER;
      }
      break;

    // Send "space" character
    case S_SPACE:
      new_byte = 0x00;
      column_position++;
      if(column_position == SPACE_SIZE)  {
        column_position = 0;
        letter_position++;
        get_letter_variables();
        actual_state = S_LETTER;
      }
      break;
    
    // Send "blank" before the next string  
    case S_BLANK:
    
      new_byte = 0x00;
      column_position++;
      if(column_position == BLANK_SIZE) {
        //prepareText();
        letter_position = 0;
        column_position = 0;        
        get_letter_variables();
        actual_state = S_LETTER;
      }
      break;    
  }
  
  updateDisplay(new_byte);
}

void updateDisplay(byte new_byte) {
  
  display_buffer[buffer_position] = new_byte;
  buffer_position = (buffer_position + 1) % 32;


  digitalWrite(DISPLAY_CS, LOW);  
  ht1632c_send_bits(HT1632C_WRITE, 1 << 2);
  ht1632c_send_bits(0x00, 1 << 6);

  for(int i = 0; i < 32; i++) {
    ht1632c_send_bits(display_buffer[(i + buffer_position) % 32], 1<<7);  
  }

  digitalWrite(DISPLAY_CS, HIGH);  
}

