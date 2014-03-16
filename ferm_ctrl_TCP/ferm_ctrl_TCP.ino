
#include <Dhcp.h>
#include <Dns.h>
#include <Ethernet.h>
#include <EthernetClient.h>
#include <EthernetServer.h>


#include <SPI.h>

#include <OneWire.h>

#include <LiquidCrystal.h>



#include <DallasTemperature.h>

#include "M2tk.h"
#include "utility/m2ghlc.h"
#include <DueFlashStorage.h>
DueFlashStorage dueFlashStorage;
#define ONE_WIRE_BUS_PIN 2

struct PrevConfig {
  int8_t sp1;
  int8_t sp2;
  int8_t csp;
};
PrevConfig prevConfig;

OneWire oneWire(ONE_WIRE_BUS_PIN);
DallasTemperature sensors(&oneWire);
DeviceAddress Probe01 = {0x28, 0xE8, 0xDF, 0xD9, 0x04, 0x00, 0x00, 0xAB};
DeviceAddress Probe02 = {0x28, 0x0D, 0x16, 0xDA, 0x04, 0x00, 0x00, 0x30};
DeviceAddress Probe03 = {0x28, 0x82, 0xB2, 0xD9, 0x04, 0x00, 0x00, 0x5A};
DeviceAddress Probe04 = {0x28, 0x5A, 0x2A, 0xDA, 0x04, 0x00, 0x00, 0x3E};
DeviceAddress Probe05 = {0x28, 0x8A, 0x00, 0xDA, 0x04, 0x00, 0x00, 0xBF};
DeviceAddress Probe06 = {0x28, 0x36, 0x10, 0xDA, 0x04, 0x00, 0x00, 0xB9};


//Server vars, including a timeout delay
byte mac[] = { 0x90, 0xA2, 0xDA, 0x0E, 0xF6, 0xEF };
IPAddress serverIP(192, 168, 1, 201);
int serverPort=8888;
EthernetServer server(serverPort);
EthernetClient client; 
char pageAdd[64];
#define delayMillis 30000UL
unsigned long thisMillis = 0;
unsigned long lastMillis = 0;
unsigned long timeout = 5000;

LiquidCrystal lcd(8,9,3,5,6,7);
double sp1=72.6;
double dif1=1.0;
double sp2=68.0;
double dif2=1.0;
double csp=42.0;
double difc=3.0;

//calibration offsets for tc1,2,3
double tc1off=-1.2;
double tc2off=-1.2;
double tc3off=-2.0;

//dummy values for tc1, tc2
 double tc1=35;
 double tc2=35;
 double tc3=35;
 double fv1BathTC=35;
 double fv2BathTC=35;
 double tcAMB=35;
//timer begin
 unsigned long startMillis;
//polling interval
 unsigned long pollint = 1000;
 unsigned long currentTime;
//booleans for pump relays
boolean fv1pump=false;
boolean fv2pump=false;
boolean fvCpump=false;
const int fv1Pin = 14;
const int fv2Pin = 15;
const int fvcPin = 16;
//vars for chill logic
boolean fv1HeatAllow=true;
boolean fv2HeatAllow=true;
boolean fv1Heating=false;
boolean fv2Heating=false;
boolean fv1ChillAllow=true;
boolean fv2ChillAllow=true;
boolean fvCChillAllow=true;
unsigned long fv1ChillTime;
unsigned long fv2ChillTime;
unsigned long fvCChillTime;
unsigned long fv1ChillWaitTime;
unsigned long fv2ChillWaitTime;
unsigned long fvCChillWaitTime;
unsigned int waitTime=60000;
unsigned int fvCwaitTime=300000;
String fv1RelayStr="";
String fv2RelayStr="";
String fvCRelayStr="";
unsigned int chillDuration = 30000;
unsigned int fvCchillDuration = 600000;
  int read_LCD_buttons_original()
{
  uint16_t adc_key_in = analogRead(A0);
  if (adc_key_in > 650 && adc_key_in < 900) return M2_KEY_SELECT;
  if (adc_key_in > 128 && adc_key_in < 200) return M2_KEY_DATA_UP;
  if (adc_key_in > 350 && adc_key_in < 450) return M2_KEY_DATA_DOWN;
  if (adc_key_in > 500 && adc_key_in < 520) return M2_KEY_PREV; //was 420
  if (adc_key_in >= 0 && adc_key_in < 80) return M2_KEY_NEXT;
  return M2_KEY_NONE;
}

uint8_t m2_es_arduino_analog_input(m2_p ep, uint8_t msg)
{
  switch(msg)
  {
    case M2_ES_MSG_GET_KEY:
      return read_LCD_buttons_original();
    case M2_ES_MSG_INIT:
      return 0;
  }
  return 0;
}
//asssigns sp1 to a uint32 for printing (multiplied by 10) 
uint32_t number1 = sp1*10;
uint32_t number2 = dif1*10;
uint32_t number3 = sp2*10;
uint32_t number4 = dif2*10;
uint32_t number5 = csp*10;
uint32_t number6 = difc*10;

uint32_t fv1num = tc1*10;
uint32_t fv2num = tc2*10;
uint32_t fvcnum = tc3*10;

//fv1 and fv2 are updated in tempUpdate routine


//status setup here &status_list_element is the alias
M2_LABEL(el_toplabel_status, NULL, "FV1" );
M2_LABEL(el_toplabel_chill, NULL, "CO" );
M2_LABEL(el_label_status, NULL, "FV2");
M2_U32NUM(el_num_tc1, "r1c3.1", &fv1num);
M2_U32NUM(el_num_tcc, "r1c3.1", &fvcnum);
M2_U32NUM(el_num_tc2, "r1c3.1", &fv2num);
M2_BUTTON(stat_butt, "", "Menu", fn_butok);
M2_LIST(status_list_top)={&el_toplabel_status, &el_num_tc1, &el_toplabel_chill, &el_num_tcc};
M2_HLIST(status_top_hlist, NULL, status_list_top);
M2_LIST(status_list_bot)={&el_label_status, &el_num_tc2, &stat_butt};
M2_HLIST(status_bot_hlist, NULL, status_list_bot);
M2_LIST(status_vert)={&status_top_hlist, &status_bot_hlist};
M2_VLIST(status_list_element, NULL, status_vert);

//SP1 setup here - &sp1_list_element is the alias
M2_LABEL(el_label, NULL, "SP1");
M2_LABEL(el_toplabel, NULL, "Set SP1" );
M2_U32NUM(el_num, "a1c3.1", &number1);
M2_BUTTON(el_ok, "", "ok", fn_ok);
M2_LIST(toplist) = { &el_toplabel};
M2_HLIST(el_list_top, NULL, toplist);
M2_LIST(list) = { &el_label, &el_num, &el_ok };
M2_HLIST(list_element_bot, NULL, list);
M2_LIST(alignlist) = {&el_list_top, &list_element_bot};
M2_VLIST(sp1_list_element, NULL, alignlist);

//Dif1 setup here - &dif1_list_element is the alias
M2_LABEL(el_label_dif1, NULL, "Diff1");
M2_LABEL(el_toplabel_dif1, NULL, "Set Diff 1" );
M2_U32NUM(el_num_dif1, "a1c2.1", &number2);
M2_BUTTON(el_ok_dif1, "", "ok", fn_ok);
M2_LIST(toplist2) = { &el_toplabel_dif1};
M2_HLIST(el_list_top_dif1, NULL, toplist2);
M2_LIST(list2) = { &el_label_dif1, &el_num_dif1, &el_ok };
M2_HLIST(list_element_bot_dif1, NULL, list2);
M2_LIST(alignlist2) = {&el_list_top_dif1, &list_element_bot_dif1};
M2_VLIST(dif1_list_element, NULL, alignlist2);

//SP2 setup here - &sp2_list_element is the alias
M2_LABEL(el_label_sp2, NULL, "SP2");
M2_LABEL(el_toplabel_sp2, NULL, "Set SP2" );
M2_U32NUM(el_num_sp2, "a1c3.1", &number3);
M2_BUTTON(el_ok_sp2, "", "ok", fn_ok_sp2);
M2_LIST(toplist_sp2) = { &el_toplabel_sp2};
M2_HLIST(el_list_top_sp2, NULL, toplist_sp2);
M2_LIST(list_sp2) = { &el_label_sp2, &el_num_sp2, &el_ok_sp2 };
M2_HLIST(list_element_bot_sp2, NULL, list_sp2);
M2_LIST(alignlist_sp2) = {&el_list_top_sp2, &list_element_bot_sp2};
M2_VLIST(sp2_list_element, NULL, alignlist_sp2);

//Dif2 setup here - &dif1_list_element is the alias
M2_LABEL(el_label_dif2, NULL, "Diff2");
M2_LABEL(el_toplabel_dif2, NULL, "Set Diff 2" );
M2_U32NUM(el_num_dif2, "a1c2.1", &number4);
M2_BUTTON(el_ok_dif2, "", "ok", fn_ok_dif2);
M2_LIST(toplist2_dif2) = { &el_toplabel_dif2};
M2_HLIST(el_list_top_dif2, NULL, toplist2_dif2);
M2_LIST(list2_dif2) = { &el_label_dif2, &el_num_dif2, &el_ok_dif2 };
M2_HLIST(list_element_bot_dif2, NULL, list2_dif2);
M2_LIST(alignlist2_dif2) = {&el_list_top_dif2, &list_element_bot_dif2};
M2_VLIST(dif2_list_element, NULL, alignlist2_dif2);

//CoolSP setup here - &csp_list_element is the alias
M2_LABEL(el_label_csp, NULL, "Cool SP");
M2_LABEL(el_toplabel_csp, NULL, "Set Cool SP" );
M2_U32NUM(el_num_csp, "a1c3.1", &number5);
M2_BUTTON(el_ok_csp, "", "ok", fn_ok_csp);
M2_LIST(toplist_csp) = { &el_toplabel_csp};
M2_HLIST(el_list_top_csp, NULL, toplist_csp);
M2_LIST(list_csp) = { &el_label_csp, &el_num_csp, &el_ok_csp };
M2_HLIST(list_element_bot_csp, NULL, list_csp);
M2_LIST(alignlist_csp) = {&el_list_top_csp, &list_element_bot_csp};
M2_VLIST(csp_list_element, NULL, alignlist_csp);

//CoolDif setup here - &difc_list_element is the alias
M2_LABEL(el_label_difc, NULL, "DiffC");
M2_LABEL(el_toplabel_difc, NULL, "Set Cool Diff" );
M2_U32NUM(el_num_difc, "a1c2.1", &number6);
M2_BUTTON(el_ok_difc, "", "ok", fn_ok_difc);
M2_LIST(toplist2_difc) = { &el_toplabel_difc};
M2_HLIST(el_list_top_difc, NULL, toplist2_difc);
M2_LIST(list2_difc) = { &el_label_difc, &el_num_difc, &el_ok_difc };
M2_HLIST(list_element_bot_difc, NULL, list2_difc);
M2_LIST(alignlist2_difc) = {&el_list_top_difc, &list_element_bot_difc};
M2_VLIST(difc_list_element, NULL, alignlist2_difc);


m2_menu_entry menu_data[] = 
{
  { "Status", &status_list_element },
  { "SP1", &sp1_list_element },
  { "Diff 1", &dif1_list_element },
  { "SP2", &sp2_list_element },
  { "Diff 2", &dif2_list_element },
  { "Cool SP", &csp_list_element },
  { "Cool Diff", &difc_list_element },
  { NULL, NULL },
};


// The first visible line and the total number of visible lines.
// Both values are written by M2_2LMENU and read by M2_VSB
uint8_t el_2lme_first = 0;
uint8_t el_2lme_cnt = 7;

M2_2LMENU(el_2lmenu, "l2", &el_2lme_first, &el_2lme_cnt, menu_data, NULL, NULL, NULL);
M2_SPACE(el_space, "W1h1");
M2_LIST(list_2lmenu) = { &el_2lmenu, &el_space };
M2_HLIST(el_hlist, NULL, list_2lmenu);
M2_ALIGN(top_el_menu, "-0|2W64H64", &el_hlist);



M2tk m2(&top_el_menu, m2_es_arduino_analog_input, m2_eh_6bs, m2_gh_lc);

void fn_ok(m2_el_fnarg_p fnarg) {
  m2.clear();
  sp1=number1;
  sp1=sp1/10;
  //division nonsense is restoration of sp1 as xx.x from uint32 xxxx
  m2.setRoot(&top_el_menu);
}
void fn_ok_dif1(m2_el_fnarg_p fnarg) {
  m2.clear();
  dif1=number2;
  dif1=dif1/10;
  m2.setRoot(&top_el_menu);
}
void fn_ok_sp2(m2_el_fnarg_p fnarg) {
  m2.clear();
  sp2=number3;
  sp2=sp2/10;
  m2.setRoot(&top_el_menu);
}
void fn_ok_dif2(m2_el_fnarg_p fnarg) {
  m2.clear();
  dif2=number4;
  dif2=sp1/10;
  m2.setRoot(&top_el_menu);
}
void fn_ok_csp(m2_el_fnarg_p fnarg) {
  m2.clear();
  csp=number1;
  csp=csp/10;
  m2.setRoot(&top_el_menu);

}
void fn_ok_difc(m2_el_fnarg_p fnarg) {
  m2.clear();
  difc=number1;
  difc=difc/10;
  m2.setRoot(&top_el_menu);

}
void fn_butok(m2_el_fnarg_p fnarg) {
  m2.clear();
  m2.setRoot(&top_el_menu);
}
double getTemp(DeviceAddress devAddr){
  //I forgot why I did this, the lib provides a getTempF() fxn...
  double temp=sensors.getTempC(devAddr);
  temp=(temp*1.8) +32.0;
  return temp;
}
  
void time(){
 currentTime=millis();
 //checks to see if millis has reset after 50 days of operation
 //corrects the problem at the cost of an update cycle
 if(startMillis>currentTime){
      startMillis=currentTime;
    }
  if (currentTime-startMillis>pollint){
    sensors.requestTemperatures();
    tc1=getTemp(Probe01)+tc1off;
    tc2=getTemp(Probe02)+tc2off;
    tc3=getTemp(Probe03)+tc3off;
    fv1BathTC=getTemp(Probe04);
    fv2BathTC=getTemp(Probe05);
    tcAMB=getTemp(Probe06);
    fv1num=tc1*10;
    fv2num=tc2*10;
    fvcnum=tc3*10;
    startMillis=currentTime;
    m2.draw();
  } 

if (sp1>tcAMB){
  fv1Heating=true;
}else{
  fv1Heating=false;
}

if (sp2>tcAMB){
  fv2Heating=true;
}else{
  fv2Heating=false;
}
//HotHOT logic
if (fv1Heating){
    if (tc1<sp1){
    if (fv1HeatAllow && !fv1pump && fv1BathTC<sp1){
      //if the pump relay isn't on AND the waiting interval is over AND the bath is too cold
    fv1ChillTime=currentTime;
      digitalWrite(fv1Pin, HIGH);
      fv1HeatAllow=false;
      fv1pump=true;
      //Aaand we're warming this bitch up
    }
  }
  if (fv1BathTC>=sp1&&fv1pump){
    //if the bath is just right, turn the pump off
      digitalWrite(fv1Pin, LOW);
      fv1ChillWaitTime=currentTime;
      fv1ChillTime=currentTime;
      fv1pump=false; 
  }
  if(currentTime-fv1ChillWaitTime>waitTime){
    //if the wait time period has expired, allow the pump to be reactivated if needed
    fv1HeatAllow=true;
  }
}else{
//fv1 chill logic
  if (tc1>sp1){
    if (fv1ChillAllow && !fv1pump && fv1BathTC>sp1&tc3<=sp1){
      //if the pump relay isn't on AND the waiting interval is over AND the bath is too warm
    fv1ChillTime=currentTime;
      digitalWrite(fv1Pin, HIGH);
      fv1ChillAllow=false;
      fv1pump=true;
      //Aaand we're chilling
    }
  }
  if (fv1BathTC<=sp1&&fv1pump&&fv1pump){
    //if the bath is just right, turn the pump off
      digitalWrite(fv1Pin, LOW);
      fv1ChillWaitTime=currentTime;
      fv1ChillTime=currentTime;
      fv1pump=false; 
  }
  if(currentTime-fv1ChillWaitTime>waitTime){
    //if the wait time period has expired, allow the pump to be reactivated if needed
    fv1ChillAllow=true;
  }
}
//fv2 logic

//fv2 HOT HOT SO HOT logic
if (fv2Heating){
    if (tc2<sp2){
    if (fv2HeatAllow && !fv1pump && fv2BathTC<sp2){
      //if the pump relay isn't on AND the waiting interval is over AND the bath is too cold
    fv2ChillTime=currentTime;
      digitalWrite(fv2Pin, HIGH);
      fv2HeatAllow=false;
      fv2pump=true;
      //Aaand we're warming this bitch up
    }
  }
  if (fv2BathTC>=sp2&&fv2pump){
     //if the bath is just right, turn the pump off
      digitalWrite(fv2Pin, LOW);
      fv2ChillWaitTime=currentTime;
      fv2ChillTime=currentTime;
      fv2pump=false; 
  }
  if(currentTime-fv2ChillWaitTime>waitTime){
    //if the wait time period has expired, allow the pump to be reactivated if needed
    fv2HeatAllow=true;
  }
}else{
//else, this shit
  if (tc2>sp2){
    if (fv2ChillAllow & !fv2pump & fv2BathTC>sp2&tc3<=sp2){
      //if the pump relay isn't on AND the waiting interval is over AND the bath is too warm
     //AND the coolant is COLDER than the setpoint desired 
    fv2ChillTime=currentTime;
      digitalWrite(fv2Pin, HIGH);
      fv2ChillAllow=false;
      fv2pump=true;
      //Aaand we're chilling
    }
  }
    if (fv2BathTC<=sp2&&fv2pump){
       //if the bath is just right, turn the pump off
      digitalWrite(fv2Pin, LOW);
      fv2ChillWaitTime=currentTime;
      fv2ChillTime=currentTime;
      fv2pump=false;  
  }
  if(currentTime-fv2ChillWaitTime>waitTime){
    //if the wait time period has expired, allow the pump to be reactivated if needed
    fv2ChillAllow=true;
  }
}
//fvC logic - this is different to accommodate the compressor
if (csp>tcAMB){
  //disable chiller if it's colder outside than the setpoint of the coolant
  fvCChillAllow=false;
}else{
  if (tc3>csp){
    if (fvCChillAllow & !fvCpump){
      //if the pump relay isn't on AND the waiting interval is over 
      fvCChillTime=currentTime;
      digitalWrite(fvcPin, HIGH);
      fvCChillAllow=false;
      fvCpump=true;
      //Aaand we're chilling
    }
  }
    if (currentTime-fvCChillTime>fvCchillDuration&&fvCpump||tc3<=csp-5){
      //if the time the relay has been on has exceeded 10 min, turn the compressor off
      //Or if were 5 deg below the compressor set point
      digitalWrite(fvcPin, LOW);
      fvCChillWaitTime=currentTime;
      fvCChillTime=currentTime;
      fvCpump=false;  
  }
  if(currentTime-fvCChillWaitTime>fvCwaitTime){
    //if the wait time period has expired, allow the pump to be reactivated if needed
    fvCChillAllow=true;
  }
}
//the warm water reservoir is contolled by a seperate arduino running a PID algorithim
//This will change, eventually
}
  

void setup() {
m2_SetLiquidCrystal(&lcd, 16, 2);
startMillis=millis();
Serial.begin(9600);
sensors.begin();
sensors.setResolution(Probe01, 10);
sensors.setResolution(Probe02, 10);
//set relay pins as output
pinMode(fv1Pin, OUTPUT);
pinMode(fv2Pin, OUTPUT);
pinMode(fvcPin, OUTPUT);
//Disable SD SPI
 pinMode(4,OUTPUT);
  digitalWrite(4,HIGH);
Ethernet.begin(mac, serverIP);
  server.begin();
//  Serial.println("Server started");
uint8_t codeRunningForTheFirstTime = dueFlashStorage.read(0); // flash bytes will be 255 at first run
if (codeRunningForTheFirstTime) {
  prevConfig.sp1=sp1;
  prevConfig.sp2=sp2;
  prevConfig.csp=csp;
  byte b2[sizeof(PrevConfig)]; // create byte array to store the struct
  memcpy(b2, &prevConfig, sizeof(PrevConfig)); // copy the struct to the byte array
  dueFlashStorage.write(4, b2, sizeof(PrevConfig)); // write byte array to flash
    // write 0 to address 0 to indicate that it is not the first time running anymore
  dueFlashStorage.write(0, 0); 
  }else{
  byte* b = dueFlashStorage.readAddress(4); // byte array which is read from flash at address 4
  PrevConfig configurationFromFlash; // create a temporary struct
  memcpy(&configurationFromFlash, b, sizeof(PrevConfig)); // copy byte array to temporary struc 
  sp1=configurationFromFlash.sp1;
  sp2=configurationFromFlash.sp2;
  csp=configurationFromFlash.csp;
  }

}

boolean parseMsg(String msg){
     int begin=0;
     int index=0;
     int msgLength=msg.length();
     double tempsp1;
     double tempsp2;
     double tempcsp;
     boolean changed=false;
     
     if (msgLength==0){
       return false;
     }
     int i=0;
     begin=msg.indexOf('&',begin);
     while(index<msgLength){
      int end = msg.indexOf('&', begin+1);
      if (end==-1){
       index=msgLength; 
      }else {
       String value =msg.substring(begin+1, end);
       char buff[value.length()+1];
       value.toCharArray(buff, value.length()+1);
       if (i==0){
         tempsp1=atof(buff);
         if (tempsp1!=sp1){
           sp1=tempsp1;
           changed=true;
           prevConfig.sp1=sp1;
         }
       }else if (i==1){
         tempsp2=atof(buff);
         if (tempsp2!=sp2){
           sp2=tempsp2;
           changed=true;
           prevConfig.sp2=sp2;
         }
       }else if (i==2){
         tempcsp = atof(buff);
         if (tempcsp!=csp){
           csp=tempcsp;
           changed=true;
           prevConfig.csp=csp;
         }
       }
       begin=end;
       i++;
      }
     }
    if (changed){
      byte b2[sizeof(PrevConfig)]; // create byte array to store the struct
      memcpy(b2, &prevConfig, sizeof(PrevConfig)); // copy the struct to the byte array
      dueFlashStorage.write(4, b2, sizeof(PrevConfig)); // write byte array to flash
    }
number1 = sp1*10;
number3 = sp2*10;
number5 = csp*10;
}
 
void checkRelayStates(){
 if (fv1pump){
   fv1RelayStr="*";
 }else{
  fv1RelayStr=""; 
 }
 if (fv2pump){
   fv2RelayStr="*";
 }else{
  fv2RelayStr=""; 
 }
 if (fvCpump){
   fvCRelayStr="*";
 }else{
  fvCRelayStr=""; 
 }
}


void loop() {

EthernetClient client = server.available();//gets client connected to server
if (client){
  Serial.println("Client Connected");
   String clientMsg ="";
   lastMillis=millis();
   thisMillis=lastMillis;
   //code in while loop to timeout after some time
    while (client.connected()&&thisMillis-lastMillis<timeout) {
      Serial.println("1st Loop");
      thisMillis=millis();
      while (client.available()!=0) {
        Serial.println("2nd loop");        
        char c = client.read();
        clientMsg+=c;
        if (c == '\n') {          
          if(clientMsg[2]!=NULL && clientMsg[1]!=NULL){//makes sure msg has atleast 3 chars
          if (clientMsg[0]=='G'&& clientMsg[1]=='E'&& clientMsg[2]=='T'){
            Serial.println("Got Get");
            checkRelayStates();
            String outputStr = String("&&"+(String)sp1+"&"+(String)sp2+"&"+(String)csp+"&"+(String)tc1+fv1RelayStr+"&"+(String)fv1BathTC+"&"+(String)tc2+fv2RelayStr+"&"+(String)fv2BathTC+"&"+(String)tc3+fvCRelayStr+"&"+(String)tcAMB+"&/");
 //         client.print("&&"+(String)sp1+"&"+(String)sp2+"&"+(String)csp+"&"+(String)tc1+fv1RelayStr+"&"+(String)tc2+fv2RelayStr+"&"+(String)tc3+fvCRelayStr+"&/");
           client.println(outputStr);
           delay(1);
           Serial.println(outputStr);
          Serial.println("Sent get");
          outputStr="";
    }else if (clientMsg[0]=='S'&& clientMsg[1]=='E'&& clientMsg[2]=='T'){
          //assumes SET&sp1Value&sp2value&spCvalue&/
          //String setMsg = String(clientMsg);
          boolean setbool = parseMsg(clientMsg);
          if(setbool){
           client.print("");
            }else if(!setbool){
            }
          }
          clientMsg="";
          }     
      }
    }  
client.flush(); 
  }
client.stop();
Serial.println("Client has been stopped");
  }
  m2.checkKey();
  m2.checkKey();
  if ( m2.handleKey() )
    m2.draw();
  m2.checkKey();
  time();

}



