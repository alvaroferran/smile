
#include <Servo.h> 

Servo myservo; 
int degreeOld=90;
int degree=degreeOld;
boolean right=true;
int faceNear=1;
long previousMillis = 0;
long duration=1000;


void setup() { 
	Serial.begin(19200);
	Serial.flush();
	myservo.attach(7);
	myservo.write(degreeOld);
} 


String readString(){
	String inString ="";
	char inChar;
	while(Serial.available()>0){
		inChar =(char) Serial.read();
		inString+=inChar;
	}
	delay(1);
	return inString;
}




void search(){
  if (faceNear==1) {  //First call of function
  	previousMillis=millis();
  	faceNear=0;
  }
  unsigned long currentMillis = millis();
  if(currentMillis - previousMillis > duration) {
  	int steps=1;
  	int limit=30;
  	if(right==true){
  		degree+=steps;
  		myservo.write(degree);
  		if(degree>=90+limit) right=false;
  	}
  	if(right==false){
  		degree-=steps;
  		myservo.write(degree);
  		if(degree<=90-limit) right=true;
  	}
  }
}


void track(String msg){
	degree=degreeOld+msg.toInt();
	myservo.write(degree);
	faceNear=1;
}


void loop() { 
	String incoming="";
	if(Serial.available()) incoming=readString();
	if(incoming=="search") search();
	if(incoming.toInt()){
		track(incoming);
		degreeOld=degree;
		delay(10);
	}
} 
