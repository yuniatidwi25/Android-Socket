package com.example.socketserver;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    private static final int PORT = 1803;
    private List<Socket> mList = new ArrayList<Socket>();
    private volatile ServerSocket server=null;
    private ExecutorService mExecutorService = null;
    private String hostip;
    private TextView mText1;
    private TextView mText2, mText3;
    private Button mBut1=null;
    private Handler myHandler=null;
    private volatile boolean flag= true;
    private Handler handler = new Handler();
    String s ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        hostip = getLocalIpAddress();
        mText1=(TextView) findViewById(R.id.textView1);
        mText1.setText(hostip);
        mText1.setEnabled(false);
        mText3=(TextView) findViewById(R.id.textViewport);
        mText3.setEnabled(false);
        mText2=(TextView) findViewById(R.id.textView2);

        mBut1=(Button) findViewById(R.id.but1);
        mBut1.setOnClickListener(new Button1ClickListener());
        myHandler =new Handler(){
            @SuppressLint("HandlerLeak")
            public void handleMessage(Message msg){
                if(msg.what==0x1234){
                    mText2.append("\n" + msg.obj.toString());
                }
            }
        };

    }
    private final class Button1ClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(mBut1.getText().toString().equals("Start")){
                ServerThread serverThread=new ServerThread();
                flag=true;
                serverThread.start();
                mBut1.setText("Shut Down");
                mText1.setText(getLocalIpAddress());
                mText3.setText("1803");
                //show IP address
                Toast toast = Toast.makeText(MainActivity.this, "IP address: "+getLocalIpAddress()+ ", PORT: " + PORT , Toast.LENGTH_LONG);
                toast.show();
            }else{
                try {
                    flag=false;
                    server.close();
                    for(int p=0;p<mList.size();p++){
                        Socket s=mList.get(p);
                        s.close();
                    }
                    mExecutorService.shutdownNow();
                    mBut1.setText("Start");
                    mText1.setText("Client Not Connected");
                    Log.v("Socket-status","Server is shut down");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    class ServerThread extends Thread {
        public void run() {
            try {
                server = new ServerSocket(PORT);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            mExecutorService = Executors.newCachedThreadPool();
            Socket client = null;
            while(flag) {
                try {
                    Log.v("test", String.valueOf(flag));
                    client = server.accept();
                    handler.post(new Runnable() {
                        public void run() {
                            mText1.setText("Client Connected");
                        }
                    });
                    try {
                        handler.post(new Runnable() {
                            public void run() {
                                mText2.setText(s);  //post message on the textView
                            }
                        });
                    } catch (Exception e) {
                        handler.post(new Runnable() {
                            public void run() {
                                mText2.setText(s);
                            }
                        });
                    }
                    mList.add(client);
                    mExecutorService.execute(new Service(client));
                }catch ( IOException e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        public void run() {
                            mText2.setText("Disconnected");
                        }
                    });
                }
            }
        }
    }
    @SuppressLint("LongLogTag")
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return null;
    }
    class Service implements Runnable {
        private volatile boolean kk=true;
        private Socket socket;
        private BufferedReader in = null;
        private String msg = "";

        public Service(Socket socket) {
            this.socket = socket;   //reada6
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
                msg="Connected to server";   //reada8
                this.sendmsg(msg);  //reada9
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while(kk) {
                try {
                    if((msg = in.readLine())!= null) {
                        if(msg.equals("exit")) {
                            mList.remove(socket);
                            dialog Dialog = new dialog();
                            Dialog.show(getSupportFragmentManager(), "dialog");
                            mText1.setText("Client not connected");
                            break;
                        } else {
                            Message msgLocal = new Message();
                            msgLocal.what = 0x1234;
                            msgLocal.obj =msg+" （From Client）" ;

                            System.out.println(msgLocal.obj.toString());
                            System.out.println(msg);
                            myHandler.sendMessage(msgLocal);
                            msg = socket.getInetAddress() + " : " + msg+"（From Server）";
                            this.sendmsg(msg);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("close");
                    kk=false;
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        public void sendmsg(String msg) {
            System.out.println(msg);
            PrintWriter pout = null;
            try {
                pout = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())),true);
                pout.println(msg);
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}