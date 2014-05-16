using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Globalization;

namespace BlueMatrixWin
{
    public partial class Form1 : Form
    {
        private ComManager comManager;
        private bool connected;
        private bool displayStatus;

        public Form1()
        {
            InitializeComponent();
            
            comManager = new ComManager();
            foreach (string serialPort in comManager.getSerialPorts())
            {
                cbSerialPorts.Items.Add(serialPort);
            }
            if (cbSerialPorts.Items.Count > 0) cbSerialPorts.SelectedIndex = 0;

            connected = false;            
            updateGui();
        }

        // updates controls based on connected status
        private void updateGui()
        {
            if (connected)
            {
                btConnect.Text = "DISCONNECT";
                lbStatus.Text = "Connected :)";
                cbSerialPorts.Enabled = false;
                btSetText.Enabled = true;
                btDisplayStatus.Enabled = true;
                if (displayStatus) btDisplayStatus.Text = "Display OFF";
                else btDisplayStatus.Text = "Display ON";
            }
            else
            {
                btConnect.Text = "CONNECT";
                lbStatus.Text = "Disconnected :(";
                cbSerialPorts.Enabled = true;
                btSetText.Enabled = false;
                btDisplayStatus.Enabled = false;
            }            
        }

        // get display status
        private bool getDisplayStatus()
        {
            Console.WriteLine("Reading display status");

            string response = comManager.writeLineGetResponse("?S");
            
            if (response == null)
            {
                Console.WriteLine("Non response from the display");
                return false;
            }
            Console.WriteLine("Got response, " + response);

            if (response.Equals("ON"))
            {
                Console.WriteLine("Display is ON");
                displayStatus = true;
            }
            else if (response.Equals("OFF"))
            {
                Console.WriteLine("Display is OFF");
                displayStatus = false;
            }
            else
            {
                Console.WriteLine("Invalid response");
                return false;
            }

            return true;
        }

        private bool getDisplayText()
        {
            string response = comManager.writeLineGetResponse("?T");

            if (response == null) return false;
            tbText.Text = response;

            return true;
        }

        private bool setDisplayText()
        {
            return comManager.writeLine("!T" + tbText.Text);
        }

        private void updateBatteryStatus()
        {
            Console.WriteLine("Updating battery status...");

            string response = comManager.writeLineGetResponse("?B");
            Console.WriteLine("- got response, " + response);
            if (response != null)
            {
                try
                {
                    NumberFormatInfo nfi = new NumberFormatInfo();
                    nfi.CurrencyDecimalSeparator = ".";
                    int batteryStatus = (int)decimal.Parse(response, nfi);
                    Console.WriteLine("- converted into int, " + batteryStatus);
                    pbBatteryStatus.Value = batteryStatus;
                    Console.WriteLine("- progress bar updated");
                }
                catch (Exception)
                {
                    Console.WriteLine(" - unable to convert string to int");
                }

                Console.WriteLine("... ");
            }
            else
            {
                Console.WriteLine(" - empty or wrong response");
            }
            Console.WriteLine("...done!");
        }

        private void btConnect_Click(object sender, EventArgs e)
        {
            if (connected)
            {
                if(comManager.disconnect()) {
                    
                    connected = false;
                    updateGui();
                } 
                else {
                    MessageBox.Show(comManager.getErrorMessage(), "Unable to close the connection", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }

                // stop the battery monitor
                checkBatteryTimer.Enabled = false;
            }
            else
            {
                string serialPortName = (string)cbSerialPorts.SelectedItem;
                Console.WriteLine("Connecting to port " + serialPortName);

                if (comManager.openConnection(serialPortName, 9600))
                {
                    Console.WriteLine("Connection established");

                    comManager.setReadTimeout(500);
                    comManager.setWriteTimeout(500);
                    Console.WriteLine("Read and write timeouts set");

                    if (!getDisplayStatus())
                    {
                        MessageBox.Show(comManager.getErrorMessage(), "Unable to talk with BlueMatrix", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        comManager.disconnect();
                    }
                    else
                    {
                        connected = true;

                        // update the battery status and start the monitor
                        updateBatteryStatus();
                        checkBatteryTimer.Enabled = true;

                        // read the text from the display
                        getDisplayText();

                        updateGui();
                    }
                }
                else MessageBox.Show(comManager.getErrorMessage(), "Unable to open the connection", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }

        }

        private void exitToolStripMenuItem_Click(object sender, EventArgs e)
        {
            Environment.Exit(0);
        }

        private void aboutToolStripMenuItem_Click(object sender, EventArgs e)
        {
            MessageBox.Show("BlueMatrixWin 1.0\nLuca Dentella, 2014", "About", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        private void btSetText_Click(object sender, EventArgs e)
        {
            if(!setDisplayText())
                MessageBox.Show(comManager.getErrorMessage(), "Unable to set the new text", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }

        private void checkBatteryTimer_Tick(object sender, EventArgs e)
        {
            updateBatteryStatus();
        }

        private void btDisplayStatus_Click(object sender, EventArgs e)
        {
            comManager.writeLine("!S");
            displayStatus = !displayStatus;
            updateGui();
        }       
    }
}

