using System;
using System.Collections.Generic;
using System.IO.Ports;
using System.Linq;
using System.Text;

namespace BlueMatrixWin
{
    class ComManager
    {

        private SerialPort serialPort;
        private string errorMessage;

        // constructor
        public ComManager()
        {
            serialPort = null;
        }

        // returns the list of the available serial ports
        public string[] getSerialPorts()
        {
            return SerialPort.GetPortNames();
        }

        // sets read timeout
        public void setReadTimeout(int timeout)
        {
            serialPort.ReadTimeout = timeout;
        }

        // sets write timeout
        public void setWriteTimeout(int timeout)
        {
            serialPort.WriteTimeout = timeout;
        }

        // opens a connection to a serial port
        public bool openConnection(string portName, int baudRate)
        {
            try
            {
                serialPort = new SerialPort(portName, baudRate, Parity.None, 8, StopBits.One);
                serialPort.Open();
                return true;
            }
            catch (Exception ex)
            {
                errorMessage = ex.Message;
                return false;
            }
        }

        // closes a connection
        public bool disconnect()
        {
            try
            {
                serialPort.Close();
                return true;
            }
            catch (Exception ex)
            {
                errorMessage = ex.Message;
                return false;
            } 
        }

        // sends a line to the serial port
        public bool writeLine(string line)
        {
            try
            {
                serialPort.WriteLine(line);
                return true;
            }
            catch (Exception ex)
            {
                errorMessage = ex.Message;
                return false;
            }
        }

        // sends a line, waits for the response and returns it
        public string writeLineGetResponse(string line)
        {
            string response = null;
            try
            {
                serialPort.WriteLine(line);
                response = serialPort.ReadLine();
            }
            catch (Exception ex)
            {
                errorMessage = ex.Message;
            }
            return(response);
        }


        // returns the last error message
        public string getErrorMessage()
        {
            return errorMessage;
        }
    }
}
