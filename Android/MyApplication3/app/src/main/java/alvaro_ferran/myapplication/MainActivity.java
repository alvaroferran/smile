package alvaro_ferran.myapplication;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends ActionBarActivity implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;

    private static final String TAG = "bluetooth1"; //solo para log-> borrar
    public String sendToArduino;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private static String address = "98:D3:31:B2:DB:03"; // MAC-address of Bluetooth module
    //private static String address = "98:D3:31:B2:DC:4E"; // MAC-address of Bluetooth module
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    int screenWidth;
    int facesFound=0;
    int facesFoundOld=1;
    private ImageView myImage, blackBackground;
    TextToSpeech ttobj;
    int screenOrientation;



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies(){
        try{
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_APPEND);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // And we are ready to go
        openCvCameraView.enableView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


      //  getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        //openCvCameraView = new JavaCameraView(this, -1);//original ->default
        //openCvCameraView = new JavaCameraView(this, 0);//back
       // openCvCameraView = new JavaCameraView(this, 1); //front


        //setContentView(openCvCameraView); //ORIGINAL -> MUESTRA IMAGEN DE CAMARA
        setContentView(R.layout.activity_main);
        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        openCvCameraView.setCvCameraViewListener(this);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();  //pregunta si encender el bluetooth

        myImage = (ImageView) findViewById(R.id.imageView2);
        blackBackground = (ImageView) findViewById(R.id.imageView);
        ttobj=new TextToSpeech(getApplicationContext(),
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR){
                            ttobj.setLanguage(Locale.UK);
                        }
                    }
                });

        screenOrientation=getResources().getConfiguration().orientation;




    }




    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);


    }

    @Override
    public void onCameraViewStopped() { }






    @Override
    public Mat onCameraFrame(Mat aInputFrame) {


       //(FUNCIONA HORIZONTAL)
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);
        MatOfRect faces = new MatOfRect();


/*
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);
        Mat rotatedGrayscaleImage = grayscaleImage.clone();
        Core.transpose(grayscaleImage, rotatedGrayscaleImage);
        Core.flip(rotatedGrayscaleImage, rotatedGrayscaleImage, -1);

        MatOfRect faces = new MatOfRect();
*/

        // Use the classifier to detect faces
        if (cascadeClassifier != null) {
          // cascadeClassifier.detectMultiScale(rotatedGrayscaleImage, faces, 1.1, 2, 2,
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2, //(original)
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        double maxArea=0;
        int maxAreaIndex=0;
        for (int i = 0; i <facesArray.length; i++) {
            if(facesArray[i].area() > maxArea) { //find largest rectangle
                maxArea = facesArray[i].area();
                maxAreaIndex=i;
            }
        }

        if(facesArray.length>0) {
            Core.rectangle(aInputFrame, facesArray[maxAreaIndex].tl(), facesArray[maxAreaIndex].br(), new Scalar(0, 255, 0, 255), 3);
            checkCoordiates(facesArray[maxAreaIndex].x, facesArray[maxAreaIndex].y, facesArray[maxAreaIndex].width,facesArray[maxAreaIndex].height);
            updateUI("found");
            facesFound=1;
        }
        else {
            sendData("="+"search"+"+");
            updateUI("searching");
            facesFound=0;
        }

       // if(screenOrientation== ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) Core.flip(grayscaleImage.t(), grayscaleImage, 1);

        Core.flip(aInputFrame, aInputFrame, 1); //Espejar imagen


        return aInputFrame;
    }



    /********UPDATE UI***************************************************************************************/
    private void updateUI(final String i){
        if (facesFound!=facesFoundOld) {  //Check for change in face detection
            runOnUiThread(new Runnable() {
                public void run() {
                    int id = getResources().getIdentifier(i, "drawable", getPackageName());
                    myImage.setImageResource(id);
                    String text="";
                    if(i=="searching") text="Searching";
                    if(i=="found") text="I found you.You shall be exterminated at once.";
                    ttobj.speak(text, TextToSpeech.QUEUE_FLUSH, null);




                }
            });
        facesFoundOld=facesFound;
        }
    }

    /********CHECK COORDINATES******************************************************************************/
    private void checkCoordiates(int x, int y,int w, int h) {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new android.graphics.Point();
        display.getSize(size);


        /*if(screenOrientation== ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) screenWidth = size.y;
        if(screenOrientation== ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) screenWidth = size.x;*/

        //screenWidth = size.y;
        screenWidth = size.x;

        int midScreenWindow = 50;//error margin
        int servoPosition;

        //Find out if the X component of the face is to the left of the middle of the screen.
        if((x+w/2) < ((screenWidth/2) - midScreenWindow)){
            servoPosition=1;
           // Log.d(TAG, " LEFT");
        }
        //Find out if the X component of the face is to the right of the middle of the screen.
        else if((x+w/2) > ((screenWidth/2) + midScreenWindow)){
            servoPosition=-1;
            //Log.d(TAG, " RIGHT");
        }
        else servoPosition=0;


        sendData("="+servoPosition+"+");
    }













    /********ON RESUME**************************************************************************************/

    @Override
    public void onResume() {
        super.onResume();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);


        BluetoothDevice device = btAdapter.getRemoteDevice(address);    //Pointer to BT in Robot

        try {
            btSocket = createBluetoothSocket(device);   //Create Socket to Device
        } catch (IOException e1) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();    //Discovery consumes resources -> Cancel before connecting

        try {
            btSocket.connect();     //Connect to Robot
        } catch (IOException e) {
            try {
                btSocket.close();   //If unable to connect, close socket
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        try {
            outStream = btSocket.getOutputStream(); //Create output stream
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }







    }

    /********ON PAUSE***************************************************************************************/

    @Override
    public void onPause() {
        super.onPause();

        if (outStream != null) {
            try {
                outStream.flush();  //If output stream is not empty, send data
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();   //Close socket
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }


    /********CHECK BT STATE*********************************************************************************/

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }



    /********CREATE BT SOCKET*******************************************************************************/

  /*  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke( MY_UUID,device);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
                Toast.makeText(getBaseContext(),"Could not create socket connection", Toast.LENGTH_LONG).show();
            }

        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID); //forces the system to unpair the antenna and later ask for pairing again.
       // return  device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
    }*/

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }




    /********SEND DATA**************************************************************************************/

    public void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
            outStream.flush();

        } catch (IOException e) {
            String msg= "Phone not connected to client's Bluetooth";
            errorExit("Fatal Error", msg);
        }
    }



    /********ERROR EXIT*************************************************************************************/

    private void errorExit(String title, String message){
        /*try     {
            btSocket.close();   //Close socket
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }*/
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }




























}