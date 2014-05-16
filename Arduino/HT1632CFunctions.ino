// ----------------------------------------
//  HT1632C functions
// ----------------------------------------


void ht1632c_clear_display() {
  
  digitalWrite(DISPLAY_CS, LOW);  
  ht1632c_send_bits(HT1632C_WRITE, 1 << 2);
  ht1632c_send_bits(0x00, 1 << 6);
  for(int i = 0; i < 32; i++) ht1632c_send_bits(0x00, 1<<7);
  digitalWrite(DISPLAY_CS, HIGH);  
}

void ht1632c_display_buffer(byte* buffer) {

  digitalWrite(DISPLAY_CS, LOW);  
  ht1632c_send_bits(HT1632C_WRITE, 1 << 2);
  ht1632c_send_bits(0x00, 1 << 6);
  for(int i = 0; i < 32; i++) ht1632c_send_bits(buffer[i], 1<<7);
  digitalWrite(DISPLAY_CS, HIGH);   
}

void ht1632c_send_command(byte command) {
  
  digitalWrite(DISPLAY_CS, LOW);  
  ht1632c_send_bits(HT1632C_CMD, 1 << 2);
  ht1632c_send_bits(command, 1 << 7);
  ht1632c_send_bits(0, 1);
  digitalWrite(DISPLAY_CS, HIGH); 
}

void ht1632c_send_bits(byte bits, byte firstbit) {
  
  while(firstbit) {
    digitalWrite(DISPLAY_WR, LOW);
    if (bits & firstbit) digitalWrite(DISPLAY_DATA, HIGH);
    else digitalWrite(DISPLAY_DATA, LOW);
    digitalWrite(DISPLAY_WR, HIGH);
    firstbit >>= 1;
  }
}
